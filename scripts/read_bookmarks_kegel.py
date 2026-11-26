"""Corroborate a UI-selected Kegel fixture with a fresh authenticated catalog GET."""
import argparse
from contextlib import closing
import json
import os
from pathlib import Path
import re
import sqlite3
import tempfile
import time
import urllib.request

from read_android_module_content import adb, api_host, session, NoRedirect
from read_simulator_program_content import _oracle
from read_update_account import cached_headers
from read_ios_email_enforcement import BUNDLES, copy_data, installed_build


def ios_context(device, app):
    host, gender = BUNDLES[app]
    with tempfile.TemporaryDirectory(prefix='coach-kegel-evidence-') as temporary:
        folder = Path(temporary)
        build = installed_build(device, app, folder)
        copy_data(device, app, 'Library/com.amplitude.database', folder / 'identity.db')
        with closing(sqlite3.connect((folder / 'identity.db').as_uri() + '?mode=ro', uri=True)) as db:
            row = db.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        uid = row[0] if row else None
        if uid != os.environ.get('COACH_EXPECTED_ACCOUNT_UID'):
            raise ValueError('Kegel installation belongs to another account')
        copy_data(device, app, 'Library/Caches/' + app, folder / 'cache')
        headers = cached_headers(folder / 'cache/Cache.db', host, uid)
    return uid, host, gender, build, headers


def android_context(device, app):
    debug = os.environ.get('COACH_ANDROID_EVIDENCE_PACKAGE')
    if app not in ('com.vamapps.thecoach', 'com.vamapps.thecoach.forher'):
        raise ValueError('Unsupported Kegel package')
    if debug:
        if debug != app:
            raise ValueError('Debug evidence package differs from the UI app')
    elif app != 'com.vamapps.thecoach' or not re.fullmatch(r'emulator-[0-9]+', device):
        raise ValueError('Production Kegel evidence needs the reviewed rooted Him emulator')
    uid, token = session(device)
    if uid != os.environ.get('COACH_EXPECTED_ACCOUNT_UID'):
        raise ValueError('Kegel installation belongs to another account')
    dump = adb(device, 'shell', 'dumpsys', 'package', app).decode()
    version = re.search(r'versionName=([^\s]+)', dump)
    build = re.search(r'versionCode=(\d+)', dump)
    if not version or not build:
        raise ValueError('Installed Kegel build is unavailable')
    return uid, api_host(), 'female' if app.endswith('.forher') else 'male', {
        'version': version[1], 'build': build[1]}, {'Authorization': 'FirebaseToken ' + token}


def validate_card(payload, program, day, card_id, title, link_type, day_label, module_order=0):
    content = _oracle(payload, program)
    if content['day'] != day:
        raise ValueError('Kegel catalog belongs to another program day')
    section = payload['program_section']
    if module_order and section.get('module_order') != module_order:
        raise ValueError('Kegel catalog belongs to another UI module')
    label = ('Stage ' + str(section.get('module_current_day')) + ' of ' + str(section.get('module_total_days'))
             if section.get('module_program') is True else 'Day ' + str(content['day']))
    if label != day_label:
        raise ValueError('Kegel catalog differs from the UI day label')
    rows = [row for row in section['questions'] if row.get('headline') == title]
    if len(rows) != 1 or rows[0].get('name') != card_id:
        raise ValueError('The selected Kegel title/ID is missing or ambiguous')
    links = rows[0].get('link_type')
    if not isinstance(links, dict) or not isinstance(links.get(link_type), dict) or not links[link_type]:
        raise ValueError('The live Kegel card has the wrong workout type')
    if any(kind in links for kind in ('kegel', 'kegel_sport', 'kegel_workout') if kind != link_type):
        raise ValueError('The live Kegel card has ambiguous workout types')
    return {'programId': program, 'day': day, 'dayLabel': label, 'id': card_id,
            'title': title, 'linkType': link_type}


def read(platform, device, app, program, day, card_id, title, link_type, day_label, version, build, boundary, module_order=0):
    uid = os.environ.get('COACH_EXPECTED_ACCOUNT_UID', '')
    if not re.fullmatch(r'[A-Za-z0-9_-]{20,128}', uid):
        raise ValueError('An approved Kegel fixture UID is required')
    if not re.fullmatch(r'[a-z0-9_]{1,80}', program) or day <= 0 or link_type not in ('kegel', 'kegel_sport'):
        raise ValueError('Invalid Kegel fixture scope')
    if not 0 <= int(time.time() * 1000) - boundary <= 120000:
        raise ValueError('The Kegel UI action is stale')
    current, host, gender, installed, headers = (ios_context(device, app) if platform == 'ios'
                                               else android_context(device, app))
    if current != uid or installed['version'] != version or installed['build'] != build:
        raise ValueError('Kegel evidence differs from the current account/build')
    headers.update({'AppVersion': version, 'BuildVersion': build, 'X-Gender': gender,
                    'Language': 'en', 'Cache-Control': 'no-cache'})
    opener = urllib.request.build_opener(NoRedirect())
    def get(route):
        with opener.open(urllib.request.Request(host + route, headers=headers), timeout=12) as response:
            return json.load(response)
    user = get('/api/v1/user/')['userdata']
    expected_email = os.environ.get('COACH_BOOKMARKS_KEGEL_EXPECTED_EMAIL', '').casefold()
    if (not expected_email or user.get('analytics_profile', {}).get('email', '').casefold() != expected_email
            or user.get('subscription', {}).get('active') is not True):
        raise ValueError('Kegel account does not match the approved Premium fixture')
    if platform == 'ios' and user.get('user_id') not in (uid, 'ios_' + uid):
        raise ValueError('Kegel API response belongs to another account')
    payload = get('/api/v1/coachprogram/catalog/v3/' + program + '/?day_of_program=' + str(day))
    result = validate_card(payload, program, day, card_id, title, link_type, day_label, module_order)
    if not 0 <= int(time.time() * 1000) - boundary <= 120000:
        raise ValueError('The Kegel evidence no longer belongs to the recent UI action')
    return dict(result, version=version, build=build, identityVerified=True,
                source='Authenticated catalog GET after UI selection; not captured application traffic')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('platform', 'device', 'app', 'program', 'card-id', 'title', 'link-type', 'day-label', 'version', 'build'):
        parser.add_argument('--' + name, required=True)
    parser.add_argument('--day', type=int, required=True)
    parser.add_argument('--boundary', type=int, required=True)
    parser.add_argument('--module-order', type=int, default=0)
    args = vars(parser.parse_args())
    if args['platform'] not in ('ios', 'android'):
        raise ValueError('Unsupported Kegel platform')
    if args['platform'] == 'ios' and not re.fullmatch(r'(?:[A-Fa-f0-9-]{36}|[A-Fa-f0-9]{8}-[A-Fa-f0-9]{16})', args['device']):
        raise ValueError('An explicit iOS device is required')
    print(json.dumps(read(**args)))


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Current Kegel evidence could not verify the approved account, build and selected card')
