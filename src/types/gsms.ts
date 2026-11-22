export enum PortalRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  SCHOOL_ADMIN = 'SCHOOL_ADMIN',
  HEADTEACHER = 'HEADTEACHER',
  DEPUTY_HEADTEACHER = 'DEPUTY_HEADTEACHER',
  TEACHER = 'TEACHER',
  CLASS_TEACHER = 'CLASS_TEACHER',
  PARENT = 'PARENT',
  STUDENT = 'STUDENT',
  FINANCE_OFFICER = 'FINANCE_OFFICER',
  HR_MANAGER = 'HR_MANAGER',
  LIBRARIAN = 'LIBRARIAN',
  TRANSPORT_MANAGER = 'TRANSPORT_MANAGER',
  WELFARE_NURSE = 'WELFARE_NURSE',
  ADMISSIONS_RECEPTION = 'ADMISSIONS_RECEPTION',
  PROCUREMENT_OFFICER = 'PROCUREMENT_OFFICER',
  STOREKEEPER = 'STOREKEEPER',
  EXAMINATION_OFFICER = 'EXAMINATION_OFFICER',
  SCHOOL_COUNSELLOR = 'SCHOOL_COUNSELLOR',
  ICT_ADMIN = 'ICT_ADMIN'
}

export interface PortalRoleMeta {
  displayName: string;
  shortTitle: string;
  scopeDescription: string;
  aiCapabilities: string[];
}

