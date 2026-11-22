-- GSMS additive RLS hardening migration.
-- Preserves existing tables and rows. Replaces broad tenant-wide policies with
-- explicit owner, role, class and linked-student access checks.
-- Review against the live schema and test with separate owner/admin/teacher/parent accounts
-- before applying to production.

begin;

-- SECURITY DEFINER helpers avoid recursive RLS evaluation when checking assignments.
-- Fixed search_path prevents object-shadowing attacks.
create or replace function public.gsms_is_school_owner(target_school_id text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1
    from public.gsms_schools s
    where s.id = target_school_id
      and (
        s.supabase_owner_uid = (select auth.uid())
        or s.owner_id = (select auth.uid())::text
      )
  );
$$;

create or replace function public.gsms_has_any_role(
  target_school_id text,
  allowed_roles text[]
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1
    from public.gsms_user_access_assignments a
    where a.school_id = target_school_id
      and a.active = true
      and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
      and upper(a.primary_role) = any (allowed_roles)
  );
$$;

create or replace function public.gsms_is_school_member(target_school_id text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select public.gsms_is_school_owner(target_school_id)
      or exists (
        select 1
        from public.gsms_user_access_assignments a
        where a.school_id = target_school_id
          and a.active = true
          and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
      );
$$;

create or replace function public.gsms_is_school_manager(target_school_id text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select public.gsms_is_school_owner(target_school_id)
      or public.gsms_has_any_role(target_school_id, array['SCHOOL_ADMIN']);
$$;

create or replace function public.gsms_can_access_class(
  target_school_id text,
  target_class_name text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select
    public.gsms_is_school_manager(target_school_id)
    or public.gsms_has_any_role(
      target_school_id,
      array['HEADTEACHER', 'DEPUTY_HEADTEACHER']
    )
    or exists (
      select 1
      from public.gsms_user_access_assignments a
      where a.school_id = target_school_id
        and a.active = true
        and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
        and upper(a.primary_role) in ('TEACHER', 'CLASS_TEACHER', 'EXAMINATION_OFFICER')
        and target_class_name is not null
        and exists (
          select 1
          from unnest(a.assigned_classes) assigned_class
          where lower(assigned_class) = lower(target_class_name)
        )
    );
$$;

create or replace function public.gsms_can_access_branch(
  target_school_id text,
  target_branch_id text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $
  select
    public.gsms_is_school_manager(target_school_id)
    or public.gsms_has_any_role(
      target_school_id,
      array['HEADTEACHER', 'DEPUTY_HEADTEACHER']
    )
    or exists (
      select 1
      from public.gsms_user_access_assignments a
      where a.school_id = target_school_id
        and a.active = true
        and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
        and target_branch_id is not null
        and target_branch_id = any(a.assigned_branch_ids)
    );
$;

create or replace function public.gsms_can_access_class_in_branch(
  target_school_id text,
  target_branch_id text,
  target_class_name text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $
  select public.gsms_can_access_branch(target_school_id, target_branch_id)
     and public.gsms_can_access_class(target_school_id, target_class_name);
$;

create or replace function public.gsms_can_access_student_in_branch(
  target_school_id text,
  target_branch_id text,
  target_student_id text,
  target_class_name text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $
  select public.gsms_can_access_branch(target_school_id, target_branch_id)
     and public.gsms_can_access_student(target_school_id, target_student_id, target_class_name);
$;

create or replace function public.gsms_can_access_student(
  target_school_id text,
  target_student_id text,
  target_class_name text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select
    public.gsms_is_school_manager(target_school_id)
    or public.gsms_has_any_role(
      target_school_id,
      array['HEADTEACHER', 'DEPUTY_HEADTEACHER']
    )
    or public.gsms_can_access_class(target_school_id, target_class_name)
    or exists (
      select 1
      from public.gsms_user_access_assignments a
      where a.school_id = target_school_id
        and a.active = true
        and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
        and upper(a.primary_role) in ('PARENT', 'STUDENT')
        and target_student_id = any(a.linked_student_ids)
    );
$$;

create or replace function public.gsms_can_read_report(
  target_school_id text,
  target_student_id text,
  target_class_name text,
  target_status text
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select
    public.gsms_is_school_manager(target_school_id)
    or public.gsms_has_any_role(
      target_school_id,
      array['HEADTEACHER', 'DEPUTY_HEADTEACHER']
    )
    or public.gsms_can_access_class(target_school_id, target_class_name)
    or (
      upper(coalesce(target_status, '')) in ('APPROVED', 'PUBLISHED')
      and exists (
        select 1
        from public.gsms_user_access_assignments a
        where a.school_id = target_school_id
          and a.active = true
          and lower(a.email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
          and upper(a.primary_role) in ('PARENT', 'STUDENT')
          and target_student_id = any(a.linked_student_ids)
      )
    );
$$;

create or replace function public.gsms_can_manage_assignments(target_school_id text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select public.gsms_is_school_manager(target_school_id);
$$;

-- Do not expose helper functions to anonymous clients.
revoke all on function public.gsms_is_school_owner(text) from public, anon;
revoke all on function public.gsms_has_any_role(text, text[]) from public, anon;
revoke all on function public.gsms_is_school_member(text) from public, anon;
revoke all on function public.gsms_is_school_manager(text) from public, anon;
revoke all on function public.gsms_can_access_class(text, text) from public, anon;
revoke all on function public.gsms_can_access_branch(text, text) from public, anon;
revoke all on function public.gsms_can_access_class_in_branch(text, text, text) from public, anon;
revoke all on function public.gsms_can_access_student_in_branch(text, text, text, text) from public, anon;
revoke all on function public.gsms_can_access_student(text, text, text) from public, anon;
revoke all on function public.gsms_can_read_report(text, text, text, text) from public, anon;
revoke all on function public.gsms_can_manage_assignments(text) from public, anon;

grant execute on function public.gsms_is_school_owner(text) to authenticated;
grant execute on function public.gsms_has_any_role(text, text[]) to authenticated;
grant execute on function public.gsms_is_school_member(text) to authenticated;
grant execute on function public.gsms_is_school_manager(text) to authenticated;
grant execute on function public.gsms_can_access_class(text, text) to authenticated;
grant execute on function public.gsms_can_access_branch(text, text) to authenticated;
grant execute on function public.gsms_can_access_class_in_branch(text, text, text) to authenticated;
grant execute on function public.gsms_can_access_student_in_branch(text, text, text, text) to authenticated;
grant execute on function public.gsms_can_access_student(text, text, text) to authenticated;
grant execute on function public.gsms_can_read_report(text, text, text, text) to authenticated;
grant execute on function public.gsms_can_manage_assignments(text) to authenticated;

-- SCHOOL TENANTS: members may read their school; only its verified owner may create,
-- update or delete the tenant record.
drop policy if exists "gsms_schools_authenticated_rw" on public.gsms_schools;
drop policy if exists "gsms_schools_member_select" on public.gsms_schools;
drop policy if exists "gsms_schools_owner_insert" on public.gsms_schools;
drop policy if exists "gsms_schools_owner_update" on public.gsms_schools;
drop policy if exists "gsms_schools_owner_delete" on public.gsms_schools;

create policy "gsms_schools_member_select" on public.gsms_schools
for select to authenticated
using (public.gsms_is_school_member(id));

create policy "gsms_schools_owner_insert" on public.gsms_schools
for insert to authenticated
with check (supabase_owner_uid = (select auth.uid()));

create policy "gsms_schools_owner_update" on public.gsms_schools
for update to authenticated
using (public.gsms_is_school_owner(id))
with check (
  supabase_owner_uid = (select auth.uid())
  or owner_id = (select auth.uid())::text
);

create policy "gsms_schools_owner_delete" on public.gsms_schools
for delete to authenticated
using (public.gsms_is_school_owner(id));

-- BRANCHES: all assigned members may read; only owner/school admin may change them.
drop policy if exists "gsms_branches_tenant_isolation" on public.gsms_branches;
create policy "gsms_branches_member_select" on public.gsms_branches
for select to authenticated
using (public.gsms_is_school_member(school_id));
create policy "gsms_branches_manager_insert" on public.gsms_branches
for insert to authenticated
with check (public.gsms_is_school_manager(school_id));
create policy "gsms_branches_manager_update" on public.gsms_branches
for update to authenticated
using (public.gsms_is_school_manager(school_id))
with check (public.gsms_is_school_manager(school_id));
create policy "gsms_branches_manager_delete" on public.gsms_branches
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- USER ACCESS ASSIGNMENTS: users may read their own assignment. School managers may
-- read/manage all assignments. Non-owners cannot grant SUPER_ADMIN or edit their own role.
drop policy if exists "gsms_user_access_assignments_tenant_isolation" on public.gsms_user_access_assignments;
create policy "gsms_assignments_select" on public.gsms_user_access_assignments
for select to authenticated
using (
  public.gsms_is_school_manager(school_id)
  or lower(email) = lower(coalesce((select auth.jwt()) ->> 'email', ''))
);
create policy "gsms_assignments_manager_insert" on public.gsms_user_access_assignments
for insert to authenticated
with check (
  public.gsms_is_school_owner(school_id)
  or (
    public.gsms_has_any_role(school_id, array['SCHOOL_ADMIN'])
    and upper(primary_role) <> 'SUPER_ADMIN'
    and lower(email) <> lower(coalesce((select auth.jwt()) ->> 'email', ''))
  )
);
create policy "gsms_assignments_manager_update" on public.gsms_user_access_assignments
for update to authenticated
using (public.gsms_is_school_manager(school_id))
with check (
  public.gsms_is_school_owner(school_id)
  or (
    public.gsms_has_any_role(school_id, array['SCHOOL_ADMIN'])
    and upper(primary_role) <> 'SUPER_ADMIN'
    and lower(email) <> lower(coalesce((select auth.jwt()) ->> 'email', ''))
  )
);
create policy "gsms_assignments_manager_delete" on public.gsms_user_access_assignments
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- STUDENTS: linked parents/learners see only linked children; teachers see assigned classes.
drop policy if exists "gsms_students_tenant_isolation" on public.gsms_students;
create policy "gsms_students_scoped_select" on public.gsms_students
for select to authenticated
using (public.gsms_can_access_student_in_branch(school_id, branch_id, id, class_name));
create policy "gsms_students_manager_insert" on public.gsms_students
for insert to authenticated
with check (public.gsms_is_school_manager(school_id));
create policy "gsms_students_manager_update" on public.gsms_students
for update to authenticated
using (public.gsms_is_school_manager(school_id))
with check (public.gsms_is_school_manager(school_id));
create policy "gsms_students_manager_delete" on public.gsms_students
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- STAFF: school leadership and HR only.
drop policy if exists "gsms_staff_tenant_isolation" on public.gsms_staff;
create policy "gsms_staff_authorized_select" on public.gsms_staff
for select to authenticated
using ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_staff_authorized_insert" on public.gsms_staff
for insert to authenticated
with check ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_staff_authorized_update" on public.gsms_staff
for update to authenticated
using ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id))
with check ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_staff_authorized_delete" on public.gsms_staff
for delete to authenticated
using (public.gsms_is_school_manager(school_id)
  and public.gsms_can_access_branch(school_id, branch_id));

-- ACADEMIC RECORDS: leadership and assigned teachers/exam officers, limited to assigned branches.
drop policy if exists "gsms_academic_records_tenant_isolation" on public.gsms_academic_records;
create policy "gsms_academics_scoped_select" on public.gsms_academic_records
for select to authenticated
using (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_academics_scoped_insert" on public.gsms_academic_records
for insert to authenticated
with check (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_academics_scoped_update" on public.gsms_academic_records
for update to authenticated
using (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name))
with check (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_academics_manager_delete" on public.gsms_academic_records
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- ATTENDANCE: assigned branch/class access only. No student_id foreign key exists yet,
-- so parent/student attendance access is deliberately withheld rather than matching names.
drop policy if exists "gsms_attendance_tenant_isolation" on public.gsms_attendance;
create policy "gsms_attendance_scoped_select" on public.gsms_attendance
for select to authenticated
using (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_attendance_scoped_insert" on public.gsms_attendance
for insert to authenticated
with check (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_attendance_scoped_update" on public.gsms_attendance
for update to authenticated
using (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name))
with check (public.gsms_can_access_class_in_branch(school_id, branch_id, class_name));
create policy "gsms_attendance_manager_delete" on public.gsms_attendance
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- REPORT TEMPLATES: teachers can read; leadership/exam officers can manage.
drop policy if exists "gsms_report_templates_tenant_isolation" on public.gsms_report_templates;
create policy "gsms_templates_authorized_select" on public.gsms_report_templates
for select to authenticated
using (
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'TEACHER', 'CLASS_TEACHER', 'EXAMINATION_OFFICER'])
);
create policy "gsms_templates_authorized_insert" on public.gsms_report_templates
for insert to authenticated
with check (public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'EXAMINATION_OFFICER']));
create policy "gsms_templates_authorized_update" on public.gsms_report_templates
for update to authenticated
using (public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'EXAMINATION_OFFICER']))
with check (public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'EXAMINATION_OFFICER']));
create policy "gsms_templates_manager_delete" on public.gsms_report_templates
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- REPORT CARDS: teachers may submit drafts for assigned classes/branches; parents and
-- learners see only approved/published reports for linked student IDs.
drop policy if exists "gsms_report_cards_tenant_isolation" on public.gsms_report_cards;
create policy "gsms_reports_scoped_select" on public.gsms_report_cards
for select to authenticated
using (
  public.gsms_can_read_report(school_id, student_id, class_name, status)
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_reports_insert" on public.gsms_report_cards
for insert to authenticated
with check (
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER'])
  or (
    public.gsms_can_access_class_in_branch(school_id, branch_id, class_name)
    and public.gsms_has_any_role(school_id, array['TEACHER', 'CLASS_TEACHER'])
    and upper(status) = 'DRAFT'
  )
);
create policy "gsms_reports_update" on public.gsms_report_cards
for update to authenticated
using (
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER'])
  or (
    public.gsms_can_access_class_in_branch(school_id, branch_id, class_name)
    and public.gsms_has_any_role(school_id, array['TEACHER', 'CLASS_TEACHER'])
    and upper(status) in ('DRAFT', 'RETURNED')
  )
)
with check (
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER'])
  or (
    public.gsms_can_access_class_in_branch(school_id, branch_id, class_name)
    and public.gsms_has_any_role(school_id, array['TEACHER', 'CLASS_TEACHER'])
    and upper(status) in ('DRAFT', 'SUBMITTED')
  )
);
create policy "gsms_reports_manager_delete" on public.gsms_report_cards
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- FINANCE: owner, school admin and finance officers, scoped to assigned branches.
drop policy if exists "gsms_finance_entries_tenant_isolation" on public.gsms_finance_entries;
create policy "gsms_finance_authorized_select" on public.gsms_finance_entries
for select to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['FINANCE_OFFICER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_finance_authorized_insert" on public.gsms_finance_entries
for insert to authenticated
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['FINANCE_OFFICER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_finance_authorized_update" on public.gsms_finance_entries
for update to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['FINANCE_OFFICER']))
  and public.gsms_can_access_branch(school_id, branch_id)
)
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['FINANCE_OFFICER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_finance_manager_delete" on public.gsms_finance_entries
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- LEAVE: leadership and HR only, scoped to assigned branches. A future schema change
-- should link leave requests to auth.uid() before staff can read their own requests.
drop policy if exists "gsms_leave_requests_tenant_isolation" on public.gsms_leave_requests;
create policy "gsms_leave_authorized_select" on public.gsms_leave_requests
for select to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_leave_authorized_insert" on public.gsms_leave_requests
for insert to authenticated
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_leave_authorized_update" on public.gsms_leave_requests
for update to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id)
)
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'HR_MANAGER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_leave_manager_delete" on public.gsms_leave_requests
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- COMMUNICATIONS: only school leadership and teaching staff can read/write.
drop policy if exists "gsms_communications_tenant_isolation" on public.gsms_communications;
create policy "gsms_comms_authorized_select" on public.gsms_communications
for select to authenticated
using ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'TEACHER', 'CLASS_TEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_comms_authorized_insert" on public.gsms_communications
for insert to authenticated
with check ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'TEACHER', 'CLASS_TEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_comms_authorized_update" on public.gsms_communications
for update to authenticated
using ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id))
with check ((public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_comms_manager_delete" on public.gsms_communications
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- OPERATIONS: leadership and relevant operational role, restricted to assigned branches.
drop policy if exists "gsms_operations_tenant_isolation" on public.gsms_operations;
create policy "gsms_operations_authorized_select" on public.gsms_operations
for select to authenticated
using ((
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'ICT_ADMIN'])
  or (upper(module_type) like '%LIBRARY%' and public.gsms_has_any_role(school_id, array['LIBRARIAN']))
  or (upper(module_type) like '%TRANSPORT%' and public.gsms_has_any_role(school_id, array['TRANSPORT_MANAGER']))
  or (upper(module_type) like '%PROCUREMENT%' and public.gsms_has_any_role(school_id, array['PROCUREMENT_OFFICER']))
  or (upper(module_type) like '%STORE%' and public.gsms_has_any_role(school_id, array['STOREKEEPER']))
  or (upper(module_type) like '%HEALTH%' and public.gsms_has_any_role(school_id, array['WELFARE_NURSE']))
  or (upper(module_type) like '%ADMISSION%' and public.gsms_has_any_role(school_id, array['ADMISSIONS_RECEPTION']))
  or (upper(module_type) like '%DISCIPLINE%' and public.gsms_has_any_role(school_id, array['SCHOOL_COUNSELLOR']))
) and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_operations_authorized_insert" on public.gsms_operations
for insert to authenticated
with check ((
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'ICT_ADMIN'])
  or (upper(module_type) like '%LIBRARY%' and public.gsms_has_any_role(school_id, array['LIBRARIAN']))
  or (upper(module_type) like '%TRANSPORT%' and public.gsms_has_any_role(school_id, array['TRANSPORT_MANAGER']))
  or (upper(module_type) like '%PROCUREMENT%' and public.gsms_has_any_role(school_id, array['PROCUREMENT_OFFICER']))
  or (upper(module_type) like '%STORE%' and public.gsms_has_any_role(school_id, array['STOREKEEPER']))
  or (upper(module_type) like '%HEALTH%' and public.gsms_has_any_role(school_id, array['WELFARE_NURSE']))
  or (upper(module_type) like '%ADMISSION%' and public.gsms_has_any_role(school_id, array['ADMISSIONS_RECEPTION']))
  or (upper(module_type) like '%DISCIPLINE%' and public.gsms_has_any_role(school_id, array['SCHOOL_COUNSELLOR']))
) and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_operations_authorized_update" on public.gsms_operations
for update to authenticated
using ((
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'ICT_ADMIN'])
  or (upper(module_type) like '%LIBRARY%' and public.gsms_has_any_role(school_id, array['LIBRARIAN']))
  or (upper(module_type) like '%TRANSPORT%' and public.gsms_has_any_role(school_id, array['TRANSPORT_MANAGER']))
  or (upper(module_type) like '%PROCUREMENT%' and public.gsms_has_any_role(school_id, array['PROCUREMENT_OFFICER']))
  or (upper(module_type) like '%STORE%' and public.gsms_has_any_role(school_id, array['STOREKEEPER']))
  or (upper(module_type) like '%HEALTH%' and public.gsms_has_any_role(school_id, array['WELFARE_NURSE']))
  or (upper(module_type) like '%ADMISSION%' and public.gsms_has_any_role(school_id, array['ADMISSIONS_RECEPTION']))
  or (upper(module_type) like '%DISCIPLINE%' and public.gsms_has_any_role(school_id, array['SCHOOL_COUNSELLOR']))
) and public.gsms_can_access_branch(school_id, branch_id))
with check ((
  public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER', 'ICT_ADMIN'])
  or (upper(module_type) like '%LIBRARY%' and public.gsms_has_any_role(school_id, array['LIBRARIAN']))
  or (upper(module_type) like '%TRANSPORT%' and public.gsms_has_any_role(school_id, array['TRANSPORT_MANAGER']))
  or (upper(module_type) like '%PROCUREMENT%' and public.gsms_has_any_role(school_id, array['PROCUREMENT_OFFICER']))
  or (upper(module_type) like '%STORE%' and public.gsms_has_any_role(school_id, array['STOREKEEPER']))
  or (upper(module_type) like '%HEALTH%' and public.gsms_has_any_role(school_id, array['WELFARE_NURSE']))
  or (upper(module_type) like '%ADMISSION%' and public.gsms_has_any_role(school_id, array['ADMISSIONS_RECEPTION']))
  or (upper(module_type) like '%DISCIPLINE%' and public.gsms_has_any_role(school_id, array['SCHOOL_COUNSELLOR']))
) and public.gsms_can_access_branch(school_id, branch_id));
create policy "gsms_operations_manager_delete" on public.gsms_operations
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- ALERTS: sensitive risk/welfare data is restricted to school leadership.
drop policy if exists "gsms_early_warning_alerts_tenant_isolation" on public.gsms_early_warning_alerts;
create policy "gsms_alerts_leadership_select" on public.gsms_early_warning_alerts
for select to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_alerts_leadership_insert" on public.gsms_early_warning_alerts
for insert to authenticated
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_alerts_leadership_update" on public.gsms_early_warning_alerts
for update to authenticated
using (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id)
)
with check (
  (public.gsms_is_school_manager(school_id)
    or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'DEPUTY_HEADTEACHER']))
  and public.gsms_can_access_branch(school_id, branch_id)
);
create policy "gsms_alerts_manager_delete" on public.gsms_early_warning_alerts
for delete to authenticated
using (public.gsms_is_school_manager(school_id));

-- AUDIT LOGS: members may append; leadership and ICT may read; no update/delete policy.
drop policy if exists "gsms_audit_logs_read_insert" on public.gsms_audit_logs;
drop policy if exists "gsms_audit_logs_insert_only" on public.gsms_audit_logs;
create policy "gsms_audit_logs_authorized_select" on public.gsms_audit_logs
for select to authenticated
using (public.gsms_is_school_manager(school_id)
  or public.gsms_has_any_role(school_id, array['HEADTEACHER', 'ICT_ADMIN']));
create policy "gsms_audit_logs_member_insert" on public.gsms_audit_logs
for insert to authenticated
with check (public.gsms_is_school_member(school_id));

commit;
