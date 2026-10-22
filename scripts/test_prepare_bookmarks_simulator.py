import json
from pathlib import Path
import plistlib
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from prepare_bookmarks_simulator import reinstall_app


class SimulatorPreparationTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.app = Path(self.directory.name) / 'Coach.app'
        self.app.mkdir()
        (self.app / 'Info.plist').write_bytes(plistlib.dumps({'CFBundleIdentifier': 'test.coach'}))

    def devices(self, state='Booted'):
        return subprocess.CompletedProcess([], 0, json.dumps({'devices': {'iOS': [
            {'udid': 'dedicated', 'state': state, 'isAvailable': True}]}}))

    @patch('prepare_bookmarks_simulator.subprocess.run')
    def test_bundle_mismatch_cannot_mutate_any_simulator(self, run):
        with self.assertRaisesRegex(ValueError, 'does not match'):
            reinstall_app('dedicated', 'other.bundle', self.app)
        run.assert_not_called()

    @patch('prepare_bookmarks_simulator.subprocess.run')
    def test_unbooted_simulator_is_rejected_before_mutation(self, run):
        run.return_value = self.devices('Shutdown')
        with self.assertRaisesRegex(ValueError, 'already be booted'):
            reinstall_app('dedicated', 'test.coach', self.app)
        self.assertEqual(1, run.call_count)

    @patch('prepare_bookmarks_simulator.subprocess.run')
    def test_reinstall_changes_only_selected_bundle_and_never_reboots(self, run):
        run.side_effect = [self.devices(), subprocess.CompletedProcess([], 0, '/container'),
                           subprocess.CompletedProcess([], 0), subprocess.CompletedProcess([], 0),
                           subprocess.CompletedProcess([], 0)]
        reinstall_app('dedicated', 'test.coach', self.app)
        commands = [call.args[0] for call in run.call_args_list]
        self.assertEqual(['xcrun', 'simctl', 'terminate', 'dedicated', 'test.coach'], commands[2])
        self.assertEqual(['xcrun', 'simctl', 'uninstall', 'dedicated', 'test.coach'], commands[3])
        self.assertEqual(['xcrun', 'simctl', 'install', 'dedicated', str(self.app.resolve())], commands[4])

    @patch('prepare_bookmarks_simulator.subprocess.run')
    def test_absent_app_is_confirmed_before_install_without_uninstall(self, run):
        run.side_effect = [self.devices(), subprocess.CompletedProcess([], 1),
                           subprocess.CompletedProcess([], 0, b'{}'),
                           subprocess.CompletedProcess([], 0, b'{}'),
                           subprocess.CompletedProcess([], 0)]
        reinstall_app('dedicated', 'test.coach', self.app)
        commands = [call.args[0] for call in run.call_args_list]
        self.assertEqual(['xcrun', 'simctl', 'listapps', 'dedicated'], commands[2])
        self.assertFalse(any('uninstall' in command for command in commands))
        self.assertEqual('install', commands[-1][2])

    @patch('prepare_bookmarks_simulator.subprocess.run')
    def test_unavailable_service_does_not_install(self, run):
        run.side_effect = [self.devices(), subprocess.CompletedProcess([], 1),
                           subprocess.CalledProcessError(1, ['xcrun', 'simctl', 'listapps'])]
        with self.assertRaises(subprocess.CalledProcessError):
            reinstall_app('dedicated', 'test.coach', self.app)
        self.assertEqual(3, run.call_count)


if __name__ == '__main__':
    unittest.main()
