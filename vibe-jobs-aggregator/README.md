# Vibe Jobs Aggregator — Configurable Sources

Spring Boot service that ingests external job boards on a schedule and upserts them into the local database.

## 🌟 Supported Data Sources

- **Workday** — `/wday/cxs/{tenant}/{site}/jobs` (POST, supports facets & Chinese content)
- **Greenhouse** — `https://boards.greenhouse.io/{org}.json`
- **Ashby ATS** — Modern tech companies (Notion, Figma, Linear, etc.)
- **Amazon Jobs API** — Official APAC feed from `https://www.amazon.jobs/en/search.json`
- **Generic ATS (Moka/Beisen)** — Unified client covering Moka智聘、北森Beisen 等中国本土 ATS（现已为小红书、知乎、快手、美团、PingCAP 启用）
- **Crawler Blueprints** — Configurable crawler domain that renders career sites via HTTP/Playwright-like flows and parses them with reusable parser templates.

Each connector implements `SourceClient` and is wired through a factory so new providers can be added with minimal code.

📖 **[完整数据源配置指南 →](DATA-SOURCES.md)**

## ⚡ Quick Start

### Configuration
Edit `src/main/resources/application.yml` under the `ingestion` section:

```yaml
ingestion:
  fixedDelayMs: 180000    # 3 minutes interval
  initialDelayMs: 10000   # 10 seconds startup delay
  pageSize: 100           # Jobs per page
  concurrency: 6          # Parallel threads
  mode: companies         # Filter against the curated list below
  companies:
    - "binance"
    - "okx"
    - "grab"
    - "stripe"
    - "notion"
  recentDays: 14

  # 🇨🇳 China-Optimized Filtering (Location + Role)
  locationFilter:
    enabled: true
    includeCities:
      - "beijing" / "北京"
      - "shanghai" / "上海"
      - "shenzhen" / "深圳"
      - "guangzhou" / "广州"
      - "hong kong" / "香港"
      - "singapore"
    includeKeywords:
      - "china" / "中国"
      - "apac"
      - "greater china" / "大中华"
  roleFilter:
    enabled: true
    includeKeywords:
      - "financial" / "财务"2
      - "analyst" / "分析师"
      - "investment" / "投资"
      - "engineer" / "工程师"
      - "software" / "软件"
    excludeKeywords:
      - "intern" / "实习"
      - "campus" / "校园"
```

### Running Locally
```bash
mvn spring-boot:run
```

### Docker Deployment
```bash
# Start with MySQL
docker compose up -d

# Or with custom environment
./deploy.sh
```

## 🎯 China Market Optimization

This system is optimised for **financial analyst & engineering roles across Mainland China and Greater China**:

✅ **Dual filters** — Location + role keyword filters keep only China/APAC finance & engineering roles.
✅ **Curated connectors** — Workday, Greenhouse, Ashby and Amazon feeds preconfigured for 30+ fintech & tech companies with Mainland teams.
✅ **Mainland ATS coverage** — Generic Moka / Beisen 客户端连接小红书、知乎、快手、美团、PingCAP 等中国公司的人才系统。
✅ **Bilingual keywords** — Chinese + English synonyms for major job families (财务分析师 / Financial Analyst, 软件工程师 / Software Engineer, etc.).
✅ **Major cities** — Beijing, Shanghai, Shenzhen, Guangzhou, Hangzhou, Hong Kong, Singapore and more.

**Expected Output**: 2000+ China-focused engineer & financial openings (sampled from 35+ organisations).

## 📊 Data Source Priority

| Priority | Source | Status | Est. Jobs | Features |
|----------|--------|--------|-----------|----------|
| 🥇 P1 | **Workday** | ✅ Active | 900+ | Faceted search, strong APAC coverage |
| 🥈 P2 | **Greenhouse** | ✅ Active | 800+ | Stable JSON API |
| 🥉 P3 | **Ashby** | ✅ Active | 400+ | Modern tech companies |
| 🏆 P4 | **Amazon Jobs API** | ✅ Active | 300+ | Official APAC feed, finance & engineering search |
| 🆕 P5 | **Generic ATS** | ✅ Active | 1500+ | Moka, Beisen (Mainland-focused connectors) |

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

