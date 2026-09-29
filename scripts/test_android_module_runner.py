import contextlib
import io
import json
import types
import unittest
from unittest.mock import patch
import run_modules_android as runner


class AndroidModuleRunnerTests(unittest.TestCase):
    def test_real_device_rejected_before_reading_credentials(self):
        with patch("sys.argv", ["runner", "--serial", "personal-phone"]), \
                patch.object(runner, "keychain") as keychain, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit): runner.main()
            keychain.assert_not_called()

    def test_non_root_emulator_rejected_before_reading_credentials(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0, stdout="2000\n")), \
                patch.object(runner, "keychain") as keychain:
            with self.assertRaisesRegex(RuntimeError, "adb root"): runner.main()
            keychain.assert_not_called()

    def test_unapproved_build_rejected_before_reading_credentials(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0, stdout="0\n")), \
                patch.object(runner.subprocess, "check_output", return_value="versionName=2.15.7\n"), \
                patch.object(runner, "keychain") as keychain:
            with self.assertRaisesRegex(RuntimeError, "approved manProd"): runner.main()
            keychain.assert_not_called()

    def test_scoped_account_is_required_before_starting_maven(self):
        with patch("sys.argv", ["runner", "--serial", "emulator-5554"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0, stdout="0\n")), \
                patch.object(runner.subprocess, "check_output", side_effect=["versionName=1.40.21\n", "16\n"]), \
                patch.object(runner, "keychain", return_value=json.dumps({"uid": "some-uid", "allowedCases": []})), \
                patch.object(runner.subprocess, "Popen") as run:
            with self.assertRaisesRegex(RuntimeError, "approved"): runner.main()
            run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
