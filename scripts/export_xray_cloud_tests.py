#!/usr/bin/env python3
"""Export Xray Cloud tests from project COA to Test Case Importer CSV.

Required environment variables:
  XRAY_CLIENT_ID
  XRAY_CLIENT_SECRET

Optional environment variables:
  XRAY_BASE_URL      Default: https://xray.cloud.getxray.app
                     Region examples: https://eu.xray.cloud.getxray.app,
                     https://us.xray.cloud.getxray.app
  XRAY_PROJECT_KEY   Default: COA
  XRAY_OUTPUT_DIR    Default: xray-export
"""

from __future__ import annotations

import csv
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


PROJECT_KEY = os.getenv("XRAY_PROJECT_KEY", "COA")
BASE_URL = os.getenv("XRAY_BASE_URL", "https://xray.cloud.getxray.app").rstrip("/")
OUTPUT_DIR = Path(os.getenv("XRAY_OUTPUT_DIR", "xray-export"))
PAGE_SIZE = 100
TRASH_MARKERS = ("trash", "корзина")


def required_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def request_json(url: str, payload: dict[str, Any], token: str | None = None) -> Any:
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = Request(url, data=body, headers=headers, method="POST")
    try:
        with urlopen(request, timeout=60) as response:
            raw = response.read().decode("utf-8")
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code} from {url}: {detail}") from error
    except URLError as error:
        raise RuntimeError(f"Request failed for {url}: {error}") from error

    if not raw:
        return None
    return json.loads(raw)


def authenticate() -> str:
    response = request_json(
        f"{BASE_URL}/api/v2/authenticate",
        {
            "client_id": required_env("XRAY_CLIENT_ID"),
            "client_secret": required_env("XRAY_CLIENT_SECRET"),
        },
    )
    if not isinstance(response, str):
        raise RuntimeError(f"Unexpected authentication response: {response!r}")
    return response


def graphql(token: str, query: str, variables: dict[str, Any]) -> dict[str, Any]:
    response = request_json(
        f"{BASE_URL}/api/v2/graphql",
        {"query": query, "variables": variables},
        token=token,
    )
    if response.get("errors"):
        raise RuntimeError(json.dumps(response["errors"], ensure_ascii=False, indent=2))
    return response["data"]


def fetch_tests(token: str) -> list[dict[str, Any]]:
    query = """
    query GetTests($jql: String!, $start: Int!) {
      getTests(jql: $jql, limit: 100, start: $start) {
        total
        start
        limit
        results {
          issueId
          projectId
          testType { name kind }
          folder { name path }
          steps {
            id
            action
            data
            result
          }
          unstructured
          gherkin
          jira(fields: [
            "key",
            "summary",
            "priority",
            "labels",
            "description",
            "components"
          ])
        }
      }
    }
    """
    jql = f'project = "{PROJECT_KEY}" AND issuetype = Test ORDER BY key ASC'
    start = 0
    tests: list[dict[str, Any]] = []

    while True:
        page = graphql(token, query, {"jql": jql, "start": start})["getTests"]
        results = page["results"] or []
        tests.extend(results)

        total = int(page["total"])
        start += len(results)
        print(f"Fetched {len(tests)}/{total} tests", file=sys.stderr)
        if not results or start >= total:
            break

    return tests


def jira_field(test: dict[str, Any], field: str, default: Any = "") -> Any:
    jira = test.get("jira") or {}
    fields = jira.get("fields") or {}
    if field in jira:
        return jira[field]
    return fields.get(field, default)


