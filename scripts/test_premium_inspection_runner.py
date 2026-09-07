"""No Keychain, subprocesses, network or account writes: runner scope tests."""
import json
import unittest
from unittest.mock import patch

import run_test_model_local as runner


class PremiumInspectionRunnerTest(unittest.TestCase):
    def setUp(self):
        self.aliases = {"COA-8511": {"email": "fixture@example.invalid", "otp": "unit", "status": "confirmed"}}
        self.shared = {"email": "shared@example.invalid", "otp": "unit",
                       "allowedCases": ["COA-8511", "COA-8512"], "status": "provided"}

    def keychain(self, service, **kwargs):
        return json.dumps(self.shared if service == runner.PREMIUM_SERVICE else self.aliases)

    def test_deferred_case_cannot_run_or_provision(self):
        for extra in ([], ["--provision"]):
            with patch("sys.argv", ["runner", "COA-8518"] + extra), \
                    patch.object(runner, "keychain", side_effect=self.keychain), \
                    patch.object(runner, "run") as run, patch.object(runner, "save") as save:
                with self.assertRaisesRegex(RuntimeError, "deferred"):
                    runner.main()
                run.assert_not_called()
                save.assert_not_called()

    def test_inspection_cannot_provision_or_target_other_cases(self):
        for args in (["COA-8511", "--provision"], ["COA-8517"]):
            with patch("sys.argv", ["runner", "--inspect-premium"] + args), \
                    patch.object(runner, "keychain", side_effect=self.keychain), \
                    patch.object(runner, "run") as run:
                with self.assertRaisesRegex(RuntimeError, "never provisions"):
                    runner.main()
                run.assert_not_called()

    def test_only_inspection_record_is_updated_and_failure_stops_sequence(self):
        with patch("sys.argv", ["runner", "COA-8511", "COA-8512", "--inspect-premium"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run", return_value=1) as run, \
                patch.object(runner, "save") as save:
            self.assertEqual(1, runner.main())
        self.assertEqual(1, run.call_count)
        self.assertEqual(self.shared["email"], run.call_args.args[1]["email"])
        self.assertEqual(runner.PREMIUM_SERVICE, save.call_args.args[1])
        self.assertEqual("fixture@example.invalid", self.aliases["COA-8511"]["email"])

    def test_shared_account_cases_run_sequentially(self):
        with patch("sys.argv", ["runner", "COA-8511", "COA-8512", "--inspect-premium"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run", return_value=0) as run, \
                patch.object(runner, "save"):
            self.assertEqual(0, runner.main())
        self.assertEqual(["COA-8511", "COA-8512"], [call.args[0] for call in run.call_args_list])

    def test_program_navigation_needs_explicit_premium_inspection_mode(self):
        with patch("sys.argv", ["runner", "COA-8511", "--inspect-programs"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run") as run:
            with self.assertRaisesRegex(RuntimeError, "requires"):
                runner.main()
            run.assert_not_called()

    def test_new_shared_account_cannot_silently_fall_back_to_old_aliases(self):
        with patch("sys.argv", ["runner", "COA-8511"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run") as run:
            with self.assertRaisesRegex(RuntimeError, "old dedicated aliases"):
                runner.main()
            run.assert_not_called()

    def test_new_approval_metadata_is_not_overwritten_after_long_inspection(self):
        def finish_run(*args):
            self.shared["fixtureReady"] = False
            self.shared["fixtureBlockers"] = ["fixture-state-unavailable"]
            return 0
        with patch("sys.argv", ["runner", "COA-8511", "--inspect-premium"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run", side_effect=finish_run), \
                patch.object(runner, "save") as save:
            self.assertEqual(0, runner.main())
        self.assertEqual(["fixture-state-unavailable"], save.call_args.args[0]["fixtureBlockers"])

    def test_changed_identity_cannot_be_overwritten_with_stale_record(self):
        def finish_run(*args):
            self.shared["uid"] = "new-user-identity"
            return 0
        with patch("sys.argv", ["runner", "COA-8511", "--inspect-premium"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run", side_effect=finish_run), \
                patch.object(runner, "save") as save:
            with self.assertRaisesRegex(RuntimeError, "identity changed"):
                runner.main()
            save.assert_not_called()

    def test_authorized_kegel_cases_use_shared_record_sequentially(self):
        from test_model_kegel_fixture import SCOPE
        self.shared.update(status="identity-and-premium-verified", kegelMutationScope=dict(SCOPE), accountDeletionAllowed=False)
        with patch("sys.argv", ["runner", "COA-8511", "COA-8512"]), \
                patch.object(runner, "keychain", side_effect=self.keychain), \
                patch.object(runner, "run", return_value=0) as run, \
                patch.object(runner, "save") as save:
            self.assertEqual(0, runner.main())
        self.assertEqual(["COA-8511", "COA-8512"], [c.args[0] for c in run.call_args_list])
        self.assertTrue(all(c.args[1]["email"] == self.shared["email"] for c in run.call_args_list))
        self.assertTrue(all(c.args[1] == runner.PREMIUM_SERVICE for c in save.call_args_list))


if __name__ == "__main__":
    unittest.main()
