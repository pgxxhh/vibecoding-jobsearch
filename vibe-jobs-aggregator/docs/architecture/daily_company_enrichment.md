# Daily Company Enrichment Script

This document describes how to run the automation that adds new company records to `job_data_source_company`.

## Overview

`scripts/collect_new_companies.py` performs the following steps each day:

1. Loads existing company mappings from MySQL (`job_data_source_company`).
2. Reads candidate companies from a YAML file.
3. For each candidate, queries the vendor’s public API to confirm that there are job postings whose titles match predefined keywords (software engineers, financial analysts, and closely related roles).
4. Produces a SQL file containing `INSERT IGNORE` statements for the new companies that pass the probe.

Currently supported data source types:

- `greenhouse`
- `lever`
- `smartrecruiters`

The script can be extended with additional probes as needed.

## Prerequisites

- Python 3.8+
- `requests`, `SQLAlchemy`, `PyYAML`, and a compatible MySQL driver (e.g., `pymysql`).
- Network access from the execution environment to the vendor APIs.
- Read access to the production database (or replica) for the `job_data_source_company` table.

## Installation

Recommended virtual environment setup:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install requests sqlalchemy pymysql pyyaml
```

## Configuration Files

### Candidate YAML

The script expects a YAML file listing candidate companies per data source:

```yaml
greenhouse:
  - reference: coinbase
    display_name: Coinbase
  - reference: rippling
    display_name: Rippling
lever:
  - reference: scaleai
    display_name: Scale AI
smartrecruiters:
  - reference: canva
    display_name: Canva
```

Each entry must provide:

- `reference`: slug or identifier used by the vendor API
- `display_name`: optional human-readable label (defaults to `reference`)

### Output SQL

The script writes a `.sql` file containing the DML statements. Executing the file will insert any newly discovered companies.

## Usage

Basic invocation:

```bash
python scripts/collect_new_companies.py \
    --mysql-url "mysql+pymysql://vibejobs:vibejobs@127.0.0.1:3306/vibejobs" \
    --candidates-file scripts/company_candidates.yml \
    --output scripts/job_data_source_company_patch.sql
```

The executable supports:

- `--mysql-url`: SQLAlchemy connection string (include user, password, host, port, and database).
- `--candidates-file`: Path to YAML file described above.
- `--output`: SQL file path; the script rewrites its content on every run.

If no new companies are detected, the output file contains `-- no new companies`.

## Automation / Scheduling

To run daily at 01:00 (server local time) via cron:

```cron
0 1 * * * /usr/bin/python3 /opt/vibe/scripts/collect_new_companies.py \
    --mysql-url "mysql+pymysql://vibejobs:vibejobs@127.0.0.1:3306/vibejobs" \
    --candidates-file /opt/vibe/scripts/company_candidates.yml \
    --output /opt/vibe/scripts/job_data_source_company_patch.sql
```

After each run, review the generated SQL and execute it against the target environment.

## Extending Probes

To add support for new platforms:

1. Implement a subclass of `BaseProbe` that returns `True` when a matching job is found.
2. Register the probe in `PROBES` using the corresponding source code.
3. Update the candidate YAML with entries for the new source type.

## Monitoring / Logs

The script emits INFO-level logs, including:

- Existing data sources loaded
- Candidate counts
- API HTTP status codes
- Probe outcomes
- Number of records written to the DML file

Example log excerpt:

```
2025-10-15 00:01:00 INFO collect-new-companies Existing companies loaded for 7 sources
2025-10-15 00:01:00 INFO collect-new-companies Loaded 15 candidate companies
2025-10-15 00:01:02 INFO collect-new-companies Probe greenhouse/rippling -> True
2025-10-15 00:01:02 INFO collect-new-companies Wrote 3 entries to /opt/vibe/scripts/job_data_source_company_patch.sql
```

---

**Author:** Data Engineering Team
**Last Updated:** 2025-10-15
