# GSMS Portal Feature Specification and Delivery Roadmap

**Repository:** RONALD1789/GSMS  
**Backend constraint:** Supabase only for the target production architecture. Do not add Firebase services or credentials.  
**Product scope:** Multi-tenant, multi-school, multi-branch school management SaaS.

This document is the implementation contract for the 19 portals. A menu item or dashboard card is not a completed feature: each capability must have a working UI, persisted Supabase operation, server-enforced authorization, validation, auditability where sensitive, and automated tests.

## Platform-wide requirements

- Tenant isolation by school; branch, academic year and term scope where relevant.
- Supabase Auth, PostgreSQL, RLS, Storage, Realtime and Edge Functions where needed.
- Role-based permissions with view/create/edit/delete/approve/reject/publish/export/print/download/manage/assign actions.
- Responsive desktop, tablet and Android browser experience; accessible forms, search, filters, pagination, loading/empty/error states.
- Durable records, input validation, safe file access, notifications, audit logs and CSV/PDF/print exports where appropriate.
- Never expose service-role keys in browser or mobile clients. Never trust frontend-only role checks.
- Keep private medical, counselling, safeguarding, payroll and financial records separately permissioned.
- No fabricated dashboard metrics, placeholder actions or silent failures.
- Do not duplicate existing schema objects. Inspect current migrations and data contracts before adding migrations.

## Portal requirements

### 1. Super Administrator
Platform-wide school and tenant registration; activate/suspend schools; branches and campuses; subscription plans, billing periods, renewals, invoices and entitlements; school administrators; global configuration; platform announcements; support tickets; system health, usage, audit, security and backup monitoring. Platform owner-only data must not be exposed to school users.

### 2. School Administrator
School dashboard; student admissions and records; guardian linking; class/stream/subject setup; teacher and class assignments; academic years and terms; timetables; grading and report templates; staff records and access assignment; fee setup and oversight; school calendar, events, facilities, transport, inventory and procurement oversight; school communication; operational and academic reports.

### 3. Headteacher
School performance dashboard; academic supervision; syllabus and lesson-plan review; class/subject comparisons; teacher performance; report-card review, return with correction notes, approve, publish, lock/reopen with authorization, archive and export; student welfare/discipline escalation; staff supervision; school announcements and management reports.

### 4. Deputy Headteacher
Delegated academic and operational oversight; attendance and discipline monitoring; lesson-plan and syllabus monitoring; examination and report completion monitoring; staff duty coordination; follow-up and referrals. Approval and publication powers must be explicitly delegated, not assumed.

### 5. Teacher
Assigned classes and subjects only; learner profiles; attendance; lesson plans and syllabus coverage; homework, assignments and submissions; continuous assessment and examination marks; learner progress and intervention notes; report-card drafts, teacher comments, submission and correction/resubmission; co-curricular records; communication with assigned learners' parents; teaching resources and timetable.

### 6. Parent/Guardian
Only formally linked children; child profiles; published reports and results; attendance and absence explanations; invoices, balances, payment history, receipts and statements; configured online/mobile-money payment options; teacher/school messaging; announcements, events and calendar; transport routes and notifications; permitted profile/contact updates.

### 7. Student
Own profile and academic record; class timetable; subjects, lessons and learning resources; assignments and submissions; published results and report cards; own attendance; library catalogue and borrowing; announcements and events; approved achievements, clubs and sports; permitted profile settings.

### 8. Accountant/Finance Officer
Fee structures, invoices, receipts, partial/advance payments, payment verification and reconciliation; discounts/scholarships with approval; debtors and statements; income, expenses, suppliers and vouchers; chart of accounts, cashbook, bank accounts, ledger, journals and reconciliations; payroll access only as authorized; financial statements and audit reports.

### 9. Human Resources
Staff directory and employment files; recruitment and onboarding; contracts and qualifications; departments and job roles; staff attendance; leave applications, approvals and balances; payroll preparation and payslips with restricted access; appraisals, training, promotion and authorized disciplinary records; HR reports and contract expiry alerts.

### 10. Librarian
Book catalogue, ISBN, authors, publishers, editions, copies, barcodes and locations; issue, return, renewal and reservations; borrowing limits; overdue reminders, fines, lost/damaged records; inventory and circulation reports.

### 11. Transport Manager
Vehicles, registration, capacity, insurance and inspection; routes, stops and schedules; drivers and attendants; student route assignment and capacity; boarding/drop-off records; transport fees; maintenance, fuel and incident records; optional GPS integration with parent notifications.

### 12. Nurse/Health Officer
Restricted student health profiles; allergies and emergency contacts; clinic visits, observations, treatment and referrals; medication administration only where authorized; emergencies and parent notifications; follow-ups, screening and clinic supplies; confidential health reports.

### 13. Receptionist
Visitor registration, purpose, host, arrival/departure and visitor passes; appointments; admissions enquiries and follow-ups; incoming correspondence and calls; staff/department directory; visitor and enquiry reports.

