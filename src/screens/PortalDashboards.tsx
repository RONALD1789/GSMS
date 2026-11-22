import React from 'react';
import {
  AlertTriangle,
  Award,
  BookOpen,
  Building2,
  Bus,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  DollarSign,
  Download,
  FileText,
  GraduationCap,
  HeartPulse,
  Laptop,
  Package,
  ShieldAlert,
  ShoppingCart,
  UserCheck,
  Users
} from 'lucide-react';
import {
  AiCopilotBannerCard,
  BarChartCard,
  EmptyStateCard,
  KpiMetricCard,
  LearnerScopePicker,
  SectionHeader,
  StatusBadge,
  TrendSparklineCard,
  riskTone
} from '../components/UiPrimitives.tsx';
import { exportReportCardPdf } from '../data/pdfExport.ts';
import { RolePolicy } from '../policy/RolePolicy.ts';
import { useGsms } from '../state/GsmsContext.tsx';
import {
  GsmsModule,
  GsmsTab,
  LeaveStatus,
  PORTAL_ROLE_META,
  PortalRole,
  ReportStatus,
  schoolHasModule
} from '../types/gsms.ts';

interface Props {
  onNavigateTab: (tab: GsmsTab) => void;
}

export const PortalDashboards: React.FC<Props> = ({ onNavigateTab }) => {
  const {
    workspace,
    focusStudentId,
    selectFocusStudent,
    askAiCopilot,
    showBanner
  } = useGsms();

  if (!workspace) return null;

  const { activeRole, activeSchool, userProfile, activeExtraCapabilities } = workspace;
  const lowDataMode = userProfile?.lowDataMode ?? false;
  const aiEnabled = schoolHasModule(activeSchool, GsmsModule.AI_TOOLS);
  const roleMeta = PORTAL_ROLE_META[activeRole];

  const handleQuickAi = (capability: string) => {
    void askAiCopilot(capability);
    onNavigateTab(GsmsTab.AI_GOVERNANCE);
  };

  const handleDownloadPdf = (reportId: string) => {
    if (!activeSchool) return;
    const report = workspace.reportCards.find((r) => r.id === reportId);
    if (!report) return;
    const filename = exportReportCardPdf(activeSchool, report);
    showBanner(`Exported ${filename}`);
  };

  // Metrics shared across dashboards
  const totalStudents = workspace.students.length;
  const totalStaff = workspace.staff.length;
  const avgAttendance =
    totalStudents > 0
      ? Math.round(workspace.students.reduce((s, st) => s + st.attendanceRate, 0) / totalStudents)
      : 0;
  const avgScore =
    totalStudents > 0
      ? Math.round(workspace.students.reduce((s, st) => s + st.averageScore, 0) / totalStudents)
      : 0;

  const openAlerts = workspace.alerts.filter((a) => a.status !== 'RESOLVED');
  const pendingReports = workspace.reportCards.filter((r) => r.status === ReportStatus.SUBMITTED);
  const pendingLeaves = workspace.leaveRequests.filter((l) => l.status === LeaveStatus.PENDING);

  const invoiceEntries = workspace.finance.filter((f) => f.entryType === 'INVOICE');
  const billedUsd = invoiceEntries.reduce((s, f) => s + f.amountUsd, 0);
  const collectedUsd = invoiceEntries.reduce((s, f) => s + f.paidUsd, 0);
  const collectionPct = billedUsd > 0 ? Math.round((collectedUsd / billedUsd) * 100) : 0;

  const totalSaaSMrr = workspace.schools.reduce((s, sc) => s + sc.monthlyRevenueUsd, 0);
  const totalGlobalStudents = workspace.schools.reduce((s, sc) => s + sc.studentCount, 0);

  const scopedStudents = RolePolicy.filterStudentsForRole(
    activeRole,
    workspace.students,
    workspace.assignedClasses,
    workspace.linkedStudentIds
  );
  const requiresLearner = RolePolicy.requiresLearnerScope(activeRole, activeExtraCapabilities);
  const focusStudent = RolePolicy.resolveFocusStudent(scopedStudents, focusStudentId);

  const roleAlerts = RolePolicy.filterAlertsForRole(activeRole, workspace.alerts).filter(
    (a) => a.status !== 'RESOLVED'
  );

  const renderRoleBody = () => {
    // 1. SUPER_ADMIN
    if (activeRole === PortalRole.SUPER_ADMIN) {
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label="Active School Tenants"
              value={String(workspace.schools.length)}
              delta="LIVE SAAS"
              icon={<Building2 className="w-4 h-4" />}
              tone="cobalt"
            />
            <KpiMetricCard
              label="Monthly Recurring Revenue"
              value={`$${totalSaaSMrr.toLocaleString()}`}
              delta="+14% YoY"
              icon={<DollarSign className="w-4 h-4" />}
              tone="emerald"
            />
            <KpiMetricCard
              label="Global Enrolled Learners"
              value={totalGlobalStudents.toLocaleString()}
              delta="3 REGIONS"
              icon={<GraduationCap className="w-4 h-4" />}
              tone="violet"
            />
            <KpiMetricCard
              label="Open Risk Alerts"
              value={String(openAlerts.length)}
              delta="ACTIVE"
              icon={<AlertTriangle className="w-4 h-4" />}
              tone="amber"
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <BarChartCard
              title="Tenant Capacity & Health"
              subtitle="Normalized enrollment utilization across institutions"
              tone="cobalt"
              bars={workspace.schools.map((s) => ({
                label: s.code,
                value: Math.min(100, Math.round(s.studentCount / 15))
              }))}
            />
            <TrendSparklineCard
              title="Platform Uptime & Adoption"
              headlineValue="99.94%"
              points={[94, 96, 97, 98, 99, 100]}
              lineColor="#059669"
            />
          </div>

          <SectionHeader
            title="Institution Portfolio"
            subtitle="Multi-tenant subscriptions and active academic periods"
            actionLabel="Governance"
            onAction={() => onNavigateTab(GsmsTab.AI_GOVERNANCE)}
          />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {workspace.schools.map((sc) => (
              <div
                key={sc.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs space-y-2"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-bold text-[#0F172A] truncate">{sc.name}</span>
                  <StatusBadge
                    text={sc.plan}
                    tone={sc.id === activeSchool?.id ? 'emerald' : 'slate'}
                  />
                </div>
                <div className="text-xs text-[#475569]">
                  {sc.city}, {sc.country} • {sc.activeAcademicYear} ({sc.activeTerm})
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-[#E8EEF5] text-xs font-mono-num tabular-nums">
                  <span>{sc.studentCount} learners</span>
                  <span className="font-bold text-[#059669]">${sc.monthlyRevenueUsd}/mo</span>
                </div>
              </div>
            ))}
          </div>
        </>
      );
    }

    // 2. SCHOOL_ADMIN
    if (activeRole === PortalRole.SCHOOL_ADMIN) {
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label="Active Students"
              value={String(activeSchool?.studentCount ?? totalStudents)}
              delta={`${workspace.branches.length} Campuses`}
              icon={<GraduationCap className="w-4 h-4" />}
              tone="cobalt"
            />
            <KpiMetricCard
              label="Fee Collection Rate"
              value={`${collectionPct}%`}
              delta={`$${Math.round(collectedUsd).toLocaleString()}`}
              icon={<DollarSign className="w-4 h-4" />}
              tone="emerald"
            />
            <KpiMetricCard
              label="Pending Leaves & Reports"
              value={String(pendingLeaves.length + pendingReports.length)}
              delta="ACTION REQ"
              icon={<ClipboardCheck className="w-4 h-4" />}
              tone="amber"
            />
            <KpiMetricCard
              label="Enabled Modules"
              value={`${activeSchool?.enabledModules.length ?? 0}/21`}
              delta="REGISTRY"
              icon={<Building2 className="w-4 h-4" />}
              tone="violet"
            />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <BarChartCard
              title="Student Average Mastery"
              subtitle="Academic score comparison across sampled cohort"
              tone="cobalt"
              bars={workspace.students.map((st) => ({
                label: st.fullName.split(' ')[0],
                value: st.averageScore
              }))}
            />
            <TrendSparklineCard
              title="Term Attendance Trajectory"
              headlineValue={`${avgAttendance}%`}
              points={[89, 91, 90, 93, avgAttendance || 94]}
              lineColor="#059669"
            />
          </div>
        </>
      );
    }

    // 3. HEADTEACHER & 4. DEPUTY_HEADTEACHER
    if (activeRole === PortalRole.HEADTEACHER || activeRole === PortalRole.DEPUTY_HEADTEACHER) {
      const isDeputy = activeRole === PortalRole.DEPUTY_HEADTEACHER;
      const disciplineOps = workspace.operations.filter(
        (o) => o.moduleType === 'DISCIPLINE' || o.moduleType === 'COUNSELLING'
      );
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label="Reports Awaiting Review"
              value={String(pendingReports.length)}
              delta="APPROVAL QUEUE"
              icon={<FileText className="w-4 h-4" />}
              tone="amber"
            />
            <KpiMetricCard
              label="Cohort Academic Mean"
              value={`${avgScore}%`}
              delta={`${totalStudents} learners`}
              icon={<Award className="w-4 h-4" />}
              tone="emerald"
            />
            <KpiMetricCard
              label={isDeputy ? 'Pastoral & Discipline Cases' : 'Early-Warning Interventions'}
              value={String(isDeputy ? disciplineOps.length : openAlerts.length)}
              delta={isDeputy ? 'DEPUTY SCOPE' : 'HIGH PRIORITY'}
              icon={<ShieldAlert className="w-4 h-4" />}
              tone="crimson"
            />
            <KpiMetricCard
              label="Staff Cover Requests"
              value={String(pendingLeaves.length)}
              delta={`${totalStaff} faculty`}
              icon={<Users className="w-4 h-4" />}
              tone="cobalt"
            />
          </div>

          <SectionHeader
            title="Report Approval Queue"
            subtitle="Submitted term report cards awaiting principal sign-off"
            actionLabel="Open Reports"
            onAction={() => onNavigateTab(GsmsTab.ACADEMICS)}
          />
          {pendingReports.length === 0 ? (
            <EmptyStateCard
              icon={<CheckCircle2 className="w-5 h-5" />}
              title="All Submitted Reports Reviewed"
              description="No pending report cards are waiting for endorsement right now."
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {pendingReports.map((rc) => (
                <div
                  key={rc.id}
                  className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-center justify-between gap-3"
                >
                  <div className="min-w-0">
                    <div className="text-sm font-bold text-[#0F172A] truncate">
                      {rc.studentName} • {rc.className}
                    </div>
                    <div className="text-xs text-[#475569] truncate">
                      Avg <span className="font-mono-num font-semibold">{rc.averageScore.toFixed(1)}%</span> •{' '}
                      {rc.subjectGradesSummary}
                    </div>
                  </div>
                  <StatusBadge text={rc.status} tone="amber" />
                </div>
              ))}
            </div>
          )}
        </>
      );
    }

    // 5. TEACHER & 6. CLASS_TEACHER
    if (activeRole === PortalRole.TEACHER || activeRole === PortalRole.CLASS_TEACHER) {
      const scopedAcademics = RolePolicy.filterAcademicsForRole(
        activeRole,
        workspace.academics,
        workspace.assignedClasses,
        workspace.assignedSubjects
      );
      const timetableItems = scopedAcademics.filter((a) => a.recordType === 'TIMETABLE');
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label={activeRole === PortalRole.CLASS_TEACHER ? 'Homeroom Learners' : 'Assigned Learners'}
              value={String(scopedStudents.length)}
              delta={workspace.assignedClasses.join(', ') || 'ALL'}
              icon={<Users className="w-4 h-4" />}
              tone="cobalt"
            />
            <KpiMetricCard
              label="Scheduled Sessions"
              value={String(timetableItems.length)}
              delta="TIMETABLE"
              icon={<CalendarClock className="w-4 h-4" />}
              tone="violet"
            />
            <KpiMetricCard
              label="Draft / Returned Reports"
              value={String(
                workspace.reportCards.filter(
                  (r) => r.status === ReportStatus.DRAFT || r.status === ReportStatus.RETURNED
                ).length
              )}
              delta="ACTION"
              icon={<FileText className="w-4 h-4" />}
              tone="amber"
            />
            <KpiMetricCard
              label="Cohort Attendance"
              value={`${avgAttendance}%`}
              delta="ROLL CALL"
              icon={<UserCheck className="w-4 h-4" />}
              tone="emerald"
            />
          </div>

          <SectionHeader
            title="Upcoming Teaching Timetable"
            subtitle="Scoped to your assigned classes and subjects"
            actionLabel="Mark Attendance"
            onAction={() => onNavigateTab(GsmsTab.ACADEMICS)}
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {scopedAcademics.slice(0, 4).map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="text-sm font-bold text-[#0F172A] truncate">{item.title}</div>
                  <div className="text-xs text-[#475569] truncate">
                    {item.className} • {item.subjectName} • {item.scheduleSlot}
                  </div>
                </div>
                <StatusBadge text={item.recordType} tone="cobalt" />
              </div>
            ))}
          </div>
        </>
      );
    }

    // 7. PARENT & 8. STUDENT (strict learner scoping)
    if (activeRole === PortalRole.PARENT || activeRole === PortalRole.STUDENT) {
      const publishedReports = RolePolicy.filterReportCardsForRole(
        activeRole,
        workspace.reportCards,
        scopedStudents,
        focusStudent
      );
      const invoices =
        activeRole === PortalRole.PARENT && focusStudent
          ? RolePolicy.filterFinanceForRole(activeRole, workspace.finance, focusStudent)
          : [];
      const childBalance = invoices.reduce((s, f) => s + Math.max(0, f.amountUsd - f.paidUsd), 0);

      return (
        <>
          {requiresLearner ? (
            <LearnerScopePicker
              students={scopedStudents}
              focusStudentId={focusStudentId}
              onSelect={selectFocusStudent}
            />
          ) : null}

          {!focusStudent ? (
            <EmptyStateCard
              icon={<GraduationCap className="w-5 h-5" />}
              title="Select a Learner Profile"
              description={
                activeRole === PortalRole.PARENT
                  ? 'Select a linked child above to view attendance, published term reports, and fee balances.'
                  : 'Select your learner profile above to view your timetable, published results, and AI study tools.'
              }
            />
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <KpiMetricCard
                  label="Attendance Rate"
                  value={`${focusStudent.attendanceRate}%`}
                  delta={focusStudent.className}
                  icon={<UserCheck className="w-4 h-4" />}
                  tone="emerald"
                />
                <KpiMetricCard
                  label="Academic Average"
                  value={`${focusStudent.averageScore}%`}
                  delta={focusStudent.welfareStatus}
                  icon={<Award className="w-4 h-4" />}
                  tone="cobalt"
                />
                <KpiMetricCard
                  label="Published Reports"
                  value={String(publishedReports.length)}
                  delta="PDF READY"
                  icon={<FileText className="w-4 h-4" />}
                  tone="violet"
                />
                {activeRole === PortalRole.PARENT ? (
                  <KpiMetricCard
                    label="Outstanding Fee Balance"
                    value={`$${childBalance.toLocaleString()}`}
                    delta={childBalance > 0 ? 'DUE' : 'CLEARED'}
                    icon={<DollarSign className="w-4 h-4" />}
                    tone={childBalance > 0 ? 'amber' : 'emerald'}
                  />
                ) : (
                  <KpiMetricCard
                    label="Active Class Stream"
                    value={focusStudent.stream || 'Core'}
                    delta={focusStudent.admissionNo}
                    icon={<BookOpen className="w-4 h-4" />}
                    tone="cobalt"
                  />
                )}
              </div>

              <SectionHeader
                title={`Published Report Cards • ${focusStudent.fullName}`}
                subtitle="Officially endorsed report cards available for PDF download"
              />
              {publishedReports.length === 0 ? (
                <EmptyStateCard
                  icon={<FileText className="w-5 h-5" />}
                  title="No Published Reports Yet"
                  description="Reports appear here once teachers submit and the Headteacher publishes them."
                />
              ) : (
                <div className="space-y-3">
                  {publishedReports.map((rc) => (
                    <div
                      key={rc.id}
                      className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex flex-wrap items-center justify-between gap-3"
                    >
                      <div className="min-w-0">
                        <div className="text-sm font-bold text-[#0F172A]">
                          {rc.templateName} ({rc.academicYear} • {rc.term})
                        </div>
                        <div className="text-xs text-[#475569] mt-0.5">
                          Average <span className="font-mono-num font-semibold">{rc.averageScore.toFixed(1)}%</span> •{' '}
                          {rc.subjectGradesSummary}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => handleDownloadPdf(rc.id)}
                        className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#0F2537] hover:bg-[#091826] text-white text-xs font-semibold whitespace-nowrap shrink-0 cursor-pointer"
                      >
                        <Download className="w-3.5 h-3.5" />
                        Download PDF
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </>
      );
    }

    // 9. FINANCE_OFFICER
    if (activeRole === PortalRole.FINANCE_OFFICER) {
      const arrears = Math.max(0, billedUsd - collectedUsd);
      const expenseTotal = workspace.finance
        .filter((f) => f.entryType === 'EXPENSE')
        .reduce((s, f) => s + f.amountUsd, 0);
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label="Term Fee Collections"
              value={`$${Math.round(collectedUsd).toLocaleString()}`}
              delta={`${collectionPct}% Collected`}
              icon={<DollarSign className="w-4 h-4" />}
              tone="emerald"
            />
            <KpiMetricCard
              label="Outstanding Fee Arrears"
              value={`$${Math.round(arrears).toLocaleString()}`}
              delta="RECEIVABLE"
              icon={<AlertTriangle className="w-4 h-4" />}
              tone="amber"
            />
            <KpiMetricCard
              label="Logged Expenses"
              value={`$${Math.round(expenseTotal).toLocaleString()}`}
              delta="BUDGET"
              icon={<ShoppingCart className="w-4 h-4" />}
              tone="cobalt"
            />
            <KpiMetricCard
              label="Finance Risk Alerts"
              value={String(roleAlerts.length)}
              delta="WATCHLIST"
              icon={<ShieldAlert className="w-4 h-4" />}
              tone="crimson"
            />
          </div>
          <BarChartCard
            title="Invoice Settlement Progress"
            subtitle="Paid percentage per active fee ledger invoice"
            tone="emerald"
            bars={invoiceEntries.map((inv) => ({
              label: inv.partyName.split(' ')[0],
              value: inv.amountUsd > 0 ? Math.round((inv.paidUsd / inv.amountUsd) * 100) : 0
            }))}
          />
        </>
      );
    }

    // 10. HR_MANAGER
    if (activeRole === PortalRole.HR_MANAGER) {
      const onLeaveStaff = workspace.staff.filter((s) => s.onLeave).length;
      const payrollSum = workspace.finance
        .filter((f) => f.entryType === 'PAYROLL')
        .reduce((s, f) => s + f.amountUsd, 0);
      return (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiMetricCard
              label="Active Faculty & Staff"
              value={String(totalStaff)}
              delta="DIRECTORY"
              icon={<Users className="w-4 h-4" />}
              tone="cobalt"
            />
            <KpiMetricCard
              label="Pending Leave Requests"
              value={String(pendingLeaves.length)}
              delta="APPROVAL"
              icon={<CalendarClock className="w-4 h-4" />}
              tone="amber"
            />
            <KpiMetricCard
              label="Currently On Leave"
              value={String(onLeaveStaff)}
              delta="COVER CHECK"
              icon={<UserCheck className="w-4 h-4" />}
              tone="crimson"
            />
            <KpiMetricCard
              label="Monthly Payroll Run"
              value={`$${Math.round(payrollSum).toLocaleString()}`}
              delta="HR PAYROLL"
              icon={<DollarSign className="w-4 h-4" />}
              tone="emerald"
            />
          </div>
          <BarChartCard
            title="Staff Remaining Leave Days"
            subtitle="Available annual leave balance across faculty"
            tone="violet"
            bars={workspace.staff.map((st) => ({
              label: st.fullName.split(' ')[0],
              value: st.leaveBalanceDays
            }))}
          />
        </>
      );
    }

    // 11-19 Specialist Portals
    const specialistConfig = (() => {
      switch (activeRole) {
        case PortalRole.LIBRARIAN:
          return {
            types: ['LIBRARY_BOOK', 'LIBRARY_BORROW'],
            icon: <BookOpen className="w-4 h-4" />,
            title: 'Library Catalog & Circulation',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.TRANSPORT_MANAGER:
          return {
            types: ['VEHICLE', 'ROUTE', 'TRIP'],
            icon: <Bus className="w-4 h-4" />,
            title: 'Fleet Readiness & Bus Routes',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.WELFARE_NURSE:
          return {
            types: ['WELFARE', 'HOSTEL'],
            icon: <HeartPulse className="w-4 h-4" />,
            title: 'Campus Clinic & Student Welfare',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.ADMISSIONS_RECEPTION:
          return {
            types: ['FRONT_DESK', 'ADMISSION', 'EVENT'],
            icon: <ClipboardCheck className="w-4 h-4" />,
            title: 'Front Desk, Visitors & Admissions Intake',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.PROCUREMENT_OFFICER:
          return {
            types: ['PROCUREMENT'],
            icon: <ShoppingCart className="w-4 h-4" />,
            title: 'Requisitions, Vendor RFQs & POs',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.STOREKEEPER:
          return {
            types: ['INVENTORY', 'ASSET', 'STOCK_MOVEMENT'],
            icon: <Package className="w-4 h-4" />,
            title: 'Campus Stores, Assets & Stock Control',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.EXAMINATION_OFFICER:
          return {
            types: [],
            icon: <Award className="w-4 h-4" />,
            title: 'Examination Moderation & Report Pipeline',
            tab: GsmsTab.ACADEMICS
          };
        case PortalRole.SCHOOL_COUNSELLOR:
          return {
            types: ['COUNSELLING', 'DISCIPLINE', 'WELFARE'],
            icon: <HeartPulse className="w-4 h-4" />,
            title: 'Pastoral Guidance & Counselling Cases',
            tab: GsmsTab.OPERATIONS
          };
        case PortalRole.ICT_ADMIN:
        default:
          return {
            types: ['ICT_TICKET', 'ASSET'],
            icon: <Laptop className="w-4 h-4" />,
            title: 'ICT Systems, Helpdesk & RBAC Governance',
            tab: GsmsTab.OPERATIONS
          };
      }
    })();

    const relevantOps =
      specialistConfig.types.length > 0
        ? workspace.operations.filter((o) => specialistConfig.types.includes(o.moduleType))
        : [];

    const examRecords = workspace.academics.filter((a) => a.recordType === 'EXAM');

    return (
      <>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiMetricCard
            label={
              activeRole === PortalRole.EXAMINATION_OFFICER
                ? 'Active Exams'
                : 'Active Portal Records'
            }
            value={String(
              activeRole === PortalRole.EXAMINATION_OFFICER
                ? examRecords.length
                : relevantOps.length
            )}
            delta={roleMeta.shortTitle.toUpperCase()}
            icon={specialistConfig.icon}
            tone="cobalt"
          />
          <KpiMetricCard
            label={
              activeRole === PortalRole.ICT_ADMIN
                ? 'Delegated Grants'
                : activeRole === PortalRole.EXAMINATION_OFFICER
                  ? 'Report Templates'
                  : 'Portal Alerts'
            }
            value={String(
              activeRole === PortalRole.ICT_ADMIN
                ? workspace.accessAssignments.length
                : activeRole === PortalRole.EXAMINATION_OFFICER
                  ? workspace.reportTemplates.length
                  : roleAlerts.length
            )}
            delta="MONITORED"
            icon={<ShieldAlert className="w-4 h-4" />}
            tone="amber"
          />
          <KpiMetricCard
            label="Campus Learners"
            value={String(totalStudents)}
            delta={activeSchool?.code || 'TENANT'}
            icon={<GraduationCap className="w-4 h-4" />}
            tone="emerald"
          />
          <KpiMetricCard
            label="School Notices"
            value={String(workspace.communications.length)}
            delta="COMMS"
            icon={<FileText className="w-4 h-4" />}
            tone="violet"
          />
        </div>

        <SectionHeader
          title={specialistConfig.title}
          subtitle={roleMeta.scopeDescription}
          actionLabel="Open Workspace"
          onAction={() => onNavigateTab(specialistConfig.tab)}
        />

        {activeRole === PortalRole.EXAMINATION_OFFICER ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {examRecords.map((ex) => (
              <div
                key={ex.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="text-sm font-bold text-[#0F172A] truncate">{ex.title}</div>
                  <div className="text-xs text-[#475569] truncate">
                    {ex.className} • {ex.subjectName} • {ex.scheduleSlot}
                  </div>
                </div>
                <StatusBadge text={`${ex.score}/${ex.maxScore} (${ex.grade || 'A'})`} tone="emerald" />
              </div>
            ))}
          </div>
        ) : relevantOps.length === 0 ? (
          <EmptyStateCard
            icon={specialistConfig.icon}
            title="No Records in Current View"
            description="Open the dedicated operations section to log and manage portal items."
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {relevantOps.map((op) => (
              <div
                key={op.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="text-sm font-bold text-[#0F172A] truncate">{op.title}</div>
                  <div className="text-xs text-[#475569] truncate">
                    {op.subtitle} {op.partyName ? `• ${op.partyName}` : ''}
                  </div>
                </div>
                <StatusBadge text={op.status} tone="cobalt" />
              </div>
            ))}
          </div>
        )}
      </>
    );
  };

  return (
    <div className="space-y-5">
      <AiCopilotBannerCard
        role={activeRole}
        lowDataMode={lowDataMode}
        aiEnabled={aiEnabled}
        onPromptClick={handleQuickAi}
        onOpenFullAi={() => onNavigateTab(GsmsTab.AI_GOVERNANCE)}
      />

      {renderRoleBody()}

      {roleAlerts.length > 0 ? (
        <div className="space-y-2.5">
          <SectionHeader
            title="Role-Scoped Early-Warning Alerts"
            subtitle="Active risks routed to this portal"
            actionLabel="View All"
            onAction={() => onNavigateTab(GsmsTab.ANALYTICS)}
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {roleAlerts.slice(0, 4).map((al) => (
              <div
                key={al.id}
                className="bg-white rounded-2xl border border-[#D0D9E4] p-4 shadow-xs flex items-start justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="text-sm font-bold text-[#0F172A] truncate">{al.targetName}</div>
                  <div className="text-xs text-[#475569] mt-0.5">{al.reason}</div>
                  <div className="text-[11px] font-medium text-[#1D4ED8] mt-1">
                    Action: {al.recommendedAction}
                  </div>
                </div>
                <StatusBadge text={al.riskLevel} tone={riskTone(al.riskLevel)} />
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
};
