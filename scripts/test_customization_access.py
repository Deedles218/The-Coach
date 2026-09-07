"""Offline prerequisite checks. No network requests or real test-account changes."""
import json
import io
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import Mock, patch

from test_model_app_api import PreprodAppClient
from test_model_fixture_api import verify_preprod_account_email


class ExistingAccountIdentityTest(unittest.TestCase):
    UID = "testExistingIdentity123456789"
    EMAIL = "existing@example.invalid"

    def check(self, result, csrf=True):
        form = b'<input name="csrfmiddlewaretoken" value="unit-csrf">' if csrf else b'<p>No form</p>'
        opener = Mock()
        opener.open.side_effect = [io.BytesIO(form), io.BytesIO(result.encode())]
        with patch("test_model_fixture_api.urllib.request.build_opener", return_value=opener):
            verify_preprod_account_email(self.UID, self.EMAIL)
        for invocation in opener.open.call_args_list:
            self.assertTrue(invocation.args[0].full_url.endswith("/get_user_info/"))
        return opener

    def test_verified_identity_uses_only_read_only_account_lookup(self):
        self.assertEqual(2, self.check("<p>Email: EXISTING@example.invalid</p>").open.call_count)

    def test_different_account_email_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "does not belong"):
            self.check("<p>Email: different@example.invalid</p>")

    def test_missing_or_ambiguous_email_cannot_pass(self):
        for result in ("<p>No account</p>", "<p>existing@example.invalid other@example.invalid</p>"):
            with self.assertRaises(RuntimeError):
                self.check(result)

    def test_missing_csrf_does_not_submit_lookup(self):
        with self.assertRaisesRegex(RuntimeError, "CSRF"):
            self.check("<p>Email: existing@example.invalid</p>", csrf=False)

    def test_invalid_uid_cannot_start_network_lookup(self):
        with patch("test_model_fixture_api.urllib.request.build_opener") as opener:
            with self.assertRaises(RuntimeError):
                verify_preprod_account_email("", self.EMAIL)
            opener.assert_not_called()


class CustomizationAccessTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="coach-access-unit-")
        self.addCleanup(self.directory.cleanup)
        self.client = object.__new__(PreprodAppClient)
        self.client.case = "COA-8511"
        self.client.container = Path(self.directory.name)
        self.client.expected_uid = "unit-test-identity"
        library = self.client.container / "Library"
        library.mkdir()
        connection = sqlite3.connect(library / "com.amplitude.database")
        connection.execute("CREATE TABLE store(key TEXT, value TEXT)")
        connection.execute("INSERT INTO store VALUES('user_id',?)", (self.client.expected_uid,))
        connection.commit()
        connection.close()
        self.client.get = Mock(return_value={"userdata": {"subscription": {"active": True}}})

    def flag(self, enabled):
        path = self.client.container / "Library/Application Support/Google/RemoteConfig/RemoteConfig.sqlite3"
        path.parent.mkdir(parents=True)
        connection = sqlite3.connect(path)
        connection.execute("CREATE TABLE main_active(key TEXT, value TEXT)")
        connection.execute("INSERT INTO main_active VALUES(?,?)", (
            "abtest_complete_to_unlock_popup", json.dumps({"isEnabled": enabled, "metricsId": "unit-branch"})))
        connection.commit()
        connection.close()

    def test_active_access_is_not_claimed_as_fixture_readiness(self):
        result = self.client.customization_access()
        self.assertTrue(result["accessReady"])
        self.assertNotIn("fixtureReady", result)
        self.assertNotIn("uid", result)
        self.client.get.assert_called_once_with("/api/v1/user/")

    def test_inactive_subscription_blocks_both_move_cases(self):
        self.client.get.return_value = {"userdata": {"subscription": {"active": False}}}
        for case in ("COA-8511", "COA-8512"):
            self.client.case = case
            result = self.client.customization_access()
            self.assertFalse(result["accessReady"])
            self.assertEqual(["premium_inactive"], result["blockers"])

    def test_missing_or_non_boolean_subscription_does_not_grant_access(self):
        for subscription in ({}, {"active": "true"}, {"active": 1}):
            self.client.get.return_value = {"userdata": {"subscription": subscription}}
            result = self.client.customization_access()
            self.assertFalse(result["accessReady"])
            self.assertIsNone(result["subscriptionActive"])
            self.assertIn("subscription_state_unknown", result["blockers"])

    def test_previous_account_token_cannot_check_current_account(self):
        self.client.expected_uid = "different-identity"
        with self.assertRaisesRegex(RuntimeError, "identity differs"):
            self.client.customization_access()
        self.client.get.assert_not_called()

    def test_popup_requires_enabled_branch_in_addition_to_premium(self):
        self.client.case = "COA-8518"
        self.flag(False)
        result = self.client.customization_access()
        self.assertTrue(result["subscriptionActive"])
        self.assertEqual(["popup_branch_disabled_or_unknown"], result["blockers"])
        self.assertFalse(result["accessReady"])

    def test_unknown_popup_branch_fails_closed(self):
        self.client.case = "COA-8518"
        result = self.client.customization_access()
        self.assertFalse(result["accessReady"])
        self.assertIsNone(result["popupConfig"]["isEnabled"])

    def test_enabled_popup_branch_does_not_claim_first_show_reset(self):
        self.client.case = "COA-8518"
        self.flag(True)
        result = self.client.customization_access()
        self.assertTrue(result["accessReady"])
        self.assertNotIn("firstShowReady", result)


class SharedAccountInspectionGuardTest(unittest.TestCase):
    def setUp(self):
        self.client = object.__new__(PreprodAppClient)
        self.client.read_only = True
        self.client.case = "COA-8511"
        self.client.headers = {}
        self.client.opener = Mock()

    def test_only_verified_subscription_route_is_allowed(self):
        self.client.opener.open.return_value = io.BytesIO(b'{}')
        self.assertEqual({}, self.client.get("/api/v1/user/"))
        with self.assertRaisesRegex(RuntimeError, "cannot change"):
            self.client.get("/api/v1/coachprogram/progress")

    def test_selecting_a_day_is_rejected_even_though_it_is_get(self):
        with self.assertRaisesRegex(RuntimeError, "cannot change"):
            self.client.daily("overall_health", 2)
        self.client.opener.open.assert_not_called()

    def test_card_mutations_cannot_use_read_only_inspection(self):
        self.client.case = "COA-8517"
        with self.assertRaisesRegex(RuntimeError, "cannot modify"):
            self.client._recovery_fixture_write("restore", {"program_id": "overall_health",
                "action_titles": ["exercise_meal_plan_check"]})
        self.client.opener.open.assert_not_called()

    def test_inspection_cannot_be_used_for_other_cases(self):
        with patch("test_model_app_api.verify_preprod_account_email") as verify:
            with self.assertRaisesRegex(RuntimeError, "restricted"):
                PreprodAppClient("COA-8517", {"status": "confirmed"}, "unused", read_only=True)
            verify.assert_not_called()

    def test_read_only_identity_must_be_verified_before_cache_access(self):
        with patch("test_model_app_api.verify_preprod_account_email", side_effect=RuntimeError("identity mismatch")), \
                patch("test_model_app_api.subprocess.check_output") as cache:
            with self.assertRaisesRegex(RuntimeError, "identity mismatch"):
                PreprodAppClient("COA-8511", {"status": "confirmed", "uid": "unit", "email": "user@example.invalid"},
                                 "unused", read_only=True)
            cache.assert_not_called()


if __name__ == "__main__":
    unittest.main()
