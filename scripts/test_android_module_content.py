"""Reject mismatched identity and misleading Android module evidence offline."""
import base64
import json
import time
import unittest
import os
import runpy
import subprocess
import sys
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


class DebugAndroidIdentityTests(unittest.TestCase):
    def setUp(self):
        self.environment = patch.dict(os.environ, {
            'COACH_ANDROID_EVIDENCE_PACKAGE': 'com.vamapps.thecoach.forher',
            'COACH_ANDROID_EVIDENCE_APK_SHA256': 'a' * 64,
            'COACH_EXPECTED_PROGRAM_UID': 'fixtureIdentity123456789'})
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def token(self, provider='custom'):
        payload = base64.urlsafe_b64encode(json.dumps({'firebase': {'sign_in_provider': provider}}).encode()).decode().rstrip('=')
        return 'header.' + payload + '.signature'

    def test_unknown_package_fails_before_device_access(self):
        os.environ['COACH_ANDROID_EVIDENCE_PACKAGE'] = 'another.app'
        with patch.object(reader, 'adb') as adb:
            with self.assertRaisesRegex(reader.EvidenceError, 'package'):
                reader.session('SamsungUsb')
            adb.assert_not_called()

    def test_missing_apk_binding_fails_before_device_access(self):
        os.environ.pop('COACH_ANDROID_EVIDENCE_APK_SHA256')
        with patch.object(reader, 'adb') as adb:
            with self.assertRaisesRegex(reader.EvidenceError, 'SHA-256'):
                reader.session('SamsungUsb')
            adb.assert_not_called()

    def observe(self, sha=None, provider='custom', anonymous=False):
        import read_android_email_enforcement as debug
        with patch.object(debug, 'installed_build') as build, \
             patch.object(debug, 'session', return_value=('fixtureIdentity123456789', self.token(provider))) as sdk, \
             patch.object(reader, 'adb', side_effect=[b'package:/data/app/test/base.apk\n', ((sha or 'a' * 64) + ' base.apk').encode()]):
            result = reader.session('SamsungUsb', allow_anonymous=anonymous)
            build.assert_called_once_with('SamsungUsb', 'com.vamapps.thecoach.forher')
            sdk.assert_called_once_with('SamsungUsb', 'com.vamapps.thecoach.forher', 'fixtureIdentity123456789')
            return result

    def test_debug_session_uses_requested_package_and_bound_uid(self):
        self.assertEqual('fixtureIdentity123456789', self.observe()[0])

    def test_another_installed_apk_is_rejected(self):
        with self.assertRaisesRegex(reader.EvidenceError, 'artifact'):
            self.observe(sha='b' * 64)

    def test_debug_anonymous_session_does_not_authorize_named_module_fixture(self):
        with self.assertRaisesRegex(reader.EvidenceError, 'anonymous'):
            self.observe(provider='anonymous')

    def test_explicit_identity_observation_can_allow_anonymous(self):
        self.assertEqual('fixtureIdentity123456789', self.observe(provider='anonymous', anonymous=True)[0])

    def test_debug_mode_cannot_use_production_host(self):
        self.assertEqual('https://coach-preprod-cf87bfd42b85.herokuapp.com', reader.api_host())

    def test_legacy_mode_keeps_production_host(self):
        os.environ.pop('COACH_ANDROID_EVIDENCE_PACKAGE')
        self.assertEqual('https://content.the.coach', reader.api_host())

    def test_android_debug_import_preserves_production_host_for_ios_readers(self):
        isolated = runpy.run_path(reader.__file__, run_name='host_contract')
        self.assertEqual('https://content.the.coach', isolated['HOST'])
        self.assertEqual('https://coach-preprod-cf87bfd42b85.herokuapp.com', isolated['api_host']())

    def test_production_ios_readers_keep_their_backend_with_android_debug_enabled(self):
        program = (
            "import read_ios_email_enforcement as email; import read_update_account as update; "
            "assert email.BUNDLES['com.vamapps.The-Coach'][0] == 'https://content.the.coach'; "
            "assert email.BUNDLES['com.vamapps.The-Coach-for-her'][0] == 'https://content.the.coach'; "
            "assert update.IOS_HOSTS['com.vamapps.The-Coach'] == 'https://content.the.coach'"
        )
        result = subprocess.run([sys.executable, '-B', '-c', program], cwd=os.path.dirname(reader.__file__),
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)


if __name__ == "__main__":
    unittest.main()
