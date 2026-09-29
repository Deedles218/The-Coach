"""Reject mismatched identity and misleading Android module evidence offline."""
import base64
import json
import time
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET
import test_module_content as fixtures
import read_android_module_content as reader


class AndroidModuleEvidenceTests(unittest.TestCase):
    def payload(self):
        fixture = fixtures.ModuleContentTests()
        fixture.setUp()
        return fixture.payload

    def test_catalog_for_another_stage_cannot_validate_current_ui(self):
        with self.assertRaisesRegex(reader.EvidenceError, "UI-selected"):
            reader.validate_catalog(self.payload(), "last_longer", 2)

    def test_completed_activity_is_rejected(self):
        payload = self.payload()
        payload["program_section"]["questions"][0]["completed"] = True
        with self.assertRaisesRegex(RuntimeError, "activity state"):
            reader.validate_catalog(payload, "last_longer", 1)

    def test_first_module_incomplete_catalog_is_accepted(self):
        self.assertEqual(1, reader.validate_catalog(self.payload(), "last_longer", 1)["sectionMetadata"]["module_order"])

    def store(self, uid, claim_uid=None, anonymous=False, expires=None):
        claims = {"user_id": claim_uid or uid, "sub": claim_uid or uid,
                  "exp": time.time() + 1000 if expires is None else expires}
        payload = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
        user = {"anonymous": anonymous, "userInfos": [json.dumps({"userId": uid})],
                "cachedTokenState": json.dumps({"access_token": "header." + payload + ".signature"})}
        root = ET.Element("map")
        ET.SubElement(root, "string", name="com.google.firebase.auth.FIREBASE_USER").text = json.dumps(user)
        return ET.tostring(root)

    def session(self, store):
        with patch.object(reader, "adb", side_effect=[b"com.google.firebase.auth.api.Store.fixture.xml\n", store]):
            return reader.session("emulator-5554")

    def test_anonymous_store_cannot_authorize_modules(self):
        with self.assertRaisesRegex(reader.EvidenceError, "anonymous"):
            self.session(self.store("fixtureIdentity123456789", anonymous=True))

    def test_expired_token_cannot_authorize_modules(self):
        with self.assertRaisesRegex(reader.EvidenceError, "expired"):
            self.session(self.store("fixtureIdentity123456789", expires=1))

    def test_store_and_token_uid_must_agree(self):
        with self.assertRaisesRegex(reader.EvidenceError, "inconsistent"):
            self.session(self.store("fixtureIdentity123456789", claim_uid="anotherIdentity123456789"))

    def test_valid_named_session_returns_only_expected_identity_to_caller(self):
        self.assertEqual("fixtureIdentity123456789", self.session(self.store("fixtureIdentity123456789"))[0])

    def test_out_of_scope_program_is_rejected_before_device_or_api_access(self):
        with patch.object(reader, "session") as session:
            with self.assertRaisesRegex(reader.EvidenceError, "scoped"):
                reader.read_content("emulator-5554", "overall_health", 1, int(time.time() * 1000))
            session.assert_not_called()


if __name__ == "__main__":
    unittest.main()
