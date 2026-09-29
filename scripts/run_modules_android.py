#!/usr/bin/env python3
"""Run the seven shared module scenarios on the explicitly approved Android account."""
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
from run_test_model_local import keychain, MODULES_PREMIUM_SERVICE, ROOT

METHODS = ("testModuleTitleAndStageAreVisible", "testHeaderBoundsAndPeerOverlap",
           "testNextStageWithoutCompletingActivities", "testPreviousStagesBackToFirst",
           "testLastStageIsAccessibleWithoutCompletion", "testPreviousFromLastStage",
           "testNextModuleBlockedAndGotItRetainsStage")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--method", choices=METHODS)
    parser.add_argument("--setup", action="store_true", help="Resume previously approved initial Last Longer onboarding")
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Module evidence currently supports rooted test emulators only")
    if args.setup and args.method:
        parser.error("Run initial setup separately from a module scenario")
    device = subprocess.run([os.environ.get("ADB", "adb"), "-s", args.serial, "shell", "id", "-u"],
                            capture_output=True, text=True, timeout=15)
    if device.returncode or device.stdout.strip() != "0":
        raise RuntimeError("Start the selected test emulator and enable adb root before module evidence collection")
    adb = [os.environ.get("ADB", "adb"), "-s", args.serial]
    package = subprocess.check_output(adb + ["shell", "dumpsys", "package", "com.vamapps.thecoach"],
                                      text=True, timeout=15)
    if not re.search(r"versionName=1\.40\.21(?:\s|$)", package):
        raise RuntimeError("Install the approved manProd 1.40.21 APK before this run")
    android_version = subprocess.check_output(adb + ["shell", "getprop", "ro.build.version.release"],
                                             text=True, timeout=15).strip()
    record = json.loads(keychain(MODULES_PREMIUM_SERVICE))
    if record.get("allowedCases") != ["COA-9044"] or not record.get("uid"):
        raise RuntimeError("An approved, UID-verified module account is required")
    env = os.environ.copy()
    for field in ("email", "otp", "uid"):
        env["COACH_COA9044_" + field.upper()] = record[field]
    env["COACH_EXISTING_PROGRESS_EMAIL"] = record["email"]
    env["COACH_EXISTING_PROGRESS_OTP"] = record["otp"]
    test = "tests.AndroidModulesSetupTests" if args.setup else (
        "tests.ModulesTests#" + args.method if args.method else "suites.ModulesSuite")
    cmd = ["mvn", "-q", "test", "-Dtest=" + test, "-Dplatform=android",
           "-Dandroid.udid=" + args.serial, "-Dandroid.deviceName=" + args.serial,
           "-Dandroid.platformVersion=" + android_version, "-Dandroid.appPackage=com.vamapps.thecoach",
           "-Dandroid.appActivity=com.vamapps.thecoach.MainActivity",
           "-Dandroid.noReset=true", "-Dandroid.fullReset=false",
           "-Dcoach.modules.programId=last_longer",
           "-Dcoach.modules.completeOnboarding=true" if args.setup else "-Dcoach.modules.enabled=true"]
    start = time.time()
    print("Android modules: " + ("approved initial setup" if args.setup else test), flush=True)
    p = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in p.stdout:
        for field in ("email", "otp"):
            line = line.replace(record[field], "<redacted>")
        print(line, end="", flush=True)
    code = p.wait()
    archive = ROOT / "target" / ("android-modules-" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    for name in ("surefire-reports", "screenshots", "page-source", "appium-logs"):
        for source in (ROOT / "target" / name).glob("*"):
            if source.is_file() and source.stat().st_mtime >= start:
                destination = archive / name / source.name
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)
    print("Run artifacts: " + str(archive))
    return code


if __name__ == "__main__":
    sys.exit(main())
