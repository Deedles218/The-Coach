#!/usr/bin/env python3
"""Read redacted COA-9222 evidence from an explicit preprod debug installation."""
import argparse
import base64
import hashlib
import json
import os
import re
import time
import urllib.request
import xml.etree.ElementTree as ET

from read_android_module_content import adb, NoRedirect
from read_ios_email_enforcement import account_state
from read_update_account import PREPROD_HOST

PACKAGES = {'com.vamapps.thecoach': 'male', 'com.vamapps.thecoach.forher': 'female'}


def installed_build(device, package):
    if package not in PACKAGES:
        raise ValueError('Unsupported test package')
    info = adb(device, 'shell', 'dumpsys', 'package', package).decode()
    if 'DEBUGGABLE' not in info:
        raise ValueError('Only the reviewed preprod debug artifacts are supported')
    return {'appPackage': package, 'version': re.search(r'versionName=([^\s]+)', info)[1],
            'build': re.search(r'versionCode=(\d+)', info)[1], 'platform': 'android'}


def decode_session(xml, expected_uid=None):
    users = [json.loads(e.text) for e in ET.fromstring(xml)
             if e.get('name') == 'com.google.firebase.auth.FIREBASE_USER']
    if len(users) != 1:
        raise ValueError('Expected one Firebase session')
    user = users[0]
    token = json.loads(user['cachedTokenState'])['access_token']
    payload = token.split('.')[1]
    claims = json.loads(base64.urlsafe_b64decode(payload + '=' * (-len(payload) % 4)))
    uid = claims.get('user_id')
    identities = [json.loads(e) if isinstance(e, str) else e for e in user['userInfos']]
    if (not isinstance(uid, str) or not re.fullmatch(r'[A-Za-z0-9_-]{20,128}', uid)
            or claims.get('sub') != uid or claims.get('exp', 0) <= time.time()
            or not any(e.get('userId') == uid for e in identities)
            or (expected_uid and expected_uid != uid)):
        raise ValueError('Firebase identity/expiry verification failed')
    return uid, token


def session(device, package, expected_uid=None):
    names = adb(device, 'shell', 'run-as', package, 'ls', 'shared_prefs').decode().splitlines()
    names = [n for n in names if re.fullmatch(r'com\.google\.firebase\.auth\.api\.Store\.[A-Za-z0-9_+.=-]+\.xml', n)]
    if len(names) != 1:
        raise ValueError('Expected exactly one Firebase store')
    return decode_session(adb(device, 'exec-out', 'run-as', package, 'cat', 'shared_prefs/' + names[0]), expected_uid)


def active_config(payload, gender):
    values = payload['configs_key']
    suffix = '_for_her' if gender == 'female' else ''
    key = 'abtest_force_connect_email' + suffix
    enabled = values.get(key)
    if isinstance(enabled, str):
        enabled = json.loads(enabled)
    if isinstance(enabled, dict):
        enabled = enabled.get('isEnabled')
    if type(enabled) is not bool:
        raise ValueError('Active Android email flag is missing or has unknown type')
    legacy = values.get('abtest_enter_email_after_subscription' + suffix)
    if isinstance(legacy, str):
        legacy = json.loads(legacy)
    return {'configKey': key, 'enabled': enabled,
            'templateVersion': payload.get('template_version_number_key'),
            'fetchedAtEpochMs': payload.get('fetch_time_key'),
            'legacyEnabled': legacy.get('isEnabled') if isinstance(legacy, dict) else legacy}


def android_account_state(user, achievements, firebase_uid, expected_email):
    backend_uid = user.get('user_id')
    if not isinstance(backend_uid, str) or not backend_uid:
        raise ValueError('Backend identity is missing')
    # Android Firebase UID and API user_id are distinct. Preserve BOTH across linking.
    state = account_state(user, achievements, backend_uid, expected_email)
    state['identity'] = {'firebase': hashlib.sha256(firebase_uid.encode()).hexdigest(),
                         'backend': state['identity']}
    return state


def read(device, package, mode, expected_uid=None):
    build = installed_build(device, package)
    if mode == 'build':
        return build
    if mode == 'config':
        names = adb(device, 'shell', 'run-as', package, 'ls', 'files').decode().splitlines()
        active = [n for n in names if re.fullmatch(r'frc_[A-Za-z0-9_:.-]+_firebase_activate\.json', n)]
        if len(active) != 1:
            raise ValueError('Expected one activated Remote Config file')
        payload = json.loads(adb(device, 'exec-out', 'run-as', package, 'cat', 'files/' + active[0]))
        return dict(build, **active_config(payload, PACKAGES[package]))
    uid, token = session(device, package, expected_uid)
    headers = {'Authorization': 'FirebaseToken ' + token, 'AppVersion': build['version'],
               'BuildVersion': build['build'], 'X-Gender': PACKAGES[package], 'Language': 'en',
               'Timezone': 'Europe/Samara', 'Cache-Control': 'no-cache'}
    opener = urllib.request.build_opener(NoRedirect())
    def get(route):
        with opener.open(urllib.request.Request(PREPROD_HOST + route, headers=headers), timeout=15) as response:
            return json.load(response)
    state = android_account_state(get('/api/v1/user/')['userdata'], get('/api/v1/user/achievments/'), uid,
                                  os.environ.get('COACH_VALID_EMAIL_WITHOUT_PROGRESS', ''))
    return dict(build, **state)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--package', choices=tuple(PACKAGES), required=True)
    parser.add_argument('--mode', choices=('config', 'account', 'build'), required=True)
    args = parser.parse_args()
    print(json.dumps(read(args.device, args.package, args.mode,
                          os.environ.get('COACH_EMAIL_ENFORCEMENT_UID')), sort_keys=True))


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Android email evidence unavailable: check preprod debug build, device, config and session')
