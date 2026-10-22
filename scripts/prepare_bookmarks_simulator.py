#!/usr/bin/env python3
"""Reinstall one Bookmarks app without rebooting its dedicated iOS simulator."""
import argparse
import json
from pathlib import Path
import plistlib
import subprocess


def reinstall_app(udid, bundle_id, app):
    app = Path(app).resolve()
    if app.suffix != '.app':
        raise ValueError('Supply the simulator .app bundle')
    with (app / 'Info.plist').open('rb') as source:
        metadata = plistlib.load(source)
    if metadata.get('CFBundleIdentifier') != bundle_id:
        raise ValueError('Requested bundle ID does not match Info.plist')
    result = subprocess.run(['xcrun', 'simctl', 'list', 'devices', '--json'],
                            check=True, capture_output=True, text=True)
    devices = [device for group in json.loads(result.stdout)['devices'].values() for device in group
               if device.get('udid') == udid and device.get('isAvailable')]
    if len(devices) != 1 or devices[0].get('state') != 'Booted':
        raise ValueError('The explicitly selected dedicated simulator must already be booted')
    installed = subprocess.run(['xcrun', 'simctl', 'get_app_container', udid, bundle_id, 'app'],
                               capture_output=True, text=True)
    if installed.returncode:
        # Verify the simulator still responds, rather than treating a disconnected
        # service as an absent application and proceeding with installation.
        apps = subprocess.run(['xcrun', 'simctl', 'listapps', udid],
                              check=True, capture_output=True)
        converted = subprocess.run(['plutil', '-convert', 'json', '-o', '-', '--', '-'],
                                   input=apps.stdout, check=True, capture_output=True)
        if bundle_id in json.loads(converted.stdout):
            raise RuntimeError('Installed application container could not be resolved')
    else:
        subprocess.run(['xcrun', 'simctl', 'terminate', udid, bundle_id],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['xcrun', 'simctl', 'uninstall', udid, bundle_id], check=True)
    subprocess.run(['xcrun', 'simctl', 'install', udid, str(app)], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--udid', required=True, help='Dedicated, already booted simulator')
    parser.add_argument('--bundle-id', required=True)
    parser.add_argument('--app', type=Path, required=True)
    args = parser.parse_args()
    reinstall_app(args.udid, args.bundle_id, args.app)
    print('Application reinstalled; simulator was not restarted. Use ordinary email/OTP login.')


if __name__ == '__main__':
    main()
