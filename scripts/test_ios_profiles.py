"""Offline contracts for iOS launch policy, early failures and cascade prevention."""
import contextlib
import io
import json
import os
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

import run_ios_profiles as runner


INFO = {
    "CFBundleIdentifier": "com.vamapps.The-Coach",
    "CFBundleShortVersionString": "1.13.33",
    "CFBundleVersion": "123",
}
ENV = {
    "COACH_EXISTING_PROGRESS_EMAIL": "fixture@example.invalid",
    "COACH_EXISTING_PROGRESS_OTP": "1234",
    "COACH_DAILY_PLAN_DAY": "Stage 1",
    "COACH_FIXTURE_RESET_MODE": "prebuilt",
    "TEST_ISOLATION_MODE": "logout",
    "IOS_SIMULATOR_APP": "/fixture/Coach.app",
    "IOS_APP_VARIANT": "male",
}
KEGEL_ENV = {
    "COACH_KEGEL_PLAYER_EMAIL": "kegel@example.invalid",
    "COACH_KEGEL_PLAYER_OTP": "5678",
}
REGRESSION_PROPERTIES = {
    "coach.kegelPlayer.program": "Kegel Challenge",
    "coach.dailyPlan.corePracticeProgram": "Kegel Challenge",
    "coach.dailyPlan.corePracticeDay": "Stage 6 of 9",
    "coach.dailyPlan.corePracticeTitle": "Morning Kegel Workout",
}