export const PORTAL_ROLE_META: Record<PortalRole, PortalRoleMeta> = {
  [PortalRole.SUPER_ADMIN]: {
    displayName: 'Super Administrator Portal',
    shortTitle: 'Super Admin',
    scopeDescription: 'Overall GSMS platform governance, tenants, SaaS billing & global audit',
    aiCapabilities: ['Executive Multi-School Report', 'Churn & Revenue Forecast', 'Cross-Tenant Anomaly Scan']
  },
  [PortalRole.SCHOOL_ADMIN]: {
    displayName: 'School Administrator Portal',
    shortTitle: 'School Admin',
    scopeDescription: 'Assigned school & branch administration, module registry & permissions',
    aiCapabilities: ['AI Timetable Generator', 'Draft School Circular', 'Staff Workload Balance']
  },
  [PortalRole.HEADTEACHER]: {
    displayName: 'Headteacher / Principal Portal',
    shortTitle: 'Headteacher',
    scopeDescription: 'Academic leadership, authoritative report approvals & risk interventions',
    aiCapabilities: ['Summarize Submitted Reports', 'Learner Intervention Plan', 'Department Performance Review']
  },
  [PortalRole.DEPUTY_HEADTEACHER]: {
    displayName: 'Deputy Headteacher Portal',
    shortTitle: 'Deputy Head',
    scopeDescription: 'Curriculum coordination, timetables, discipline & delegated report review',
    aiCapabilities: ['Lesson Cover Optimization', 'Discipline Pattern Digest', 'Class Academic Audit']
  },
  [PortalRole.TEACHER]: {
    displayName: 'Teacher Portal',
    shortTitle: 'Teacher',
    scopeDescription: 'Assigned classes & subjects, marks, attendance & report card drafts',
    aiCapabilities: ['Generate Lesson Plan', 'Generate Quiz & Rubric', 'Draft Report Card Comment']
  },
  [PortalRole.CLASS_TEACHER]: {
    displayName: 'Class Teacher Portal',
    shortTitle: 'Class Teacher',
    scopeDescription: 'Homeroom class pastoral oversight, daily roll call & term report compilation',
    aiCapabilities: ['Homeroom Attendance Digest', 'Compile Class Report Summary', 'Parent Check-In Note']
  },
  [PortalRole.PARENT]: {
    displayName: 'Parent / Guardian Portal',
    shortTitle: 'Parent',
    scopeDescription: 'Linked children progress, attendance, fee invoices & published reports',
    aiCapabilities: ['Explain Grades Simply', 'Home Support Checklist', 'Translate School Notice']
  },
  [PortalRole.STUDENT]: {
    displayName: 'Student / Learner Portal',
    shortTitle: 'Student',
    scopeDescription: 'Own class timetable, attendance, published reports & study companion',
    aiCapabilities: ['Personal Revision Timetable', 'Weak-Topic Explainer', 'Exam Prep Practice Quiz']
  },
  [PortalRole.FINANCE_OFFICER]: {
    displayName: 'Accountant / Finance Officer Portal',
    shortTitle: 'Accountant',
    scopeDescription: 'Authorized financial modules, fee collections, invoices & payroll',
    aiCapabilities: ['Fee Default Risk Forecast', 'Draft Unpaid Fee Reminder', 'Cashflow & Expense Summary']
  },
  [PortalRole.HR_MANAGER]: {
    displayName: 'Human Resource Officer Portal',
    shortTitle: 'HR Officer',
    scopeDescription: 'Staff directory, leave approvals, balances & substitute cover planning',
    aiCapabilities: ['Detect Leave Conflicts', 'Suggest Substitute Cover', 'Draft HR Policy Memo']
  },
  [PortalRole.LIBRARIAN]: {
    displayName: 'Librarian Portal',
    shortTitle: 'Librarian',
    scopeDescription: 'Library catalog, borrowings, overdue tracking & reading outreach',
    aiCapabilities: ['Reading Recommendations', 'Overdue Risk Prediction', 'Auto-Categorize Titles']
  },
  [PortalRole.TRANSPORT_MANAGER]: {
    displayName: 'Transport Manager Portal',
    shortTitle: 'Transport',
    scopeDescription: 'Fleet readiness, bus routes, daily trips & route delay alerts',
    aiCapabilities: ['Optimize Bus Routes', 'Delay & Maintenance Alert', 'Fuel Efficiency Summary']
  },
  [PortalRole.WELFARE_NURSE]: {
    displayName: 'School Nurse / Medical Officer Portal',
    shortTitle: 'School Nurse',
    scopeDescription: 'Student medical visits, clinic logs, immunization & health alerts',
    aiCapabilities: ['Medical Trend Summary', 'Draft Parent Health Advisory', 'Absenteeism Health Check']
  },
  [PortalRole.ADMISSIONS_RECEPTION]: {
    displayName: 'Receptionist / Front Desk Officer Portal',
    shortTitle: 'Reception',
    scopeDescription: 'Front desk visitor logs, inquiries, admissions intake & campus events',
    aiCapabilities: ['Enrollment Forecast', 'Admission Checklist Builder', 'Visitor & Parent Reply']
  },
  [PortalRole.PROCUREMENT_OFFICER]: {
    displayName: 'Procurement Officer Portal',
    shortTitle: 'Procurement',
    scopeDescription: 'Requisitions, vendor RFQs, purchase orders & supplier spend control',
    aiCapabilities: ['Vendor Quote Comparison', 'Draft Purchase Order Memo', 'Quarterly Spend Forecast']
  },
  [PortalRole.STOREKEEPER]: {
    displayName: 'Storekeeper Portal',
    shortTitle: 'Storekeeper',
    scopeDescription: 'Campus storeroom stock, asset custody, issuances & reorder thresholds',
    aiCapabilities: ['Low-Stock Reorder Plan', 'Asset Depreciation Audit', 'Term Supply Allocation']
  },
  [PortalRole.EXAMINATION_OFFICER]: {
    displayName: 'Examination Officer Portal',
    shortTitle: 'Exam Officer',
    scopeDescription: 'Exam timetables, grading scales, mark moderation & report templates',
    aiCapabilities: ['Detect Exam Clash Schedule', 'Grade Distribution Analysis', 'Draft Exam Invigilation Roster']
  },
  [PortalRole.SCHOOL_COUNSELLOR]: {
    displayName: 'School Counsellor Portal',
    shortTitle: 'Counsellor',
    scopeDescription: 'Student guidance, behavioral wellbeing, safeguarding & intervention plans',
    aiCapabilities: ['Draft Pastoral Care Plan', 'Behavioral Risk Triage', 'Parent Counselling Summary']
  },
  [PortalRole.ICT_ADMIN]: {
    displayName: 'ICT Administrator Portal',
    shortTitle: 'ICT Admin',
    scopeDescription: 'User permission delegation, campus ICT assets, system tickets & audit',
    aiCapabilities: ['RBAC Permission Audit', 'ICT Support Triage', 'Network & Lab Readiness']
  }
};

