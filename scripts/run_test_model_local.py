#!/usr/bin/env python3
"""Local macOS runner. Credentials stay in Keychain and child environment.

Provisioning is explicit, creates at most one durable alias per case, and
records confirmation only after the app confirms the email and re-login works.
"""
import argparse
import getpass
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SERVICE = "the-coach-test-model-accounts"
OWNER = getpass.getuser()
READ_ONLY_SELECTOR_CASES = ("COA-8228", "COA-8230", "COA-8227")
SELECTOR_CASES = READ_ONLY_SELECTOR_CASES + ("COA-8232", "COA-8231")
METHODS = {
    "COA-8232": "testCoa8232SelectingProgramUpdatesToday",
    "COA-8231": "testCoa8231ActiveProgramIsFirstAfterSwitch",
    "COA-8228": "testCoa8228SwipeDownClosesSelector",
    "COA-8230": "testCoa8230ProgramsScrollVertically",
    "COA-8227": "testCoa8227OpensSelectorWithAnalytics",
    "COA-7947": "test01Coa7947ValidEmailEnablesContinue",
    "COA-8235": "test04Coa8235DailyPlanProgramsMatchExplore",
    "COA-8517": "test07Coa8517RemovedExerciseCanBeRecovered",
}

def keychain(service, optional=False):
    result = subprocess.run(["security", "find-generic-password", "-a", OWNER,
                             "-s", service, "-w"], capture_output=True, text=True)
    if result.returncode:
        if optional and result.returncode == 44:
            return None
        raise RuntimeError("Keychain item unavailable: " + service)
    return result.stdout.strip()

def save(accounts):
    # Never echo the command or its payload. These are local account records.
    result = subprocess.run(["security", "add-generic-password", "-U", "-a", OWNER,
                             "-s", SERVICE, "-w", json.dumps(accounts)], capture_output=True)
    if result.returncode:
        raise RuntimeError("Could not save account registry to Keychain")

def run(key, record, accounts, args):
    env = os.environ.copy()
    env["IOS_SIMULATOR_APP"] = args.app
    for case, data in accounts.items():
        if not isinstance(data, dict) or "email" not in data:
            continue
        prefix = "COACH_" + case.replace("-", "")
        env[prefix + "_EMAIL"] = data["email"]
        env[prefix + "_OTP"] = data["otp"]
        if data.get("uid"):
            env[prefix + "_UID"] = data["uid"]
    env["COACH_EXISTING_PROGRESS_EMAIL"] = record["email"]
    env["COACH_EXISTING_PROGRESS_OTP"] = record["otp"]
    local, domain = record["email"].split("@", 1)
    env["COACH_VALID_EMAIL_WITHOUT_PROGRESS"] = local.split("+", 1)[0] + "+validation@" + domain
    cmd = ["bash", str(ROOT / "ci-scripts/run-ios-simulator.sh"),
           "-Dios.noReset=true", "-Dios.fullReset=false"]
    if args.provision:
        cmd += ["-Dtest=tests.TestModelAccountProvisioningTests#testProvisionAndVerifyDedicatedAccount",
                "-Dcoach.testModel.provision=true", "-Dcoach.testModel.case=" + key]
    else:
        test_class = "TodayProgramSelectorTests" if key in SELECTOR_CASES else "TestModelAutomationTests"
        cmd += ["-Dtest=tests." + test_class + "#" + METHODS[key]]
    cmd += args.maven
    secrets = [data[field] for data in accounts.values() if isinstance(data, dict)
               for field in ("email", "otp") if field in data]
    secrets.append(env["COACH_VALID_EMAIL_WITHOUT_PROGRESS"])
    print(key + (": provisioning" if args.provision else ": running"), flush=True)
    process = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True)
    for line in process.stdout:
        for value in secrets:
            line = line.replace(value, "<redacted>")
        sys.stdout.write(line)
        sys.stdout.flush()
    return process.wait()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("cases", nargs="*", choices=list(METHODS))
    parser.add_argument("--provision", action="store_true")
    parser.add_argument("--status", action="store_true")
    parser.add_argument("--app", default="/Users/deedles/Downloads/The Coach 3.app")
    parser.add_argument("--maven", action="append", default=[])
    args = parser.parse_args()
    accounts = json.loads(keychain(SERVICE, optional=True) or "{}")
    if args.status:
        print(json.dumps({key: {k: v for k, v in data.items() if k not in ("email", "otp")}
                          for key, data in accounts.items()}, indent=2))
        return 0
    if not args.cases:
        parser.error("Choose explicit case keys; no full regression runs implicitly")
    base_email = keychain("the-coach-test-model-email")
    base_otp = keychain("the-coach-test-model-otp")
    for key in ("COA-7947", "COA-8235"):
        accounts.setdefault(key, {"email": base_email, "otp": base_otp, "status": "confirmed"})
    failed = False
    for key in args.cases:
        if key in READ_ONLY_SELECTOR_CASES:
            if args.provision:
                raise RuntimeError("Selector cases reuse the confirmed COA-8235 fixture; provisioning is unnecessary")
            accounts.setdefault(key, dict(accounts["COA-8235"]))
        if key not in accounts:
            if not args.provision:
                raise RuntimeError(key + ": no dedicated account; run --provision first")
            local, domain = base_email.split("@", 1)
            alias = local.split("+", 1)[0] + "+" + key.replace("-", "").lower() + "@" + domain
            accounts[key] = {"email": alias, "otp": base_otp, "status": "planned",
                             "program": "Overall Health" if key == "COA-8517" else "Last Longer"}
            save(accounts)
        record = accounts[key]
        if args.provision and record["status"] == "confirmed":
            print(key + ": confirmed account already exists; preserved")
            continue
        result = run(key, record, accounts, args)
        if args.provision and result == 0:
            record["status"] = "confirmed"
            identity = json.loads((ROOT / "target/test-model-fixtures" / (key + ".json")).read_text())
            if not identity.get("reloginVerified"):
                raise RuntimeError("Missing verified identity for " + key)
            record["uid"] = identity["uid"]
        record["lastSetupExit" if args.provision else "lastTestExit"] = result
        save(accounts)
        failed = failed or result != 0
        if args.provision and result != 0:
            break  # Preserve and resume this pending registration before starting another.
    return 1 if failed else 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        sys.exit(2)
