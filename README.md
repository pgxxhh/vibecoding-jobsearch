# Elaine Jobs · vibecoding-jobsearch

[Read this in Simplified Chinese 🇨🇳](README.zh-CN.md)

Elaine Jobs is a talent intelligence platform designed for users in Mainland China. It aggregates job postings from overseas companies through a unified ingestion pipeline and enriches the content with LLM-powered summaries so that openings are searchable, subscribable, and always up to date.

## Product tour

> _Note: The vector illustrations below are lightweight recreations of the UI so the documentation stays binary-free._

<img width="1359" height="906" alt="image" src="https://github.com/user-attachments/assets/7e3f876d-1cf1-4650-9204-aaa7a4d202df" />


- **Search experience** – The landing page lets talent discover curated openings with natural-language filters, quick tag selectors (location, job type, tech stack), and instant results.
- **Rich job detail** – AI-generated highlights and structured metadata surface the most relevant information alongside the original description so candidates can act quickly.

<img width="1305" height="896" alt="image" src="https://github.com/user-attachments/assets/b86151fc-7a07-4d24-a96d-89188d0e5f75" />


- **Ingestion controls** – Operations teams can fine-tune crawler cadence, request budgets, and retry policies directly in the admin console to match business requirements.

<img width="1321" height="898" alt="image" src="https://github.com/user-attachments/assets/0204f79c-adcc-40f8-af2e-4d855e3be49d" />


- **Source management** – Toggle ATS connectors or custom crawlers with a single click and monitor their status to ensure feeds stay fresh.
- **Crawler blueprint autogen** – Paste an entry URL and optional keywords into the admin wizard and let the backend run Playwright to infer selectors, produce a draft configuration, and register the data source after review.


<img width="1299" height="850" alt="image" src="https://github.com/user-attachments/assets/886c38ca-d416-4e0d-b824-650dc0a51499" />