## 🕸️ Crawler Blueprints

The new `crawler` source type treats each career site as a standalone blueprint. A blueprint describes:

- **Entry point** — base URL or HTTP request template, plus paging rules (query, offset, or path-based).
- **Flow DSL** — optional steps such as `REQUEST`, `WAIT`, `SCROLL`, `EXTRACT_LIST`, etc. for JavaScript-heavy pages.
- **Parser profile** — reusable templates (`crawler_parser_template` table) define CSS selectors or attributes for `title`, `url`, `externalId`, `location`, tags, and details.
- **Rate limit & concurrency** — per-blueprint restrictions to respect robots.txt and throttle sessions.

Blueprint metadata is persisted in four new tables managed by Flyway migration `V7__crawler_tables.sql`:

- `crawler_blueprint`
- `crawler_parser_template`
- `crawler_run_log`
- `crawler_cache`

### Configuring a crawler source

Add a data source with `type: crawler` and point it to a blueprint code:

```yaml
code: example-crawler
type: crawler
enabled: true
baseOptions:
  blueprintCode: example-careers
  entryUrl: https://example.com/careers
  sourceName: crawler:example
companies:
  - displayName: Example Company
    reference: example
    overrideOptions:
      entryUrl: https://example.com/careers?location=shanghai
categories:
  - name: engineering
    limit: 120
    tags: [engineering, software]
```

When the scheduler resolves this source, `SourceClientFactory` instantiates a `CrawlerSourceClient`, which delegates to `CrawlerOrchestrator`. The orchestrator loads the blueprint, spins up an execution session via `HttpCrawlerExecutionEngine`, parses the HTML with `DefaultCrawlerParserEngine`, converts the results into `FetchedJob`, and records metrics to `crawler_run_log`.

Blueprint parser templates can be reused across companies. Updating `crawler_blueprint.config_json` or the referenced template allows hot swaps without redeploying the application.

## Running locally
```
mvn spring-boot:run
```

`CareersApiStartupRunner` logs the number of jobs fetched from each source marked `runOnStartup=true`. The `JobIngestionScheduler` reuses the same configuration for recurring updates.

## Database profiles

The service ships with dedicated Spring profiles so you can switch between the embedded H2 database and MySQL without editing configuration files:

- `application-mysql.yml` — activates when `SPRING_PROFILES_ACTIVE=mysql` (default). It expects a MySQL 8+ instance and defaults to validating the schema on startup. **Credentials must be provided via environment variables or encrypted values — no plaintext username/password are checked into the repo.**
- `application-h2.yml` — activates when `SPRING_PROFILES_ACTIVE=h2`. Stores data on the local filesystem and enables the H2 console for quick development feedback.

Both profiles honour the same environment overrides: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO`, and `SPRING_JPA_DATABASE_PLATFORM`. When the `prod` profile is active and `db.credentials.encryption.enabled=true`, datasource credentials must be wrapped as `ENC(<ciphertext>)` values. The bundled `CredentialEncryptionTool` can generate the AES key and ciphertext wrappers:

```bash
# 1) Generate a key once and store it securely (e.g. /opt/vibejobs/secrets/db-aes.key)
mvn -pl vibe-jobs-aggregator -q exec:java \
  -Dexec.mainClass=com.vibe.jobs.security.CredentialEncryptionTool \
  -Dexec.args="generate-key" > db-aes.key

# 2) Encrypt credentials; the tool outputs ENC(<ciphertext>) ready for application.yml
mvn -pl vibe-jobs-aggregator -q exec:java \
  -Dexec.mainClass=com.vibe.jobs.security.CredentialEncryptionTool \
  -Dexec.args="encrypt /opt/vibejobs/secrets/db-aes.key prod_db_username"

mvn -pl vibe-jobs-aggregator -q exec:java \
  -Dexec.mainClass=com.vibe.jobs.security.CredentialEncryptionTool \
  -Dexec.args="encrypt /opt/vibejobs/secrets/db-aes.key prod_db_password"

