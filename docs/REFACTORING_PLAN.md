# GSMS Refactoring Plan

## Guiding rule

Refactor in small, reviewable steps. Each step must preserve existing behavior, tenant isolation and role permissions. Do not combine a broad UI redesign, data migration and package reorganization in one change.

## Phase 0 — Baseline and safeguards

- [x] Inspect the existing Android, Firebase and Supabase structure.
- [x] Create a dedicated refactoring branch.
- [x] Document the current architecture and target boundaries.
- [x] Move seed-data implementation into a dedicated data/seed package.
- [x] Isolate Firestore client configuration in a dedicated Firebase data provider.
- [ ] Run the existing unit, instrumentation and Firestore rules test suites in a configured build environment.
- [ ] Record baseline build and test results before moving implementation files.

## Phase 1 — Separate domain contracts from infrastructure

- Extract domain models, role/capability definitions and repository interfaces into domain packages.
- Keep existing model field names and serialization behavior.
- Add explicit mappers at Firebase/Supabase boundaries where required.
- Move Firestore implementation behind repository interfaces.
- Verify no UI screen imports Firebase SDK classes for data access.

## Phase 2 — Split the data layer

Break the current large repository into focused implementations:
- Identity and user profile
- School and branch administration
- Student and staff records
- Academics, attendance and report cards
- Finance and payments
- Leave and HR
- Communications and operations
- Alerts and audit logs

Keep a facade or compatibility adapter while callers migrate. Avoid changing Firestore paths and rules during this phase.

## Phase 3 — Split feature UI and state

- Move screens into feature packages by business capability.
- Split the large ViewModel into feature-scoped ViewModels/state holders.
- Keep shared navigation, theme and reusable components separate.
- Preserve all existing portal routes and role-specific navigation behavior.

## Phase 4 — Dependency wiring and test coverage

- Introduce a single, explicit dependency composition point.
- Add unit tests for use cases and permission decisions.
- Add repository tests for tenant-scoped queries and mapping.
- Retain and expand Firestore Rules emulator tests.
- Add Supabase RLS tests before any production use of Supabase persistence.

## Phase 5 — Backend and schema reconciliation

- Decide whether Firebase or Supabase is the authoritative persistence platform for each data domain.
- Document the synchronization direction and conflict policy if both remain active.
- Reconcile field names, timestamps, identity IDs and tenant constraints.
- Treat any schema change as a separate migration with rollback instructions.

## Acceptance criteria

- The Android app builds successfully.
- Existing automated tests pass; new tests cover extracted boundaries.
- No existing role loses or gains capabilities unintentionally.
- Cross-school and cross-branch access remains denied unless explicitly assigned.
- Parent, student and teacher record scoping remains enforced by backend rules.
- Report-card approval and publication states remain protected.
- No production data, credentials or signing material is committed.
- No migration is applied to a live Firebase or Supabase project as part of a package-only refactor.

## Current review notes

The repository currently has a mixed Firebase/Supabase footprint. Firebase is used by the Android client and Firestore rules; a Supabase schema and Retrofit integration also exist. Before further implementation, identify which backend is authoritative. Do not assume that the presence of a SQL schema means the app has fully migrated to Supabase.

Large files identified during inspection include:
- data/GsmsRepository.kt (~51 KB)
- data/GsmsSeedData.kt (~38 KB)
- ui/GsmsViewModel.kt (~31 KB)
- ui/PortalDashboards.kt (~37 KB)
- Several feature screens between ~15 KB and ~49 KB

The first code-moving increment has moved the seed-data object to data/seed and extracted Firestore client setup to data/firebase/FirestoreProvider.kt. GsmsRepository remains the compatibility facade and still owns operational Firestore queries and mutations.

The Android build has not been executed in this environment. There is no GitHub Actions workflow configured in the repository, so the changes require a local Gradle build before merge.

The next code-moving pull request should split GsmsRepository by domain while retaining a compatibility facade and preserving Firestore collection paths and security semantics.