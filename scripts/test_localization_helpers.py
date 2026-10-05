#!/usr/bin/env python3
"""Offline proof that language restoration preserves unrelated app preferences."""
import pathlib
import plistlib
import unittest
from unittest.mock import patch
from capture_localization_fixture import collect, escape, LOCALES, RESOURCES
from localization_device import update_app_language, app_preferences, android_locale, set_android_locale, language_menu_preferences
from capture_android_localization_fixture import collect as collect_android, parse_resources


class LocalizationHelpersTests(unittest.TestCase):
    def test_language_menu_preserves_the_device_language_for_override_checks(self):
        self.assertEqual(language_menu_preferences(["fr"]), ["fr", "en"])
        self.assertEqual(language_menu_preferences(["en-US"]), ["en-US", "fr"])
        self.assertEqual(language_menu_preferences(["fr", "de"]), ["fr", "de"])

    def test_restore_changes_only_language_key(self):
        preferences = {"AppleLanguages": ["fr"], "progress": 7}
        path = pathlib.Path("/test/Library/Preferences/app.plist")
        def defaults(udid, action, domain, *args):
            self.assertEqual(domain, str(path.with_suffix("")))
            if action == "export":
                return plistlib.dumps(preferences).decode()
            if action == "write":
                preferences[args[0]] = list(args[2:])
            elif action == "delete":
                del preferences[args[0]]
            return ""
        with patch("localization_device.simulator", side_effect=defaults):
            update_app_language("simulator", path, ["es"])
            self.assertEqual(app_preferences("simulator", path), {"AppleLanguages": ["es"], "progress": 7})
            update_app_language("simulator", path, None)
            self.assertEqual(app_preferences("simulator", path), {"progress": 7})

    def test_fresh_android_locale_is_read_from_current_configuration(self):
        values = {("shell", "settings", "get", "system", "system_locales"): "null",
                  ("shell", "getprop", "persist.sys.locale"): "",
                  ("shell", "am", "get-config"): "config: mcc310-mnc260-en-rUS-ldltr-sw411dp"}
        with patch("localization_device.adb", side_effect=lambda udid, *args: values[args]):
            self.assertEqual(android_locale("emulator-5554"), "en-US")

    def test_unchanged_android_locale_needs_no_reflection_or_live_driver(self):
        with patch("localization_device.android_locale", return_value="en-US"), patch("localization_device.adb") as command:
            set_android_locale("emulator-5554", "en", "US")
            command.assert_not_called()

    def test_missing_locale_never_reuses_english(self):
        def read(relative):
            if relative == "Info.plist":
                return plistlib.dumps({"CFBundleIdentifier": "app", "CFBundleShortVersionString": "1"})
            if relative.startswith("fr."):
                raise FileNotFoundError(relative)
            return plistlib.dumps({resource: "value" for _, resource, _ in RESOURCES.values()})
        with self.assertRaises(FileNotFoundError):
            collect(read, "female")

    def test_unicode_and_multiline_properties_are_preserved(self):
        self.assertEqual(escape("Français\nL'app: \\"), "Français\\nL'app\\: \\\\")
        self.assertEqual(LOCALES["female"], ("en", "es", "fr"))

    def test_ios_login_title_case_differs_between_supplied_variants(self):
        def read(relative):
            if relative == "Info.plist":
                return plistlib.dumps({"CFBundleIdentifier": "app", "CFBundleShortVersionString": "1"})
            return plistlib.dumps({resource: "Enter the mail" for _, resource, _ in RESOURCES.values()})
        self.assertEqual(collect(read, "male")["en.login.title"], "ENTER THE MAIL")
        self.assertEqual(collect(read, "female")["en.login.title"], "Enter the mail")

    def test_android_dump_preserves_native_labels_and_styled_copy(self):
        dump = '    resource 0x1 string/welcome\n      () "I\\\"ve bought"\n      (fr) (styled string) "J’ai acheté" u:2,3\n'
        self.assertEqual(parse_resources(dump), {("welcome", ""): 'I"ve bought', ("welcome", "fr"): "J’ai acheté"})

    def test_android_default_english_cannot_replace_missing_french(self):
        badging = "package: name='app' versionCode='1' versionName='1.0'\nlaunchable-activity: name='app.Main'"
        with self.assertRaisesRegex(ValueError, "Missing APK translation"):
            collect_android(badging, 'resource 0x1 string/today\n      () "Today"', "female")

    def test_android_partial_capture_reports_gaps_without_english_fallback(self):
        badging = "package: name='app' versionCode='1' versionName='1.0'\nlaunchable-activity: name='app.Main'"
        data = collect_android(badging, 'resource 0x1 string/today\n      () "Today"', "female", allow_missing=True)
        self.assertEqual(data["en.tab.today"], "Today")
        self.assertNotIn("fr.tab.today", data)
        self.assertIn("missing.fr.tab.today", data)


if __name__ == "__main__":
    unittest.main()
