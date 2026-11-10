#!/usr/bin/env python3
"""Read COA-9222 evidence from one iOS installation; never change its config or account."""
import argparse
from contextlib import closing
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import sqlite3
import subprocess
import tempfile
import urllib.request
import zipfile

from read_update_account import cached_headers, PREPROD_HOST
from read_android_module_content import NoRedirect, HOST as PROD_HOST

BUNDLES = {
    'com.vamapps.preprod.The-Coach': (PREPROD_HOST, 'male'),
    'com.vamapps.preprod.The-Coach-for-her': (PREPROD_HOST, 'female'),
    'com.vamapps.The-Coach': (PROD_HOST, 'male'),
    'com.vamapps.The-Coach-for-her': (PROD_HOST, 'female'),
}


def command(*args):
    return subprocess.check_output(args, stderr=subprocess.DEVNULL, timeout=25)


def simulator(device):
    return bool(re.fullmatch(r'[A-Fa-f0-9-]{36}', device))


def copy_data(device, bundle, relative, destination):
    """Copy directories with their SQLite WAL files, never just an uncheckpointed DB."""
    if simulator(device):
        import shutil
        root = Path(command('xcrun', 'simctl', 'get_app_container', device, bundle, 'data').decode().strip())
        source = root / relative
        if source.is_dir():
            shutil.copytree(source, destination)
        else:
            shutil.copy2(source, destination)
        return
    command('xcrun', 'devicectl', 'device', 'copy', 'from', '--device', device,
            '--domain-type', 'appDataContainer', '--domain-identifier', bundle,
            '--source', relative, '--destination', str(destination), '--quiet')


def installed_build(device, bundle, folder):
    if simulator(device):
        app = Path(command('xcrun', 'simctl', 'get_app_container', device, bundle, 'app').decode().strip())
        return build_metadata(plistlib.loads((app / 'Info.plist').read_bytes()), bundle)
    output = folder / 'apps.json'
    command('xcrun', 'devicectl', 'device', 'info', 'apps', '--device', device,
            '--json-output', str(output), '--quiet')
    apps = json.loads(output.read_text())['result']['apps']
    matches = [app for app in apps if app.get('bundleIdentifier') == bundle]
    if len(matches) != 1:
        raise ValueError('Requested installation is unavailable')
    return build_metadata({'CFBundleIdentifier': bundle,
                           'CFBundleShortVersionString': matches[0]['version'],
                           'CFBundleVersion': matches[0]['bundleVersion'],
                           'DTPlatformName': 'iphoneos'}, bundle)


def build_metadata(info, bundle):
    if info.get('CFBundleIdentifier') != bundle:
        raise ValueError('Build belongs to another bundle')
    build = str(info.get('CFBundleVersion', ''))
    if not re.fullmatch(r'\d+(?:\.\d+){0,2}', build):
        raise ValueError('A numeric build number is required')
    if info.get('DTPlatformName') not in ('iphoneos', 'iphonesimulator'):
        raise ValueError('Not an iOS build')
    return {'bundleId': bundle, 'version': info['CFBundleShortVersionString'],
            'build': build, 'platform': info['DTPlatformName']}


def artifact_build(path, bundle):
    path = Path(path)
    if path.suffix == '.app':
        info = plistlib.loads((path / 'Info.plist').read_bytes())
    elif path.suffix == '.ipa':
        with zipfile.ZipFile(path) as archive:
            names = [name for name in archive.namelist()
                     if re.fullmatch(r'Payload/[^/]+\.app/Info\.plist', name)]
            if len(names) != 1:
                raise ValueError('IPA must have one top-level app')
            info = plistlib.loads(archive.read(names[0]))
    else:
        raise ValueError('Supply a signed .ipa or Simulator .app')
    return build_metadata(info, bundle)


def active_config(database, gender):
    key = 'force_connect_email_config' + ('_for_her' if gender == 'female' else '')
    legacy_key = 'abtest_enter_email_after_subscription' + ('_for_her' if gender == 'female' else '')
    with closing(sqlite3.connect(Path(database).resolve().as_uri() + '?mode=ro', uri=True)) as connection:
        row = connection.execute('SELECT value FROM main_active WHERE key=?', (key,)).fetchone()
        legacy = connection.execute('SELECT value FROM main_active WHERE key=?', (legacy_key,)).fetchone()
    if row is None:
        raise ValueError('Active email configuration is missing')
    config = json.loads(row[0])
    if type(config.get('isEnabled')) is not bool or type(config.get('launchesBeforeShow')) is not int:
        raise ValueError('Email configuration has unknown types')
    if config['launchesBeforeShow'] <= 0:
        raise ValueError('Email launch threshold must be positive')
    legacy_enabled = json.loads(legacy[0]).get('isEnabled') if legacy else None
    if type(legacy_enabled) is not bool:
        legacy_enabled = None
    return {'configKey': key, 'enabled': config['isEnabled'],
            'launchesBeforeShow': config['launchesBeforeShow'], 'legacyEnabled': legacy_enabled}