### 14. Procurement Officer
Purchase requisitions; supplier register and due diligence; quotation/tender requests and comparisons; budget checks; approval workflow; purchase orders; delivery and invoice tracking; supplier performance and procurement reports.

### 15. Storekeeper/Inventory Manager
Item catalogue, categories, units, locations and reorder levels; receiving, issuing, transfer and returns; stock adjustments with authorization; stock cards; damaged/expired/lost items; stocktaking and reconciliation; low-stock alerts; valuation and movement reports.

### 16. Examination Officer
Exam periods, assessment types, timetables, rooms and invigilators; examination papers and incident logs; marks submission tracking and validation; missing/outlier mark flags; controlled mark approval/locking; grading, totals, averages and configured rankings; result publication workflow; transcripts and analysis reports.

### 17. School Counsellor
Confidential referrals from authorized staff/parents; case prioritization; appointments and session notes; intervention and follow-up plans; academic, social and wellbeing support; external referrals; case closure and restricted aggregate reports. Counselling notes must not be visible to ordinary teachers, parents or general administrators.

### 18. Class Teacher
Assigned class register and learner profiles; class attendance and absentee follow-up; learner academic, behaviour, punctuality and co-curricular monitoring; parent engagement and class announcements; class duty roster, meetings and events; class summaries and report-card completion tracking. Access must be limited to assigned classes.

### 19. ICT Administrator
School-level user support; account provisioning and approved role assignments; delegated permission administration; support tickets; device and software inventory; integration and connectivity monitoring; technical logs and security incident escalation. No automatic access to confidential finance, medical, counselling or payroll data.

## Cross-portal workflows that must work end to end

1. Admissions: enquiry/application → review → approval → admission number → student record → class allocation → parent linking.
2. Student lifecycle: registration → class/stream assignment → transfer/promotion/repeat → graduation/withdrawal → archive.
3. Attendance: teacher records attendance → authorized correction window → parent notification/visibility → reports.
4. Academics: teacher lesson/assignment/assessment → marks validation → examination analysis → approved results.
5. Report cards: teacher drafts → teacher submits → leadership reviews/returns → teacher corrects → leadership approves → publish → parent/student access → archive.
6. Fees: fee structure → invoice → payment → verification/reconciliation → receipt → updated balance → parent statement.
7. HR: staff application → onboarding → role assignment → attendance/leave → authorized payroll → payslip and audit.
8. Library: catalogue → issue/reservation → return/renewal → overdue/fine → reports.
9. Transport: route/vehicle/driver setup → student assignment → boarding/drop-off → parent notices → transport billing.
10. Procurement/inventory: requisition → approval → quotations → purchase order → delivery → stock receipt → issue → reconciliation.
11. Student welfare: referral → restricted case record → intervention → follow-up → authorized closure.
12. Notifications: event/action → audience and recipient resolution → delivery attempt → read/delivery/failure status.

## Minimum data model domains

Schools, branches, academic years, terms, users/profiles, roles, permissions, role permissions, user-role assignments, departments, classes, streams, subjects, teacher assignments, students, guardians and relationships, admissions, attendance, timetables, lesson plans, assignments/submissions, assessments, examinations/marks, grading scales, report templates/cards/subjects/approvals, staff, leave, payroll, fee structures, invoices, payments, receipts, expenses, accounts/journals, books/copies/loans/reservations, vehicles/routes/drivers/student transport, health records/visits, counselling cases, discipline/safeguarding, suppliers/requisitions/orders, inventory/stock movements, notifications/messages/announcements, documents, events, visitors, support tickets, subscriptions, audit logs and system settings.

Use stable foreign keys (including student_id, staff_id and auth user UUID) rather than matching people by display names. Apply additive, reviewed migrations; preserve existing data and avoid destructive schema replacement.

## Authorization invariants

- Enforce access in Supabase RLS and trusted server/Edge Function operations, not only in UI.
- School users cannot read or mutate another school's records.
- Branch scope must be checked for branch-limited users.
- Teachers and class teachers can access only assigned classes/subjects.
- Parents can access only linked children; students only their own records.
- Report-card state transitions are role- and state-constrained.
- Financial, payroll, medical, counselling and safeguarding records require dedicated permissions.
- Audit history for sensitive changes is append-only for ordinary application users.
- Reject privilege escalation and unauthorized tenant/branch/role reassignment.

## Automation and optional AI

Automate admission/staff/reference numbers, fee balances, receipts, grading, attendance summaries, reminders, expiry alerts, low-stock alerts, timetable conflict checks, report generation and approval notifications. AI may suggest lesson plans, teacher comments and analytics, but a qualified human must review consequential outputs; AI must not bypass permissions or approvals.

## Delivery gates

For each portal, complete: navigation and role access; database-backed read/create/update/delete operations as permitted; validations; RLS policies; audit events; notifications; filters/search; export/print where required; mobile layout; unit/integration/security tests. Run TypeScript/lint and web build for web changes, and Android build/tests for Android changes. Test both allowed and denied access with owner, admin, teacher, parent and student identities.

Do not mark a portal complete because it renders. Mark each feature as **implemented**, **tested**, and **verified against RLS** separately.
