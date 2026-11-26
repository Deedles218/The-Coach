#!/usr/bin/env python3
"""Merge this run only, keeping build variants separate in Allure history."""
from pathlib import Path
import datetime
import filecmp
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile


def same_results(previous, generated):
    names = {path.name for path in generated.iterdir()}
    if names != {path.name for path in previous.iterdir()}:
        return False
    return all((previous / name).is_file() and (generated / name).is_file()
               and filecmp.cmp(previous / name, generated / name, shallow=False)
               for name in names)


OUT = Path(sys.argv[1]).resolve()
CONFIG = json.loads((OUT / 'run-config.json').read_text())
aggregate = OUT / 'allure-results'
with tempfile.TemporaryDirectory(prefix='.allure-results-', dir=OUT) as temporary:
    generated = Path(temporary) / 'allure-results'
    generated.mkdir()
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
                    (generated / path.name).write_text(json.dumps(result, ensure_ascii=False))
                elif path.is_file() and path.suffix != '.properties':
                    shutil.copy2(path, generated / path.name)
    (generated / 'environment.properties').write_text(
        'Platform=iOS\n' + '\n'.join(key + '=' + str(CONFIG[key]) for key in ('deviceName', 'platformVersion')) + '\n')
    if not aggregate.exists() or not same_results(aggregate, generated):
        if aggregate.exists():
            archive = OUT / 'attempts' / ('allure-aggregate-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
            archive.parent.mkdir(exist_ok=True)
            aggregate.rename(archive)
        generated.rename(aggregate)
subprocess.run(['allure', 'generate', str(aggregate), '-o', str(OUT / 'allure-report'), '--clean'], check=True)
