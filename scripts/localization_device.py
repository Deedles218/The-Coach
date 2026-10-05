#!/usr/bin/env python3
"""Snapshot/restore language preferences on a local test Simulator/emulator."""
import json
import os
from pathlib import Path
import plistlib
import re
import subprocess
import sys


def command(*args):
    return subprocess.check_output(args, stderr=subprocess.STDOUT, timeout=20).decode().strip()


def simulator(udid, *args):
    return command("xcrun", "simctl", "spawn", udid, "defaults", *args)


def adb(udid, *args):
    return command(os.environ.get("ADB", "adb"), "-s", udid, *args)


def android_locale(udid):
    locales = adb(udid, "shell", "settings", "get", "system", "system_locales")
    if "," in locales:
        raise ValueError("Use a test emulator with one system language; multi-language restoration is not implemented")
    locale = locales if locales not in ("", "null") else adb(udid, "shell", "getprop", "persist.sys.locale")
    if not locale:
        # Fresh API 36 images leave persist.sys.locale unset until the first change.
        config = adb(udid, "shell", "am", "get-config")
        match = re.search(r"(?:^|[- ])([a-z]{2,3})-r([A-Z]{2})(?:-|$)", config)
        locale = "-".join(match.groups()) if match else ""
    if not re.fullmatch(r"[a-z]{2,8}-[A-Z]{2}", locale):
        raise ValueError("Unsupported original Android locale: " + locale)
    return locale


def set_android_locale(udid, language, country):
    if android_locale(udid) == language + "-" + country:
        return
    args = ("shell", "am", "broadcast", "-a", "io.appium.settings.locale",
            "-n", "io.appium.settings/.receivers.LocaleSettingReceiver",
            "--es", "lang", language, "--es", "country", country)
    output = adb(udid, *args)
    if "NoSuchMethodException" in output:
        # Same one-time restart used by the installed Appium Settings client:
        # an existing process may not have picked up the driver's API policy.
        adb(udid, "shell", "am", "force-stop", "io.appium.settings")
        adb(udid, "shell", "am", "start", "-n", "io.appium.settings/.Settings")
        output = adb(udid, *args)
    if "result=-1" not in output or android_locale(udid) != language + "-" + country:
        raise RuntimeError("Android system-language change failed: " + output)


def app_preferences(udid, path):
    # Read through CFPreferences, including pending values that may not yet
    # have reached the plist. An absolute defaults domain excludes .plist.
    domain = str(Path(path).with_suffix(""))
    return plistlib.loads(simulator(udid, "export", domain, "-").encode())


def update_app_language(udid, path, languages):
    domain = str(Path(path).with_suffix(""))
    if languages is None:
        if "AppleLanguages" in app_preferences(udid, path):
            simulator(udid, "delete", domain, "AppleLanguages")
    else:
        simulator(udid, "write", domain, "AppleLanguages", "-array", *languages)


def stop_ios_language_clients(state):
    for app_id in (state["appId"], "com.apple.Preferences"):
        subprocess.run(["xcrun", "simctl", "terminate", state["udid"], app_id],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20)


def prepare(state):
    # System-language checks require no app-specific override on iOS.
    if state["platform"] == "ios":
        stop_ios_language_clients(state)
        update_app_language(state["udid"], state["appPreferences"], None)


def language_menu_preferences(languages):
    preferred = list(languages)
    if not preferred:
        raise ValueError("Current preferred language list is empty")
    codes = {item.replace("_", "-").split("-")[0] for item in preferred}
    if len(codes) < 2:
        preferred.append("fr" if "en" in codes else "en")
    return preferred


def snapshot(platform, udid, app_id):
    if platform == "ios":
        if not re.fullmatch(r"[A-Fa-f0-9-]{36}", udid):
            raise ValueError("Use a local iOS Simulator UDID")
        global_data = plistlib.loads(simulator(udid, "export", "NSGlobalDomain", "-").encode())
        container = command("xcrun", "simctl", "get_app_container", udid, app_id, "data")
        preferences = Path(container) / "Library/Preferences" / (app_id + ".plist")
        app_data = app_preferences(udid, preferences)
        return dict(platform=platform, udid=udid, appId=app_id,
                    languages=global_data["AppleLanguages"], locale=global_data["AppleLocale"],
                    appPreferences=str(preferences), appLanguages=app_data.get("AppleLanguages"))
    if platform != "android" or not re.fullmatch(r"emulator-[0-9]+", udid):
        raise ValueError("Use an explicit Android test emulator")
    return dict(platform=platform, udid=udid, appId=app_id, locale=android_locale(udid))


def restore(state):
    udid = state["udid"]
    if state["platform"] == "ios":
        # Coordinate with CFPreferences instead of racing its cached plist writes.
        stop_ios_language_clients(state)
        update_app_language(udid, state["appPreferences"], state["appLanguages"])
        simulator(udid, "write", "NSGlobalDomain", "AppleLanguages", "-array", *state["languages"])
        simulator(udid, "write", "NSGlobalDomain", "AppleLocale", "-string", state["locale"])
        restored = snapshot("ios", udid, state["appId"])
        if any(restored[key] != state[key] for key in ("languages", "locale", "appLanguages")):
            raise RuntimeError("iOS language preferences did not restore to the snapshot")
    else:
        lang, country = state["locale"].split("-")
        set_android_locale(udid, lang, country)


def main(args):
    action, path, *rest = args
    if action == "locale" and path == "android":
        print(android_locale(rest[0]))
    elif action == "set-locale" and path == "android":
        set_android_locale(*rest)
    elif action == "snapshot":
        state = snapshot(*rest)
        Path(path).write_text(json.dumps(state), encoding="utf-8")
    elif action == "prepare":
        state = json.loads(Path(path).read_text(encoding="utf-8"))
        prepare(state)
    elif action == "prepare-menu":
        state = json.loads(Path(path).read_text(encoding="utf-8"))
        # iOS hides the per-app language menu with only one preferred language.
        # Preserve the current device language: English override tests must
        # select English while French is the actual device default.
        if state["platform"] == "ios":
            current = plistlib.loads(simulator(state["udid"], "export", "NSGlobalDomain", "-").encode())
            preferred = language_menu_preferences(current["AppleLanguages"])
            simulator(state["udid"], "write", "NSGlobalDomain", "AppleLanguages", "-array", *preferred)
    elif action == "restore":
        restore(json.loads(Path(path).read_text(encoding="utf-8")))
    else:
        raise ValueError("Expected snapshot, prepare, prepare-menu or restore")


if __name__ == "__main__":
    try:
        main(sys.argv[1:])
    except Exception as failure:
        sys.exit("Localization device operation failed (recovery file: "
                 + (sys.argv[2] if len(sys.argv) > 2 else "unknown") + "): " + str(failure))
