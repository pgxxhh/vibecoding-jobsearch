# Technical Design Template

> Usage: copy this template before drafting a new design. Keep the document in English and ensure it complies with the root `AGENTS.md` plus any nested rules.

## 1. Overview
- **Document title**: `<feature / initiative>`
- **Author**: `<owner>`
- **Reviewers**: `<reviewer list>`
- **Revision log**: `<date + change summary>`
- **Scope**: `<Backend | Frontend | Full-stack>`
- **Related requirement / issue**: `<link>`

## 2. Background & Goals
### 2.1 Problem Statement
- Current state / pain: `<business or technical gap>`
- Trigger: `<OKR / incident / customer feedback>`

### 2.2 Success Metrics
- KPIs (functional, performance, cost, etc.): `<quantitative targets>`
- Non-goals (out of scope): `<explicit list>`

## 3. Constraints, Assumptions & Dependencies
- DDD boundaries / contexts: `<com.vibe.jobs.<context> or app/(site) modules>`
- Technical assumptions: `<language, framework, protocol limits>`
- External dependencies / services: `<third-party APIs, queues, schedulers>`
- Compliance / security / privacy prerequisites: `<data residency, access control>`

## 4. Solution Overview
### 4.1 Architecture Sketch
- Provide a `mermaid` diagram or textual flow showing interface directions, data flow, failure handling, and resiliency measures.

### 4.2 Option Comparison (if applicable)
| Option | Description | Pros | Risks / When to use |
| --- | --- | --- | --- |

- Recommended option: `<choice + rationale>`

## 5. Detailed Design
### 5.1 Backend (`vibe-jobs-aggregator`)
- Domain & application layer: `<new aggregates, application services, domain.spi ports>`
- Interface layer: `<interfaces.rest/graphql controllers, DTOs, validation>`
- Infrastructure: `<RepositoryAdapter implementations, external clients>`
- Data model:
  - Tables / views / indexes: `<names, columns, types, include create_time/update_time/deleted>`
  - Migration strategy: `<V*_ddl.sql naming, idempotency, soft-delete composite index requirement>`
- Transactions & consistency: `<isolation level, retry policies>`

### 5.2 Frontend (`vibe-jobs-view`)
- Routes & modules: `<app/(site)|(admin)|api structure>`
- State & data fetching: `<React Server/Client components, caching strategy>`
- UI / accessibility: `<component library, form validation, error handling>`

### 5.3 Integration & Workflow
- Scheduling / messaging: `<cron, queues, idempotency keys>`
- External system interfaces: `<contracts, rate limits, retries>`

### 5.4 Observability
- Logging: `<structured fields, sampling>`
- Metrics: `<Prometheus/StatsD metric names>`
- Tracing: `<span naming and propagation>`

## 6. Security & Compliance
- Authorization model: `<RBAC, API auth>`
- Data protection: `<encryption at rest/in transit, masking>`
- Privacy: `<PII handling, retention policy>`

## 7. Test & Validation Plan
- Unit tests: `<package, critical cases>`
- Integration / contract / E2E: `<required containers, mocks>`
- Verification commands: `mvn clean verify`, `pnpm lint`, `pnpm test` (note blockers if they cannot run).

## 8. Delivery & Release
- Work breakdown & milestones: `<task split, target dates>`
- Feature flags / configuration: `<flag strategy, defaults>`
- Rollback strategy: `<DB rollback, gradual rollout, traffic shifting>`

## 9. Risks & Mitigations
| Risk | Impact | Likelihood | Mitigation / Monitoring |
| --- | --- | --- | --- |

## 10. Open Questions & TODOs
- `<item / owner / due date>`

## 11. Appendix
- Reference links (PRD, tickets, related docs).
- Glossary, data samples, API contract snippets, etc.
