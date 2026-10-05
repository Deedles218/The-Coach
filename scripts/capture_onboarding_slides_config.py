#!/usr/bin/env python3
"""Read the installed iOS app's active Remote Config; never change Firebase."""
import argparse
import json
from pathlib import Path
import sqlite3
import subprocess
import urllib.request
from urllib.parse import urlparse


def gif_frame_count(data):
    """Count GIF image descriptors without adding an imaging dependency."""
    if data[:6] not in (b'GIF87a', b'GIF89a'):
        raise ValueError('Configured GIF URL did not return a GIF')
    offset = 13
    if data[10] & 128:
        offset += 3 * (2 ** ((data[10] & 7) + 1))
    frames = 0
    while offset < len(data):
        marker = data[offset]
        offset += 1
        if marker == 0x3B:
            return frames
        if marker == 0x2C:
            frames += 1
            packed = data[offset + 8]
            offset += 9
            if packed & 128:
                offset += 3 * (2 ** ((packed & 7) + 1))
            offset += 1  # LZW minimum code size
        elif marker == 0x21:
            offset += 1  # Extension label
        else:
            raise ValueError('Unexpected GIF block marker')
        while data[offset]:
            offset += data[offset] + 1
        offset += 1
    raise ValueError('Truncated GIF')


def capture(device, bundle, goal, output):
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)  # A failed capture must never leave stale expectations.
    database = output.with_suffix('.sqlite3')
    subprocess.run([
        'xcrun', 'devicectl', 'device', 'copy', 'from', '--device', device,
        '--domain-type', 'appDataContainer', '--domain-identifier', bundle,
        '--source', 'Library/Application Support/Google/RemoteConfig/RemoteConfig.sqlite3',
        '--destination', str(database), '--quiet',
    ], check=True)
    config_key = ('daily_plan_onboarding_slides_for_her'
                  if bundle.endswith('-for-her') else 'daily_plan_onboarding_slides')
    with sqlite3.connect(database.resolve().as_uri() + '?mode=ro', uri=True) as connection:
        values = dict(connection.execute(
            'SELECT key, value FROM main_active WHERE key IN (?, ?)',
            (config_key, 'abtest_onboarding_slides')))
    return export_properties(values, bundle, goal, output)


def export_properties(values, bundle, goal, output, platform='ios'):
    """Convert the app's active configuration, never use another platform's fixture."""
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    config_key = ('daily_plan_onboarding_slides_for_her'
                  if bundle.endswith('-for-her') or bundle.endswith('.forher') else 'daily_plan_onboarding_slides')
    errors = []
    if not json.loads(values['abtest_onboarding_slides']).get('isEnabled'):
        errors.append('Active abtest_onboarding_slides is disabled')
    slides = [slide for slide in json.loads(values[config_key])['onboarding_slides']
              if not slide.get('userGoal') or slide['userGoal'].casefold() == goal.casefold()]
    slides.sort(key=lambda slide: slide['order'])
    if len(slides) < 2 or [slide['order'] for slide in slides] != list(range(1, len(slides) + 1)):
        errors.append('No complete ordered slide sequence for selected goal: ' + goal
                      + '; matching orders: ' + str([slide['order'] for slide in slides]))
    if len({slide['header'] for slide in slides}) != len(slides):
        errors.append('Slide headers must be unambiguous accessibility labels')
    properties = {('appPackage' if platform == 'android' else 'bundleId'): bundle, 'goal': goal, 'configKey': config_key, 'count': len(slides)}
    for index, slide in enumerate(slides, 1):
        for key in ('id', 'header', 'buttonText', 'imageUrl'):
            if not slide.get(key):
                errors.append('Missing ' + key + ' for slide ' + str(index))
            properties[str(index) + '.' + key] = slide.get(key, '')
        if not errors and urlparse(slide['imageUrl']).path.lower().endswith('.gif'):
            with urllib.request.urlopen(slide['imageUrl'], timeout=30) as response:
                properties[str(index) + '.gifFrameCount'] = gif_frame_count(response.read())
    if errors:
        properties['validationError'] = '; '.join(errors)
    def escape(value):
        return str(value).replace('\\', '\\\\').replace('\n', '\\n').replace('\r', '\\r')
    output.write_text('\n'.join(key + '=' + escape(value) for key, value in properties.items()) + '\n',
                      encoding='utf-8')
    if errors:
        # Replace any older fixture with a diagnostic that the Java test must reject.
        raise ValueError(properties['validationError'])
    print(f'{config_key}: {len(slides)} slides for {goal}; fixture: {output}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', required=True)
    parser.add_argument('--bundle', required=True)
    parser.add_argument('--goal', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    capture(args.device, args.bundle, args.goal, args.output)
