"""Offline guards for the approved Kegel program, no Keychain or network writes."""
import copy
import unittest
from unittest.mock import Mock
from test_model_kegel_fixture import SCOPE, ACTION, require_approval, KegelFixture


class KegelFixtureTest(unittest.TestCase):
    def setUp(self):
        self.account = {"allowedCases": ["COA-8511", "COA-8512"], "status": "identity-and-premium-verified",
                        "kegelMutationScope": dict(SCOPE), "accountDeletionAllowed": False}
        self.client = object.__new__(KegelFixture)
        self.client.case = "COA-8511"
        self.client.account = self.account
        self.client.section = Mock(return_value={"questions": [{"name": ACTION}, {"name": "lesson_unit"}]})
        self.client.opener = Mock()

    def test_only_two_approved_cases(self):
        for case in ("COA-8511", "COA-8512"): require_approval(case, self.account)
        for case in ("COA-8517", "COA-8518"):
            with self.assertRaises(RuntimeError): require_approval(case, self.account)

    def test_overall_health_approval_does_not_grant_kegel_permission(self):
        account = copy.deepcopy(self.account)
        account["kegelMutationScope"]["program"] = "overall_health"
        with self.assertRaises(RuntimeError): require_approval("COA-8511", account)

    def test_no_other_program_or_unbounded_day_queries(self):
        for program, day in (("overall_health", 1), ("maintenance_pe", 1), ("kegel_only", 4), ("kegel_only", True)):
            with self.assertRaises(RuntimeError): self.client.get("/api/v1/daily_program/v2/", program_id=program, day_of_program=day)
        self.client.opener.open.assert_not_called()

    def test_second_move_cannot_be_done_by_setup_api(self):
        with self.assertRaises(RuntimeError): self.client._setup_write("move_action_forward", ACTION, 2)
        self.client.opener.open.assert_not_called()

    def test_target_cannot_be_marked_complete(self):
        with self.assertRaises(RuntimeError): self.client._setup_write("action_completed", ACTION, 2)
        self.client.opener.open.assert_not_called()

    def test_absent_card_cannot_be_mutated(self):
        with self.assertRaises(RuntimeError): self.client._setup_write("action_completed", "unknown", 1)
        self.client.opener.open.assert_not_called()

    def test_target_must_be_unique_and_modifiable(self):
        card = {"name": ACTION, "allow_modification": True}
        self.assertEqual(card, KegelFixture.target({"questions": [card]}))
        for cards in ([], [card, card], [dict(card, allow_modification=False)]):
            with self.assertRaises(RuntimeError): KegelFixture.target({"questions": cards})

    def test_scheduled_twin_cannot_replace_the_catch_up_card(self):
        scheduled = {"name": ACTION, "allow_modification": True, "section_id": 1}
        moved = dict(scheduled, section_id=3)
        self.assertEqual(moved, KegelFixture.target({"questions": [scheduled, moved]}, catch_up=True))
        with self.assertRaises(RuntimeError): KegelFixture.target({"questions": [scheduled]}, catch_up=True)

    def test_one_pending_task_is_validated_not_just_one_modifiable_task(self):
        client = object.__new__(KegelFixture)
        client.case = "COA-8512"
        states = {
            1: {"module_total_days": 9, "questions": [{"name": ACTION, "headline": "Resistance Kegel Training", "allow_modification": True, "completed": False}, {"name": "lesson", "completed": False}]},
            2: {"module_total_days": 9, "questions": [{"name": ACTION, "headline": "Resistance Kegel Training", "section_id": 3, "allow_modification": True, "completed": False}, {"name": "workout", "allow_modification": False, "completed": False}]}}
        client.section = lambda day: copy.deepcopy(states[day])
        def write(operation, name, day):
            for q in states[day]["questions"]:
                if q["name"] == name:
                    if operation == "move_action_forward": q["is_moved_forward"] = True
                    # Simulate an unsuccessful completion of a non-modifiable task.
                    elif name != "workout": q["completed"] = True
        client._setup_write = write
        with self.assertRaisesRegex(RuntimeError, "exactly one remaining task"):
            client.prepare_first_move()


if __name__ == "__main__": unittest.main()
