#!/usr/bin/env python3
"""Read the current production For Her identity/membership; never grant access or export auth."""
import argparse
from contextlib import closing
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile
import urllib.request

from read_update_account import cached_headers, ANDROID_HOST, NoRedirect

BUNDLE = 'com.vamapps.The-Coach-for-her'


def read_recipient(device):
    if not re.fullmatch(r'[A-Fa-f0-9]{8}-[A-Fa-f0-9]{16}', device):
        raise ValueError('An explicit physical iPhone UDID is required')
    with tempfile.TemporaryDirectory(prefix='partner-recipient-') as temporary:
        directory = Path(temporary)

        def copy(source, destination, required=True):
            result = subprocess.run([
                'xcrun', 'devicectl', 'device', 'copy', 'from', '--device', device,
                '--domain-type', 'appDataContainer', '--domain-identifier', BUNDLE,
                '--source', source, '--destination', str(destination), '--quiet'],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=15)
            if required and result.returncode:
                raise RuntimeError('Current recipient evidence is unavailable')

        copy('Library/com.amplitude.database', directory / 'identity.sqlite')
        with closing(sqlite3.connect((directory / 'identity.sqlite').as_uri() + '?mode=ro', uri=True)) as connection:
            identity = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        if not identity or not isinstance(identity[0], str):
            raise RuntimeError('Current recipient identity is missing')
        uid = identity[0]
        for suffix in ('', '-wal', '-shm'):
            copy('Library/Caches/' + BUNDLE + '/Cache.db' + suffix,
                 directory / ('Cache.db' + suffix), required=not suffix)
        headers = cached_headers(directory / 'Cache.db', ANDROID_HOST, uid)
        headers.update({'AppVersion': '2.1.0', 'BuildVersion': '3583', 'X-Gender': 'female',
                        'Language': 'en', 'Cache-Control': 'no-cache'})
        request = urllib.request.Request(ANDROID_HOST + '/api/v1/user/', headers=headers)
        with urllib.request.build_opener(NoRedirect()).open(request, timeout=12) as response:
            user = json.load(response)['userdata']
        active = user.get('subscription', {}).get('active')
        if type(active) is not bool:
            raise RuntimeError('Current recipient membership is unknown')
        return {'identityHash': hashlib.sha256(uid.encode()).hexdigest(),
                'identityVerified': True, 'subscriptionActive': active}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--udid', required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(read_recipient(args.udid)))
    except Exception:
        raise SystemExit('Current recipient evidence unavailable: check device, identity, session and /user/ access')


if __name__ == '__main__':
    main()
