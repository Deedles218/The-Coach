"""Physical-device transport must preserve the analytics identity/freshness oracle."""
from contextlib import contextmanager
import json
from pathlib import Path
import sqlite3
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from ios_device_evidence import amplitude_snapshot, copy_file, read_identity, validate_device
from observe_simulator_event import observe

UDID = "01234567-0123456789ABCDEF"
BUNDLE = "com.vamapps.The-Coach"
UID = "dedicated-physical-test-account"


class DeviceEvidenceTest(unittest.TestCase):
    def test_invalid_device_bundle_or_path_rejected_before_command(self):
        for udid, bundle in [("", BUNDLE), ("simulator", BUNDLE), (UDID, "com.other.app")]:
            with self.assertRaises(ValueError):
                validate_device(udid, bundle)
        with tempfile.TemporaryDirectory() as directory, patch("ios_device_evidence.subprocess.run") as run:
            for path in ("../outside", "/outside"):
                with self.assertRaises(ValueError):
                    copy_file(directory, path, UDID, BUNDLE)
            run.assert_not_called()

    def test_copy_only_reads_exact_app_container_and_has_timeout(self):
        with tempfile.TemporaryDirectory() as directory, patch("ios_device_evidence.subprocess.run") as run:
            run.return_value.returncode = 0
            copy_file(directory, "Library/com.amplitude.database", UDID, BUNDLE)
            args, options = run.call_args
            self.assertEqual(["xcrun", "devicectl", "device", "copy", "from"], args[0][:5])
            self.assertIn(BUNDLE, args[0])
            self.assertEqual(10, options["timeout"])
            self.assertEqual(subprocess.DEVNULL, options["stdout"])
            self.assertEqual(subprocess.DEVNULL, options["stderr"])

    def test_snapshot_is_removed_even_when_consumer_fails(self):
        def fake_copy(root, relative, *args, **kwargs):
            path = Path(root)/relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"private database")
            return path
        with patch("ios_device_evidence.copy_file", side_effect=fake_copy):
            with self.assertRaisesRegex(RuntimeError, "consumer"):
                with amplitude_snapshot(UDID, BUNDLE) as database:
                    self.assertTrue(database.exists())
                    raise RuntimeError("consumer failed")
            self.assertFalse(database.exists())

    def test_missing_required_database_cannot_produce_evidence(self):
        with tempfile.TemporaryDirectory() as directory, patch("ios_device_evidence.subprocess.run") as run:
            run.return_value.returncode = 1
            with self.assertRaises(RuntimeError):
                copy_file(directory, "Library/com.amplitude.database", UDID, BUNDLE)
            self.assertIsNone(copy_file(directory, "Library/com.amplitude.database-wal", UDID, BUNDLE, required=False))

    def physical_observe(self, changed_uid=False):
        with tempfile.TemporaryDirectory() as directory:
            database = Path(directory)/"events.db"
            output = Path(directory)/"minimal.json"
            with sqlite3.connect(database) as db:
                db.executescript("CREATE TABLE store(key TEXT,value TEXT); CREATE TABLE events(id INTEGER,event TEXT);")
                db.execute("INSERT INTO store VALUES('user_id',?)", (UID,))
            copies = []

            @contextmanager
            def snapshot():
                copies.append(True)
                if len(copies) == 2:
                    with sqlite3.connect(database) as db:
                        if changed_uid:
                            db.execute("UPDATE store SET value='another-physical-account'")
                        event = dict(event_type="ProgramModalSelect", user_id=UID, uuid="fresh",
                                     timestamp=1700000000000, event_properties=dict(program_id="keep_it_hard", secret="never-save"))
                        db.execute("INSERT INTO events VALUES(1,?)", (json.dumps(event),))
                yield database

            with patch("observe_simulator_event.time.time", return_value=1700000000), \
                    patch("observe_simulator_event.print"):
                result = observe(None, UID, "ProgramModalSelect", output, timeout=1,
                                 expected_program_id="keep_it_hard", snapshot=snapshot)
            self.assertEqual(2, len(copies))
            return result, output.read_text()

    def test_refresh_observes_fresh_event_and_keeps_only_allowlisted_evidence(self):
        result, text = self.physical_observe()
        self.assertTrue(result)
        self.assertNotIn(UID, text)
        self.assertNotIn("never-save", text)
        self.assertTrue(json.loads(text)["expectedUserMatched"])

    def test_account_switch_after_arming_cannot_pass(self):
        with self.assertRaisesRegex(RuntimeError, "dedicated test account"):
            self.physical_observe(changed_uid=True)


if __name__ == "__main__":
    unittest.main()
