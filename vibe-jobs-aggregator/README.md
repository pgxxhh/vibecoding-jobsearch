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
  concurrency: 4
  mode: companies # or recent
  companies:
    - Stripe
    - Ramp
    - Deloitte
    - Datadog
  recentDays: 7
  sources:
    - id: greenhouse
      type: greenhouse
      enabled: true
      runOnStartup: true
      options:
        slug: "{{slug}}"
    - id: lever
      type: lever
      enabled: true
      runOnStartup: true
      options:
        company: "{{slug}}"
    - id: workday
      type: workday
      enabled: true
      runOnStartup: true
      options:
        baseUrl: "https://{{slug}}.wd1.myworkdayjobs.com"
        tenant: "{{slug}}"
        site: "{{slugUpper}}"
```

- Set `enabled: false` to skip a connector entirely.
- `runOnStartup: false` keeps the source scheduled but excludes it from the startup runner.
- `pageSize` controls the maximum page size passed to paginated connectors.
- `mode`
  - `companies`: only ingest jobs whose `company` matches the configured `companies` list.
  - `recent`: ignore the company list and only ingest roles whose `postedAt` is within the last `recentDays` (defaults to 7).
- `recentDays` is used when `mode=recent`.
- Companies define provider-specific overrides under `sources`. Each enabled provider spawns one client per company; placeholders like `{{company}}`, `{{slug}}`, `{{slugUpper}}` are resolved automatically.
- `concurrency` controls how many provider/company tasks run in parallel (default 4).
- Each source entry supports `enabled` (toggle ingestion entirely) and `runOnStartup` (include/exclude from the startup runner).

## Running locally
```
mvn spring-boot:run
```

`CareersApiStartupRunner` logs the number of jobs fetched from each source marked `runOnStartup=true`. The `JobIngestionScheduler` reuses the same configuration for recurring updates.

## Database profiles

The service ships with dedicated Spring profiles so you can switch between the embedded H2 database and MySQL without editing configuration files:

- `application-h2.yml` — activates when `SPRING_PROFILES_ACTIVE=h2` (the default). Stores data on the local filesystem and enables the H2 console for quick development feedback.
- `application-mysql.yml` — activates when `SPRING_PROFILES_ACTIVE=mysql`. It expects a MySQL 8+ instance and defaults to validating the schema on startup.

Both profiles honour the same environment overrides: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO`, and `SPRING_JPA_DATABASE_PLATFORM`.

```bash
# Run with MySQL
SPRING_PROFILES_ACTIVE=mysql \
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/vibejobs?useSSL=false \
SPRING_DATASOURCE_USERNAME=vibejobs \
SPRING_DATASOURCE_PASSWORD=secret \
mvn spring-boot:run
```

### Email verification

To deliver login verification codes, configure an SMTP server using Spring Boot's mail settings and provide a sender address:

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-smtp-username
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

auth:
  email:
    senderAddress: no-reply@example.com
```

If no SMTP configuration is supplied, the application falls back to logging verification codes to the console.

## Docker & deployment

- `docker-compose.yml` now provisions a `mysql:8` container and injects the connection details into the backend service via `SPRING_DATASOURCE_*` environment variables. Update `.env` before running `docker compose up -d` to customise the database name, credentials, or choose the `h2` profile for local experiments.
- For managed database providers (AWS RDS, Azure Database for MySQL, etc.) set `SPRING_PROFILES_ACTIVE=mysql` and supply the managed endpoint credentials (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) through your host environment or secrets manager. Ensure the security group / firewall allows inbound traffic from the application subnet on port 3306 while keeping the instance closed to the public internet.

## Notes
- Greenhouse does not return `postedAt`; we stamp the current time. Extend `GreenhouseSourceClient` if you need more metadata.
- Lever timestamps are provided in epoch milliseconds and are normalised to `Instant`.
- Workday endpoints vary by tenant/site; ensure `baseUrl`, `tenant`, and `site` values match the organisation you are integrating.
- Job descriptions are stored in the `job_details` table and exposed via `GET /jobs/{id}/detail` so the frontend can lazy-load content.