export const ALL_PORTAL_ROLES: PortalRole[] = Object.values(PortalRole);

export enum GsmsModule {
  ACADEMICS = 'ACADEMICS',
  ADMISSIONS = 'ADMISSIONS',
  ATTENDANCE = 'ATTENDANCE',
  EXAMS = 'EXAMS',
  REPORT_CARDS = 'REPORT_CARDS',
  FINANCE = 'FINANCE',
  HR_STAFF = 'HR_STAFF',
  LEAVE_MANAGEMENT = 'LEAVE_MANAGEMENT',
  PROCUREMENT = 'PROCUREMENT',
  COMMUNICATION = 'COMMUNICATION',
  LIBRARY = 'LIBRARY',
  TRANSPORT = 'TRANSPORT',
  HEALTH_WELFARE = 'HEALTH_WELFARE',
  INVENTORY_ASSETS = 'INVENTORY_ASSETS',
  HOSTEL_BOARDING = 'HOSTEL_BOARDING',
  DISCIPLINE = 'DISCIPLINE',
  EVENTS = 'EVENTS',
  ANALYTICS = 'ANALYTICS',
  AI_TOOLS = 'AI_TOOLS',
  DOCUMENTS_TEMPLATES = 'DOCUMENTS_TEMPLATES',
  ICT_SYSTEMS = 'ICT_SYSTEMS'
}

export const GSMS_MODULE_META: Record<GsmsModule, { label: string; category: string }> = {
  [GsmsModule.ACADEMICS]: { label: 'Academics', category: 'Core' },
  [GsmsModule.ADMISSIONS]: { label: 'Admissions', category: 'Enrollment' },
  [GsmsModule.ATTENDANCE]: { label: 'Attendance', category: 'Core' },
  [GsmsModule.EXAMS]: { label: 'Exams', category: 'Core' },
  [GsmsModule.REPORT_CARDS]: { label: 'Report Cards', category: 'Core' },
  [GsmsModule.FINANCE]: { label: 'Fees / Finance', category: 'Operations' },
  [GsmsModule.HR_STAFF]: { label: 'HR / Staff', category: 'Operations' },
  [GsmsModule.LEAVE_MANAGEMENT]: { label: 'Leave Management', category: 'Operations' },
  [GsmsModule.PROCUREMENT]: { label: 'Procurement', category: 'Operations' },
  [GsmsModule.COMMUNICATION]: { label: 'Communication', category: 'Engagement' },
  [GsmsModule.LIBRARY]: { label: 'Library', category: 'Campus' },
  [GsmsModule.TRANSPORT]: { label: 'Transport', category: 'Campus' },
  [GsmsModule.HEALTH_WELFARE]: { label: 'Health / Welfare', category: 'Campus' },
  [GsmsModule.INVENTORY_ASSETS]: { label: 'Inventory / Stores', category: 'Campus' },
  [GsmsModule.HOSTEL_BOARDING]: { label: 'Hostel / Boarding', category: 'Campus' },
  [GsmsModule.DISCIPLINE]: { label: 'Discipline & Guidance', category: 'Campus' },
  [GsmsModule.EVENTS]: { label: 'Events', category: 'Engagement' },
  [GsmsModule.ANALYTICS]: { label: 'Analytics', category: 'Intelligence' },
  [GsmsModule.AI_TOOLS]: { label: 'AI Tools', category: 'Intelligence' },
  [GsmsModule.DOCUMENTS_TEMPLATES]: { label: 'Documents / Templates', category: 'Administration' },
  [GsmsModule.ICT_SYSTEMS]: { label: 'ICT & Access Control', category: 'Administration' }
};

export const ALL_GSMS_MODULES: GsmsModule[] = Object.values(GsmsModule);
export const DEFAULT_ENABLED_MODULES: string[] = ALL_GSMS_MODULES;

export enum ReportStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  RETURNED = 'RETURNED',
  APPROVED = 'APPROVED',
  PUBLISHED = 'PUBLISHED'
}

export enum LeaveStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum GsmsTab {
  DASHBOARD = 'DASHBOARD',
  ACADEMICS = 'ACADEMICS',
  PEOPLE = 'PEOPLE',
  OPERATIONS = 'OPERATIONS',
  ANALYTICS = 'ANALYTICS',
  AI_GOVERNANCE = 'AI_GOVERNANCE'
}

