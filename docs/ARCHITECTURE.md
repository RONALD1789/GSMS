# GSMS Architecture

## Product architecture

GSMS is a multi-school school-management platform with:
- Android application: Kotlin and Jetpack Compose.
- Browser application: React, TypeScript and Vite.
- Backend target: Supabase only (Auth, PostgreSQL, Row Level Security, Storage, Realtime and Edge Functions).

The Android and web clients must use the same Supabase tenant, identity and authorization model. Firebase is not part of the target architecture. Any remaining Firebase code or configuration in the default branch must be treated as migration work, not as an approved backend dependency.

## Repository layout

```text
app/                         # Android application
  src/main/java/com/example/
    data/                    # Data access and Supabase integration
    model/                   # Domain and UI-facing models
    ui/                      # Compose screens, ViewModels and portal UI
web/ or src/                 # Browser application (React + TypeScript + Vite)
supabase/
  schema.sql                 # Existing baseline schema (review before changes)
  migrations/                # Additive, ordered database migrations
  functions/                 # Trusted Edge Functions
docs/
  PORTAL_FEATURE_SPECIFICATION.md
  ARCHITECTURE.md
```

This is a logical map. Keep the actual existing project paths unless a deliberate, tested refactor is approved.

## Backend responsibilities

- **Supabase Auth:** identity, sign-in, recovery and OAuth.
- **PostgreSQL:** authoritative tenant, student, staff, academic, finance and operational records.
- **RLS:** server-enforced tenant, branch, role, assigned-class and linked-student authorization.
- **Storage:** private school documents, report exports and profile files with scoped access policies.
- **Realtime:** permitted live updates such as notifications, attendance status and workflow changes.
- **Edge Functions:** trusted operations that require secrets, external provider calls, privileged validation or AI proxying.

Never expose the Supabase service-role key, database password or provider secrets in Android, browser bundles, public repositories or client environment variables. Client applications use the Supabase publishable key and must rely on RLS for data authorization.

## Application boundaries

1. UI components render state and emit user actions; they do not bypass repositories or authorization.
2. ViewModels/hooks coordinate feature operations and expose explicit loading, success, empty and error states.
3. Shared domain concepts and validation must remain consistent between Android and web clients.
4. Data-access modules own Supabase queries, mapping and error handling.
5. Authentication identifies the user; authorization is checked independently for each action and record.
6. Tenant context is explicit: school, branch, academic year and term where relevant.
7. UI role checks improve usability only. RLS and trusted server-side checks are authoritative.
8. AI may suggest or summarize; it must never bypass permissions, approvals or human review.
9. Development seed data must be isolated from production and must not create fake production records.
10. Exports must use the same authorized data scope as the screen or workflow that requests them.

## Multi-tenant security invariants

- Every tenant-owned record is scoped to a school; branch scope applies where relevant.
- A school user cannot access another school's records.
- Teachers and class teachers are restricted to assigned classes and subjects.
- Parents are restricted to formally linked students.
- Students are restricted to their own records.
- Report-card draft, submit, return, approve and publish transitions are separately authorized.
- Finance, payroll, medical, counselling and safeguarding records require dedicated permissions.
- Audit history for sensitive actions is append-only for ordinary application users.
- Role assignment and tenant reassignment must prevent privilege escalation.
- All access rules must be tested using normal authenticated user sessions, not only SQL Editor or service-role access.

## Database change policy

- Inspect the existing schema and migration history before adding tables or columns.
- Prefer additive, reversible migrations; preserve existing school data.
- Do not duplicate existing tables or silently change established identifiers.
- Use stable UUID foreign keys for Auth users and stable student/staff relationships; do not authorize by display name.
- Test each migration in a disposable Supabase project before production.
- Keep a documented positive and negative RLS test matrix for every portal role.

## Delivery and deployment

- Web: run lint, type checks and production build; deploy the SPA with the configured hosting provider and Supabase environment variables.
- Android: run unit tests, instrumentation tests and release build with the configured Supabase URL and publishable key.
- Never mark a portal complete because its navigation or dashboard renders. Each feature must be implemented, tested and verified against RLS.
- Production deployment requires passing builds, tested migrations, configured OAuth/provider secrets and verified tenant isolation.

## Current implementation status

This document describes the approved target architecture, not a claim that all migration and portal work is already complete. Track implementation against `docs/PORTAL_FEATURE_SPECIFICATION.md` and the active GitHub issues/pull requests. The Firebase-to-Supabase migration and role-scoped RLS migration must not be considered complete until the migration is integrated into the current default branch and verified with real authenticated test identities.
