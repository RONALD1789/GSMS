import { RolePolicy } from '../policy/RolePolicy.ts';
import {
  GsmsWorkspaceState,
  LeaveStatus,
  PORTAL_ROLE_META,
  PortalRole,
  ReportStatus,
  RiskLevel,
  School
} from '../types/gsms.ts';

export function buildRoleScopedContext(
  role: PortalRole,
  activeSchool: School | null,
  dataSummary: String | string
): string {
  const meta = PORTAL_ROLE_META[role];
  const schoolHeader = activeSchool
    ? `School: ${activeSchool.name} (${activeSchool.code}), ${activeSchool.city}, ${activeSchool.country}. Academic Year: ${activeSchool.activeAcademicYear}, Term: ${activeSchool.activeTerm}. Enabled Modules: ${activeSchool.enabledModules.join(', ')}.`
    : 'Multi-School Platform Scope.';

  return [
    'You are the Global School Management System (GSMS) AI Assistant.',
    `Active Portal: ${meta.displayName}`,
    `Role Scope: ${meta.scopeDescription}`,
    `Tenant Context: ${schoolHeader}`,
    '',
    'STRICT GOVERNANCE RULES:',
    '1. Only reference facts present in the Verified School Data Context below. Never invent student names, grades, or financial figures.',
    `2. Respect role permissions for ${meta.displayName}. Do not expose data outside this role's scope.`,
    '3. Provide crisp, structured, actionable institutional output.',
    '',
    'Verified School Data Context:',
    String(dataSummary)
  ].join('\n');
}

