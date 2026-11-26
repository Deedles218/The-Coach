"""Destination evidence uses the selected APK/environment and the same named fixture."""
import io,json,os,time,unittest
from unittest.mock import patch
import read_android_program_content as reader

class DestinationEvidenceTests(unittest.TestCase):
    def read(self,debug=True,email='fixture@example.test'):
        env={'COACH_EXPECTED_ACCOUNT_UID':'named-fixture','COACH_EXISTING_PROGRESS_EMAIL':'fixture@example.test'}
        if debug:env['COACH_ANDROID_EVIDENCE_PACKAGE']='com.vamapps.thecoach'
        requests=[]
        def response(req,timeout):
            requests.append(req)
            data={'userdata':{'analytics_profile':{'email':email}}} if req.full_url.endswith('/api/v1/user/') else {'catalog':'fixture'}
            return io.BytesIO(json.dumps(data).encode())
        with patch.dict(os.environ,env,clear=True),patch.object(reader,'session',return_value=('named-fixture','unused-token')), \
             patch('read_android_email_enforcement.installed_build',return_value={'version':'1.45.6','build':'357'}) as build, \
             patch.object(reader.urllib.request,'build_opener') as opener,patch.object(reader,'_oracle',return_value={'checked':True}):
            opener.return_value.open.side_effect=response
            result=reader.read('qa-device','keep_it_hard',int(time.time()*1000))
        return requests,result,build
    def test_debug_requests_use_current_artifact_headers_and_preprod_host(self):
        requests,result,build=self.read()
        self.assertTrue(result['checked']);build.assert_called_once_with('qa-device','com.vamapps.thecoach')
        self.assertEqual('1.45.6',requests[0].get_header('Appversion'));self.assertEqual('357',requests[0].get_header('Buildversion'))
        self.assertTrue(all(r.full_url.startswith('https://coach-preprod-cf87bfd42b85.herokuapp.com/') for r in requests))
    def test_legacy_root_emulator_headers_and_host_are_preserved(self):
        requests,_,build=self.read(debug=False);build.assert_not_called()
        self.assertEqual('1.40.21',requests[0].get_header('Appversion'));self.assertEqual('339',requests[0].get_header('Buildversion'))
        self.assertTrue(all(r.full_url.startswith('https://content.the.coach/') for r in requests))
    def test_wrong_backend_email_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError,'Wrong fixture email'):self.read(email='another@example.test')
    def test_other_app_cannot_read_the_male_destination(self):
        with patch.dict(os.environ,{'COACH_ANDROID_EVIDENCE_PACKAGE':'com.vamapps.thecoach.forher'},clear=True),patch.object(reader,'session') as session:
            with self.assertRaisesRegex(RuntimeError,'male app'):reader.read('qa-device','keep_it_hard',0)
            session.assert_not_called()
    def test_unreviewed_destination_is_rejected_before_device_access(self):
        with patch.object(reader,'session') as session:
            with self.assertRaisesRegex(RuntimeError,'Unreviewed'):reader.read('qa-device','another_program',0)
            session.assert_not_called()

if __name__=='__main__':unittest.main()
