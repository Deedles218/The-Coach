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


if __name__ == "__main__":
    unittest.main()
