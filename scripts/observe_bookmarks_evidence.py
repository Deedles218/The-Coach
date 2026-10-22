#!/usr/bin/env python3
"""Read fresh Bookmarks SDK events and cached HTTP requests from an explicit iOS simulator."""
import argparse
import base64
from contextlib import closing
import email.utils
import hashlib
import json
import math
from pathlib import Path
import plistlib
import re
import sqlite3
import subprocess
import threading
import time
import urllib.parse

from observe_simulator_event import event_key
from read_android_module_content import HOST

EVENTS = {'BookmarksOpened', 'BookmarkSaved', 'BookmarkDeleted'}
CF_EPOCH = 978307200


def fresh_event(payload, uid, not_before_ms):
    event = json.loads(payload)
    stamp = event.get('timestamp')
    if (event.get('event_type') not in EVENTS or event.get('user_id') != uid or not uid
            or not isinstance(stamp, (int, float)) or isinstance(stamp, bool)
            or not math.isfinite(stamp) or stamp < not_before_ms):
        return None
    properties = event.get('event_properties', {})
    return {'event': event['event_type'], 'timestamp': event['timestamp'], 'accountMatched': True,
            'fingerprint': event_key(payload),
            'properties': {key: properties[key] for key in ('lesson_id', 'slide_number') if key in properties}}


def fresh_request(request_blob, response_blob, uid, not_before, now, host):
    request, response = plistlib.loads(request_blob), plistlib.loads(response_blob)
    if request.get('Version') != 9 or response.get('Version') != 1:
        return None
    req, res = request['Array'], response['Array']
    if len(req) < 22 or len(res) < 5 or res[3] != 200:
        return None
    if not isinstance(res[1], (int, float)) or isinstance(res[1], bool) or not math.isfinite(res[1]):
        return None
    stamp = res[1] + CF_EPOCH
    date = next((value for key, value in res[4].items() if key.lower() == 'date'), None)
    if (not date or stamp < not_before or stamp > now + 2
            or abs(email.utils.parsedate_to_datetime(date).timestamp() - stamp) > 2):
        return None
    urls = [item['_CFURLString'] for item in req if isinstance(item, dict) and '_CFURLString' in item]
    if len(urls) != 1:
        return None
    url = urllib.parse.urlsplit(urls[0])
    if (url.scheme + '://' + url.netloc != host or url.query or url.fragment
            or not re.fullmatch(r'/api/v1/coachprogram/lessons/[A-Za-z0-9_-]+/bookmark', url.path)
            or req[18] != 'POST'):
        return None
    sources = [item for item in req if isinstance(item, dict) and 'Authorization' in item]
    if len(sources) != 1:
        return None
    parts = sources[0]['Authorization'].split(' ')
    if len(parts) != 2 or parts[0].lower() not in ('firebasetoken', 'bearer'):
        return None
    token = parts[1].split('.')
    if len(token) != 3:
        return None
    claims = json.loads(base64.urlsafe_b64decode(token[1] + '=' * (-len(token[1]) % 4)))
    expiry, subject = claims.get('exp'), claims.get('user_id')
    if (not uid or uid not in (subject, 'ios_' + str(subject)) or claims.get('sub') != subject
            or not isinstance(expiry, (int, float)) or isinstance(expiry, bool)
            or not math.isfinite(expiry) or expiry <= now):
        return None
    bodies = [item for item in req[21] if isinstance(item, bytes)]
    if len(bodies) != 1:
        return None
    body = json.loads(bodies[0])
    if (not isinstance(body, dict) or not isinstance(body.get('program_id'), str)
            or not re.fullmatch(r'[A-Za-z0-9_-]{1,120}', body['program_id'])
            or type(body.get('program_day')) is not int or body['program_day'] < 1
            or type(body.get('slide_number')) is not int or body['slide_number'] < 1):
        return None
    return {'method': 'POST', 'path': url.path, 'timestamp': stamp, 'accountMatched': True,
            'body': {key: body[key] for key in ('program_id', 'program_day', 'slide_number')},
            'fingerprint': hashlib.sha256(request_blob + response_blob).hexdigest()}


def container(udid, bundle):
    result = subprocess.run(['xcrun', 'simctl', 'get_app_container', udid, bundle, 'data'],
                            capture_output=True, text=True, timeout=10)
    return Path(result.stdout.strip()) if result.returncode == 0 else None


