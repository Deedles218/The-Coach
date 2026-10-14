"""Guard against a green safe-suite run silently skipping the module scenarios."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import types
import unittest
from unittest.mock import patch
import run_android_parity as runner


class AndroidParityRunnerTests(unittest.TestCase):
    def test_slides_without_active_fixture_fail_before_keychain_or_device_access(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "slides"]), \
                patch.object(runner, "keychain") as secrets, \
                patch.object(runner.subprocess, "check_output") as device, \
                contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                runner.main()
            secrets.assert_not_called()
            device.assert_not_called()

    def test_prepared_slides_keep_installation_and_select_shared_test(self):
        record = {"email": "fixture@example.invalid", "otp": "0000", "uid": "fixture-uid"}
        process = types.SimpleNamespace(stdout=iter([]), wait=lambda: 0)
        with tempfile.TemporaryDirectory() as folder:
            fixture = Path(folder) / "slides.properties"
            fixture.write_text("appPackage=com.vamapps.thecoach\n")
            with patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "slides",
                                    "--slides-fixture", str(fixture), "--prepared-slides"]), \
                    patch.object(runner, "ROOT", Path(folder)), \
                    patch.object(runner, "keychain", return_value=json.dumps(record)), \
                    patch.object(runner.subprocess, "check_output", return_value="16\n"), \
                    patch.object(runner.subprocess, "Popen", return_value=process) as launch, \
                    contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, runner.main())
            command = launch.call_args.args[0]
            self.assertIn("-Dtest=tests.OnboardingSlidesTests", command)
            self.assertIn("-Donboarding.slides.prepared=true", command)
            self.assertIn("-Dandroid.fullReset=false", command)
            self.assertNotIn("-Dandroid.fullReset=true", command)

    def test_safe_suite_enables_uid_bound_module_tests_and_preserves_exit_status(self):
        record = {"email": "fixture@example.invalid", "otp": "0000", "uid": "fixture-uid"}
        process = types.SimpleNamespace(stdout=iter([]), wait=lambda: 1)
        with tempfile.TemporaryDirectory() as folder, \
                patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "safe"]), \
                patch.object(runner, "ROOT", Path(folder)), \
                patch.object(runner, "keychain", return_value=json.dumps(record)), \
                patch.object(runner.subprocess, "check_output", return_value="16\n"), \
                patch.object(runner.subprocess, "Popen", return_value=process) as launch, \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(1, runner.main())
        command = launch.call_args.args[0]
        environment = launch.call_args.kwargs["env"]
        self.assertIn("-Dtest=suites.AndroidRegressionSuite", command)
        self.assertIn("-Dcoach.modules.enabled=true", command)
        self.assertIn("-Dcoach.modules.programId=last_longer", command)
        self.assertEqual(record["uid"], environment["COACH_COA9044_UID"])
        self.assertEqual(record["email"], environment["COACH_COA9044_EMAIL"])
        self.assertEqual(record["otp"], environment["COACH_COA9044_OTP"])

    def test_female_variant_selects_female_package_and_account_contract(self):
        record = {"email": "fixture@example.invalid", "otp": "0000", "variant": "female"}
        process = types.SimpleNamespace(stdout=iter([]), wait=lambda: 0)
        with tempfile.TemporaryDirectory() as folder, \
                patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "auth",
                                   "--variant", "female"]), \
                patch.dict("os.environ", {"COACH_VALID_EMAIL_WITHOUT_PROGRESS": "new@example.invalid"}), \
                patch.object(runner, "ROOT", Path(folder)), \
                patch.object(runner, "keychain", return_value=json.dumps(record)), \
                patch.object(runner.subprocess, "check_output", return_value="16\n"), \
                patch.object(runner.subprocess, "Popen", return_value=process) as launch, \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, runner.main())
        command = launch.call_args.args[0]
        environment = launch.call_args.kwargs["env"]
        self.assertIn("-Dandroid.appPackage=com.vamapps.thecoach.forher", command)
        self.assertIn("-Dandroid.appActivity=com.vamapps.thecoach.MainActivity", command)
        self.assertEqual("female", environment["COACH_ACCOUNT_VARIANT"])
        self.assertEqual("female", environment["ANDROID_APP_VARIANT"])

    def test_account_variant_mismatch_fails_before_maven(self):
        record = {"email": "fixture@example.invalid", "otp": "0000", "variant": "male"}
        with patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "auth",
                                "--variant", "female"]), \
                patch.dict("os.environ", {"COACH_VALID_EMAIL_WITHOUT_PROGRESS": "new@example.invalid"}), \
                patch.object(runner, "keychain", return_value=json.dumps(record)), \
                patch.object(runner.subprocess, "check_output", return_value="16\n"), \
                patch.object(runner.subprocess, "Popen") as launch, \
                contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                runner.main()
        launch.assert_not_called()

    def test_functional_suite_missing_role_data_fails_before_device_access(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "functional"]), \
                patch.dict("os.environ", {}, clear=True), \
                patch.object(runner, "keychain") as secrets, \
                patch.object(runner.subprocess, "check_output") as device, \
                contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                runner.main()
        secrets.assert_not_called()
        device.assert_not_called()

    def test_male_only_suite_is_rejected_for_female_before_device_access(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554", "--suite", "functional",
                                "--variant", "female"]), \
                patch.object(runner, "keychain") as secrets, \
                patch.object(runner.subprocess, "check_output") as device, \
                contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                runner.main()
        secrets.assert_not_called()
        device.assert_not_called()


if __name__ == "__main__":
    unittest.main()
