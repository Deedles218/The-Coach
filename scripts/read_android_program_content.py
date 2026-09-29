#!/usr/bin/env python3
"""Read a UID-verified catalog for the Android selector's destination, without selecting it."""
import argparse
import json
import os
from pathlib import Path
import time
import urllib.request
from read_android_module_content import session, HOST, NoRedirect
from read_simulator_program_content import _oracle


def read(serial, program, boundary):
    if program not in ('last_longer', 'keep_it_hard'): raise RuntimeError('Unreviewed destination')
    uid, token=session(serial)
    if uid != os.environ.get('COACH_EXPECTED_ACCOUNT_UID'): raise RuntimeError('Wrong fixture UID')
    headers={'Authorization':'FirebaseToken '+token,'AppVersion':'1.40.21','BuildVersion':'339',
             'X-Gender':'male','Language':'en','Timezone':'Europe/Samara','Cache-Control':'no-cache'}
    opener=urllib.request.build_opener(NoRedirect())
    def get(route):
        with opener.open(urllib.request.Request(HOST+route,headers=headers),timeout=12) as response:
            return json.load(response)
    user=get('/api/v1/user/')['userdata']
    if user.get('analytics_profile',{}).get('email','').lower()!=os.environ['COACH_EXISTING_PROGRESS_EMAIL'].lower():
        raise RuntimeError('Wrong fixture email')
    result=_oracle(get('/api/v1/coachprogram/catalog/v3/'+program+'/?day_of_program=1'),program)
    now=int(time.time()*1000)
    if not 0<=now-boundary<=120000: raise RuntimeError('Stale UI action')
    result.update(source='Android catalog GET after UI selection; not captured application traffic',uid=uid,fetchedAt=now)
    return result


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--program',required=True)
    p.add_argument('--boundary',required=True,type=int);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
    try: a.output.write_text(json.dumps(read(a.serial,a.program,a.boundary),ensure_ascii=False,indent=2))
    except Exception: raise SystemExit('Android destination catalog evidence failed verification')
