#!/usr/bin/env python3
"""Sequential iOS regression; credentials only enter child environments."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HELPERS = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'scripts'))
from run_test_model_local import keychain

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--config', type=Path, required=True, help='Local run configuration; see docs/ios-regression.example.json')
parser.add_argument('--output', type=Path, help='Output directory; reuse it with --resume')
parser.add_argument('--resume', action='store_true')
parser.add_argument('--list', action='store_true', help='Show jobs without reading credentials or starting tests')
parser.add_argument('--only', action='append', help='Run only the specified job label; repeatable')
args = parser.parse_args()
CONFIG = json.loads(args.config.read_text())
if not CONFIG.get('runId'):
    parser.error('Set a unique runId; keep it unchanged for --resume')
OUT = (args.output or ROOT / 'target' / CONFIG['runId']).resolve()
REGISTRY = []
ACCOUNTS = {}
APPS = {variant: (row['app'], row['oldApp'], row['bundleId'], row['version'], row['oldVersion'])
        for variant, row in CONFIG['apps'].items()}
if not APPS or not set(APPS) <= {'male', 'female'}:
    parser.error('apps must contain male and/or female')
for app in CONFIG['apps'].values():
    groups = [app.get('properties', {})] + list(app.get('suiteProperties', {}).values())
    for properties in groups:
        for name in properties:
            if name.startswith(('ios.', 'allure.', 'appium.')) or name in ('test', 'platform'):
                parser.error('Use top-level run settings for capability/output parameter: ' + name)
            if name.lower().endswith(('.email', '.otp', '.token', '.password')):
                parser.error('Supply credentials through the environment or Keychain, not properties')
COMMON = [
    'IOSSmokeTests', 'CoachAuthorizationTests', 'CoachProfileTests',
    'CoachAuthorizationFlowTests', 'DailyPlanTests', 'SmokeTests',
    'ReleaseSmokeTests', 'ConfiguredExploreTests', 'ExploreTests', 'BookmarksTests',
    'KegelExerciseTests', 'KegelOctoberCasesTests', 'TodayProgramSelectorTests',
    'TestModelAutomationTests', 'ModulesTests',
    'VideoPlayerFeatureTests', 'PdfGuideTests', 'PushNotificationTests',
    'StoreKitPurchaseTests', 'ProfileTests', 'ChatTests', 'SearchTests',
]
FRESH = ['OnboardingGoalProgramTests', 'ProgramSwitchFeatureTests',
         'OnboardingSlidesTests', 'PushPermissionTests']
RESULTS = []
SECRETS = []

def redact(text):
    for secret in sorted(set(SECRETS), key=len, reverse=True):
        if secret:
            text = text.replace(secret, '<redacted>')
    return re.sub(r'[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}', '<redacted-email>', text)

def persist(active=None):
    payload = {'updated': datetime.datetime.now(datetime.timezone.utc).isoformat(),
               'controllerPid': os.getpid(), 'runId': CONFIG['runId'],
               'active': active, 'completed': RESULTS, 'planned': len(JOBS),
               'revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()}
    temporary = OUT / 'status.tmp'
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2))
    temporary.replace(OUT / 'status.json')
    renderer = HELPERS / 'render_results.py'
    if renderer.exists():
        subprocess.run([sys.executable, str(renderer), str(OUT)], cwd=ROOT,
                       stdout=subprocess.DEVNULL, check=True)

def environment(variant, role='premium_no_pdf'):
    env = os.environ.copy()
    # These accounts were all registered and re-logged-in using this approved OTP service.
    otp = keychain(CONFIG['otpService'])
    SECRETS.extend([otp] + [row['email'] for row in REGISTRY])
    account = ACCOUNTS[(variant, role)]
    env.update(COACH_EXISTING_PROGRESS_EMAIL=account['email'],
               COACH_EXISTING_PROGRESS_OTP=otp,
               COACH_FIXTURE_RESET_MODE='prebuilt',
               COACH_DAILY_PLAN_DAY=CONFIG['apps'][variant]['dailyPlanDay'],
               COACH_ACCOUNT_VARIANT=variant, TEST_ISOLATION_MODE='logout',
               APPIUM_URL=CONFIG['appiumUrl'])
    env['COACH_VALID_EMAIL_WITHOUT_PROGRESS'] = ACCOUNTS[(variant, 'fresh_program_tooltip')]['email']
    env['COACH_KEGEL_PLAYER_EMAIL'] = ACCOUNTS[(variant, 'kegel')]['email']
    env['COACH_KEGEL_PLAYER_OTP'] = otp
    service = CONFIG['apps'][variant]['existingAccountService']
    kegel_account = json.loads(keychain(service))
    if not kegel_account.get('email') or not kegel_account.get('otp'):
        raise RuntimeError('Existing-progress Keychain record requires email and otp')
    if kegel_account.get('variant', variant) != variant:
        raise RuntimeError('Existing-progress account variant does not match the build')
    env['COACH_KEGEL_PLAYER_EMAIL'] = kegel_account['email']
    env['COACH_KEGEL_PLAYER_OTP'] = kegel_account['otp']
    SECRETS.extend([kegel_account['email'], kegel_account['otp']])
    env['COACH_NO_PDF_ENTITLEMENT_EMAIL'] = ACCOUNTS[(variant, 'premium_no_pdf')]['email']
    env['COACH_NO_PDF_ENTITLEMENT_OTP'] = otp
    env['COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID'] = CONFIG['runId'] + '-' + variant + '-premium_no_pdf'
    for key in ['COA8094', 'COA8096', 'COA8170', 'COA8171', 'COA8506']:
        env['COACH_' + key + '_EMAIL'] = env['COACH_KEGEL_PLAYER_EMAIL']
        env['COACH_' + key + '_OTP'] = env['COACH_KEGEL_PLAYER_OTP']
    env['COACH_COA9551_EMAIL'] = ACCOUNTS[(variant, 'video')]['email']
    env['COACH_COA9551_OTP'] = otp
    if (variant, 'sias_marketing') in ACCOUNTS:
        env['COACH_COA7937_EMAIL'] = ACCOUNTS[(variant, 'sias_marketing')]['email']
    return bind_environment(variant, env)


def bind_environment(variant, env):
    # Optional per-variant environment bindings keep Bookmarks/dedicated roles separate.
    for target, source in CONFIG['apps'][variant].get('environmentBindings', {}).items():
        if not target.startswith('COACH_') or not source.startswith('COACH_'):
            raise ValueError('environmentBindings accepts COACH_ variables only')
        if not env.get(source):
            raise RuntimeError('Missing environment variable: ' + source)
        env[target] = env[source]
        if target.endswith(('_EMAIL', '_OTP', '_TOKEN')):
            SECRETS.append(env[target])
    SECRETS.extend(value for name, value in env.items()
                   if name.startswith('COACH_') and name.endswith(('_EMAIL', '_OTP', '_TOKEN')))
    # Dedicated mutable fixtures are supplied explicitly, never inferred from this registry.
    return env

def run_job(variant, label, test, extra=(), old=False, fresh=False, role='premium_no_pdf'):
    app, old_app, bundle, version, old_version = APPS[variant]
    destination = OUT / variant / label
    if destination.exists():
        archive = OUT / 'attempts' / (variant + '-' + label + '-' +
                                    datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
        archive.parent.mkdir(parents=True, exist_ok=True)
        destination.rename(archive)
    destination.mkdir(parents=True, exist_ok=True)
    bookmarks_job = test.split('#', 1)[0] == 'tests.BookmarksTests'
    env = bind_environment(variant, os.environ.copy()) if bookmarks_job else (
        os.environ.copy() if label == 'offline-unit' else environment(variant, role))
    direct_existing_account = test.split('#', 1)[0] in (
        'tests.CoachAuthorizationTests', 'tests.CoachProfileTests')
    if direct_existing_account:
        env['COACH_EXISTING_PROGRESS_EMAIL'] = env['COACH_KEGEL_PLAYER_EMAIL']
        env['COACH_EXISTING_PROGRESS_OTP'] = env['COACH_KEGEL_PLAYER_OTP']
    cmd = ['mvn', '-q', 'test', '-Dtest=' + test, '-Dplatform=ios',
           '-Dappium.url=' + CONFIG['appiumUrl'], '-Dios.isHeadless=true',
           '-Dios.udid=' + CONFIG['deviceUdid'],
           '-Dios.deviceName=' + CONFIG['deviceName'], '-Dios.platformVersion=' + CONFIG['platformVersion'],
           '-Dios.bundleId=' + bundle, '-Dios.app=' + (old_app if old else app),
           '-Dios.noReset=' + str(not fresh).lower(),
           '-Dios.fullReset=' + str(fresh).lower(),
           '-Dallure.results.directory=' + str(destination / 'allure-results')]
    cmd.extend(extra)
    if test.startswith('tests.IOSSmokeTests'):
        cmd.append('-Dcoach.kegelPlayer.program=' + (
            'Kegel Challenge' if variant == 'male' else 'Kegel Journey'))
    if test.split('#', 1)[0] == 'tests.DailyPlanTests':
        practice_fixture = CONFIG['apps'][variant].get('dailyPlanFixture', {})
        cmd.extend('-Dcoach.dailyPlan.' + key + '=' + value for key, value in practice_fixture.items())
    if label != 'offline-unit':
        cmd.extend(['-Dcoach.ios.preparePostAuthQuestionnaire=' + str(not direct_existing_account).lower(),
                    '-Dcoach.ios.postAuthGoal=' + CONFIG['apps'][variant]['postAuthGoal']])
    for key, value in CONFIG['apps'][variant].get('properties', {}).items():
        cmd.append('-D' + key + '=' + str(value))
    for key, value in CONFIG['apps'][variant].get('suiteProperties', {}).get(label, {}).items():
        cmd.append('-D' + key + '=' + str(value))
    if direct_existing_account:
        cmd.append('-Dcoach.ios.quizProfileLogin=false')
    started = time.time()
    record = {'variant': variant, 'label': label, 'test': test, 'command': cmd,
              'appVersion': old_version if old else version, 'bundleId': bundle,
              'accountRole': 'bookmarks-role-fixtures' if bookmarks_job else ('user-selected-existing-progress' if direct_existing_account else role),
              'primaryAccountSource': 'variant-specific Bookmarks environment' if bookmarks_job else (
                  'user-selected Keychain fixture' if direct_existing_account else 'new account registry'),
              'accountRegistrySha256': CONFIG.get('registrySha256'),
              'kegelAccountSource': None if bookmarks_job or label == 'offline-unit' else 'user-selected Keychain fixture',
              'accessConfiguredInRegistry': None if direct_existing_account or bookmarks_job or label == 'offline-unit'
                  else ACCOUNTS[(variant, role)].get('accessConfigured', False),
              'started': datetime.datetime.now(datetime.timezone.utc).isoformat()}
    if test.split('#', 1)[0] == 'tests.SmokeTests':
        record['caseAccountSources'] = {
            'testKegelPlayerFlow': 'user-selected Keychain fixture (Kegel)',
            'testCleanInstallOnboardingLeadsToDailyPlan': 'fresh anonymous onboarding',
        }
    if test.split('#', 1)[0] == 'tests.DailyPlanTests':
        record['caseAccountSources'] = {
            'test05DailyPlanRemoveActionCanBeCancelled': 'user-selected Keychain fixture (COA8506)',
            'test06MandatoryKegelCannotBeCustomized': 'user-selected Keychain fixture (Kegel)',
            'test07DailyLessonCannotBeCustomized': 'new account registry',
        }
        record['practiceFixture'] = practice_fixture
    persist(record)
    print('START', variant, label, flush=True)
    with (destination / 'run.log').open('w') as log:
        process = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            log.write(redact(line))
            log.flush()
        record['exitCode'] = process.wait()
    record['seconds'] = round(time.time() - started, 1)
    record['cases'] = []
    selected_classes = {item.split('#', 1)[0] for item in test.split(',')}
    report_dir = destination / 'surefire-reports'
    report_dir.mkdir(exist_ok=True)
    for path in (ROOT / 'target/surefire-reports').glob('*'):
        if path.is_file() and path.stat().st_mtime >= started:
            raw = path.read_text(errors='replace')
            if path.suffix == '.xml':
                tree = ET.fromstring(raw)
                if tree.tag == 'testsuite' and tree.get('name') not in selected_classes:
                    continue
                for node in tree.iter():
                    node.attrib.update({key: redact(value) for key, value in node.attrib.items()})
                    if node.text:
                        node.text = redact(node.text)
                    if node.tail:
                        node.tail = redact(node.tail)
                content = ET.tostring(tree, encoding='unicode')
            else:
                content = redact(raw)
            (report_dir / path.name).write_text(content)
            if path.name.startswith('TEST-') and path.suffix == '.xml':
                tree = ET.fromstring(content)
                for case in tree.findall('testcase'):
                    problem = case.find('failure')
                    if problem is None:
                        problem = case.find('error')
                    skip = case.find('skipped')
                    status = 'skipped' if skip is not None else ('failed' if problem is not None else 'passed')
                    message = problem.get('message', '') if problem is not None else (skip.get('message', '') if skip is not None else '')
                    record['cases'].append({'class': case.get('classname'), 'name': case.get('name'),
                                            'status': status, 'seconds': case.get('time'),
                                            'message': redact(message)})
    if not record['cases']:
        record['mavenExitCode'] = record['exitCode']
        record['exitCode'] = record['exitCode'] or 2
        record['stage'] = 'before-test-results'
    (destination / 'result.json').write_text(json.dumps(record, ensure_ascii=False, indent=2))
    RESULTS.append(record)
    persist()
    counts = {status: sum(c['status'] == status for c in record['cases'])
              for status in ['passed', 'failed', 'skipped']}
    print('DONE', variant, label, 'exit=' + str(record['exitCode']), counts, flush=True)
    if record['exitCode'] and not record['cases']:
        raise RuntimeError('Maven produced no test cases; queue stopped. Inspect this job before --resume')

JOBS = []
unit_classes = sorted('tests.' + p.stem for p in (ROOT / 'src/test/java/tests').glob('*UnitTests.java'))
JOBS.append((next(iter(APPS)), 'offline-unit', ','.join(unit_classes), (), False, False))
for name in COMMON:
    for variant in APPS:
        extra = []
        if name == 'ModulesTests':
            extra = ['-Dcoach.modules.enabled=true', '-Dcoach.modules.programId=last_longer']
        if name == 'BookmarksTests':
            methods = re.findall(r'\bpublic\s+void\s+(\w+)\s*\(',
                                 (ROOT / 'src/test/java/tests/BookmarksTests.java').read_text())
            separate = {
                'tooltipWaitsForFirstNonEmptyEntry': ('BookmarksTooltipFirstEntry', False, True),
                'anyTapDismissesTooltipAndItDoesNotReturn': ('BookmarksTooltipDismissal', False, True),
                'bookmarksAndTooltipStateAcrossUpdateAndReinstall': ('BookmarksUpdate', True, False),
            }
            assert set(separate) <= set(methods) and len(methods) == len(set(methods))
            JOBS.append((variant, name, 'tests.' + name + '#' + '+'.join(m for m in methods if m not in separate),
                         (), False, False))
            for method, (label, old, fresh) in separate.items():
                parameters = ['-Dios.update.app=' + APPS[variant][0]] if old else []
                JOBS.append((variant, label, 'tests.' + name + '#' + method, parameters, old, fresh))
            continue
        if name == 'SmokeTests':
            methods = re.findall(r'\bpublic\s+void\s+(test\w+)\s*\(',
                                 (ROOT / 'src/test/java/tests/SmokeTests.java').read_text())
            clean = 'testCleanInstallOnboardingLeadsToDailyPlan'
            assert clean in methods and len(methods) == len(set(methods))
            JOBS.append((variant, name, 'tests.' + name + '#' + '+'.join(m for m in methods if m != clean),
                         extra, False, False))
            JOBS.append((variant, 'SmokeCleanInstall', 'tests.' + name + '#' + clean,
                         ['-Donboarding.goal=' + CONFIG['apps'][variant]['postAuthGoal']],
                         False, True))
        else:
            JOBS.append((variant, name, 'tests.' + name, extra, False, False))
for variant in APPS:
    for method, entitlement in [
            ('testUnpaidCourseOpensAndReturns', 'unpaid'),
            ('testPurchasedAccessSurvivesReopenAndRestart', 'purchased'),
            ('testNextDayRemainsLockedBeforeCompletion', 'purchased'),
            ('testPlaybackPauseResumeAndForward', 'purchased')]:
        role = ('basic_partner_buyer' if variant == 'male' else 'masterclass_unpaid_isolated') \
               if entitlement == 'unpaid' else 'masterclass_purchased'
        JOBS.append((variant, 'MasterclassTests-' + method, 'tests.MasterclassTests#' + method,
                     ['-Dmasterclass.login=true', '-Dmasterclass.entitlement=' + entitlement],
                     False, False, role))
for variant in APPS:
    if variant == 'male':
        JOBS.append((variant, 'MaleBuildStartScreenTests', 'tests.MaleBuildStartScreenTests', (), False, True))
    for name in FRESH:
        JOBS.append((variant, name, 'tests.' + name, (), False, True))
    # Existing TestFlight attach guard will report the .app incompatibility.
    JOBS.append((variant, 'OnboardingPaywallTests', 'tests.OnboardingPaywallTests', (), False, False))
    baseline = CONFIG['apps'][variant]['localizationFixture']
    JOBS.append((variant, 'localization', 'tests.LocalizationTests',
                 ['-Dlocalization.variant=' + variant,
                  '-Dlocalization.fixture=' + baseline], False, False))
    JOBS.append((variant, 'update', 'tests.ReleaseSmokeTests#testUpdatePreservesAuthorizationAndProgress',
                 ['-Dios.update.app=' + APPS[variant][0]], True, False))

if __name__ == '__main__':
    if args.only:
        unknown = set(args.only) - {job[1] for job in JOBS}
        if unknown:
            parser.error('Unknown job labels: ' + ', '.join(sorted(unknown)))
        JOBS = [job for job in JOBS if job[1] in args.only]
    if args.list:
        print(json.dumps([{'variant': j[0], 'label': j[1], 'test': j[2]} for j in JOBS], indent=2))
        sys.exit(0)
    needs_registry = any(job[1] != 'offline-unit' and job[2].split('#', 1)[0] != 'tests.BookmarksTests' for job in JOBS)
    if needs_registry:
        REGISTRY = json.loads(Path(CONFIG['registryPath']).read_text())
        ACCOUNTS = {(row['variant'], row['role']): row for row in REGISTRY}
        if len(ACCOUNTS) != len(REGISTRY) or any(row.get('status') != 'verified' for row in REGISTRY):
            raise RuntimeError('Registry must contain unique, verified variant/role records')
        CONFIG['registrySha256'] = hashlib.sha256(Path(CONFIG['registryPath']).read_bytes()).hexdigest()
    else:
        CONFIG['registrySha256'] = None
    CONFIG['selectedJobs'] = [{'variant': job[0], 'label': job[1], 'test': job[2]} for job in JOBS]
    OUT.mkdir(parents=True, exist_ok=True)
    if (OUT / 'status.json').exists() and not args.resume:
        parser.error('Output already contains a run; use --resume or a new --output')
    recorded_config = OUT / 'run-config.json'
    if recorded_config.exists() and json.loads(recorded_config.read_text()) != CONFIG:
        parser.error('Resume requires the same run configuration and account registry')
    recorded_config.write_text(json.dumps(CONFIG, ensure_ascii=False, indent=2))
    if args.resume:
        planned = {(job[0], job[1]) for job in JOBS}
        for path in OUT.glob('*/*/result.json'):
            row = json.loads(path.read_text())
            if (row['variant'], row['label']) in planned and (row.get('cases') or row['exitCode'] == 0):
                RESULTS.append(row)
    done = {(row['variant'], row['label']) for row in RESULTS}
    for job in JOBS:
        if (job[0], job[1]) not in done:
            run_job(*job)
    persist()
    subprocess.run([sys.executable, str(HELPERS / 'finalize_allure.py'), str(OUT)], cwd=ROOT, check=True)
    print('ALL JOBS FINISHED', flush=True)
    sys.exit(1 if any(row['exitCode'] for row in RESULTS) else 0)
