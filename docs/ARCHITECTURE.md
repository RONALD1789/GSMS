# GSMS Architecture

## Current implementation

GSMS is an Android application built with Kotlin and Jetpack Compose.

- Entry point: app/src/main/java/com/example/MainActivity.kt
- UI and navigation: com.example.ui
- Data access: com.example.data
- Domain models and role definitions: com.example.model
- Firebase: Authentication, Firestore, App Check and Firestore Security Rules
- Supabase: SQL schema and a Retrofit-based synchronization path
- Tests: Firestore rules tests at repository root and Android unit/instrumentation test source sets

The current implementation is functional but several files combine multiple responsibilities. In particular, the Firestore repository, ViewModel, seed data and large Compose screens are too broad to evolve independently.

## Target architecture

Keep the existing Android application and backend integrations. Refactor internally into clear layers without changing user-facing behavior or database contracts during the initial phases.

```text
app/src/main/java/com/example/
  app/                 # Application startup and dependency wiring
  core/
    auth/              # Authentication state and identity
    security/          # Permission evaluation and access policy adapters
    common/            # Shared result types, dispatchers and utilities
    design/            # Shared Compose components and theme
  domain/
    model/             # Domain entities and value types
    access/            # Role, capability and permission contracts
    repository/        # Repository interfaces
    usecase/           # Business actions and validation
  data/
    firebase/
      auth/            # Firebase authentication adapter
      firestore/       # Firestore implementation and query mapping
    supabase/          # Supabase API and synchronization adapter
    repository/        # Repository implementations and mappers
    seed/              # Demo and development seed data
    export/            # PDF and document export
    ai/                # AI provider integration
  feature/
    auth/              # Login and account recovery
    dashboard/         # Portal landing pages
    academics/         # Academics, attendance and reports
    people/            # Students, staff and leave
    finance/           # Fees, payments and finance
    operations/        # Library, transport, procurement and operations
    communications/    # Messaging and announcements
    analytics/         # Reports, metrics and alerts
    governance/        # Permissions, audit and AI governance
  navigation/          # Navigation graph and portal routing
```

This is a target package map, not a request to create empty packages. Move code only when its dependencies and tests can be updated in the same change.

## Architectural boundaries

1. Compose screens render state and emit user actions. They must not query Firebase or Supabase directly.
2. ViewModels coordinate feature use cases and expose immutable UI state.
3. Domain models and use cases must not depend on Android, Compose, Firebase or Supabase SDK types.
4. Repository interfaces belong to the domain layer; Firebase and Supabase implementations belong to the data layer.
5. Authentication identifies the user. Authorization is evaluated independently for each action and resource.
6. Tenant scope must be explicit: school, branch, academic year and term where applicable.
7. Firestore Security Rules and Supabase Row Level Security remain authoritative server-side enforcement. Hiding a button or tab is not authorization.
8. AI features may propose or summarize actions, but must not bypass permissions or approval workflows.
9. Demo seed data must be isolated from production initialization and must never silently create production users or records.
10. PDF export must consume approved, authorized domain data and must not independently widen access.

## Data and security invariants

- Never expose service-role credentials or private signing secrets in the client.
- Do not weaken existing Firestore rules or Supabase RLS to make a refactor pass.
- Every tenant-owned record must be scoped to its school and, where relevant, branch.
- Parent access is limited to linked students; student access is limited to their own records.
- Teacher access is limited to assigned classes and subjects.
- Report-card submission, review, approval and publication remain separate authorized transitions.
- Audit records remain immutable to ordinary application users.
- Keep Firebase and Supabase schema changes out of pure package-structure refactors.

## Compatibility strategy

The refactor is incremental. Preserve public model shapes, Firestore collection names, document field names, Supabase table names and security semantics until a separately reviewed migration is approved. Prefer adapters and interfaces before changing persistence contracts.