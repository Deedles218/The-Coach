"""Device transport must retain current-action, account and cache safety checks."""
from pathlib import Path
import shutil
import unittest
from unittest.mock import patch

from read_simulator_program_content import EvidenceError, ROUTE, read_device_content
import test_read_simulator_program_content as simulator_fixture


class DeviceProgramContentTests(unittest.TestCase):
    UDID = "01234567-0123456789ABCDEF"
    BUNDLE = "com.vamapps.The-Coach"

    def setUp(self):
        self.fixture = simulator_fixture.SimulatorProgramContentTests()
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        self.copies = []

    def copy(self, root, relative, udid, bundle, required=True):
        self.assertEqual((self.UDID, self.BUNDLE), (udid, bundle))
        destination = Path(root) / relative
        self.copies.append(destination)
        source = self.fixture.database if relative.endswith("/Cache.db") else (
            self.fixture.database.parent / "fsCachedData" / Path(relative).name)
        if not source.is_file():
            if not required:
                return None
            raise RuntimeError("Required test cache is absent")
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        return destination

    def read(self):
        with patch("ios_device_evidence.copy_file", side_effect=self.copy), \
                patch("read_simulator_program_content.time.time", return_value=self.fixture.NOW):
            return read_device_content(self.UDID, self.BUNDLE, self.fixture.UID,
                                       self.fixture.PROGRAM, self.fixture.boundary)

    def insert(self, **changes):
        self.fixture.insert(url="https://content.the.coach" + ROUTE + "?date=2026-09-07", **changes)

    def assert_cleaned(self):
        self.assertTrue(self.copies)
        self.assertTrue(all(not path.exists() for path in self.copies))

    def test_reads_production_inline_content_without_exporting_user_data(self):
        self.insert()
        result = self.read()
        self.assertEqual("overall_health", result["programId"])
        self.assertNotIn("must-not-be-exported", str(result))
        self.assertEqual(2, len(self.copies))  # Required database and optional WAL only.
        self.assert_cleaned()

    def test_reads_only_selected_filesystem_body_and_removes_snapshot(self):
        self.insert(filesystem=True)
        self.assertEqual("overall_health", self.read()["programId"])
        bodies = [path for path in self.copies if path.parent.name == "fsCachedData"]
        self.assertEqual(1, len(bodies))
        self.assertEqual("D0488488-9026-4564-96CC-42DDBA2DA101", bodies[0].name)
        self.assert_cleaned()

    def test_foreign_uid_rejected_before_copying_body(self):
        self.insert(uid="another-fixture-uid-1234567890", filesystem=True)
        with self.assertRaisesRegex(EvidenceError, "UID does not match"):
            self.read()
        self.assertEqual(2, len(self.copies))
        self.assert_cleaned()

    def test_old_response_cannot_be_used_on_device(self):
        self.insert(stamp=self.fixture.NOW - 5, filesystem=True)
        with self.assertRaisesRegex(EvidenceError, "predates"):
            self.read()
        self.assertEqual(2, len(self.copies))
        self.assert_cleaned()

    def test_unsafe_body_path_is_rejected_before_copy(self):
        self.insert(filesystem=True, stored_name="../unrelated-file")
        with self.assertRaisesRegex(EvidenceError, "Unsafe"):
            self.read()
        self.assertEqual(2, len(self.copies))
        self.assert_cleaned()

    def test_preprod_cache_cannot_supply_production_content(self):
        self.fixture.insert()
        with self.assertRaisesRegex(EvidenceError, "No cached Today response"):
            self.read()
        self.assert_cleaned()

    def test_unknown_bundle_is_rejected_before_transport(self):
        with patch("ios_device_evidence.copy_file") as transport:
            with self.assertRaisesRegex(EvidenceError, "Unsupported"):
                read_device_content(self.UDID, "com.unrelated.app", self.fixture.UID,
                                    self.fixture.PROGRAM, self.fixture.boundary)
            transport.assert_not_called()


if __name__ == "__main__":
    unittest.main()
