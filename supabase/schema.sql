-- ============================================================================
-- Global School Management System (GSMS) - Supabase PostgreSQL & RLS Schema
-- Multi-tenant, 19-Portal RBAC + Permission Delegation + Audit Trail
-- Run this script in your Supabase Project -> SQL Editor
-- ============================================================================

-- 1. SCHOOLS (Tenants)
create table if not exists public.gsms_schools (
    id text primary key,
    owner_id text not null,
    supabase_owner_uid uuid default auth.uid(),
    name text not null,
    code text not null,
    country text not null default '',
    city text not null default '',
    plan text not null default 'ENTERPRISE',
    subscription_status text not null default 'ACTIVE',
    brand_color_hex text not null default '#1D4ED8',
    active_academic_year text not null default '2026/2027',
    active_term text not null default 'Term 1',
    enabled_modules text[] not null default '{}',
    student_count bigint not null default 0,
    staff_count bigint not null default 0,
    monthly_revenue_usd double precision not null default 0,
    churn_risk_score bigint not null default 0,
    updated_at timestamptz not null default now()
);

-- 2. BRANCHES / CAMPUSES
create table if not exists public.gsms_branches (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    owner_id text not null,
    name text not null,
    code text not null,
    city text not null default '',
    main_campus boolean not null default false,
    updated_at timestamptz not null default now()
);

-- 3. USER ACCESS & DELEGATED PERMISSIONS (Primary Role Locked)
create table if not exists public.gsms_user_access_assignments (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    owner_id text not null,
    full_name text not null,
    email text not null,
    primary_role text not null,
    assigned_branch_ids text[] not null default '{branch_main}',
    assigned_classes text[] not null default '{}',
    assigned_subjects text[] not null default '{}',
    linked_student_ids text[] not null default '{}',
    extra_permissions text[] not null default '{}',
    active boolean not null default true,
    updated_by text,
    updated_at timestamptz not null default now()
);

-- 4. STUDENTS
create table if not exists public.gsms_students (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    admission_no text not null,
    full_name text not null,
    class_name text not null,
    stream text,
    parent_name text not null default '',
    parent_contact text,
    attendance_rate double precision not null default 95.0,
    average_score double precision not null default 80.0,
    fee_balance_usd double precision not null default 0.0,
    risk_level text not null default 'LOW',
    welfare_status text not null default 'Cleared',
    updated_at timestamptz not null default now()
);

-- 5. STAFF & FACULTY
create table if not exists public.gsms_staff (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    staff_no text not null,
    full_name text not null,
    role_title text not null,
    department text not null,
    subjects_summary text,
    leave_balance_days bigint not null default 21,
    leave_used_days bigint not null default 0,
    on_leave boolean not null default false,
    workload_hours bigint not null default 24,
    updated_at timestamptz not null default now()
);

-- 6. ACADEMICS, TIMETABLES, EXAMS & MARKS
create table if not exists public.gsms_academic_records (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    academic_year text not null default '2026/2027',
    term text not null default 'Term 1',
    owner_id text not null,
    record_type text not null,
    title text not null,
    class_name text not null,
    subject_name text,
    teacher_name text,
    schedule_slot text,
    score double precision not null default 0.0,
    max_score double precision not null default 100.0,
    grade text,
    updated_at timestamptz not null default now()
);

-- 7. ATTENDANCE
create table if not exists public.gsms_attendance (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    academic_year text not null default '2026/2027',
    term text not null default 'Term 1',
    owner_id text not null,
    target_type text not null default 'STUDENT',
    target_name text not null,
    class_name text,
    date_iso text not null,
    status text not null,
    updated_at timestamptz not null default now()
);

-- 8. REPORT TEMPLATES
create table if not exists public.gsms_report_templates (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    owner_id text not null,
    name text not null,
    class_level text not null default '',
    sections text[] not null default '{}',
    grading_scale text not null default 'A-F Percentage',
    active boolean not null default true,
    updated_at timestamptz not null default now()
);

-- 9. REPORT CARDS (Approval Workflow)
create table if not exists public.gsms_report_cards (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    academic_year text not null default '2026/2027',
    term text not null default 'Term 1',
    owner_id text not null,
    student_id text not null,
    student_name text not null,
    class_name text not null,
    template_id text not null,
    template_name text not null,
    subject_grades_summary text not null default '',
    average_score double precision not null default 0.0,
    teacher_remarks text,
    ai_summary text,
    headteacher_remarks text,
    status text not null default 'DRAFT',
    version bigint not null default 1,
    submitted_by text,
    reviewed_by text,
    updated_at timestamptz not null default now()
);

-- 10. FINANCE LEDGER (Invoices, Expenses, Payroll)
create table if not exists public.gsms_finance_entries (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    academic_year text not null default '2026/2027',
    term text not null default 'Term 1',
    owner_id text not null,
    entry_type text not null,
    reference_no text not null,
    title text not null,
    party_name text not null,
    category text not null default 'Tuition',
    amount_usd double precision not null default 0.0,
    paid_usd double precision not null default 0.0,
    status text not null default 'PENDING',
    updated_at timestamptz not null default now()
);

-- 11. LEAVE REQUESTS
create table if not exists public.gsms_leave_requests (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    staff_id text not null,
    staff_name text not null,
    role_title text not null,
    leave_type text not null,
    start_date text not null,
    end_date text not null,
    days_count bigint not null default 1,
    reason text not null,
    attachment_note text,
    status text not null default 'PENDING',
    reviewed_by text,
    ai_conflict_note text,
    updated_at timestamptz not null default now()
);

