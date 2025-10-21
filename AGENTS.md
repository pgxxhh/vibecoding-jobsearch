# Repository Guidelines

## Project Structure & Module Organization
The workspace contains `vibe-jobs-aggregator` (Spring Boot 3, Java 17) and `vibe-jobs-view` (Next.js 14, TypeScript). Backend contexts live in `src/main/java/com/vibe/jobs/<context>/{domain,application,infrastructure,interfaces}` with migrations under `src/main/resources/db/migrations`. The frontend mirrors those boundaries: `app/(site)` for job seekers, `app/(admin)` for operations, `app/api` for backend proxies, and shared utilities in `src/modules`, `src/shared`, and `vibe-jobs-ui-pack`.

When reorganising backend layers, always keep HTTP entry points under the `interfaces` namespace (e.g. `interfaces.rest`, `interfaces.graphql`). A directory rename must be accompanied by matching `package` statement updates, adjusted imports/component scanning hints, and a repo-wide search to confirm no references to the old package remain before opening a PR.

## Build, Test, and Development Commands
Backend: `cd vibe-jobs-aggregator && mvn clean verify`; use `mvn spring-boot:run` for `:8080` or append `-DskipTests package` for fast builds. Frontend: `cd vibe-jobs-view && pnpm install`, then `pnpm dev` (`:3000`) or `pnpm build`. `docker compose up --build` spins MySQL, the Java API, the Next.js app, and Caddy for integrated testing.

## Backend Data & Architecture Rules
Migrations must reside in `db/migrations` and follow `V<increment>_<description>_<ddl|dml>.sql`; ensure they are idempotent and that new tables include `create_time`, `update_time`, `deleted default false`. Prefer configuration over hard-coded fixes, and default to reuse and clarity when extending services. Domain code stays framework-free: expose persistence via `<Aggregate>RepositoryPort` in `domain.spi` and implement adapters as `<Aggregate>RepositoryAdapter` in infrastructure.

## Frontend Context Rules
Respect context boundaries—pages remain in their `(site)` or `(admin)` scopes, while cross-cutting logic belongs in `lib/domain`, `lib/application`, or `lib/infrastructure`. Browser code never targets the Java host directly; route through `app/api/*` and helper clients like `createBackendClient`. UI components use PascalCase, hooks use `useCamelCase`, and shared components depend only on domain types with stateful work handled in hooks.

## Validation Expectations
Frontends must perform basic request validation (required fields, formats) and surface clear toasts or inline errors. Every corresponding backend entry point needs a matching guard so unvalidated payloads cannot slip through; treat frontend checks as UX and backend checks as enforcement.

## Testing & Clean Up
Backend tests sit in `src/test/java` as `*Test`; reuse existing container fixtures and remove temporary files after execution. Frontend tests live beside the subject or in `__tests__`, named `*.test.ts(x)` and driven by Jest + Testing Library. Assert on rendered text or ARIA roles instead of snapshots, and confine React Query logic to application-layer hooks for easy stubbing.

## Documentation, Commits & PRs
Do not create stray Markdown—data-source changes belong in `docs/DATA-SOURCES.md`, other notes live in `docs/`. When UX or API contexts shift, update the relevant README and rules files. Commits use imperative or Conventional Commit subjects with wrapped bodies. Pull requests must describe the problem, approach, linked issues, required env vars, and UI evidence when applicable. Run `mvn clean verify`, `pnpm lint`, and `pnpm test` before requesting review.
