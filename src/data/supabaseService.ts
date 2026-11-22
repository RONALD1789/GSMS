import { RolePolicy } from '../policy/RolePolicy.ts';
import {
  AcademicRecord,
  AttendanceRecord,
  AuditLog,
  Branch,
  CommunicationItem,
  EarlyWarningAlert,
  FinanceEntry,
  GsmsWorkspaceState,
  LeaveRequest,
  OperationRecord,
  PortalRole,
  ReportCard,
  ReportTemplate,
  RoleCapability,
  School,
  StaffMember,
  Student,
  SupabaseSyncState,
  UserAccessAssignment
} from '../types/gsms.ts';

export interface SupabaseSession {
  userId: string;
  email: string;
  displayName: string;
  accessToken: string;
}

let inMemorySession: SupabaseSession | null = null;

function buildAuthHeaders(extra?: Record<string, string>): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...extra
  };
  if (inMemorySession?.accessToken) {
    headers.Authorization = `Bearer ${inMemorySession.accessToken}`;
  }
  return headers;
}

async function parseError(res: Response, operation: string): Promise<string> {
  let detail = '';
  let pgCode = '';
  try {
    const json = (await res.json()) as {
      message?: string;
      msg?: string;
      error_description?: string;
      error?: string;
      code?: string;
      hint?: string;
    };
    detail = json.error_description || json.message || json.msg || json.error || json.hint || '';
    pgCode = json.code || '';
  } catch {
    // ignore
  }
  if (res.status === 404 || pgCode === 'PGRST205') {
    return `${operation} failed: Supabase tables missing. Run supabase/schema.sql in your Supabase SQL Editor.`;
  }
  if (res.status === 401 || res.status === 403 || pgCode === '42501') {
    return `${operation} denied (HTTP ${res.status}): ${detail || 'Sign in with a valid email account or check RLS ownership.'}`;
  }
  return detail ? `${operation} failed (HTTP ${res.status}): ${detail}` : `${operation} failed with HTTP ${res.status}.`;
}

export function canSyncSupabase(role: PortalRole, extra: RoleCapability[] = []): boolean {
  return (
    role === PortalRole.SUPER_ADMIN ||
    role === PortalRole.SCHOOL_ADMIN ||
    role === PortalRole.ICT_ADMIN ||
    RolePolicy.has(role, RoleCapability.MANAGE_MODULES, extra) ||
    RolePolicy.has(role, RoleCapability.MANAGE_USER_PERMISSIONS, extra)
  );
}

export async function fetchSupabaseStatus(): Promise<Partial<SupabaseSyncState>> {
  try {
    const res = await fetch('/api/supabase/status');
    const data = (await res.json()) as {
      isConfigured: boolean;
      endpointLabel: string;
      configIssue: string | null;
    };
    return {
      isConfigured: data.isConfigured,
      endpointLabel: data.endpointLabel,
      configIssue: data.configIssue,
      connectionStatus: data.isConfigured ? 'Ready to connect' : 'Secrets needed'
    };
  } catch {
    return {
      isConfigured: false,
      endpointLabel: 'Offline',
      connectionStatus: 'Server unreachable'
    };
  }
}

export async function testSupabaseConnection(): Promise<{ message: string; latencyMs: number }> {
  const start = performance.now();
  const res = await fetch('/api/supabase/proxy/rest/v1/', {
    headers: buildAuthHeaders()
  });
  const latencyMs = Math.max(1, Math.round(performance.now() - start));
  if (!res.ok) {
    throw new Error(await parseError(res, 'Connection test'));
  }
  return {
    message: `Connected to Supabase PostgREST (${latencyMs}ms)`,
    latencyMs
  };
}