-- 12. COMMUNICATIONS
create table if not exists public.gsms_communications (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    channel text not null,
    audience text not null,
    title text not null,
    body text not null,
    sender_name text not null,
    sender_role text not null,
    ai_drafted boolean not null default false,
    updated_at timestamptz not null default now()
);

-- 13. OPERATIONS (Procurement, Stores, Library, Transport, Welfare, Counselling, Discipline, Hostel, Front Desk, ICT)
create table if not exists public.gsms_operations (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    module_type text not null,
    title text not null,
    subtitle text,
    party_name text,
    status text not null default 'ACTIVE',
    metric_label text,
    notes text,
    updated_at timestamptz not null default now()
);

-- 14. EARLY WARNING ALERTS
create table if not exists public.gsms_early_warning_alerts (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    branch_id text not null default 'branch_main',
    owner_id text not null,
    category text not null,
    risk_level text not null,
    target_name text not null,
    reason text not null,
    recommended_action text not null,
    assigned_role text not null,
    status text not null default 'OPEN',
    updated_at timestamptz not null default now()
);

-- 15. IMMUTABLE AUDIT LOGS
create table if not exists public.gsms_audit_logs (
    id text primary key,
    school_id text not null references public.gsms_schools(id) on delete cascade,
    owner_id text not null,
    actor_name text not null,
    actor_role text not null,
    action_type text not null,
    module_name text not null,
    summary text not null,
    ai_action boolean not null default false,
    created_at timestamptz not null default now()
);

-- ============================================================================
-- INDEXES FOR MULTI-TENANT PERFORMANCE
-- ============================================================================
create index if not exists idx_gsms_branches_school on public.gsms_branches(school_id);
create index if not exists idx_gsms_access_school on public.gsms_user_access_assignments(school_id);
create index if not exists idx_gsms_students_school_class on public.gsms_students(school_id, class_name);
create index if not exists idx_gsms_staff_school on public.gsms_staff(school_id);
create index if not exists idx_gsms_academics_school on public.gsms_academic_records(school_id, class_name);
create index if not exists idx_gsms_attendance_school on public.gsms_attendance(school_id, date_iso);
create index if not exists idx_gsms_reports_school_status on public.gsms_report_cards(school_id, status);
create index if not exists idx_gsms_finance_school on public.gsms_finance_entries(school_id, entry_type);
create index if not exists idx_gsms_leaves_school on public.gsms_leave_requests(school_id, status);
create index if not exists idx_gsms_ops_school_type on public.gsms_operations(school_id, module_type);
create index if not exists idx_gsms_alerts_school on public.gsms_early_warning_alerts(school_id, status);
create index if not exists idx_gsms_audit_school on public.gsms_audit_logs(school_id);

-- ============================================================================
-- STRICT ROW-LEVEL SECURITY (RLS) POLICIES
-- Authenticated Supabase users can access only rows belonging to schools they own
-- or schools where their email appears in active gsms_user_access_assignments.
-- ============================================================================
create or replace function public.gsms_can_access_school(target_school_id text)
returns boolean
language sql
stable
security definer
as $$
  select exists (
    select 1 from public.gsms_schools s
    where s.id = target_school_id
      and (
        s.supabase_owner_uid = auth.uid()
        or exists (
          select 1 from public.gsms_user_access_assignments a
          where a.school_id = s.id
            and a.active = true
            and lower(a.email) = lower(coalesce(auth.jwt() ->> 'email', ''))
        )
      )
  );
$$;

do $$
declare
  tbl text;
begin
  foreach tbl in array array[
    'gsms_schools', 'gsms_branches', 'gsms_user_access_assignments',
    'gsms_students', 'gsms_staff', 'gsms_academic_records', 'gsms_attendance',
    'gsms_report_templates', 'gsms_report_cards', 'gsms_finance_entries',
    'gsms_leave_requests', 'gsms_communications', 'gsms_operations',
    'gsms_early_warning_alerts', 'gsms_audit_logs'
  ] loop
    execute format('alter table public.%I enable row level security;', tbl);
  end loop;
end $$;

-- School tenant policy (authenticated Supabase session required)
drop policy if exists "gsms_schools_authenticated_rw" on public.gsms_schools;
create policy "gsms_schools_authenticated_rw" on public.gsms_schools
  for all to authenticated
  using (supabase_owner_uid = auth.uid() or public.gsms_can_access_school(id))
  with check (supabase_owner_uid = auth.uid());

-- Tenant subtable policies
do $$
declare
  tbl text;
begin
  foreach tbl in array array[
    'gsms_branches', 'gsms_user_access_assignments',
    'gsms_students', 'gsms_staff', 'gsms_academic_records', 'gsms_attendance',
    'gsms_report_templates', 'gsms_report_cards', 'gsms_finance_entries',
    'gsms_leave_requests', 'gsms_communications', 'gsms_operations',
    'gsms_early_warning_alerts'
  ] loop
    execute format('drop policy if exists "%s_tenant_isolation" on public.%I;', tbl, tbl);
    execute format(
      'create policy "%s_tenant_isolation" on public.%I for all to authenticated using (public.gsms_can_access_school(school_id)) with check (public.gsms_can_access_school(school_id));',
      tbl, tbl
    );
  end loop;
end $$;

-- Audit logs: append & read only (no deletes or updates)
drop policy if exists "gsms_audit_logs_read_insert" on public.gsms_audit_logs;
create policy "gsms_audit_logs_read_insert" on public.gsms_audit_logs
  for select to authenticated
  using (public.gsms_can_access_school(school_id));

drop policy if exists "gsms_audit_logs_insert_only" on public.gsms_audit_logs;
create policy "gsms_audit_logs_insert_only" on public.gsms_audit_logs
  for insert to authenticated
  with check (public.gsms_can_access_school(school_id));
