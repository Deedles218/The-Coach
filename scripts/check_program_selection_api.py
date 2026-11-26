#!/usr/bin/env python3
"""QA checks through preprod API, with opt-in production fixture reads.

Never changes Firebase configuration or prepares account data automatically.
"""
import argparse
import hashlib
from contextlib import closing
import json
import os
from pathlib import Path
import re
import sqlite3
import tempfile
import urllib.error
import urllib.request

from read_android_module_content import NoRedirect
from read_ios_email_enforcement import BUNDLES, copy_data, installed_build
from read_update_account import PREPROD_HOST, cached_headers
from read_program_selection_exchange import read_exchange, is_selection_route, is_activation_route
from read_simulator_program_content import EvidenceError as CacheEvidenceError

ROOT = '/api/v1/coachprogram/'
ENTRY_FIELDS = ('program_id', 'title', 'membership_status', 'status',
                'current_module_name', 'current_stage', 'total_stages',
                'total_program_stages', 'current_program_day', 'progress_percent', 'is_active')
OPERATIONS = ('secondary-includes-main', 'activate-unadded', 'complete-without-main')
CHECKS = OPERATIONS + ('new-onboarding-profile', 'secondary-onboarding-profile', 'observe', 'first-activation')


def gender_header(bundle):
    # The native client's cached requests use M/F, not male/female.
    return 'F' if BUNDLES[bundle][1] == 'female' else 'M'


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def session_host(bundle, allow_production_read=False):
    require(bundle in BUNDLES, 'Unsupported program selection bundle')
    host = BUNDLES[bundle][0]
    require(host == PREPROD_HOST or allow_production_read,
            'Program selection checks require an explicit preprod bundle or approved production read')
    return host


def entry(value):
    return None if value is None else {k: value[k] for k in ENTRY_FIELDS if k in value}


def selection_flags(value, path=''):
    result = {}
    if isinstance(value, dict):
        for key, child in value.items():
            result.update(selection_flags(child, path + '.' + key))
    elif type(value) is bool and 'program' in path.lower() and any(
            word in path.lower() for word in ('onboard', 'selection')):
        result[path] = value
    return result


