"""Reject stale events and events from another account/program before reporting UI analytics."""
import unittest
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

if __name__=='__main__':unittest.main()
