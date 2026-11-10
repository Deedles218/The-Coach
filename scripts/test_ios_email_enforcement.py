import json
from pathlib import Path
import plistlib
import sqlite3
import tempfile
import unittest
import zipfile

import read_ios_email_enforcement as evidence

UID = 'dedicatedEmailFixture12345'
BUNDLE = 'com.vamapps.preprod.The-Coach'


class EmailEvidenceTests(unittest.TestCase):
    def state(self, connected=None):
        return {'user_id': 'ios_' + UID, 'connected_email': connected,
                'analytics_profile': {}, 'subscription': {'active': False}}

    def progress(self, email=None):
        return {'lessons_completed': 0, 'exercises_completed': 2, 'longest_streak': 1, 'email': email}

    def test_anonymous_null_is_unlinked_and_output_contains_no_credentials(self):
        state = evidence.account_state(self.state(), self.progress(), UID, 'free@example.test')
        self.assertFalse(state['emailPresent'])
        self.assertFalse(state['emailVerified'])
        self.assertEqual(2, state['progress']['exercises_completed'])
        self.assertNotIn(UID, json.dumps(state))
        self.assertNotIn('free@example.test', json.dumps(state))
        omitted = self.state()
        del omitted['connected_email']
        self.assertFalse(evidence.account_state(omitted, self.progress(), UID, '')['emailPresent'])

    def test_pending_email_is_not_mistaken_for_confirmation(self):
        connected = {'email': 'free@example.test', 'verified': True, 'user_confirmed': False}
        state = evidence.account_state(self.state(connected), self.progress(connected['email']), UID, connected['email'])
        self.assertTrue(state['emailMatchesExpected'])
        self.assertFalse(state['emailVerified'])
        connected['user_confirmed'] = True
        self.assertTrue(evidence.account_state(self.state(connected), self.progress(connected['email']), UID,
                                             connected['email'])['emailVerified'])

    def test_wrong_identity_missing_progress_and_unknown_email_cannot_pass(self):
        with self.assertRaisesRegex(ValueError, 'account differs'):
            evidence.account_state(self.state(), self.progress(), 'anotherFixture123456789', '')
        with self.assertRaisesRegex(ValueError, 'progress is missing'):
            evidence.account_state(self.state(), {}, UID, '')
        with self.assertRaisesRegex(ValueError, 'state is unavailable'):
            evidence.account_state({'user_id': UID}, self.progress(), UID, '')
        with self.assertRaisesRegex(ValueError, 'confirmation state is unknown'):
            evidence.account_state(self.state({'email': '', 'verified': 'false', 'user_confirmed': False}),
                                   self.progress(), UID, '')

    def test_config_uses_the_requested_app_variant_and_rejects_bad_thresholds(self):
        with tempfile.TemporaryDirectory() as temporary:
            database = Path(temporary) / 'config.db'
            with sqlite3.connect(database) as connection:
                connection.execute('CREATE TABLE main_active(key TEXT, value BLOB)')
                connection.executemany('INSERT INTO main_active VALUES (?, ?)', [
                    ('force_connect_email_config', json.dumps({'isEnabled': True, 'launchesBeforeShow': 3})),
                    ('force_connect_email_config_for_her', json.dumps({'isEnabled': False, 'launchesBeforeShow': 5})),
                    ('abtest_enter_email_after_subscription', json.dumps({'isEnabled': False})),
                    ('abtest_enter_email_after_subscription_for_her', json.dumps({'isEnabled': True}))])
            before = database.read_bytes()
            self.assertTrue(evidence.active_config(database, 'male')['enabled'])
            self.assertFalse(evidence.active_config(database, 'female')['enabled'])
            self.assertEqual(5, evidence.active_config(database, 'female')['launchesBeforeShow'])
            self.assertFalse(evidence.active_config(database, 'male')['legacyEnabled'])
            self.assertTrue(evidence.active_config(database, 'female')['legacyEnabled'])
            self.assertEqual(before, database.read_bytes())
            with sqlite3.connect(database) as connection:
                connection.execute('UPDATE main_active SET value=?', (json.dumps({'isEnabled': True, 'launchesBeforeShow': True}),))
            with self.assertRaises(ValueError): evidence.active_config(database, 'male')

    def test_ipa_checks_top_level_app_bundle_and_numeric_build(self):
        info = {'CFBundleIdentifier': BUNDLE, 'CFBundleShortVersionString': '1.13.34',
                'CFBundleVersion': '3575', 'DTPlatformName': 'iphoneos'}
        with tempfile.TemporaryDirectory() as temporary:
            ipa = Path(temporary) / 'update.ipa'
            with zipfile.ZipFile(ipa, 'w') as archive:
                archive.writestr('Payload/Coach.app/Info.plist', plistlib.dumps(info))
            self.assertEqual('3575', evidence.artifact_build(ipa, BUNDLE)['build'])
            with self.assertRaises(ValueError): evidence.artifact_build(ipa, BUNDLE + '-for-her')
        info['CFBundleVersion'] = 'BITRISE_BUILD_NUMBER'
        with self.assertRaises(ValueError): evidence.build_metadata(info, BUNDLE)


if __name__ == '__main__':
    unittest.main()
