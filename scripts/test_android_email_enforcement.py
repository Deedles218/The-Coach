"""Validate strict Android evidence parsing without a phone or live credentials."""
import base64
import json
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

from read_android_email_enforcement import active_config, android_account_state, decode_session
import prepare_android_email_enforcement as preparation


class AndroidEmailEvidenceTests(unittest.TestCase):
    def test_active_android_key_not_ios_shaped_flag(self):
        payload = {'configs_key': {'abtest_force_connect_email': '{"isEnabled":true}',
                                  'abtest_force_connect_email_for_her': '{"isEnabled":false}',
                                  'force_connect_email_config': '{"isEnabled":false}'}}
        self.assertIs(active_config(payload, 'male')['enabled'], True)
        self.assertIs(active_config(payload, 'female')['enabled'], False)
        for value in ('{"isEnabled":"true"}', '1', 'null'):
            payload['configs_key']['abtest_force_connect_email'] = value
            with self.assertRaises(ValueError):
                active_config(payload, 'male')

    def token_xml(self, sub=None, expires=None):
        uid = 'testFirebaseIdentity12345678'
        payload = {'user_id': uid, 'sub': sub or uid, 'exp': expires or time.time() + 60}
        token = 'test.' + base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip('=') + '.test'
        root = ET.Element('map')
        item = ET.SubElement(root, 'string', name='com.google.firebase.auth.FIREBASE_USER')
        item.text = json.dumps({'cachedTokenState': json.dumps({'access_token': token}),
                                'userInfos': [json.dumps({'userId': uid})]})
        return ET.tostring(root)

    def test_session_requires_matching_unexpired_identity(self):
        self.assertEqual(decode_session(self.token_xml())[0], 'testFirebaseIdentity12345678')
        for xml, expected in [(self.token_xml(sub='different'), None),
                              (self.token_xml(expires=1), None), (self.token_xml(), 'anotherFixture')]:
            with self.assertRaises(ValueError):
                decode_session(xml, expected)

    def test_distinct_firebase_and_backend_ids_are_both_preserved(self):
        user = {'user_id': 'android-backend-id', 'analytics_profile': {}, 'subscription': {'active': True}}
        progress = {'lessons_completed': 0, 'exercises_completed': 0, 'longest_streak': 0}
        before = android_account_state(user, progress, 'firebase-id', '')
        user['connected_email'] = {'email': 'own@example.test', 'verified': True, 'user_confirmed': True}
        progress['email'] = 'own@example.test'
        after = android_account_state(user, progress, 'firebase-id', 'own@example.test')
        self.assertEqual(before['identity'], after['identity'])
        self.assertNotEqual(after['identity']['firebase'], after['identity']['backend'])
        self.assertTrue(after['emailVerified'])
        self.assertTrue(after['emailMatchesExpected'])
        self.assertNotIn('own@example.test', json.dumps(after))
        self.assertNotIn('android-backend-id', json.dumps(after))
        user['connected_email']['verified'] = 'true'
        with self.assertRaises(ValueError):
            android_account_state(user, progress, 'firebase-id', 'own@example.test')


class AndroidFixtureMutationTests(unittest.TestCase):
    def setUp(self):
        self.state = {'identity': {'firebase': 'digest-one', 'backend': 'digest-two'},
                      'emailPresent': False, 'emailVerified': False,
                      'subscriptionActive': True, 'progress': {'lessons_completed': 0}}
        self.xml = b'<map><boolean name="is_new_user" value="true"/><int name="shown_count" value="0"/><string name="other">keep</string></map>'

    def write_xml(self, *args, **kwargs):
        self.xml = kwargs['input']

    def test_seed_and_restore_only_change_new_user_marker(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(preparation, 'read', return_value=self.state), \
                patch.object(preparation, 'adb', side_effect=lambda *args: self.xml), \
                patch.object(preparation.subprocess, 'run', side_effect=self.write_xml):
            path = Path(folder) / 'seed.json'
            preparation.change('test-device', 'com.vamapps.thecoach', 'seed-existing', path)
            fields = {e.get('name'): (e.get('value'), e.text) for e in ET.fromstring(self.xml)}
            self.assertEqual(fields, {'is_new_user': ('false', None), 'shown_count': ('0', None), 'other': (None, 'keep')})
            preparation.change('test-device', 'com.vamapps.thecoach', 'restore', path)
            self.assertEqual(ET.fromstring(self.xml)[0].get('value'), 'true')

    def test_linked_account_cannot_be_seeded(self):
        self.state['emailPresent'] = True
        with tempfile.TemporaryDirectory() as folder, patch.object(preparation, 'read', return_value=self.state), \
                patch.object(preparation, 'adb', return_value=self.xml), patch.object(preparation.subprocess, 'run') as write:
            with self.assertRaises(ValueError):
                preparation.change('test-device', 'com.vamapps.thecoach', 'seed-existing', Path(folder) / 'seed.json')
            write.assert_not_called()

    def test_different_account_cannot_be_restored(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(preparation, 'read', return_value=self.state), \
                patch.object(preparation, 'adb', return_value=self.xml), patch.object(preparation.subprocess, 'run') as write:
            path = Path(folder) / 'seed.json'
            path.write_text(json.dumps({'originalMarker': 'true', 'baseline': {'identity': 'different'}}))
            with self.assertRaises(ValueError):
                preparation.change('test-device', 'com.vamapps.thecoach', 'restore', path)
            write.assert_not_called()


if __name__ == '__main__':
    unittest.main()
