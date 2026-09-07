"""Offline checks for fixture guards and fresh analytics evidence (no app/API writes)."""
import contextlib
import io
import json
from pathlib import Path
import sqlite3
import tempfile
import time
import unittest
from unittest.mock import patch, Mock, call

from observe_simulator_event import observe
from test_model_fixture_api import Form, PreprodFixtures, SameHostRedirect
from test_model_app_api import PreprodAppClient
from test_model_fixture_commands import verify_recovered_days


class FixtureGuardsTest(unittest.TestCase):
    def account(self):
        return {"status": "confirmed", "uid": "testFixtureIdentity123456789",
                "email": "qa+coa8511@example.invalid"}

    def test_shared_or_unconfirmed_account_is_rejected(self):
        with self.assertRaises(RuntimeError):
            PreprodFixtures("COA-8235", self.account())
        for field, value in (("status", "planned"), ("email", "shared@example.invalid"), ("uid", "")):
            account = self.account()
            account[field] = value
            with self.assertRaises(RuntimeError):
                PreprodFixtures("COA-8511", account)

    def test_identity_mismatch_prevents_program_write(self):
        client = PreprodFixtures("COA-8511", self.account())
        response = Form()
        response.feed("<p>Email: another@example.invalid</p>")
        with patch.object(client, "_post", return_value=response) as request:
            with self.assertRaises(RuntimeError):
                client.set_program("last_longer")
            self.assertEqual(["/get_user_info/"], [call.args[0] for call in request.call_args_list])

    def test_official_base_variant_trailing_space_is_preserved(self):
        form = Form()
        form.feed('<select><option value="overall_health ">Base</option></select>')
        self.assertEqual(["overall_health "], form.options)

    def test_cross_host_redirect_is_rejected(self):
        with self.assertRaises(RuntimeError):
            SameHostRedirect().redirect_request(None, None, 302, "", {}, "https://example.invalid/")


class AnalyticsEvidenceTest(unittest.TestCase):
    UID = "verified-test-identity"
    EVENT = "ToolTipPostponeActivity shown"

    def check(self, inserted_event=None, old_event=None, expected_uid=UID, reset_ids=False):
        with tempfile.TemporaryDirectory(prefix="coach-event-unit-") as directory:
            db = Path(directory) / "events.db"
            writer = sqlite3.connect(db)
            writer.executescript("CREATE TABLE store(key TEXT,value TEXT); CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,event TEXT);")
            writer.execute("INSERT INTO store VALUES('user_id',?)", (self.UID,))
            if old_event:
                writer.execute("INSERT INTO events(event) VALUES(?)", (json.dumps(old_event),))
            if reset_ids:
                writer.execute("UPDATE events SET id=100")
            writer.commit()
            def ready(line):
                if line == "READY" and inserted_event:
                    if reset_ids:
                        writer.execute("DELETE FROM events")
                        writer.execute("DELETE FROM sqlite_sequence WHERE name='events'")
                    item = dict(inserted_event)
                    item.setdefault("timestamp", int(time.time() * 1000))
                    writer.execute("INSERT INTO events(event) VALUES(?)", (json.dumps(item),))
                    writer.commit()
                return len(line)
            output = Path(directory) / "evidence.json"
            stream = io.StringIO()
            stream.write = ready
            try:
                with contextlib.redirect_stdout(stream):
                    result = observe(db, expected_uid, self.EVENT, output, timeout=.2)
                return result, json.loads(output.read_text())
            finally:
                writer.close()

    def test_current_event_is_observed_without_payload(self):
        result, data = self.check({"event_type": self.EVENT, "user_id": self.UID,
                                   "event_properties": {"private": "must not persist"}})
        self.assertTrue(result)
        self.assertTrue(data["expectedUserMatched"])
        self.assertNotIn("user_id", data)
        self.assertNotIn("event_properties", data)

    def test_old_row_or_stale_timestamp_cannot_pass(self):
        item = {"event_type": self.EVENT, "user_id": self.UID, "timestamp": 1}
        self.assertFalse(self.check(old_event=item)[0])
        self.assertFalse(self.check(inserted_event=item)[0])

    def test_other_user_event_cannot_pass(self):
        self.assertFalse(self.check({"event_type": self.EVENT, "user_id": "someone-else"})[0])

    def test_wrong_session_identity_cannot_arm(self):
        with self.assertRaises(RuntimeError):
            self.check(expected_uid="wrong-identity")