export enum RoleCapability {
  VIEW_ACADEMICS = 'VIEW_ACADEMICS',
  EDIT_ACADEMICS = 'EDIT_ACADEMICS',
  MODERATE_EXAMS = 'MODERATE_EXAMS',
  VIEW_ATTENDANCE = 'VIEW_ATTENDANCE',
  MARK_ATTENDANCE = 'MARK_ATTENDANCE',
  VIEW_ALL_REPORT_CARDS = 'VIEW_ALL_REPORT_CARDS',
  VIEW_OWN_PUBLISHED_REPORTS = 'VIEW_OWN_PUBLISHED_REPORTS',
  FILL_SUBMIT_REPORT_CARDS = 'FILL_SUBMIT_REPORT_CARDS',
  APPROVE_PUBLISH_REPORT_CARDS = 'APPROVE_PUBLISH_REPORT_CARDS',
  VIEW_REPORT_TEMPLATES = 'VIEW_REPORT_TEMPLATES',
  MANAGE_REPORT_TEMPLATES = 'MANAGE_REPORT_TEMPLATES',

  VIEW_ALL_STUDENTS = 'VIEW_ALL_STUDENTS',
  ENROLL_STUDENT = 'ENROLL_STUDENT',
  VIEW_STAFF_DIRECTORY = 'VIEW_STAFF_DIRECTORY',
  MANAGE_STAFF = 'MANAGE_STAFF',
  VIEW_LEAVE_WORKFLOW = 'VIEW_LEAVE_WORKFLOW',
  APPLY_LEAVE = 'APPLY_LEAVE',
  APPROVE_LEAVE = 'APPROVE_LEAVE',

  VIEW_FINANCE_ALL = 'VIEW_FINANCE_ALL',
  VIEW_PAYROLL_ONLY = 'VIEW_PAYROLL_ONLY',
  VIEW_OWN_INVOICES_ONLY = 'VIEW_OWN_INVOICES_ONLY',
  RECORD_FINANCE_PAYMENT = 'RECORD_FINANCE_PAYMENT',
  EDIT_OPERATIONS = 'EDIT_OPERATIONS',
  MANAGE_PROCUREMENT = 'MANAGE_PROCUREMENT',
  MANAGE_STORES = 'MANAGE_STORES',
  MANAGE_COUNSELLING = 'MANAGE_COUNSELLING',
  MANAGE_FRONT_DESK = 'MANAGE_FRONT_DESK',
  MANAGE_ICT_SYSTEMS = 'MANAGE_ICT_SYSTEMS',

  VIEW_ANALYTICS_CHARTS = 'VIEW_ANALYTICS_CHARTS',
  VIEW_ALERTS = 'VIEW_ALERTS',
  MANAGE_ALERTS = 'MANAGE_ALERTS',
  VIEW_COMMUNICATIONS = 'VIEW_COMMUNICATIONS',
  COMPOSE_COMMUNICATIONS = 'COMPOSE_COMMUNICATIONS',
  VIEW_AUDIT_LOGS = 'VIEW_AUDIT_LOGS',

  CREATE_SCHOOL_TENANT = 'CREATE_SCHOOL_TENANT',
  SWITCH_SCHOOL_TENANT = 'SWITCH_SCHOOL_TENANT',
  SWITCH_BRANCH = 'SWITCH_BRANCH',
  MANAGE_BRANCHES = 'MANAGE_BRANCHES',
  MANAGE_MODULES = 'MANAGE_MODULES',
  UPDATE_ACADEMIC_PERIOD = 'UPDATE_ACADEMIC_PERIOD',
  MANAGE_USER_PERMISSIONS = 'MANAGE_USER_PERMISSIONS'
}

