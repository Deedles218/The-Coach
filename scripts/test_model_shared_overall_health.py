"""Explicitly approved shared-account fixture scope, separate from disposable aliases.

Only Overall Health and COA-8511/8512. Never deletes an account, purchases
Premium, changes global configuration, or mutates another program.
"""
import http.cookiejar
import urllib.request

from test_model_fixture_api import PreprodFixtures, SameHostRedirect, verify_preprod_account_email

CASES = ("COA-8511", "COA-8512")
SCOPE = {"program": "overall_health", "variantChangeAllowed": True,
         "dayChangeAllowed": True, "progressResetAllowed": True,
         "accountDeletionAllowed": False, "otherProgramsAllowed": False}


def require_approval(case, account):
    if (case not in CASES or account.get("allowedCases") != list(CASES)
            or account.get("status") != "identity-and-premium-verified"
            or account.get("mutationScope") != SCOPE
            or account.get("accountDeletionAllowed") is not False):
        raise RuntimeError("Missing explicit, program-scoped shared Premium fixture approval")


class ApprovedOverallHealthFixtures:
    _request = PreprodFixtures._request
    _post = PreprodFixtures._post

    def __init__(self, case, account):
        require_approval(case, account)
        self.account = account
        self.opener = urllib.request.build_opener(SameHostRedirect(),
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def verify_identity(self):
        verify_preprod_account_email(self.account.get("uid", ""), self.account.get("email", ""))

    def set_program(self, program, variant=""):
        if program != "overall_health" or variant not in ("", "theory_only"):
            raise RuntimeError("Shared fixture variant change is limited to Overall Health")
        return PreprodFixtures.set_program(self, program, variant)

    def move_to_day(self, program, day):
        if program != "overall_health" or type(day) is not int or not 1 <= day <= 3:
            raise RuntimeError("Shared fixture day preparation is limited to Overall Health days 1..3")
        return PreprodFixtures.move_to_day(self, program, day)
