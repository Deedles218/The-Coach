"""Offline checks of fixture restoration and mutation boundaries, not API results."""
from copy import deepcopy
import hashlib
import io
import json
import unittest
from unittest.mock import Mock, patch

import check_program_selection_api as checks


class FakeSession:
    def __init__(self):
        self.main = 'main'
        self.secondary = ['one', 'two']
        self.active = 'main'
        self.status = 400
        self.bad_mutation = False
        self.exception = False
        self.writes = []

    def snapshot(self):
        def row(name):
            return {'program_id': name, 'progress_percent': 0,
                    'is_active': self.active == name}
        return {'main': None if self.main is None else row(self.main),
                'secondary': [row(p) for p in sorted(self.secondary)],
                'active': self.active,
                'catalog_membership': {p: 'main_journey' if self.main == p else
                                       'secondary' if p in self.secondary else 'none'
                                       for p in ('main', 'one', 'two', 'unadded')},
                'selection_flags': {'.onboarding_program_selection_completed': self.main is not None},
                'achievements': {'lessons_completed': 0}}

    def post(self, route, payload=None):
        self.writes.append((route, deepcopy(payload)))
        if route == 'secondary_programs/':
            self.secondary = sorted(payload['program_ids'])
        elif route == 'active_program/':
            self.active = payload['program_id']
        elif route == 'main_journey/':
            self.main = self.active = payload['program_id']
        elif route == 'unadded/add_and_activate/' and payload is None:
            self.main = self.active = 'unadded'
            return {'main_journey_program_id': self.main, 'secondary_program_ids': [],
                    'active_program_id': self.active, 'onboarding_program_selection_completed': True}
        else:
            raise AssertionError('Unexpected fixture write')

    def request(self, route, payload=None, post=False):
        if self.exception:
            raise OSError('Transport unavailable')
        if self.bad_mutation:
            self.post(route.removeprefix(checks.ROOT), payload)
        return self.status, None


