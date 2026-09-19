"""Offline checks for module-account isolation and provisioning preflight."""
import contextlib
import io
import json
import types
import unittest
from unittest.mock import patch

import run_test_model_local as runner


class ModuleRunnerTests(unittest.TestCase):
    def test_supplied_account_cannot_be_provisioned(self):
        with patch("sys.argv", ["runner", "COA-9044", "--modules-premium", "--provision"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0)), \
                patch.object(runner, "keychain") as keychain, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit): runner.main()
            keychain.assert_not_called()

    def test_supplied_account_uses_separate_registry_and_preserves_dedicated_account(self):
        record = {"email": "unit@example.invalid", "otp": "unit", "uid": "verified-uid",
                  "allowedCases": ["COA-9044"], "subscriptionActive": True}
        with patch("sys.argv", ["runner", "COA-9044", "--modules-premium", "--modules-program-id=last_longer"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0)), \
                patch.object(runner, "keychain", return_value=json.dumps(record)) as keychain, \
                patch.object(runner, "run", return_value=1) as run, patch.object(runner, "save") as save:
            self.assertEqual(1, runner.main())
            self.assertTrue(all(c.args[0] == runner.MODULES_PREMIUM_SERVICE for c in keychain.call_args_list))
            self.assertEqual({"COA-9044": record}, run.call_args.args[2])
            self.assertEqual(runner.MODULES_PREMIUM_SERVICE, save.call_args.args[1])

    def test_unverified_supplied_account_cannot_run_modules(self):
        with patch("sys.argv", ["runner", "COA-9044", "--modules-premium", "--modules-program-id=last_longer"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=0)), \
                patch.object(runner, "keychain", return_value=json.dumps({"allowedCases": ["COA-9044"]})), \
                patch.object(runner, "run") as run:
            with self.assertRaisesRegex(RuntimeError, "verify Premium"): runner.main()
            run.assert_not_called()

    def test_module_inspection_client_cannot_select_a_day(self):
        from test_model_app_api import PreprodAppClient
        client = object.__new__(PreprodAppClient)
        client.read_only = True
        with self.assertRaisesRegex(RuntimeError, "cannot change"):
            client.get("/api/v1/daily_program/v2/", program_id="last_longer", day_of_program=2)

    def test_unavailable_simulator_does_not_reserve_account(self):
        with patch("sys.argv", ["runner", "COA-9044", "--provision"]), \
                patch.object(runner.subprocess, "run", return_value=types.SimpleNamespace(returncode=1)), \
                patch.object(runner, "keychain") as keychain, patch.object(runner, "save") as save:
            with self.assertRaisesRegex(RuntimeError, "Simulator unavailable"):
                runner.main()
            keychain.assert_not_called()
            save.assert_not_called()

    def test_mixed_accounts_and_unspecified_program_fail_before_keychain(self):
        for args in (["COA-9044", "COA-8232", "--provision"], ["COA-9044"]):
            with self.subTest(args=args), patch("sys.argv", ["runner"] + args), \
                    patch.object(runner, "keychain") as keychain, contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit):
                    runner.main()
                keychain.assert_not_called()

    def test_module_account_runs_suite_with_its_own_identity(self):
        account = {"email": "unit+coa9044@example.invalid", "otp": "unit", "uid": "fixture-uid"}
        args = types.SimpleNamespace(app="fixture.app", provision=False, maven=[], modules_program_id="last_longer")
        process = types.SimpleNamespace(stdout=[], wait=lambda: 0)
        with patch.object(runner.subprocess, "Popen", return_value=process) as popen, \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, runner.run("COA-9044", account, {"COA-9044": account}, args))
        command = popen.call_args.args[0]
        self.assertIn("-Dtest=suites.ModulesSuite", command)
        self.assertIn("-Dcoach.modules.enabled=true", command)
        self.assertIn("-Dcoach.modules.programId=last_longer", command)
        self.assertEqual(account["uid"], popen.call_args.kwargs["env"]["COACH_COA9044_UID"])
        self.assertNotIn(account["email"], " ".join(command))


if __name__ == "__main__":
    unittest.main()
