#!/usr/bin/env python3
"""Narrow fixture/evidence bridge for the JUnit suite; credentials come from env."""
import argparse
import json
import os
from pathlib import Path
import sys
import time

from test_model_app_api import PreprodAppClient
from test_model_fixture_api import PreprodFixtures


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
    parser.add_argument("operation", choices=["prepare-recovery", "verify-recovery", "reset-recovery"])
    parser.add_argument("--udid", required=True)
    parser.add_argument("--evidence", required=True)
    args = parser.parse_args()
    case = "COA-8517"
    account = {"email": os.environ.get("COACH_COA8517_EMAIL", ""),
               "uid": os.environ.get("COACH_COA8517_UID", ""), "status": "confirmed"}
    client = PreprodAppClient(case, account, args.udid)
    folder = Path(args.evidence)
    folder.mkdir(parents=True, exist_ok=True)
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
        sys.exit(1)
