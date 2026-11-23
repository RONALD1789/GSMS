# GSMS Implementation and Refactoring Plan

## Guiding rules

- Supabase is the only backend for Android and web.
- Refactor in small, reviewable steps.
- Preserve tenant isolation, role permissions and existing school data.
- Do not combine broad UI redesigns, schema changes and package reorganization in one unreviewed change.
- Never place privileged keys or provider secrets in client code.

## Phase 1 — Stabilize the Supabase foundation

- [x] Standardize client configuration around the Supabase URL and publishable key.
- [x] Route Android authentication and session restoration through Supabase Auth.
- [x] Route browser authentication and data access through Supabase.
- [x] Move AI provider requests behind an authenticated Supabase Edge Function.
- [x] Add role-aware tenant access checks and an additive RLS migration.
- [ ] Apply migrations to a disposable Supabase project and run the complete RLS test plan.
- [ ] Verify owner, administrator, headteacher, teacher, parent and student access using separate authenticated accounts.
- [ ] Confirm OAuth provider configuration and recovery flows.

## Phase 2 — Separate domain contracts from infrastructure

- Extract domain models, role/capability definitions and repository interfaces into domain packages.
- Keep model field names and serialization behavior stable unless a reviewed migration requires a change.
- Keep Supabase queries and response mapping inside data-access modules.
- Ensure UI components do not issue direct database requests.
- Add explicit error, loading, empty and retry states to every data workflow.

## Phase 3 — Complete the data layer

Create focused implementations for:
- Identity, user profiles and role assignments
- School, branch and academic-year administration
- Student and staff records
- Academics, attendance and report cards
- Finance, invoices and payments
- Leave and HR
- Communications and notifications
- Library, transport, procurement and inventory
- Alerts, audit logs and analytics

Every query and mutation must respect tenant context and server-side RLS. Use stable UUID relationships rather than matching records by display names.

## Phase 4 — Complete role-specific workflows

- Implement create, view, edit and archive actions where permitted.
- Enforce approval and publication transitions on the server.
- Restrict parent and student access to linked records.
- Restrict teachers to assigned classes and subjects.
- Protect finance, payroll, health, counselling and safeguarding data with dedicated permissions.
- Add audit events for sensitive operations.
- Test negative access cases as well as successful workflows.

## Phase 5 — Quality, release and operations

- Run Android unit, instrumentation and release builds.
- Run web lint, type checks and production build.
- Test migrations against a disposable Supabase project before production.
- Verify backup, restore, storage policies and Edge Function secrets.
- Configure production hosting and verify the deployed URL and authentication callbacks.
- Record release evidence and known limitations before marking a feature complete.

## Acceptance criteria

- Android and web use Supabase as their only backend.
- No Firebase SDK, configuration, service, rules or build plugin is part of the active application.
- Builds and automated tests pass.
- Cross-school and cross-branch access is denied unless explicitly assigned.
- Parent, student and teacher record scoping is enforced by RLS.
- Report-card approval and publication states are protected.
- No production data, credentials or signing material is committed.
- All remaining limitations are documented and assigned before production release.