class ProgramSelectionSafetyTests(unittest.TestCase):
    def test_valid_activation_uses_only_the_observed_bodyless_program_url(self):
        session = self.fresh_ui_session()
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9797'}, clear=True):
            evidence = {}
            checks.run(session, 'first-activation', evidence, 'unadded')
        self.assertEqual([('unadded/add_and_activate/', None)], session.writes)
        self.assertEqual('unadded', evidence['after']['main']['program_id'])
        self.assertEqual([], evidence['after']['secondary'])

    def test_activation_transport_rejects_body_and_scope_before_network_access(self):
        session = checks.Session.__new__(checks.Session)
        session.read_only = False
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        session.opener = Mock()
        with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9797'}, clear=True):
            with self.assertRaisesRegex(AssertionError, 'bodyless'):
                session.request(checks.ROOT + 'unadded/add_and_activate/', {'program_id': 'unadded'}, post=True)
        with patch.dict(checks.os.environ, {}, clear=True):
            with self.assertRaisesRegex(AssertionError, 'dedicated COA-9797 UID'):
                session.request(checks.ROOT + 'unadded/add_and_activate/', post=True)
        session.opener.open.assert_not_called()

    def test_activation_transport_sends_a_post_without_payload(self):
        session = checks.Session.__new__(checks.Session)
        session.read_only = False
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        session.host, session.headers = checks.PREPROD_HOST, {}
        session.opener = Mock()
        response = io.BytesIO(json.dumps({'active_program_id': 'unadded'}).encode())
        response.status = 200
        session.opener.open.return_value = response
        with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9797'}, clear=True):
            session.request(checks.ROOT + 'unadded/add_and_activate/', post=True)
        request = session.opener.open.call_args.args[0]
        self.assertEqual(checks.PREPROD_HOST + checks.ROOT + 'unadded/add_and_activate/', request.full_url)
        self.assertEqual('POST', request.get_method())
        self.assertIsNone(request.data)

    def test_legacy_catalog_cannot_be_used_as_a_read_only_program_oracle(self):
        session = checks.Session.__new__(checks.Session)
        session.read_only = False
        session.opener = Mock()
        for route in (checks.ROOT + 'catalog/v3/last_longer/', checks.ROOT + 'chooseprogram',
                      checks.ROOT + 'progress', checks.ROOT + '../user/'):
            with self.assertRaisesRegex(AssertionError, 'Unexpected program selection endpoint'):
                session.request(route)
        session.opener.open.assert_not_called()

    def test_content_access_requires_its_qa_scope_and_cannot_be_replayed_on_production(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        session.opener = Mock()
        for production, scope in ((False, 'COA-9790'), (True, 'COA-9796')):
            session.read_only = production
            with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': scope}, clear=True):
                with self.assertRaisesRegex(AssertionError, 'dedicated COA-9796 UID'):
                    session.request(checks.ROOT + 'lessons/fixture_lesson/')
        session.opener.open.assert_not_called()

    def test_first_valid_activation_requires_its_dedicated_case_and_uid_before_any_write(self):
        session = self.fresh_ui_session()
        for scope in ({}, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9797'}):
            with patch.dict(checks.os.environ, scope, clear=True):
                with self.assertRaisesRegex(AssertionError, 'dedicated COA-9797 UID'):
                    checks.run(session, 'first-activation', {}, 'unadded')
            self.assertEqual([], session.writes)

    def test_production_rejects_the_valid_activation_operation_before_fixture_access(self):
        session = Mock(read_only=True)
        with self.assertRaisesRegex(AssertionError, 'read-only onboarding'):
            checks.run(session, 'first-activation', {}, 'unadded')
        session.snapshot.assert_not_called()
        session.request.assert_not_called()

    def test_android_other_case_is_rejected_before_device_access(self):
        with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9790'}, clear=True), \
                patch('read_android_email_enforcement.installed_build') as build:
            with self.assertRaisesRegex(AssertionError, 'cross-platform QA case'):
                checks.AndroidSession('qa-device', 'com.vamapps.thecoach')
            build.assert_not_called()

    def test_android_requires_backend_and_reviewed_apk_bindings_before_device_access(self):
        with patch.dict(checks.os.environ, {'COACH_PROGRAM_SELECTION_CASE': 'COA-9795'}, clear=True), \
                patch('read_android_email_enforcement.installed_build') as build:
            with self.assertRaisesRegex(AssertionError, 'bindings'):
                checks.AndroidSession('qa-device', 'com.vamapps.thecoach')
            build.assert_not_called()

    def test_android_transport_can_never_mutate_the_shared_account(self):
        session = checks.AndroidSession.__new__(checks.AndroidSession)
        session.opener = Mock()
        for payload, post in ((None, True), ({'program_id': 'main'}, True), ({'program_id': 'main'}, False)):
            with self.assertRaisesRegex(AssertionError, 'read-only'):
                session.request(checks.ROOT + 'active_program/', payload, post)
        session.opener.open.assert_not_called()

    def test_android_uses_backend_identity_instead_of_equating_ios_and_android_firebase_uids(self):
        session = checks.AndroidSession.__new__(checks.AndroidSession)
        backend = 'ios_fixtureIdentity123456789'
        session.backend_hash = hashlib.sha256(backend.encode()).hexdigest()
        session.expected_email = 'fixture@example.invalid'
        user = {'user_id': backend, 'connected_email': {'email': session.expected_email,
                                                       'verified': True, 'user_confirmed': True}}
        session.get = lambda route: {'userdata': user}
        session.require_fixture_identity()
        user['user_id'] = 'differentBackendIdentity123456789'
        with self.assertRaisesRegex(AssertionError, 'different backend accounts'):
            session.require_fixture_identity()

    def test_restores_secondary_and_active_after_rejection(self):
        for operation in checks.OPERATIONS[:2]:
            with self.subTest(operation=operation):
                session = FakeSession()
                original = session.snapshot()
                evidence = {}
                checks.run(session, operation, evidence)
                self.assertEqual(original, session.snapshot())
                self.assertTrue(evidence['restored'])
                self.assertEqual(['one', 'two'], checks.secondary_ids(evidence['before']))
                self.assertEqual('main', evidence['before']['active'])

    def test_unexpected_success_fails_and_still_restores(self):
        session = FakeSession()
        session.status = 200
        original = session.snapshot()
        with self.assertRaisesRegex(AssertionError, 'Expected HTTP 400'):
            checks.run(session, 'activate-unadded', {})
        self.assertEqual(original, session.snapshot())

    def test_mutating_rejection_is_detected_and_restored(self):
        for operation in checks.OPERATIONS[:2]:
            with self.subTest(operation=operation):
                session = FakeSession()
                session.bad_mutation = True
                original = session.snapshot()
                with self.assertRaisesRegex(AssertionError, 'changed account'):
                    checks.run(session, operation, {})
                self.assertEqual(original, session.snapshot())

    def test_transport_failure_during_check_does_not_leave_prepared_state(self):
        session = FakeSession()
        session.exception = True
        original = session.snapshot()
        with self.assertRaises(OSError):
            checks.run(session, 'activate-unadded', {})
        self.assertEqual(original, session.snapshot())

    def test_insufficient_catalog_is_rejected_before_fixture_writes(self):
        session = FakeSession()
        session.secondary = ['one']
        original_snapshot = session.snapshot
        def snapshot():
            state = original_snapshot()
            del state['catalog_membership']['two']
            del state['catalog_membership']['unadded']
            return state
        session.snapshot = snapshot
        with self.assertRaisesRegex(AssertionError, 'outside the saved set'):
            checks.run(session, 'activate-unadded', {})
        self.assertEqual([], session.writes)

    def test_missing_main_checks_conflict_without_fixture_preparation(self):
        session = FakeSession()
        session.main, session.active, session.secondary, session.status = None, None, [], 409
        checks.run(session, 'complete-without-main', {})
        self.assertEqual([], session.writes)

    def test_main_in_secondary_does_not_need_an_unrelated_catalog_program(self):
        session = FakeSession()
        original_snapshot = session.snapshot
        def snapshot():
            state = original_snapshot()
            del state['catalog_membership']['unadded']
            return state
        session.snapshot = snapshot
        checks.run(session, 'secondary-includes-main', {})

    def test_registered_accounts_require_an_explicit_fixture_binding(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_email = None
        session.get = lambda route: {'userdata': {'user_id': session.uid,
                                    'connected_email': {'email': 'fixture@example.invalid'}}}
        with self.assertRaisesRegex(AssertionError, 'COACH_PROGRAM_SELECTION_EMAIL'):
            session.require_fixture_identity()

    def test_confirmed_qa_account_is_accepted_when_bound(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_email = 'fixture@example.invalid'
        session.get = lambda route: {'userdata': {'user_id': 'ios_' + session.uid,
                                    'connected_email': {'email': 'FIXTURE@example.invalid',
                                                        'verified': True, 'user_confirmed': True}}}
        session.require_fixture_identity()

    def test_qa_binding_rejects_a_different_account(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_email = 'expected@example.invalid'
        session.get = lambda route: {'userdata': {'user_id': session.uid,
                                    'connected_email': {'email': 'other@example.invalid'}}}
        with self.assertRaisesRegex(AssertionError, 'differs from'):
            session.require_fixture_identity()

    def test_qa_binding_does_not_silently_accept_an_anonymous_profile(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_email = 'expected@example.invalid'
        session.get = lambda route: {'userdata': {'user_id': session.uid}}
        with self.assertRaisesRegex(AssertionError, 'differs from'):
            session.require_fixture_identity()

    def test_anonymous_profile_requires_an_explicit_uid_binding(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_email = None
        session.get = lambda route: {'userdata': {'user_id': session.uid}}
        with self.assertRaisesRegex(AssertionError, 'COACH_PROGRAM_SELECTION_UID'):
            session.require_fixture_identity()

    def test_new_email_enrollment_is_bound_to_the_same_qa_uid_before_and_after_confirmation(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        session.expected_email = None
        session.pending_email = 'new-fixture@example.invalid'
        user = {'user_id': session.uid}
        session.get = lambda route: {'userdata': user}
        session.require_fixture_identity()
        user['connected_email'] = {'email': session.pending_email, 'verified': True, 'user_confirmed': True}
        session.require_fixture_identity()
        user['connected_email']['email'] = 'other@example.invalid'
        with self.assertRaisesRegex(AssertionError, 'differs from the prepared enrollment'):
            session.require_fixture_identity()
        user['connected_email']['email'] = session.pending_email
        session.uid = 'differentFixtureIdentity123456789'
        user['user_id'] = session.uid
        with self.assertRaisesRegex(AssertionError, 'differs from COACH_PROGRAM_SELECTION_UID'):
            session.require_fixture_identity()

    def test_bound_anonymous_qa_profile_is_accepted(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = session.expected_uid = 'fixtureIdentity123456789'
        session.expected_email = None
        session.get = lambda route: {'userdata': {'user_id': session.uid}}
        session.require_fixture_identity()

    def test_wrong_anonymous_uid_is_rejected(self):
        session = checks.Session.__new__(checks.Session)
        session.uid = 'fixtureIdentity123456789'
        session.expected_uid = 'otherFixtureIdentity123456789'
        session.expected_email = None
        session.get = lambda route: {'userdata': {'user_id': session.uid}}
        with self.assertRaisesRegex(AssertionError, 'differs from COACH_PROGRAM_SELECTION_UID'):
            session.require_fixture_identity()

    def test_production_bundle_is_rejected_before_reading_a_session(self):
        with self.assertRaisesRegex(AssertionError, 'explicit preprod'):
            checks.Session('00008030-000929442E01802E', 'com.vamapps.The-Coach')

    def test_production_fixture_read_uses_the_matching_host_only_when_approved(self):
        self.assertEqual(checks.BUNDLES['com.vamapps.The-Coach'][0],
                         checks.session_host('com.vamapps.The-Coach', True))
        with self.assertRaisesRegex(AssertionError, 'Unsupported'):
            checks.session_host('com.example.unapproved', True)

    def test_production_fixture_read_rejects_missing_email_before_device_access(self):
        with patch.dict(checks.os.environ, {}, clear=True), patch.object(checks, 'copy_data') as copy:
            with self.assertRaisesRegex(AssertionError, 'registered QA email'):
                checks.Session('00008030-000929442E01802E', 'com.vamapps.The-Coach', True)
            copy.assert_not_called()

    def test_production_rejects_mutating_operations_before_reading_the_fixture(self):
        for operation in checks.OPERATIONS:
            session = Mock(read_only=True)
            with self.assertRaisesRegex(AssertionError, 'read-only onboarding'):
                checks.run(session, operation, {})
            session.snapshot.assert_not_called()
            session.request.assert_not_called()

    def test_production_request_boundary_blocks_posts_and_get_payloads(self):
        session = checks.Session.__new__(checks.Session)
        session.read_only = True
        session.opener = Mock()
        for payload, post in ((None, True), ({'program_ids': []}, True), ({'program_ids': []}, False)):
            with self.assertRaisesRegex(AssertionError, 'read-only'):
                session.request(checks.ROOT + 'secondary_programs/', payload, post)
        session.opener.open.assert_not_called()

    def test_gender_headers_match_observed_native_requests(self):
        self.assertEqual('M', checks.gender_header('com.vamapps.preprod.The-Coach'))
        self.assertEqual('F', checks.gender_header('com.vamapps.preprod.The-Coach-for-her'))

    def test_active_secondary_is_rejected_before_any_fixture_writes(self):
        session = FakeSession()
        session.active = 'one'
        with self.assertRaisesRegex(AssertionError, 'active before the check'):
            checks.run(session, 'secondary-includes-main', {})
        self.assertEqual([], session.writes)

    def fresh_ui_session(self):
        session = FakeSession()
        session.main, session.active, session.secondary = None, None, []
        snapshot = session.snapshot
        def fresh():
            result = snapshot()
            result['subscription_active'] = True
            result['achievements'] = {'lessons_completed': 0, 'exercises_completed': 0, 'longest_streak': 0}
            return result
        session.snapshot = fresh
        return session

    def test_fresh_ui_profile_validation_is_read_only(self):
        session = self.fresh_ui_session()
        evidence = {}
        checks.run(session, 'new-onboarding-profile', evidence)
        self.assertTrue(evidence['read_only'])
        self.assertEqual([], session.writes)

    def test_ui_profile_with_saved_roles_is_rejected_before_writes(self):
        session = FakeSession()
        with self.assertRaisesRegex(AssertionError, 'saved program roles'):
            checks.run(session, 'new-onboarding-profile', {})
        self.assertEqual([], session.writes)

    def test_ui_profile_with_existing_progress_is_rejected_before_writes(self):
        session = self.fresh_ui_session()
        snapshot = session.snapshot
        def progressed():
            result = snapshot()
            result['achievements']['lessons_completed'] = 1
            return result
        session.snapshot = progressed
        with self.assertRaisesRegex(AssertionError, 'existing progress'):
            checks.run(session, 'new-onboarding-profile', {})
        self.assertEqual([], session.writes)

    def test_ui_profile_without_access_is_rejected_before_writes(self):
        session = self.fresh_ui_session()
        snapshot = session.snapshot
        def unsubscribed():
            result = snapshot()
            result['subscription_active'] = False
            return result
        session.snapshot = unsubscribed
        with self.assertRaisesRegex(AssertionError, 'successful test Paywall'):
            checks.run(session, 'new-onboarding-profile', {})
        self.assertEqual([], session.writes)

    def test_prepared_secondary_draft_requires_the_expected_main_and_zero_progress(self):
        session = self.fresh_ui_session()
        session.main = 'main'
        snapshot = session.snapshot
        def draft():
            result = snapshot()
            result['selection_flags'] = {'.onboarding_program_selection_completed': False}
            return result
        session.snapshot = draft
        evidence = {}
        checks.run(session, 'secondary-onboarding-profile', evidence, 'main')
        self.assertTrue(evidence['read_only'])
        self.assertEqual([], session.writes)
        def free_draft():
            result = draft()
            result['subscription_active'] = False
            return result
        session.snapshot = free_draft
        checks.run(session, 'secondary-onboarding-profile', {}, 'main')
        with self.assertRaisesRegex(AssertionError, 'expected Main Journey draft'):
            checks.run(session, 'secondary-onboarding-profile', {}, 'different')
        session.secondary = ['one']
        with self.assertRaisesRegex(AssertionError, 'expected Main Journey draft'):
            checks.run(session, 'secondary-onboarding-profile', {}, 'main')
        self.assertEqual([], session.writes)


if __name__ == '__main__':
    unittest.main()