export const ROLE_CAPABILITY_META: Record<RoleCapability, { label: string; category: string }> = {
  [RoleCapability.VIEW_ACADEMICS]: { label: 'View Timetables & Academics', category: 'Academics' },
  [RoleCapability.EDIT_ACADEMICS]: { label: 'Edit Timetables, Exams & Marks', category: 'Academics' },
  [RoleCapability.MODERATE_EXAMS]: { label: 'Moderate Exams & Grading Scales', category: 'Academics' },
  [RoleCapability.VIEW_ATTENDANCE]: { label: 'View Attendance Logs', category: 'Academics' },
  [RoleCapability.MARK_ATTENDANCE]: { label: 'Mark Daily Attendance', category: 'Academics' },
  [RoleCapability.VIEW_ALL_REPORT_CARDS]: { label: 'View All School Report Cards', category: 'Reports' },
  [RoleCapability.VIEW_OWN_PUBLISHED_REPORTS]: { label: 'View Own Published Reports', category: 'Reports' },
  [RoleCapability.FILL_SUBMIT_REPORT_CARDS]: { label: 'Draft & Submit Report Cards', category: 'Reports' },
  [RoleCapability.APPROVE_PUBLISH_REPORT_CARDS]: { label: 'Approve & Publish Report Cards', category: 'Reports' },
  [RoleCapability.VIEW_REPORT_TEMPLATES]: { label: 'View Report Templates', category: 'Reports' },
  [RoleCapability.MANAGE_REPORT_TEMPLATES]: { label: 'Upload & Manage Report Templates', category: 'Reports' },

  [RoleCapability.VIEW_ALL_STUDENTS]: { label: 'View Student Directory', category: 'People & HR' },
  [RoleCapability.ENROLL_STUDENT]: { label: 'Enroll New Students', category: 'People & HR' },
  [RoleCapability.VIEW_STAFF_DIRECTORY]: { label: 'View Staff Directory', category: 'People & HR' },
  [RoleCapability.MANAGE_STAFF]: { label: 'Manage Staff Records', category: 'People & HR' },
  [RoleCapability.VIEW_LEAVE_WORKFLOW]: { label: 'View Leave Workflow', category: 'People & HR' },
  [RoleCapability.APPLY_LEAVE]: { label: 'Submit Leave Requests', category: 'People & HR' },
  [RoleCapability.APPROVE_LEAVE]: { label: 'Approve & Deduct Staff Leave', category: 'People & HR' },

  [RoleCapability.VIEW_FINANCE_ALL]: { label: 'View School Finance Ledger', category: 'Operations' },
  [RoleCapability.VIEW_PAYROLL_ONLY]: { label: 'View Staff Payroll Ledger', category: 'Operations' },
  [RoleCapability.VIEW_OWN_INVOICES_ONLY]: { label: 'View Own Child Invoices', category: 'Operations' },
  [RoleCapability.RECORD_FINANCE_PAYMENT]: { label: 'Record Fee Payments', category: 'Operations' },
  [RoleCapability.EDIT_OPERATIONS]: { label: 'Edit Campus Operations', category: 'Operations' },
  [RoleCapability.MANAGE_PROCUREMENT]: { label: 'Manage Procurement RFQs & POs', category: 'Operations' },
  [RoleCapability.MANAGE_STORES]: { label: 'Manage Stores & Assets', category: 'Operations' },
  [RoleCapability.MANAGE_COUNSELLING]: { label: 'Manage Counselling & Welfare', category: 'Operations' },
  [RoleCapability.MANAGE_FRONT_DESK]: { label: 'Manage Front Desk & Visitors', category: 'Operations' },
  [RoleCapability.MANAGE_ICT_SYSTEMS]: { label: 'Manage ICT Systems & Tickets', category: 'Operations' },

  [RoleCapability.VIEW_ANALYTICS_CHARTS]: { label: 'View Analytics Charts', category: 'Intelligence' },
  [RoleCapability.VIEW_ALERTS]: { label: 'View Early-Warning Alerts', category: 'Intelligence' },
  [RoleCapability.MANAGE_ALERTS]: { label: 'Assign & Resolve Alerts', category: 'Intelligence' },
  [RoleCapability.VIEW_COMMUNICATIONS]: { label: 'Read School Communications', category: 'Comms' },
  [RoleCapability.COMPOSE_COMMUNICATIONS]: { label: 'Compose & Dispatch Notices', category: 'Comms' },
  [RoleCapability.VIEW_AUDIT_LOGS]: { label: 'Inspect Security Audit Logs', category: 'Governance' },

  [RoleCapability.CREATE_SCHOOL_TENANT]: { label: 'Provision New School Tenants', category: 'Governance' },
  [RoleCapability.SWITCH_SCHOOL_TENANT]: { label: 'Switch School Tenants', category: 'Governance' },
  [RoleCapability.SWITCH_BRANCH]: { label: 'Switch Campus Branches', category: 'Governance' },
  [RoleCapability.MANAGE_BRANCHES]: { label: 'Create Campus Branches', category: 'Governance' },
  [RoleCapability.MANAGE_MODULES]: { label: 'Toggle Module Registry', category: 'Governance' },
  [RoleCapability.UPDATE_ACADEMIC_PERIOD]: { label: 'Change Academic Year & Term', category: 'Governance' },
  [RoleCapability.MANAGE_USER_PERMISSIONS]: { label: 'Delegate User Permissions', category: 'Governance' }
};

