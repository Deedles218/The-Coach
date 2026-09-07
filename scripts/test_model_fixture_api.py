"""Official per-user preprod QA forms (COA-8704 / COA-9042 / COA-8754).

Never changes global A/B configuration or production data. Every write checks
that the UID belongs to the confirmed, dedicated mailbox in local Keychain.
HTTP success is not fixture readiness: the app must verify the resulting state.
"""
import http.cookiejar
from html.parser import HTMLParser
import re
import urllib.parse
import urllib.request

HOST = "https://coach-preprod-cf87bfd42b85.herokuapp.com"
DEDICATED_CASES = {"COA-8511", "COA-8512", "COA-8517", "COA-8518"}


class Form(HTMLParser):
    def __init__(self):
        super().__init__()
        self.inputs = {}
        self.options = []
        self.text = []
        self.ignored = 0

    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        if tag in ("script", "style"):
            self.ignored += 1
        if tag == "input" and values.get("name"):
            self.inputs[values["name"]] = values.get("value", "")
        if tag == "option":
            self.options.append(values.get("value", ""))

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self.ignored -= 1

    def handle_data(self, value):
        if not self.ignored and value.strip():
            self.text.append(value.strip())


class SameHostRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        if urllib.parse.urlparse(newurl).netloc != urllib.parse.urlparse(HOST).netloc:
            raise RuntimeError("Preprod fixture request redirected to another host")
        return super().redirect_request(request, fp, code, msg, headers, newurl)


def verify_preprod_account_email(uid, expected_email):
    """Read-only UID/email lookup. Does not grant fixture mutation permissions."""
    if not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", uid) or not expected_email:
        raise RuntimeError("Existing-account verification requires a UID and configured email")
    opener = urllib.request.build_opener(
        SameHostRedirect(), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    url = HOST + "/get_user_info/"
    with opener.open(urllib.request.Request(url, headers={"Referer": url}), timeout=30) as response:
        form = Form()
        form.feed(response.read().decode())
    token = form.inputs.get("csrfmiddlewaretoken")
    if not token:
        raise RuntimeError("Read-only account lookup has no CSRF token")
    request = urllib.request.Request(url, headers={"Referer": url}, data=urllib.parse.urlencode(
        {"user_id": uid, "csrfmiddlewaretoken": token}).encode())
    with opener.open(request, timeout=30) as response:
        result = Form()
        result.feed(response.read().decode())
    emails = re.findall(r"[\w.+-]+@[\w.-]+", "\n".join(result.text))
    if [value.lower() for value in emails] != [expected_email.lower()]:
        raise RuntimeError("Authenticated UID does not belong to the requested existing email")


class PreprodFixtures:
    def __init__(self, case, account):
        if (case not in DEDICATED_CASES or account.get("status") != "confirmed"
                or not re.fullmatch(r"[A-Za-z0-9_-]{20,128}", account.get("uid", ""))):
            raise RuntimeError("Fixture writes require a confirmed dedicated test identity")
        alias = "+" + case.replace("-", "").lower() + "@"
        if alias not in account.get("email", ""):
            raise RuntimeError("Refusing to modify a shared/non-fixture account")
        self.account = account
        self.opener = urllib.request.build_opener(
            SameHostRedirect(),
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def _request(self, route, data=None):
        if route not in ("/get_user_info/", "/set_abtest_program/", "/move_to_day/"):
            raise ValueError("Unsupported fixture operation")
        url = HOST + route
        request = urllib.request.Request(url, headers={"Referer": url})
        if data is not None:
            request.data = urllib.parse.urlencode(data).encode()
        with self.opener.open(request, timeout=30) as response:
            parser = Form()
            parser.feed(response.read().decode())
        return parser

    def _post(self, route, values, form=None):
        form = form or self._request(route)
        token = form.inputs.get("csrfmiddlewaretoken")
        if not token:
            raise RuntimeError("Preprod QA form has no CSRF token")
        return self._request(route, dict(values, csrfmiddlewaretoken=token))

    def verify_identity(self):
        response = self._post("/get_user_info/", {"user_id": self.account["uid"]})
        emails = re.findall(r"[\w.+-]+@[\w.-]+", "\n".join(response.text))
        if [x.lower() for x in emails] != [self.account["email"].lower()]:
            raise RuntimeError("Preprod UID/email mismatch: no fixture changes were made")

    def set_program(self, program, variant=""):
        self.verify_identity()
        form = self._request("/set_abtest_program/")
        choice = program + " " + variant
        if choice not in form.options:
            raise RuntimeError("The requested program variant is not offered by preprod")
        return self._post("/set_abtest_program/", {
            "user_id": self.account["uid"], "program": choice}, form).text

    def move_to_day(self, program, day):
        if not isinstance(day, int) or not 1 <= day <= 100:
            raise ValueError("Fixture day must be between 1 and 100")
        self.verify_identity()
        form = self._request("/move_to_day/")
        if program not in form.options:
            raise RuntimeError("The requested program is not offered by the day tool")
        return self._post("/move_to_day/", {
            "user_id": self.account["uid"], "program": program,
            "day": day, "module_no": "", "stage_no": ""}, form).text
