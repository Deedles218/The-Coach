import base64
import email.utils
import json
import plistlib
from pathlib import Path
import sqlite3
import tempfile
import unittest

from observe_bookmarks_evidence import AnalyticsReader, CF_EPOCH, fresh_event, fresh_request


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


if __name__ == '__main__':
    unittest.main()
