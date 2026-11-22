import {
  AcademicRecord,
  CommunicationItem,
  EarlyWarningAlert,
  FinanceEntry,
  GsmsModule,
  GsmsTab,
  PORTAL_ROLE_META,
  PortalRole,
  ReportCard,
  ReportStatus,
  RoleCapability,
  School,
  Student,
  schoolHasModule
} from '../types/gsms.ts';

const capabilitiesByRole: Record<PortalRole, Set<RoleCapability>> = {
  [PortalRole.SUPER_ADMIN]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.VIEW_FINANCE_ALL,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS,
    RoleCapability.VIEW_AUDIT_LOGS,
    RoleCapability.CREATE_SCHOOL_TENANT,
    RoleCapability.SWITCH_SCHOOL_TENANT,
    RoleCapability.SWITCH_BRANCH,
    RoleCapability.MANAGE_BRANCHES,
    RoleCapability.MANAGE_MODULES,
    RoleCapability.UPDATE_ACADEMIC_PERIOD,
    RoleCapability.MANAGE_USER_PERMISSIONS
  ]),
  [PortalRole.SCHOOL_ADMIN]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.MARK_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.FILL_SUBMIT_REPORT_CARDS,
    RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.MANAGE_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.ENROLL_STUDENT,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.MANAGE_STAFF,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.APPROVE_LEAVE,
    RoleCapability.VIEW_FINANCE_ALL,
    RoleCapability.RECORD_FINANCE_PAYMENT,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_PROCUREMENT,
    RoleCapability.MANAGE_STORES,
    RoleCapability.MANAGE_COUNSELLING,
    RoleCapability.MANAGE_FRONT_DESK,
    RoleCapability.MANAGE_ICT_SYSTEMS,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS,
    RoleCapability.VIEW_AUDIT_LOGS,
    RoleCapability.SWITCH_SCHOOL_TENANT,
    RoleCapability.SWITCH_BRANCH,
    RoleCapability.MANAGE_BRANCHES,
    RoleCapability.MANAGE_MODULES,
    RoleCapability.UPDATE_ACADEMIC_PERIOD,
    RoleCapability.MANAGE_USER_PERMISSIONS
  ]),
  [PortalRole.HEADTEACHER]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.MODERATE_EXAMS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.MARK_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.MANAGE_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.APPROVE_LEAVE,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS,
    RoleCapability.VIEW_AUDIT_LOGS,
    RoleCapability.SWITCH_BRANCH
  ]),
  [PortalRole.DEPUTY_HEADTEACHER]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.MODERATE_EXAMS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.MARK_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_COUNSELLING,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS,
    RoleCapability.SWITCH_BRANCH
  ]),
  [PortalRole.TEACHER]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.MARK_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.FILL_SUBMIT_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.CLASS_TEACHER]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.MARK_ATTENDANCE,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.FILL_SUBMIT_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.PARENT]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.VIEW_OWN_PUBLISHED_REPORTS,
    RoleCapability.VIEW_OWN_INVOICES_ONLY,
    RoleCapability.VIEW_COMMUNICATIONS
  ]),
  [PortalRole.STUDENT]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.VIEW_OWN_PUBLISHED_REPORTS,
    RoleCapability.VIEW_COMMUNICATIONS
  ]),
  [PortalRole.FINANCE_OFFICER]: new Set([
    RoleCapability.VIEW_FINANCE_ALL,
    RoleCapability.RECORD_FINANCE_PAYMENT,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_PROCUREMENT,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.HR_MANAGER]: new Set([
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.MANAGE_STAFF,
    RoleCapability.VIEW_LEAVE_WORKFLOW,
    RoleCapability.APPLY_LEAVE,
    RoleCapability.APPROVE_LEAVE,
    RoleCapability.VIEW_PAYROLL_ONLY,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.LIBRARIAN]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.TRANSPORT_MANAGER]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.WELFARE_NURSE]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_COUNSELLING,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.ADMISSIONS_RECEPTION]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.ENROLL_STUDENT,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_FRONT_DESK,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.PROCUREMENT_OFFICER]: new Set([
    RoleCapability.VIEW_FINANCE_ALL,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_PROCUREMENT,
    RoleCapability.MANAGE_STORES,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.STOREKEEPER]: new Set([
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_PROCUREMENT,
    RoleCapability.MANAGE_STORES,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.EXAMINATION_OFFICER]: new Set([
    RoleCapability.VIEW_ACADEMICS,
    RoleCapability.EDIT_ACADEMICS,
    RoleCapability.MODERATE_EXAMS,
    RoleCapability.VIEW_ALL_REPORT_CARDS,
    RoleCapability.FILL_SUBMIT_REPORT_CARDS,
    RoleCapability.VIEW_REPORT_TEMPLATES,
    RoleCapability.MANAGE_REPORT_TEMPLATES,
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_ANALYTICS_CHARTS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.SCHOOL_COUNSELLOR]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_ATTENDANCE,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_COUNSELLING,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS
  ]),
  [PortalRole.ICT_ADMIN]: new Set([
    RoleCapability.VIEW_ALL_STUDENTS,
    RoleCapability.VIEW_STAFF_DIRECTORY,
    RoleCapability.EDIT_OPERATIONS,
    RoleCapability.MANAGE_STORES,
    RoleCapability.MANAGE_ICT_SYSTEMS,
    RoleCapability.MANAGE_USER_PERMISSIONS,
    RoleCapability.VIEW_AUDIT_LOGS,
    RoleCapability.VIEW_ALERTS,
    RoleCapability.MANAGE_ALERTS,
    RoleCapability.VIEW_COMMUNICATIONS,
    RoleCapability.COMPOSE_COMMUNICATIONS,
    RoleCapability.SWITCH_BRANCH
  ])
};

