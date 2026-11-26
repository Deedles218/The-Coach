#!/usr/bin/env python3
"""Read a UID-verified catalog for the Android selector's destination, without selecting it."""
import argparse
import json
import os
from pathlib import Path
import time
import urllib.request
from read_android_module_content import session, api_host, NoRedirect
from read_simulator_program_content import _oracle


def read(serial, program, boundary):
    if program not in ('last_longer', 'keep_it_hard'): raise RuntimeError('Unreviewed destination')
    package=os.environ.get('COACH_ANDROID_EVIDENCE_PACKAGE')
    if package and package!='com.vamapps.thecoach':raise RuntimeError('Destination evidence is scoped to the male app')
    uid, token=session(serial)
    if uid != os.environ.get('COACH_EXPECTED_ACCOUNT_UID'): raise RuntimeError('Wrong fixture UID')
    version,build='1.40.21','339'
    if package:
        from read_android_email_enforcement import installed_build
        artifact=installed_build(serial,package);version,build=artifact['version'],artifact['build']
    headers={'Authorization':'FirebaseToken '+token,'AppVersion':version,'BuildVersion':build,
             'X-Gender':'male','Language':'en','Timezone':'Europe/Samara','Cache-Control':'no-cache'}
    opener=urllib.request.build_opener(NoRedirect())
    def get(route):
        with opener.open(urllib.request.Request(api_host()+route,headers=headers),timeout=12) as response:
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
