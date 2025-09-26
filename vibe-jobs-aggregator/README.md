# Vibe Jobs Aggregator — Configurable Sources

Spring Boot service that ingests external job boards on a schedule and upserts them into the local database.

## Supported data sources
- **Greenhouse** — `https://boards-api.greenhouse.io/v1/boards/{slug}/jobs`
- **Lever** — `https://api.lever.co/v0/postings/{company}?mode=json`
- **Workday** — `https://{tenant-domain}/wday/cxs/{tenant}/{site}/jobs`

Each connector implements `SourceClient` and is wired through a factory so new providers can be added with minimal code.

## Configuration
Edit `src/main/resources/application.yml` under the `ingestion` section:

```yaml
ingestion:
  fixedDelayMs: 3600000
  initialDelayMs: 10000
  pageSize: 20
  mode: companies # or recent
  companies:
    - Stripe
    - Ramp
  recentDays: 7
  sources:
    - id: greenhouse-stripe
      type: greenhouse
      enabled: true
      runOnStartup: true
      options:
        slug: stripe
    - id: greenhouse-datadog
      type: greenhouse
      enabled: false
      runOnStartup: true
      options:
        slug: datadog
    - id: lever-ramp
      type: lever
      enabled: true
      runOnStartup: true
      options:
        company: ramp
    - id: workday-deloitte
      type: workday
      enabled: true
      runOnStartup: true
      options:
        company: Deloitte
        baseUrl: https://deloitte.wd1.myworkdayjobs.com
        tenant: deloitte
        site: DELOITTEJOBS
```

- Set `enabled: false` to skip a connector entirely.
- `runOnStartup: false` keeps the source scheduled but excludes it from the startup runner.
- `pageSize` controls the maximum page size passed to paginated connectors.
- `mode`
  - `companies`: only ingest jobs whose `company` matches the configured `companies` list.
  - `recent`: ignore the company list and only ingest roles whose `postedAt` is within the last `recentDays` (defaults to 7).
- `recentDays` is used when `mode=recent`.
- Each source entry supports `enabled` (toggle ingestion entirely) and `runOnStartup` (include/exclude from the startup runner). Add multiple entries per provider to pull several companies.

## Running locally
```
mvn spring-boot:run
```

`CareersApiStartupRunner` logs the number of jobs fetched from each source marked `runOnStartup=true`. The `JobIngestionScheduler` reuses the same configuration for recurring updates.

## Notes
- Greenhouse does not return `postedAt`; we stamp the current time. Extend `GreenhouseSourceClient` if you need more metadata.
- Lever timestamps are provided in epoch milliseconds and are normalised to `Instant`.
- Workday endpoints vary by tenant/site; ensure `baseUrl`, `tenant`, and `site` values match the organisation you are integrating.