export function buildRoleScopedSummary(
  w: GsmsWorkspaceState,
  focusStudentId: string | null
): string {
  const school = w.activeSchool;
  const lines: string[] = [];

  switch (w.activeRole) {
    case PortalRole.SUPER_ADMIN: {
      lines.push(`Platform Schools (${w.schools.length}):`);
      w.schools.forEach((s) => {
        lines.push(
          `- ${s.name} (${s.city}, ${s.country}): ${s.studentCount} students, ${s.staffCount} staff, USD ${Math.round(s.monthlyRevenueUsd)}/mo, status=${s.subscriptionStatus}, churnRisk=${s.churnRiskScore}%`
        );
      });
      break;
    }
    case PortalRole.SCHOOL_ADMIN:
    case PortalRole.HEADTEACHER:
    case PortalRole.DEPUTY_HEADTEACHER: {
      lines.push(
        `School: ${school?.name ?? 'None'}, Students: ${w.students.length} tracked (total ${school?.studentCount ?? 0}), Staff: ${w.staff.length}`
      );
      const atRisk = w.students.filter(
        (s) => s.riskLevel === RiskLevel.HIGH || s.riskLevel === RiskLevel.CRITICAL
      );
      lines.push(
        `At-Risk Students: ${atRisk.map((s) => `${s.fullName} (${s.riskLevel}, Avg ${s.averageScore}%, Att ${s.attendanceRate}%)`).join(', ') || 'None'}`
      );
      lines.push(
        `Reports: Draft=${w.reportCards.filter((r) => r.status === ReportStatus.DRAFT).length}, Submitted=${w.reportCards.filter((r) => r.status === ReportStatus.SUBMITTED).length}, Published=${w.reportCards.filter((r) => r.status === ReportStatus.PUBLISHED).length}`
      );
      lines.push(
        `Pending Leaves: ${w.leaveRequests.filter((l) => l.status === LeaveStatus.PENDING).length}, Open Alerts: ${w.alerts.filter((a) => a.status !== 'RESOLVED').length}`
      );
      break;
    }
    case PortalRole.TEACHER:
    case PortalRole.CLASS_TEACHER:
    case PortalRole.EXAMINATION_OFFICER: {
      const scopedAcad = RolePolicy.filterAcademicsForRole(
        w.activeRole,
        w.academics,
        w.assignedClasses,
        w.assignedSubjects
      );
      const scopedStudents = RolePolicy.filterStudentsForRole(
        w.activeRole,
        w.students,
        w.assignedClasses,
        w.linkedStudentIds
      );
      lines.push(
        `School: ${school?.name ?? 'None'}, Timetable & Exams: ${scopedAcad.slice(0, 6).map((a) => `${a.title} (${a.className})`).join(', ')}`
      );
      lines.push(
        `Students (${scopedStudents.length}): ${scopedStudents.slice(0, 6).map((s) => `${s.fullName} (${s.className}, Avg ${s.averageScore}%, Att ${s.attendanceRate}%)`).join(', ')}`
      );
      lines.push(`Report Cards Tracked: ${w.reportCards.length}`);
      break;
    }
    case PortalRole.STUDENT: {
      const focusStudent = RolePolicy.resolveFocusStudent(w.students, focusStudentId);
      if (focusStudent) {
        lines.push(
          `Student Focus: ${focusStudent.fullName} (${focusStudent.className}), Avg ${focusStudent.averageScore}%, Attendance ${focusStudent.attendanceRate}%`
        );
      }
      w.reportCards
        .filter((r) => r.status === ReportStatus.PUBLISHED && (!focusStudent || r.studentId === focusStudent.id))
        .forEach((r) => {
          lines.push(`Published Report: ${r.studentName} (${r.term}) Avg ${r.averageScore}% - ${r.subjectGradesSummary}`);
        });
      break;
    }
    case PortalRole.PARENT: {
      const focusStudent = RolePolicy.resolveFocusStudent(w.students, focusStudentId);
      if (focusStudent) {
        lines.push(
          `Child Focus: ${focusStudent.fullName} (${focusStudent.className}), Avg ${focusStudent.averageScore}%, Attendance ${focusStudent.attendanceRate}%, Fee Balance USD ${Math.round(focusStudent.feeBalanceUsd)}`
        );
      }
      w.reportCards
        .filter((r) => r.status === ReportStatus.PUBLISHED && (!focusStudent || r.studentId === focusStudent.id))
        .forEach((r) => {
          lines.push(`Published Report: ${r.studentName} (${r.term}) Avg ${r.averageScore}% - ${r.subjectGradesSummary}`);
        });
      break;
    }
    case PortalRole.FINANCE_OFFICER: {
      const billed = w.finance.reduce((acc, f) => acc + f.amountUsd, 0);
      const paid = w.finance.reduce((acc, f) => acc + f.paidUsd, 0);
      lines.push(
        `Finance Ledger: Total Billed USD ${Math.round(billed)}, Total Paid USD ${Math.round(paid)}, Outstanding USD ${Math.round(billed - paid)}`
      );
      w.finance.slice(0, 8).forEach((f) => {
        lines.push(`- [${f.entryType}] ${f.referenceNo} ${f.title}: USD ${Math.round(f.paidUsd)} / USD ${Math.round(f.amountUsd)} (${f.status})`);
      });
      break;
    }
    case PortalRole.HR_MANAGER: {
      const onLeave = w.staff.filter((s) => s.onLeave);
      lines.push(
        `Staff Count: ${w.staff.length}, Currently On Leave: ${onLeave.length}, Pending Leaves: ${w.leaveRequests.filter((l) => l.status === LeaveStatus.PENDING).length}`
      );
      w.leaveRequests.forEach((l) => {
        lines.push(
          `- Leave: ${l.staffName} (${l.leaveType}, ${l.daysCount}d, ${l.startDate}..${l.endDate}) status=${l.status}, note=${l.aiConflictNote ?? 'None'}`
        );
      });
      break;
    }
    case PortalRole.LIBRARIAN: {
      const ops = w.operations.filter((o) => ['LIBRARY_BOOK', 'LIBRARY_BORROW'].includes(o.moduleType));
      lines.push(`Library Records (${ops.length}): ${ops.map((o) => `${o.title} [${o.status}]`).join(', ')}`);
      break;
    }
    case PortalRole.TRANSPORT_MANAGER: {
      const ops = w.operations.filter((o) => ['VEHICLE', 'ROUTE', 'TRIP'].includes(o.moduleType));
      lines.push(`Transport Fleet & Routes (${ops.length}): ${ops.map((o) => `${o.title} [${o.status}]`).join(', ')}`);
      break;
    }
    case PortalRole.WELFARE_NURSE:
    case PortalRole.SCHOOL_COUNSELLOR: {
      const ops = w.operations.filter((o) => ['WELFARE', 'COUNSELLING', 'DISCIPLINE', 'HOSTEL'].includes(o.moduleType));
      lines.push(`Welfare & Safeguarding (${ops.length}): ${ops.map((o) => `${o.title} [${o.status}]`).join(', ')}`);
      const alerts = RolePolicy.filterAlertsForRole(w.activeRole, w.alerts);
      lines.push(`Alerts: ${alerts.map((a) => `${a.targetName}: ${a.reason}`).join(', ') || 'None'}`);
      break;
    }
    case PortalRole.ADMISSIONS_RECEPTION: {
      const ops = w.operations.filter((o) => ['FRONT_DESK', 'ADMISSION', 'EVENT'].includes(o.moduleType));
      lines.push(`Admissions, Front Desk & Events (${ops.length}): ${ops.map((o) => `${o.title} [${o.status}]`).join(', ')}`);
      lines.push(`Enrolled Students Tracked: ${w.students.length}`);
      break;
    }
    case PortalRole.PROCUREMENT_OFFICER:
    case PortalRole.STOREKEEPER: {
      const ops = w.operations.filter((o) => ['PROCUREMENT', 'INVENTORY', 'ASSET', 'STOCK_MOVEMENT'].includes(o.moduleType));
      lines.push(`Procurement & Stores (${ops.length}): ${ops.map((o) => `${o.title} (${o.metricLabel ?? ''}) [${o.status}]`).join(', ')}`);
      break;
    }
    case PortalRole.ICT_ADMIN: {
      const ops = w.operations.filter((o) => ['ICT_TICKET', 'ASSET', 'INVENTORY'].includes(o.moduleType));
      lines.push(`ICT Systems & Assets (${ops.length}): ${ops.map((o) => `${o.title} [${o.status}]`).join(', ')}`);
      lines.push(`RBAC Access Grants: ${w.accessAssignments.length}`);
      break;
    }
  }

  return lines.join('\n');
}

