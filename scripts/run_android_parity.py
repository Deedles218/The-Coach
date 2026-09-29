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
    "explore": "tests.ExploreTests",
    "explore-cards": "tests.AndroidExploreTests",
    "welcome": "tests.MaleBuildStartScreenTests",
    "selector": "tests.AndroidProgramSelectorTests",
    "kegel": "tests.KegelExerciseTests",
    "update": "tests.AndroidUpdateTests",
    "release": "tests.AndroidReleaseTests",
    "push": "tests.AndroidPushPermissionTests",
    "billing": "tests.AndroidPurchaseTests",
    "test-model": "tests.AndroidTestModelTests",
    "safe": "suites.AndroidRegressionSuite",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--suite", choices=SUITES, default="safe")
    parser.add_argument("--method", help="A method in the selected test class")
    parser.add_argument("--old-apk", type=Path)
    parser.add_argument("--new-apk", type=Path)
    parser.add_argument("--fixture-service", default=MODULES_PREMIUM_SERVICE)
    args = parser.parse_args()
    if not re.fullmatch(r"emulator-[0-9]+", args.serial):
        parser.error("Select the explicit test emulator")
    if args.method and not all(method.replace('_','').isalnum() for method in args.method.split('+')):
        parser.error("Invalid method name")
    if args.suite == "update" and (not args.old_apk or not args.new_apk):
        parser.error("Update requires both --old-apk and --new-apk")
    version = subprocess.check_output([os.environ.get("ADB","adb"),"-s",args.serial,"shell","getprop","ro.build.version.release"], text=True,timeout=15).strip()
    record = json.loads(keychain(args.fixture_service))
    if not all(record.get(k) for k in ("uid", "email", "otp")):
        parser.error("Use a UID-verified account record")
    env = os.environ.copy()
    env.update(COACH_EXISTING_PROGRESS_EMAIL=record["email"], COACH_EXISTING_PROGRESS_OTP=record["otp"],
               COACH_EXPECTED_ACCOUNT_UID=record["uid"], COACH_VALID_EMAIL_WITHOUT_PROGRESS=record["email"],
               COACH_DAILY_PLAN_DAY="Stage 1", COACH_FIXTURE_RESET_MODE="prebuilt")
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
           "-Dandroid.appPackage=com.vamapps.thecoach", "-Dandroid.appActivity=com.vamapps.thecoach.MainActivity"]
    if args.suite == "safe":
        cmd += ["-Dcoach.modules.enabled=true", "-Dcoach.modules.programId=last_longer"]
    if args.suite in ("push", "billing", "test-model") and not (args.suite == "test-model" and args.method == "testValidEmailEnablesContinue"):
        if not args.new_apk: parser.error("Clean-install suite requires --new-apk")
        cmd = [arg for arg in cmd if not arg.startswith(("-Dandroid.noReset=", "-Dandroid.fullReset="))]
        cmd += ["-Dandroid.noReset=false", "-Dandroid.fullReset=true", "-Dandroid.autoGrantPermissions=" + ("false" if args.suite == "push" else "true"), "-Dandroid.app=" + str(args.new_apk.resolve())]
    if args.suite == "update" and args.new_apk:
        cmd.append("-Dandroid.app=" + str(args.new_apk.resolve()))
    for key, value in (("old", args.old_apk), ("new", args.new_apk)):
        if value:
            if not value.is_file(): parser.error("APK does not exist: " + str(value))
            cmd.append("-Dandroid.update." + key + "=" + str(value.resolve()))
    start = time.time()
    archive = ROOT / "target" / ("android-parity-" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    archive.mkdir(parents=True, exist_ok=False)
    with (archive / "run.log").open("w") as log:
        process = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            for secret in (record["email"], record["otp"]): line = line.replace(secret, "<redacted>")
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
