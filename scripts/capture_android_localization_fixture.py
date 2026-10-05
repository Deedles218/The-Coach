#!/usr/bin/env python3
"""Freeze Android APK resources plus observed Welcome/Login UI without English fallback."""
import argparse
import ast
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
from capture_localization_fixture import LOCALES, escape
from localization_device import snapshot, restore, set_android_locale

RESOURCES = {
    "welcome.start": ("continue_with_guest_account", True),
    "welcome.login": ("start_login_text", True),
    "login.title": ("email_linked_title_text", True),
    "login.continue": ("continue_", True),
    "tab.today": ("today", False), "tab.explore": ("explore", False),
    "tab.shop": ("shop_title", False), "profile.account": ("account_detail", False),
    "profile.support": ("support", False), "profile.terms": ("terms_and_policy", False),
    "profile.logout": ("logout", False),
}


def parse_resources(dump):
    values = {}
    key = None
    for line in dump.splitlines():
        entry = re.match(r"\s*resource \S+ string/(\w+)", line)
        if entry:
            key = entry.group(1)
            continue
        if re.match(r"\s*resource ", line):
            key = None
        value = re.match(r'\s+\(([^)]*)\)\s+(?:\(styled string\)\s+)?("(?:[^"\\]|\\.)*")', line)
        if key and value:
            values[(key, value.group(1))] = ast.literal_eval(value.group(2))
    return values


def collect(badging, dump, variant, allow_missing=False):
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    activity = re.search(r"launchable-activity: name='([^']+)'", badging)
    if not package or not activity:
        raise ValueError("APK package/version/launchable activity is unavailable")
    result = {"platform": "android", "variant": variant, "appId": package.group(1),
              "source.version": package.group(3), "source.build": package.group(2),
              "source.activity": activity.group(1),
              "source.origin": "frozen APK resources; UI case/remote copy and linguistic review pending"}
    values = parse_resources(dump)
    for locale in LOCALES[variant]:
        for key, (resource, uppercase) in RESOURCES.items():
            # Android's default resource is explicitly English in these supplied APKs.
            qualifier = "" if locale == "en" else locale
            value = values.get((resource, qualifier))
            if not value or not value.strip():
                if allow_missing:
                    result["missing." + locale + "." + key] = "APK resource " + resource + " has no " + locale + " translation"
                    continue
                raise ValueError("Missing APK translation: " + locale + "." + key)
            result[locale + "." + key] = value.upper() if uppercase else value
    return result


def capture_ui(data, udid, url):
    state = snapshot("android", udid, data["appId"])
    session = None
    def request(path, body=None, method=None):
        req = urllib.request.Request(url.rstrip("/") + path,
                                     data=None if body is None else json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"}, method=method)
        with urllib.request.urlopen(req, timeout=90) as response:
            return json.load(response)["value"]
    try:
        caps = {"platformName": "Android", "appium:automationName": "UiAutomator2", "appium:udid": udid,
                "appium:deviceName": udid, "appium:appPackage": data["appId"],
                "appium:appActivity": data["source.activity"], "appium:noReset": True}
        session = request("/session", {"capabilities": {"alwaysMatch": caps}})["sessionId"]
        path = "/session/" + session
        def tap(resource):
            element = request(path + "/element", {"using": "id", "value": resource})["element-6066-11e4-a52e-4f735466cecf"]
            request(path + "/element/" + element + "/click", {})
        def await_ids(ids):
            deadline = time.monotonic() + 25
            while True:
                root = ET.fromstring(request(path + "/source"))
                nodes = {n.get("resource-id"): n for n in root.iter()}
                deny = "com.android.permissioncontroller:id/permission_deny_button"
                if deny in nodes:
                    tap(deny)
                    continue
                if all(i in nodes and nodes[i].get("displayed") == "true" for i in ids):
                    return nodes
                if time.monotonic() > deadline:
                    raise ValueError("Capture requires a logged-out Welcome/Login screen: " + ",".join(ids))
        prefix = data["appId"] + ":id/"
        regions = {"en": "US", "fr": "FR", "de": "DE", "it": "IT", "es": "ES"}
        for language in LOCALES[data["variant"]]:
            set_android_locale(udid, language, regions[language])
            for action in ("terminate_app", "activate_app"):
                request(path + "/appium/device/" + action, {"appId": data["appId"]})
            nodes = await_ids([prefix + "btnSignInAnonymous", prefix + "btnLogin"])
            data[language + ".welcome.start"] = nodes[prefix + "btnSignInAnonymous"].get("text")
            data[language + ".welcome.login"] = nodes[prefix + "btnLogin"].get("text")
            tap(prefix + "btnLogin")
            nodes = await_ids([prefix + "tvTitleText", prefix + "btnContinue"])
            for key, resource in (("login.title", "tvTitleText"), ("login.continue", "btnContinue")):
                actual = nodes[prefix + resource].get("text")
                expected = data[language + "." + key]
                if " ".join(actual.split()) != " ".join(expected.split()):
                    raise ValueError("UI does not match the requested APK locale: " + language + "." + key)
                data[language + "." + key] = actual
            # Cold launch at the next iteration returns to Welcome. Login's
            # toolbar back opens the questionnaire and races its transition.
            print("Captured Welcome/Login: " + language, flush=True)
        data["source.origin"] = "frozen APK resources + observed Welcome/Login UI; linguistic review pending"
    finally:
        try:
            restore(state)
        finally:
            if session: request("/session/" + session, method="DELETE")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--aapt2", required=True, type=Path)
    parser.add_argument("--variant", required=True, choices=LOCALES)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--capture-ui", action="store_true")
    parser.add_argument("--allow-missing", action="store_true", help="Record absent resources explicitly; affected checks still fail fixture validation")
    parser.add_argument("--udid")
    parser.add_argument("--appium-url", default="http://127.0.0.1:4723/")
    args = parser.parse_args()
    if args.output.exists(): parser.error("Output already exists; use a new path to preserve the previous oracle")
    if args.capture_ui and not args.udid: parser.error("--capture-ui requires --udid")
    badging = subprocess.check_output([str(args.aapt2), "dump", "badging", str(args.apk)], text=True, timeout=30)
    dump = subprocess.check_output([str(args.aapt2), "dump", "resources", str(args.apk)], text=True, timeout=30)
    data = collect(badging, dump, args.variant, args.allow_missing)
    if args.capture_ui: capture_ui(data, args.udid, args.appium_url)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as output:
        output.write("# Provisional APK baseline. Linguistic review pending.\n")
        for key, value in data.items(): output.write(escape(key) + "=" + escape(value) + "\n")
    print("Captured " + str(len(LOCALES[args.variant])) + " locales: " + str(args.output))


if __name__ == "__main__":
    main()