export async function signInSupabase(email: string, password: string): Promise<{ session: SupabaseSession | null; message: string }> {
  const cleanEmail = email.trim();
  if (!cleanEmail.includes('@')) throw new Error('Enter a valid email address.');
  if (password.length < 6) throw new Error('Password must be at least 6 characters.');

  const res = await fetch('/api/supabase/proxy/auth/v1/token?grant_type=password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ email: cleanEmail, password })
  });
  if (!res.ok) throw new Error(await parseError(res, 'Email Sign-In'));

  const data = (await res.json()) as {
    access_token?: string;
    user?: { id?: string; email?: string; user_metadata?: { full_name?: string } };
  };
  if (!data.access_token) {
    return { session: null, message: `Verification email sent to ${cleanEmail}. Confirm your email link, then sign in.` };
  }
  const verifiedEmail = data.user?.email || cleanEmail;
  const session: SupabaseSession = {
    userId: data.user?.id || cleanEmail.toLowerCase(),
    email: verifiedEmail,
    displayName: data.user?.user_metadata?.full_name || verifiedEmail.split('@')[0],
    accessToken: data.access_token
  };
  inMemorySession = session;
  return { session, message: `Signed in as ${verifiedEmail}` };
}

export async function signUpSupabase(
  email: string,
  password: string,
  fullName?: string
): Promise<{ session: SupabaseSession | null; message: string }> {
  const cleanEmail = email.trim();
  const cleanName = fullName?.trim();
  if (!cleanEmail.includes('@')) throw new Error('Enter a valid email address.');
  if (password.length < 6) throw new Error('Password must be at least 6 characters.');

  const res = await fetch('/api/supabase/proxy/auth/v1/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      email: cleanEmail,
      password,
      ...(cleanName ? { data: { full_name: cleanName } } : {})
    })
  });
  if (!res.ok) throw new Error(await parseError(res, 'Email Registration'));

  const data = (await res.json()) as {
    access_token?: string;
    user?: { id?: string; email?: string; user_metadata?: { full_name?: string } };
  };
  if (!data.access_token) {
    return { session: null, message: `Verification email sent to ${cleanEmail}. Confirm your email link, then sign in.` };
  }
  const verifiedEmail = data.user?.email || cleanEmail;
  const session: SupabaseSession = {
    userId: data.user?.id || cleanEmail.toLowerCase(),
    email: verifiedEmail,
    displayName: data.user?.user_metadata?.full_name || cleanName || verifiedEmail.split('@')[0],
    accessToken: data.access_token
  };
  inMemorySession = session;
  return { session, message: `Account created and signed in as ${verifiedEmail}` };
}

export async function sendSupabasePasswordReset(email: string): Promise<string> {
  const cleanEmail = email.trim();
  if (!cleanEmail.includes('@')) throw new Error('Enter your account email address first.');
  const res = await fetch('/api/supabase/proxy/auth/v1/recover', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ email: cleanEmail })
  });
  if (!res.ok) throw new Error(await parseError(res, 'Password reset'));
  return `Password reset email requested for ${cleanEmail}.`;
}

export async function signOutSupabase(): Promise<void> {
  const hadToken = Boolean(inMemorySession?.accessToken);
  if (hadToken) {
    try {
      await fetch('/api/supabase/proxy/auth/v1/logout', {
        method: 'POST',
        headers: buildAuthHeaders()
      });
    } catch {
      // ignore
    }
  }
  inMemorySession = null;
}

export function getActiveSupabaseSession(): SupabaseSession | null {
  return inMemorySession;
}

async function fetchCount(table: string, schoolId?: string): Promise<number> {
  const qs = schoolId ? `?select=id&school_id=eq.${encodeURIComponent(schoolId)}` : '?select=id';
  const res = await fetch(`/api/supabase/proxy/rest/v1/${table}${qs}`, {
    headers: buildAuthHeaders()
  });
  if (!res.ok) throw new Error(await parseError(res, `Query ${table}`));
  const rows = (await res.json()) as Array<{ id: string }>;
  return rows.length;
}

