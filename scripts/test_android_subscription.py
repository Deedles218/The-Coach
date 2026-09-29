import io
import json
import unittest
from unittest.mock import patch, MagicMock
import read_android_subscription as billing
import read_android_module_content as sessions
import test_android_module_content as fixtures

class AndroidSubscriptionEvidenceTests(unittest.TestCase):
    def read(self, state):
        opener=MagicMock()
        opener.open.return_value=io.StringIO(json.dumps({'userdata':{'subscription':state}}))
        with patch.object(billing,'session',return_value=('new-fixture','token')), patch.object(billing.urllib.request,'build_opener',return_value=opener):
            return billing.read('emulator-5554','new-fixture')
    def test_inactive_fixture_cannot_count_as_paid(self): self.assertIs(False,self.read({'active':False})['active'])
    def test_active_state_is_preserved(self): self.assertIs(True,self.read({'active':True})['active'])
    def test_missing_subscription_is_not_success(self):
        with self.assertRaisesRegex(RuntimeError,'missing'):self.read({})
    def test_string_true_is_not_a_verified_boolean(self):
        with self.assertRaisesRegex(RuntimeError,'missing'):self.read({'active':'true'})
    def test_wrong_account_is_rejected_before_network_access(self):
        with patch.object(billing,'session',return_value=('other','token')),patch.object(billing.urllib.request,'build_opener') as network:
            with self.assertRaisesRegex(RuntimeError,'identity'):billing.read('emulator-5554','expected')
            network.assert_not_called()
    def test_anonymous_session_requires_explicit_opt_in(self):
        store=fixtures.AndroidModuleEvidenceTests().store('fixtureIdentity123456789',anonymous=True)
        with patch.object(sessions,'adb',side_effect=[b'com.google.firebase.auth.api.Store.fixture.xml\n',store]):
            self.assertEqual('fixtureIdentity123456789',sessions.session('emulator-5554',allow_anonymous=True)[0])

if __name__=='__main__':unittest.main()