def account_state(user, achievements, uid, expected_email):
    """Return only the identity digest and the business fields asserted by the tests."""
    if user.get('user_id') not in (uid, 'ios_' + uid):
        raise ValueError('API account differs from current installation')
    if not isinstance(user.get('analytics_profile'), dict):
        raise ValueError('Email state is unavailable')
    connected = user.get('connected_email')
    if connected is None and not user['analytics_profile'].get('email') and not achievements.get('email'):
        # Anonymous responses omit connected_email and both email fields entirely.
        connected = {'email': '', 'verified': False, 'user_confirmed': False}
    if not isinstance(connected, dict):
        raise ValueError('Email state is unavailable')
    if any(type(connected.get(key)) is not bool for key in ('verified', 'user_confirmed')):
        raise ValueError('Email confirmation state is unknown')
    email = connected.get('email') or ''
    if not isinstance(email, str):
        raise ValueError('Email has an unknown type')
    active = user.get('subscription', {}).get('active')
    if type(active) is not bool:
        raise ValueError('Subscription state is unknown')
    progress = {}
    for key in ('lessons_completed', 'exercises_completed', 'longest_streak'):
        value = achievements.get(key)
        if type(value) is not int or value < 0:
            raise ValueError('Control progress is missing')
        progress[key] = value
    if (achievements.get('email') or '').casefold() != email.casefold():
        raise ValueError('Account and achievements email disagree')
    return {'identity': hashlib.sha256(uid.encode()).hexdigest(),
            'subscriptionActive': active, 'progress': progress,
            'emailPresent': bool(email),
            'emailVerified': connected['verified'] and connected['user_confirmed'],
            'emailMatchesExpected': bool(expected_email) and email.casefold() == expected_email.casefold()}


def read(device, bundle, mode, expected_uid=None):
    host, gender = BUNDLES[bundle]
    with tempfile.TemporaryDirectory(prefix='coach-email-evidence-') as temporary:
        folder = Path(temporary)
        build = installed_build(device, bundle, folder)
        if mode == 'build':
            return build
        if mode == 'config':
            copy_data(device, bundle, 'Library/Application Support/Google/RemoteConfig', folder / 'config')
            return dict(build, **active_config(folder / 'config/RemoteConfig.sqlite3', gender))
        # The SDK identity and URL-cache JWT must agree before any API request.
        copy_data(device, bundle, 'Library/com.amplitude.database', folder / 'identity.db')
        with closing(sqlite3.connect((folder / 'identity.db').as_uri() + '?mode=ro', uri=True)) as connection:
            row = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        uid = row[0] if row else None
        if not isinstance(uid, str) or not re.fullmatch(r'[A-Za-z0-9_-]{20,128}', uid):
            raise ValueError('Current account identity is unavailable')
        if expected_uid and uid != expected_uid:
            raise ValueError('Current account differs from the dedicated fixture')
        copy_data(device, bundle, 'Library/Caches/' + bundle, folder / 'cache')
        headers = cached_headers(folder / 'cache/Cache.db', host, uid)
        headers.update({'AppVersion': build['version'], 'BuildVersion': build['build'],
                        'X-Gender': gender, 'Language': 'en', 'Cache-Control': 'no-cache'})
        opener = urllib.request.build_opener(NoRedirect())
        def get(route):
            with opener.open(urllib.request.Request(host + route, headers=headers), timeout=12) as response:
                return json.load(response)
        state = account_state(get('/api/v1/user/')['userdata'], get('/api/v1/user/achievments/'), uid,
                              os.environ.get('COACH_VALID_EMAIL_WITHOUT_PROGRESS', ''))
        return dict(build, **state)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--bundle', choices=tuple(BUNDLES), required=True)
    parser.add_argument('--mode', choices=('config', 'account', 'build', 'artifact'), required=True)
    parser.add_argument('--app')
    args = parser.parse_args()
    if not re.fullmatch(r'(?:[A-Fa-f0-9-]{36}|[A-Fa-f0-9]{8}-[A-Fa-f0-9]{16})', args.device):
        raise ValueError('An explicit iOS device is required')
    if args.mode == 'artifact':
        result = artifact_build(args.app, args.bundle)
    else:
        result = read(args.device, args.bundle, args.mode, os.environ.get('COACH_EMAIL_ENFORCEMENT_UID'))
    print(json.dumps(result, sort_keys=True))


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Email evidence unavailable: check device, build, active config and current-account cache')
