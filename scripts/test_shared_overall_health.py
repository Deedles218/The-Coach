"""Offline scope guards; never contacts preprod or Keychain."""
import copy
import unittest
from unittest.mock import Mock, patch
from test_model_shared_overall_health import SCOPE, CASES, ApprovedOverallHealthFixtures, require_approval
from test_model_app_api import PreprodAppClient
from test_model_fixture_api import PreprodFixtures


class SharedOverallHealthTest(unittest.TestCase):
    def setUp(self):
        self.account = {"email": "shared@example.invalid", "uid": "unitTestUserIdentity123456789",
                        "allowedCases": list(CASES), "status": "identity-and-premium-verified",
                        "mutationScope": dict(SCOPE), "accountDeletionAllowed": False}

    def test_only_approved_cases(self):
        for case in CASES: require_approval(case, self.account)
        for case in ("COA-8517", "COA-8518", "COA-7949"):
            with self.assertRaises(RuntimeError): require_approval(case, self.account)

    def test_missing_or_expanded_scope_fails_closed(self):
        for key in SCOPE:
            account = copy.deepcopy(self.account)
            del account["mutationScope"][key]
            with self.assertRaises(RuntimeError): require_approval("COA-8511", account)
        account = copy.deepcopy(self.account)
        account["mutationScope"]["otherProgramsAllowed"] = True
        with self.assertRaises(RuntimeError): require_approval("COA-8511", account)

    def test_dedicated_alias_guard_remains(self):
        account = dict(self.account, status="confirmed")
        with self.assertRaisesRegex(RuntimeError, "shared/non-fixture"):
            PreprodFixtures("COA-8511", account)

    def test_other_program_and_day_writes_stop_before_network(self):
        fixture = ApprovedOverallHealthFixtures("COA-8511", self.account)
        with patch.object(fixture, "verify_identity") as identity:
            for program, day in (("kegel_only", 2), ("maintenance_pe", 2), ("overall_health", 4), ("overall_health", True)):
                with self.assertRaises(RuntimeError): fixture.move_to_day(program, day)
            with self.assertRaises(RuntimeError): fixture.set_program("kegel_only")
            with self.assertRaises(RuntimeError): fixture.set_program("overall_health", "arbitrary")
            identity.assert_not_called()

    def test_shared_api_requires_explicit_overall_health(self):
        client = object.__new__(PreprodAppClient)
        client.shared_overall_health = True
        client.opener = Mock()
        for kwargs in ({}, {"program_id": "kegel_only"}, {"program_id": "overall_health", "day_of_program": 4}):
            with self.assertRaises(RuntimeError): client.get("/api/v1/daily_program/v2/", **kwargs)
        with self.assertRaises(RuntimeError): client.get("/api/v1/coachprogram/progress")
        client.opener.open.assert_not_called()


if __name__ == "__main__": unittest.main()
