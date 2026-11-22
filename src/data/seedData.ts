import {
  AcademicRecord,
  AttendanceRecord,
  AuditLog,
  Branch,
  CommunicationItem,
  DEFAULT_ENABLED_MODULES,
  EarlyWarningAlert,
  FinanceEntry,
  GsmsModule,
  LeaveRequest,
  LeaveStatus,
  OperationRecord,
  PortalRole,
  ReportCard,
  ReportStatus,
  ReportTemplate,
  RiskLevel,
  School,
  StaffMember,
  Student,
  UserAccessAssignment,
  UserProfile
} from '../types/gsms.ts';

export interface SchoolTenantData {
  branches: Branch[];
  accessAssignments: UserAccessAssignment[];
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

export interface MultiTenantStore {
  userProfile: UserProfile;
  schools: School[];
  tenants: Record<string, SchoolTenantData>;
}

export function createInitialMultiTenantStore(
  uid: string,
  displayName: string,
  email: string | null
): MultiTenantStore {
  const shortUid = uid.replace(/[^a-zA-Z0-9]/g, '').slice(0, 6) || 'gsms01';
  const mainSchoolId = `school_oakridge_${shortUid}`;
  const secondSchoolId = `school_horizon_${shortUid}`;
  const thirdSchoolId = `school_pacific_${shortUid}`;

  const userProfile: UserProfile = {
    id: uid,
    userId: uid,
    displayName: displayName.trim() || 'GSMS Executive',
    email,
    primaryRole: PortalRole.SUPER_ADMIN,
    activeSchoolId: mainSchoolId,
    activeBranchId: 'branch_main',
    activeRole: PortalRole.SCHOOL_ADMIN,
    assignedSchoolId: mainSchoolId,
    assignedBranchIds: ['branch_main', 'branch_north'],
    assignedClasses: ['Grade 10A'],
    assignedSubjects: ['Mathematics', 'Physics'],
    linkedStudentIds: ['stu_1'],
    extraPermissions: [],
    lowDataMode: false
  };

  const schools: School[] = [
    {
      id: mainSchoolId,
      ownerId: uid,
      name: 'Oakridge International Academy',
      code: 'OIA-UK',
      country: 'United Kingdom',
      city: 'London',
      plan: 'ENTERPRISE',
      subscriptionStatus: 'ACTIVE',
      brandColorHex: '#1D4ED8',
      activeAcademicYear: '2026/2027',
      activeTerm: 'Term 1',
      enabledModules: [...DEFAULT_ENABLED_MODULES],
      studentCount: 1240,
      staffCount: 92,
      monthlyRevenueUsd: 4850,
      churnRiskScore: 8
    },
    {
      id: secondSchoolId,
      ownerId: uid,
      name: 'Horizon STEM College',
      code: 'HSC-KE',
      country: 'Kenya',
      city: 'Nairobi',
      plan: 'ENTERPRISE',
      subscriptionStatus: 'ACTIVE',
      brandColorHex: '#059669',
      activeAcademicYear: '2026/2027',
      activeTerm: 'Term 1',
      enabledModules: [...DEFAULT_ENABLED_MODULES],
      studentCount: 980,
      staffCount: 68,
      monthlyRevenueUsd: 3600,
      churnRiskScore: 14
    },
    {
      id: thirdSchoolId,
      ownerId: uid,
      name: 'Pacific Crest Preparatory',
      code: 'PCP-SG',
      country: 'Singapore',
      city: 'Singapore',
      plan: 'PRO',
      subscriptionStatus: 'AT_RISK',
      brandColorHex: '#7C3AED',
      activeAcademicYear: '2026/2027',
      activeTerm: 'Term 1',
      enabledModules: DEFAULT_ENABLED_MODULES.filter((m) => m !== GsmsModule.HOSTEL_BOARDING),
      studentCount: 540,
      staffCount: 41,
      monthlyRevenueUsd: 1950,
      churnRiskScore: 74
    }
  ];

  const mainTenant: SchoolTenantData = {
    branches: [
      {
        id: 'branch_main',
        schoolId: mainSchoolId,
        ownerId: uid,
        name: 'Main Campus (London Central)',
        code: 'BR-01',
        city: 'London',
        mainCampus: true
      },
      {
        id: 'branch_north',
        schoolId: mainSchoolId,
        ownerId: uid,
        name: 'North STEM Wing',
        code: 'BR-02',
        city: 'London',
        mainCampus: false
      }
    ],
    accessAssignments: [
      {
        id: 'assign_teacher_kamau',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Dr. Samuel Kamau',
        email: 'kamau@oakridge.edu',
        primaryRole: PortalRole.TEACHER,
        assignedBranchIds: ['branch_main'],
        assignedClasses: ['Grade 10A'],
        assignedSubjects: ['Mathematics', 'Physics'],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_class_teacher_priya',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Priya Nair',
        email: 'priya.nair@oakridge.edu',
        primaryRole: PortalRole.CLASS_TEACHER,
        assignedBranchIds: ['branch_main'],
        assignedClasses: ['Grade 10A', 'Grade 9C'],
        assignedSubjects: ['English'],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_deputy_head',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Dr. Miriam Mensah',
        email: 'deputy@oakridge.edu',
        primaryRole: PortalRole.DEPUTY_HEADTEACHER,
        assignedBranchIds: ['branch_main', 'branch_north'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_exam_officer',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Prof. Arthur Pendelton',
        email: 'exams@oakridge.edu',
        primaryRole: PortalRole.EXAMINATION_OFFICER,
        assignedBranchIds: ['branch_main'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_parent_wei',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Wei Lin (Guardian)',
        email: 'wei.lin@family.org',
        primaryRole: PortalRole.PARENT,
        assignedBranchIds: ['branch_main'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: ['stu_1'],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_student_maya',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Maya Lin (Learner)',
        email: 'maya.lin@oakridge.edu',
        primaryRole: PortalRole.STUDENT,
        assignedBranchIds: ['branch_main'],
        assignedClasses: ['Grade 10A'],
        assignedSubjects: [],
        linkedStudentIds: ['stu_1'],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_procurement',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Kofi Boateng',
        email: 'procurement@oakridge.edu',
        primaryRole: PortalRole.PROCUREMENT_OFFICER,
        assignedBranchIds: ['branch_main'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_storekeeper',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'James Osei',
        email: 'stores@oakridge.edu',
        primaryRole: PortalRole.STOREKEEPER,
        assignedBranchIds: ['branch_main'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_counsellor',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Dr. Nadia Hassan',
        email: 'counsellor@oakridge.edu',
        primaryRole: PortalRole.SCHOOL_COUNSELLOR,
        assignedBranchIds: ['branch_main'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      },
      {
        id: 'assign_ict',
        schoolId: mainSchoolId,
        ownerId: uid,
        fullName: 'Victor Chen',
        email: 'ict.admin@oakridge.edu',
        primaryRole: PortalRole.ICT_ADMIN,
        assignedBranchIds: ['branch_main', 'branch_north'],
        assignedClasses: [],
        assignedSubjects: [],
        linkedStudentIds: [],
        extraPermissions: [],
        active: true,
        updatedBy: 'GSMS Provisioning Engine'
      }
    ],
    students: [
      {
        id: 'stu_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        admissionNo: 'ADM-2026-101',
        fullName: 'Maya Lin',
        className: 'Grade 10A',
        stream: 'STEM',
        parentName: 'Wei Lin',
        parentContact: '+44 7700 900111',
        attendanceRate: 97.5,
        averageScore: 92.4,
        feeBalanceUsd: 0,
        riskLevel: RiskLevel.LOW,
        welfareStatus: 'Cleared'
      },
      {
        id: 'stu_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        admissionNo: 'ADM-2026-102',
        fullName: "Liam O'Connor",
        className: 'Grade 10A',
        stream: 'STEM',
        parentName: "Claire O'Connor",
        parentContact: '+44 7700 900222',
        attendanceRate: 74.0,
        averageScore: 61.5,
        feeBalanceUsd: 1450,
        riskLevel: RiskLevel.HIGH,
        welfareStatus: 'Counselor Review'
      },
      {
        id: 'stu_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        admissionNo: 'ADM-2026-103',
        fullName: 'Amina Yusuf',
        className: 'Grade 11B',
        stream: 'Humanities',
        parentName: 'Tariq Yusuf',
        parentContact: '+44 7700 900333',
        attendanceRate: 94.0,
        averageScore: 86.8,
        feeBalanceUsd: 320,
        riskLevel: RiskLevel.LOW,
        welfareStatus: 'Cleared'
      },
      {
        id: 'stu_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        admissionNo: 'ADM-2026-104',
        fullName: 'Mateo Silva',
        className: 'Grade 9C',
        stream: 'General',
        parentName: 'Sofia Silva',
        parentContact: '+44 7700 900444',
        attendanceRate: 68.5,
        averageScore: 54.0,
        feeBalanceUsd: 2100,
        riskLevel: RiskLevel.CRITICAL,
        welfareStatus: 'Medical Follow-Up'
      },
      {
        id: 'stu_5',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        admissionNo: 'ADM-2026-105',
        fullName: 'Zara Mensah',
        className: 'Grade 12A',
        stream: 'IB Science',
        parentName: 'Kwame Mensah',
        parentContact: '+44 7700 900555',
        attendanceRate: 98.2,
        averageScore: 95.1,
        feeBalanceUsd: 0,
        riskLevel: RiskLevel.LOW,
        welfareStatus: 'Cleared'
      }
    ],
    staff: [
      {
        id: 'stf_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-01',
        fullName: 'Dr. Samuel Kamau',
        roleTitle: 'Head of Mathematics',
        department: 'STEM',
        subjectsSummary: 'Grade 10A & 12A Math',
        leaveBalanceDays: 18,
        leaveUsedDays: 3,
        onLeave: false,
        workloadHours: 24
      },
      {
        id: 'stf_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-02',
        fullName: 'Elena Rostova',
        roleTitle: 'Senior Physics Teacher',
        department: 'STEM',
        subjectsSummary: 'Grade 10A & 11B Physics',
        leaveBalanceDays: 16,
        leaveUsedDays: 5,
        onLeave: true,
        workloadHours: 22
      },
      {
        id: 'stf_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-03',
        fullName: 'Priya Nair',
        roleTitle: 'English & Literature Lead',
        department: 'Humanities',
        subjectsSummary: 'Grade 9C & 10A English',
        leaveBalanceDays: 21,
        leaveUsedDays: 0,
        onLeave: false,
        workloadHours: 20
      },
      {
        id: 'stf_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-04',
        fullName: 'David Vance',
        roleTitle: 'Principal / Headteacher',
        department: 'Leadership',
        subjectsSummary: 'Academic Governance',
        leaveBalanceDays: 20,
        leaveUsedDays: 1,
        onLeave: false,
        workloadHours: 30
      },
      {
        id: 'stf_5',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-05',
        fullName: 'Nurse Grace Okello',
        roleTitle: 'Lead School Nurse',
        department: 'Welfare',
        subjectsSummary: 'Campus Health Clinic',
        leaveBalanceDays: 19,
        leaveUsedDays: 2,
        onLeave: false,
        workloadHours: 35
      },
      {
        id: 'stf_6',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-06',
        fullName: 'Dr. Miriam Mensah',
        roleTitle: 'Deputy Headteacher',
        department: 'Leadership',
        subjectsSummary: 'Curriculum & Discipline',
        leaveBalanceDays: 21,
        leaveUsedDays: 0,
        onLeave: false,
        workloadHours: 28
      },
      {
        id: 'stf_7',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffNo: 'STF-07',
        fullName: 'Victor Chen',
        roleTitle: 'ICT Administrator',
        department: 'ICT Systems',
        subjectsSummary: 'RBAC, Network & Smart Labs',
        leaveBalanceDays: 20,
        leaveUsedDays: 1,
        onLeave: false,
        workloadHours: 32
      }
    ],
    academics: [
      {
        id: 'acad_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'TIMETABLE',
        title: 'Advanced Calculus Period 1',
        className: 'Grade 10A',
        subjectName: 'Mathematics',
        teacherName: 'Dr. Samuel Kamau',
        scheduleSlot: 'Mon 08:30 - 09:30',
        score: 0,
        maxScore: 100,
        grade: '-'
      },
      {
        id: 'acad_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'TIMETABLE',
        title: 'Quantum & Mechanics Lab',
        className: 'Grade 10A',
        subjectName: 'Physics',
        teacherName: 'Elena Rostova',
        scheduleSlot: 'Mon 10:00 - 11:30',
        score: 0,
        maxScore: 100,
        grade: '-'
      },
      {
        id: 'acad_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'EXAM',
        title: 'Term 1 Mid-Term STEM Mock',
        className: 'Grade 10A',
        subjectName: 'Mathematics',
        teacherName: 'Dr. Samuel Kamau',
        scheduleSlot: '2026-11-28 09:00',
        score: 88.5,
        maxScore: 100,
        grade: 'A-'
      },
      {
        id: 'acad_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'MARK',
        title: 'Maya Lin • Mathematics Mid-Term',
        className: 'Grade 10A',
        subjectName: 'Mathematics',
        teacherName: 'Dr. Samuel Kamau',
        scheduleSlot: 'Graded',
        score: 94,
        maxScore: 100,
        grade: 'A'
      },
      {
        id: 'acad_5',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'MARK',
        title: "Liam O'Connor • Physics Lab",
        className: 'Grade 10A',
        subjectName: 'Physics',
        teacherName: 'Elena Rostova',
        scheduleSlot: 'Graded',
        score: 58,
        maxScore: 100,
        grade: 'C-'
      },
      {
        id: 'acad_6',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'EXAM',
        title: 'World Literature Essay Assessment',
        className: 'Grade 11B',
        subjectName: 'English',
        teacherName: 'Priya Nair',
        scheduleSlot: '2026-11-30 11:00',
        score: 86,
        maxScore: 100,
        grade: 'B+'
      },
      {
        id: 'acad_7',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        recordType: 'TIMETABLE',
        title: 'Cell Biology & Genetics',
        className: 'Grade 11B',
        subjectName: 'Biology',
        teacherName: 'Dr. Miriam Mensah',
        scheduleSlot: 'Tue 09:00 - 10:30',
        score: 0,
        maxScore: 100,
        grade: '-'
      }
    ],
    attendance: [
      {
        id: 'att_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        targetType: 'STUDENT',
        targetName: 'Maya Lin',
        className: 'Grade 10A',
        dateIso: '2026-11-21',
        status: 'PRESENT'
      },
      {
        id: 'att_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        targetType: 'STUDENT',
        targetName: "Liam O'Connor",
        className: 'Grade 10A',
        dateIso: '2026-11-21',
        status: 'ABSENT'
      },
      {
        id: 'att_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        targetType: 'STUDENT',
        targetName: 'Amina Yusuf',
        className: 'Grade 11B',
        dateIso: '2026-11-21',
        status: 'PRESENT'
      },
      {
        id: 'att_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        targetType: 'STUDENT',
        targetName: 'Mateo Silva',
        className: 'Grade 9C',
        dateIso: '2026-11-21',
        status: 'LATE'
      }
    ],
    reportTemplates: [
      {
        id: 'tpl_senior',
        schoolId: mainSchoolId,
        ownerId: uid,
        name: 'Senior Secondary Term Report',
        classLevel: 'Grade 10 - Grade 12',
        sections: ['Core Subjects', 'Attendance & Conduct', 'Teacher Narrative', 'Principal Endorsement'],
        gradingScale: 'A (90-100), B (80-89), C (70-79), D (60-69)',
        active: true
      }
    ],
    reportCards: [
      {
        id: 'rc_maya',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        studentId: 'stu_1',
        studentName: 'Maya Lin',
        className: 'Grade 10A',
        templateId: 'tpl_senior',
        templateName: 'Senior Secondary Term Report',
        subjectGradesSummary: 'Math: 94 (A), Physics: 91 (A), English: 92 (A)',
        averageScore: 92.3,
        teacherRemarks: 'Outstanding analytical precision across STEM labs.',
        aiSummary: 'Top 5% academic trajectory; ready for Olympiad enrichment.',
        headteacherRemarks: 'Commended for Honor Roll distinction.',
        status: ReportStatus.PUBLISHED,
        version: 3,
        submittedBy: 'Dr. Samuel Kamau',
        reviewedBy: 'Principal David Vance'
      },
      {
        id: 'rc_liam',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        studentId: 'stu_2',
        studentName: "Liam O'Connor",
        className: 'Grade 10A',
        templateId: 'tpl_senior',
        templateName: 'Senior Secondary Term Report',
        subjectGradesSummary: 'Math: 64 (D), Physics: 58 (F), English: 71 (C)',
        averageScore: 64.3,
        teacherRemarks: 'Needs structured weekly tutoring in Mechanics.',
        aiSummary: 'Attendance drop correlates with 14% Physics decline; recommend remedial plan.',
        headteacherRemarks: null,
        status: ReportStatus.SUBMITTED,
        version: 1,
        submittedBy: 'Dr. Samuel Kamau',
        reviewedBy: null
      },
      {
        id: 'rc_amina',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        studentId: 'stu_3',
        studentName: 'Amina Yusuf',
        className: 'Grade 11B',
        templateId: 'tpl_senior',
        templateName: 'Senior Secondary Term Report',
        subjectGradesSummary: 'English: 89 (A-), Biology: 86 (B+), History: 85 (B)',
        averageScore: 86.7,
        teacherRemarks: 'Thoughtful seminar leadership and analytical writing.',
        aiSummary: null,
        headteacherRemarks: null,
        status: ReportStatus.DRAFT,
        version: 1,
        submittedBy: 'Priya Nair',
        reviewedBy: null
      }
    ],
    finance: [
      {
        id: 'fin_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        entryType: 'INVOICE',
        referenceNo: 'INV-2026-101',
        title: 'Term 1 Tuition - Maya Lin',
        partyName: 'Wei Lin',
        category: 'Tuition',
        amountUsd: 2400,
        paidUsd: 2400,
        status: 'PAID'
      },
      {
        id: 'fin_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        entryType: 'INVOICE',
        referenceNo: "INV-2026-102",
        title: "Term 1 Tuition - Liam O'Connor",
        partyName: "Claire O'Connor",
        category: 'Tuition',
        amountUsd: 2400,
        paidUsd: 950,
        status: 'OVERDUE'
      },
      {
        id: 'fin_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        entryType: 'EXPENSE',
        referenceNo: 'EXP-2026-044',
        title: 'STEM Physics Lab Equipment',
        partyName: 'OptiLab UK',
        category: 'Laboratory',
        amountUsd: 3200,
        paidUsd: 3200,
        status: 'PAID'
      },
      {
        id: 'fin_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        academicYear: '2026/2027',
        term: 'Term 1',
        ownerId: uid,
        entryType: 'PAYROLL',
        referenceNo: 'PAY-2026-NOV',
        title: 'November Faculty Payroll Batch',
        partyName: 'All Teaching Staff',
        category: 'Payroll',
        amountUsd: 48500,
        paidUsd: 48500,
        status: 'PAID'
      }
    ],
    leaveRequests: [
      {
        id: 'lv_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffId: 'stf_2',
        staffName: 'Elena Rostova',
        roleTitle: 'Senior Physics Teacher',
        leaveType: 'STUDY',
        startDate: '2026-11-19',
        endDate: '2026-11-23',
        daysCount: 5,
        reason: 'STEM Curriculum Accreditation Workshop',
        attachmentNote: null,
        status: LeaveStatus.APPROVED,
        reviewedBy: 'Principal David Vance',
        aiConflictNote: 'Covered Grade 10A lab via Dr. Samuel Kamau.'
      },
      {
        id: 'lv_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        staffId: 'stf_3',
        staffName: 'Priya Nair',
        roleTitle: 'English & Literature Lead',
        leaveType: 'ANNUAL',
        startDate: '2026-12-02',
        endDate: '2026-12-05',
        daysCount: 4,
        reason: 'Family wedding out of city',
        attachmentNote: null,
        status: LeaveStatus.PENDING,
        reviewedBy: null,
        aiConflictNote: '2 Grade 10A English essay workshops overlap; 2 humanities substitutes available.'
      }
    ],
    communications: [
      {
        id: 'comm_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        channel: 'ANNOUNCEMENT',
        audience: 'All Parents & Teachers',
        title: 'Term 1 Mid-Term Exams & Parent Conferences',
        body: 'Mid-term assessments commence Nov 28. Approved report cards will publish directly to Parent Portals.',
        senderName: 'Principal David Vance',
        senderRole: PortalRole.HEADTEACHER,
        aiDrafted: true
      }
    ],
    operations: [
      {
        id: 'op_lib1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'LIBRARY_BOOK',
        title: 'Linear Algebra Done Right',
        subtitle: 'Sheldon Axler • STEM Shelf B2',
        partyName: 'Library Catalog',
        status: 'AVAILABLE',
        metricLabel: '12 Copies',
        notes: 'High demand for Grade 12 IB'
      },
      {
        id: 'op_lib2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'LIBRARY_BORROW',
        title: 'Quantum Physics Primer (Overdue)',
        subtitle: "Borrower: Liam O'Connor",
        partyName: "Liam O'Connor",
        status: 'OVERDUE',
        metricLabel: '4 Days Late',
        notes: 'Auto reminder queued'
      },
      {
        id: 'op_trn1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'VEHICLE',
        title: 'Bus GSMS-04 • North Route',
        subtitle: 'Driver: Samuel Osei • 42 Seats',
        partyName: 'North Wing Express',
        status: 'ON_ROUTE',
        metricLabel: '98% On-Time',
        notes: 'Zero maintenance alerts'
      },
      {
        id: 'op_trn2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'ROUTE',
        title: 'Westlands & Riverside Loop',
        subtitle: '18 Stops • 42 Students Assigned',
        partyName: 'Transport Control',
        status: 'DELAY_RISK',
        metricLabel: '+7 min traffic',
        notes: 'AI recommends 07:10 departure shift'
      },
      {
        id: 'op_wel1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'WELFARE',
        title: 'Clinic Visit: Mateo Silva',
        subtitle: 'Seasonal asthma check & inhaler log',
        partyName: 'Nurse Grace Okello',
        status: 'FOLLOW_UP',
        metricLabel: 'Priority Check',
        notes: 'Parent notified via SMS'
      },
      {
        id: 'op_dis1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'DISCIPLINE',
        title: 'Lab Safety Protocol Reminder',
        subtitle: 'Grade 10A Chemistry Bench 3',
        partyName: 'Dr. Samuel Kamau',
        status: 'RESOLVED',
        metricLabel: 'Minor',
        notes: 'Safety briefing completed'
      },
      {
        id: 'op_inv1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'ASSET',
        title: 'Smart Interactive Boards (x24)',
        subtitle: 'North STEM Wing Classrooms',
        partyName: 'IT & Facilities',
        status: 'OPERATIONAL',
        metricLabel: '$38,400 Value',
        notes: 'Annual warranty active'
      },
      {
        id: 'op_hos1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'HOSTEL',
        title: 'Falcon Boarding House',
        subtitle: 'Occupancy: 118 / 120 Beds',
        partyName: 'Matron Hannah Reid',
        status: 'ACTIVE',
        metricLabel: '98% Full',
        notes: 'Weekend curfew audit clean'
      },
      {
        id: 'op_adm1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'ADMISSION',
        title: 'Inquiry: Sophia Chen (Grade 9)',
        subtitle: "Transfer from Zurich Int'l",
        partyName: 'Admissions Desk',
        status: 'INTERVIEW',
        metricLabel: '91% Likelihood',
        notes: 'Transcripts & passport verified'
      },
      {
        id: 'op_evt1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'EVENT',
        title: 'Annual Global STEM Fair 2026',
        subtitle: 'Dec 12 • Main Glass Atrium',
        partyName: 'Events Committee',
        status: 'SCHEDULED',
        metricLabel: '420 RSVPs',
        notes: 'Live parent stream enabled'
      },
      {
        id: 'op_proc1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'PROCUREMENT',
        title: 'PO-2026-88: Optics Lab Microscopes',
        subtitle: 'Vendor: OptiLab UK • 20 Units',
        partyName: 'Kofi Boateng',
        status: 'RFQ_APPROVED',
        metricLabel: '$6,400 PO',
        notes: '3 vendor quotes compared; 14% savings'
      },
      {
        id: 'op_proc2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'PROCUREMENT',
        title: 'RFQ-2026-91: Exam Stationery Batch',
        subtitle: 'Vendor: Civic Paper Ltd • 500 Reams',
        partyName: 'Kofi Boateng',
        status: 'PENDING_PO',
        metricLabel: '$1,850 Est.',
        notes: 'Awaiting storekeeper stock verification'
      },
      {
        id: 'op_store1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'STOCK_MOVEMENT',
        title: 'Chemistry Titration Kits Reorder',
        subtitle: 'Central Storeroom Aisle 4',
        partyName: 'James Osei',
        status: 'LOW_STOCK',
        metricLabel: '8 Packs Left',
        notes: 'Below reorder threshold of 15 packs'
      },
      {
        id: 'op_couns1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'COUNSELLING',
        title: "Pastoral Check-In: Liam O'Connor",
        subtitle: 'Academic stress & attendance coaching',
        partyName: 'Dr. Nadia Hassan',
        status: 'IN_PROGRESS',
        metricLabel: 'Session 2/4',
        notes: 'Parent conference scheduled Friday'
      },
      {
        id: 'op_desk1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'FRONT_DESK',
        title: 'Visitor Pass #V-402: Cambridge Inspector',
        subtitle: 'Main Reception Gate A',
        partyName: 'Front Desk Team',
        status: 'CHECKED_IN',
        metricLabel: 'Badge Issued',
        notes: 'Escorted to Principal Conference Room'
      },
      {
        id: 'op_ict1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        moduleType: 'ICT_TICKET',
        title: 'RBAC & SSO Audit • North Wing Wi-Fi',
        subtitle: 'Core Switch Rack B',
        partyName: 'Victor Chen',
        status: 'ACTIVE',
        metricLabel: '99.94% Uptime',
        notes: '256-bit TLS & App Check enforced'
      }
    ],
    alerts: [
      {
        id: 'alt_1',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        category: 'ACADEMIC',
        riskLevel: RiskLevel.HIGH,
        targetName: "Liam O'Connor",
        reason: 'Physics score dropped 14% alongside 74% attendance.',
        recommendedAction: "Assign peer math/physics tutor & notify parent Claire O'Connor.",
        assignedRole: PortalRole.HEADTEACHER,
        status: 'OPEN'
      },
      {
        id: 'alt_2',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        category: 'FINANCE',
        riskLevel: RiskLevel.CRITICAL,
        targetName: 'Invoice INV-2026-102',
        reason: 'Balance $1,450 overdue by 21 days.',
        recommendedAction: 'Send staggered installment reminder via Finance Portal.',
        assignedRole: PortalRole.FINANCE_OFFICER,
        status: 'OPEN'
      },
      {
        id: 'alt_3',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        category: 'STAFFING',
        riskLevel: RiskLevel.MEDIUM,
        targetName: 'Humanities Department',
        reason: 'Pending leave request lv_2 overlaps 2 Grade 10A workshops.',
        recommendedAction: 'Assign substitute teacher before approving leave.',
        assignedRole: PortalRole.HR_MANAGER,
        status: 'IN_PROGRESS'
      },
      {
        id: 'alt_4',
        schoolId: mainSchoolId,
        branchId: 'branch_main',
        ownerId: uid,
        category: 'ICT',
        riskLevel: RiskLevel.MEDIUM,
        targetName: 'North Wing Smart Lab',
        reason: '2 interactive boards require firmware patch before exams.',
        recommendedAction: 'Deploy remote MDM patch tonight.',
        assignedRole: PortalRole.ICT_ADMIN,
        status: 'OPEN'
      }
    ],
    auditLogs: [
      {
        id: 'log_init',
        schoolId: mainSchoolId,
        ownerId: uid,
        actorName: 'GSMS Provisioning Engine',
        actorRole: PortalRole.SUPER_ADMIN,
        actionType: 'TENANT_INITIALIZED',
        moduleName: 'SYSTEM',
        summary: 'Provisioned multi-tenant workspace for Oakridge International Academy with 19 portals & RBAC.',
        aiAction: false,
        createdAtIso: '2026-11-21T08:00:00Z'
      }
    ]
  };

  function makeSecondaryTenant(schoolId: string, schoolName: string, city: string, isSingapore: boolean): SchoolTenantData {
    return {
      branches: [
        {
          id: 'branch_main',
          schoolId,
          ownerId: uid,
          name: `${schoolName} Main Campus`,
          code: 'BR-01',
          city,
          mainCampus: true
        }
      ],
      accessAssignments: [],
      students: [
        {
          id: `stu_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          ownerId: uid,
          admissionNo: 'ADM-2026-801',
          fullName: isSingapore ? 'Ethan Tan' : 'Brian Kiprop',
          className: 'Grade 11A',
          stream: 'Science',
          parentName: isSingapore ? 'Mei Tan' : 'Grace Kiprop',
          parentContact: null,
          attendanceRate: 93.4,
          averageScore: 84.5,
          feeBalanceUsd: 250,
          riskLevel: RiskLevel.LOW,
          welfareStatus: 'Cleared'
        }
      ],
      staff: [
        {
          id: `stf_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          ownerId: uid,
          staffNo: 'STF-11',
          fullName: isSingapore ? 'Dr. Evelyn Lim' : 'Prof. Wanjiku Njoroge',
          roleTitle: 'Campus Principal',
          department: 'Leadership',
          subjectsSummary: 'Academic Oversight',
          leaveBalanceDays: 21,
          leaveUsedDays: 0,
          onLeave: false,
          workloadHours: 28
        }
      ],
      academics: [
        {
          id: `acad_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          academicYear: '2026/2027',
          term: 'Term 1',
          ownerId: uid,
          recordType: 'TIMETABLE',
          title: 'STEM Applied Physics',
          className: 'Grade 11A',
          subjectName: 'Physics',
          teacherName: isSingapore ? 'Dr. Evelyn Lim' : 'Prof. Wanjiku Njoroge',
          scheduleSlot: 'Mon 09:00 - 10:30',
          score: 85,
          maxScore: 100,
          grade: 'B+'
        }
      ],
      attendance: [
        {
          id: `att_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          academicYear: '2026/2027',
          term: 'Term 1',
          ownerId: uid,
          targetType: 'STUDENT',
          targetName: isSingapore ? 'Ethan Tan' : 'Brian Kiprop',
          className: 'Grade 11A',
          dateIso: '2026-11-21',
          status: 'PRESENT'
        }
      ],
      reportTemplates: [
        {
          id: `tpl_${schoolId}_1`,
          schoolId,
          ownerId: uid,
          name: `${schoolName} Term Assessment Report`,
          classLevel: 'Grade 9 - Grade 12',
          sections: ['Core Subjects', 'Attendance', 'Teacher Remarks', 'Principal Endorsement'],
          gradingScale: 'A-F Percentage',
          active: true
        }
      ],
      reportCards: [
        {
          id: `rc_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          academicYear: '2026/2027',
          term: 'Term 1',
          ownerId: uid,
          studentId: `stu_${schoolId}_1`,
          studentName: isSingapore ? 'Ethan Tan' : 'Brian Kiprop',
          className: 'Grade 11A',
          templateId: `tpl_${schoolId}_1`,
          templateName: `${schoolName} Term Assessment Report`,
          subjectGradesSummary: 'Physics: 86 (B+), Math: 84 (B)',
          averageScore: 84.5,
          teacherRemarks: 'Steady progress across lab assignments.',
          aiSummary: null,
          headteacherRemarks: 'Approved for Term 1 release.',
          status: ReportStatus.PUBLISHED,
          version: 1,
          submittedBy: isSingapore ? 'Dr. Evelyn Lim' : 'Prof. Wanjiku Njoroge',
          reviewedBy: 'Campus Principal'
        }
      ],
      finance: [
        {
          id: `fin_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          academicYear: '2026/2027',
          term: 'Term 1',
          ownerId: uid,
          entryType: 'INVOICE',
          referenceNo: 'INV-2026-801',
          title: `Term 1 Tuition - ${isSingapore ? 'Ethan Tan' : 'Brian Kiprop'}`,
          partyName: isSingapore ? 'Mei Tan' : 'Grace Kiprop',
          category: 'Tuition',
          amountUsd: 1850,
          paidUsd: 1600,
          status: 'PARTIAL'
        }
      ],
      leaveRequests: [],
      communications: [
        {
          id: `comm_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          ownerId: uid,
          channel: 'ANNOUNCEMENT',
          audience: 'All Parents & Staff',
          title: `${schoolName} Term 1 Calendar`,
          body: 'Term 1 continuous assessment schedule is now live.',
          senderName: 'Campus Principal',
          senderRole: PortalRole.HEADTEACHER,
          aiDrafted: false
        }
      ],
      operations: [],
      alerts: [
        {
          id: `alt_${schoolId}_1`,
          schoolId,
          branchId: 'branch_main',
          ownerId: uid,
          category: 'FINANCE',
          riskLevel: isSingapore ? RiskLevel.HIGH : RiskLevel.MEDIUM,
          targetName: schoolName,
          reason: isSingapore
            ? 'Module engagement dropped 18% this month (churn watch).'
            : 'Term 1 lab fee collection at 79%.',
          recommendedAction: 'Schedule executive onboarding check-in & finance digest.',
          assignedRole: PortalRole.SUPER_ADMIN,
          status: 'OPEN'
        }
      ],
      auditLogs: [
        {
          id: `log_${schoolId}_init`,
          schoolId,
          ownerId: uid,
          actorName: 'GSMS Provisioning Engine',
          actorRole: PortalRole.SUPER_ADMIN,
          actionType: 'TENANT_INITIALIZED',
          moduleName: 'SYSTEM',
          summary: `Provisioned isolated school workspace for ${schoolName}.`,
          aiAction: false,
          createdAtIso: '2026-11-21T08:05:00Z'
        }
      ]
    };
  }

  return {
    userProfile,
    schools,
    tenants: {
      [mainSchoolId]: mainTenant,
      [secondSchoolId]: makeSecondaryTenant(secondSchoolId, 'Horizon STEM College', 'Nairobi', false),
      [thirdSchoolId]: makeSecondaryTenant(thirdSchoolId, 'Pacific Crest Preparatory', 'Singapore', true)
    }
  };
}
