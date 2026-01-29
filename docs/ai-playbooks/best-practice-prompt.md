# Best Practice Template Prompt

Use this template to request AI assistance across the full delivery cycle. Replace bracketed fields.

---

## Role & Context
You are working in the repository `[repo-name]`. Follow all rules in `AGENTS.md` and any nested AGENTS.md files. Use domain boundaries exactly as defined. Do not skip validation or architecture constraints.

## Task
Implement `[feature/bug/refactor]` for `[module/area]`.

### Business Goal
- `[What outcome do we need?]`

### Non-Goals
- `[What should NOT be changed?]`

### Constraints & Dependencies
- `[APIs, schemas, migration rules, performance or security constraints]`
- `[External services or feature flags]`

### Acceptance Criteria
- `[List concrete behaviors / UI states / API responses]`

### Design & Impact Checklist
Please provide:
- Impacted modules and boundary checks.
- Data model changes (if any) and migration plan.
- API contracts and validation points.
- Frontend UX validation behavior.
- Risks and rollback strategy.

### Execution Plan
- Outline steps before coding.
- Call out any additional files or docs that must be updated.

### Testing Requirements
- Required tests: `[unit/integration/e2e]`.
- Commands to run (if possible): `[mvn clean verify]`, `[pnpm lint]`, `[pnpm test]`.
- If tests cannot run, explain why with exact command.

### Deliverables
- Summary of code changes with file references.
- Test results with commands and status.
- If UI changes, provide screenshot evidence.
- PR message: problem, approach, linked issues, required env vars, UI evidence.

---

## Output Format
1. Plan (short, numbered)
2. Changes (bullet list with file paths)
3. Tests (command + status)
4. Risks & Rollback
5. PR Draft (problem/approach/issues/env vars/UI evidence)
