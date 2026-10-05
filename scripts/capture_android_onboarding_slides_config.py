#!/usr/bin/env python3
"""Export slide expectations from Android's active Firebase Remote Config JSON cache."""
import argparse
import json
from pathlib import Path
import subprocess
from capture_onboarding_slides_config import export_properties


def capture(package, goal, output, config_file=None, serial=None):
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    if config_file:
        document = json.loads(Path(config_file).read_text(encoding='utf-8'))
    else:
        # run-as intentionally fails for a non-debuggable release build. Do not
        # fall back to APK defaults, fetched-but-not-activated config, or iOS values.
        files = subprocess.check_output(
            ['adb', '-s', serial, 'shell', 'run-as', package, 'ls', 'files'],
            text=True, timeout=15).splitlines()
        active = [name for name in files if name.startswith('frc_') and name.endswith('_firebase_activate.json')]
        if len(active) != 1:
            raise ValueError('Expected one active Firebase config cache; found ' + str(len(active)))
        document = json.loads(subprocess.check_output(
            ['adb', '-s', serial, 'shell', 'run-as', package, 'cat', 'files/' + active[0]],
            text=True, timeout=15))
    values = document.get('configs', document)
    # Cache values are usually JSON strings; accept exported parsed objects too.
    values = {key: value if isinstance(value, str) else json.dumps(value)
              for key, value in values.items()}
    return export_properties(values, package, goal, output, platform='android')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--serial', help='Debuggable test build with readable active cache')
    source.add_argument('--config', type=Path, help='Export of this Android build\'s active config cache')
    parser.add_argument('--package', required=True)
    parser.add_argument('--goal', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    capture(args.package, args.goal, args.output, args.config, args.serial)
