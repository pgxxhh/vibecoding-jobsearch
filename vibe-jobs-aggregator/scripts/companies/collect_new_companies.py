#!/usr/bin/env python3
"""Daily job-data-source company augmentation script.

This script:
 1. Loads current `job_data_source_company` records from MySQL.
 2. Reads a YAML file listing candidate companies per source.
 3. For each candidate, checks the vendor API to ensure there are jobs whose
    titles match predefined engineering / finance keywords.
 4. Outputs INSERT statements for the new companies that pass the check.

Usage example:
  python scripts/collect_new_companies.py \
      --mysql-url "mysql+pymysql://vibejobs:vibejobs@127.0.0.1:3306/vibejobs" \
      --candidates-file scripts/company_candidates.yml \
      --output scripts/job_data_source_company_patch.sql

This script uses only requests + SQLAlchemy (default driver: PyMySQL).
"""

from __future__ import annotations

import argparse
import datetime as dt
import logging
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional

import requests
import sqlalchemy as sa
from sqlalchemy import text
import yaml


LOG = logging.getLogger("collect-new-companies")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

# Keywords used to determine if a posting is related to engineering or finance roles.
ROLE_KEYWORDS = (
    "software engineer",
    "software developer",
    "frontend engineer",
    "backend engineer",
    "data engineer",
    "platform engineer",
    "devops",
    "financial analyst",
    "investment analyst",
    "quantitative analyst",
)


@dataclass(frozen=True)
class CandidateCompany:
    data_source_code: str
    reference: str
    display_name: str
    extra: Dict[str, str]


class BaseProbe:
    def __init__(self, candidate: CandidateCompany):
        self.candidate = candidate

    @staticmethod
    def matches(title: str) -> bool:
        title_lower = title.lower()
        return any(keyword in title_lower for keyword in ROLE_KEYWORDS)

    def has_target_job(self) -> bool:
        raise NotImplementedError


class GreenhouseProbe(BaseProbe):
    def has_target_job(self) -> bool:
        slug = self.candidate.reference
        url = f"https://boards-api.greenhouse.io/v1/boards/{slug}/jobs"
        resp = requests.get(url, timeout=15)
        if resp.status_code != 200:
            LOG.info("Greenhouse %s HTTP %s", slug, resp.status_code)
            return False
        data = resp.json()
        for job in data.get("jobs", []):
            title = job.get("title", "")
            if self.matches(title):
                return True
        return False


class LeverProbe(BaseProbe):
    def has_target_job(self) -> bool:
        slug = self.candidate.reference
        url = f"https://api.lever.co/v0/postings/{slug}"
        resp = requests.get(url, params={"mode": "json"}, timeout=15)
        if resp.status_code != 200:
            LOG.info("Lever %s HTTP %s", slug, resp.status_code)
            return False
        data = resp.json()
        for job in data:
            title = job.get("text", "")
            if self.matches(title):
                return True
        return False


class SmartRecruitersProbe(BaseProbe):
    def has_target_job(self) -> bool:
        slug = self.candidate.reference
        params = {"limit": 50, "offset": 0}
        while True:
            resp = requests.get(
                f"https://api.smartrecruiters.com/v1/companies/{slug}/postings",
                params=params,
                timeout=15,
            )
            if resp.status_code != 200:
                LOG.info("SmartRecruiters %s HTTP %s", slug, resp.status_code)
                return False
            data = resp.json()
            postings = data.get("content", [])
            for posting in postings:
                title = posting.get("name", "")
                if self.matches(title):
                    return True
            paging = data.get("paging", {})
            total = paging.get("totalElements", 0)
            offset = paging.get("offset", 0)
            limit = paging.get("limit", len(postings))
            if offset + limit >= total:
                break
            params["offset"] = offset + limit
        return False


PROBES = {
    "greenhouse": GreenhouseProbe,
    "lever": LeverProbe,
    "smartrecruiters": SmartRecruitersProbe,
}


def load_existing_companies(engine) -> Dict[str, set]:
    query = text("SELECT data_source_code, reference FROM job_data_source_company")
    existing: Dict[str, set] = defaultdict(set)
    with engine.connect() as conn:
        for row in conn.execute(query):
            existing[row.data_source_code].add((row.reference or "").lower())
    return existing


def load_candidates(path: Path) -> List[CandidateCompany]:
    raw = yaml.safe_load(path.read_text())
    result: List[CandidateCompany] = []
    for source_code, entries in (raw or {}).items():
        for elem in entries or []:
            result.append(
                CandidateCompany(
                    data_source_code=source_code,
                    reference=elem["reference"],
                    display_name=elem.get("display_name", elem["reference"]),
                    extra=elem.get("extra", {}),
                )
            )
    return result


def run_probe(candidate: CandidateCompany) -> bool:
    probe_cls = PROBES.get(candidate.data_source_code)
    if not probe_cls:
        LOG.warning("Unsupported data source: %s", candidate.data_source_code)
        return False
    try:
        ok = probe_cls(candidate).has_target_job()
        LOG.info("Probe %s/%s -> %s", candidate.data_source_code, candidate.reference, ok)
        return ok
    except requests.RequestException as exc:
        LOG.warning("Probe error %s/%s: %s", candidate.data_source_code, candidate.reference, exc)
        return False


def write_dml(new_companies: Iterable[CandidateCompany], output_path: Path) -> None:
    companies = list(new_companies)
    if not companies:
        output_path.write_text("-- no new companies\n", encoding="utf-8")
        return

    timestamp = dt.datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
    lines = [f"-- generated at {timestamp} UTC"]

    for company in companies:
        ref = company.reference.replace("'", "''")
        disp = company.display_name.replace("'", "''")
        lines.append(
            "INSERT IGNORE INTO job_data_source_company "
            "(data_source_code, reference, display_name) "
            f"VALUES ('{company.data_source_code}','{ref}','{disp}');"
        )

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    LOG.info("Wrote %s entries to %s", len(companies), output_path)


def main():
    parser = argparse.ArgumentParser(description="Collect new companies for job data sources")
    parser.add_argument("--mysql-url", required=True, help="SQLAlchemy URL, e.g. mysql+pymysql://user:pass@host:3306/db")
    parser.add_argument("--candidates-file", required=True, help="YAML file listing candidate companies")
    parser.add_argument("--output", required=True, help="Output SQL file path")
    args = parser.parse_args()

    engine = sa.create_engine(args.mysql_url)
    existing = load_existing_companies(engine)
    LOG.info("Existing companies loaded for %s sources", len(existing))

    candidates = load_candidates(Path(args.candidates_file))
    LOG.info("Loaded %s candidate companies", len(candidates))

    new_entries: List[CandidateCompany] = []
    for candidate in candidates:
        ref_lower = (candidate.reference or "").lower()
        if ref_lower in existing.get(candidate.data_source_code, set()):
            continue
        if run_probe(candidate):
            new_entries.append(candidate)

    write_dml(new_entries, Path(args.output))


if __name__ == "__main__":
    main()
