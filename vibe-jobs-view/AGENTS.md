# Frontend Guidelines (vibe-jobs-view)

## Scope & Rule Priority
- This file governs all files under `vibe-jobs-view/`.
- For shared/global requirements, follow the repository root `AGENTS.md`.

## Frontend Context Rules
- Respect context boundaries—pages remain in their `(site)` or `(admin)` scopes, while cross-cutting logic belongs in `lib/domain`, `lib/application`, or `lib/infrastructure`.
- Browser code never targets the Java host directly; route through `app/api/*` and helper clients like `createBackendClient`.
- UI components use PascalCase, hooks use `useCamelCase`.
- Shared components depend only on domain types with stateful work handled in hooks.

## Validation Expectations
- Perform basic request validation (required fields, formats) and surface clear toasts or inline errors.
- Backend enforcement is mandatory; frontend validation is for UX.

## Testing & Clean Up
- Frontend tests live beside the subject or in `__tests__`, named `*.test.ts(x)` and driven by Jest + Testing Library.
- Assert on rendered text or ARIA roles instead of snapshots.
- Confine React Query logic to application-layer hooks for easy stubbing.
