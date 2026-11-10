#!/usr/bin/env python3
"""Read-only, temporary app-container snapshots from a paired physical iPhone.

Raw databases never become report attachments. Callers retain only allowlisted
assertion evidence; TemporaryDirectory owns and removes the copied databases.
"""
import argparse
from contextlib import contextmanager, closing
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile

ALLOWED_BUNDLES = {
    "com.vamapps.The-Coach", "com.vamapps.The-Coach-for-her",
    "com.vamapps.preprod.The-Coach",
}


def validate_device(udid, bundle):
    if not isinstance(udid, str) or not re.fullmatch(r"[A-Fa-f0-9]{8}-[A-Fa-f0-9]{16}", udid):
        raise ValueError("An explicit physical iPhone UDID is required")
    if bundle not in ALLOWED_BUNDLES:
        raise ValueError("Unsupported Coach bundle for read-only evidence")


def copy_file(root, relative, udid, bundle, required=True):
    validate_device(udid, bundle)
    relative = Path(relative)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError("Evidence path must stay inside the app container")
    destination = Path(root)/relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    result = subprocess.run([
        "xcrun", "devicectl", "device", "copy", "from", "--device", udid,
        "--domain-type", "appDataContainer", "--domain-identifier", bundle,
        "--source", str(relative), "--destination", str(destination), "--timeout", "7",
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=10)
    if result.returncode:
        if required:
            raise RuntimeError("Cannot copy required read-only device evidence")
        return None
    return destination


@contextmanager
def amplitude_snapshot(udid, bundle):
    validate_device(udid, bundle)
    with tempfile.TemporaryDirectory(prefix="coach-device-evidence-") as directory:
        database = copy_file(directory, "Library/com.amplitude.database", udid, bundle)
        copy_file(directory, "Library/com.amplitude.database-wal", udid, bundle, required=False)
        yield database


@contextmanager
def url_cache_snapshot(udid, bundle):
    validate_device(udid, bundle)
    with tempfile.TemporaryDirectory(prefix="coach-device-cache-") as directory:
        relative = "Library/Caches/" + bundle + "/Cache.db"
        database = copy_file(directory, relative, udid, bundle)
        copy_file(directory, relative + "-wal", udid, bundle, required=False)
        yield database


def read_identity(udid, bundle):
    with amplitude_snapshot(udid, bundle) as database:
        with closing(sqlite3.connect(database.as_uri()+"?mode=ro", uri=True)) as connection:
            row = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
            if not row or not isinstance(row[0], str) or not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", row[0]):
                raise RuntimeError("Missing analytics UID")
            return row[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--udid", required=True)
    parser.add_argument("--bundle", required=True)
    args = parser.parse_args()
    # Consumed privately by the fixture helper, never an Allure attachment.
    print(read_identity(args.udid, args.bundle))


if __name__ == "__main__":
    main()