class ProgramModalOpenEvidenceTest(AnalyticsEvidenceTest):
    EVENT = "ProgramModalOpen"

    def test_baseline_sdk_uuid_cannot_pass_with_new_row_and_timestamp(self):
        result, _ = self.check(
            inserted_event={"event_type": self.EVENT, "user_id": self.UID, "uuid": "same-sdk-event"},
            old_event={"event_type": self.EVENT, "user_id": self.UID, "uuid": "same-sdk-event", "timestamp": 1})
        self.assertFalse(result)

    def test_fresh_sdk_event_after_sql_row_id_restart_is_observed(self):
        result, data = self.check(
            inserted_event={"event_type": self.EVENT, "user_id": self.UID, "uuid": "new-sdk-event"},
            old_event={"event_type": self.EVENT, "user_id": self.UID, "uuid": "old-sdk-event", "timestamp": 1},
            reset_ids=True)
        self.assertTrue(result)
        self.assertEqual(100, data["baselineId"])
        self.assertEqual(1, data["eventId"])
        self.assertTrue(data["rowIdRestarted"])

    def test_event_uploaded_between_100ms_samples_is_still_observed(self):
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory) / "events.db"
            writer = sqlite3.connect(database)
            writer.executescript("CREATE TABLE store(key TEXT,value TEXT); CREATE TABLE events(id INTEGER PRIMARY KEY,event TEXT);")
            writer.execute("INSERT INTO store VALUES('user_id',?)", (self.UID,))
            writer.commit()
            clock = [0.0]
            inserted = [False]

            def advance(seconds):
                clock[0] += seconds
                if clock[0] >= .02 and not inserted[0]:
                    writer.execute("INSERT INTO events VALUES(1,?)", (json.dumps({
                        "event_type": self.EVENT, "user_id": self.UID,
                        "timestamp": int(time.time() * 1000)}),))
                    inserted[0] = True
                if clock[0] >= .07:
                    writer.execute("DELETE FROM events")
                writer.commit()

            try:
                with patch("observe_simulator_event.time.monotonic", side_effect=lambda: clock[0]), \
                        patch("observe_simulator_event.time.sleep", side_effect=advance), \
                        contextlib.redirect_stdout(io.StringIO()):
                    self.assertTrue(observe(database, self.UID, self.EVENT,
                                            Path(directory) / "evidence.json", timeout=.2))
            finally:
                writer.close()


class RecoveryFixtureTest(unittest.TestCase):
    def test_traversal_is_sequential_and_returns_to_fixture_day(self):
        client, fixture = Mock(), Mock()
        fixture.move_to_day.return_value = ["Transfer completed"]
        client.daily.side_effect = lambda program, day: {"program_section": {
            "day_in_program": day, "questions": [{"name": "card", "headline": "Fixture"}]}}
        data = {"program": "overall_health", "actionName": "card", "title": "Fixture", "applicableDays": [2, 3, 4]}
        result = verify_recovered_days(client, fixture, data)
        self.assertEqual([call("overall_health", day) for day in [2, 3, 4, 2]], client.daily.call_args_list)
        fixture.move_to_day.assert_called_once_with("overall_health", 2)
        self.assertEqual([1, 1, 1], [item["matchingCards"] for item in result])

    def test_failed_traversal_still_returns_to_fixture_day(self):
        client, fixture = Mock(), Mock()
        fixture.move_to_day.return_value = ["Transfer completed"]
        client.daily.side_effect = [RuntimeError("Request failed"), {"program_section": {"day_in_program": 2}}]
        with self.assertRaisesRegex(RuntimeError, "Request failed"):
            verify_recovered_days(client, fixture, {"program": "overall_health", "applicableDays": [3]})
        fixture.move_to_day.assert_called_once_with("overall_health", 2)
        self.assertEqual(call("overall_health", 2), client.daily.call_args_list[-1])

    def client(self):
        client = object.__new__(PreprodAppClient)
        client.case = "COA-8517"
        return client

    def section(self):
        return {"program_section": {"total_days": 70, "questions": [{
            "name": "exercise_meal_plan_check", "headline": "Check Your Meal Plan",
            "allow_modification": True, "program_days": ",".join(str(day) for day in range(2, 71))}]}}

    def test_reseeds_only_the_fixture_card_after_interrupted_run(self):
        client = self.client()
        removed = {"items": [{"headline": "Check Your Meal Plan"}]}
        with patch.object(client, "get", side_effect=[removed, {"items": []}, removed]), \
                patch.object(client, "daily", return_value=self.section()), \
                patch.object(client, "_recovery_fixture_write") as writes:
            result = client.prepare_removed_meal_plan()
        self.assertEqual(["restore", "remove"], [call.args[0] for call in writes.call_args_list])
        self.assertEqual(["exercise_meal_plan_check"], writes.call_args_list[0].args[1]["action_titles"])
        self.assertEqual(list(range(2, 71)), result["applicableDays"])
        self.assertTrue(result["baselineContentValidated"])

    def test_http_success_without_reset_postcondition_cannot_pass(self):
        client = self.client()
        removed = {"items": [{"headline": "Check Your Meal Plan"}]}
        with patch.object(client, "get", return_value=removed), patch.object(client, "_recovery_fixture_write") as writes:
            with self.assertRaisesRegex(RuntimeError, "did not restore"):
                client.prepare_removed_meal_plan()
        self.assertEqual(["restore"], [call.args[0] for call in writes.call_args_list])

    def test_content_drift_prevents_removal(self):
        client = self.client()
        section = self.section()
        section["program_section"]["total_days"] = 15
        with patch.object(client, "get", return_value={"items": []}), \
                patch.object(client, "daily", return_value=section), \
                patch.object(client, "_recovery_fixture_write") as writes:
            with self.assertRaises(RuntimeError):
                client.prepare_removed_meal_plan()
            writes.assert_not_called()

    def test_other_accounts_or_cards_cannot_be_modified(self):
        client = self.client()
        with self.assertRaises(RuntimeError):
            client._recovery_fixture_write("remove", {"program_id": "overall_health", "program_day": 2,
                                                       "action_name": "not-the-fixture"})
        client.case = "COA-8511"
        with self.assertRaises(RuntimeError):
            client._recovery_fixture_write("restore", {"program_id": "overall_health",
                                                        "action_titles": ["Check Your Meal Plan"]})


if __name__ == "__main__":
    unittest.main()