class Session:
    def __init__(self, device, bundle, allow_production_read=False):
        self.device, self.bundle = device, bundle
        self.host = session_host(bundle, allow_production_read)
        self.read_only = self.host != PREPROD_HOST
        require(bool(re.fullmatch(r'(?:[A-Fa-f0-9-]{36}|[A-Fa-f0-9]{8}-[A-Fa-f0-9]{16})', device)),
                'An explicit iOS test device is required')
        self.opener = urllib.request.build_opener(NoRedirect())
        self.expected_email = os.environ.get('COACH_PROGRAM_SELECTION_EMAIL')
        self.pending_email = os.environ.get('COACH_PROGRAM_SELECTION_NEW_EMAIL')
        self.expected_uid = os.environ.get('COACH_PROGRAM_SELECTION_UID')
        require(not self.read_only or bool(self.expected_email),
                'Production fixture reads require a registered QA email binding')
        with tempfile.TemporaryDirectory(prefix='coach-program-api-') as temporary:
            folder = Path(temporary)
            build = installed_build(device, bundle, folder)
            copy_data(device, bundle, 'Library/com.amplitude.database', folder / 'identity.db')
            with closing(sqlite3.connect((folder / 'identity.db').as_uri() + '?mode=ro', uri=True)) as db:
                row = db.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
            require(row is not None and bool(re.fullmatch(r'[A-Za-z0-9_-]{20,128}', row[0])),
                    'Current installation identity is unavailable')
            self.uid = row[0]
            copy_data(device, bundle, 'Library/Caches/' + bundle, folder / 'cache')
            self.headers = cached_headers(folder / 'cache/Cache.db', self.host, self.uid)
            self.headers.update({'AppVersion': build['version'], 'BuildVersion': build['build'],
                                 'X-Gender': gender_header(bundle), 'Language': 'en',
                                 # Backend classifies iOS catalog availability by this transport marker.
                                 # Python's default UA hides unadded programs even with the same UID/build.
                                 'User-Agent': 'TheCoach-Automation/' + build['version'] + ' CFNetwork',
                                 'Cache-Control': 'no-cache', 'Content-Type': 'application/json'})
        self.require_fixture_identity()

    def request(self, route, payload=None, post=False):
        require(not self.read_only or (not post and payload is None),
                'Production program selection API sessions are read-only')
        safe_get = route in (ROOT + 'my_programs/', ROOT + 'catalog/v4/',
                             ROOT + 'onboarding/recommended_program/', '/api/v1/user/', '/api/v1/user/achievments/')
        safe_get = safe_get or bool(re.fullmatch(re.escape(ROOT) + r'catalog/v4/[a-z0-9_]{1,80}/', route))
        content = bool(re.fullmatch(re.escape(ROOT) + r'(?:lessons|exercises|actions)/[A-Za-z0-9_-]{1,128}/?', route))
        if content:
            require(not self.read_only and os.environ.get('COACH_PROGRAM_SELECTION_CASE') == 'COA-9796'
                    and bool(getattr(self, 'expected_uid', None))
                    and self.expected_uid == self.uid, 'Content access needs the dedicated COA-9796 UID binding')
            safe_get = True
        relative = route.removeprefix(ROOT)
        safe_post = route.startswith(ROOT) and is_selection_route(relative)
        if post and is_activation_route(relative):
            require(os.environ.get('COACH_PROGRAM_SELECTION_CASE') == 'COA-9797'
                    and bool(getattr(self, 'expected_uid', None)) and self.expected_uid == self.uid,
                    'Activation POST requires the dedicated COA-9797 UID binding')
            require(payload is None, 'The observed activation POST is bodyless; its program ID belongs in the URL')
        require(safe_post if post else safe_get, 'Unexpected program selection endpoint')
        require(post or payload is None, 'Program observation GET cannot contain a payload')
        require('?' not in route and '#' not in route and '..' not in route,
                'Unexpected program selection URL')
        data = None if payload is None else json.dumps(payload).encode()
        request = urllib.request.Request(self.host + route, headers=self.headers,
                                         data=data, method='POST' if post else 'GET')
        try:
            with self.opener.open(request, timeout=15) as response:
                return response.status, json.load(response)
        except urllib.error.HTTPError as failure:
            # Response bodies may include backend diagnostics; never log them.
            failure.close()
            return failure.code, None

    def get(self, route):
        status, data = self.request(route)
        require(status == 200, 'Program selection prerequisite GET failed with HTTP ' + str(status))
        return data

    def require_fixture_identity(self):
        user = self.get('/api/v1/user/')['userdata']
        require(user.get('user_id') in (self.uid, 'ios_' + self.uid),
                'API identity differs from the current installation')
        expected_uid = getattr(self, 'expected_uid', None)
        if expected_uid:
            require(self.uid == expected_uid, 'Current account differs from COACH_PROGRAM_SELECTION_UID')
        connected = user.get('connected_email') or {}
        analytics = user.get('analytics_profile') or {}
        addresses = [v for v in (connected.get('email'), analytics.get('email')) if v]
        pending = getattr(self, 'pending_email', None)
        if not self.expected_email and pending:
            require(expected_uid == self.uid, 'New-email enrollment requires the original dedicated QA UID')
            if addresses:
                require(all(v.casefold() == pending.casefold() for v in addresses)
                        and connected.get('verified') is True and connected.get('user_confirmed') is True,
                        'New QA email differs from the prepared enrollment')
            return user
        if self.expected_email:
            require(addresses and all(v.casefold() == self.expected_email.casefold() for v in addresses),
                    'Current account differs from COACH_PROGRAM_SELECTION_EMAIL')
            require(connected.get('verified') is True and connected.get('user_confirmed') is True,
                    'The prepared QA account requires a confirmed email')
        else:
            require(not addresses, 'Set COACH_PROGRAM_SELECTION_EMAIL for a registered QA profile')
            require(expected_uid == self.uid,
                    'Set COACH_PROGRAM_SELECTION_UID for the intended anonymous QA profile')
        return user

    def snapshot(self):
        user = self.require_fixture_identity()
        programs = self.get(ROOT + 'my_programs/')
        catalog = self.get(ROOT + 'catalog/v4/')['programs']
        achievements = self.get('/api/v1/user/achievments/')
        return {
            'main': entry(programs['main_journey']),
            'backend_identity': hashlib.sha256(user['user_id'].encode()).hexdigest(),
            'secondary': sorted((entry(p) for p in programs['secondary_programs']),
                                key=lambda p: p['program_id']),
            'active': programs['active_program_id'],
            'catalog_membership': {p['program_id']: p['membership_status'] for p in catalog},
            'selection_flags': selection_flags(user),
            'subscription_active': user.get('subscription', {}).get('active'),
            'achievements': {k: achievements[k] for k in
                             ('lessons_completed', 'exercises_completed', 'longest_streak')},
        }

    def post(self, route, payload=None):
        status, result = self.request(ROOT + route, payload, post=True)
        require(status == 200, 'Fixture selection POST failed with HTTP ' + str(status))
        return result

    def exchange(self, route, not_before_ms):
        require(not self.read_only, 'Program exchange evidence requires preprod')
        with tempfile.TemporaryDirectory(prefix='coach-selection-exchange-') as temporary:
            folder = Path(temporary) / 'cache'
            copy_data(self.device, self.bundle, 'Library/Caches/' + self.bundle, folder)
            return read_exchange(folder / 'Cache.db', self.host, self.uid, route, not_before_ms)


