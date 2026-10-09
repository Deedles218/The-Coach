#!/usr/bin/env python3
"""Read COA-7915 account state; never emit tokens, email or complete API payloads."""
import argparse
import base64
from contextlib import closing
import json
import math
from pathlib import Path
import plistlib
import re
import sqlite3
import subprocess
import time
import urllib.parse
import urllib.request

from read_android_module_content import adb, session, NoRedirect, HOST as ANDROID_HOST

PREPROD_HOST = "https://coach-preprod-cf87bfd42b85.herokuapp.com"
IOS_HOSTS = {"com.vamapps.preprod.The-Coach": PREPROD_HOST,
             "com.vamapps.The-Coach": ANDROID_HOST}


def fetch_user(host, headers):
    # Exactly one read-only endpoint. Never call daily_program, which can select a day.
    request = urllib.request.Request(host + "/api/v1/user/", headers=headers)
    with urllib.request.build_opener(NoRedirect()).open(request, timeout=12) as response:
        user = json.load(response)["userdata"]
    active = user.get("subscription", {}).get("active")
    if type(active) is not bool:
        raise RuntimeError("Subscription state is missing or not boolean")
    return active


def read_android(serial, expected_uid):
    uid, token = session(serial)  # Update fixture must be an authenticated, non-anonymous account.
    if uid != expected_uid:
        raise RuntimeError("Update account identity differs from fixture")
    package = adb(serial, "shell", "dumpsys", "package", "com.vamapps.thecoach").decode()
    version = re.search(r"versionName=([^\s]+)", package)
    build = re.search(r"versionCode=(\d+)", package)
    if not version or not build:
        raise RuntimeError("Installed Android build metadata is unavailable")
    active = fetch_user(ANDROID_HOST, {"Authorization": "FirebaseToken " + token,
                        "AppVersion": version[1], "BuildVersion": build[1],
                        "X-Gender": "male", "Language": "en", "Cache-Control": "no-cache"})
    return {"uid": uid, "subscriptionActive": active}


def verified_headers(blob, host, uid, now):
    request = plistlib.loads(blob)
    if request.get("Version") != 9 or not isinstance(request.get("Array"), list):
        raise RuntimeError("Unsupported iOS request metadata")
    items = request["Array"]
    urls = [item["_CFURLString"] for item in items if isinstance(item, dict) and "_CFURLString" in item]
    if len(urls) != 1:
        raise RuntimeError("Ambiguous cached request URL")
    parsed = urllib.parse.urlsplit(urls[0])
    if (parsed.scheme + "://" + parsed.netloc != host or parsed.fragment
            or parsed.path not in ("/api/v1/user/", "/api/v1/daily_program/v2/")):
        raise RuntimeError("Cached request has an unexpected endpoint")
    sources = [item for item in items if isinstance(item, dict) and "Authorization" in item]
    if len(sources) != 1:
        raise RuntimeError("Cached authorization is ambiguous")
    authorization = sources[0]["Authorization"]
    if not isinstance(authorization, str) or "\r" in authorization or "\n" in authorization:
        raise RuntimeError("Invalid cached authorization")
    pieces = authorization.split(" ")
    if len(pieces) != 2 or pieces[0].lower() not in ("firebasetoken", "bearer"):
        raise RuntimeError("Invalid cached authorization format")
    parts = pieces[1].split(".")
    if len(parts) != 3:
        raise RuntimeError("Invalid cached token")
    claims = json.loads(base64.urlsafe_b64decode(parts[1] + "=" * (-len(parts[1]) % 4)))
    expiry = claims.get("exp")
    if (claims.get("user_id") != uid or claims.get("sub") != uid
            or not isinstance(expiry, (int, float)) or isinstance(expiry, bool)
            or not math.isfinite(expiry) or expiry <= now):
        raise RuntimeError("Cached session is expired or belongs to another account")
    return {"Authorization": authorization}


def cached_headers(database, host, uid):
    database = Path(database).resolve()
    if not database.is_file():
        raise RuntimeError("iOS authenticated URL cache is missing")
    with closing(sqlite3.connect(database.as_uri() + "?mode=ro", uri=True, timeout=2)) as connection:
        connection.execute("PRAGMA query_only=ON")
        rows = connection.execute(
            "SELECT b.request_object FROM cfurl_cache_blob_data b JOIN cfurl_cache_response r "
            "ON b.entry_ID=r.entry_ID WHERE r.request_key LIKE ? ORDER BY r.entry_ID DESC",
            (host + "/api/v1/%",)).fetchall()
    for (blob,) in rows:
        try:
            return verified_headers(blob, host, uid, time.time())
        except (ValueError, TypeError, KeyError, RuntimeError, plistlib.InvalidFileException):
            continue
    raise RuntimeError("No valid current-account iOS session in URL cache; log in first")


def sim_container(device, bundle, kind):
    return Path(subprocess.check_output(["xcrun", "simctl", "get_app_container", device, bundle, kind],
                                        text=True, timeout=10).strip())


def read_ios(device, bundle, expected_uid):
    if bundle not in IOS_HOSTS:
        raise RuntimeError("Unsupported update bundle ID")
    data = sim_container(device, bundle, "data")
    identity = data / "Library/com.amplitude.database"
    with closing(sqlite3.connect(identity.resolve().as_uri() + "?mode=ro", uri=True, timeout=2)) as connection:
        current = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
    if current != (expected_uid,):
        raise RuntimeError("Current iOS account differs from fixture")
    app = sim_container(device, bundle, "app")
    with (app / "Info.plist").open("rb") as source:
        info = plistlib.load(source)
    if info.get("CFBundleIdentifier") != bundle:
        raise RuntimeError("Installed iOS bundle differs from configuration")
    headers = cached_headers(data / "Library/Caches" / bundle / "Cache.db", IOS_HOSTS[bundle], expected_uid)
    headers.update({"AppVersion": info["CFBundleShortVersionString"], "BuildVersion": info["CFBundleVersion"],
                    "X-Gender": "male", "Language": "en", "Cache-Control": "no-cache"})
    return {"uid": expected_uid, "subscriptionActive": fetch_user(IOS_HOSTS[bundle], headers)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--platform", choices=("android", "ios"), required=True)
    parser.add_argument("--device", required=True)
    parser.add_argument("--uid", required=True)
    parser.add_argument("--bundle")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", args.uid):
        raise RuntimeError("An approved fixture UID is required")
    if args.platform == "android":
        if not re.fullmatch(r"emulator-[0-9]+", args.device):
            raise RuntimeError("An explicit test emulator is required")
        result = read_android(args.device, args.uid)
    else:
        if not re.fullmatch(r"[A-Fa-f0-9-]{36}", args.device):
            raise RuntimeError("An explicit iOS Simulator is required")
        result = read_ios(args.device, args.bundle, args.uid)
    print(json.dumps(result))


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Even parser, subprocess and HTTP exceptions must not print a token or API payload.
        raise SystemExit("Update account evidence unavailable: check fixture identity, login, build metadata and /user/ access")
