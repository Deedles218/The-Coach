"""Offline contract and stale/foreign-cache rejection tests; no simulator/network."""
import base64
import copy
import datetime
import email.utils
import json
from pathlib import Path
import plistlib
import sqlite3
import tempfile
import unittest

from read_simulator_program_content import (CF_EPOCH_SECONDS, EvidenceError, HOST, ROUTE,
                                            read_content)


class SimulatorProgramContentTests(unittest.TestCase):
    NOW = 1788758000.8
    UID = "fixture-account-uid-1234567890"
    PROGRAM = "overall_health"

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.database = Path(self.temp.name) / "Cache.db"
        self.connection = sqlite3.connect(self.database)
        self.addCleanup(self.connection.close)
        self.connection.executescript("""
            CREATE TABLE cfurl_cache_response (entry_ID INTEGER, request_key TEXT, time_stamp TEXT);
            CREATE TABLE cfurl_cache_blob_data (entry_ID INTEGER, request_object BLOB, response_object BLOB);
            CREATE TABLE cfurl_cache_receiver_data (entry_ID INTEGER, receiver_data BLOB, isDataOnFS INTEGER);
        """)
        self.payload = {"program_cover": {"program_id": self.PROGRAM},
                        "program_section": {"section_id": self.PROGRAM, "headline": "Overall Health", "day_in_program": 1,
                                            "total_days": 70, "questions": [
                                                {"name": "lesson_health", "headline": "Your Health",
                                                 "completed": False, "private": "must-not-be-exported"}]},
                        "userdata": {"email": "must-not-be-exported"}}
        self.boundary = int((self.NOW - 1) * 1000)

    def insert(self, entry=1, uid=None, expiry=None, stamp=None, payload=None, filesystem=False,
               date_offset=0, version=1, stored_name=None, status=200, url=None):
        stamp = self.NOW - 0.3 if stamp is None else stamp
        url = "https://" + HOST + ROUTE + "?date=2026-09-07" if url is None else url
        claims = {"user_id": self.UID if uid is None else uid,
                  "exp": self.NOW + 60 if expiry is None else expiry}
        encoded = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
        request = {"Version": 9, "Array": [{"_CFURLString": url},
                   {"Authorization": "FirebaseToken header." + encoded + ".signature"}]}
        http_date = email.utils.format_datetime(datetime.datetime.fromtimestamp(
            stamp + date_offset, datetime.timezone.utc), usegmt=True)
        response = {"Version": version, "Array": [{"_CFURLString": url}, stamp - CF_EPOCH_SECONDS,
                                                  0, status, {"Date": http_date}]}
        body = json.dumps(self.payload if payload is None else payload).encode()
        if filesystem:
            name = stored_name or "D0488488-9026-4564-96CC-42DDBA2DA101"
            if stored_name is None:
                folder = self.database.parent / "fsCachedData"
                folder.mkdir(exist_ok=True)
                (folder / name).write_bytes(body)
            body = name.encode()
        self.connection.execute("INSERT INTO cfurl_cache_response VALUES (?,?,?)",
                                (entry, url, "2001-01-01 00:00:00"))
        self.connection.execute("INSERT INTO cfurl_cache_blob_data VALUES (?,?,?)",
                                (entry, plistlib.dumps(request), plistlib.dumps(response)))
        self.connection.execute("INSERT INTO cfurl_cache_receiver_data VALUES (?,?,?)",
                                (entry, body, int(filesystem)))
        self.connection.commit()

    def read(self, **changes):
        values = {"database": self.database, "expected_uid": self.UID,
                  "expected_program": self.PROGRAM, "not_before_ms": self.boundary, "now": self.NOW}
        values.update(changes)
        return read_content(**values)

    def test_exports_only_program_oracle_fields_from_inline_body(self):
        self.insert()
        self.assertEqual({"programId": self.PROGRAM, "headline": "Overall Health", "day": 1, "totalDays": 70,
                          "sectionMetadata": {}, "cover": {"program_id": self.PROGRAM},
                          "questions": [{"id": "lesson_health", "headline": "Your Health"}]}, self.read())

    def test_reads_filesystem_body_without_touching_cache_or_body(self):
        self.insert(filesystem=True)
        paths = list(self.database.parent.rglob("*"))
        before = {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in paths if p.is_file()}
        self.assertEqual(self.PROGRAM, self.read()["programId"])
        self.assertEqual(before, {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in before})

    def test_rejects_other_uid(self):
        self.insert(uid="another-fixture-uid-1234567890")
        with self.assertRaisesRegex(EvidenceError, "UID does not match"):
            self.read()

    def test_rejects_newer_other_uid_instead_of_using_older_matching_request(self):
        self.insert(stamp=self.NOW - 0.7)
        self.insert(entry=2, uid="another-fixture-uid-1234567890", stamp=self.NOW - 0.1)
        with self.assertRaisesRegex(EvidenceError, "UID does not match"):
            self.read()

    def test_rejects_expired_request_jwt(self):
        self.insert(expiry=self.NOW - 1)
        with self.assertRaisesRegex(EvidenceError, "expired"):
            self.read()

    def test_rejects_prior_action_response_even_with_current_row_metadata(self):
        self.insert(stamp=self.NOW - 5)
        self.connection.execute("UPDATE cfurl_cache_response SET time_stamp='2026-09-07 05:13:20'")
        self.connection.commit()
        with self.assertRaisesRegex(EvidenceError, "predates"):
            self.read()

    def test_uses_fractional_response_time_when_http_date_has_same_second(self):
        self.insert(stamp=self.NOW - 0.1)
        self.assertEqual(self.PROGRAM, self.read(not_before_ms=int((self.NOW - 0.2) * 1000))["programId"])

    def test_rejects_unknown_response_serialization(self):
        self.insert(version=2)
        with self.assertRaisesRegex(EvidenceError, "metadata version"):
            self.read()

    def test_rejects_unknown_request_serialization(self):
        self.insert()
        data = plistlib.loads(self.connection.execute(
            "SELECT request_object FROM cfurl_cache_blob_data").fetchone()[0])
        data["Version"] = 1
        self.connection.execute("UPDATE cfurl_cache_blob_data SET request_object=?", (plistlib.dumps(data),))
        self.connection.commit()
        with self.assertRaisesRegex(EvidenceError, "request metadata version"):
            self.read()

    def test_rejects_disagreeing_http_date(self):
        self.insert(date_offset=30)
        with self.assertRaisesRegex(EvidenceError, "cannot be corroborated"):
            self.read()

    def test_rejects_newer_wrong_program_instead_of_falling_back(self):
        self.insert(stamp=self.NOW - 0.7)
        other = copy.deepcopy(self.payload)
        other["program_cover"]["program_id"] = "last_longer"
        other["program_section"]["section_id"] = "last_longer"
        self.insert(entry=2, stamp=self.NOW - 0.1, payload=other)
        with self.assertRaisesRegex(EvidenceError, "expected program"):
            self.read()

    def test_rejects_disagreeing_cover_and_section(self):
        self.payload["program_section"]["section_id"] = "last_longer"
        self.insert()
        with self.assertRaisesRegex(EvidenceError, "expected program"):
            self.read()

    def test_rejects_unsafe_filesystem_path(self):
        self.insert(filesystem=True, stored_name="../private-file")
        with self.assertRaisesRegex(EvidenceError, "Unsafe"):
            self.read()

    def test_rejects_filesystem_directory_symlink(self):
        self.insert(filesystem=True)
        folder = self.database.parent / "fsCachedData"
        actual = self.database.parent / "moved-body-directory"
        folder.rename(actual)
        folder.symlink_to(actual, target_is_directory=True)
        with self.assertRaisesRegex(EvidenceError, "directory is unsafe"):
            self.read()

    def test_rejects_duplicate_question_ids(self):
        self.payload["program_section"]["questions"] *= 2
        self.insert()
        with self.assertRaisesRegex(EvidenceError, "duplicated"):
            self.read()

    def test_exports_only_whitelisted_scalar_display_metadata(self):
        self.payload["program_section"].update(module_name="Stage 1", module_current_day=1,
                                              module_total_days=4, private_identity="not exported")
        self.payload["program_cover"].update(program_name="OH", program_text_2="0% completed",
                                            URL_for_i="not exported")
        self.insert()
        result = self.read()
        self.assertEqual({"module_name": "Stage 1", "module_current_day": 1, "module_total_days": 4},
                         result["sectionMetadata"])
        self.assertEqual({"program_id": self.PROGRAM, "program_name": "OH", "program_text_2": "0% completed"},
                         result["cover"])

    def test_rejects_missing_or_future_action_boundary(self):
        self.insert()
        for boundary in [0, int((self.NOW + 1) * 1000)]:
            with self.subTest(boundary=boundary), self.assertRaises(EvidenceError):
                self.read(not_before_ms=boundary)

    def test_rejects_response_without_http_date(self):
        self.insert()
        data = plistlib.loads(self.connection.execute(
            "SELECT response_object FROM cfurl_cache_blob_data").fetchone()[0])
        data["Array"][4] = {}
        self.connection.execute("UPDATE cfurl_cache_blob_data SET response_object=?", (plistlib.dumps(data),))
        self.connection.commit()
        with self.assertRaisesRegex(EvidenceError, "HTTP Date"):
            self.read()


if __name__ == "__main__":
    unittest.main()
