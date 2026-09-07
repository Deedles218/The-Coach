#!/usr/bin/env python3
"""Narrow fixture/evidence bridge for the JUnit suite; credentials come from env."""
import argparse
import json
import os
from pathlib import Path
import sys
import time
import sqlite3
import subprocess
import urllib.error
import urllib.parse
from contextlib import closing

from test_model_app_api import PreprodAppClient
from test_model_fixture_api import PreprodFixtures, verify_preprod_account_email


def verify_recovered_days(client, fixture, data):
    """The day endpoint also changes current day; restore the isolated fixture."""
    days = []
    try:
        for index, day in enumerate(data["applicableDays"], 1):
            section = client.daily(data["program"], day)["program_section"]
            matches = [item for item in section["questions"]
                       if item.get("name") == data["actionName"] and item.get("headline") == data["title"]]
            days.append({"day": day, "returnedDay": section["day_in_program"], "matchingCards": len(matches)})
            if index % 10 == 0:
                print("COA-8517 checked " + str(index) + " program days", flush=True)
    finally:
        if fixture.move_to_day(data["program"], 2) != ["Transfer completed"]:
            raise RuntimeError("Could not return the recovery fixture to day 2")
        restored = client.daily(data["program"], 2)["program_section"]
        if restored["day_in_program"] != 2:
            raise RuntimeError("Recovery fixture did not return to day 2")
    return days


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["prepare-recovery", "verify-recovery", "reset-recovery", "verify-access", "verify-existing-account", "inspect-premium", "approve-kegel", "prepare-kegel", "verify-kegel"])
    parser.add_argument("--case", default="COA-8517", choices=["COA-7949", "COA-8511", "COA-8512", "COA-8517", "COA-8518"])
    parser.add_argument("--udid", required=True)
    parser.add_argument("--evidence", required=True)
    args = parser.parse_args()
    case = args.case
    folder = Path(args.evidence)
    folder.mkdir(parents=True, exist_ok=True)
    if args.operation.endswith("-kegel"):
        from run_test_model_local import keychain, PREMIUM_SERVICE
        from test_model_kegel_fixture import KegelFixture, require_approval
        account = json.loads(keychain(PREMIUM_SERVICE))
        require_approval(case, account)
        prefix = "COACH_" + case.replace("-", "")
        if any(account.get(field) != os.environ.get(prefix + "_" + field.upper()) for field in ("email", "uid")):
            raise RuntimeError("Kegel credentials do not match the approved registry")
        client = KegelFixture(case, account, args.udid)
        if args.operation == "approve-kegel":
            data = {"case": case, "uid": account["uid"], "program": "kegel_only", "resetAllowed": True}
        elif args.operation == "prepare-kegel":
            data = client.prepare_first_move()
        else:
            data = client.verify_second_move()
        data.update(case=case, checkedAt=int(time.time() * 1000))
        (folder / (args.operation + ".json")).write_text(json.dumps(data, indent=2) + "\n")
        print(case + " " + args.operation + " verified")
        return
    if args.operation in ("verify-existing-account", "inspect-premium"):
        inspection = args.operation == "inspect-premium"
        if (inspection and case not in ("COA-8511", "COA-8512")) or (not inspection and case != "COA-7949"):
            raise RuntimeError("Account verification requested for an unsupported case")
        container = subprocess.check_output(["xcrun", "simctl", "get_app_container", args.udid,
                                            "com.vamapps.preprod.The-Coach", "data"], text=True).strip()
        database = Path(container) / "Library/com.amplitude.database"
        with closing(sqlite3.connect(database.as_uri() + "?mode=ro", uri=True)) as connection:
            row = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        uid = row[0] if row else ""
        email = os.environ.get("COACH_" + case.replace("-", "") + "_EMAIL", "")
        if inspection:
            account = {"email": email, "uid": uid, "status": "confirmed"}
            client = PreprodAppClient(case, account, args.udid, read_only=True)
            data = client.customization_access()
            data.update(uid=uid, readOnly=True)
            (folder / "inspection.json").write_text(json.dumps(data, indent=2) + "\n")
            print(case + " approved account inspected without program/reset operations")
            if not data["accessReady"]:
                raise RuntimeError("Approved account has no confirmed Premium access on this build/environment")
            return
        verify_preprod_account_email(uid, email)
        (folder / "account.json").write_text(json.dumps({"case": case, "uid": uid,
            "emailVerified": True, "checkedAt": int(time.time() * 1000)}) + "\n")
        print("COA-7949 authenticated identity matches the requested existing account")
        return
    if args.operation != "verify-access" and case != "COA-8517":
        raise RuntimeError("Recovery operations are restricted to COA-8517")
    prefix = "COACH_" + case.replace("-", "")
    account = {"email": os.environ.get(prefix + "_EMAIL", ""),
               "uid": os.environ.get(prefix + "_UID", ""), "status": "confirmed"}
    client = PreprodAppClient(case, account, args.udid)
    if args.operation == "verify-access":
        data = client.customization_access()
        (folder / "access.json").write_text(json.dumps(data, indent=2) + "\n")
        if not data["accessReady"]:
            raise RuntimeError(case + " environment prerequisites: " + ", ".join(data["blockers"]))
        print(case + " access prerequisites confirmed; fixture state still requires validation")
        return
    manifest = folder / "prepared.json"
    if args.operation == "reset-recovery":
        if PreprodFixtures(case, account).move_to_day("overall_health", 2) != ["Transfer completed"]:
            raise RuntimeError("Could not reset the interrupted recovery fixture")
        section = client.daily("overall_health", 2)["program_section"]
        if section["day_in_program"] != 2:
            raise RuntimeError("Interrupted recovery fixture did not return to day 2")
        (folder / "reset.json").write_text(json.dumps({"case": case, "fixtureReturnedToDay": 2}) + "\n")
        return
    if args.operation == "prepare-recovery":
        fixture = PreprodFixtures(case, account)
        if fixture.set_program("overall_health") != ["Abtest program set."]:
            raise RuntimeError("The base Overall Health variant was not confirmed")
        if fixture.move_to_day("overall_health", 2) != ["Transfer completed"]:
            raise RuntimeError("The prepared Overall Health day was not confirmed")
        data = client.prepare_removed_meal_plan()
        data.update(case=case, preparedAt=int(time.time() * 1000))
        manifest.write_text(json.dumps(data, indent=2) + "\n")
        print("COA-8517 removed-card fixture confirmed")
        return
    data = json.loads(manifest.read_text())
    if data["case"] != case or int(time.time() * 1000) - data["preparedAt"] > 600_000:
        raise RuntimeError("Recovery evidence requires this run's fresh fixture manifest")
    if data["program"] != "overall_health" or data["actionName"] != "exercise_meal_plan_check":
        raise RuntimeError("Unexpected recovery fixture identity")
    removed = client.get("/api/v1/coachprogram/removed_actions/", program_id=data["program"])["items"]
    if any(item.get("headline") == data["title"] for item in removed):
        raise RuntimeError("Restored card is still present in Removed exercises")

    days = verify_recovered_days(client, PreprodFixtures(case, account), data)
    result = {"case": case, "checkedAt": int(time.time() * 1000), "title": data["title"],
              "days": days, "removedListClear": True, "fixtureReturnedToDay": 2,
              "passed": all(day["day"] == day["returnedDay"] and day["matchingCards"] == 1 for day in days)}
    (folder / "recovered.json").write_text(json.dumps(result, indent=2) + "\n")
    if not result["passed"]:
        raise RuntimeError("The restored card is missing or duplicated on a program day; see recovered.json")
    print("COA-8517 recovery verified on all " + str(len(days)) + " applicable program days")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        # Low-level request exceptions may contain cached header values.
        print("Fixture command failed (" + type(error).__name__ + ")", file=sys.stderr)
        if isinstance(error, RuntimeError):
            print(str(error), file=sys.stderr)
        elif isinstance(error, urllib.error.HTTPError):
            print("HTTP " + str(error.code) + " for " + urllib.parse.urlsplit(error.url).path, file=sys.stderr)
        sys.exit(1)
