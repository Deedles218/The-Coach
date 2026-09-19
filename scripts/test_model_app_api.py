"""Scoped preprod API evidence/fixtures using the UID-verified simulator session.

The auth header is read in memory from the app's own URL cache. It is never
printed or persisted. No account belonging to another user/case can be used.
API contracts: Daily Program API Documentation and COA-8315.
"""
import base64
from contextlib import closing
import datetime
import json
from pathlib import Path
import plistlib
import sqlite3
import subprocess
import time
import urllib.parse
import urllib.error
import urllib.request

from test_model_fixture_api import HOST, PreprodFixtures, SameHostRedirect, verify_preprod_account_email

BUNDLE = "com.vamapps.preprod.The-Coach"


class PreprodAppClient:
    def __init__(self, case, account, udid, read_only=False, shared_overall_health=False):
        self.read_only = read_only
        self.shared_overall_health = shared_overall_health
        if shared_overall_health:
            from test_model_shared_overall_health import ApprovedOverallHealthFixtures
            ApprovedOverallHealthFixtures(case, account).verify_identity()
        elif read_only:
            if case not in ("COA-8511", "COA-8512", "COA-9044") or account.get("status") != "confirmed":
                raise RuntimeError("Shared-account inspection requires a confirmed identity for an explicitly supported case")
            verify_preprod_account_email(account.get("uid", ""), account.get("email", ""))
        else:
            PreprodFixtures(case, account).verify_identity()
        self.case = case
        container = subprocess.check_output([
            "xcrun", "simctl", "get_app_container", udid, BUNDLE, "data"], text=True).strip()
        self.container = Path(container)
        self.expected_uid = account["uid"]
        cache = Path(container) / "Library/Caches" / BUNDLE / "Cache.db"
        connection = sqlite3.connect(cache.as_uri() + "?mode=ro", uri=True)
        try:
            rows = connection.execute(
                "SELECT b.request_object FROM cfurl_cache_blob_data b JOIN cfurl_cache_response r "
                "ON b.entry_ID=r.entry_ID WHERE r.request_key LIKE ? ORDER BY r.entry_ID DESC",
                (HOST + "/api/v1/daily_program/v2/?%",)).fetchall()
        finally:
            connection.close()
        self.headers = None
        for (payload,) in rows:
            request = plistlib.loads(payload)
            for item in request.get("Array", []):
                if not isinstance(item, dict) or "Authorization" not in item:
                    continue
                token = item["Authorization"].split(" ")[-1]
                parts = token.split(".")
                if len(parts) != 3:
                    continue
                claims = json.loads(base64.urlsafe_b64decode(parts[1] + "=" * (-len(parts[1]) % 4)))
                if claims.get("user_id") != account["uid"] or claims.get("exp", 0) <= time.time():
                    continue
                self.headers = {k: v for k, v in item.items() if k in (
                    "Authorization", "BuildVersion", "AppVersion", "Language", "Timezone",
                    "X-Gender", "Accept", "Content-Type")}
        if not self.headers:
            raise RuntimeError("No fresh authenticated daily-plan request for this fixture; log in first")
        if any("\r" in value or "\n" in value for value in self.headers.values()):
            raise RuntimeError("Invalid cached request header")
        self.opener = urllib.request.build_opener(SameHostRedirect())

    def get(self, route, **query):
        if getattr(self, "shared_overall_health", False):
            if route not in ("/api/v1/user/", "/api/v1/daily_program/v2/", "/api/v1/coachprogram/catalog/v3/overall_health/", "/api/v1/coachprogram/removed_actions/"):
                raise RuntimeError("Shared fixture API is limited to Overall Health")
            if route in ("/api/v1/daily_program/v2/", "/api/v1/coachprogram/removed_actions/") and query.get("program_id") != "overall_health":
                raise RuntimeError("Shared fixture cannot select/read another program")
            if "day_of_program" in query and (type(query["day_of_program"]) is not int or not 1 <= query["day_of_program"] <= 3):
                raise RuntimeError("Shared fixture only prepares Overall Health days 1..3")
        if getattr(self, "read_only", False) and route != "/api/v1/user/":
            raise RuntimeError("Shared-account inspection cannot change a program or selected day")
        if route not in ("/api/v1/daily_program/v2/", "/api/v1/coachprogram/catalog/v3/overall_health/",
                         "/api/v1/coachprogram/removed_actions/", "/api/v1/coachprogram/progress",
                         "/api/v1/user/"):
            raise ValueError("Unsupported fixture API read")
        url = HOST + route + ("?" + urllib.parse.urlencode(query) if query else "")
        with self.opener.open(urllib.request.Request(url, headers=self.headers), timeout=30) as response:
            return json.load(response)

    def customization_access(self):
        """Read prerequisites, not fixture readiness; never grants entitlements."""
        if self.case not in ("COA-8511", "COA-8512", "COA-8518"):
            raise RuntimeError("Access check requires a dedicated customization case")
        database = self.container / "Library/com.amplitude.database"
        with closing(sqlite3.connect(database.as_uri() + "?mode=ro", uri=True)) as connection:
            identity = connection.execute("SELECT value FROM store WHERE key='user_id'").fetchone()
        if not identity or identity[0] != self.expected_uid:
            raise RuntimeError("Current simulator identity differs from the fixture; access check refused")
        active = self.get("/api/v1/user/").get("userdata", {}).get("subscription", {}).get("active")
        result = {"case": self.case, "checkedAt": int(time.time() * 1000),
                  "identityVerified": True, "subscriptionActive": active if isinstance(active, bool) else None,
                  "blockers": []}
        if active is not True:
            result["blockers"].append("premium_inactive" if active is False else "subscription_state_unknown")
        if self.case == "COA-8518":
            config_db = self.container / "Library/Application Support/Google/RemoteConfig/RemoteConfig.sqlite3"
            flag = {}
            if config_db.exists():
                with closing(sqlite3.connect(config_db.as_uri() + "?mode=ro", uri=True)) as connection:
                    row = connection.execute(
                        "SELECT value FROM main_active WHERE key='abtest_complete_to_unlock_popup'").fetchone()
                if row:
                    flag = json.loads(row[0])
            result["popupConfig"] = {key: flag.get(key) for key in ("isEnabled", "metricsId")}
            if flag.get("isEnabled") is not True:
                result["blockers"].append("popup_branch_disabled_or_unknown")
        result["accessReady"] = not result["blockers"]
        return result

    def daily(self, program=None, day=None):
        # Despite GET, an explicit day also selects that day for the user.
        # See "Новый Дейли План" (Confluence 3473408160). Callers traversing
        # days must restore their fixture, and must not parallelize requests.
        query = {"date": datetime.date.today().isoformat()}
        if program is not None:
            query["program_id"] = program
        if day is not None:
            query["day_of_program"] = day
        return self.get("/api/v1/daily_program/v2/", **query)

    def prepare_removed_meal_plan(self):
        """Reversible removal of a program card, never deletion of an account."""
        if self.case != "COA-8517":
            raise RuntimeError("The removed-card fixture is restricted to COA-8517")
        title = "Check Your Meal Plan"
        program = "overall_health"
        removed = self.get("/api/v1/coachprogram/removed_actions/", program_id=program)["items"]
        if any(item.get("headline") == title for item in removed):
            # Recover only this fixture card after an interrupted earlier run.
            # This is preparation; the scenario below still exercises UI Restore.
            # Despite the parameter's name, the real app submits action IDs,
            # not localized headlines (verified from its Restore request).
            self._recovery_fixture_write("restore", {"action_titles": ["exercise_meal_plan_check"], "program_id": program})
            after_reset = self.get("/api/v1/coachprogram/removed_actions/", program_id=program)["items"]
            if any(item.get("headline") == title for item in after_reset):
                raise RuntimeError("The interrupted-run reset did not restore the fixture card")
        section = self.daily(program, 2)["program_section"]
        matches = [q for q in section["questions"] if q.get("name") == "exercise_meal_plan_check"]
        if len(matches) != 1 or matches[0].get("allow_modification") is not True:
            raise RuntimeError("Expected modifiable meal-plan card is absent on Overall Health day 2")
        days = [int(day) for day in matches[0]["program_days"].split(",")]
        if section["total_days"] != 70 or days != list(range(2, 71)) or matches[0]["headline"] != title:
            raise RuntimeError("Overall Health content changed; re-review the recovery fixture contract")
        self._recovery_fixture_write("remove", {"action_name": matches[0]["name"],
                                                "program_id": program, "program_day": 2})
        removed = self.get("/api/v1/coachprogram/removed_actions/", program_id=program)["items"]
        matches = [item for item in removed if item.get("headline") == title]
        if len(matches) != 1:
            raise RuntimeError("Removed-card fixture was not confirmed by the backend")
        return {"program": program, "title": title, "removed": True,
                "actionName": "exercise_meal_plan_check", "applicableDays": days,
                "baselineContentValidated": True}

    def _recovery_fixture_write(self, operation, body):
        if getattr(self, "read_only", False):
            raise RuntimeError("Read-only account inspection cannot modify program cards")
        routes = {"remove": ("/api/v1/daily_plan/remove_action", "DELETE"),
                  "restore": ("/api/v1/coachprogram/restore_actions/", "POST")}
        if self.case != "COA-8517" or body.get("program_id") != "overall_health":
            raise RuntimeError("Recovery fixture writes are limited to their dedicated account/program")
        if (operation == "remove" and (body.get("action_name") != "exercise_meal_plan_check" or body.get("program_day") != 2)
                or operation == "restore" and body.get("action_titles") != ["exercise_meal_plan_check"]):
            raise RuntimeError("Refusing to modify a non-fixture program card")
        route, method = routes[operation]
        request = urllib.request.Request(HOST + route, data=json.dumps(body).encode(),
                                         headers=self.headers, method=method)
        try:
            with self.opener.open(request, timeout=30) as response:
                response.read()
        except urllib.error.HTTPError as error:
            raise RuntimeError("Recovery fixture request failed: HTTP " + str(error.code)) from None
