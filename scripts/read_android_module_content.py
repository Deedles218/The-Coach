"""Read Android module evidence from the explicit local emulator, never log tokens."""
import argparse
import base64
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import urllib.request
from pathlib import Path
from read_simulator_program_content import _oracle

HOST = "https://content.the.coach"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise EvidenceError("Evidence request redirected; refused")


class EvidenceError(RuntimeError):
    pass


def adb(serial, *args):
    if not re.fullmatch(r"[A-Za-z0-9_.:-]+", serial):
        raise EvidenceError("Invalid Android serial")
    result = subprocess.run([os.environ.get("ADB", "adb"), "-s", serial, *args],
                            capture_output=True, timeout=15)
    if result.returncode:
        raise EvidenceError("Cannot read Android app evidence; use a rooted test emulator")
    return result.stdout


def session(serial, allow_anonymous=False):
    base = "/data/data/com.vamapps.thecoach/shared_prefs/"
    files = adb(serial, "shell", "ls", base).decode().splitlines()
    names = [f for f in files if f.startswith("com.google.firebase.auth.api.Store.") and f.endswith(".xml")]
    if len(names) != 1:
        raise EvidenceError("Expected one Android Firebase account store")
    root = ET.fromstring(adb(serial, "exec-out", "cat", base + names[0]))
    users = [json.loads(x.text) for x in root if x.get("name") == "com.google.firebase.auth.FIREBASE_USER"]
    if len(users) != 1 or (users[0].get("anonymous") is not False and not (allow_anonymous and users[0].get("anonymous") is True)):
        raise EvidenceError("Android session is missing or anonymous")
    user = users[0]
    token = json.loads(user["cachedTokenState"])["access_token"]
    payload = token.split(".")[1]
    claims = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
    uid = claims.get("user_id")
    identities = [json.loads(x) if isinstance(x, str) else x for x in user["userInfos"]]
    if (not isinstance(uid, str) or not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", uid)
            or claims.get("sub") != uid or claims.get("exp", 0) <= time.time()
            or not any(x.get("userId") == uid for x in identities)):
        raise EvidenceError("Android session UID/token is inconsistent or expired")
    return uid, token


def read_content(serial, program, day, boundary):
    # Catalog reads are evidence AFTER the UI has reached this day. Do not use
    # daily_program's selecting endpoint to prepare or drive the UI transition.
    if program != "last_longer" or not 1 <= day <= 7:
        raise EvidenceError("Android evidence is scoped to the approved first Last Longer module")
    uid, token = session(serial)
    if uid != os.environ.get("COACH_EXPECTED_PROGRAM_UID"):
        raise EvidenceError("Wrong Android module account")
    package = adb(serial, "shell", "dumpsys", "package", "com.vamapps.thecoach").decode()
    version = re.search(r"versionName=([^\s]+)", package)
    build = re.search(r"versionCode=(\d+)", package)
    if not version or not build:
        raise EvidenceError("Cannot verify installed Android version")
    headers = {"Authorization": "FirebaseToken " + token, "AppVersion": version[1],
               "BuildVersion": build[1], "X-Gender": "male", "Language": "en",
               "Timezone": "Europe/Samara", "Cache-Control": "no-cache"}
    opener = urllib.request.build_opener(NoRedirect())
    def get(route):
        with opener.open(urllib.request.Request(HOST + route, headers=headers), timeout=12) as response:
            return json.load(response)
    user = get("/api/v1/user/")["userdata"]
    expected_email = os.environ.get("COACH_COA9044_EMAIL", "").lower()
    email = user.get("analytics_profile", {}).get("email", "")
    if not expected_email or not isinstance(email, str) or email.lower() != expected_email:
        raise EvidenceError("Authenticated Android API account does not match the approved email")
    if user.get("subscription", {}).get("active") is not True:
        raise EvidenceError("Android module account requires active Premium")
    payload = get("/api/v1/coachprogram/catalog/v3/" + program + "/?day_of_program=" + str(day))
    result = validate_catalog(payload, program, day)
    now = int(time.time() * 1000)
    if boundary > now or now - boundary > 120000:
        raise EvidenceError("Android evidence is not associated with a recent UI action")
    result.update(source="authenticated Android catalog read after UI transition", uid=uid,
                  fetchedAt=now, actionBoundary=boundary, subscriptionActive=True)
    return result


def validate_catalog(payload, program, day):
    result = _oracle(payload, program, require_incomplete_module=True)
    section = payload["program_section"]
    if (section["day_in_program"] != day or section["module_current_day"] != day
            or section["module_order"] != 1):
        raise EvidenceError("Catalog does not describe the UI-selected first-module stage")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--identity", action="store_true")
    parser.add_argument("--program-id")
    parser.add_argument("--day", type=int)
    parser.add_argument("--not-before-ms", type=int)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    uid, _ = session(args.serial)
    expected = os.environ.get("COACH_EXPECTED_PROGRAM_UID")
    if expected and uid != expected:
        raise EvidenceError("Android account does not match the approved module UID")
    if args.identity:
        print(uid)
    else:
        if not args.program_id or not args.day or not args.not_before_ms or not args.output:
            raise EvidenceError("Content arguments are required")
        result = read_content(args.serial, args.program_id, args.day, args.not_before_ms)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except EvidenceError as error:
        raise SystemExit(str(error))
    except Exception:
        # Parser/auth failures must not print the source, token or account data.
        raise SystemExit("Android module evidence unavailable or identity verification failed")
