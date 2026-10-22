#!/usr/bin/env python3
"""Render isolated job results, keeping offline and UI counts separate."""
import collections
import datetime
from html import escape
import json
from pathlib import Path
import sys


def observed_result(directory, metadata):
    latest = {}
    for path in (directory / 'allure-results').glob('*-result.json'):
        try:
            data = json.loads(path.read_text())
        except json.JSONDecodeError:
            continue  # An active test may be finishing this file.
        identity = (data.get('fullName'), json.dumps(data.get('parameters', []), sort_keys=True))
        if identity in latest and data.get('stop', 0) < latest[identity].get('stop', 0):
            continue
        latest[identity] = data
    cases = []
    for data in latest.values():
        labels = {label['name']: label['value'] for label in data.get('labels', [])}
        status = data.get('status', 'unknown')
        cases.append({'class': labels.get('testClass', data.get('fullName', '')),
                      'name': labels.get('testMethod', data.get('name', '')),
                      'status': 'failed' if status in ('failed', 'broken') else status,
                      'message': data.get('statusDetails', {}).get('message', '')})
    return cases or metadata.get('cases', [])


def render(out):
    state = json.loads((out / 'status.json').read_text())
    cases = []
    jobs = []
    for metadata in state.get('completed', []) + ([state['active']] if state.get('active') else []):
        directory = out / metadata['variant'] / metadata['label']
        jobs.append({key: metadata.get(key) for key in ('variant', 'label', 'exitCode')})
        for case in observed_result(directory, metadata):
            cases.append(dict(case, variant=metadata['variant'], job=metadata['label'],
                              artifact=str(directory.relative_to(out))))
    def counts(rows):
        return dict(collections.Counter(row['status'] for row in rows))
    summary = {'generated': datetime.datetime.now(datetime.timezone.utc).isoformat(),
               'completedJobs': len(state['completed']), 'plannedJobs': state['planned'],
               'active': state.get('active'), 'jobs': jobs, 'cases': cases,
               'counts': counts(cases),
               'offlineCounts': counts(c for c in cases if c['job'] == 'offline-unit'),
               'uiCounts': counts(c for c in cases if c['job'] != 'offline-unit'),
               'perVariantUI': {variant: counts(c for c in cases if c['variant'] == variant
                                               and c['job'] != 'offline-unit')
                                for variant in sorted({c['variant'] for c in cases})}}
    (out / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2))
    body = ['<!doctype html><html><meta charset="utf-8"><title>iOS regression</title>',
            '<style>body{font:15px system-ui;margin:24px}table{border-collapse:collapse;width:100%}'
            'td,th{border:1px solid #ddd;padding:8px;text-align:left}.failed{color:#b02020}'
            '.passed{color:#176c32}small{white-space:pre-wrap}</style>', '<h1>iOS regression</h1>',
            '<p>Completed jobs: %s / %s. UI: %s. Offline: %s.</p>' % (
                summary['completedJobs'], summary['plannedJobs'], escape(str(summary['uiCounts'])),
                escape(str(summary['offlineCounts']))),
            '<p>UI per build: ' + escape(str(summary['perVariantUI'])) + '</p>',
            '<p>Job exit codes include setup/build failures even when no test case was produced. '
            'Failure messages require investigation; failed cases are not a count of product defects.</p>',
            '<p><a href="summary.json">Summary JSON</a> · <a href="status.json">Status</a> · '
            '<a href="allure-report/index.html">Allure</a></p>',
            '<table><tr><th>Build / job</th><th>Exit code</th></tr>']
    for job in jobs:
        body.append('<tr><td>%s / %s</td><td>%s</td></tr>' % tuple(
            escape(str(job[key])) for key in ('variant', 'label', 'exitCode')))
    body.append('</table><h2>Cases</h2><table><tr><th>Build / test</th><th>Result</th>'
                '<th>Failure</th><th>Evidence</th></tr>')
    for case in cases:
        body.append('<tr><td>%s / %s<br>%s</td><td class="%s">%s</td><td><small>%s</small></td>'
                    '<td><a href="%s/run.log">Log</a> · <a href="%s/surefire-reports/">XML</a></td></tr>' % (
                        escape(case['variant']), escape(case['job']), escape(case['name']),
                        escape(case['status']), escape(case['status']), escape(case.get('message', '')[:1400]),
                        escape(case['artifact']), escape(case['artifact'])))
    body.append('</table></html>')
    (out / 'report.html').write_text('\n'.join(body))
    return summary


if __name__ == '__main__':
    render(Path(sys.argv[1]).resolve())
