#!/usr/bin/env python3
"""Observe fresh UID-bound Amplitude events in the selected test emulator's disk queue."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import time
from read_android_module_content import adb, session
from observe_simulator_event import validate_program_expectation

QUEUE = '/data/data/com.vamapps.thecoach/app_amplitude-disk-queue/'


def events(serial):
    result = []
    for name in adb(serial, 'exec-out', 'ls', QUEUE).decode().splitlines():
        if not re.fullmatch(r'[a-zA-Z0-9_.-]+', name):
            continue
        try:
            data = json.loads(adb(serial, 'exec-out', 'cat', QUEUE + name))
            if isinstance(data, list): result.extend(x for x in data if isinstance(x, dict))
        except json.JSONDecodeError:
            # The SDK writes .tmp files incrementally; the next poll reads the complete file.
            continue
        except RuntimeError:
            # Files may disappear as the SDK uploads the queue.
            continue
    return result


def fingerprint(event):
    return hashlib.sha256(str(event.get('insert_id') or json.dumps(event, sort_keys=True)).encode()).hexdigest()


def matching(event, uid, event_name, boundary, baseline, program=None):
    properties = event.get('event_properties')
    return (event.get('user_id') == uid and event.get('event_type') == event_name
            and isinstance(event.get('time'), (int,float)) and event['time'] >= boundary
            and fingerprint(event) not in baseline
            and (program is None or isinstance(properties, dict) and properties.get('program_id') == program))


def observe(serial, event_name, output, program=None, timeout=35):
    validate_program_expectation(event_name, program)
    uid = os.environ['COACH_EXPECTED_ANALYTICS_UID']
    if session(serial)[0] != uid: raise RuntimeError('Wrong Android analytics fixture UID')
    baseline = {fingerprint(e) for e in events(serial)}
    boundary = int(time.time()*1000)
    evidence = dict(event=event_name, armedAt=boundary, observed=False, platform='Android')
    output.parent.mkdir(parents=True,exist_ok=True)
    print('READY',flush=True)
    deadline = time.monotonic()+timeout
    while time.monotonic()<deadline:
        for event in events(serial):
            if matching(event,uid,event_name,boundary,baseline,program):
                evidence.update(observed=True, expectedUserMatched=True, timestamp=event['time'],
                                freshEventFingerprint=fingerprint(event))
                if program: evidence['eventProperties']={'program_id':program}
                output.write_text(json.dumps(evidence,indent=2));return
        time.sleep(.12)
    output.write_text(json.dumps(evidence,indent=2))
    raise RuntimeError('Fresh UID-bound Android analytics event not observed')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',required=True)
    parser.add_argument('--event',required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--expected-program-id')
    args=parser.parse_args()
    try: observe(args.serial,args.event,args.output,args.expected_program_id)
    except Exception: raise SystemExit('Android analytics observation failed; see sanitized evidence')
