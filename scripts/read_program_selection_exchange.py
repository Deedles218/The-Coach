"""Read a successful current-action selection exchange, without network or cache writes."""
from contextlib import closing
import json
from pathlib import Path
import plistlib
import re
import sqlite3
import time

from read_simulator_program_content import (
    EvidenceError, _body, _number, _plist, _response_time, _verify_request,
)

ROOT = '/api/v1/coachprogram/'
ROUTES = frozenset(('main_journey/', 'secondary_programs/', 'onboarding/complete/',
                    'active_program/'))
CARD_KEYS = ('program_id', 'title', 'membership_status', 'is_recommended', 'duration_days', 'lessons_count')
STATE_KEYS = ('main_journey_program_id', 'secondary_program_ids', 'active_program_id',
              'onboarding_program_selection_completed')


def is_activation_route(route):
    return isinstance(route, str) and bool(re.fullmatch(r'[a-z0-9_]{1,80}/add_and_activate/', route))


def is_selection_route(route):
    return isinstance(route, str) and (route in ROUTES or is_activation_route(route))


def read_exchange(database, host, uid, route, not_before_ms, now=None):
    now = time.time() if now is None else now
    if not is_selection_route(route) or not _number(not_before_ms) or not 0 < not_before_ms <= now * 1000:
        raise EvidenceError('A supported selection route and current-action timestamp are required')
    if host != 'https://coach-preprod-cf87bfd42b85.herokuapp.com':
        raise EvidenceError('Program exchange evidence requires preprod')
    database = Path(database).resolve()
    full_route, hostname = ROOT + route, host.split('://', 1)[1]
    with closing(sqlite3.connect(database.as_uri() + '?mode=ro', uri=True, timeout=2)) as connection:
        connection.execute('PRAGMA query_only=ON')
        rows = connection.execute(
            'SELECT b.request_object,b.response_object,d.receiver_data,d.isDataOnFS '
            'FROM cfurl_cache_response r JOIN cfurl_cache_blob_data b ON r.entry_ID=b.entry_ID '
            'JOIN cfurl_cache_receiver_data d ON r.entry_ID=d.entry_ID WHERE r.request_key=?',
            (host + full_route,)).fetchall()
    candidates = []
    for request, response, stored, filesystem in rows:
        stamp = _response_time(response, now, hostname, full_route)
        if stamp * 1000 >= not_before_ms:
            candidates.append((stamp, request, stored, filesystem))
    if not candidates:
        raise EvidenceError('No successful selection response belongs to the current UI action')
    candidates.sort(key=lambda row: row[0], reverse=True)
    if len(candidates) > 1 and candidates[0][0] == candidates[1][0]:
        raise EvidenceError('Selection response timestamp is ambiguous')
    stamp, request, stored, filesystem = candidates[0]
    _verify_request(request, uid, now, hostname, full_route)
    items = _plist(request, 'request', hostname, full_route)
    # Observed CFURL request v9 stores method at 18 and the body segments at 21.
    if len(items) != 22 or items[18] != 'POST':
        raise EvidenceError('Unsupported selection request method/body metadata')
    segments = items[21]
    # Observed on Complete and <program_id>/add_and_activate POST. Do not treat arbitrary
    # strings (or a missing Main/Secondary request body) as an empty payload.
    if (route == 'onboarding/complete/' or is_activation_route(route)) and segments == '__CFURLRequestNullTokenString__':
        segments = []
    if not isinstance(segments, list) or not all(isinstance(segment, bytes) for segment in segments):
        raise EvidenceError('Unsupported selection request body segments')
    body = b''.join(segments)
    payload = json.loads(body) if body else None
    if is_activation_route(route) and payload is not None:
        raise EvidenceError('Activation program ID belongs in the URL; the observed POST is bodyless')
    if payload is not None and (not isinstance(payload, dict)
                                or set(payload) - {'program_id', 'program_ids'}):
        raise EvidenceError('Unexpected selection request payload')
    response = _body(database.parent, stored, filesystem)
    if not isinstance(response, dict):
        raise EvidenceError('Selection response is not an object')
    if route == 'main_journey/':
        card = response.get('main_journey')
        if not isinstance(card, dict):
            raise EvidenceError('Selection response has no Main Journey')
        result = {'main_journey': {key: card[key] for key in CARD_KEYS if key in card}}
    elif route == 'secondary_programs/':
        cards = response.get('secondary_programs')
        if not isinstance(cards, list) or not all(isinstance(card, dict) for card in cards):
            raise EvidenceError('Selection response has no Secondary Programs')
        result = {'secondary_programs': [{key: card[key] for key in CARD_KEYS if key in card} for card in cards]}
    elif route == 'active_program/':
        result = {key: response[key] for key in STATE_KEYS if key in response}
    else:
        if not all(key in response for key in STATE_KEYS):
            raise EvidenceError('Selection response has no complete ProgramSelectionState')
        result = {key: response[key] for key in STATE_KEYS}
    return {'route': route, 'http_status': 200, 'response_time_ms': stamp * 1000,
            'request': payload, 'response': result}