# 3) Provide the wrapped values via SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
export SPRING_DATASOURCE_USERNAME='ENC(...)'
export SPRING_DATASOURCE_PASSWORD='ENC(...)'

# 4) Start the service
mvn spring-boot:run
```

### Email verification

To deliver login verification codes, configure an SMTP server using Spring Boot's mail settings and provide a sender address:

```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME}
    password: ${SPRING_MAIL_PASSWORD}
    properties:
      mail.smtp.auth: ${SPRING_MAIL_SMTP_AUTH:true}
      mail.smtp.starttls.enable: ${SPRING_MAIL_SMTP_STARTTLS:true}

auth:
  email:
    fromAddress: ${AUTH_EMAIL_FROM:no-reply@example.com}
    sender: ${AUTH_EMAIL_SENDER:smtp}
```

If no SMTP configuration is supplied, the application falls back to logging verification codes to the console.

## Docker & deployment

- `docker-compose.yml` now provisions a `mysql:8` container and injects the connection details into the backend service via `SPRING_DATASOURCE_*` environment variables. Update `.env` before running `docker compose up -d` to customise the database name, credentials, or choose the `h2` profile for local experiments.
- For managed database providers (AWS RDS, Azure Database for MySQL, etc.) set `SPRING_PROFILES_ACTIVE=mysql` and supply the managed endpoint credentials (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) through your host environment or secrets manager. Ensure the security group / firewall allows inbound traffic from the application subnet on port 3306 while keeping the instance closed to the public internet.
- A production checklist for Aurora/RDS lives in `docs/production-rds-checklist.md`. Update `.env.production` with the managed database secrets, and `deploy.sh` will automatically export the variables before rebuilding the containers.
- **Database credential encryption (prod profile):** production now expects the database username and password to be AES-encrypted. Generate a Base64 key once (`mvn -pl vibe-jobs-aggregator -q exec:java -Dexec.mainClass=com.vibe.jobs.security.CredentialEncryptionTool -Dexec.args="generate-key" > db-aes.key`), copy the resulting file to the server (default path `/opt/vibejobs/secrets/db-aes.key`), and encrypt each secret (`mvn -pl vibe-jobs-aggregator -q exec:java -Dexec.mainClass=com.vibe.jobs.security.CredentialEncryptionTool -Dexec.args="encrypt /opt/vibejobs/secrets/db-aes.key my-db-password"`). Save the encrypted strings in your environment variables (`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) and supply `DB_AES_KEY_PATH` if you use a custom location.
- **Public API rate limiting:** anonymous access to `/api/jobs/**` is capped at 20 requests per minute per IP/user in production. Adjust `security.rate-limiter.*` properties if you need to increase or decrease the throttle.

## Notes
- Greenhouse does not return `postedAt`; we stamp the current time. Extend `GreenhouseSourceClient` if you need more metadata.
- Lever timestamps are provided in epoch milliseconds and are normalised to `Instant`.
- Workday endpoints vary by tenant/site; ensure `baseUrl`, `tenant`, and `site` values match the organisation you are integrating.
- Job descriptions are stored in the `job_details` table and exposed via `GET /jobs/{id}/detail` so the frontend can lazy-load content.

## Migrating from H2 to MySQL with Flyway

`spring-boot-starter-data-jpa` now ships with Flyway migrations. When the backend starts with the `mysql` profile (`SPRING_PROFILES_ACTIVE=mysql,prod`), Flyway runs automatically before Hibernate initialises and validates the schema.

### 1. Back up the embedded H2 database
- Stop the running backend container or application.
- Copy the file-based H2 database to a safe location, e.g. `cp vibe-jobs-aggregator/data/jobsdb.mv.db /backups/jobsdb-$(date +%Y%m%d).mv.db` or `docker cp backend:/app/data/jobsdb.mv.db ./jobsdb-backup.mv.db` if you are using the Compose stack.

