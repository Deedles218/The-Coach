#!/usr/bin/env python3
"""Run Android parity suites with credentials from the approved local Keychain."""
import argparse
import datetime
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import sys
import time
from run_test_model_local import ROOT, keychain, MODULES_PREMIUM_SERVICE

SUITES = {
    "auth": "tests.CoachAuthorizationTests",
    "profile": "tests.CoachProfileTests",
    "daily": "tests.DailyPlanTests",
    "explore": "tests.ConfiguredExploreTests",
    "explore-cards": "tests.AndroidExploreTests",
    "welcome": "tests.MaleBuildStartScreenTests",
    "selector": "tests.AndroidProgramSelectorTests",
    "kegel": "tests.KegelExerciseTests",
    "update": "tests.AndroidUpdateTests",
    "release": "tests.AndroidReleaseTests",
    "push": "tests.AndroidPushPermissionTests",
    "billing": "tests.AndroidPurchaseTests",
    "test-model": "tests.AndroidTestModelTests",
    "slides": "tests.OnboardingSlidesTests",
    "clean-install": "tests.OnboardingGoalProgramTests,tests.ProgramSwitchFeatureTests",
    "localization": "suites.LocalizationSuite",
    "safe": "suites.AndroidRegressionSuite",
    "functional": ",".join((
        "tests.CoachAuthorizationTests",
        "tests.AndroidSmokeTests",
        "tests.AndroidReleaseTests",
        "tests.CoachProfileTests",
        "tests.DailyPlanTests",
        "tests.ConfiguredExploreTests",
        "tests.AndroidExploreTests",
        "tests.AndroidProgramSelectorTests",
        "tests.ModulesTests",
        "tests.KegelExerciseTests",
        "tests.PdfGuideTests",
    )),
}

