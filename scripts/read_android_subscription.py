#!/usr/bin/env python3
"""Read current billing entitlement without treating a visible Today tab as Premium."""
import argparse
import json
import urllib.request
from read_android_module_content import session, HOST, NoRedirect


def read(serial, expected_uid=None):
    uid, token = session(serial, allow_anonymous=True)
    if expected_uid is not None and uid != expected_uid:
        raise RuntimeError('Billing identity changed')
    request = urllib.request.Request(HOST + '/api/v1/user/', headers={
        'Authorization': 'FirebaseToken ' + token, 'AppVersion': '1.40.21',
        'BuildVersion': '339', 'X-Gender': 'male', 'Language': 'en', 'Cache-Control': 'no-cache'})
    with urllib.request.build_opener(NoRedirect()).open(request, timeout=12) as response:
        user = json.load(response)['userdata']
    active = user.get('subscription', {}).get('active')
    if not isinstance(active, bool):
        raise RuntimeError('Subscription state is missing')
    return {'uid': uid, 'active': active}


if __name__ == '__main__':
    p = argparse.ArgumentParser();p.add_argument('--serial', required=True);p.add_argument('--uid')
    args = p.parse_args()
    try: print(json.dumps(read(args.serial, args.uid)))
    except Exception: raise SystemExit('Android billing entitlement could not be verified')
