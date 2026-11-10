#!/usr/bin/env python3
"""Prepare independent debug accounts and run only the six approved COA-9222 cases.

Requires an explicit dedicated-test-installation reset flag. Never run against personal data.
Appium must use skipLogcatCapture=true: these preprod debug builds log authentication traffic.
Credentials are supplied through existing project environment variables or local macOS Keychain.
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
import xml.etree.ElementTree as ET

from read_android_email_enforcement import installed_build, session, read
from read_android_module_content import adb
from prepare_android_email_enforcement import change

ROOT = Path(__file__).resolve().parents[1]
METHODS = {
    '9411': 'testCoa9411EmailConfirmationPreservesAccount',
    '9424': 'testCoa9424OccupiedEmailOffersLogin',
    '9702': 'testCoa9702SupportReturnsToMandatoryFlow',
    '9696': 'testCoa9696ProgressiveEnforcement',
    '9699': 'testCoa9699KillAdvancesStage',
    '9701': 'testCoa9701RepeatedDailyDoesNotAdvanceStage',
}
DATASETS = [('9411', 'N', 'cross'), ('9424', 'N', 'cross'), ('9702', 'N', 'cross'),
            ('9696', 'E1', 'cross'), ('9696', 'E1', 'later'),
            ('9699', 'E1', 'cross'), ('9699', 'E2', 'cross'), ('9699', 'E3', 'cross'),
            ('9701', 'E1', 'cross'), ('9701', 'E2', 'cross')]


def credentials():
    email = os.environ.get('COACH_EXISTING_PROGRESS_EMAIL')
    otp = os.environ.get('COACH_EXISTING_PROGRESS_OTP')
    if not email or not otp:
        from run_test_model_local import keychain, SERVICE
        record = json.loads(keychain(SERVICE))['COA-8235']
        email, otp = record['email'], record['otp']
    return email, otp


def run_maven(args, env, output, secrets):
    with output.open('w') as log:
        process = subprocess.Popen(['mvn', '-q', 'test', *args], cwd=ROOT, env=env,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            for secret in secrets:
                if secret:
                    line = line.replace(secret, '<redacted>')
            log.write(line)
            log.flush()
        return process.wait()


def execute(args, variant, case, stage, action, email, otp):
    package = 'com.vamapps.thecoach' + ('.forher' if variant == 'her' else '')
    expected_version = '1.44.1' if variant == 'him' else '2.19.1'
    build = installed_build(args.device, package)
    if build['version'] != expected_version or build['build'] != '353':
        raise ValueError('Fixture template does not match the installed reviewed build')
    name = f'{variant}-{case}-{stage}-{action}'
    output = args.output / name
    output.mkdir(parents=True, exist_ok=False)
    result = {'dataset': name, 'case': 'COA-' + case, 'stage': stage, 'variant': variant, 'build': build,
              'startedAt': datetime.now(timezone.utc).isoformat()}
    # A new independent UI-created anonymous account per dataset; no paid action is performed.
    adb(args.device, 'shell', 'am', 'force-stop', package)
    if adb(args.device, 'shell', 'pm', 'clear', package).decode().strip() != 'Success':
        raise ValueError('Dedicated test installation reset failed')
    env = os.environ.copy()
    env.pop('COACH_EMAIL_ENFORCEMENT_UID', None)
    local, domain = email.split('@', 1)
    free_email = local.split('+', 1)[0] + '+coa9222a' + uuid.uuid4().hex[:12] + '@' + domain
    env.update(COACH_EXISTING_PROGRESS_EMAIL=email, COACH_EXISTING_PROGRESS_OTP=otp,
               COACH_VALID_EMAIL_WITHOUT_PROGRESS=free_email)
    common = ['-Dplatform=android', '-Dandroid.udid=' + args.device,
              '-Dandroid.deviceName=COA-9222 test Android', '-Dandroid.platformVersion=13',
              '-Dandroid.appPackage=' + package, '-Dandroid.appActivity=com.vamapps.thecoach.MainActivity',
              '-Dandroid.noReset=true', '-Dandroid.fullReset=false', '-Dappium.url=' + args.appium,
              '-Donboarding.goal=' + ('Cultivate desire' if variant == 'her' else 'Beat premature ejaculation')]
    try:
        prep = run_maven(common + ['-Dtest=tests.AndroidEmailEnforcementPreparation',
                         '-Dcoach.emailEnforcement.prepareStage=' + stage,
                         '-Dcoach.emailEnforcement.output=' + str(output),
                         '-Dallure.results.directory=' + str(output / 'preparation-allure')],
                         env, output / 'preparation.log', [email, otp, free_email])
        result['preparationExit'] = prep
        if prep:
            result['status'] = 'PREPARATION_FAILED'
            return result
        uid, _ = session(args.device, package)
        env['COACH_EMAIL_ENFORCEMENT_UID'] = uid
        baseline = read(args.device, package, 'account', uid)
        if baseline['emailPresent'] or baseline['emailVerified']:
            raise ValueError('Prepared account is not anonymous/unlinked')
        (output / 'baseline.json').write_text(json.dumps(baseline, indent=2))
        template = (ROOT / f'src/test/resources/email-enforcement/android-preprod-{variant}-353.properties').read_text()
        fixture = output / 'fixture.properties'
        fixture.write_text(template + f'\ncase=COA-{case}\nstage={stage}\ndismiss.action={action}\n')
        code = run_maven(common + ['-Dtest=tests.AndroidEmailEnforcementTests#' + METHODS[case],
                         '-Dcoach.emailEnforcement.fixture=' + str(fixture),
                         '-Dallure.results.directory=' + str(output / 'allure-results')],
                         env, output / 'test.log', [email, otp, free_email, uid])
        result['testExit'] = code
        result['status'] = 'PASSED' if code == 0 else 'FAILED'
        report = ROOT / 'target/surefire-reports/TEST-tests.AndroidEmailEnforcementTests.xml'
        if report.exists():
            report_text = report.read_text()
            for secret in (email, otp, free_email, uid):
                report_text = report_text.replace(secret, '&lt;redacted&gt;')
            (output / 'junit.xml').write_text(report_text)
            root = ET.fromstring(report_text)
            failures = root.findall('.//failure') + root.findall('.//error')
            if failures:
                result['failure'] = failures[0].get('message', '').split('\n')[0]
        try:
            (output / 'after.json').write_text(json.dumps(read(args.device, package, 'account', uid), indent=2))
        except Exception as error:
            result['afterEvidenceError'] = type(error).__name__
        return result
    finally:
        seed = output / 'seed.json'
        if seed.exists():
            adb(args.device, 'shell', 'am', 'force-stop', package)
            try:
                restored = change(args.device, package, 'restore', seed, env.get('COACH_EMAIL_ENFORCEMENT_UID'))
                (output / 'restored.json').write_text(json.dumps(restored, indent=2))
                result['markerRestored'] = True
            except Exception as error:
                result['markerRestored'] = False
                result['restoreError'] = type(error).__name__
        result['finishedAt'] = datetime.now(timezone.utc).isoformat()
        (output / 'result.json').write_text(json.dumps(result, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--variant', choices=('him', 'her', 'both'), default='both')
    parser.add_argument('--dataset', help='One dataset, e.g. 9424-N-cross')
    parser.add_argument('--appium', default='http://127.0.0.1:4725/')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--reset-dedicated-test-installation', action='store_true', required=True)
    args = parser.parse_args()
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=True)
    selected = [d for d in DATASETS if not args.dataset or '-'.join(d) == args.dataset]
    if not selected:
        parser.error('Unknown or excluded dataset')
    email, otp = credentials()
    results = []
    for variant in ('him', 'her') if args.variant == 'both' else (args.variant,):
        for case, stage, action in selected:
            print(f'START {variant}-{case}-{stage}-{action}', flush=True)
            result = execute(args, variant, case, stage, action, email, otp)
            results.append(result)
            (args.output / 'summary.json').write_text(json.dumps(results, indent=2))
            print(json.dumps(result), flush=True)
            if result.get('markerRestored') is False:
                raise SystemExit('Fixture restore failed; stop before any next reset')
            if result['status'] == 'PREPARATION_FAILED':
                raise SystemExit('Fixture preparation failed; inspect prerequisites before any next reset')
    return int(any(r['status'] != 'PASSED' for r in results))


if __name__ == '__main__':
    sys.exit(main())
