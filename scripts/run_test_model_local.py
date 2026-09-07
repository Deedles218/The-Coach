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
PREMIUM_SERVICE = "the-coach-test-model-premium"
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
    "COA-7949": "test02Coa7949ValidEmailAuthorization",
    "COA-7950": "test03Coa7950InvalidEmailIsRejected",
    "COA-8235": "test04Coa8235DailyPlanProgramsMatchExplore",
    "COA-8511": "test05Coa8511RepeatedPostponeMovesCardToTargetDay",
    "COA-8512": "test06Coa8512PostponeDoesNotChangeProgressAfterRestart",
    "COA-8517": "test07Coa8517RemovedExerciseCanBeRecovered",
    "COA-8518": "test08Coa8518FirstLockedNextDayTapShowsPopup",
}

def keychain(service, optional=False):
    result = subprocess.run(["security", "find-generic-password", "-a", OWNER,
                             "-s", service, "-w"], capture_output=True, text=True)
    if result.returncode:
        if optional and result.returncode == 44:
            return None
        raise RuntimeError("Keychain item unavailable: " + service)
    return result.stdout.strip()

def save(accounts, service=SERVICE):
    # Never echo the command or its payload. These are local account records.
    result = subprocess.run(["security", "add-generic-password", "-U", "-a", OWNER,
                             "-s", service, "-w", json.dumps(accounts)], capture_output=True)
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
    if getattr(args, "inspect_premium", False):
        cmd += ["-Dtest=tests.TestModelPremiumInspectionTests#inspectApprovedPremiumAccountWithoutReset",
                "-Dcoach.testModel.inspectPremium=true", "-Dcoach.testModel.case=" + key]
        if getattr(args, "inspect_programs", False):
            cmd += ["-Dcoach.testModel.inspectOtherPrograms=true"]
    elif args.provision:
        cmd += ["-Dtest=tests.TestModelAccountProvisioningTests#testProvisionAndVerifyDedicatedAccount",
                "-Dcoach.testModel.provision=true", "-Dcoach.testModel.case=" + key]
    else:
        test_class = "TodayProgramSelectorTests" if key in SELECTOR_CASES else "TestModelAutomationTests"
        cmd += ["-Dtest=tests." + test_class + "#" + METHODS[key]]
    cmd += args.maven
    secrets = [data[field] for data in accounts.values() if isinstance(data, dict)
               for field in ("email", "otp") if field in data]
    secrets.append(env["COACH_VALID_EMAIL_WITHOUT_PROGRESS"])
    mode = ": read-only Premium inspection" if getattr(args, "inspect_premium", False) else (
        ": provisioning" if args.provision else ": running")
    if getattr(args, "inspect_programs", False):
        mode = ": program inspection with mandatory restoration"
    print(key + mode, flush=True)
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
    parser.add_argument("--inspect-premium", action="store_true", help="Inspect the approved shared Premium account without reset or card mutations")
    parser.add_argument("--inspect-programs", action="store_true", help="Temporarily view Overall Health/Kegel and verify restoration of the original program, day and progress")
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
    if "COA-8518" in args.cases:
        raise RuntimeError("COA-8518 is deferred by the user: Coach for Her only; no run or provisioning performed")
    if args.inspect_programs and not args.inspect_premium:
        raise RuntimeError("Program inspection requires the approved Premium inspection mode")
    if args.inspect_premium:
        if args.provision or any(key not in ("COA-8511", "COA-8512") for key in args.cases):
            raise RuntimeError("Premium inspection accepts only COA-8511/8512 and never provisions accounts")
        approved = json.loads(keychain(PREMIUM_SERVICE))
        if approved.get("allowedCases") != ["COA-8511", "COA-8512"]:
            raise RuntimeError("Shared Premium inspection requires the explicitly approved case scope")
        inspection_accounts = dict(accounts)
        inspection_accounts.update({key: approved for key in approved["allowedCases"]})
        failed = False
        for key in args.cases:
            result = run(key, approved, inspection_accounts, args)
            # Preserve new fixture approvals/diagnostics saved during a long UI run.
            latest = json.loads(keychain(PREMIUM_SERVICE))
            if any(latest.get(field) != approved.get(field) for field in ("email", "uid")):
                raise RuntimeError("Approved Premium identity changed during inspection; stale registry write refused")
            latest.setdefault("inspectionExit", {})[key] = result
            save(latest, PREMIUM_SERVICE)
            approved = latest
            failed = failed or result != 0
            if result != 0:
                break
        return 1 if failed else 0
    if not args.provision and any(key in ("COA-8511", "COA-8512") for key in args.cases):
        shared = keychain(PREMIUM_SERVICE, optional=True)
        if shared:
            from test_model_kegel_fixture import require_approval
            approved = json.loads(shared)
            if any(key not in ("COA-8511", "COA-8512") for key in args.cases):
                raise RuntimeError("Run shared Kegel fixtures separately from other accounts")
            for key in args.cases:
                require_approval(key, approved)
            run_accounts = dict(accounts)
            run_accounts.update({key: approved for key in ("COA-8511", "COA-8512")})
            for key in args.cases:
                result = run(key, approved, run_accounts, args)
                latest = json.loads(keychain(PREMIUM_SERVICE))
                if any(latest.get(field) != approved.get(field) for field in ("email", "uid")):
                    raise RuntimeError("Approved identity changed during the test; stale registry write refused")
                latest.setdefault("kegelTestExit", {})[key] = result
                latest["kegelFixtureReady"] = all(latest["kegelTestExit"].get(case) == 0 for case in ("COA-8511", "COA-8512"))
                latest["activeCustomizationProgram"] = "kegel_only"
                save(latest, PREMIUM_SERVICE)
                if result != 0: return 1
            return 0
    base_email = keychain("the-coach-test-model-email")
    base_otp = keychain("the-coach-test-model-otp")
    for key in ("COA-7947", "COA-7949", "COA-7950", "COA-8235"):
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
