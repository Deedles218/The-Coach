"""Verify platform identity and rejection of stale/invalid active slide fixtures."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from capture_onboarding_slides_config import export_properties
from capture_android_onboarding_slides_config import capture


class OnboardingSlidesConfigTests(unittest.TestCase):
    def values(self, enabled=True):
        slides = [dict(id=str(i), order=i, header='Header ' + str(i),
                       buttonText='GOT IT', imageUrl='https://example.invalid/image.png')
                  for i in (1, 2)]
        return {'abtest_onboarding_slides': json.dumps({'isEnabled': enabled}),
                'daily_plan_onboarding_slides': json.dumps({'onboarding_slides': slides})}

    def test_android_export_is_bound_to_package_and_does_not_claim_ios_identity(self):
        with tempfile.TemporaryDirectory() as folder, contextlib.redirect_stdout(io.StringIO()):
            source = Path(folder) / 'activate.json'
            source.write_text(json.dumps({'configs': self.values()}))
            output = Path(folder) / 'slides.properties'
            capture('com.vamapps.thecoach', 'BOOST OVERALL HEALTH', output, config_file=source)
            text = output.read_text()
            self.assertIn('appPackage=com.vamapps.thecoach\n', text)
            self.assertNotIn('bundleId=', text)
            self.assertIn('count=2\n', text)

    def test_ios_identity_is_preserved(self):
        with tempfile.TemporaryDirectory() as folder, contextlib.redirect_stdout(io.StringIO()):
            output = Path(folder) / 'slides.properties'
            export_properties(self.values(), 'com.vamapps.The-Coach', 'BOOST OVERALL HEALTH', output)
            self.assertIn('bundleId=com.vamapps.The-Coach\n', output.read_text())

    def test_disabled_config_replaces_stale_success_with_validation_error(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / 'slides.properties'
            output.write_text('count=99\n')
            with self.assertRaisesRegex(ValueError, 'disabled'):
                export_properties(self.values(False), 'com.vamapps.thecoach', 'BOOST OVERALL HEALTH', output, 'android')
            self.assertIn('validationError=', output.read_text())
            self.assertNotIn('count=99', output.read_text())

    def test_goal_filter_rejects_missing_orders(self):
        values = self.values()
        config = json.loads(values['daily_plan_onboarding_slides'])
        config['onboarding_slides'][0]['userGoal'] = 'ANOTHER GOAL'
        values['daily_plan_onboarding_slides'] = json.dumps(config)
        with tempfile.TemporaryDirectory() as folder, self.assertRaisesRegex(ValueError, 'matching orders: \\[2\\]'):
            export_properties(values, 'com.vamapps.thecoach', 'BOOST OVERALL HEALTH', Path(folder) / 'slides.properties', 'android')

    def test_unreadable_release_cache_removes_stale_fixture(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / 'slides.properties'
            output.write_text('count=99\n')
            with patch('capture_android_onboarding_slides_config.subprocess.check_output', side_effect=RuntimeError('not debuggable')):
                with self.assertRaisesRegex(RuntimeError, 'not debuggable'):
                    capture('com.vamapps.thecoach', 'BOOST OVERALL HEALTH', output, serial='emulator-5554')
            self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