### 2. Export data from H2 (optional seed step)
If you plan to migrate existing data, export each table to CSV using the H2 console (`/api/h2-console`) or the CLI:

```sql
CALL CSVWRITE('/tmp/jobs.csv', 'SELECT * FROM JOBS');
CALL CSVWRITE('/tmp/job_details.csv', 'SELECT * FROM JOB_DETAILS');
CALL CSVWRITE('/tmp/job_tags.csv', 'SELECT * FROM JOB_TAGS');
CALL CSVWRITE('/tmp/auth_user.csv', 'SELECT * FROM AUTH_USER');
CALL CSVWRITE('/tmp/auth_login_challenge.csv', 'SELECT * FROM AUTH_LOGIN_CHALLENGE');
CALL CSVWRITE('/tmp/auth_session.csv', 'SELECT * FROM AUTH_SESSION');
```

Alternatively, run `scripts/h2-to-mysql.sh` to generate a MySQL-compatible schema file (`scripts/h2-schema-mysql.sql`). Pass `WIPE_H2=true` (or answer `y` to the prompt) to remove the old H2 files once the schema has been exported.

### 3. Prepare the MySQL schema
- Create a database using the same name you intend to pass to `spring.flyway.schemas` (defaults to `vibejobs`):

```sql
CREATE DATABASE vibejobs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- Configure the backend environment (for example in `.env` or the deployment pipeline):
  - `SPRING_PROFILES_ACTIVE=mysql,prod`
  - `DB_URL=jdbc:mysql://<host>:3306/vibejobs`
  - `DB_USER` / `DB_PASSWORD`
  - `SPRING_FLYWAY_SCHEMAS=vibejobs` (or your chosen database name)
  - Optional: `SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver`

### 4. Run Flyway migrations
- Start the backend once the environment variables are in place. The Flyway dependency creates the schema using `db/migration/V1__init.sql` before the application finishes booting:

```bash
SPRING_PROFILES_ACTIVE=mysql,prod DB_URL=jdbc:mysql://localhost:3306/vibejobs \
  DB_USER=vibejobs DB_PASSWORD=vibejobs docker compose up -d backend
```

- Check `flyway_schema_history` in MySQL to confirm version `1` is applied.

### 5. Import legacy data with `LOAD DATA`
After Flyway has created the tables, you can replay the CSV export. MySQL’s `LOAD DATA` works well and keeps the script idempotent—you can run it on an empty database or re-run it after truncating tables.