class AndroidSession(Session):
    """Read-only adapter for the independently installed, reviewed preprod APK."""
    def __init__(self, device, package):
        from read_android_email_enforcement import installed_build as android_build, session as android_session, PACKAGES
        from read_android_module_content import adb
        self.case = os.environ.get('COACH_PROGRAM_SELECTION_CASE')
        require(self.case in ('COA-9794', 'COA-9795'),
                'Android observations require the progress or cross-platform QA case')
        self.expected_email = os.environ.get('COACH_PROGRAM_SELECTION_EMAIL')
        self.backend_hash = os.environ.get('COACH_PROGRAM_SELECTION_BACKEND_HASH', '')
        expected_apk = os.environ.get('COACH_PROGRAM_SELECTION_ANDROID_APK_SHA256', '')
        require((self.expected_email or self.case == 'COA-9794') and re.fullmatch(r'[a-f0-9]{64}', self.backend_hash)
                and re.fullmatch(r'[a-f0-9]{64}', expected_apk),
                'Cross-platform observations need confirmed email, backend identity and reviewed APK bindings')
        require(package in PACKAGES, 'Unsupported Android program package')
        build = android_build(device, package)
        paths = adb(device, 'shell', 'pm', 'path', package).decode().splitlines()
        paths = [p.removeprefix('package:') for p in paths if p.endswith('/base.apk')]
        require(len(paths) == 1 and re.fullmatch(r'/data/app/[A-Za-z0-9_=+.~/-]+/base\.apk', paths[0]),
                'Installed Android APK path is unavailable')
        actual_hash = adb(device, 'shell', 'sha256sum', paths[0]).decode().split()[0]
        require(actual_hash == expected_apk, 'Installed Android APK differs from the reviewed preprod artifact')
        self.uid, token = android_session(device, package, os.environ.get('COACH_PROGRAM_SELECTION_ANDROID_UID'))
        if self.case == 'COA-9794':
            require(self.uid == os.environ.get('COACH_PROGRAM_SELECTION_ANDROID_UID'),
                    'Progress observations require the dedicated Android UID binding')
            if not self.expected_email:
                import base64
                claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '=' * (-len(token.split('.')[1]) % 4)))
                require(claims.get('firebase', {}).get('sign_in_provider') == 'anonymous',
                        'Unregistered progress fixtures must use their own anonymous SDK session')
        self.host, self.read_only = PREPROD_HOST, False
        self.opener = urllib.request.build_opener(NoRedirect())
        self.headers = {'Authorization': 'FirebaseToken ' + token, 'AppVersion': build['version'],
                        'BuildVersion': build['build'], 'X-Gender': PACKAGES[package], 'Language': 'en',
                        'User-Agent': 'TheCoach-Automation/' + build['version'] + ' okhttp',
                        'Cache-Control': 'no-cache'}
        self.require_fixture_identity()

    def request(self, route, payload=None, post=False):
        require(not post and payload is None, 'Android program observations are read-only')
        return super().request(route, payload, post)

    def require_fixture_identity(self):
        user = self.get('/api/v1/user/')['userdata']
        require(isinstance(user.get('user_id'), str)
                and hashlib.sha256(user['user_id'].encode()).hexdigest() == self.backend_hash,
                'Android and iOS belong to different backend accounts')
        connected = user.get('connected_email') or {}
        if self.case == 'COA-9794' and not self.expected_email:
            require(not connected.get('email') and not (user.get('analytics_profile') or {}).get('email'),
                    'The anonymous progress fixture cannot be a registered account')
            return user
        require(isinstance(connected.get('email'), str)
                and connected['email'].casefold() == self.expected_email.casefold()
                and connected.get('verified') is True and connected.get('user_confirmed') is True,
                'Android does not have the confirmed dedicated QA email')
        return user


