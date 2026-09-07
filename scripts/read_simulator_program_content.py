#!/usr/bin/env python3
"""Read current-action Today content from the iOS simulator's URL cache.

No network requests or cache mutations. COACH_EXPECTED_PROGRAM_UID identifies
the dedicated, already verified account. The request JWT is checked for that
UID and expiry; this does not cryptographically verify its signature.

The observed CFURL request format is Version 9; the response format is Version 1:
Array[1] in the response is fractional
CFAbsoluteTime, Array[3] is HTTP status, Array[4] contains HTTP headers. We require
the response time to agree with HTTP Date and be newer than the UI-action
boundary. The SQLite row's time_stamp is NOT used: it can remain unchanged when
the app replaces the response for the same URL. Unknown metadata fails closed.
"""
import argparse
import base64
from contextlib import closing
import email.utils
import json
import math
import os
from pathlib import Path
import plistlib
import re
import sqlite3
import subprocess
import sys
import time
import urllib.parse


BUNDLE = "com.vamapps.preprod.The-Coach"
HOST = "coach-preprod-cf87bfd42b85.herokuapp.com"
ROUTE = "/api/v1/daily_program/v2/"
CF_EPOCH_SECONDS = 978307200
MAX_BODY_BYTES = 10 * 1024 * 1024


class EvidenceError(RuntimeError):
    """A safe, fixed diagnostic that never contains credentials or payloads."""


def _number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _route_matches(url):
    if not isinstance(url, str):
        return False
    parsed = urllib.parse.urlsplit(url)
    return (parsed.scheme == "https" and parsed.netloc == HOST
            and parsed.path == ROUTE and not parsed.fragment)


def _plist(blob, label):
    try:
        value = plistlib.loads(blob)
    except Exception:
        raise EvidenceError("Unreadable cached " + label + " metadata") from None
    expected_version = 9 if label == "request" else 1
    if (not isinstance(value, dict) or value.get("Version") != expected_version
            or not isinstance(value.get("Array"), list)):
        raise EvidenceError("Unsupported cached " + label + " metadata version")
    urls = [v.get("_CFURLString") for v in value["Array"]
            if isinstance(v, dict) and "_CFURLString" in v]
    if len(urls) != 1 or not _route_matches(urls[0]):
        raise EvidenceError("Cached " + label + " URL is not the preprod Today endpoint")
    return value["Array"]


def _verify_request(blob, expected_uid, now):
    items = _plist(blob, "request")
    headers = [v for v in items if isinstance(v, dict) and "Authorization" in v]
    if len(headers) != 1 or not isinstance(headers[0]["Authorization"], str):
        raise EvidenceError("Cached Today request has no unambiguous authorization")
    authorization = headers[0]["Authorization"]
    authorization_parts = authorization.split(" ")
    if (len(authorization_parts) != 2 or authorization_parts[0].lower() not in ("firebasetoken", "bearer")
            or "\r" in authorization or "\n" in authorization):
        raise EvidenceError("Cached Today authorization format is unsupported")
    try:
        parts = authorization_parts[1].split(".")
        if len(parts) != 3:
            raise ValueError()
        claims = json.loads(base64.urlsafe_b64decode(parts[1] + "=" * (-len(parts[1]) % 4)))
    except Exception:
        raise EvidenceError("Cached Today authorization is not a readable JWT") from None
    if not isinstance(claims, dict) or claims.get("user_id") != expected_uid:
        raise EvidenceError("Cached Today request UID does not match the dedicated account")
    if not _number(claims.get("exp")) or claims["exp"] <= now:
        raise EvidenceError("Cached Today request JWT has expired or has no valid expiry")