```sql
SET FOREIGN_KEY_CHECKS = 0;
LOAD DATA LOCAL INFILE '/tmp/jobs.csv'
INTO TABLE jobs
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, source, external_id, title, company, location, level, @posted_at, url, @created_at, @updated_at, checksum)
SET posted_at = CASE WHEN @posted_at = '' THEN NULL ELSE STR_TO_DATE(@posted_at, '%Y-%m-%d %H:%i:%s.%f') END,
    created_at = STR_TO_DATE(@created_at, '%Y-%m-%d %H:%i:%s.%f'),
    updated_at = STR_TO_DATE(@updated_at, '%Y-%m-%d %H:%i:%s.%f');

LOAD DATA LOCAL INFILE '/tmp/job_details.csv'
INTO TABLE job_details
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, job_id, content, @created_at, @updated_at)
SET created_at = STR_TO_DATE(@created_at, '%Y-%m-%d %H:%i:%s.%f'),
    updated_at = STR_TO_DATE(@updated_at, '%Y-%m-%d %H:%i:%s.%f');

LOAD DATA LOCAL INFILE '/tmp/job_tags.csv'
INTO TABLE job_tags
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(job_id, tag);

LOAD DATA LOCAL INFILE '/tmp/auth_user.csv'
INTO TABLE auth_user
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, email, @created_at, @updated_at, @last_login_at)
SET id = UNHEX(REPLACE(@id, '-', '')),
    created_at = STR_TO_DATE(@created_at, '%Y-%m-%d %H:%i:%s.%f'),
    updated_at = STR_TO_DATE(@updated_at, '%Y-%m-%d %H:%i:%s.%f'),
    last_login_at = CASE WHEN @last_login_at = '' THEN NULL ELSE STR_TO_DATE(@last_login_at, '%Y-%m-%d %H:%i:%s.%f') END;

LOAD DATA LOCAL INFILE '/tmp/auth_login_challenge.csv'
INTO TABLE auth_login_challenge
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, email, code_hash, @expires_at, @last_sent_at, @verified, attempts, @created_at, @updated_at)
SET id = UNHEX(REPLACE(@id, '-', '')),
    expires_at = STR_TO_DATE(@expires_at, '%Y-%m-%d %H:%i:%s.%f'),
    last_sent_at = STR_TO_DATE(@last_sent_at, '%Y-%m-%d %H:%i:%s.%f'),
    verified = IF(@verified IN ('1', 'TRUE', 'true'), b'1', b'0'),
    created_at = STR_TO_DATE(@created_at, '%Y-%m-%d %H:%i:%s.%f'),
    updated_at = STR_TO_DATE(@updated_at, '%Y-%m-%d %H:%i:%s.%f');

LOAD DATA LOCAL INFILE '/tmp/auth_session.csv'
INTO TABLE auth_session
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, @user_id, token_hash, @expires_at, @created_at, @revoked_at)
SET id = UNHEX(REPLACE(@id, '-', '')),
    user_id = UNHEX(REPLACE(@user_id, '-', '')),
    expires_at = STR_TO_DATE(@expires_at, '%Y-%m-%d %H:%i:%s.%f'),
    created_at = STR_TO_DATE(@created_at, '%Y-%m-%d %H:%i:%s.%f'),
    revoked_at = CASE WHEN @revoked_at = '' THEN NULL ELSE STR_TO_DATE(@revoked_at, '%Y-%m-%d %H:%i:%s.%f') END;
SET FOREIGN_KEY_CHECKS = 1;
```

If you create a reusable seed export, place it under `src/main/resources/db/migration/V2__seed_from_export.sql` so Flyway can manage it; otherwise keep the CSV workflow documented here.

### 6. Verify the migration
- Compare record counts between H2 and MySQL for each table (`SELECT COUNT(*) FROM jobs`, etc.).
- Hit health endpoints (`/actuator/health`, `/jobs`) to ensure the application starts normally with `spring.jpa.hibernate.ddl-auto=validate`.
- Inspect the Flyway history table to confirm subsequent deployments will continue from version 1.

## Database indexes & query tuning

- Flyway manages production indexes under `src/main/resources/db/migration`. New composite indexes cover the most common filters:
  - `idx_jobs_posted_at_id_desc` to fetch the newest jobs first while avoiding filesorts.
  - Functional indexes on `LOWER(company)` and `LOWER(location)` combined with `posted_at` to accelerate case-insensitive filters plus recency sorting. MySQL requires version **8.0.13+** for expression indexes.
  - `idx_jobs_level` for exact level matches and two supporting indexes on `job_tags` (`tag` and `(job_id, tag)`) to speed up tag joins.
- For future text search needs, prefer MySQL 8.0 `FULLTEXT` indexes on `title` / `description` or an external search service (e.g. OpenSearch). MySQL `FULLTEXT` indexes only work on InnoDB tables starting in 5.6; configure them via a dedicated Flyway migration so lower environments stay in sync.
- Verify that queries benefit from the indexes with `EXPLAIN`, e.g.:

  ```sql
  EXPLAIN SELECT id, title FROM jobs
  WHERE LOWER(company) = LOWER('Acme')
  ORDER BY posted_at DESC, id DESC
  LIMIT 20;
  ```

  Inspect the `key`/`key_len` columns to confirm index usage. Combine with `ANALYZE TABLE jobs;` after large data imports so the optimizer statistics stay fresh.
- When deploying to new environments, baseline the existing schema (`spring.flyway.baseline-on-migrate=true`) before applying migrations to avoid checksum conflicts. Hibernate's `ddl-auto` should be set to `validate` or `none` in production; schema changes—including new indexes—must be introduced through Flyway scripts.
