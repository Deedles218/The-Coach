#!/usr/bin/env python3
"""Observe fresh UID-bound Amplitude events in the selected Android app's disk queue."""
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


def decode_batch(raw):
    def invalid_constant(value):
        # Python accepts NaN/Infinity by default, but neither is valid SDK JSON.
        raise json.JSONDecodeError('Invalid JSON constant', '', 0)
    return json.loads(raw, parse_constant=invalid_constant)


def events(serial, diagnostics=None):
    stats=diagnostics if diagnostics is not None else {}
    stats['queuePolls']=stats.get('queuePolls',0)+1
    package = os.environ.get('COACH_ANDROID_EVIDENCE_PACKAGE')
    if package and package not in ('com.vamapps.thecoach', 'com.vamapps.thecoach.forher'):
        raise RuntimeError('Unsupported debug analytics package')
    # observe() verifies the same SDK UID and exact debug APK before arming.
    # run-as reads only this debuggable app; the rooted-emulator path is retained.
    prefix = ['exec-out', 'run-as', package] if package else ['exec-out']
    folder = 'app_amplitude-disk-queue/' if package else QUEUE
    result = []
    for name in adb(serial, *prefix, 'ls', folder).decode().splitlines():
        if not re.fullmatch(r'[a-zA-Z0-9_.-]+', name):
            continue
        try:
            raw=adb(serial, *prefix, 'cat', folder + name)
            stats['fileReads']=stats.get('fileReads',0)+1
            try:
                data = decode_batch(raw)
            except json.JSONDecodeError:
                # The SDK keeps its active .tmp batch as an open JSON array.
                # Close only the container: an incomplete event or malformed
                # payload still fails JSON decoding and is read on a later poll.
                if not name.endswith('.tmp') or not raw.lstrip().startswith(b'['):
                    raise
                data = decode_batch(raw+b']')
                stats['openBatchSnapshots']=stats.get('openBatchSnapshots',0)+1
            if isinstance(data, list): result.extend(x for x in data if isinstance(x, dict))
            else:stats['nonArrayPayloads']=stats.get('nonArrayPayloads',0)+1
        except (json.JSONDecodeError, UnicodeDecodeError):
            # The SDK writes .tmp files incrementally; the next poll reads the complete file.
            stats['unparsedFiles']=stats.get('unparsedFiles',0)+1
            try:
                lines=[x for x in raw.splitlines() if x.strip()]
                if len(lines)>1 and all(isinstance(json.loads(x),dict) for x in lines):
                    stats['jsonLinesPayloads']=stats.get('jsonLinesPayloads',0)+1
            except (json.JSONDecodeError, UnicodeDecodeError):pass
            continue
        except RuntimeError:
            # Files may disappear as the SDK uploads the queue.
            stats['unavailableFiles']=stats.get('unavailableFiles',0)+1
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
    diagnostics={}
    evidence = dict(event=event_name, armedAt=boundary, observed=False, platform='Android',sourceDiagnostics=diagnostics)
    output.parent.mkdir(parents=True,exist_ok=True)
    print('READY',flush=True)
    deadline = time.monotonic()+timeout
    while time.monotonic()<deadline:
        for event in events(serial, diagnostics):
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
