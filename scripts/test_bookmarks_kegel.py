"""Current typed Kegel evidence must agree with UI, identity and installed build."""
import copy
import io
import json
import os
from pathlib import Path
import sqlite3
import time
import unittest
from unittest.mock import MagicMock, patch

import read_bookmarks_kegel as reader

UID = 'fixtureIdentity123456789'


class BookmarksKegelEvidenceTests(unittest.TestCase):
    def setUp(self):
        environment = patch.dict(os.environ, {'COACH_EXPECTED_ACCOUNT_UID': UID,
                                               'COACH_BOOKMARKS_KEGEL_EXPECTED_EMAIL': 'qa@example.invalid'})
        environment.start()
        self.addCleanup(environment.stop)
        self.payload = {
            'program_cover': {'program_id': 'kegel_challenge'},
            'program_section': {'section_id': 'kegel_challenge', 'headline': 'Kegel Challenge',
                'day_in_program': 4, 'total_days': 20, 'module_program': True,
                'module_current_day': 2, 'module_total_days': 9, 'module_order': 2,
                'questions': [{'name': 'workout-card', 'headline': 'Workout',
                               'link_type': {'kegel': {'workout_id': 'classic'}}}]}}
        self.user = {'userdata': {'user_id': 'ios_' + UID,
                     'analytics_profile': {'email': 'QA@example.invalid'}, 'subscription': {'active': True}}}
        self.args = dict(platform='ios', device='01234567-0123456789ABCDEF', app='com.vamapps.The-Coach',
                         program='kegel_challenge', day=4, card_id='workout-card', title='Workout',
                         link_type='kegel', day_label='Stage 2 of 9', version='1.15.0', build='3597',
                         module_order=2,
                         boundary=int(time.time() * 1000))

    def validate(self):
        return reader.validate_card(self.payload, 'kegel_challenge', 4, 'workout-card', 'Workout',
                                    'kegel', 'Stage 2 of 9', 2)

    def context(self, uid=UID, build='3597'):
        return uid, 'https://content.the.coach', 'male', {'version': '1.15.0', 'build': build}, {'Authorization': 'fixture-token'}

    def network(self):
        opener = MagicMock()
        opener.open.side_effect = [io.StringIO(json.dumps(self.user)), io.StringIO(json.dumps(self.payload))]
        return opener

    def test_current_catalog_proves_only_the_selected_typed_card(self):
        opener = self.network()
        with patch.object(reader, 'ios_context', return_value=self.context()), \
                patch.object(reader.urllib.request, 'build_opener', return_value=opener):
            result = reader.read(**self.args)
        self.assertTrue(result['identityVerified'])
        self.assertEqual('workout-card', result['id'])
        self.assertEqual('3597', result['build'])
        self.assertNotIn(UID, json.dumps(result))
        self.assertNotIn('fixture-token', json.dumps(result))
        requests = [call.args[0] for call in opener.open.call_args_list]
        self.assertEqual(['GET', 'GET'], [request.get_method() for request in requests])
        self.assertEqual('https://content.the.coach/api/v1/coachprogram/catalog/v3/kegel_challenge/?day_of_program=4', requests[1].full_url)
        self.assertEqual('3597', requests[1].get_header('Buildversion'))

    def test_different_current_account_fails_before_network(self):
        with patch.object(reader, 'ios_context', return_value=self.context(uid='anotherIdentity123456789')), \
                patch.object(reader.urllib.request, 'build_opener') as network:
            with self.assertRaisesRegex(ValueError, 'account/build'):
                reader.read(**self.args)
            network.assert_not_called()

    def test_different_installed_build_fails_before_network(self):
        with patch.object(reader, 'ios_context', return_value=self.context(build='3598')), \
                patch.object(reader.urllib.request, 'build_opener') as network:
            with self.assertRaisesRegex(ValueError, 'account/build'):
                reader.read(**self.args)
            network.assert_not_called()

    def test_stale_ui_action_cannot_use_a_fresh_catalog(self):
        self.args['boundary'] -= 120001
        with patch.object(reader, 'ios_context') as context:
            with self.assertRaisesRegex(ValueError, 'stale'):
                reader.read(**self.args)
            context.assert_not_called()

    def test_wrong_api_identity_cannot_prove_the_fixture(self):
        self.user['userdata']['user_id'] = 'ios_other'
        with patch.object(reader, 'ios_context', return_value=self.context()), \
                patch.object(reader.urllib.request, 'build_opener', return_value=self.network()):
            with self.assertRaisesRegex(ValueError, 'another account'):
                reader.read(**self.args)

    def test_another_email_or_inactive_subscription_cannot_prove_the_fixture(self):
        original = copy.deepcopy(self.user)
        for field in ('email', 'subscription'):
            self.user = copy.deepcopy(original)
            if field == 'email': self.user['userdata']['analytics_profile']['email'] = 'other@example.invalid'
            else: self.user['userdata']['subscription']['active'] = False
            with patch.object(reader, 'ios_context', return_value=self.context()), \
                    patch.object(reader.urllib.request, 'build_opener', return_value=self.network()):
                with self.assertRaisesRegex(ValueError, 'Premium fixture'):
                    reader.read(**self.args)

    def test_same_label_with_another_card_id_is_rejected(self):
        self.payload['program_section']['questions'][0]['name'] = 'different-card'
        with self.assertRaisesRegex(ValueError, 'title/ID'):
            self.validate()

    def test_same_card_on_another_global_day_is_rejected(self):
        self.payload['program_section']['day_in_program'] = 5
        with self.assertRaisesRegex(ValueError, 'program day'):
            self.validate()

    def test_another_visible_stage_is_rejected(self):
        self.payload['program_section']['module_current_day'] = 3
        with self.assertRaisesRegex(ValueError, 'UI day label'):
            self.validate()

    def test_the_same_stage_number_in_another_module_is_rejected(self):
        self.payload['program_section']['module_order'] = 3
        with self.assertRaisesRegex(ValueError, 'another UI module'):
            self.validate()

    def test_modern_workout_cannot_certify_a_legacy_type(self):
        self.payload['program_section']['questions'][0]['link_type'] = {'kegel_workout': {'id': 'modern'}}
        with self.assertRaisesRegex(ValueError, 'wrong workout type'):
            self.validate()

    def test_conflicting_types_cannot_certify_a_legacy_type(self):
        self.payload['program_section']['questions'][0]['link_type']['kegel_workout'] = {'id': 'modern'}
        with self.assertRaisesRegex(ValueError, 'ambiguous workout'):
            self.validate()

    def test_duplicate_visible_titles_are_not_an_identity_match(self):
        self.payload['program_section']['questions'].append({'name': 'twin', 'headline': 'Workout'})
        with self.assertRaisesRegex(ValueError, 'title/ID'):
            self.validate()

    def test_debug_package_mismatch_fails_before_device_access(self):
        with patch.dict(os.environ, {'COACH_ANDROID_EVIDENCE_PACKAGE': 'com.vamapps.thecoach.forher'}), \
                patch.object(reader, 'session') as session:
            with self.assertRaisesRegex(ValueError, 'differs from the UI app'):
                reader.android_context('qa_usb', 'com.vamapps.thecoach')
            session.assert_not_called()

    def test_wrong_ios_sdk_identity_fails_before_session_cache_is_read(self):
        def copy(device, bundle, relative, destination):
            self.assertEqual('Library/com.amplitude.database', relative)
            with sqlite3.connect(destination) as db:
                db.execute('CREATE TABLE store(key TEXT, value TEXT)')
                db.execute("INSERT INTO store VALUES ('user_id', 'wrong')")
        with patch.object(reader, 'installed_build', return_value={'version': '1.15.0', 'build': '3597'}), \
                patch.object(reader, 'copy_data', side_effect=copy), patch.object(reader, 'cached_headers') as headers:
            with self.assertRaisesRegex(ValueError, 'another account'):
                reader.ios_context(self.args['device'], self.args['app'])
            headers.assert_not_called()


if __name__ == '__main__':
    unittest.main()