export interface UserProfile {
  id: string;
  userId: string;
  displayName: string;
  email: string | null;
  primaryRole: string;
  activeSchoolId: string | null;
  activeBranchId: string | null;
  activeRole: string;
  assignedSchoolId: string | null;
  assignedBranchIds: string[];
  assignedClasses: string[];
  assignedSubjects: string[];
  linkedStudentIds: string[];
  extraPermissions: string[];
  lowDataMode: boolean;
}

export interface UserAccessAssignment {
  id: string;
  schoolId: string;
  ownerId: string;
  fullName: string;
  email: string;
  primaryRole: string;
  assignedBranchIds: string[];
  assignedClasses: string[];
  assignedSubjects: string[];
  linkedStudentIds: string[];
  extraPermissions: string[];
  active: boolean;
  updatedBy: string | null;
}

export interface School {
  id: string;
  ownerId: string;
  name: string;
  code: string;
  country: string;
  city: string;
  plan: string;
  subscriptionStatus: string;
  brandColorHex: string;
  activeAcademicYear: string;
  activeTerm: string;
  enabledModules: string[];
  studentCount: number;
  staffCount: number;
  monthlyRevenueUsd: number;
  churnRiskScore: number;
}

export interface Branch {
  id: string;
  schoolId: string;
  ownerId: string;
  name: string;
  code: string;
  city: string;
  mainCampus: boolean;
}

export interface Student {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  admissionNo: string;
  fullName: string;
  className: string;
  stream: string | null;
  parentName: string;
  parentContact: string | null;
  attendanceRate: number;
  averageScore: number;
  feeBalanceUsd: number;
  riskLevel: string;
  welfareStatus: string;
}

export interface StaffMember {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  staffNo: string;
  fullName: string;
  roleTitle: string;
  department: string;
  subjectsSummary: string | null;
  leaveBalanceDays: number;
  leaveUsedDays: number;
  onLeave: boolean;
  workloadHours: number;
}

export interface AcademicRecord {
  id: string;
  schoolId: string;
  branchId: string;
  academicYear: string;
  term: string;
  ownerId: string;
  recordType: string;
  title: string;
  className: string;
  subjectName: string | null;
  teacherName: string | null;
  scheduleSlot: string | null;
  score: number;
  maxScore: number;
  grade: string | null;
}

export interface AttendanceRecord {
  id: string;
  schoolId: string;
  branchId: string;
  academicYear: string;
  term: string;
  ownerId: string;
  targetType: string;
  targetName: string;
  className: string | null;
  dateIso: string;
  status: string;
}

export interface ReportTemplate {
  id: string;
  schoolId: string;
  ownerId: string;
  name: string;
  classLevel: string;
  sections: string[];
  gradingScale: string;
  active: boolean;
}

export interface ReportCard {
  id: string;
  schoolId: string;
  branchId: string;
  academicYear: string;
  term: string;
  ownerId: string;
  studentId: string;
  studentName: string;
  className: string;
  templateId: string;
  templateName: string;
  subjectGradesSummary: string;
  averageScore: number;
  teacherRemarks: string | null;
  aiSummary: string | null;
  headteacherRemarks: string | null;
  status: string;
  version: number;
  submittedBy: string | null;
  reviewedBy: string | null;
}

