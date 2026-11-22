# GSMS Supabase RLS verification plan

Run this plan against a non-production Supabase project after applying the existing `supabase/schema.sql` and then `supabase/migrations/20261122120000_gsms_role_scoped_rls.sql`.

Do not test with the service-role key from the Android client. Use separate real Supabase Auth users and their normal authenticated sessions.

## Test identities

Create two school tenants (School A and School B) with distinct owners. In School A, provision:
- one School Admin;
- one Headteacher;
- one Teacher assigned to Branch A1 and Class P5;
- one Parent linked to Student S1;
- one Finance Officer assigned to Branch A1;
- one HR Manager assigned to Branch A1;
- one Librarian assigned to Branch A1;
- one Deputy Headteacher, Class Teacher, Procurement Officer, Storekeeper, Examination Officer, School Counsellor, Welfare Nurse, Admissions Receptionist and ICT Admin, each assigned to Branch A1;
- staff directory rows whose full names match their authenticated access assignments.

Also create a second branch A2, a second class P6, and a second student S2.

## Expected access matrix

| Identity | Expected access |
|---|---|
| School A owner | Read/write all School A records and branches; may manage assignments and portal role override |
| School A School Admin | Manage permitted school operations and assignments; cannot grant SUPER_ADMIN or change own assignment |
| Headteacher | School-wide academic/report leadership access; can review and approve report cards |
| Teacher | Can register and access students only in assigned branch/class; can manage class academics, attendance and reports, submit drafts, but cannot approve/publish |
| Parent | Only linked student summaries, APPROVED/PUBLISHED reports and parent-audience communications; no internal risk/welfare fields, school revenue/subscription metrics, finance, staff, alerts or unrelated learners |
| Finance Officer | Finance records only in assigned branch |
| HR Manager | Staff and leave records only in assigned branch |
| Librarian | LIBRARY operations and own leave request in assigned branch |
| Deputy Headteacher | School-wide academic oversight, report review, exam moderation, leave workflow visibility; cannot approve leave unless separately authorized |
| Class Teacher | Assigned-class learners, attendance, report drafting/submission and own leave only |
| Procurement Officer | Procurement create/update; Stores is read-only |
| Storekeeper | Stores create/update; Procurement is read-only |
| Examination Officer | Exam schedules, invigilation, mark moderation and report templates; assigned-class academic scope |
| School Counsellor | Branch-scoped learner support and counselling/discipline cases; Welfare is read-only |
| Welfare Nurse | Welfare/hostel updates; counselling/discipline is read-only |
| Admissions Reception | Front desk, admissions and event workflows |
| ICT Admin | ICT tickets/assets/incidents and limited user provisioning; cannot create school leadership, finance or HR roles |
| School B owner/member | No access to School A rows |
| Unassigned authenticated user | No access to either school |

## Identity and branch-link tests

- A new/unassigned authenticated account cannot create a school through the Android publishable-key API.
- The first tenant is inserted by a trusted database administrator using `supabase/first-school-provisioning.sql`; an existing verified school owner can create additional tenants.
- Existing access assignments are backfilled to `auth_user_id` using their matching Supabase Auth account.
- A newly created Auth account is linked to a pre-provisioned assignment by email; subsequent authorization uses the immutable Auth UUID.
- Changing an Auth account's email does not silently transfer its access assignment to a different account.
- Legacy `branch_main` placeholders are mapped to the actual main-campus branch ID where one exists.
- New staff assignments default to the currently selected campus; parent/learner branch grants are derived from linked students' campuses.

## Required negative tests

1. Teacher assigned to P5/A1 can register a learner in P5/A1 but cannot register, select or mutate P6/A2 records.
2. Teacher cannot read finance, staff, leave, alerts or access-assignment rows belonging to other users.
3. Teacher cannot update an APPROVED/PUBLISHED report or set a report status to APPROVED/PUBLISHED.
4. Parent cannot select another student's records or any report that is not APPROVED/PUBLISHED.
5. Parent/student communication reads must be limited to rows whose audience explicitly includes their portal audience or ALL.
6. Non-owner school summaries must mask owner IDs, subscription details, revenue and churn metrics.
7. Parent can read attendance only when the row has a stable student_id matching one of their linked students. Backfill must update only unambiguous name matches; ambiguous legacy rows remain unavailable until manually linked.
8. Finance Officer cannot read student medical/welfare alerts or HR records.
9. No access assignment can grant SUPER_ADMIN; the verified school owner is the only account treated as the owner/Super Admin. A School Admin also cannot alter their own role by changing their assignment.
10. A non-owner cannot append an audit record using another user's name or portal role.
11. A member of School A cannot read or mutate any School B data.
12. A disabled assignment (`active = false`) must immediately lose access after the next authenticated request.
13. An unauthenticated/anonymous request must not read or mutate any tenant data.

## Remaining schema / workflow limitations

- Finance rows still need a stable student/guardian relationship before parents can see their own itemized invoices. The parent-safe student summary exposes the cached fee balance only.
- Communications currently has no `target_class` or recipient-student field. Parent communications remain audience- and branch-scoped; teacher-to-class-parent targeting still needs a schema/UI enhancement.
- The current report-card status model should be validated against the app's exact workflow before production approval. Teachers are restricted to DRAFT/RETURNED rows and may submit; only leadership can approve/publish.
- Operational module records now use role-specific labels and status workflows, but inventory quantities, purchase-order line items, exam timetables and ICT ticket SLAs still require dedicated structured fields and end-to-end operational testing.

## Release gate

Do not merge or apply to production until:
- this migration executes successfully in a disposable Supabase project;
- all positive and negative tests above pass using real authenticated JWTs;
- Android owner, school-admin, teacher, parent and finance flows have been tested;
- RLS behavior is verified from the Android publishable-key client, not just from SQL Editor/service-role access.
