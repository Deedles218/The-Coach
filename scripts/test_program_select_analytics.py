"""Offline SQLite checks for selection analytics and its allowlisted property."""
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import patch

from observe_simulator_event import main, observe


class ProgramModalSelectEvidenceTest(unittest.TestCase):
    UID = "dedicated-selector-test-user"
    EVENT = "ProgramModalSelect"
    PROGRAM = "last_longer"
    ARMED_AT = 1700000000000

    def event(self, **overrides):
        return dict({"event_type": self.EVENT, "user_id": self.UID,
                     "uuid": "fresh-sdk-event", "timestamp": self.ARMED_AT,
                     "event_properties": {"program_id": self.PROGRAM}}, **overrides)

    def observe_events(self, events, old_event=None, reset_ids=False, session_uid=UID):
        with tempfile.TemporaryDirectory(prefix="coach-select-unit-") as directory:
            database = Path(directory) / "events.db"
            output = Path(directory) / "evidence.json"
            writer = sqlite3.connect(database)
            writer.executescript("CREATE TABLE store(key TEXT,value TEXT); "
                                 "CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,event TEXT);")
            writer.execute("INSERT INTO store VALUES('user_id',?)", (session_uid,))
            if old_event is not None:
                writer.execute("INSERT INTO events(id,event) VALUES(100,?)", (json.dumps(old_event),))
            writer.commit()
            clock = [0.0]

            def arm(*args, **kwargs):
                self.assertEqual(("READY",), args)
                if reset_ids:
                    writer.execute("DELETE FROM events")
                    writer.execute("DELETE FROM sqlite_sequence WHERE name='events'")
                for item in events:
                    writer.execute("INSERT INTO events(event) VALUES(?)", (json.dumps(item),))
                writer.commit()

            def advance(seconds):
                clock[0] += seconds

            try:
                with patch("observe_simulator_event.print", side_effect=arm), \
                        patch("observe_simulator_event.time.time", return_value=self.ARMED_AT / 1000), \
                        patch("observe_simulator_event.time.monotonic", side_effect=lambda: clock[0]), \
                        patch("observe_simulator_event.time.sleep", side_effect=advance):
                    observed = observe(database, self.UID, self.EVENT, output, timeout=.05,
                                       expected_program_id=self.PROGRAM)
                return observed, json.loads(output.read_text())
            finally:
                writer.close()

    def test_exact_program_is_observed_and_only_allowlisted_properties_are_persisted(self):
        observed, evidence = self.observe_events([self.event(
            event_properties={"program_id": self.PROGRAM, "private": "DO-NOT-PERSIST"},
            user_properties={"email": "DO-NOT-PERSIST"})])
        self.assertTrue(observed)
        self.assertTrue(evidence["expectedUserMatched"])
        self.assertEqual({"program_id": self.PROGRAM}, evidence["eventProperties"])
        self.assertEqual({"program_id": self.PROGRAM}, evidence["expectedEventProperties"])
        serialized = json.dumps(evidence)
        self.assertNotIn("DO-NOT-PERSIST", serialized)
        self.assertNotIn(self.UID, serialized)
        self.assertNotIn("fresh-sdk-event", serialized)
        self.assertNotIn("event_properties", serialized)

    def test_wrong_missing_or_malformed_program_property_cannot_pass(self):
        for properties in ({}, {"program_id": "overall_health"}, {"program_id": None},
                           {"program_id": [self.PROGRAM]}, {"program_id": 1},
                           {"program_id": self.PROGRAM + " "}, {"program_id": self.PROGRAM.upper()},
                           {"programId": self.PROGRAM}, None, []):
            with self.subTest(properties=properties):
                observed, evidence = self.observe_events([self.event(event_properties=properties)])
                self.assertFalse(observed)
                self.assertEqual(1, evidence["programIdMismatchCount"])
                self.assertNotIn("eventProperties", evidence)

    def test_absent_properties_cannot_pass(self):
        event = self.event()
        del event["event_properties"]
        self.assertFalse(self.observe_events([event])[0])

    def test_wrong_program_before_matching_program_does_not_mask_match(self):
        observed, evidence = self.observe_events([
            self.event(uuid="wrong-program-event", event_properties={"program_id": "another-program"}),
            self.event()])
        self.assertTrue(observed)
        self.assertEqual(1, evidence["programIdMismatchCount"])
        self.assertEqual(2, evidence["eventId"])
        self.assertNotIn("another-program", json.dumps(evidence))

    def test_event_must_belong_to_expected_user_and_event_type_and_current_time(self):
        for overrides in ({"user_id": "another-user"}, {"event_type": "ProgramModalOpen"},
                          {"timestamp": self.ARMED_AT - 1}):
            with self.subTest(overrides=overrides):
                self.assertFalse(self.observe_events([self.event(**overrides)])[0])

    def test_wrong_analytics_session_cannot_arm(self):
        with self.assertRaisesRegex(RuntimeError, "dedicated test account"):
            self.observe_events([self.event()], session_uid="another-user")

    def test_baseline_uuid_cannot_pass_even_with_new_row_and_fresh_timestamp(self):
        observed, _ = self.observe_events([self.event()], old_event=self.event(timestamp=1))
        self.assertFalse(observed)

    def test_fresh_uuid_survives_sqlite_row_id_restart(self):
        observed, evidence = self.observe_events([self.event()],
            old_event=self.event(uuid="old-sdk-event", timestamp=1), reset_ids=True)
        self.assertTrue(observed)
        self.assertEqual(100, evidence["baselineId"])
        self.assertEqual(1, evidence["eventId"])
        self.assertTrue(evidence["rowIdRestarted"])

    def test_unrelated_tooltip_properties_are_not_in_selection_evidence(self):
        observed, evidence = self.observe_events([
            self.event(uuid="tooltip-event", event_type="ToolTipPostponeActivity shown",
                       event_properties={"DO-NOT-PERSIST": "shown"}), self.event()])
        self.assertTrue(observed)
        self.assertNotIn("candidates", evidence)
        self.assertNotIn("DO-NOT-PERSIST", json.dumps(evidence))

    def test_selection_without_exact_expected_program_fails_before_arming(self):
        for program in (None, "", "  ", 1):
            with self.subTest(program=program), self.assertRaisesRegex(RuntimeError, "exact expected"):
                observe("unused.db", self.UID, self.EVENT, "unused.json", expected_program_id=program)

    def test_other_events_cannot_silently_accept_program_expectation(self):
        with self.assertRaisesRegex(RuntimeError, "only for ProgramModalSelect"):
            observe("unused.db", self.UID, "ProgramModalOpen", "unused.json", expected_program_id=self.PROGRAM)

    def test_cli_forwards_exact_expected_program_id(self):
        args = ["observe_simulator_event.py", "--database", "events.db", "--output", "evidence.json",
                "--event", self.EVENT, "--expected-program-id", self.PROGRAM]
        with patch("sys.argv", args), patch.dict("os.environ", {"COACH_EXPECTED_ANALYTICS_UID": self.UID}), \
                patch("observe_simulator_event.observe", return_value=True) as observer:
            self.assertEqual(0, main())
        observer.assert_called_once_with("events.db", self.UID, self.EVENT, "evidence.json",
                                         expected_program_id=self.PROGRAM)


if __name__ == "__main__":
    unittest.main()