export interface FinanceEntry {
  id: string;
  schoolId: string;
  branchId: string;
  academicYear: string;
  term: string;
  ownerId: string;
  entryType: string;
  referenceNo: string;
  title: string;
  partyName: string;
  category: string;
  amountUsd: number;
  paidUsd: number;
  status: string;
}

export interface LeaveRequest {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  staffId: string;
  staffName: string;
  roleTitle: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  daysCount: number;
  reason: string;
  attachmentNote: string | null;
  status: string;
  reviewedBy: string | null;
  aiConflictNote: string | null;
}

export interface CommunicationItem {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  channel: string;
  audience: string;
  title: string;
  body: string;
  senderName: string;
  senderRole: string;
  aiDrafted: boolean;
}

export interface OperationRecord {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  moduleType: string;
  title: string;
  subtitle: string | null;
  partyName: string | null;
  status: string;
  metricLabel: string | null;
  notes: string | null;
}

export interface EarlyWarningAlert {
  id: string;
  schoolId: string;
  branchId: string;
  ownerId: string;
  category: string;
  riskLevel: string;
  targetName: string;
  reason: string;
  recommendedAction: string;
  assignedRole: string;
  status: string;
}

export interface AuditLog {
  id: string;
  schoolId: string;
  ownerId: string;
  actorName: string;
  actorRole: string;
  actionType: string;
  moduleName: string;
  summary: string;
  aiAction: boolean;
  createdAtIso?: string;
}

export interface GsmsWorkspaceState {
  userProfile: UserProfile | null;
  schools: School[];
  activeSchool: School | null;
  branches: Branch[];
  activeBranchId: string;
  activeRole: PortalRole;
  accessAssignments: UserAccessAssignment[];
  activeExtraCapabilities: RoleCapability[];
  assignedClasses: string[];
  assignedSubjects: string[];
  linkedStudentIds: string[];
  students: Student[];
  staff: StaffMember[];
  academics: AcademicRecord[];
  attendance: AttendanceRecord[];
  reportTemplates: ReportTemplate[];
  reportCards: ReportCard[];
  finance: FinanceEntry[];
  leaveRequests: LeaveRequest[];
  communications: CommunicationItem[];
  operations: OperationRecord[];
  alerts: EarlyWarningAlert[];
  auditLogs: AuditLog[];
}

export interface SupabaseSyncState {
  isConfigured: boolean;
  configIssue: string | null;
  endpointLabel: string;
  isAuthenticated: boolean;
  authEmail: string | null;
  autoMirrorEnabled: boolean;
  isBusy: boolean;
  connectionStatus: string;
  lastLatencyMs: number | null;
  lastSyncSummary: string | null;
  lastError: string | null;
  remoteCounts: Record<string, number>;
}

export type AiUiState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'streaming'; text: string }
  | { status: 'success'; text: string }
  | { status: 'error'; message: string };

export function parseEnumOrDefault<T extends string>(
  raw: string | null | undefined,
  values: readonly T[],
  fallback: T
): T {
  if (!raw) return fallback;
  const upper = raw.trim().toUpperCase();
  return values.find((v) => v.toUpperCase() === upper) ?? fallback;
}

export function schoolHasModule(school: School | null | undefined, mod: GsmsModule): boolean {
  if (!school) return false;
  return school.enabledModules.includes(mod);
}

export function resolvePrimaryRole(profile: UserProfile | null | undefined): PortalRole {
  return parseEnumOrDefault(profile?.primaryRole, ALL_PORTAL_ROLES, PortalRole.SUPER_ADMIN);
}

export function resolveEffectiveRole(profile: UserProfile | null | undefined): PortalRole {
  const primary = resolvePrimaryRole(profile);
  if (primary === PortalRole.SUPER_ADMIN) {
    return parseEnumOrDefault(profile?.activeRole, ALL_PORTAL_ROLES, PortalRole.SCHOOL_ADMIN);
  }
  return primary;
}

export function parseExtraCapabilities(rawList: string[] | null | undefined): Set<RoleCapability> {
  const allCaps = Object.values(RoleCapability);
  const result = new Set<RoleCapability>();
  if (!rawList) return result;
  for (const item of rawList) {
    const match = allCaps.find((c) => c.toUpperCase() === item.trim().toUpperCase());
    if (match) result.add(match);
  }
  return result;
}
