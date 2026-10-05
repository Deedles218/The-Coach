#!/usr/bin/env python3
"""Freeze selected iOS string resources as a provisional localization smoke oracle."""
import argparse
import json
from pathlib import Path
import plistlib
import zipfile
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET
import time
from localization_device import snapshot, restore, prepare

LOCALES = {"male": ("en", "fr", "de", "it", "es"),
           "female": ("en", "es", "fr"), "female-legacy": ("en",)}
# Case transformations are view-specific and must be confirmed against visible UI.
RESOURCES = {
    "welcome.start": ("StartOnboarding", "startButton", False),
    "welcome.login": ("StartOnboarding", "loginButton", False),
    "login.title": ("EnterEmail", "enterEmailTitle", True),
    "login.continue": ("Common", "Continue", True),
    "tab.today": ("DailyPlan", "today", False),
    "tab.explore": ("Explore", "title", False),
    "tab.shop": ("Common", "shop", False),
    "profile.account": ("Profile", "account", False),
    "profile.support": ("Profile", "support", False),
    "profile.terms": ("Profile", "termsAndPrivacy", False),
    "profile.logout": ("Profile", "logOut", False),
    "paywall.restore": ("Common", "Restore", True),
}


def escape(value):
    return str(value).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r") \
        .replace("\t", "\\t").replace("=", "\\=").replace(":", "\\:")


def collect(read, variant):
    info = plistlib.loads(read("Info.plist"))
    result = {"platform": "ios", "variant": variant, "appId": info["CFBundleIdentifier"],
              "source.version": info["CFBundleShortVersionString"],
              "source.build": info.get("CFBundleVersion", "unknown"),
              "source.origin": "frozen iOS .lproj resources; UI case/remote copy and linguistic review pending"}
    for locale in LOCALES[variant]:
        for key, (table, resource, uppercase) in RESOURCES.items():
            data = plistlib.loads(read(locale + ".lproj/" + table + ".strings"))
            value = data[resource]
            if not isinstance(value, str) or not value.strip():
                raise ValueError("Missing/empty resource: " + locale + "." + key)
            # Women's iOS Login title keeps resource case in accessibility/UI;
            # the men's view uppercases the same resource.
            uppercase_for_view = uppercase and not (key == "login.title" and variant.startswith("female"))
            result[locale + "." + key] = value.upper() if uppercase_for_view else value
    # Stable IDs, where observed, should be added as locator.<checkpoint> keys.
    return result


def capture(app, variant):
    if app.suffix == ".ipa":
        with zipfile.ZipFile(app) as archive:
            roots = [name[:-len("Info.plist")] for name in archive.namelist()
                     if name.startswith("Payload/") and name.endswith(".app/Info.plist")
                     and name.count("/") == 2]
            if len(roots) != 1:
                raise ValueError("Expected exactly one top-level application in IPA")
            return collect(lambda relative: archive.read(roots[0] + relative), variant)
    if app.suffix != ".app" or not app.is_dir():
        raise ValueError("Provide an iOS .app directory or .ipa file")
    return collect(lambda relative: (app / relative).read_bytes(), variant)


def capture_welcome(data, udid, url):
    """Remote-configured Welcome copy is absent from .lproj; capture it separately."""
    state = snapshot("ios", udid, data["appId"])
    session = None
    def request(path, body=None, method=None):
        req = urllib.request.Request(url.rstrip("/") + path,
                                     data=None if body is None else json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"}, method=method)
        try:
            with urllib.request.urlopen(req, timeout=90) as response:
                return json.load(response)["value"]
        except urllib.error.HTTPError as error:
            raise RuntimeError(error.read().decode()) from error
    try:
        prepare(state)
        result = request("/session", {"capabilities": {"alwaysMatch": {
            "platformName": "iOS", "appium:automationName": "XCUITest", "appium:udid": udid,
            "appium:bundleId": data["appId"], "appium:noReset": True, "appium:isHeadless": True,
            "appium:useNewWDA": True}}})
        session = result["sessionId"]
        path = "/session/" + session
        regions = {"en": "US", "fr": "FR", "de": "DE", "it": "IT", "es": "ES"}
        for language in LOCALES[data["variant"]]:
            request(path + "/execute/sync", {"script": "mobile: configureLocalization", "args": [{
                "language": {"name": language}, "locale": {"name": language + "_" + regions[language]}}]})
            for action in ("terminateApp", "activateApp"):
                request(path + "/execute/sync", {"script": "mobile: " + action, "args": [{"bundleId": data["appId"]}]})
            deadline = time.monotonic() + 25
            while True:
                root = ET.fromstring(request(path + "/source"))
                buttons = [node for node in root.iter() if node.tag == "XCUIElementTypeButton"
                           and node.get("visible") == "true" and node.get("label")]
                if len(buttons) == 2 and not any(node.tag.endswith("TextField") for node in root.iter()):
                    break
                if time.monotonic() > deadline:
                    raise ValueError("Capture requires the logged-out Welcome screen; do not capture a questionnaire/profile")
            buttons.sort(key=lambda node: float(node.get("y", "0")))
            data[language + ".welcome.start"] = buttons[0].get("label")
            data[language + ".welcome.login"] = buttons[1].get("label")
            print("Captured Welcome: " + language, flush=True)
        data["source.origin"] = "frozen .lproj resources + observed remote Welcome UI; linguistic review pending"
    finally:
        try:
            if session: request("/session/" + session, method="DELETE")
        finally:
            restore(state)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", required=True, type=Path)
    parser.add_argument("--variant", required=True, choices=LOCALES)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--capture-welcome", action="store_true", help="Capture remote copy from an installed logged-out Simulator app")
    parser.add_argument("--udid", help="Explicit booted local iOS Simulator for Welcome capture")
    parser.add_argument("--appium-url", default="http://127.0.0.1:4723/")
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Output already exists; choose a new path to preserve the previous oracle")
    data = capture(args.app, args.variant)
    if args.capture_welcome:
        if not args.udid: parser.error("--capture-welcome requires --udid")
        capture_welcome(data, args.udid, args.appium_url)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as output:
        output.write("# Provisional resource baseline. Review against visible UI before release gating.\n")
        for key, value in data.items():
            output.write(escape(key) + "=" + escape(value) + "\n")
    print("Captured " + str(len(LOCALES[args.variant])) + " locales: " + str(args.output))


if __name__ == "__main__":
    main()
