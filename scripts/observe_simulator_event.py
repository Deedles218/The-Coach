#!/usr/bin/env python3
"""Read-only observer of a fresh event in this simulator's Amplitude queue.

Arms before the UI action. Requires a new SDK event identity and a current timestamp for
the verified test UID. Persists only minimal non-credential evidence, never the
event payload, user properties or raw analytics database.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import sys
import time


def event_key(payload):
    item = json.loads(payload)
    # SDK UUID survives SQLite queue maintenance; SQL row IDs may restart.
    identity = str(item["uuid"]) if item.get("uuid") else payload
    return hashlib.sha256(identity.encode("utf-8")).hexdigest()


def validate_program_expectation(event, expected_program_id):
    if event == "ProgramModalSelect":
        if not isinstance(expected_program_id, str) or not expected_program_id.strip():
            raise RuntimeError("ProgramModalSelect requires an exact expected program_id")
    elif expected_program_id is not None:
        raise RuntimeError("Expected program_id is supported only for ProgramModalSelect")


def observe(database, expected_uid, event, output, timeout=40, expected_program_id=None):
    validate_program_expectation(event, expected_program_id)
    connection = sqlite3.connect(Path(database).resolve().as_uri() + "?mode=ro", uri=True, timeout=2)
    uid = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
    if not uid or uid[0] != expected_uid:
        connection.close()
        raise RuntimeError("Analytics UID is not the expected dedicated test account")
    baseline_rows = connection.execute("SELECT id,event FROM events").fetchall()
    baseline = max((row[0] for row in baseline_rows), default=0)
    baseline_keys = {event_key(payload) for _, payload in baseline_rows}
    armed_at = int(time.time() * 1000)
    evidence = {"event": event, "armedAt": armed_at, "baselineId": baseline, "observed": False,
                "baselineQueueSize": len(baseline_rows),
                "pollCount": 0, "maxNewRowIdSeen": None, "queueDrained": False}
    if expected_program_id is not None:
        evidence["expectedEventProperties"] = {"program_id": expected_program_id}
        evidence["programIdMismatchCount"] = 0
    mismatched_keys = set()
    config_db = Path(database).parent / "Application Support/Google/RemoteConfig/RemoteConfig.sqlite3"
    if config_db.exists():
        config = sqlite3.connect(config_db.as_uri() + "?mode=ro", uri=True)
        try:
            row = config.execute("SELECT value FROM main_active WHERE key='abtest_complete_to_unlock_popup'").fetchone()
            if row:
                flag = json.loads(row[0])
                evidence["popupConfig"] = {key: flag.get(key) for key in ("isEnabled", "metricsId")}
        finally:
            config.close()
    print("READY", flush=True)
    deadline = time.monotonic() + timeout
    try:
        while time.monotonic() < deadline:
            evidence["pollCount"] += 1
            rows = connection.execute("SELECT id,event FROM events ORDER BY id").fetchall()
            if not rows:
                evidence["queueDrained"] = True
            for row_id, payload in rows:
                fingerprint = event_key(payload)
                if fingerprint in baseline_keys:
                    continue
                evidence["maxNewRowIdSeen"] = max(evidence["maxNewRowIdSeen"] or 0, row_id)
                item = json.loads(payload)
                if (event.startswith("ToolTipPostponeActivity")
                        and item.get("event_type", "").startswith("ToolTipPostponeActivity")
                        and item.get("user_id") == expected_uid and item.get("timestamp", 0) >= armed_at):
                    properties = item.get("event_properties", {})
                    candidate = {"eventType": item["event_type"], "eventId": row_id,
                                 "propertyKeys": list(properties),
                                 "markers": {k: v for k, v in properties.items() if v in ("shown", "opened")}}
                    evidence.setdefault("candidates", {})[str(row_id)] = candidate
                if (item.get("event_type") == event and item.get("user_id") == expected_uid
                        and item.get("timestamp", 0) >= armed_at):
                    if expected_program_id is not None:
                        properties = item.get("event_properties")
                        if (not isinstance(properties, dict)
                                or properties.get("program_id") != expected_program_id):
                            # Count distinct candidates without storing wrong values or payloads.
                            mismatched_keys.add(fingerprint)
                            evidence["programIdMismatchCount"] = len(mismatched_keys)
                            continue
                        evidence["eventProperties"] = {"program_id": properties["program_id"]}
                    evidence.update(observed=True, eventId=row_id, timestamp=item["timestamp"],
                                    expectedUserMatched=True, freshEventFingerprint=fingerprint,
                                    rowIdRestarted=row_id <= baseline)
                    return True
            # The SDK removes uploaded rows. A 100 ms sample can miss an event
            # whose batch upload completes between samples; keep this read-only
            # observation at 10 ms. This is not a retry of the UI action.
            time.sleep(0.01)
        return False
    finally:
        connection.close()
        Path(output).parent.mkdir(parents=True, exist_ok=True)
        Path(output).write_text(json.dumps(evidence, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--event", choices=["ToolTipPostponeActivity shown", "ProgramModalOpen", "ProgramModalSelect"], required=True)
    parser.add_argument("--expected-program-id", help="Exact event_properties.program_id for ProgramModalSelect")
    args = parser.parse_args()
    uid = os.environ.get("COACH_EXPECTED_ANALYTICS_UID")
    if not uid:
        raise RuntimeError("Expected test UID is required")
    return 0 if observe(args.database, uid, args.event, args.output,
                        expected_program_id=args.expected_program_id) else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (RuntimeError, sqlite3.Error) as error:
        print(str(error), file=sys.stderr)
        sys.exit(2)
