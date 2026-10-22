import base64
import email.utils
import json
import plistlib
from pathlib import Path
import sqlite3
import tempfile
import unittest
from unittest.mock import patch

from observe_bookmarks_evidence import AnalyticsReader, CF_EPOCH, current_user, fresh_event, fresh_request, read_requests


class BookmarksEvidenceTests(unittest.TestCase):
    def request(self, uid='fixture-user', stamp=2000, host='https://content.the.coach', day=2):
        encode = lambda value: base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip('=')
        token = encode({'alg': 'none'}) + '.' + encode({'user_id': uid, 'sub': uid, 'exp': 3000}) + '.signature'
        items = [None] * 22
        items[1] = {'_CFURLString': host + '/api/v1/coachprogram/lessons/lesson-a/bookmark'}
        items[18] = 'POST'
        items[19] = {'Authorization': 'FirebaseToken ' + token, 'Sensitive': 'never-export'}
        items[21] = [json.dumps({'program_id': 'program-a', 'program_day': day, 'slide_number': 3}).encode()]
        response = [None, stamp - CF_EPOCH, None, 200,
                    {'Date': email.utils.formatdate(stamp, usegmt=True)}]
        return (plistlib.dumps({'Version': 9, 'Array': items}, fmt=plistlib.FMT_BINARY),
                plistlib.dumps({'Version': 1, 'Array': response}, fmt=plistlib.FMT_BINARY))

    def test_fresh_request_exports_only_verified_noncredential_fields(self):
        request, response = self.request()
        result = fresh_request(request, response, 'ios_fixture-user', 1999, 2001, 'https://content.the.coach')
        self.assertEqual({'program_id': 'program-a', 'program_day': 2, 'slide_number': 3}, result['body'])
        self.assertNotIn('Authorization', json.dumps(result))
        self.assertNotIn('never-export', json.dumps(result))

    def test_old_or_other_account_request_is_rejected(self):
        request, response = self.request()
        self.assertIsNone(fresh_request(request, response, 'another-user', 1999, 2001, 'https://content.the.coach'))
        self.assertIsNone(fresh_request(request, response, 'fixture-user', 2001, 2002, 'https://content.the.coach'))

    def test_unrelated_host_and_boolean_day_are_rejected(self):
        request, response = self.request(host='https://unrelated.example')
        self.assertIsNone(fresh_request(request, response, 'fixture-user', 1999, 2001, 'https://content.the.coach'))
        request, response = self.request(day=True)
        self.assertIsNone(fresh_request(request, response, 'fixture-user', 1999, 2001, 'https://content.the.coach'))

    def test_only_fresh_current_account_bookmark_events_are_exported(self):
        event = {'uuid': 'event-one', 'event_type': 'BookmarkSaved', 'user_id': 'fixture-user',
                 'timestamp': 2000, 'user_properties': {'private': 'never-export'},
                 'event_properties': {'lesson_id': 'lesson-a', 'slide_number': 2, 'email': 'never-export'}}
        payload = json.dumps(event)
        self.assertIsNone(fresh_event(payload, 'other-user', 1999))
        self.assertIsNone(fresh_event(payload, 'fixture-user', 2001))
        result = fresh_event(payload, 'fixture-user', 1999)
        self.assertEqual({'lesson_id': 'lesson-a', 'slide_number': 2}, result['properties'])
        self.assertNotIn('never-export', json.dumps(result))

    def sdk_database(self, root, event_id):
        database = root / 'Library/com.amplitude.database'
        database.parent.mkdir(parents=True)
        with sqlite3.connect(database) as connection:
            connection.execute('CREATE TABLE store (key TEXT, value TEXT)')
            connection.execute('CREATE TABLE events (event TEXT)')
            connection.execute("INSERT INTO store VALUES ('user_id', 'fixture-user')")
            connection.execute('INSERT INTO events VALUES (?)', (json.dumps({
                'uuid': event_id, 'event_type': 'BookmarkSaved', 'user_id': 'fixture-user',
                'timestamp': 2000, 'event_properties': {'lesson_id': 'lesson-a', 'slide_number': 2}}),))
        return database

    def test_sdk_events_do_not_depend_on_url_cache_being_present(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            database = self.sdk_database(root, 'first-event')
            reader = AnalyticsReader()
            try:
                first = reader.poll(root, 1.9)
                self.assertEqual(1, len(first))
                with sqlite3.connect(database) as connection:
                    connection.execute('UPDATE events SET event=?', (json.dumps({
                        'uuid': 'second-event', 'event_type': 'BookmarkDeleted', 'user_id': 'fixture-user',
                        'timestamp': 2001, 'event_properties': {'lesson_id': 'lesson-a', 'slide_number': 2}}),))
                self.assertEqual('BookmarkDeleted', reader.poll(root, 1.9)[0]['event'])
            finally:
                reader.close()

    def test_reinstall_follows_new_container_even_when_old_directory_remains(self):
        with tempfile.TemporaryDirectory() as folder:
            old, new = Path(folder) / 'old', Path(folder) / 'new'
            self.sdk_database(old, 'old-event')
            self.sdk_database(new, 'new-event')
            reader = AnalyticsReader()
            try:
                before = reader.poll(old, 1.9)
                after = reader.poll(new, 1.9)
                self.assertTrue(old.exists())
                self.assertNotEqual(before[0]['fingerprint'], after[0]['fingerprint'])
            finally:
                reader.close()

    def cache_user(self, root, stamp=2000, identity='ios_fixture-user', email='fixture@example.test',
                   route='/api/v1/user/'):
        cache = root / 'Library/Caches/com.vamapps.The-Coach'
        cache.mkdir(parents=True, exist_ok=True)
        request, response = self.request(stamp=stamp)
        req = plistlib.loads(request)
        req['Array'][1]['_CFURLString'] = 'https://content.the.coach' + route
        req['Array'][18] = 'GET'
        payload = ({'userdata': {'user_id': identity, 'analytics_profile': {'email': email}}}
                   if route == '/api/v1/user/' else {'email': email})
        body = json.dumps(payload).encode()
        with sqlite3.connect(cache / 'Cache.db') as connection:
            connection.execute('CREATE TABLE IF NOT EXISTS cfurl_cache_response (entry_ID INTEGER, request_key TEXT)')
            connection.execute('CREATE TABLE IF NOT EXISTS cfurl_cache_blob_data '
                               '(entry_ID INTEGER, request_object BLOB, response_object BLOB)')
            connection.execute('CREATE TABLE IF NOT EXISTS cfurl_cache_receiver_data '
                               '(entry_ID INTEGER, receiver_data BLOB, isDataOnFS INTEGER)')
            key = connection.execute('SELECT count(*) FROM cfurl_cache_response').fetchone()[0] + 1
            connection.execute('INSERT INTO cfurl_cache_response VALUES (?,?)',
                               (key, 'https://content.the.coach' + route))
            connection.execute('INSERT INTO cfurl_cache_blob_data VALUES (?,?,?)',
                               (key, plistlib.dumps(req, fmt=plistlib.FMT_BINARY), response))
            connection.execute('INSERT INTO cfurl_cache_receiver_data VALUES (?,?,0)', (key, body))

    def test_verified_api_identity_allows_events_when_sdk_store_is_empty(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            database = self.sdk_database(root, 'event-without-persisted-user')
            with sqlite3.connect(database) as connection:
                connection.execute('DELETE FROM store')
            self.cache_user(root, identity='fixture-user')
            uid = current_user(root, 'com.vamapps.The-Coach', 1999, 2001, 'https://content.the.coach')
            self.assertEqual('fixture-user', uid)
            reader = AnalyticsReader()
            try:
                self.assertEqual([], reader.poll(root, 1.9))
                self.assertEqual('BookmarkSaved', reader.poll(root, 1.9, uid)[0]['event'])
                self.assertEqual([], reader.poll(root, 1.9, 'wrong-user'))
            finally:
                reader.close()

    def test_cached_identity_must_match_request_and_be_fresh(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.cache_user(root, identity='ios_another-user')
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 1999, 2001, 'https://content.the.coach'))
            self.cache_user(root, stamp=2001)
            self.assertEqual('fixture-user', current_user(
                root, 'com.vamapps.The-Coach', 1999, 2002, 'https://content.the.coach'))
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 2002, 2003, 'https://content.the.coach'))
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 1999, 3001, 'https://content.the.coach'))
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 1999, 2002, 'https://other.example'))

    def test_newer_anonymous_identity_invalidates_previous_signed_in_account(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.cache_user(root)
            self.cache_user(root, stamp=2001, email=None)
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 1999, 2002, 'https://content.the.coach'))

    def test_bookmark_request_identity_does_not_require_an_sdk_database(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.cache_user(root)
            cache = root / 'Library/Caches/com.vamapps.The-Coach/Cache.db'
            request, response = self.request(stamp=2001)
            with sqlite3.connect(cache) as connection:
                connection.execute('INSERT INTO cfurl_cache_response VALUES (?,?)',
                                   (2, 'https://content.the.coach/api/v1/coachprogram/lessons/lesson-a/bookmark'))
                connection.execute('INSERT INTO cfurl_cache_blob_data VALUES (?,?,?)', (2, request, response))
            rows = read_requests(root, 'com.vamapps.The-Coach', 1999, 2002, 'https://content.the.coach')
            self.assertEqual(1, len(rows))
            self.assertTrue(rows[0]['accountMatched'])
            self.assertEqual(2, rows[0]['body']['program_day'])
            self.assertFalse((root / 'Library/com.amplitude.database').exists())

    def test_profile_identity_supports_missing_user_response_and_sdk_store(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.cache_user(root, route='/api/v1/user/achievments/')
            self.assertEqual('fixture-user', current_user(
                root, 'com.vamapps.The-Coach', 1999, 2001, 'https://content.the.coach'))
            self.cache_user(root, stamp=2001, email=None, route='/api/v1/user/achievments/')
            self.assertIsNone(current_user(root, 'com.vamapps.The-Coach', 1999, 2002, 'https://content.the.coach'))

    def test_drained_event_waits_for_matching_verified_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            database = self.sdk_database(root, 'event-before-identity')
            with sqlite3.connect(database) as connection:
                connection.execute('DELETE FROM store')
            reader = AnalyticsReader()
            try:
                self.assertEqual([], reader.poll(root, 1.9))
                with sqlite3.connect(database) as connection:
                    connection.execute('DELETE FROM events')
                self.assertEqual([], reader.poll(root, 1.9, 'wrong-user'))
                self.assertEqual('BookmarkSaved', reader.poll(root, 1.9, 'fixture-user')[0]['event'])
                self.assertEqual([], reader.poll(root, 1.9, 'fixture-user'))
            finally:
                reader.close()

    def test_unverified_old_queue_row_cannot_restart_its_buffer_window(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            database = self.sdk_database(root, 'unverified-event')
            with sqlite3.connect(database) as connection:
                connection.execute('DELETE FROM store')
            reader = AnalyticsReader()
            try:
                with patch('observe_bookmarks_evidence.time.monotonic', side_effect=[100, 106, 107]):
                    self.assertEqual([], reader.poll(root, 1.9))
                    self.assertEqual([], reader.poll(root, 1.9, 'fixture-user'))
                    reader.close()  # Reopening after a transient read error must not revive the old row.
                    self.assertEqual([], reader.poll(root, 1.9, 'fixture-user'))
            finally:
                reader.close()


if __name__ == '__main__':
    unittest.main()
