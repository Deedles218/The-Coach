#!/usr/bin/env python3
"""Merge this run only, keeping build variants separate in Allure history."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import subprocess
import sys

OUT = Path(sys.argv[1]).resolve()
CONFIG = json.loads((OUT / 'run-config.json').read_text())
aggregate = OUT / 'allure-results'
if aggregate.exists():
    archive = OUT / 'attempts' / ('allure-aggregate-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    archive.parent.mkdir(exist_ok=True)
    aggregate.rename(archive)
aggregate.mkdir(exist_ok=True)
for variant, app in CONFIG['apps'].items():
    for source in (OUT / variant).glob('*/allure-results'):
        if source.parent.name.startswith('targeted-'):
            continue
        version = app['version']
        for path in source.iterdir():
            if path.name.endswith('-result.json'):
                result = json.loads(path.read_text())
                result.setdefault('parameters', []).extend([
                    {'name': 'App variant', 'value': variant},
                    {'name': 'Application version', 'value': version}])
                result['historyId'] = hashlib.sha256((str(result.get('historyId')) + variant +
                                                     version + source.parent.name).encode()).hexdigest()
                result.setdefault('labels', []).append({'name': 'parentSuite',
                                                       'value': variant + ' ' + version})
                (aggregate / path.name).write_text(json.dumps(result, ensure_ascii=False))
            elif path.is_file() and path.suffix != '.properties':
                shutil.copy2(path, aggregate / path.name)
(aggregate / 'environment.properties').write_text(
    'Platform=iOS\n' + '\n'.join(key + '=' + str(CONFIG[key]) for key in ('deviceName', 'platformVersion')) + '\n')
subprocess.run(['allure', 'generate', str(aggregate), '-o', str(OUT / 'allure-report'), '--clean'], check=True)
