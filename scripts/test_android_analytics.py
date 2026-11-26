"""Reject stale events and events from another account/program before reporting UI analytics."""
import unittest
import json
from pathlib import Path
from unittest.mock import patch,call
import observe_android_event as observer
from observe_android_event import matching, fingerprint

class AndroidAnalyticsTest(unittest.TestCase):
    def setUp(self):
        self.event=dict(user_id='fixture',event_type='ProgramModalSelect',time=2000,insert_id='fresh',
                        event_properties=dict(program_id='keep_it_hard'))
    def matches(self,event=None,baseline=frozenset()):
        return matching(self.event if event is None else event,'fixture','ProgramModalSelect',1900,baseline,'keep_it_hard')
    def test_accepts_fresh_exact_event(self): self.assertTrue(self.matches())
    def test_rejects_other_identity(self): self.assertFalse(self.matches(dict(self.event,user_id='other')))
    def test_rejects_old_timestamp(self): self.assertFalse(self.matches(dict(self.event,time=1899)))
    def test_rejects_already_queued_event(self): self.assertFalse(self.matches(baseline={fingerprint(self.event)}))
    def test_rejects_wrong_event(self): self.assertFalse(self.matches(dict(self.event,event_type='ProgramModalOpen')))
    def test_rejects_wrong_destination(self):
        self.assertFalse(self.matches(dict(self.event,event_properties=dict(program_id='last_longer'))))
    def test_rejects_missing_destination(self): self.assertFalse(self.matches(dict(self.event,event_properties={})))
    def test_rejects_null_properties(self): self.assertFalse(self.matches(dict(self.event,event_properties=None)))
    def test_rejects_missing_timestamp(self): self.assertFalse(self.matches(dict(self.event,time=None)))
    def test_debug_queue_reads_only_the_bound_app_via_run_as(self):
        package='com.vamapps.thecoach'
        with patch.dict('os.environ',{'COACH_ANDROID_EVIDENCE_PACKAGE':package},clear=True), \
                patch.object(observer,'adb',side_effect=[b'new.json\n',json.dumps([self.event]).encode()]) as adb:
            self.assertEqual([self.event],observer.events('Samsung'))
        self.assertEqual([call('Samsung','exec-out','run-as',package,'ls','app_amplitude-disk-queue/'),
                          call('Samsung','exec-out','run-as',package,'cat','app_amplitude-disk-queue/new.json')],adb.call_args_list)
    def test_root_emulator_queue_path_remains_available(self):
        with patch.dict('os.environ',{},clear=True), \
                patch.object(observer,'adb',side_effect=[b'new.json\n',json.dumps([self.event]).encode()]) as adb:
            self.assertEqual([self.event],observer.events('emulator-5554'))
        self.assertEqual(call('emulator-5554','exec-out','ls',observer.QUEUE),adb.call_args_list[0])
    def test_other_app_cannot_be_used_as_debug_analytics_source(self):
        with patch.dict('os.environ',{'COACH_ANDROID_EVIDENCE_PACKAGE':'another.app'},clear=True), \
                patch.object(observer,'adb') as adb:
            with self.assertRaisesRegex(RuntimeError,'Unsupported debug'):observer.events('Samsung')
            adb.assert_not_called()
    def test_partial_and_disappeared_queue_files_do_not_hide_a_complete_event(self):
        with patch.dict('os.environ',{'COACH_ANDROID_EVIDENCE_PACKAGE':'com.vamapps.thecoach'},clear=True), \
                patch.object(observer,'adb',side_effect=[b'partial.tmp\ngone.json\nnew.json\n',b'[',RuntimeError('uploaded'),json.dumps([self.event]).encode()]):
            self.assertEqual([self.event],observer.events('Samsung'))
    def test_wrong_sdk_uid_cannot_read_the_queue_or_arm(self):
        with patch.dict('os.environ',{'COACH_EXPECTED_ANALYTICS_UID':'fixture'},clear=True), \
                patch.object(observer,'session',return_value=('another-user','unused-token')), \
                patch.object(observer,'events') as events:
            with self.assertRaisesRegex(RuntimeError,'Wrong Android analytics fixture UID'):
                observer.observe('Samsung','ProgramModalOpen',Path('unused.json'))
            events.assert_not_called()
    def test_unsupported_payload_diagnostics_contain_no_raw_event_values(self):
        stats={};raw=json.dumps({'user_id':'PRIVATE-USER','private':'DO-NOT-PERSIST'})
        with patch.dict('os.environ',{},clear=True),patch.object(observer,'adb',side_effect=[b'entry.json\n',raw.encode()]):
            self.assertEqual([],observer.events('emulator-5554',stats))
        self.assertEqual(1,stats['nonArrayPayloads']);self.assertEqual(1,stats['fileReads'])
        self.assertNotIn('PRIVATE-USER',json.dumps(stats));self.assertNotIn('DO-NOT-PERSIST',json.dumps(stats))

if __name__=='__main__':unittest.main()
