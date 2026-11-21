"""Offline checks of the account, freshness and request evidence boundaries."""
import base64
import datetime
import email.utils
import json
from pathlib import Path
import plistlib
import sqlite3
import tempfile
import unittest

from read_program_selection_exchange import read_exchange, ROOT
from read_simulator_program_content import CF_EPOCH_SECONDS, EvidenceError


class ProgramSelectionExchangeTests(unittest.TestCase):
    NOW = 1795241000
    UID = 'dedicatedQaIdentity123456789'
    HOST = 'https://coach-preprod-cf87bfd42b85.herokuapp.com'

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.database = Path(self.temporary.name) / 'Cache.db'
        self.db = sqlite3.connect(self.database)
        self.addCleanup(self.db.close)
        self.db.executescript('''
            CREATE TABLE cfurl_cache_response (entry_ID INTEGER, request_key TEXT);
            CREATE TABLE cfurl_cache_blob_data (entry_ID INTEGER, request_object BLOB, response_object BLOB);
            CREATE TABLE cfurl_cache_receiver_data (entry_ID INTEGER, receiver_data BLOB, isDataOnFS INTEGER);
        ''')

    def insert(self, route='main_journey/', payload=None, response=None, uid=None, stamp=None, method='POST', body_slot=None):
        stamp = self.NOW - .25 if stamp is None else stamp
        url = self.HOST + ROOT + route
        claims = {'user_id': self.UID if uid is None else uid, 'exp': self.NOW + 60}
        token = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=')
        items = [0] * 22
        items[1], items[18] = {'_CFURLString': url}, method
        items[19] = {'Authorization': 'FirebaseToken header.' + token + '.signature'}
        items[21] = [] if payload is None else [json.dumps(payload).encode()]
        if body_slot is not None:
            items[21] = body_slot
        request = {'Version': 9, 'Array': items}
        date = email.utils.format_datetime(datetime.datetime.fromtimestamp(stamp, datetime.timezone.utc), usegmt=True)
        metadata = {'Version': 1, 'Array': [{'_CFURLString': url}, stamp - CF_EPOCH_SECONDS, 0, 200, {'Date': date}]}
        response = {'main_journey': {'program_id': 'main', 'private': 'must-not-export'}} if response is None else response
        self.db.execute('INSERT INTO cfurl_cache_response VALUES (?,?)', (1, url))
        self.db.execute('INSERT INTO cfurl_cache_blob_data VALUES (?,?,?)',
                        (1, plistlib.dumps(request), plistlib.dumps(metadata)))
        self.db.execute('INSERT INTO cfurl_cache_receiver_data VALUES (?,?,?)',
                        (1, json.dumps(response).encode(), 0))
        self.db.commit()

    def read(self, route='main_journey/', **changes):
        args = dict(database=self.database, host=self.HOST, uid=self.UID, route=route,
                    not_before_ms=int((self.NOW - 1) * 1000), now=self.NOW)
        args.update(changes)
        return read_exchange(**args)

    def test_current_main_request_and_allowlisted_response(self):
        self.insert(payload={'program_id': 'main'})
        result = self.read()
        self.assertEqual({'program_id': 'main'}, result['request'])
        self.assertEqual({'main_journey': {'program_id': 'main'}}, result['response'])

    def test_cleared_secondary_request_is_not_missing_or_merged(self):
        self.insert(route='secondary_programs/', payload={'program_ids': []}, response={'secondary_programs': []})
        self.assertEqual({'program_ids': []}, self.read('secondary_programs/')['request'])

    def test_complete_requires_all_state_fields(self):
        self.insert(route='onboarding/complete/', response={'active_program_id': 'main'})
        with self.assertRaisesRegex(EvidenceError, 'complete ProgramSelectionState'):
            self.read('onboarding/complete/')

    def test_observed_null_token_is_accepted_only_for_the_bodyless_complete_request(self):
        response = {'main_journey_program_id': 'main', 'secondary_program_ids': [],
                    'active_program_id': 'main', 'onboarding_program_selection_completed': True}
        self.insert(route='onboarding/complete/', response=response, body_slot='__CFURLRequestNullTokenString__')
        result = self.read('onboarding/complete/')
        self.assertIsNone(result['request'])
        self.assertEqual(response, result['response'])

    def test_null_token_cannot_hide_a_missing_main_selection_payload(self):
        self.insert(body_slot='__CFURLRequestNullTokenString__')
        with self.assertRaisesRegex(EvidenceError, 'body segments'):
            self.read()

    def test_activation_id_is_bound_to_the_url_and_the_observed_request_has_no_body(self):
        response = {'main_journey_program_id': 'main', 'secondary_program_ids': ['added'],
                    'active_program_id': 'added', 'onboarding_program_selection_completed': True}
        self.insert(route='added/add_and_activate/', response=response, body_slot='__CFURLRequestNullTokenString__')
        result = self.read('added/add_and_activate/')
        self.assertIsNone(result['request'])
        self.assertEqual(response, result['response'])
        with self.assertRaisesRegex(EvidenceError, 'current UI action'):
            self.read('different/add_and_activate/')

    def test_activation_body_cannot_substitute_an_id_in_the_url(self):
        self.insert(route='added/add_and_activate/', payload={'program_id': 'other'})
        with self.assertRaisesRegex(EvidenceError, 'bodyless'):
            self.read('added/add_and_activate/')

    def test_unsupported_activation_paths_are_rejected_before_cache_access(self):
        for route in ('add_and_activate/', '../added/add_and_activate/', 'added/add_and_activate/?other=true'):
            with self.assertRaisesRegex(EvidenceError, 'supported selection route'):
                self.read(route)

    def test_old_response_cannot_pass_as_current_action(self):
        self.insert(stamp=self.NOW - 10)
        with self.assertRaisesRegex(EvidenceError, 'current UI action'):
            self.read()

    def test_other_account_response_is_rejected(self):
        self.insert(uid='differentQaIdentity123456789')
        with self.assertRaisesRegex(EvidenceError, 'UID'):
            self.read()

    def test_non_post_request_cannot_prove_a_saved_draft(self):
        self.insert(method='GET')
        with self.assertRaisesRegex(EvidenceError, 'method/body'):
            self.read()

    def test_production_and_unknown_routes_are_rejected(self):
        with self.assertRaisesRegex(EvidenceError, 'preprod'):
            self.read(host='https://content.the.coach')
        with self.assertRaisesRegex(EvidenceError, 'supported selection route'):
            self.read(route='../user/')


if __name__ == '__main__':
    unittest.main()