export async function streamAssistantResponse(
  role: PortalRole,
  activeSchool: School | null,
  dataSummary: string,
  userPrompt: string,
  onChunk: (accumulatedText: string) => void
): Promise<string> {
  const fullPrompt = `${buildRoleScopedContext(role, activeSchool, dataSummary)}\n\nUser Request:\n${userPrompt}`;
  const response = await fetch('/api/ai/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt: fullPrompt })
  });

  if (!response.ok) {
    let errorMsg = `AI request failed (${response.status})`;
    try {
      const errJson = (await response.json()) as { error?: string };
      if (errJson.error) errorMsg = errJson.error;
    } catch {
      // ignore json parse failure
    }
    throw new Error(errorMsg);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('Streaming response body unavailable.');
  }

  const decoder = new TextDecoder();
  let buffer = '';
  let accumulated = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split('\n\n');
    buffer = events.pop() ?? '';

    for (const evt of events) {
      const line = evt.trim();
      if (!line.startsWith('data:')) continue;
      const payload = line.slice(5).trim();
      if (payload === '[DONE]') continue;
      try {
        const parsed = JSON.parse(payload) as { text?: string; error?: string };
        if (parsed.error) throw new Error(parsed.error);
        if (parsed.text) {
          accumulated += parsed.text;
          onChunk(accumulated);
        }
      } catch (err) {
        if (err instanceof Error && err.message !== 'Unexpected end of JSON input') {
          throw err;
        }
      }
    }
  }

  if (!accumulated.trim()) {
    throw new Error('Gemini returned an empty response.');
  }
  return accumulated;
}

export async function generateWorkflowText(
  role: PortalRole,
  activeSchool: School | null,
  dataSummary: string,
  taskPrompt: string
): Promise<string> {
  const fullPrompt = `${buildRoleScopedContext(role, activeSchool, dataSummary)}\n\nTask:\n${taskPrompt}`;
  const response = await fetch('/api/ai/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt: fullPrompt })
  });
  const data = (await response.json()) as { text?: string; error?: string };
  if (!response.ok || !data.text) {
    throw new Error(data.error || `AI generation failed (${response.status})`);
  }
  return data.text;
}
