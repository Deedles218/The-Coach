import base64
from contextlib import closing
import io
import json
from pathlib import Path
import plistlib
import sqlite3
import tempfile
import time
import unittest
from unittest.mock import MagicMock, patch

import read_update_account as evidence

UID = "fixtureIdentity123456789"


def request_blob(uid=UID, expiry=None, host=evidence.ANDROID_HOST, path="/api/v1/daily_program/v2/"):
    claims = {"user_id": uid, "sub": uid, "exp": time.time() + 300 if expiry is None else expiry}
    token = "header." + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=") + ".signature"
    return plistlib.dumps({"Version": 9, "Array": [{"_CFURLString": host + path},
                            {"Authorization": "FirebaseToken " + token, "Unrelated-Secret": "never-forward"}]})


class UpdateAccountEvidenceTests(unittest.TestCase):
    def test_android_uses_installed_version_and_returns_only_allowed_fields(self):
        with patch.object(evidence, "session", return_value=(UID, "token")) as session, \
                patch.object(evidence, "adb", return_value=b"versionName=1.42.1 versionCode=412"), \
                patch.object(evidence, "fetch_user", return_value=True) as fetch:
            self.assertEqual({"uid": UID, "subscriptionActive": True}, evidence.read_android("emulator-5556", UID))
            self.assertEqual("1.42.1", fetch.call_args.args[1]["AppVersion"])
            self.assertEqual("412", fetch.call_args.args[1]["BuildVersion"])
            session.assert_called_once_with("emulator-5556")

    def test_wrong_android_uid_is_rejected_before_network(self):
        with patch.object(evidence, "session", return_value=("other", "token")), \
                patch.object(evidence, "fetch_user") as network:
            with self.assertRaisesRegex(RuntimeError, "identity"):
                evidence.read_android("emulator-5556", UID)
            network.assert_not_called()

    def test_missing_android_build_is_not_success(self):
        with patch.object(evidence, "session", return_value=(UID, "token")), \
                patch.object(evidence, "adb", return_value=b""), patch.object(evidence, "fetch_user") as network:
            with self.assertRaisesRegex(RuntimeError, "metadata"):
                evidence.read_android("emulator-5556", UID)
            network.assert_not_called()

    def test_ios_token_is_uid_bound_and_only_authorization_is_forwarded(self):
        headers = evidence.verified_headers(request_blob(), evidence.ANDROID_HOST, UID, time.time())
        self.assertEqual({"Authorization"}, set(headers))

    def test_wrong_uid_expired_or_boolean_expiry_are_rejected(self):
        for blob in [request_blob(uid="another"), request_blob(expiry=time.time()-1), request_blob(expiry=True)]:
            with self.assertRaisesRegex(RuntimeError, "expired|another"):
                evidence.verified_headers(blob, evidence.ANDROID_HOST, UID, time.time())

    def test_wrong_host_or_state_selecting_route_is_rejected(self):
        for blob in [request_blob(host="https://wrong.example"), request_blob(path="/api/v1/daily_program/")]:
            with self.assertRaisesRegex(RuntimeError, "endpoint"):
                evidence.verified_headers(blob, evidence.ANDROID_HOST, UID, time.time())

    def test_missing_or_string_subscription_cannot_pass(self):
        for state in [{}, {"active": "true"}, {"active": None}]:
            opener = MagicMock()
            opener.open.return_value = io.StringIO(json.dumps({"userdata": {"subscription": state}}))
            with patch.object(evidence.urllib.request, "build_opener", return_value=opener):
                with self.assertRaisesRegex(RuntimeError, "boolean"):
                    evidence.fetch_user(evidence.ANDROID_HOST, {})

    def test_false_subscription_is_observed_without_being_promoted_to_active(self):
        opener = MagicMock()
        opener.open.return_value = io.StringIO('{"userdata":{"subscription":{"active":false}}}')
        with patch.object(evidence.urllib.request, "build_opener", return_value=opener):
            self.assertIs(False, evidence.fetch_user(evidence.ANDROID_HOST, {}))
        self.assertEqual("GET", opener.open.call_args.args[0].get_method())
        self.assertEqual(evidence.ANDROID_HOST + "/api/v1/user/", opener.open.call_args.args[0].full_url)

    def test_cache_is_read_only_and_falls_back_only_to_valid_same_account_session(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "Cache.db"
            with closing(sqlite3.connect(path)) as connection, connection:
                connection.execute("CREATE TABLE cfurl_cache_response(entry_ID INTEGER, request_key TEXT)")
                connection.execute("CREATE TABLE cfurl_cache_blob_data(entry_ID INTEGER, request_object BLOB)")
                for index, blob in enumerate([request_blob(), request_blob(uid="other")], 1):
                    connection.execute("INSERT INTO cfurl_cache_response VALUES (?,?)", (index, evidence.ANDROID_HOST + "/api/v1/user/"))
                    connection.execute("INSERT INTO cfurl_cache_blob_data VALUES (?,?)", (index, blob))
            before = path.read_bytes()
            self.assertEqual({"Authorization"}, set(evidence.cached_headers(path, evidence.ANDROID_HOST, UID)))
            self.assertEqual(before, path.read_bytes())
            with self.assertRaisesRegex(RuntimeError, "No valid"):
                evidence.cached_headers(path, evidence.ANDROID_HOST, "wrong")

    def test_ios_current_identity_must_match_before_api_read(self):
        with tempfile.TemporaryDirectory() as folder:
            data = Path(folder)
            (data / "Library").mkdir()
            with closing(sqlite3.connect(data / "Library/com.amplitude.database")) as connection, connection:
                connection.execute("CREATE TABLE store(key TEXT, value TEXT)")
                connection.execute("INSERT INTO store VALUES ('user_id', 'wrong')")
            with patch.object(evidence, "sim_container", return_value=data), patch.object(evidence, "fetch_user") as network:
                with self.assertRaisesRegex(RuntimeError, "differs"):
                    evidence.read_ios("simulator", "com.vamapps.The-Coach", UID)
                network.assert_not_called()

    def test_ios_reads_current_account_and_installed_build_without_writing_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            data = Path(folder) / "data"
            app = Path(folder) / "TheCoach.app"
            (data / "Library").mkdir(parents=True)
            app.mkdir()
            identity = data / "Library/com.amplitude.database"
            with closing(sqlite3.connect(identity)) as connection, connection:
                connection.execute("CREATE TABLE store(key TEXT, value TEXT)")
                connection.execute("INSERT INTO store VALUES ('user_id', ?)", (UID,))
            info = {"CFBundleIdentifier": "com.vamapps.The-Coach",
                    "CFBundleShortVersionString": "1.2.3", "CFBundleVersion": "456"}
            (app / "Info.plist").write_bytes(plistlib.dumps(info))
            before = identity.read_bytes()
            with patch.object(evidence, "sim_container", side_effect=[data, app]), \
                    patch.object(evidence, "cached_headers", return_value={"Authorization": "fixture-token"}), \
                    patch.object(evidence, "fetch_user", return_value=True) as network:
                self.assertEqual({"uid": UID, "subscriptionActive": True},
                                 evidence.read_ios("simulator", "com.vamapps.The-Coach", UID))
                self.assertEqual(evidence.ANDROID_HOST, network.call_args.args[0])
                self.assertEqual("1.2.3", network.call_args.args[1]["AppVersion"])
                self.assertEqual("456", network.call_args.args[1]["BuildVersion"])
            self.assertEqual(before, identity.read_bytes())


if __name__ == "__main__":
    unittest.main()