## Table of contents
- [System overview](#system-overview)
- [Architecture](#architecture)
- [Ingestion & processing pipeline](#ingestion--processing-pipeline)
- [LLM enrichment loop](#llm-enrichment-loop)
- [Front-end & back-end collaboration](#front-end--back-end-collaboration)
- [Technology stack](#technology-stack)
- [Deployment & operations](#deployment--operations)
- [Local development](#local-development)
- [Repository structure](#repository-structure)
- [Roadmap](#roadmap)

## System overview
- **Target audience**: Job seekers in Mainland China who want quick access to overseas openings without requiring a VPN.
- **Core value**: Normalises data from major ATS platforms and official career sites, deduplicates the content, and enriches it with AI-generated highlights for a consistent job search experience.
- **Production environment**: https://elainejobs.com/ (hosted on AWS EC2 with Amazon Aurora MySQL 8.0-compatible database).

## Architecture
| Layer | Description | Key location |
| --- | --- | --- |
| Presentation | Next.js front-end for search, browsing, subscriptions, and the admin console. | [`vibe-jobs-view/`](./vibe-jobs-view) |
| Application | Spring Boot service responsible for ingestion, scheduling, persistence, search APIs, and admin configuration. | [`vibe-jobs-aggregator/`](./vibe-jobs-aggregator) |
| Data | Aurora/MySQL with Flyway migrations, plus service-managed caching and task scheduling. | `Aurora / RDS` |
| Edge | Caddy reverse proxy and Docker Compose stack that exposes a unified domain and routing rules. | [`Caddyfile`](./Caddyfile), [`docker-compose.yml`](./docker-compose.yml), [`docker/`](./docker) |

## Ingestion & processing pipeline
1. **Source configuration**
   - Supports major ATS providers such as `ashby`, `greenhouse`, `lever`, `recruitee`, `smartrecruiters`, and `workday`.
   - Admin UI can enable/disable sources, toggle modes (incremental vs. company-specific), adjust pagination, concurrency, and initial delay.
   - The crawler blueprint generator accepts entry URLs, keywords, and exclusion selectors, then spawns a Playwright run to propose a draft JSON blueprint plus validation report.
2. **Scheduling**
   - Spring scheduler triggers ingestion jobs based on configuration, supporting both fixed-delay and custom start times.
   - Each run resolves the company list or incremental cursor to avoid redundant fetches.
3. **Fetching strategy**
   - **HTTP/API first**: Integrates with public APIs or JSON endpoints when available.
   - **Dynamic fallback**: Switches to Playwright-powered browser crawling for sources that require client-side rendering.
4. **Parsing & normalisation**
   - Jsoup/JSON parsers convert responses into a unified `Job` model (title, location, salary, tags, etc.).
   - Deduplication (external ID + content fingerprint) ensures each job only appears once.
5. **Detail enrichment**
   - Supplementary detail fetches store rich content in the `job_details` table to power LLM enrichment.
6. **Persistence & indexing**
   - Spring Data JPA writes jobs into MySQL; Flyway keeps schemas aligned with code.
   - Key fields (company, location, tags) are indexed to speed up search queries.

## LLM enrichment loop
1. **Trigger**: When job detail content changes, a `JobDetailContentUpdatedEvent` is published.
2. **Async call**: Post-transaction listeners invoke `JobContentEnrichmentClient`, sending requests to a configurable LLM provider (e.g. OpenAI).
3. **Structured storage**: Results are persisted as `JobDetailEnrichment` records with generic key-value JSON payloads (`summary`, `skills`, `highlights`, `status`, etc.).
4. **Idempotency & versioning**: The service validates `contentVersion` and `sourceFingerprint` before saving to prevent stale data overwriting new results; failures are recorded and retried.
5. **Front-end consumption**: APIs expose the `enrichments` array so the UI can render AI output or gracefully fall back to the raw description.

## Front-end & back-end collaboration
- **Search & filters**: Front-end queries `/backend-api/jobs/search` (example path) with keyword, location, company filters, etc.
- **Job detail**: Detail pages call `/backend-api/jobs/{id}/detail` to retrieve LLM-generated summaries alongside original HTML.
- **Subscriptions**: Users submit subscription criteria via `/backend-api/subscriptions`; email delivery is planned for future iterations.
- **Admin console**: Shares the same APIs to manage scheduling options, source catalogs, task status, and crawl logs.

## Technology stack
- **Front-end**: Next.js (App Router), TypeScript, React, Tailwind CSS, Zustand, SWR.
- **Back-end**: Spring Boot 3, Java 17, Spring WebFlux/WebMVC, Spring Data JPA, Flyway, Jackson, Jsoup, Playwright.
- **Infrastructure**: Docker, Docker Compose, Caddy, AWS EC2, Amazon Aurora (MySQL 8.0).
- **Tooling**: Maven Wrapper, PNPM, GitHub Actions (optional CI/CD).

## Deployment & operations
- **CI/CD**: GitHub Actions pipelines build, test, and deploy both front-end and back-end on each push/PR. See [.github/workflows/](./.github/workflows/).
- **Containerisation**: [`docker-compose.yml`](./docker-compose.yml) orchestrates front-end, back-end, and Caddy containers; `docker/` stores Dockerfiles.
- **Reverse proxy**: Caddy routes `/backend-api/*` to the back-end (8080) and `/api/*` to the front-end (3000), while exposing Swagger/Actuator endpoints.
- **Database migrations**: Flyway runs automatically on service startup to keep schemas in sync.
- **Monitoring & throttling**: Scheduler settings allow tuning concurrency and retries; follow source site policies and robots.txt guidance.

## Local development
### Back-end
```bash
cd vibe-jobs-aggregator
./mvnw spring-boot:run
```
Configure MySQL 8.0 (or H2) with environment variables:
- `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/elaine_jobs?useSSL=false&serverTimezone=UTC`
- `SPRING_DATASOURCE_USERNAME=...`
- `SPRING_DATASOURCE_PASSWORD=...`

### Front-end
```bash
cd vibe-jobs-view
pnpm install
NEXT_PUBLIC_BACKEND_BASE=http://localhost:8080 pnpm dev
```

## Repository structure
```
.
├── Caddyfile                    # Reverse proxy & routing rules
├── docker-compose.yml           # Docker Compose stack (front-end + back-end + Caddy)
├── docker/                      # Service Dockerfiles & deployment scripts
├── deploy.sh                    # Example deployment script
├── vibe-jobs-aggregator/        # Back-end: Spring Boot ingestion + LLM enrichment
│   ├── pom.xml
│   └── src/...
└── vibe-jobs-view/              # Front-end: Next.js (App Router)
    └── app/page.tsx
```

## Roadmap
- [ ] Expand ATS connectors and official career site coverage.
- [ ] Introduce semantic/embedding search for better relevance.
- [ ] Complete subscription & notification channels (email, push, webhooks).
- [ ] Build ingestion monitoring and alerting (failure rates, rate limits, LLM metrics).