def _response_time(blob, now):
    items = _plist(blob, "response")
    if (len(items) < 5 or not _number(items[1]) or items[3] != 200
            or not isinstance(items[4], dict)):
        raise EvidenceError("Unsupported cached Today response time/status metadata")
    stamp = items[1] + CF_EPOCH_SECONDS
    date_values = [v for k, v in items[4].items() if k.lower() == "date"]
    if len(date_values) != 1 or not isinstance(date_values[0], str):
        raise EvidenceError("Cached Today response lacks an unambiguous HTTP Date")
    try:
        http_date = email.utils.parsedate_to_datetime(date_values[0])
        if http_date.tzinfo is None:
            raise ValueError()
        http_stamp = http_date.timestamp()
    except Exception:
        raise EvidenceError("Cached Today HTTP Date is invalid") from None
    if abs(stamp - http_stamp) > 2 or stamp > now + 2:
        raise EvidenceError("Cached Today response time cannot be corroborated by HTTP Date/local time")
    return stamp


def _body(cache_dir, stored, on_filesystem):
    if on_filesystem not in (0, 1):
        raise EvidenceError("Unsupported cached Today body storage")
    if on_filesystem:
        try:
            name = stored.decode("utf-8") if isinstance(stored, bytes) else stored
        except UnicodeError:
            raise EvidenceError("Invalid cached Today body filename") from None
        # The observed cache stores UUID basenames under fsCachedData. Do not
        # follow arbitrary paths from a corrupt database or escape its folder.
        if not isinstance(name, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", name):
            raise EvidenceError("Unsafe cached Today body filename")
        folder = cache_dir / "fsCachedData"
        if folder.is_symlink() or folder.resolve().parent != cache_dir.resolve():
            raise EvidenceError("Cached Today body directory is unsafe")
        folder = folder.resolve()
        path = folder / name
        if path.is_symlink() or not path.is_file() or path.resolve().parent != folder:
            raise EvidenceError("Cached Today body file is absent or unsafe")
        if path.stat().st_size > MAX_BODY_BYTES:
            raise EvidenceError("Cached Today body is too large")
        stored = path.read_bytes()
    if not isinstance(stored, bytes) or len(stored) > MAX_BODY_BYTES:
        raise EvidenceError("Unsupported cached Today body")
    try:
        return json.loads(stored)
    except Exception:
        raise EvidenceError("Cached Today body is not JSON") from None


def _oracle(payload, expected_program):
    if not isinstance(payload, dict):
        raise EvidenceError("Cached Today payload is not an object")
    section, cover = payload.get("program_section"), payload.get("program_cover")
    if (not isinstance(section, dict) or not isinstance(cover, dict)
            or cover.get("program_id") != expected_program
            or section.get("section_id") != expected_program):
        raise EvidenceError("Cached Today response does not belong to the expected program")
    day, total = section.get("day_in_program"), section.get("total_days")
    if (not isinstance(day, int) or isinstance(day, bool) or not isinstance(total, int)
            or isinstance(total, bool) or not 1 <= day <= total):
        raise EvidenceError("Cached Today response has invalid program day metadata")
    questions = section.get("questions")
    if not isinstance(questions, list) or not questions:
        raise EvidenceError("Cached Today response has no program questions")
    result, seen = [], set()
    for question in questions:
        if not isinstance(question, dict):
            raise EvidenceError("Cached Today question is malformed")
        identifier, headline = question.get("name"), question.get("headline")
        if (not isinstance(identifier, str) or not identifier.strip()
                or not isinstance(headline, str) or not headline.strip() or identifier in seen):
            raise EvidenceError("Cached Today question identity/headline is missing or duplicated")
        seen.add(identifier)
        result.append({"id": identifier, "headline": headline})
    headline = section.get("headline")
    if not isinstance(headline, str) or not headline.strip():
        raise EvidenceError("Cached Today program headline is absent")
    def allowed_fields(source, fields):
        values = {}
        for field in fields:
            value = source.get(field)
            if value is None:
                continue
            if not isinstance(value, (str, bool)) and not _number(value):
                raise EvidenceError("Cached Today display metadata is not scalar")
            if isinstance(value, str) and len(value) > 2048:
                raise EvidenceError("Cached Today display metadata is unexpectedly long")
            values[field] = value
        return values
    return {"programId": expected_program, "headline": headline,
            "day": day, "totalDays": total, "questions": result,
            "sectionMetadata": allowed_fields(section, (
                "module_name", "module_current_day", "module_total_days", "module_order",
                "module_program", "last_day_of_module", "module_completed", "first_module_in_program",
                "show_remaining_days", "last_available_day", "program_abtest_name")),
            "cover": allowed_fields(cover, (
                "program_id", "program_name", "program_text_1", "program_text_2", "program_text_3"))}


def read_content(database, expected_uid, expected_program, not_before_ms, now=None):
    """Return only allowed oracle fields, or fail without an old-cache fallback."""
    now = time.time() if now is None else now
    if not isinstance(expected_uid, str) or not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", expected_uid):
        raise EvidenceError("Expected dedicated account UID is required")
    if not isinstance(expected_program, str) or not re.fullmatch(r"[a-z0-9_]{1,80}", expected_program):
        raise EvidenceError("Expected program_id is required")
    if not _number(not_before_ms) or not 0 < not_before_ms <= now * 1000:
        raise EvidenceError("A valid current-action not-before timestamp is required")
    database = Path(database).resolve()
    if not database.is_file():
        raise EvidenceError("Simulator URL cache database is absent")
    with closing(sqlite3.connect(database.as_uri() + "?mode=ro", uri=True, timeout=2)) as connection:
        connection.execute("PRAGMA query_only=ON")
        rows = connection.execute(
            "SELECT r.request_key,b.request_object,b.response_object,d.receiver_data,d.isDataOnFS "
            "FROM cfurl_cache_response r JOIN cfurl_cache_blob_data b ON r.entry_ID=b.entry_ID "
            "JOIN cfurl_cache_receiver_data d ON r.entry_ID=d.entry_ID "
            "WHERE r.request_key LIKE ? ORDER BY r.entry_ID DESC",
            ("https://" + HOST + ROUTE + "%",)).fetchall()
    candidates = []
    failures = []
    for url, request, response, stored, on_filesystem in rows:
        if not _route_matches(url):
            continue
        try:
            stamp = _response_time(response, now)
            if stamp * 1000 < not_before_ms:
                raise EvidenceError("Cached Today response predates the current UI action")
            candidates.append((stamp, request, stored, on_filesystem))
        except EvidenceError as error:
            if "predates the current UI action" not in str(error):
                # Unknown response metadata cannot establish which body is
                # newest. Do not use an older parseable response as a fallback.
                raise
            failures.append(str(error))
    if not candidates:
        reason = "; ".join(dict.fromkeys(failures)) or "No cached Today response exists"
        raise EvidenceError("No verified current-action Today response: " + reason)
    # Only inspect the newest identity/freshness-verified response. An older
    # matching body must not hide a newer response selecting the wrong program.
    candidates.sort(key=lambda value: value[0], reverse=True)
    newest = candidates[0]
    if len(candidates) > 1 and candidates[1][0] == newest[0]:
        raise EvidenceError("Multiple cached Today responses have the same newest timestamp")
    _verify_request(newest[1], expected_uid, now)
    return _oracle(_body(database.parent, newest[2], newest[3]), expected_program)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--udid", required=True)
    parser.add_argument("--program-id", required=True)
    parser.add_argument("--not-before-ms", required=True, type=int)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Fa-f0-9-]{36}", args.udid):
        raise EvidenceError("An explicit iOS simulator UDID is required")
    completed = subprocess.run(["xcrun", "simctl", "get_app_container", args.udid, BUNDLE, "data"],
                               capture_output=True, text=True, check=False)
    if completed.returncode:
        raise EvidenceError("Cannot locate the preprod app's simulator data container")
    database = Path(completed.stdout.strip()) / "Library/Caches" / BUNDLE / "Cache.db"
    result = read_content(database, os.environ.get("COACH_EXPECTED_PROGRAM_UID"),
                          args.program_id, args.not_before_ms)
    output = Path(args.output).resolve()
    if output == database.resolve() or database.parent.resolve() in output.parents:
        raise EvidenceError("The evidence output must be outside the app URL cache")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print("Current-action Today content verified for " + args.program_id)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except EvidenceError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
    except Exception as error:
        # plist/sqlite/OS exceptions can contain filenames or payload fragments.
        print("Simulator content evidence failed (" + type(error).__name__ + ")", file=sys.stderr)
        sys.exit(2)