export async function pullSupabaseCounts(schoolId: string): Promise<Record<string, number>> {
  if (!inMemorySession) {
    throw new Error('Sign in with Supabase Auth first so RLS permits reading tenant rows.');
  }
  const pairs: Array<[string, string, boolean]> = [
    ['Schools', 'gsms_schools', false],
    ['Branches', 'gsms_branches', true],
    ['RBAC Grants', 'gsms_user_access_assignments', true],
    ['Students', 'gsms_students', true],
    ['Staff', 'gsms_staff', true],
    ['Academics', 'gsms_academic_records', true],
    ['Attendance', 'gsms_attendance', true],
    ['Templates', 'gsms_report_templates', true],
    ['Reports', 'gsms_report_cards', true],
    ['Finance', 'gsms_finance_entries', true],
    ['Leaves', 'gsms_leave_requests', true],
    ['Comms', 'gsms_communications', true],
    ['Operations', 'gsms_operations', true],
    ['Alerts', 'gsms_early_warning_alerts', true],
    ['Audit Logs', 'gsms_audit_logs', true]
  ];

  const counts: Record<string, number> = {};
  await Promise.all(
    pairs.map(async ([label, table, scoped]) => {
      counts[label] = await fetchCount(table, scoped ? schoolId : undefined);
    })
  );
  return counts;
}

async function postRows(table: string, rows: unknown[], ignoreDup = false): Promise<void> {
  if (rows.length === 0) return;
  const prefer = ignoreDup
    ? 'resolution=ignore-duplicates,return=minimal'
    : 'resolution=merge-duplicates,return=minimal';
  const res = await fetch(`/api/supabase/proxy/rest/v1/${table}`, {
    method: 'POST',
    headers: buildAuthHeaders({
      'Content-Type': 'application/json',
      Prefer: prefer
    }),
    body: JSON.stringify(rows)
  });
  if (!res.ok) throw new Error(await parseError(res, `Sync ${table}`));
}