class IOSProfileTests(unittest.TestCase):
    def test_broad_discovery_and_overridden_results_are_rejected(self):
        for option in ("-Dtest=tests.*Tests,tests.HW.*", "-Dallure.results.directory=target/old",
                       "-Dcoach.run.runProfile=wrong", "-Dplatform=android"):
            with self.subTest(option=option), self.assertRaises(runner.PreflightError):
                runner.properties([option])

    def test_last_maven_property_wins_without_printing_its_value(self):
        props = runner.properties(["-Dcoach.existingProgress.otp=first", "-Dcoach.existingProgress.otp=second"])
        env = runner.environment_for("male", ENV, props)
        self.assertEqual("second", env["COACH_EXISTING_PROGRESS_OTP"])

    def test_variant_specific_account_is_selected_and_incomplete_pair_fails(self):
        supplied = dict(ENV, COACH_FEMALE_EXISTING_PROGRESS_EMAIL="female@example.invalid")
        env = runner.environment_for("female", supplied, {})
        self.assertEqual("", env["COACH_EXISTING_PROGRESS_OTP"])
        with self.assertRaises(runner.PreflightError):
            runner.validate_profile("explore", {}, env, INFO)

    def test_missing_data_stops_before_device_access_or_maven(self):
        with patch.dict("os.environ", {}, clear=True), \
                patch.object(runner, "validate_runtime") as devices, \
                patch.object(runner, "execute") as execute, \
                contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(2, runner.main(["explore"]))
        devices.assert_not_called()
        execute.assert_not_called()

    def test_plan_needs_no_accounts_apps_or_devices(self):
        with patch.dict("os.environ", {}, clear=True), \
                patch.object(runner, "validate_runtime") as devices, \
                patch.object(runner, "load_app") as app, \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, runner.main(["full", "--profiles", "regression,localization,push-permission", "--plan"]))
        devices.assert_not_called()
        app.assert_not_called()

    def test_conflicting_reset_is_rejected(self):
        with self.assertRaises(runner.PreflightError):
            runner.validate_profile("push-permission", {"ios.noReset": "true"}, ENV, INFO)
        with self.assertRaises(runner.PreflightError):
            runner.validate_profile("explore", {"ios.fullReset": "true"}, ENV, INFO)

    def test_daily_plan_profiles_require_a_dedicated_kegel_account(self):
        for name in ("regression", "daily-plan"):
            with self.subTest(name=name), self.assertRaisesRegex(runner.PreflightError, "COACH_KEGEL_PLAYER"):
                runner.validate_profile(name, REGRESSION_PROPERTIES, ENV, INFO)

    def test_daily_plan_profiles_require_the_prepared_core_practice(self):
        for name in ("regression", "daily-plan"):
            for key in ("coach.dailyPlan.corePracticeProgram", "coach.dailyPlan.corePracticeDay",
                        "coach.dailyPlan.corePracticeTitle"):
                props = dict(REGRESSION_PROPERTIES)
                del props[key]
                with self.subTest(name=name, key=key), self.assertRaisesRegex(runner.PreflightError, key):
                    runner.validate_profile(name, props, dict(ENV, **KEGEL_ENV), INFO)

    def test_ios_smoke_profiles_require_an_observed_workout_program(self):
        props = dict(REGRESSION_PROPERTIES)
        del props["coach.kegelPlayer.program"]
        for name in ("regression", "ios-parity-smoke"):
            with self.subTest(name=name), self.assertRaisesRegex(runner.PreflightError, "coach.kegelPlayer.program"):
                runner.validate_profile(name, props, dict(ENV, **KEGEL_ENV), INFO)

    def test_complete_daily_plan_and_smoke_configuration_is_accepted(self):
        for name in ("regression", "daily-plan", "ios-parity-smoke"):
            with self.subTest(name=name):
                runner.validate_profile(name, REGRESSION_PROPERTIES, dict(ENV, **KEGEL_ENV), INFO)

    def test_incomplete_regression_configuration_stops_before_devices_and_maven(self):
        with patch.dict("os.environ", ENV, clear=True), \
                patch.object(runner, "load_app", return_value=(Path("/fixture/Coach.app"), dict(INFO))), \
                patch.object(runner, "validate_runtime") as runtime, \
                patch.object(runner, "execute", return_value=0) as execute, \
                tempfile.TemporaryDirectory() as folder, \
                contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(2, runner.main(["regression", "--results-root", folder]))
        runtime.assert_not_called()
        execute.assert_not_called()

    def test_fixed_profile_policy_overrides_inherited_reset_environment(self):
        env = dict(ENV, IOS_NO_RESET="true", IOS_FULL_RESET="false")
        command = runner.command_for("push-permission", {}, env, INFO, "sim",
                                     Path("/results"), "commit")
        self.assertIn("-Dios.noReset=false", command)
        self.assertIn("-Dios.fullReset=true", command)

    def test_installed_build_omits_app_capability(self):
        command = runner.command_for("localization", {}, ENV, INFO, "device",
                                     Path("/results"), "commit")
        self.assertEqual(["mvn", "test"], command[:2])
        self.assertFalse(any(argument.startswith("-Dios.app=") for argument in command))
        self.assertIn("-Dios.bundleId=com.vamapps.The-Coach", command)

    def test_installed_profile_rejects_inherited_ios_app(self):
        with self.assertRaises(runner.PreflightError):
            runner.validate_profile("localization", {}, dict(ENV, IOS_APP="/device/app.ipa"), INFO)

    def test_localization_fixture_is_required_before_any_session(self):
        with self.assertRaisesRegex(runner.PreflightError, "localization.fixture"):
            runner.validate_profile("localization", {"localization.scenarios": "welcome"}, ENV, INFO)

    def test_modules_cannot_silently_skip_without_fixture(self):
        env = dict(ENV, COACH_COA9044_EMAIL="module@example.invalid",
                   COACH_COA9044_OTP="5678", COACH_COA9044_UID="fixture-uid")
        with self.assertRaisesRegex(runner.PreflightError, "coach.modules.programId"):
            runner.validate_profile("modules", {}, env, INFO)

    def test_purchase_opt_in_is_not_automatically_enabled(self):
        with self.assertRaisesRegex(runner.PreflightError, "storekit.sandbox"):
            runner.validate_profile("onboarding-paywall", {}, ENV, INFO)

    def test_marketing_link_case_does_not_require_an_unused_otp(self):
        self.assertNotIn("COACH_COA7937_OTP", runner.PROFILES["kegel-october"]["required"])
        self.assertIn("COACH_COA7937_LINK", runner.PROFILES["kegel-october"]["required"])

    def test_male_start_screen_cannot_be_selected_for_a_female_build(self):
        with self.assertRaisesRegex(runner.PreflightError, "male app variant"):
            runner.validate_profile("male-start-screen", {}, dict(ENV, IOS_APP_VARIANT="female"), INFO)

    def test_update_requires_same_bundle_and_different_build(self):
        env = dict(ENV, COACH_EXPECTED_ACCOUNT_UID="fixture-uid", IOS_UPDATE_APP="/new.app")
        with patch.object(runner, "load_app", return_value=(Path("/new.app"), dict(INFO))), \
                self.assertRaisesRegex(runner.PreflightError, "different CFBundleVersion"):
            runner.validate_profile("release-smoke", {}, env, INFO)

    def test_log_redaction_covers_selected_and_variant_credentials(self):
        env = dict(ENV, COACH_FEMALE_EXISTING_PROGRESS_EMAIL="female@example.invalid",
                   COACH_COA7937_LINK="https://example.invalid/token-secret")
        output = runner.redact("fixture@example.invalid 1234 female@example.invalid https://example.invalid/token-secret", env)
        for secret in ("fixture@example.invalid", "1234", "female@example.invalid", "token-secret"):
            self.assertNotIn(secret, output)

    def test_log_redaction_covers_environment_and_jvm_uids(self):
        env = {"COACH_EXPECTED_ACCOUNT_UID": "synthetic-expected-uid",
               "COACH_COA9044_UID": "synthetic-module-uid"}
        props = {"coach.expectedAccount.uid": "synthetic-jvm-uid"}
        text = "expected=synthetic-expected-uid module=synthetic-module-uid actual=synthetic-jvm-uid"
        self.assertEqual("expected=<redacted> module=<redacted> actual=<redacted>",
                         runner.redact(text, env, props))

    def test_uid_redaction_handles_trimmed_overlapping_values_and_mixed_case_keys(self):
        env = {"COACH_EXPECTED_ACCOUNT_UID": "  synthetic-uid  "}
        props = {"coach.fixture.UID": "synthetic-uid-long"}
        self.assertEqual("<redacted> <redacted> <redacted>", runner.redact(
            "  synthetic-uid   synthetic-uid-long synthetic-uid", env, props))

    def test_uid_redaction_does_not_treat_fixture_ids_or_blank_values_as_uids(self):
        env = {"COACH_EXPECTED_ACCOUNT_UID": "  ", "COACH_FIXTURE_ID": "fixture-identifier"}
        props = {"coach.account.uid": "", "coach.fixture.id": "fixture-identifier"}
        self.assertEqual("fixture-identifier  remains readable", runner.redact(
            "fixture-identifier  remains readable", env, props))

    def run_mocked(self, folder, codes):
        with patch.dict("os.environ", dict(ENV, **KEGEL_ENV), clear=True), \
                patch.object(runner, "load_app", return_value=(Path("/fixture/Coach.app"), dict(INFO))), \
                patch.object(runner, "validate_runtime"), \
                patch.object(runner, "execute", side_effect=codes) as execute, \
                contextlib.redirect_stdout(io.StringIO()):
            code = runner.main(["full", "--profiles", "regression,profile", "--results-root", folder]
                               + ["-D" + key + "=" + value for key, value in REGRESSION_PROPERTIES.items()])
        return code, execute, json.loads((Path(folder) / "run-summary.json").read_text())

    def test_failed_ui_preflight_blocks_dependent_tests_and_preserves_failure_exit(self):
        with tempfile.TemporaryDirectory() as folder:
            code, execute, summary = self.run_mocked(folder, [1])
        self.assertEqual(1, code)
        self.assertEqual(1, execute.call_count)
        self.assertEqual("blocked-by-precondition", summary["profiles"]["regression"]["status"])
        self.assertEqual("blocked-before-run", summary["profiles"]["profile"]["status"])
        self.assertNotIn("testsExit", summary["profiles"]["regression"])

    def test_successful_gate_runs_only_selected_profiles_with_separate_results(self):
        with tempfile.TemporaryDirectory() as folder:
            code, execute, summary = self.run_mocked(folder, [0, 0, 0])
        self.assertEqual(0, code)
        self.assertEqual(3, execute.call_count)
        self.assertEqual("passed", summary["profiles"]["regression"]["status"])
        self.assertEqual("not-requested", summary["profiles"]["storekit-purchase"]["status"])
        commands = [call.args[0] for call in execute.call_args_list]
        directories = [next(arg for arg in command if arg.startswith("-Dallure.results.directory=")) for command in commands]
        self.assertEqual(3, len(set(directories)))
        self.assertIn("-Dcoach.run.runProfile=regression", commands[1])

    def test_test_failure_is_not_masked_by_later_success(self):
        with tempfile.TemporaryDirectory() as folder:
            code, _, summary = self.run_mocked(folder, [0, 1, 0])
        self.assertEqual(1, code)
        self.assertEqual("failed", summary["profiles"]["regression"]["status"])
        self.assertEqual("passed", summary["profiles"]["profile"]["status"])

    def test_existing_results_are_never_overwritten(self):
        with tempfile.TemporaryDirectory() as folder:
            marker = Path(folder) / "existing.txt"
            marker.write_text("preserve")
            with patch.dict("os.environ", ENV, clear=True), \
                    patch.object(runner, "load_app", return_value=(Path("/fixture/Coach.app"), dict(INFO))), \
                    patch.object(runner, "validate_runtime"), \
                    patch.object(runner, "execute") as execute, \
                    contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(2, runner.main(["explore", "--results-root", folder]))
            self.assertEqual("preserve", marker.read_text())
        execute.assert_not_called()

    def test_raw_simulator_launcher_rejects_package_discovery_before_app_checks(self):
        result = runner.subprocess.run(["bash", str(runner.ROOT / "ci-scripts/run-ios-simulator.sh"),
                                        "-Dtest=tests.*Tests,tests.HW.*"],
                                       capture_output=True, text=True)
        self.assertEqual(2, result.returncode)
        self.assertIn("Broad package discovery", result.stderr)

    def test_existing_suite_keeps_maven_options_and_variant_account_selection(self):
        with tempfile.TemporaryDirectory() as folder:
            ci = Path(folder) / "ci-scripts"
            ci.mkdir()
            launcher = ci / "run-ios-suite.sh"
            shutil.copyfile(runner.ROOT / "ci-scripts/run-ios-suite.sh", launcher)
            simulator = ci / "run-ios-simulator.sh"
            simulator.write_text(
                "#!/usr/bin/env python3\nimport json, os, sys\n"
                "print(json.dumps({'args': sys.argv[1:], 'variantSelected': "
                "os.environ.get('COACH_EXISTING_PROGRESS_EMAIL') == 'male@example.invalid' "
                "and os.environ.get('COACH_EXISTING_PROGRESS_OTP') == '5678'}))\n")
            simulator.chmod(0o755)
            env = dict(os.environ, IOS_APP_VARIANT="male",
                       COACH_MALE_EXISTING_PROGRESS_EMAIL="male@example.invalid",
                       COACH_MALE_EXISTING_PROGRESS_OTP="5678")
            result = runner.subprocess.run(
                ["bash", str(launcher), "localization", "-Plocalization", "-q",
                 "-Dlocalization.locales=en"], env=env, capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        output = json.loads(result.stdout)
        self.assertTrue(output["variantSelected"])
        self.assertEqual(["-Dtest=suites.LocalizationSuite", "-Plocalization", "-q",
                          "-Dlocalization.locales=en"], output["args"])

    def test_managed_profiles_are_available_without_changing_existing_suite_commands(self):
        for arguments, expected in ((["full", "--plan"], "suites.IOSRegressionSuite"),
                                    (["profiles", "explore", "--plan"], "suites.ExploreParitySuite"),
                                    (["profiles", "localization", "--plan"], "suites.LocalizationSuite")):
            with self.subTest(arguments=arguments):
                result = runner.subprocess.run(
                    ["bash", str(runner.ROOT / "ci-scripts/run-ios-suite.sh")] + arguments,
                    env={}, capture_output=True, text=True)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertIn(expected, result.stdout)

    def test_history_is_copied_without_modifying_the_previous_report(self):
        with tempfile.TemporaryDirectory() as folder:
            history = Path(folder) / "old" / "history"
            history.mkdir(parents=True)
            (history / "history.json").write_text("{}")
            results = Path(folder) / "new" / "allure-results"
            results.mkdir(parents=True)
            with patch.object(runner.subprocess, "call", return_value=0) as generate:
                self.assertEqual(0, runner.generate_report("/allure", [results],
                                                          Path(folder) / "report", history))
            self.assertEqual("{}", (history / "history.json").read_text())
            self.assertEqual("{}", (results / "history" / "history.json").read_text())
            self.assertEqual(["/allure", "generate", str(results)], generate.call_args.args[0][:3])

    def test_localization_configuration_gate_failure_prevents_the_locale_matrix(self):
        with tempfile.TemporaryDirectory() as folder:
            fixture = Path(folder) / "fixture.properties"
            fixture.write_text("platform=ios\n")
            run_root = Path(folder) / "run"
            with patch.dict("os.environ", ENV, clear=True), \
                    patch.object(runner, "load_app", return_value=(Path("/fixture/Coach.app"), dict(INFO))), \
                    patch.object(runner, "validate_runtime"), \
                    patch.object(runner, "execute", return_value=1) as execute, \
                    contextlib.redirect_stdout(io.StringIO()):
                code = runner.main(["localization", "--results-root", str(run_root),
                                    "-Dlocalization.fixture=" + str(fixture)])
            summary = json.loads((run_root / "run-summary.json").read_text())
        self.assertEqual(1, code)
        self.assertEqual(1, execute.call_count)
        self.assertIn("-Dtest=tests.LocalizationConfigurationTests", execute.call_args.args[0])
        self.assertEqual("blocked-by-precondition", summary["profiles"]["localization"]["status"])
        self.assertNotIn("testsExit", summary["profiles"]["localization"])

    def test_missing_report_tool_fails_before_sessions_or_device_access(self):
        with patch.dict("os.environ", dict(ENV, **KEGEL_ENV, ALLURE_BIN="/not-installed/allure"), clear=True), \
                patch.object(runner, "load_app", return_value=(Path("/fixture/Coach.app"), dict(INFO))), \
                patch.object(runner, "validate_runtime") as runtime, \
                patch.object(runner, "execute") as execute, \
                contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(2, runner.main(["regression", "--report"]
                             + ["-D" + key + "=" + value for key, value in REGRESSION_PROPERTIES.items()]))
        runtime.assert_not_called()
        execute.assert_not_called()


if __name__ == "__main__":
    unittest.main()