class AnalyticsReader:
    """Keep the SDK queue connection warm; URLCache work must not block this reader."""
    def __init__(self):
        self.root = None
        self.connection = None

    def close(self):
        if self.connection:
            self.connection.close()
        self.connection = None

    def poll(self, root, started):
        if root != self.root or (root and not root.exists()):
            self.close()
            self.root = root
        if root is None:
            return []
        if self.connection is None:
            database = root / 'Library/com.amplitude.database'
            self.connection = sqlite3.connect(database.as_uri() + '?mode=ro', uri=True, timeout=.2)
        row = self.connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        uid = row[0] if row else None
        records = []
        for payload, in self.connection.execute('SELECT event FROM events'):
            try:
                event = fresh_event(payload, uid, int(started * 1000))
                if event:
                    records.append(event)
            except (ValueError, TypeError, KeyError):
                continue  # An unrelated malformed SDK row must not discard valid events.
        return records


def read_requests(root, bundle, started, now, host):
    database = root / 'Library/com.amplitude.database'
    with closing(sqlite3.connect(database.as_uri() + '?mode=ro', uri=True, timeout=.2)) as connection:
        row = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        uid = row[0] if row else None
    cache = root / 'Library/Caches' / bundle / 'Cache.db'
    with closing(sqlite3.connect(cache.as_uri() + '?mode=ro', uri=True, timeout=.2)) as connection:
        rows = connection.execute(
            "SELECT b.request_object,b.response_object FROM cfurl_cache_response r "
            "JOIN cfurl_cache_blob_data b ON r.entry_ID=b.entry_ID WHERE r.request_key LIKE '%/bookmark'").fetchall()
    records = []
    for request, response in rows:
        try:
            record = fresh_request(request, response, uid, started, now, host)
            if record:
                records.append(record)
        except (ValueError, TypeError, KeyError, IndexError, plistlib.InvalidFileException):
            continue
    return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--udid', required=True)
    parser.add_argument('--bundle-id', required=True)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--api-host', default=HOST)
    parser.add_argument('--timeout', type=int, default=3600)
    parser.add_argument('--stop-file', type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Fa-f0-9-]{36}', args.udid):
        parser.error('Select an explicit test simulator UDID')
    if args.timeout <= 0:
        parser.error('timeout must be positive')
    if args.bundle_id not in ('com.vamapps.The-Coach', 'com.vamapps.The-Coach-for-her',
                              'com.vamapps.preprod.The-Coach'):
        parser.error('Unsupported Coach bundle ID')
    host = urllib.parse.urlsplit(args.api_host)
    if (host.scheme != 'https' or not host.netloc or host.path or host.query or host.fragment
            or host.username or host.password):
        parser.error('api-host must be an HTTPS origin without a path')
    args.output.mkdir(parents=True, exist_ok=True)
    outputs = {kind: args.output / (kind + '.jsonl') for kind in ('analytics', 'network')}
    for path in outputs.values():
        path.touch(exist_ok=True)
    state, started = {'root': None}, time.time()
    deadline = time.monotonic() + args.timeout
    stop = threading.Event()
    errors = {'analytics': 0, 'network': 0}
    counts = {'analytics': 0, 'network': 0}

    def observe(kind):
        reader, seen = AnalyticsReader(), set()
        try:
            while not stop.is_set():
                try:
                    root = state['root']
                    records = (reader.poll(root, started) if kind == 'analytics' else
                               read_requests(root, args.bundle_id, started, time.time(), args.api_host)
                               if root else [])
                    for record in records:
                        if record['fingerprint'] in seen:
                            continue
                        with outputs[kind].open('a') as output:
                            output.write(json.dumps(record) + '\n')
                        seen.add(record['fingerprint'])
                        counts[kind] += 1
                except (sqlite3.Error, OSError, ValueError, TypeError, KeyError, IndexError):
                    errors[kind] += 1
                    reader.close()
                # SDK rows can disappear after upload. Cache parsing runs independently.
                stop.wait(.005 if kind == 'analytics' else .1)
        finally:
            reader.close()

    workers = [threading.Thread(target=observe, args=(kind,)) for kind in outputs]
    for worker in workers:
        worker.start()
    print('READY: read-only Bookmarks evidence observer', flush=True)
    try:
        while time.monotonic() < deadline and not (args.stop_file and args.stop_file.exists()):
            try:
                state['root'] = container(args.udid, args.bundle_id)
            except (OSError, subprocess.SubprocessError):
                state['root'] = None
            stop.wait(.5)  # Follow reinstalls without pausing either evidence stream.
    finally:
        stop.set()
        for worker in workers:
            worker.join()
    print('Observed records:', counts, '; transient read errors:', errors, flush=True)


if __name__ == '__main__':
    main()
