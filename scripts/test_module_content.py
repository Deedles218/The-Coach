"""Module fixture rejection tests using the actual cached-response reader."""
import copy
import unittest

from read_simulator_program_content import EvidenceError, _oracle


class ModuleContentTests(unittest.TestCase):
    def setUp(self):
        self.payload = {
            "program_cover": {"program_id": "last_longer"},
            "program_section": {
                "section_id": "last_longer", "headline": "Last Longer", "day_in_program": 1,
                "total_days": 10, "module_order": 1, "module_name": "Module 1: Starting",
                "module_current_day": 1, "module_total_days": 2, "module_completed": False,
                "last_day_of_module": False,
                "questions": [{"name": "lesson_start", "headline": "Starting",
                               "completed": False, "is_moved_forward": False}]}}

    def read(self):
        return _oracle(self.payload, "last_longer", require_incomplete_module=True)

    def test_accepts_incomplete_module_with_following_module(self):
        self.assertEqual(1, self.read()["sectionMetadata"]["module_current_day"])

    def test_accepts_omitted_optional_postpone_flag_on_untouched_cards(self):
        del self.payload["program_section"]["questions"][0]["is_moved_forward"]
        self.assertEqual(1, self.read()["sectionMetadata"]["module_current_day"])

    def test_rejects_completed_and_unknown_activity_states(self):
        for state in (True, None, 0, "false"):
            with self.subTest(state=state):
                self.payload["program_section"]["questions"][0]["completed"] = state
                with self.assertRaisesRegex(EvidenceError, "activity state"):
                    self.read()

    def test_rejects_postponed_and_unknown_activity_states(self):
        for state in (True, None, 0, "false"):
            with self.subTest(state=state):
                self.payload["program_section"]["questions"][0]["is_moved_forward"] = state
                with self.assertRaisesRegex(EvidenceError, "activity state"):
                    self.read()

    def test_rejects_missing_and_invalid_module_metadata(self):
        baseline = copy.deepcopy(self.payload)
        for field, value in (("module_current_day", True), ("module_total_days", 1),
                             ("module_order", 0), ("module_completed", True),
                             ("module_completed", None), ("module_name", ""),
                             ("last_day_of_module", True), ("module_current_day", 3)):
            with self.subTest(field=field, value=value):
                self.payload = copy.deepcopy(baseline)
                self.payload["program_section"][field] = value
                with self.assertRaisesRegex(EvidenceError, "metadata"):
                    self.read()

    def test_rejects_program_end_instead_of_misreporting_module_lock(self):
        self.payload["program_section"]["total_days"] = 2
        with self.assertRaisesRegex(EvidenceError, "following module"):
            self.read()

    def test_last_stage_requires_consistent_boundary_metadata(self):
        self.payload["program_section"].update(module_current_day=2, day_in_program=2, last_day_of_module=True)
        self.assertEqual(2, self.read()["sectionMetadata"]["module_current_day"])

    def test_new_option_does_not_change_existing_selector_oracle(self):
        self.payload["program_section"]["questions"][0]["completed"] = True
        self.assertEqual("last_longer", _oracle(self.payload, "last_longer")["programId"])


if __name__ == "__main__":
    unittest.main()