def plain_text_from_adf(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if isinstance(value, dict):
        if value.get("type") == "text":
            return value.get("text", "")
        parts = [plain_text_from_adf(item) for item in value.get("content", [])]
        return "\n".join(part for part in parts if part)
    if isinstance(value, list):
        return "\n".join(part for part in (plain_text_from_adf(item) for item in value) if part)
    return str(value)


def test_key(test: dict[str, Any]) -> str:
    return str(jira_field(test, "key", "") or "")


def test_summary(test: dict[str, Any]) -> str:
    return str(jira_field(test, "summary", "") or "")


def test_priority(test: dict[str, Any]) -> str:
    priority = jira_field(test, "priority", "") or ""
    if isinstance(priority, dict):
        return str(priority.get("name", ""))
    return str(priority)


def test_labels(test: dict[str, Any]) -> str:
    labels = jira_field(test, "labels", []) or []
    if isinstance(labels, list):
        return ";".join(str(label) for label in labels)
    return str(labels)


def test_components(test: dict[str, Any]) -> str:
    components = jira_field(test, "components", []) or []
    if isinstance(components, list):
        return ";".join(str(component.get("name", component)) for component in components)
    return str(components)


def test_description(test: dict[str, Any]) -> str:
    return plain_text_from_adf(jira_field(test, "description", ""))


def folder_path(test: dict[str, Any]) -> str:
    folder = test.get("folder") or {}
    return str(folder.get("path") or "").strip("/")


def is_in_trash(test: dict[str, Any]) -> bool:
    parts = [part.strip().lower() for part in folder_path(test).split("/") if part.strip()]
    return any(marker in part for part in parts for marker in TRASH_MARKERS)


def test_type_name(test: dict[str, Any]) -> str:
    test_type = test.get("testType") or {}
    return str(test_type.get("name") or "")


def base_row(issue_id: int, test: dict[str, Any]) -> dict[str, str]:
    return {
        "Issue Id": str(issue_id),
        "Issue key": test_key(test),
        "Issue type": "test",
        "Test type": test_type_name(test),
        "Test Summary": test_summary(test),
        "Test Priority": test_priority(test),
        "Description": test_description(test),
        "Labels": test_labels(test),
        "Component": test_components(test),
        "Action": "",
        "Data": "",
        "Result": "",
        "Project key": PROJECT_KEY,
        "Test Repo": folder_path(test),
        "Unstructured Definition": str(test.get("unstructured") or ""),
        "Gherkin Definition": str(test.get("gherkin") or ""),
    }


def csv_rows(tests: list[dict[str, Any]]) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for issue_id, test in enumerate(tests, start=1):
        row = base_row(issue_id, test)
        steps = test.get("steps") or []

        if not steps:
            rows.append(row)
            continue

        for index, step in enumerate(steps):
            step_row = row.copy()
            if index > 0:
                step_row.update(
                    {
                        "Issue key": "",
                        "Issue type": "",
                        "Test Summary": "",
                        "Test Priority": "",
                        "Description": "",
                        "Labels": "",
                        "Unstructured Definition": "",
                        "Gherkin Definition": "",
                    }
                )
            step_row["Action"] = str(step.get("action") or "")
            step_row["Data"] = str(step.get("data") or "")
            step_row["Result"] = str(step.get("result") or "")
            rows.append(step_row)

    return rows


def write_outputs(tests: list[dict[str, Any]]) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    filtered = [test for test in tests if not is_in_trash(test)]
    removed = len(tests) - len(filtered)

    raw_path = OUTPUT_DIR / f"{PROJECT_KEY.lower()}-xray-tests-raw.json"
    csv_path = OUTPUT_DIR / f"{PROJECT_KEY.lower()}-xray-tests-import.csv"

    raw_path.write_text(json.dumps(filtered, ensure_ascii=False, indent=2), encoding="utf-8")

    rows = csv_rows(filtered)
    fieldnames = [
        "Issue Id",
        "Issue key",
        "Issue type",
        "Test type",
        "Test Summary",
        "Test Priority",
        "Description",
        "Labels",
        "Component",
        "Action",
        "Data",
        "Result",
        "Project key",
        "Test Repo",
        "Unstructured Definition",
        "Gherkin Definition",
    ]
    with csv_path.open("w", newline="", encoding="utf-8-sig") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter=";")
        writer.writeheader()
        writer.writerows(rows)

    print(f"Total tests fetched: {len(tests)}")
    print(f"Excluded as trash: {removed}")
    print(f"Exported tests: {len(filtered)}")
    print(f"CSV: {csv_path}")
    print(f"Raw JSON: {raw_path}")


def main() -> None:
    token = authenticate()
    tests = fetch_tests(token)
    write_outputs(tests)


if __name__ == "__main__":
    main()