export async function pushWorkspaceToSupabase(workspace: GsmsWorkspaceState): Promise<{
  summary: string;
  latencyMs: number;
  counts: Record<string, number>;
}> {
  if (!inMemorySession) {
    throw new Error('Sign in to Supabase Auth first so PostgreSQL RLS policies allow tenant writes.');
  }
  const activeSchool = workspace.activeSchool;
  if (!activeSchool) throw new Error('No active school tenant selected.');
  if (!canSyncSupabase(workspace.activeRole, workspace.activeExtraCapabilities)) {
    throw new Error(`Access denied: ${workspace.activeRole} cannot push tenant records to Supabase.`);
  }

  const start = performance.now();
  const ownerUid = workspace.userProfile?.userId || inMemorySession.userId;
  const sbUid = inMemorySession.userId;
  const schoolId = activeSchool.id;

  const schoolsToSync: School[] =
    workspace.activeRole === PortalRole.SUPER_ADMIN && workspace.schools.length > 0
      ? workspace.schools
      : [activeSchool];

  await postRows(
    'gsms_schools',
    schoolsToSync.map((s) => ({
      id: s.id,
      owner_id: ownerUid,
      supabase_owner_uid: sbUid,
      name: s.name,
      code: s.code,
      country: s.country,
      city: s.city,
      plan: s.plan,
      subscription_status: s.subscriptionStatus,
      brand_color_hex: s.brandColorHex,
      active_academic_year: s.activeAcademicYear,
      active_term: s.activeTerm,
      enabled_modules: s.enabledModules,
      student_count: s.studentCount,
      staff_count: s.staffCount,
      monthly_revenue_usd: s.monthlyRevenueUsd,
      churn_risk_score: s.churnRiskScore
    }))
  );

  await Promise.all([
    postRows(
      'gsms_branches',
      workspace.branches.map((b: Branch) => ({
        id: b.id,
        school_id: schoolId,
        owner_id: ownerUid,
        name: b.name,
        code: b.code,
        city: b.city,
        main_campus: b.mainCampus
      }))
    ),
    postRows(
      'gsms_user_access_assignments',
      workspace.accessAssignments.map((a: UserAccessAssignment) => ({
        id: a.id,
        school_id: schoolId,
        owner_id: ownerUid,
        full_name: a.fullName,
        email: a.email,
        primary_role: a.primaryRole,
        assigned_branch_ids: a.assignedBranchIds,
        assigned_classes: a.assignedClasses,
        assigned_subjects: a.assignedSubjects,
        linked_student_ids: a.linkedStudentIds,
        extra_permissions: a.extraPermissions,
        active: a.active,
        updated_by: a.updatedBy
      }))
    ),
    postRows(
      'gsms_students',
      workspace.students.map((st: Student) => ({
        id: st.id,
        school_id: schoolId,
        branch_id: st.branchId,
        owner_id: ownerUid,
        admission_no: st.admissionNo,
        full_name: st.fullName,
        class_name: st.className,
        stream: st.stream,
        parent_name: st.parentName,
        parent_contact: st.parentContact,
        attendance_rate: st.attendanceRate,
        average_score: st.averageScore,
        fee_balance_usd: st.feeBalanceUsd,
        risk_level: st.riskLevel,
        welfare_status: st.welfareStatus
      }))
    ),
    postRows(
      'gsms_staff',
      workspace.staff.map((sf: StaffMember) => ({
        id: sf.id,
        school_id: schoolId,
        branch_id: sf.branchId,
        owner_id: ownerUid,
        staff_no: sf.staffNo,
        full_name: sf.fullName,
        role_title: sf.roleTitle,
        department: sf.department,
        subjects_summary: sf.subjectsSummary,
        leave_balance_days: sf.leaveBalanceDays,
        leave_used_days: sf.leaveUsedDays,
        on_leave: sf.onLeave,
        workload_hours: sf.workloadHours
      }))
    ),
    postRows(
      'gsms_academic_records',
      workspace.academics.map((ac: AcademicRecord) => ({
        id: ac.id,
        school_id: schoolId,
        branch_id: ac.branchId,
        academic_year: ac.academicYear,
        term: ac.term,
        owner_id: ownerUid,
        record_type: ac.recordType,
        title: ac.title,
        class_name: ac.className,
        subject_name: ac.subjectName,
        teacher_name: ac.teacherName,
        schedule_slot: ac.scheduleSlot,
        score: ac.score,
        max_score: ac.maxScore,
        grade: ac.grade
      }))
    ),
    postRows(
      'gsms_attendance',
      workspace.attendance.map((at: AttendanceRecord) => ({
        id: at.id,
        school_id: schoolId,
        branch_id: at.branchId,
        academic_year: at.academicYear,
        term: at.term,
        owner_id: ownerUid,
        target_type: at.targetType,
        target_name: at.targetName,
        class_name: at.className,
        date_iso: at.dateIso,
        status: at.status
      }))
    ),
    postRows(
      'gsms_report_templates',
      workspace.reportTemplates.map((tp: ReportTemplate) => ({
        id: tp.id,
        school_id: schoolId,
        owner_id: ownerUid,
        name: tp.name,
        class_level: tp.classLevel,
        sections: tp.sections,
        grading_scale: tp.gradingScale,
        active: tp.active
      }))
    ),
    postRows(
      'gsms_report_cards',
      workspace.reportCards.map((rc: ReportCard) => ({
        id: rc.id,
        school_id: schoolId,
        branch_id: rc.branchId,
        academic_year: rc.academicYear,
        term: rc.term,
        owner_id: ownerUid,
        student_id: rc.studentId,
        student_name: rc.studentName,
        class_name: rc.className,
        template_id: rc.templateId,
        template_name: rc.templateName,
        subject_grades_summary: rc.subjectGradesSummary,
        average_score: rc.averageScore,
        teacher_remarks: rc.teacherRemarks,
        ai_summary: rc.aiSummary,
        headteacher_remarks: rc.headteacherRemarks,
        status: rc.status,
        version: rc.version,
        submitted_by: rc.submittedBy,
        reviewed_by: rc.reviewedBy
      }))
    ),
    postRows(
      'gsms_finance_entries',
      workspace.finance.map((fn: FinanceEntry) => ({
        id: fn.id,
        school_id: schoolId,
        branch_id: fn.branchId,
        academic_year: fn.academicYear,
        term: fn.term,
        owner_id: ownerUid,
        entry_type: fn.entryType,
        reference_no: fn.referenceNo,
        title: fn.title,
        party_name: fn.partyName,
        category: fn.category,
        amount_usd: fn.amountUsd,
        paid_usd: fn.paidUsd,
        status: fn.status
      }))
    ),
    postRows(
      'gsms_leave_requests',
      workspace.leaveRequests.map((lv: LeaveRequest) => ({
        id: lv.id,
        school_id: schoolId,
        branch_id: lv.branchId,
        owner_id: ownerUid,
        staff_id: lv.staffId,
        staff_name: lv.staffName,
        role_title: lv.roleTitle,
        leave_type: lv.leaveType,
        start_date: lv.startDate,
        end_date: lv.endDate,
        days_count: lv.daysCount,
        reason: lv.reason,
        attachment_note: lv.attachmentNote,
        status: lv.status,
        reviewed_by: lv.reviewedBy,
        ai_conflict_note: lv.aiConflictNote
      }))
    ),
    postRows(
      'gsms_communications',
      workspace.communications.map((cm: CommunicationItem) => ({
        id: cm.id,
        school_id: schoolId,
        branch_id: cm.branchId,
        owner_id: ownerUid,
        channel: cm.channel,
        audience: cm.audience,
        title: cm.title,
        body: cm.body,
        sender_name: cm.senderName,
        sender_role: cm.senderRole,
        ai_drafted: cm.aiDrafted
      }))
    ),
    postRows(
      'gsms_operations',
      workspace.operations.map((op: OperationRecord) => ({
        id: op.id,
        school_id: schoolId,
        branch_id: op.branchId,
        owner_id: ownerUid,
        module_type: op.moduleType,
        title: op.title,
        subtitle: op.subtitle,
        party_name: op.partyName,
        status: op.status,
        metric_label: op.metricLabel,
        notes: op.notes
      }))
    ),
    postRows(
      'gsms_early_warning_alerts',
      workspace.alerts.map((al: EarlyWarningAlert) => ({
        id: al.id,
        school_id: schoolId,
        branch_id: al.branchId,
        owner_id: ownerUid,
        category: al.category,
        risk_level: al.riskLevel,
        target_name: al.targetName,
        reason: al.reason,
        recommended_action: al.recommendedAction,
        assigned_role: al.assignedRole,
        status: al.status
      }))
    ),
    postRows(
      'gsms_audit_logs',
      workspace.auditLogs.map((au: AuditLog) => ({
        id: au.id,
        school_id: schoolId,
        owner_id: ownerUid,
        actor_name: au.actorName,
        actor_role: au.actorRole,
        action_type: au.actionType,
        module_name: au.moduleName,
        summary: au.summary,
        ai_action: au.aiAction
      })),
      true
    )
  ]);

  const totalSynced =
    schoolsToSync.length +
    workspace.branches.length +
    workspace.accessAssignments.length +
    workspace.students.length +
    workspace.staff.length +
    workspace.academics.length +
    workspace.attendance.length +
    workspace.reportTemplates.length +
    workspace.reportCards.length +
    workspace.finance.length +
    workspace.leaveRequests.length +
    workspace.communications.length +
    workspace.operations.length +
    workspace.alerts.length +
    workspace.auditLogs.length;

  const latencyMs = Math.max(1, Math.round(performance.now() - start));
  const counts = await pullSupabaseCounts(schoolId);
  return {
    summary: `Synced ${totalSynced} records for ${activeSchool.name} to Supabase PostgreSQL`,
    latencyMs,
    counts
  };
}
