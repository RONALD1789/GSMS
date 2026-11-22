# GSMS Web Portal

The repository now contains a browser-based GSMS portal alongside the existing Android application.

## Stack

- React + TypeScript + Vite
- Supabase Auth for email/password sessions
- Supabase Postgres for school access context
- Supabase Row Level Security remains the authority for data access
- Vercel for static web hosting

The web client does not use Firebase. Do not add Firebase SDKs or Firebase credentials to the web portal.

## Local development

1. Install Node.js 20 or newer.
2. Copy `.env.web.example` to `.env.local`.
3. Set the Supabase project URL and publishable key.
4. Run `npm install`.
5. Run `npm run dev`.

## Vercel configuration

Import `RONALD1789/GSMS` as a Vercel project and keep the project root directory as `./`.

- Framework preset: Vite
- Install command: `npm install`
- Build command: `npm run build`
- Output directory: `dist`

Set these Environment Variables for Preview and Production:

- `VITE_SUPABASE_URL`: the GSMS Supabase project URL
- `VITE_SUPABASE_PUBLISHABLE_KEY`: the Supabase publishable key (`sb_publishable_...`)

Never set a Supabase service-role key in a Vite environment variable. Vite variables are shipped to the browser.

## Authentication and access

After sign-in, the portal reads active rows from `public.gsms_user_access_assignments` for the authenticated user's `auth_user_id`, then resolves the selected school from `public.gsms_schools`. The portal only displays navigation items allowed for the resolved role. This client-side filtering is for usability; database RLS must enforce every read and write.

The current access bootstrap expects the role-scoped Supabase migration to have been applied successfully. Users without an active assignment see an unassigned workspace state rather than fabricated school data.

## Implementation status

This branch now includes Supabase sign-in, active assignment lookup, tenant context, responsive role-filtered navigation, searchable/paginated data workspaces for the verified GSMS tables and summary views, and report-card submit/approve/publish actions. Most other data modules are currently read-only. Create/edit forms, detailed module workflows, exports, messaging delivery integrations and end-to-end role testing remain before production-complete release.