const EXTRA_SET_EMPTY = new Set<RoleCapability>();

function normalizeExtra(extra?: Set<RoleCapability> | RoleCapability[]): Set<RoleCapability> {
  if (!extra) return EXTRA_SET_EMPTY;
  return Array.isArray(extra) ? new Set(extra) : extra;
}

export const RolePolicy = {
  baseCapabilities(role: PortalRole): Set<RoleCapability> {
    return capabilitiesByRole[role] ?? new Set();
  },

  has(
    role: PortalRole,
    capability: RoleCapability,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): boolean {
    if (capabilitiesByRole[role]?.has(capability)) return true;
    return normalizeExtra(extra).has(capability);
  },

  requiresLearnerScope(role: PortalRole, extra?: Set<RoleCapability> | RoleCapability[]): boolean {
    return (
      RolePolicy.has(role, RoleCapability.VIEW_OWN_PUBLISHED_REPORTS, extra) &&
      !RolePolicy.has(role, RoleCapability.VIEW_ALL_STUDENTS, extra)
    );
  },

  resolveFocusStudent(students: Student[], focusStudentId: string | null | undefined): Student | null {
    const id = focusStudentId?.trim();
    if (!id) return null;
    return students.find((s) => s.id === id) ?? null;
  },

  canViewLearnerFeeBalances(role: PortalRole, extra?: Set<RoleCapability> | RoleCapability[]): boolean {
    return (
      RolePolicy.has(role, RoleCapability.VIEW_FINANCE_ALL, extra) ||
      RolePolicy.has(role, RoleCapability.VIEW_OWN_INVOICES_ONLY, extra)
    );
  },

  canViewStaffLeaveBalances(role: PortalRole, extra?: Set<RoleCapability> | RoleCapability[]): boolean {
    return RolePolicy.has(role, RoleCapability.APPROVE_LEAVE, extra);
  },

  academicSubTabs(
    role: PortalRole,
    school: School | null,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): string[] {
    if (!school) return [];
    const tabs: string[] = [];
    if (
      RolePolicy.has(role, RoleCapability.VIEW_ACADEMICS, extra) &&
      (schoolHasModule(school, GsmsModule.ACADEMICS) || schoolHasModule(school, GsmsModule.EXAMS))
    ) {
      tabs.push('Academics & Exams');
    }
    if (
      RolePolicy.has(role, RoleCapability.VIEW_ATTENDANCE, extra) &&
      schoolHasModule(school, GsmsModule.ATTENDANCE)
    ) {
      tabs.push('Attendance');
    }
    if (
      (RolePolicy.has(role, RoleCapability.VIEW_ALL_REPORT_CARDS, extra) ||
        RolePolicy.has(role, RoleCapability.VIEW_OWN_PUBLISHED_REPORTS, extra)) &&
      schoolHasModule(school, GsmsModule.REPORT_CARDS)
    ) {
      tabs.push('Report Cards');
    }
    if (
      RolePolicy.has(role, RoleCapability.VIEW_REPORT_TEMPLATES, extra) &&
      schoolHasModule(school, GsmsModule.REPORT_CARDS)
    ) {
      tabs.push('Templates');
    }
    return tabs;
  },

  peopleSubTabs(
    role: PortalRole,
    school: School | null,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): string[] {
    if (!school) return [];
    const tabs: string[] = [];
    if (RolePolicy.has(role, RoleCapability.VIEW_ALL_STUDENTS, extra)) {
      tabs.push('Students');
    }
    if (
      RolePolicy.has(role, RoleCapability.VIEW_STAFF_DIRECTORY, extra) &&
      schoolHasModule(school, GsmsModule.HR_STAFF)
    ) {
      tabs.push('Staff & Faculty');
    }
    if (
      RolePolicy.has(role, RoleCapability.VIEW_LEAVE_WORKFLOW, extra) &&
      schoolHasModule(school, GsmsModule.LEAVE_MANAGEMENT)
    ) {
      tabs.push('Leave Workflow');
    }
    return tabs;
  },

  operationSections(
    role: PortalRole,
    school: School | null,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): string[] {
    if (!school) return [];
    const extraSet = normalizeExtra(extra);

    const roleSections: Record<PortalRole, string[]> = {
      [PortalRole.SUPER_ADMIN]: [
        'Finance',
        'Procurement',
        'Stores',
        'Library',
        'Transport',
        'Welfare',
        'Counselling',
        'Discipline',
        'Hostel',
        'Front Desk',
        'Admissions',
        'ICT Systems',
        'Events'
      ],
      [PortalRole.SCHOOL_ADMIN]: [
        'Finance',
        'Procurement',
        'Stores',
        'Library',
        'Transport',
        'Welfare',
        'Counselling',
        'Discipline',
        'Hostel',
        'Front Desk',
        'Admissions',
        'ICT Systems',
        'Events'
      ],
      [PortalRole.HEADTEACHER]: ['Welfare', 'Counselling', 'Discipline', 'Admissions', 'Events'],
      [PortalRole.DEPUTY_HEADTEACHER]: ['Welfare', 'Counselling', 'Discipline', 'Admissions', 'Events'],
      [PortalRole.CLASS_TEACHER]: ['Welfare', 'Discipline'],
      [PortalRole.FINANCE_OFFICER]: ['Finance', 'Procurement'],
      [PortalRole.PARENT]: ['Finance'],
      [PortalRole.HR_MANAGER]: ['Finance'],
      [PortalRole.LIBRARIAN]: ['Library'],
      [PortalRole.TRANSPORT_MANAGER]: ['Transport'],
      [PortalRole.WELFARE_NURSE]: ['Welfare', 'Counselling', 'Discipline', 'Hostel'],
      [PortalRole.ADMISSIONS_RECEPTION]: ['Front Desk', 'Admissions', 'Events'],
      [PortalRole.PROCUREMENT_OFFICER]: ['Finance', 'Procurement', 'Stores'],
      [PortalRole.STOREKEEPER]: ['Procurement', 'Stores'],
      [PortalRole.SCHOOL_COUNSELLOR]: ['Welfare', 'Counselling', 'Discipline'],
      [PortalRole.ICT_ADMIN]: ['Stores', 'ICT Systems'],
      [PortalRole.TEACHER]: [],
      [PortalRole.STUDENT]: [],
      [PortalRole.EXAMINATION_OFFICER]: []
    };

    const allowed = new Set<string>(roleSections[role] ?? []);

    if (extraSet.has(RoleCapability.VIEW_FINANCE_ALL) || extraSet.has(RoleCapability.RECORD_FINANCE_PAYMENT)) {
      allowed.add('Finance');
    }
    if (extraSet.has(RoleCapability.MANAGE_PROCUREMENT)) allowed.add('Procurement');
    if (extraSet.has(RoleCapability.MANAGE_STORES)) allowed.add('Stores');
    if (extraSet.has(RoleCapability.MANAGE_COUNSELLING)) allowed.add('Counselling');
    if (extraSet.has(RoleCapability.MANAGE_FRONT_DESK)) allowed.add('Front Desk');
    if (extraSet.has(RoleCapability.MANAGE_ICT_SYSTEMS)) allowed.add('ICT Systems');

    const ordered: Array<{ section: string; mod: GsmsModule }> = [
      { section: 'Finance', mod: GsmsModule.FINANCE },
      { section: 'Procurement', mod: GsmsModule.PROCUREMENT },
      { section: 'Stores', mod: GsmsModule.INVENTORY_ASSETS },
      { section: 'Library', mod: GsmsModule.LIBRARY },
      { section: 'Transport', mod: GsmsModule.TRANSPORT },
      { section: 'Welfare', mod: GsmsModule.HEALTH_WELFARE },
      { section: 'Counselling', mod: GsmsModule.DISCIPLINE },
      { section: 'Discipline', mod: GsmsModule.DISCIPLINE },
      { section: 'Hostel', mod: GsmsModule.HOSTEL_BOARDING },
      { section: 'Front Desk', mod: GsmsModule.ADMISSIONS },
      { section: 'Admissions', mod: GsmsModule.ADMISSIONS },
      { section: 'ICT Systems', mod: GsmsModule.ICT_SYSTEMS },
      { section: 'Events', mod: GsmsModule.EVENTS }
    ];

    return ordered
      .filter((item) => allowed.has(item.section) && schoolHasModule(school, item.mod))
      .map((item) => item.section);
  },

  analyticsSubTabs(
    role: PortalRole,
    school: School | null,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): string[] {
    if (!school) return [];
    const tabs: string[] = [];
    if (
      (RolePolicy.has(role, RoleCapability.VIEW_ANALYTICS_CHARTS, extra) ||
        RolePolicy.has(role, RoleCapability.VIEW_ALERTS, extra)) &&
      schoolHasModule(school, GsmsModule.ANALYTICS)
    ) {
      tabs.push(
        RolePolicy.has(role, RoleCapability.VIEW_ANALYTICS_CHARTS, extra)
          ? 'Analytics & Alerts'
          : 'Role Alerts'
      );
    }
    if (
      RolePolicy.has(role, RoleCapability.VIEW_COMMUNICATIONS, extra) &&
      schoolHasModule(school, GsmsModule.COMMUNICATION)
    ) {
      tabs.push('Communications');
    }
    if (RolePolicy.has(role, RoleCapability.VIEW_AUDIT_LOGS, extra)) {
      tabs.push('Audit Logs');
    }
    return tabs;
  },

  visibleTabs(
    role: PortalRole,
    school: School | null,
    extra?: Set<RoleCapability> | RoleCapability[]
  ): GsmsTab[] {
    const tabs: GsmsTab[] = [GsmsTab.DASHBOARD];
    if (RolePolicy.academicSubTabs(role, school, extra).length > 0) tabs.push(GsmsTab.ACADEMICS);
    if (RolePolicy.peopleSubTabs(role, school, extra).length > 0) tabs.push(GsmsTab.PEOPLE);
    if (RolePolicy.operationSections(role, school, extra).length > 0) tabs.push(GsmsTab.OPERATIONS);
    if (RolePolicy.analyticsSubTabs(role, school, extra).length > 0) tabs.push(GsmsTab.ANALYTICS);
    tabs.push(GsmsTab.AI_GOVERNANCE);
    return tabs;
  },

  filterStudentsForRole(
    role: PortalRole,
    students: Student[],
    assignedClasses: string[] = [],
    linkedStudentIds: string[] = []
  ): Student[] {
    if (role === PortalRole.PARENT || role === PortalRole.STUDENT) {
      if (linkedStudentIds.length > 0) {
        const linked = students.filter((s) => linkedStudentIds.includes(s.id));
        if (linked.length > 0) return linked;
      }
      return students;
    }
    if (role === PortalRole.TEACHER || role === PortalRole.CLASS_TEACHER) {
      if (assignedClasses.length > 0) {
        const lowerClasses = assignedClasses.map((c) => c.toLowerCase());
        return students.filter((s) => lowerClasses.includes(s.className.toLowerCase()));
      }
      return students;
    }
    return students;
  },

  filterAcademicsForRole(
    role: PortalRole,
    academics: AcademicRecord[],
    assignedClasses: string[] = [],
    assignedSubjects: string[] = []
  ): AcademicRecord[] {
    if (role === PortalRole.TEACHER || role === PortalRole.CLASS_TEACHER) {
      return academics.filter((rec) => {
        const classMatch =
          assignedClasses.length === 0 ||
          assignedClasses.some((c) => c.toLowerCase() === rec.className.toLowerCase());
        const subjectMatch =
          assignedSubjects.length === 0 ||
          !rec.subjectName ||
          assignedSubjects.some((s) => s.toLowerCase() === rec.subjectName?.toLowerCase());
        return classMatch && subjectMatch;
      });
    }
    return academics;
  },

  filterReportCardsForRole(
    role: PortalRole,
    reports: ReportCard[],
    linkedStudents: Student[] = [],
    focusStudent?: Student | null
  ): ReportCard[] {
    if (role === PortalRole.PARENT || role === PortalRole.STUDENT) {
      const targetIds = focusStudent
        ? new Set([focusStudent.id])
        : new Set(linkedStudents.map((s) => s.id));
      return reports.filter(
        (r) => r.status === ReportStatus.PUBLISHED && (targetIds.size === 0 || targetIds.has(r.studentId))
      );
    }
    return reports;
  },

  filterAlertsForRole(role: PortalRole, alerts: EarlyWarningAlert[]): EarlyWarningAlert[] {
    switch (role) {
      case PortalRole.SUPER_ADMIN:
      case PortalRole.SCHOOL_ADMIN:
      case PortalRole.HEADTEACHER:
      case PortalRole.DEPUTY_HEADTEACHER:
        return alerts;
      case PortalRole.TEACHER:
      case PortalRole.CLASS_TEACHER:
      case PortalRole.EXAMINATION_OFFICER:
        return alerts.filter((a) => ['ACADEMIC', 'ATTENDANCE', 'BEHAVIOR'].includes(a.category));
      case PortalRole.FINANCE_OFFICER:
      case PortalRole.PROCUREMENT_OFFICER:
        return alerts.filter((a) => a.category === 'FINANCE');
      case PortalRole.HR_MANAGER:
        return alerts.filter((a) => a.category === 'STAFFING');
      case PortalRole.WELFARE_NURSE:
      case PortalRole.SCHOOL_COUNSELLOR:
        return alerts.filter((a) => ['WELFARE', 'ATTENDANCE', 'BEHAVIOR'].includes(a.category));
      case PortalRole.TRANSPORT_MANAGER:
        return alerts.filter((a) => ['TRANSPORT', 'ATTENDANCE'].includes(a.category));
      case PortalRole.ICT_ADMIN:
        return alerts.filter((a) => a.category === 'ICT');
      case PortalRole.STOREKEEPER:
        return alerts.filter((a) => ['FINANCE', 'STORES'].includes(a.category));
      default:
        return [];
    }
  },

  filterFinanceForRole(
    role: PortalRole,
    entries: FinanceEntry[],
    focusStudentOrList: Student | Student[] | null
  ): FinanceEntry[] {
    switch (role) {
      case PortalRole.SUPER_ADMIN:
      case PortalRole.SCHOOL_ADMIN:
      case PortalRole.FINANCE_OFFICER:
      case PortalRole.PROCUREMENT_OFFICER:
        return entries;
      case PortalRole.HR_MANAGER:
        return entries.filter((e) => e.entryType === 'PAYROLL');
      case PortalRole.PARENT: {
        if (!focusStudentOrList) return [];
        const targetStudents = Array.isArray(focusStudentOrList)
          ? focusStudentOrList
          : [focusStudentOrList];
        if (targetStudents.length === 0) return [];
        const matchTokens = new Set<string>();
        targetStudents.forEach((st) => {
          if (st.fullName) matchTokens.add(st.fullName.toLowerCase());
          if (st.parentName) matchTokens.add(st.parentName.toLowerCase());
        });
        return entries.filter(
          (e) =>
            e.entryType === 'INVOICE' &&
            (matchTokens.has(e.partyName.toLowerCase()) ||
              Array.from(matchTokens).some((tok) => e.title.toLowerCase().includes(tok)))
        );
      }
      default:
        return [];
    }
  },

  filterCommunicationsForRole(role: PortalRole, items: CommunicationItem[]): CommunicationItem[] {
    if (role === PortalRole.STUDENT) {
      return items.filter((it) => {
        const aud = it.audience.toLowerCase();
        return aud.includes('all') || aud.includes('student') || aud.includes('grade');
      });
    }
    if (role === PortalRole.PARENT) {
      return items.filter((it) => {
        const aud = it.audience.toLowerCase();
        return aud.includes('all') || aud.includes('parent') || aud.includes('guardian');
      });
    }
    return items;
  },

  tabDisplayLabel(role: PortalRole, tab: GsmsTab): string {
    if (tab === GsmsTab.DASHBOARD) return 'Dashboard';
    if (tab === GsmsTab.ACADEMICS) return 'Academics';
    if (tab === GsmsTab.PEOPLE) return 'People & HR';
    if (tab === GsmsTab.OPERATIONS) {
      switch (role) {
        case PortalRole.PARENT:
          return 'Fees';
        case PortalRole.FINANCE_OFFICER:
          return 'Finance';
        case PortalRole.PROCUREMENT_OFFICER:
          return 'Procurement';
        case PortalRole.STOREKEEPER:
          return 'Stores';
        case PortalRole.LIBRARIAN:
          return 'Library';
        case PortalRole.TRANSPORT_MANAGER:
          return 'Transport';
        case PortalRole.WELFARE_NURSE:
          return 'Welfare';
        case PortalRole.SCHOOL_COUNSELLOR:
          return 'Counselling';
        case PortalRole.ADMISSIONS_RECEPTION:
          return 'Front Desk';
        case PortalRole.ICT_ADMIN:
          return 'ICT Systems';
        case PortalRole.HR_MANAGER:
          return 'Payroll';
        default:
          return 'Operations';
      }
    }
    if (tab === GsmsTab.ANALYTICS) {
      return role === PortalRole.STUDENT || role === PortalRole.PARENT ? 'Notices' : 'Analytics';
    }
    if (tab === GsmsTab.AI_GOVERNANCE) {
      return role === PortalRole.SUPER_ADMIN || role === PortalRole.SCHOOL_ADMIN || role === PortalRole.ICT_ADMIN
        ? 'AI & Admin'
        : 'AI & Profile';
    }
    return 'Dashboard';
  },

  accessSummary(role: PortalRole): string {
    return PORTAL_ROLE_META[role]?.scopeDescription ?? 'Role-scoped GSMS portal';
  }
};
