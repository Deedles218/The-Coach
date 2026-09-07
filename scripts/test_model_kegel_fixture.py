"""User-approved Kegel Challenge fixtures; UI performs reset and the tested second move.

API setup contracts: COA-8315, Confluence 3473408160. No account deletion,
subscription changes, global configuration, or writes to another program.
"""
import datetime
import json
import urllib.request

from test_model_app_api import PreprodAppClient
from test_model_fixture_api import HOST

SCOPE = {"program": "kegel_only", "variantChangeAllowed": True,
         "dayChangeAllowed": True, "progressResetAllowed": True,
         "accountDeletionAllowed": False, "otherProgramsAllowed": False}
CASES = ("COA-8511", "COA-8512")
ACTION = "exercise_kegel_sport_w1_t1"


def require_approval(case, account):
    if (case not in CASES or account.get("allowedCases") != list(CASES)
            or account.get("status") != "identity-and-premium-verified"
            or account.get("kegelMutationScope") != SCOPE
            or account.get("accountDeletionAllowed") is not False):
        raise RuntimeError("Missing explicit Kegel Challenge fixture approval; old dedicated aliases cannot be used")


class KegelFixture(PreprodAppClient):
    def __init__(self, case, account, udid):
        require_approval(case, account)
        super().__init__(case, dict(account, status="confirmed"), udid, read_only=True)
        # A separate authorized scope; get() below never permits arbitrary base-client reads.
        self.read_only = False
        self.account = account
        if not self.customization_access()["accessReady"]:
            raise RuntimeError("Kegel fixture requires active Premium on the current approved UID")

    def get(self, route, **query):
        if route != "/api/v1/user/":
            if route != "/api/v1/daily_program/v2/" or query.get("program_id") != "kegel_only":
                raise RuntimeError("Kegel fixture cannot read/select another program")
            day = query.get("day_of_program")
            if day is not None and (type(day) is not int or not 1 <= day <= 3):
                raise RuntimeError("Kegel fixture is restricted to days 1..3")
        return super().get(route, **query)

    def section(self, day):
        s = self.daily("kegel_only", day)["program_section"]
        if s.get("section_id") != "kegel_only" or s.get("day_in_program") != day:
            raise RuntimeError("Kegel response does not match the requested program/day")
        if not s.get("module_name") or not s.get("module_current_day"):
            raise RuntimeError("Kegel fixture lacks module metadata")
        return s

    @staticmethod
    def target(section, catch_up=False):
        matches = [q for q in section["questions"] if q.get("name") == ACTION
                   and (not catch_up or q.get("section_id") == 3)]
        if len(matches) != 1 or matches[0].get("allow_modification") is not True:
            raise RuntimeError("The expected modifiable Kegel practice is absent/ambiguous")
        return matches[0]

    def _setup_write(self, operation, action, day):
        require_approval(self.case, self.account)
        if operation not in ("move_action_forward", "action_completed") or type(day) is not int or day not in (1, 2):
            raise RuntimeError("Unsupported Kegel setup write")
        if operation == "move_action_forward" and (day != 1 or action != ACTION):
            raise RuntimeError("API may only prepare the first move; the second move must use UI")
        # Only act on a card actually returned for this day on the approved program.
        if not any(q.get("name") == action for q in self.section(day)["questions"]):
            raise RuntimeError("Cannot prepare an action absent from this Kegel day")
        body = {"program_id": "kegel_only", "program_day": day, "action_name": action}
        if operation == "action_completed":
            if action == ACTION:
                raise RuntimeError("The tested practice must remain incomplete")
            body.update(action_type="done", date=datetime.date.today().isoformat())
        request = urllib.request.Request(HOST + "/api/v1/daily_plan/" + operation,
            data=json.dumps(body).encode(), headers=self.headers, method="POST")
        with self.opener.open(request, timeout=30) as response:
            response.read()

    def prepare_first_move(self):
        s = self.section(1)
        q = self.target(s)
        if q.get("completed") or q.get("is_moved_forward"):
            raise RuntimeError("UI reset did not clear the Kegel practice state")
        self._setup_write("move_action_forward", ACTION, 1)
        for name in dict.fromkeys(q["name"] for q in s["questions"] if q["name"] != ACTION and not q.get("completed")):
            self._setup_write("action_completed", name, 1)
        if self.target(self.section(1)).get("is_moved_forward") is not True:
            raise RuntimeError("First fixture move was not persisted")
        s2 = self.section(2)
        q2 = self.target(s2, catch_up=True)
        if q2.get("section_id") != 3 or q2.get("completed") or q2.get("is_moved_forward"):
            raise RuntimeError("First move did not create an incomplete TO CATCH-UP card on day 2")
        if self.case == "COA-8512":
            for name in dict.fromkeys(q["name"] for q in s2["questions"] if q["name"] != ACTION and not q.get("completed")):
                self._setup_write("action_completed", name, 2)
            pending = [q for q in self.section(2)["questions"] if not q.get("completed") and not q.get("is_moved_forward")]
            if len(pending) != 1 or pending[0].get("name") != ACTION:
                raise RuntimeError("COA-8512 requires exactly one remaining task, not merely one modifiable task")
        self.section(1)
        return {"program": "kegel_only", "actionName": ACTION, "title": q2["headline"],
                "sourceDay": 2, "targetDay": 3, "sourceLabel": "Stage 2 of " + str(s2["module_total_days"]),
                "targetLabel": "Stage 3 of " + str(s2["module_total_days"]), "singlePending": self.case == "COA-8512"}

    def verify_second_move(self):
        if self.target(self.section(2), catch_up=True).get("is_moved_forward") is not True:
            raise RuntimeError("UI second move was not persisted on day 2")
        q = self.target(self.section(3), catch_up=True)
        if q.get("section_id") != 3 or q.get("completed") or q.get("is_moved_forward"):
            raise RuntimeError("The same practice did not reach TO CATCH-UP on day 3")
        return {"program": "kegel_only", "actionName": ACTION, "day": 3, "section": "TO CATCH-UP", "verified": True}
