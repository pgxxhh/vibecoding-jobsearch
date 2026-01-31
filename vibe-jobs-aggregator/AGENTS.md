# Backend Guidelines (vibe-jobs-aggregator)

## Scope & Rule Priority
- This file governs all files under `vibe-jobs-aggregator/`.
- The backend **source of truth** is `docs/rules.md`. Update it first, then sync summaries here.

## Architecture & Module Organization
Backend contexts live in `src/main/java/com/vibe/jobs/<context>/{domain,application,infrastructure,interfaces}` with migrations under `src/main/resources/db/migrations`.

- HTTP entry points **must** stay under `interfaces` namespaces (e.g., `interfaces.rest`, `interfaces.graphql`).
- If you rename a directory, also update `package` statements, imports/component scanning hints, and confirm no references to the old package remain before opening a PR.
- Domain code remains framework-free. Expose persistence via `<Aggregate>RepositoryPort` in `domain.spi` and implement adapters as `<Aggregate>RepositoryAdapter` in infrastructure.

## Data & Migrations
- Migrations must reside in `src/main/resources/db/migrations`.
- Naming: `V<increment>_<description>_<ddl|dml>.sql`.
- Ensure idempotency and include `create_time`, `update_time`, `deleted default false` for new tables.
- Avoid standalone indexes on soft-delete booleans (such as `deleted`). Always build composite indexes that match real predicates (e.g., `(status, deleted)` or `(run_id, deleted)`) to prevent low-selectivity index maintenance.

## Testing & Clean Up
- Backend tests sit in `src/test/java` as `*Test`.
- Reuse existing container fixtures and remove temporary files after execution.

## Validation Expectations
- Treat frontend checks as UX only; backend entry points must enforce request validation.
