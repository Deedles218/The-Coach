import hashlib
from contextlib import closing
import io
import json
from pathlib import Path
import sqlite3
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

import read_partner_recipient as evidence

DEVICE = '00008030-000929442E01802E'
UID = 'isolatedRecipientIdentity12345'


class PartnerRecipientEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temporary_files = []

    def copy_device_file(self, args, **kwargs):
        destination = Path(args[args.index('--destination') + 1])
        self.temporary_files.append(destination)
        if args[args.index('--source') + 1] == 'Library/com.amplitude.database':
            with closing(sqlite3.connect(str(destination))) as connection, connection:
                connection.execute('CREATE TABLE store(key TEXT, value TEXT)')
                connection.execute("INSERT INTO store VALUES ('user_id', ?)", (UID,))
        else:
            destination.write_bytes(b'private-test-cache')
        return SimpleNamespace(returncode=0)

    def read_with_state(self, state):
        opener = MagicMock()
        opener.open.return_value = io.StringIO(json.dumps({'userdata': {'subscription': state}}))
        with patch.object(evidence.subprocess, 'run', side_effect=self.copy_device_file), \
                patch.object(evidence, 'cached_headers', return_value={'Authorization': 'FirebaseToken test-only'}) as auth, \
                patch.object(evidence.urllib.request, 'build_opener', return_value=opener):
            result = evidence.read_recipient(DEVICE)
        auth.assert_called_once()
        self.assertEqual(UID, auth.call_args.args[2])
        request = opener.open.call_args.args[0]
        self.assertEqual(evidence.ANDROID_HOST + '/api/v1/user/', request.full_url)
        self.assertEqual('GET', request.get_method())
        self.assertEqual('female', request.get_header('X-gender'))
        return result

    def test_unpaid_is_a_verified_boolean_and_identity_is_hashed(self):
        result = self.read_with_state({'active': False})
        self.assertEqual({'identityHash': hashlib.sha256(UID.encode()).hexdigest(),
                          'identityVerified': True, 'subscriptionActive': False}, result)
        self.assertNotIn(UID, json.dumps(result))
        self.assertNotIn('test-only', json.dumps(result))

    def test_active_membership_is_preserved(self):
        self.assertIs(True, self.read_with_state({'active': True})['subscriptionActive'])

    def test_unknown_membership_never_proves_an_unpaid_recipient(self):
        for state in ({}, {'active': None}, {'active': 'false'}):
            with self.subTest(state=state), self.assertRaisesRegex(RuntimeError, 'unknown'):
                self.read_with_state(state)

    def test_missing_verified_session_stops_before_network(self):
        with patch.object(evidence.subprocess, 'run', side_effect=self.copy_device_file), \
                patch.object(evidence, 'cached_headers', side_effect=RuntimeError('No current session')), \
                patch.object(evidence.urllib.request, 'build_opener') as network:
            with self.assertRaisesRegex(RuntimeError, 'current session'):
                evidence.read_recipient(DEVICE)
        network.assert_not_called()

    def test_invalid_device_is_rejected_before_any_device_read(self):
        with patch.object(evidence.subprocess, 'run') as device:
            with self.assertRaisesRegex(ValueError, 'explicit physical'):
                evidence.read_recipient('default')
        device.assert_not_called()

    def tearDown(self):
        self.assertTrue(all(not path.exists() for path in self.temporary_files),
                        'Temporary authenticated app cache was retained')


if __name__ == '__main__':
    unittest.main()
