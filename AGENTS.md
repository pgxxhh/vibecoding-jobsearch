# Repository Guidelines

## Scope & Rule Priority
- This root file defines **global rules** that apply across the repo.
- **More specific AGENTS.md files take precedence** within their directory scopes:
  - `vibe-jobs-aggregator/AGENTS.md` for backend rules.
  - `vibe-jobs-view/AGENTS.md` for frontend rules.
- **Authoritative rule source** for backend is `vibe-jobs-aggregator/docs/rules.md`. Update that file first, then sync summaries here as needed.

## Quick Index
- Backend rules: `vibe-jobs-aggregator/AGENTS.md`
- Frontend rules: `vibe-jobs-view/AGENTS.md`
- Backend rule source of truth: `vibe-jobs-aggregator/docs/rules.md`
- Data-source documentation: `docs/DATA-SOURCES.md`
- Prompt library: `docs/ai-playbooks/`

## Project Structure & Module Organization (Global)
The workspace contains `vibe-jobs-aggregator` (Spring Boot 3, Java 17) and `vibe-jobs-view` (Next.js 14, TypeScript). Backend contexts live in `src/main/java/com/vibe/jobs/<context>/{domain,application,infrastructure,interfaces}` with migrations under `src/main/resources/db/migrations`. The frontend mirrors those boundaries: `app/(site)` for job seekers, `app/(admin)` for operations, `app/api` for backend proxies, and shared utilities in `src/modules`, `src/shared`, and `vibe-jobs-ui-pack`.

When reorganising backend layers, always keep HTTP entry points under the `interfaces` namespace (e.g. `interfaces.rest`, `interfaces.graphql`). A directory rename must be accompanied by matching `package` statement updates, adjusted imports/component scanning hints, and a repo-wide search to confirm no references to the old package remain before opening a PR.

## Build, Test, and Development Commands
Backend: `cd vibe-jobs-aggregator && mvn clean verify`; use `mvn spring-boot:run` for `:8080` or append `-DskipTests package` for fast builds. Frontend: `cd vibe-jobs-view && pnpm install`, then `pnpm dev` (`:3000`) or `pnpm build`. `docker compose up --build` spins MySQL, the Java API, the Next.js app, and Caddy for integrated testing.

## Documentation, Commits & PRs
Do not create stray Markdown—data-source changes belong in `docs/DATA-SOURCES.md`, other notes live in `docs/`. When UX or API contexts shift, update the relevant README and rules files. Commits use imperative or Conventional Commit subjects with wrapped bodies. Pull requests must describe the problem, approach, linked issues, required env vars, and UI evidence when applicable. Run `mvn clean verify`, `pnpm lint`, and `pnpm test` before requesting review.

## Compliance Rules
- If a required test cannot run, note it explicitly in the final response with the command and the blocking reason (dependency, environment limitation, or missing config).
- PR descriptions must include:
  - **Problem** (what/why)
  - **Approach** (how)
  - **Linked issues**
  - **Required env vars** (or “none”)
  - **UI evidence** (screenshots or “not applicable”)
