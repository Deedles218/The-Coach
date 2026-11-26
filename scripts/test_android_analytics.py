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
    def queue(self,name,payload,stats=None):
        with patch.dict('os.environ',{'COACH_ANDROID_EVIDENCE_PACKAGE':'com.vamapps.thecoach'},clear=True), \
                patch.object(observer,'adb',side_effect=[(name+'\n').encode(),payload]):
            return observer.events('Samsung',stats)
    def test_complete_records_in_an_open_sdk_batch_are_read(self):
        raw=json.dumps([self.event]).encode()[:-1];stats={}
        self.assertEqual([self.event],self.queue('batch.tmp',raw,stats))
        self.assertEqual(1,stats['openBatchSnapshots'])
    def test_a_partial_event_in_an_open_batch_is_not_accepted(self):
        self.assertEqual([],self.queue('batch.tmp',b'[{"event_type":"ProgramModalOpen"'))
    def test_an_open_non_temporary_file_is_not_repaired(self):
        self.assertEqual([],self.queue('batch.json',json.dumps([self.event]).encode()[:-1]))
    def test_malformed_complete_payload_is_not_repaired(self):
        self.assertEqual([],self.queue('batch.tmp',json.dumps([self.event]).encode()+b'garbage'))
    def test_preexisting_open_batch_event_remains_in_the_baseline(self):
        rows=self.queue('batch.tmp',json.dumps([self.event]).encode()[:-1])
        self.assertFalse(self.matches(baseline={fingerprint(x) for x in rows}))
    def test_open_batch_diagnostics_do_not_store_event_values(self):
        stats={};self.queue('batch.tmp',json.dumps([self.event]).encode()[:-1],stats)
        self.assertNotIn('fixture',json.dumps(stats));self.assertNotIn('keep_it_hard',json.dumps(stats))
    def test_incomplete_later_record_does_not_export_the_complete_prefix(self):
        prefix=json.dumps([self.event]).encode()[:-1]
        for suffix in (b',', b',{"user_id":"PRIVATE', b',{"nested":[1,2', b',{"value":tru'):
            with self.subTest(suffix=suffix):
                stats={}
                self.assertEqual([],self.queue('batch.tmp',prefix+suffix,stats))
                self.assertEqual(1,stats['unparsedFiles'])
                self.assertNotIn('PRIVATE',json.dumps(stats))
    def test_non_json_constants_are_rejected_without_exporting_the_prefix(self):
        prefix=json.dumps([self.event]).encode()[:-1]
        for constant in (b'NaN', b'Infinity', b'-Infinity'):
            for suffix in (b'', b']'):
                with self.subTest(constant=constant,suffix=suffix):
                    self.assertEqual([],self.queue('batch.tmp',prefix+b',{"time":'+constant+b'}'+suffix))
    def test_broken_utf8_does_not_hide_a_later_valid_queue_file(self):
        stats={}
        with patch.dict('os.environ',{},clear=True),patch.object(observer,'adb',side_effect=[
                b'broken.tmp\nvalid.json\n', b'[{"private":"\xff"}', json.dumps([self.event]).encode()]):
            self.assertEqual([self.event],observer.events('emulator-5554',stats))
        self.assertEqual(1,stats['unparsedFiles'])
        self.assertEqual(2,stats['fileReads'])
    def test_malformed_complete_objects_and_trailing_content_are_rejected(self):
        prefix=json.dumps([self.event]).encode()[:-1]
        for suffix in (b',{"value":}', b',{"value":1,}', b']\n{"secret":"PRIVATE"}', b',{}]garbage'):
            with self.subTest(suffix=suffix):
                self.assertEqual([],self.queue('batch.tmp',prefix+suffix))
    def test_whitespace_around_a_complete_open_batch_is_allowed(self):
        self.assertEqual([self.event],self.queue('batch.tmp',b' \n'+json.dumps([self.event]).encode()[:-1]+b' \n'))
    def test_failed_open_batch_diagnostics_do_not_persist_secrets(self):
        stats={}
        self.assertEqual([],self.queue('batch.tmp',b'[{"token":"DO-NOT-PERSIST","user_id":"PRIVATE-USER"',stats))
        self.assertEqual(1,stats['unparsedFiles'])
        self.assertNotIn('PRIVATE-USER',json.dumps(stats));self.assertNotIn('DO-NOT-PERSIST',json.dumps(stats))

if __name__=='__main__':unittest.main()