APP_VARIANTS = {
    "male": ("com.vamapps.thecoach", "com.vamapps.thecoach.MainActivity"),
    "female": ("com.vamapps.thecoach.forher", "com.vamapps.thecoach.MainActivity"),
}
POST_AUTH_GOALS = {"male": "Beat premature ejaculation", "female": "Cultivate desire"}
CLEAN_INSTALL_SUITES = {"push", "billing", "test-model", "clean-install"}
UID_REQUIRED_SUITES = {"safe", "functional", "kegel", "test-model", "update"}
MALE_ONLY_SUITES = set(SUITES) - {"auth", "profile", "localization"}
SUITE_ENV_REQUIREMENTS = {
    "auth": ("COACH_VALID_EMAIL_WITHOUT_PROGRESS",),
    "test-model": ("COACH_VALID_EMAIL_WITHOUT_PROGRESS",),
    "release": ("COACH_NO_PDF_ENTITLEMENT_EMAIL", "COACH_NO_PDF_ENTITLEMENT_OTP",
                "COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID"),
    "functional": ("COACH_VALID_EMAIL_WITHOUT_PROGRESS", "COACH_NO_PDF_ENTITLEMENT_EMAIL",
                   "COACH_NO_PDF_ENTITLEMENT_OTP", "COACH_NO_PDF_ENTITLEMENT_FIXTURE_ID"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--suite", choices=SUITES, default="safe")
    parser.add_argument("--variant", choices=APP_VARIANTS, default="male")
    parser.add_argument("--method", help="A method in the selected test class")
    parser.add_argument("--old-apk", type=Path)
    parser.add_argument("--new-apk", type=Path)
    parser.add_argument("--fixture-service", default=MODULES_PREMIUM_SERVICE)
    parser.add_argument("--slides-fixture", type=Path)
    parser.add_argument("--prepared-slides", action="store_true",
                        help="The first uncompleted slide is already open; preserve this installation")
    parser.add_argument("--post-auth-goal", help="Explicit goal for separate, opt-in fixture preparation; does not enable it")
    parser.add_argument("--confirm-immersive-mode", action="store_true",
                        help="Confirm the OS full-screen hint on the selected test emulator (opt-in)")
    args = parser.parse_args()
    if not re.fullmatch(r"emulator-[0-9]+", args.serial):
        parser.error("Select the explicit test emulator")
    if args.method and (not re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*", args.method) or "," in SUITES[args.suite]):
        parser.error("Select one exact method in a single test class; Surefire 2.12.4 does not support method1+method2")
    if args.suite == "update" and (not args.old_apk or not args.new_apk):
        parser.error("Update requires both --old-apk and --new-apk")
    if args.suite == "slides" and (not args.slides_fixture or not args.slides_fixture.is_file()):
        parser.error("Slides require --slides-fixture from this Android build's active configuration")
    if args.suite == "welcome" and args.variant != "male":
        parser.error("The male Welcome suite cannot run against the female app variant")
    if args.variant == "female" and args.suite in MALE_ONLY_SUITES:
        parser.error("Suite has no reviewed female manifest; use auth, profile, or localization")
    if args.suite in CLEAN_INSTALL_SUITES and not args.new_apk:
        parser.error("Clean-install suite requires --new-apk")
    if args.suite == "billing" and (
            os.environ.get("GOOGLE_PLAY_LICENSE_TESTER", "").lower() != "true"
            or os.environ.get("PURCHASE_ALLOW", "").lower() != "true"):
        parser.error("Billing requires GOOGLE_PLAY_LICENSE_TESTER=true and PURCHASE_ALLOW=true")
    missing_environment = [name for name in SUITE_ENV_REQUIREMENTS.get(args.suite, ())
                           if not os.environ.get(name)]
    if missing_environment:
        parser.error("Suite data contract is incomplete. Missing: " + ", ".join(missing_environment))
    record = json.loads(keychain(args.fixture_service))
    required_record_fields = ["email", "otp"] + (["uid"] if args.suite in UID_REQUIRED_SUITES else [])
    if not all(record.get(k) for k in required_record_fields):
        parser.error("Account record is missing required fields: " + ", ".join(required_record_fields))
    record_variant = record.get("variant", args.variant).lower()
    if record_variant != args.variant:
        parser.error("Account record variant does not match --variant")
    required_environment = SUITE_ENV_REQUIREMENTS.get(args.suite, ())
    no_progress_email = os.environ.get("COACH_VALID_EMAIL_WITHOUT_PROGRESS")
    if "COACH_VALID_EMAIL_WITHOUT_PROGRESS" in required_environment \
            and no_progress_email.lower() == record["email"].lower():
        parser.error("Existing-progress and valid-no-progress accounts must be distinct")
    no_pdf_email = os.environ.get("COACH_NO_PDF_ENTITLEMENT_EMAIL")
    if "COACH_NO_PDF_ENTITLEMENT_EMAIL" in required_environment \
            and no_pdf_email.lower() == record["email"].lower():
        parser.error("Existing-progress and no-PDF entitlement accounts must be distinct")
    package_name, activity_name = APP_VARIANTS[args.variant]
    adb = os.environ.get("ADB", "adb")
    version = subprocess.check_output([adb, "-s", args.serial, "shell", "getprop", "ro.build.version.release"],
                                      text=True, timeout=15).strip()
    env = os.environ.copy()
    env.update(COACH_EXISTING_PROGRESS_EMAIL=record["email"], COACH_EXISTING_PROGRESS_OTP=record["otp"],
               COACH_DAILY_PLAN_DAY="Stage 1", COACH_FIXTURE_RESET_MODE="prebuilt", COACH_ACCOUNT_VARIANT=args.variant,
               ANDROID_APP_VARIANT=args.variant, TEST_ISOLATION_MODE="logout",
               POST_AUTH_QUESTIONNAIRE_GOAL=args.post_auth_goal or record.get("onboardingGoal") or POST_AUTH_GOALS[args.variant])
    if record.get("uid"):
        env["COACH_EXPECTED_ACCOUNT_UID"] = record["uid"]
    if args.suite == "safe":
        for field in ("email", "otp", "uid"):
            env["COACH_COA9044_" + field.upper()] = record[field]
    # Dedicated, mutable fixtures must be supplied explicitly; never repurpose the module account.
    if args.suite in ("kegel", "test-model") and args.fixture_service != MODULES_PREMIUM_SERVICE:
        env.update(COACH_KEGEL_PLAYER_EMAIL=record["email"], COACH_KEGEL_PLAYER_OTP=record["otp"],
                   COACH_ANDROID_MUTABLE_UID=record["uid"])
    test = SUITES[args.suite] + ("#" + args.method if args.method else "")
    cmd = ["mvn", "-q", "test", "-Dtest=" + test, "-Dplatform=android", "-Dandroid.udid=" + args.serial,
           "-Dandroid.deviceName=" + args.serial, "-Dandroid.platformVersion=" + version, "-Dandroid.noReset=true", "-Dandroid.fullReset=false",
           "-Dandroid.appPackage=" + package_name, "-Dandroid.appActivity=" + activity_name,
           "-Dandroid.app.variant=" + args.variant]
    if args.suite == "safe":
        cmd += ["-Dcoach.modules.enabled=true", "-Dcoach.modules.programId=last_longer"]
    fresh_slides = (args.suite == "slides" and not args.prepared_slides
                    and args.method != "testExistingProgressUserDoesNotSeeSlides")
    if (fresh_slides or args.suite in CLEAN_INSTALL_SUITES) and not (args.suite == "test-model" and args.method == "testValidEmailEnablesContinue"):
        if not args.new_apk: parser.error("Clean-install suite requires --new-apk")
        cmd = [arg for arg in cmd if not arg.startswith(("-Dandroid.noReset=", "-Dandroid.fullReset="))]
        cmd += ["-Dandroid.noReset=false", "-Dandroid.fullReset=true", "-Dandroid.autoGrantPermissions=" + ("false" if args.suite == "push" else "true"), "-Dandroid.app=" + str(args.new_apk.resolve())]
    if args.suite == "slides":
        cmd += ["-Donboarding.slides.fixture=" + str(args.slides_fixture.resolve()),
                "-Donboarding.slides.prepared=" + str(args.prepared_slides).lower()]
    if args.suite == "update" and args.new_apk:
        cmd.append("-Dandroid.app=" + str(args.new_apk.resolve()))
    if args.new_apk:
        env["ANDROID_RECOVERY_MODE"] = "reinstall"
        if not any(argument.startswith("-Dandroid.app=") for argument in cmd):
            cmd.append("-Dandroid.app=" + str(args.new_apk.resolve()))
    else:
        env["ANDROID_RECOVERY_MODE"] = "relaunch"
    for key, value in (("old", args.old_apk), ("new", args.new_apk)):
        if value:
            if not value.is_file(): parser.error("APK does not exist: " + str(value))
            cmd.append("-Dandroid.update." + key + "=" + str(value.resolve()))
    if args.confirm_immersive_mode:
        # The local API-36 capture showed an OS-owned hint absent from UiAutomator's tree.
        # Validate account/APK configuration first; this must never run for rejected input.
        subprocess.check_output([adb, "-s", args.serial, "shell", "settings", "put", "secure",
                                 "immersive_mode_confirmations", "confirmed"], text=True, timeout=15)
    start = time.time()
    archive = ROOT / "target" / ("android-parity-" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    archive.mkdir(parents=True, exist_ok=False)
    with (archive / "run.log").open("w") as log:
        process = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            for field in ("email", "otp", "uid"):
                if record.get(field): line = line.replace(record[field], "<redacted>")
            log.write(line); log.flush(); print(line, end="", flush=True)
        code = process.wait()
    for directory in ("surefire-reports", "screenshots", "page-source"):
        for source in (ROOT / "target" / directory).glob("*"):
            if source.is_file() and source.stat().st_mtime >= start:
                destination = archive / directory / source.name
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)
    print("Android parity artifacts:", archive)
    return code


if __name__ == "__main__":
    sys.exit(main())