def program_id(value):
    return None if value is None else value['program_id']


def secondary_ids(state):
    return [program_id(p) for p in state['secondary']]


def restore(session, original):
    current = session.snapshot()
    if program_id(current['main']) != program_id(original['main']):
        session.post('main_journey/', {'program_id': program_id(original['main'])})
    if secondary_ids(current) != secondary_ids(original):
        session.post('secondary_programs/', {'program_ids': secondary_ids(original)})
    current = session.snapshot()
    if current['active'] != original['active']:
        session.post('active_program/', {'program_id': original['active']})
    require(session.snapshot() == original, 'Original selection/progress was not fully restored')


def run(session, operation, evidence, program=None, recommendation=False, exchange=None, not_before_ms=None, content_route=None):
    require(operation in CHECKS, 'Unsupported program selection check')
    require(not getattr(session, 'read_only', False) or operation == 'new-onboarding-profile',
            'Production sessions allow only the read-only onboarding prerequisite')
    original = session.snapshot()
    evidence['original'] = original
    if operation == 'first-activation':
        require(os.environ.get('COACH_PROGRAM_SELECTION_CASE') == 'COA-9797'
                and getattr(session, 'expected_uid', None) == getattr(session, 'uid', None)
                and bool(getattr(session, 'expected_uid', None)),
                'First activation requires the dedicated COA-9797 UID binding')
        require(isinstance(program, str) and re.fullmatch(r'[a-z0-9_]{1,80}', program), 'Invalid program_id')
        require(original['main'] is None and not original['secondary'], 'First activation requires no saved roles')
        require(original['catalog_membership'].get(program) == 'none', 'First activation requires an available unadded program')
        require(all(original['achievements'].get(k) == 0 for k in
                    ('lessons_completed', 'exercises_completed', 'longest_streak')),
                'First activation cannot consume existing progress')
        response = session.post(program + '/add_and_activate/')
        after = session.snapshot()
        evidence.update(response=response, after=after)
        require(program_id(after['main']) == program and after['active'] == program
                and not after['secondary'], 'First activation did not create Main Journey and Active Program')
        require(after['achievements'] == original['achievements'], 'First activation changed completed content')
        require(response.get('main_journey_program_id') == program
                and response.get('active_program_id') == program and response.get('secondary_program_ids') == [],
                'First activation response disagrees with saved roles')
        return
    if operation == 'observe':
        require(not getattr(session, 'read_only', False), 'Observation runs require preprod')
        evidence['catalog'] = session.get(ROOT + 'catalog/v4/')['programs']
        if recommendation:
            evidence['recommendation'] = session.get(ROOT + 'onboarding/recommended_program/')
        if program is not None:
            require(bool(re.fullmatch(r'[a-z0-9_]{1,80}', program)), 'Invalid program_id')
            evidence['detail'] = session.get(ROOT + 'catalog/v4/' + program + '/')
        if exchange is not None:
            evidence['action_not_before_ms'] = not_before_ms
            evidence['exchange'] = session.exchange(exchange, not_before_ms)
        if content_route is not None:
            require(bool(re.fullmatch(re.escape(ROOT) + r'(?:lessons|exercises|actions)/[A-Za-z0-9_-]{1,128}/?', content_route)),
                    'Supply the observed content endpoint; legacy catalog is not a content access oracle')
            status, _ = session.request(content_route)
            evidence['content_access'] = {'route': content_route, 'http_status': status}
        evidence['read_only'] = True
        return
    if operation in ('new-onboarding-profile', 'secondary-onboarding-profile'):
        if operation == 'new-onboarding-profile':
            require(original['main'] is None and not original['secondary'],
                    'UI selection tests require a new profile without saved program roles')
        else:
            require(isinstance(program, str) and re.fullmatch(r'[a-z0-9_]{1,80}', program), 'Invalid program_id')
            require(program_id(original['main']) == program and not original['secondary'],
                    'Secondary fixture requires only the expected Main Journey draft')
        require(not any(value for key, value in original['selection_flags'].items()
                        if 'complet' in key.lower()), 'UI profile already completed program selection')
        require(all(original['achievements'].get(k) == 0 for k in
                    ('lessons_completed', 'exercises_completed', 'longest_streak')),
                'UI selection tests cannot consume a profile with existing progress')
        if operation == 'new-onboarding-profile':
            require(original.get('subscription_active') is True,
                    'UI profile must have access after a successful test Paywall')
        evidence['read_only'] = True
        return
    if operation == 'complete-without-main':
        require(original['main'] is None and not original['secondary'],
                'COA-9799 B requires a profile without Main Journey or Secondary')
        require(not any(value for key, value in original['selection_flags'].items()
                        if 'complet' in key.lower()),
                'COA-9799 B already completed program selection')
        status, _ = session.request(ROOT + 'onboarding/complete/', post=True)
        after = session.snapshot()
        evidence.update(http_status=status, after=after)
        require(after == original, 'Rejected completion changed account selection/progress')
        require(status == 409, 'Expected HTTP 409 for completion without Main Journey; got ' + str(status))
        return

    require(original['main'] is not None and original['secondary'],
            'COA-9799 A requires Main Journey and at least one Secondary')
    main = program_id(original['main'])
    original_ids = secondary_ids(original)
    require(original['active'] == main, 'COA-9799 A requires Main Journey to be active before the check')
    candidates = sorted(p for p, membership in original['catalog_membership'].items() if membership == 'none')
    secondary = list(original_ids)
    if operation == 'activate-unadded':
        require(candidates, 'COA-9799 A needs a catalog program outside the saved set')
    unadded = candidates[0] if candidates else None
    try:
        before = original
        if operation == 'secondary-includes-main':
            route, payload = 'secondary_programs/', {'program_ids': secondary + [main]}
        else:
            route, payload = 'active_program/', {'program_id': unadded}
        status, _ = session.request(ROOT + route, payload, post=True)
        after = session.snapshot()
        evidence.update(before=before, http_status=status, after=after)
        require(after == before, 'Rejected transition changed account selection/progress')
        require(status == 400, 'Expected HTTP 400 for invalid transition; got ' + str(status))
    finally:
        restore(session, original)
        evidence['restored'] = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=CHECKS)
    parser.add_argument('--device', required=True)
    parser.add_argument('--platform', choices=('ios', 'android'), default='ios')
    parser.add_argument('--bundle', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--allow-production-read', action='store_true')
    parser.add_argument('--program')
    parser.add_argument('--recommendation', action='store_true')
    parser.add_argument('--exchange')
    parser.add_argument('--not-before-ms', type=int)
    parser.add_argument('--content-route')
    args = parser.parse_args()
    case = ('COA-9797' if args.operation == 'first-activation' else
            'program-selection-observation' if args.operation == 'observe' else
            'COA-9790' if args.operation in ('new-onboarding-profile', 'secondary-onboarding-profile') else 'COA-9799')
    evidence = {'case': case, 'operation': args.operation, 'bundle': args.bundle,
                'remote_configuration_changed': False}
    try:
        require(args.exchange is None or is_selection_route(args.exchange), 'Unsupported selection exchange route')
        require(args.platform == 'ios' or (args.operation == 'observe'
                and not args.allow_production_read and not args.exchange),
                'Android supports only read-only preprod observations')
        require(not args.allow_production_read or args.operation == 'new-onboarding-profile',
                'Production opt-in is only for the read-only onboarding prerequisite')
        require(args.operation in ('observe', 'first-activation', 'secondary-onboarding-profile')
                or not (args.program or args.recommendation or args.exchange or args.content_route),
                'Content observations require the read-only observe operation')
        require(args.operation not in ('first-activation', 'secondary-onboarding-profile')
                or args.program and not (args.recommendation or args.exchange or args.content_route),
                'Fixture operations require exactly one expected program_id')
        session = (AndroidSession(args.device, args.bundle) if args.platform == 'android'
                   else Session(args.device, args.bundle, args.allow_production_read))
        run(session, args.operation, evidence,
            args.program, args.recommendation, args.exchange, args.not_before_ms, args.content_route)
        evidence['result'] = 'PASS'
    except Exception as failure:
        evidence['result'] = 'FAIL'
        evidence['error'] = str(failure) if isinstance(failure, (AssertionError, CacheEvidenceError)) else type(failure).__name__
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n')
    if evidence['result'] != 'PASS':
        raise SystemExit('Program selection check failed: ' + evidence['error'])


if __name__ == '__main__':
    main()
