import collections
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
HELPERS = ROOT / 'scripts/ios_regression'


class IOSRegressionTests(unittest.TestCase):
    def load(self, name, argv):
        spec = importlib.util.spec_from_file_location(name, HELPERS / (name + '.py'))
        module = importlib.util.module_from_spec(spec)
        with patch.object(sys, 'argv', argv):
            spec.loader.exec_module(module)
        return module

    def runner(self):
        return self.load('run_regression', ['runner', '--config', str(ROOT / 'docs/ios-regression.example.json')])

    def test_matrix_retains_every_smoke_method_with_separate_reset_modes(self):
        module = self.runner()
        for variant in ('male', 'female'):
            jobs = [job for job in module.JOBS if job[0] == variant and job[1] in ('SmokeTests', 'SmokeCleanInstall')]
            self.assertEqual(2, len(jobs))
            regular, clean = jobs
            self.assertFalse(regular[5])
            self.assertTrue(clean[5])
            methods = regular[2].split('#')[1].split('+') + clean[2].split('#')[1].split('+')
            expected = module.re.findall(r'\bpublic\s+void\s+(test\w+)\s*\(',
                                         (ROOT / 'src/test/java/tests/SmokeTests.java').read_text())
            self.assertEqual(collections.Counter(expected), collections.Counter(methods))

    def test_matrix_has_bookmarks_for_both_variants_and_no_duplicate_job(self):
        module = self.runner()
        keys = [(job[0], job[1]) for job in module.JOBS]
        self.assertEqual(70, len(keys))
        self.assertEqual(len(keys), len(set(keys)))
        for variant in ('male', 'female'):
            self.assertIn((variant, 'BookmarksTests'), keys)

    def test_list_needs_neither_registry_nor_keychain(self):
        result = subprocess.run([sys.executable, str(HELPERS / 'run_regression.py'), '--config',
                                 str(ROOT / 'docs/ios-regression.example.json'), '--only', 'SmokeCleanInstall', '--list'],
                                capture_output=True, text=True, check=True)
        self.assertEqual(2, len(json.loads(result.stdout)))

    def test_selected_unknown_job_is_rejected(self):
        result = subprocess.run([sys.executable, str(HELPERS / 'run_regression.py'), '--config',
                                 str(ROOT / 'docs/ios-regression.example.json'), '--only', 'unknown', '--list'],
                                capture_output=True, text=True)
        self.assertEqual(2, result.returncode)
        self.assertIn('Unknown job labels', result.stderr)

    def test_redaction_removes_otp_and_email_from_child_output(self):
        module = self.runner()
        module.SECRETS.append('fixture-otp-value')
        self.assertEqual('<redacted> <redacted-email>', module.redact('fixture-otp-value test@example.invalid'))

    def test_config_cannot_override_installation_mode_or_store_otp(self):
        for name in ('ios.fullReset', 'coach.bookmarks.content.otp'):
            config = json.loads((ROOT / 'docs/ios-regression.example.json').read_text())
            config['apps']['male']['properties'] = {name: 'placeholder'}
            with tempfile.TemporaryDirectory() as temporary:
                path = Path(temporary) / 'config.json'
                path.write_text(json.dumps(config))
                result = subprocess.run([sys.executable, str(HELPERS / 'run_regression.py'),
                                         '--config', str(path), '--list'], capture_output=True, text=True)
                self.assertEqual(2, result.returncode)

    def test_job_command_applies_clean_reset_and_archives_interrupted_attempt(self):
        module = self.runner()
        with tempfile.TemporaryDirectory() as temporary:
            module.OUT = Path(temporary)
            module.ROOT = Path(temporary)
            module.CONFIG['registrySha256'] = 'registry-digest'
            module.ACCOUNTS = {('male', 'premium_no_pdf'): {'accessConfigured': False}}
            destination = module.OUT / 'male/SmokeCleanInstall'
            destination.mkdir(parents=True)
            (destination / 'old-evidence.txt').write_text('preserve this attempt')
            with patch.object(module, 'environment', return_value={}), patch.object(module, 'persist'), \
                    patch.object(module.subprocess, 'Popen') as popen:
                popen.return_value.stdout = iter([])
                popen.return_value.wait.return_value = 1
                module.run_job('male', 'SmokeCleanInstall',
                               'tests.SmokeTests#testCleanInstallOnboardingLeadsToDailyPlan', fresh=True)
                command = popen.call_args[0][0]
                self.assertIn('-Dios.fullReset=true', command)
                self.assertIn('-Dios.noReset=false', command)
            self.assertEqual(1, len(list((module.OUT / 'attempts').glob('*/old-evidence.txt'))))
            self.assertEqual(1, module.RESULTS[0]['exitCode'])

    def test_report_uses_latest_isolated_attempt_and_separates_offline(self):
        module = self.load('render_results', ['renderer'])
        with tempfile.TemporaryDirectory() as temporary:
            out = Path(temporary)
            jobs = [{'variant': 'male', 'label': 'offline-unit', 'exitCode': 0,
                     'cases': [{'name': 'offline', 'status': 'passed'}]},
                    {'variant': 'female', 'label': 'BookmarksTests', 'exitCode': 1}]
            directory = out / 'female/BookmarksTests/allure-results'
            directory.mkdir(parents=True)
            for number, status in enumerate(('failed', 'passed')):
                (directory / ('%s-result.json' % number)).write_text(json.dumps({
                    'fullName': 'tests.BookmarksTests.example', 'name': 'example', 'status': status,
                    'stop': number, 'parameters': [], 'labels': []}))
            (out / 'status.json').write_text(json.dumps({'completed': jobs, 'planned': 2, 'active': None}))
            summary = module.render(out)
            self.assertEqual({'passed': 1}, summary['offlineCounts'])
            self.assertEqual({'passed': 1}, summary['uiCounts'])
            self.assertEqual(1, summary['jobs'][1]['exitCode'])
            self.assertTrue((out / 'report.html').is_file())

    def test_allure_merge_keeps_build_histories_separate_and_archives_old_aggregate(self):
        with tempfile.TemporaryDirectory() as temporary:
            out = Path(temporary)
            config = json.loads((ROOT / 'docs/ios-regression.example.json').read_text())
            (out / 'run-config.json').write_text(json.dumps(config))
            (out / 'allure-results').mkdir()
            (out / 'allure-results/stale.txt').write_text('previous aggregation')
            originals = []
            for variant in ('male', 'female'):
                directory = out / variant / 'SmokeTests/allure-results'
                directory.mkdir(parents=True)
                path = directory / (variant + '-result.json')
                path.write_text(json.dumps({'historyId': 'shared-history', 'parameters': [], 'labels': []}))
                originals.append(path.read_text())
            with patch('subprocess.run') as generate:
                self.load('finalize_allure', ['finalizer', str(out)])
                self.assertEqual('allure', generate.call_args[0][0][0])
            male = json.loads((out / 'allure-results/male-result.json').read_text())
            female = json.loads((out / 'allure-results/female-result.json').read_text())
            self.assertNotEqual(male['historyId'], female['historyId'])
            self.assertEqual(1, len(list((out / 'attempts').glob('*/stale.txt'))))
            for index, variant in enumerate(('male', 'female')):
                path = out / variant / 'SmokeTests/allure-results' / (variant + '-result.json')
                self.assertEqual(originals[index], path.read_text())


if __name__ == '__main__':
    unittest.main()
