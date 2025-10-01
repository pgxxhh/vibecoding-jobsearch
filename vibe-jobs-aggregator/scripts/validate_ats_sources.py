#!/usr/bin/env python3
"""Validate SmartRecruiters/Workable/Recruitee overrides defined in application.yml."""

from __future__ import annotations

import argparse
import json
import os
import sys
import textwrap
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Iterable, List
from pathlib import Path


CONFIG_PATH = os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources", "application.yml"
)
TIMEOUT = 15
USER_AGENT = "VibeCoding-JobAggregator/validator"


CONFIG_PATH = os.path.normpath(CONFIG_PATH)

@dataclass
class ValidationResult:
    company: str
    source: str
    base_url: str
    success: bool
    total: int
    status: int
    error: str | None = None

    def as_row(self) -> List[str]:
        status_text = "ok" if self.success else f"fail ({self.status})"
        total_text = str(self.total) if self.total >= 0 else "-"
        return [self.company, self.source, total_text, status_text, self.base_url, self.error or ""]


class ConfigError(RuntimeError):
    pass


def parse_overrides(path: str) -> dict:
    try:
        lines = Path(path).read_text(encoding="utf-8").splitlines()
    except FileNotFoundError as exc:
        raise ConfigError(f"Configuration file not found: {path}") from exc

    overrides: dict[str, dict[str, dict[str, object]]] = {}
    inside = False
    base_indent = 0
    current_company: str | None = None
    current_source: str | None = None

    for raw_line in lines:
        if not inside:
            if raw_line.strip() == "companyOverrides:":
                inside = True
                base_indent = len(raw_line) - len(raw_line.lstrip())
            continue

        if raw_line.strip() == "" or raw_line.strip().startswith("#"):
            continue

        indent = len(raw_line) - len(raw_line.lstrip())
        if indent <= base_indent and raw_line.strip():
            break

        stripped = raw_line.strip()

        if indent == base_indent + 2 and stripped.endswith(":"):
            current_company = stripped[:-1]
            overrides.setdefault(current_company, {})
            current_source = None
            continue

        if current_company is None:
            continue

        if indent == base_indent + 4:
            if stripped != "sources:":
                continue
            current_source = None
            continue

        if indent == base_indent + 6 and stripped.endswith(":"):
            current_source = stripped[:-1]
            overrides[current_company].setdefault(current_source, {"enabled": False, "options": {}})
            continue

        if current_source is None:
            continue

        if indent == base_indent + 8 and stripped.startswith("enabled:"):
            value = stripped.split(":", 1)[1].strip().lower()
            overrides[current_company][current_source]["enabled"] = value == "true"
            continue

        if indent == base_indent + 8 and stripped == "options:":
            overrides[current_company][current_source].setdefault("options", {})
            continue

        if indent >= base_indent + 10 and ":" in stripped:
            key, value = stripped.split(":", 1)
            overrides[current_company][current_source].setdefault("options", {})[key.strip()] = value.strip().strip('"')

    return overrides


def iter_overrides(config: dict, companies: Iterable[str] | None = None):
    requested = set(c.lower() for c in (companies or []))

    for company_key, sources in config.items():
        if requested and company_key.lower() not in requested:
            continue
        for source_name, source_data in sources.items():
            if source_name not in {"smartrecruiters", "workable", "recruitee"}:
                continue
            if not source_data.get("enabled", False):
                continue
            options = source_data.get("options", {})
            company_label = options.get("company") or company_key
            base_url = options.get("baseUrl")
            yield company_label, source_name, base_url


def _fetch_json(url: str) -> tuple[int, dict, str | None]:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            status = response.getcode()
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        return exc.code, {}, exc.reason
    except urllib.error.URLError as exc:
        return -1, {}, str(exc.reason)

    try:
        payload = json.loads(body)
    except json.JSONDecodeError as exc:
        return status, {}, f"invalid JSON: {exc}"
    return status, payload, None


def validate_smartrecruiters(company: str, base_url: str | None) -> ValidationResult:
    base = base_url or f"https://api.smartrecruiters.com/v1/companies/{company}"
    url = f"{base.rstrip('/')}/postings?limit=1"
    status, payload, error = _fetch_json(url)
    total = int(payload.get("totalFound", 0)) if payload else -1
    success = error is None and total > 0
    if not success and error is None:
        error = "no postings returned"
    return ValidationResult(company, "smartrecruiters", base, success, total, status, error)


def validate_workable(company: str, base_url: str | None) -> ValidationResult:
    base = (base_url or f"https://apply.workable.com/api/v3/accounts/{company}").rstrip("/")
    url = f"{base}/jobs?limit=1"
    status, payload, error = _fetch_json(url)
    jobs = payload.get("jobs", []) if payload else []
    total = len(jobs) if isinstance(jobs, list) else -1
    success = error is None and isinstance(jobs, list) and total > 0
    if not success and error is None:
        error = "no jobs returned"
    return ValidationResult(company, "workable", base, success, total, status, error)


def validate_recruitee(company: str, base_url: str | None) -> ValidationResult:
    base = (base_url or f"https://{company}.recruitee.com/api").rstrip("/")
    url = f"{base}/offers/?limit=1"
    status, payload, error = _fetch_json(url)
    offers = payload.get("offers", []) if payload else []
    total = len(offers) if isinstance(offers, list) else -1
    success = error is None and isinstance(offers, list) and total > 0
    if not success and error is None:
        error = "no offers returned"
    return ValidationResult(company, "recruitee", base, success, total, status, error)


VALIDATORS = {
    "smartrecruiters": validate_smartrecruiters,
    "workable": validate_workable,
    "recruitee": validate_recruitee,
}


def run(companies: Iterable[str] | None = None) -> List[ValidationResult]:
    config = parse_overrides(CONFIG_PATH)
    results: List[ValidationResult] = []
    for company, source, base_url in iter_overrides(config, companies):
        validator = VALIDATORS[source]
        result = validator(company, base_url)
        results.append(result)
    return results


def print_summary(results: Iterable[ValidationResult]) -> None:
    headers = ["Company", "Source", "Sample", "Status", "Base URL", "Error"]
    rows = [headers]
    for result in results:
        rows.append(result.as_row())
    widths = [max(len(row[i]) for row in rows) for i in range(len(headers))]
    for idx, row in enumerate(rows):
        line = " | ".join(value.ljust(widths[i]) for i, value in enumerate(row))
        if idx == 0:
            print(line)
            print("-+-".join("-" * width for width in widths))
        else:
            print(line)


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate SmartRecruiters/Workable/Recruitee overrides",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent(
            """Examples:
  python scripts/validate_ats_sources.py --companies okx bitget
  python scripts/validate_ats_sources.py
"""
        ),
    )
    parser.add_argument(
        "--companies",
        nargs="*",
        help="Subset of company override keys to validate (default: all overrides)",
    )
    return parser.parse_args(argv)


def main(argv: List[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        results = run(args.companies)
    except ConfigError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    print_summary(results)
    failures = [r for r in results if not r.success]
    if failures:
        print(f"\n{len(failures)} override(s) failed validation", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
