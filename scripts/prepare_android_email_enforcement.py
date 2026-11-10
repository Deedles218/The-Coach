#!/usr/bin/env python3
"""Seed/restore only is_new_user on a fresh, dedicated COA-9222 debug installation.

No counters, Firebase identity, Remote Config, backend state or subscription are edited.
This is an existing-installation fixture, not an update/migration test.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from read_android_email_enforcement import PACKAGES, read
from read_android_module_content import adb

PREF = 'shared_prefs/app_installation_prefs.xml'


def change(device, package, action, evidence_path, expected_uid=None):
    state = read(device, package, 'account', expected_uid)
    tree = ET.fromstring(adb(device, 'exec-out', 'run-as', package, 'cat', PREF))
    markers = [e for e in tree if e.tag == 'boolean' and e.get('name') == 'is_new_user']
    if len(markers) != 1:
        raise ValueError('Expected one existing new-user marker')
    marker = markers[0]
    if action == 'seed-existing':
        if state['emailPresent'] or state['emailVerified'] or marker.get('value') != 'true':
            raise ValueError('Only a fresh unlinked test installation may be seeded')
        if any(state['progress'].values()):
            raise ValueError('Do not seed a progressed account')
        if evidence_path.exists():
            raise ValueError('Do not overwrite fixture seed evidence')
        evidence_path.write_text(json.dumps({'originalMarker': marker.get('value'), 'baseline': state}, indent=2))
        marker.set('value', 'false')
    else:
        evidence = json.loads(evidence_path.read_text())
        if evidence['baseline']['identity'] != state['identity']:
            raise ValueError('Do not restore a different account')
        marker.set('value', evidence['originalMarker'])
    # The caller has stopped this task's app. Send XML through stdin, never shell interpolation.
    subprocess.run([os.environ.get('ADB', 'adb'), '-s', device, 'exec-in', 'run-as', package,
                    'sh', '-c', 'cat > ' + PREF], input=ET.tostring(tree, encoding='utf-8', xml_declaration=True),
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=15)
    actual = ET.fromstring(adb(device, 'exec-out', 'run-as', package, 'cat', PREF))
    assert next(e for e in actual if e.get('name') == 'is_new_user').get('value') == marker.get('value')
    return {'action': action, 'marker': marker.get('value'), 'identity': state['identity']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--package', choices=tuple(PACKAGES), required=True)
    parser.add_argument('--action', choices=('seed-existing', 'restore'), required=True)
    parser.add_argument('--evidence', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(change(args.device, args.package, args.action, args.evidence,
                            os.environ.get('COACH_EMAIL_ENFORCEMENT_UID'))))


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Fixture preparation failed; inspect dedicated installation and redacted baseline')
