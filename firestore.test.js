const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_001";
const BOB_UID = "bob_other_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedProfile(uid, role, overrides = {}) {
  const db = testEnv.authenticatedContext(uid).firestore();
  const ts = pastDate(5000);
  await assertSucceeds(
    db.collection("users").doc(uid).set({
      userId: uid,
      displayName: overrides.displayName || "GSMS User",
      email: overrides.email || "user@oakridge.edu",
      primaryRole: overrides.primaryRole || role,
      activeSchoolId: overrides.activeSchoolId || "school_oakridge",
      activeBranchId: overrides.activeBranchId || "branch_main",
      activeRole: overrides.activeRole || role,
      assignedSchoolId: overrides.assignedSchoolId || "school_oakridge",
      assignedBranchIds: overrides.assignedBranchIds || ["branch_main"],
      assignedClasses: overrides.assignedClasses || [],
      assignedSubjects: overrides.assignedSubjects || [],
      linkedStudentIds: overrides.linkedStudentIds || [],
      extraPermissions: overrides.extraPermissions || [],
      lowDataMode: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  return db;
}

async function seedSchoolForAlice(uid = ALICE_UID) {
  const aliceDb = testEnv.authenticatedContext(uid).firestore();
  const ts = pastDate(5000);
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_oakridge").set({
      ownerId: uid,
      name: "Oakridge International Academy",
      code: "OIA-01",
      country: "United Kingdom",
      city: "London",
      plan: "ENTERPRISE",
      subscriptionStatus: "ACTIVE",
      brandColorHex: "#0E7490",
      activeAcademicYear: "2026/2027",
      activeTerm: "Term 1",
      enabledModules: ["ACADEMICS", "ATTENDANCE", "REPORT_CARDS", "FINANCE", "LEAVE_MANAGEMENT", "PROCUREMENT", "INVENTORY_ASSETS", "ICT_SYSTEMS", "AI_TOOLS"],
      studentCount: 1240,
      staffCount: 86,
      monthlyRevenueUsd: 4200,
      churnRiskScore: 8,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  return aliceDb;
}

test("Unauthenticated users cannot read or write any GSMS collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("schools").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("schools").doc("school_1").set({
      ownerId: ALICE_UID,
      name: "Fake School",
    })
  );
});

test("Anti-escalation: non-SUPER_ADMIN cannot self-promote primaryRole, activeRole, extraPermissions, or assignedSchoolId", async () => {
  const teacherUid = "teacher_locked_001";
  const teacherDb = await seedProfile(teacherUid, "TEACHER", {
    assignedClasses: ["Grade 10A"],
    assignedSubjects: ["Mathematics"],
  });

  // Self-promotion of primaryRole, activeRole, extraPermissions, or assignedSchoolId MUST fail
  await assertFails(
    teacherDb.collection("users").doc(teacherUid).update({
      primaryRole: "SUPER_ADMIN",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    teacherDb.collection("users").doc(teacherUid).update({
      activeRole: "SCHOOL_ADMIN",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    teacherDb.collection("users").doc(teacherUid).update({
      extraPermissions: ["APPROVE_PUBLISH_REPORT_CARDS", "MANAGE_MODULES"],
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    teacherDb.collection("users").doc(teacherUid).update({
      assignedSchoolId: "school_other",
      updatedAt: pastDate(1000),
    })
  );

  // Legitimate low-data toggle succeeds
  await assertSucceeds(
    teacherDb.collection("users").doc(teacherUid).update({
      lowDataMode: true,
      updatedAt: pastDate(1000),
    })
  );
});

test("User profile isolation and strict schema enforcement for SUPER_ADMIN", async () => {
  const aliceDb = await seedProfile(ALICE_UID, "SUPER_ADMIN", { activeRole: "SCHOOL_ADMIN" });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // SUPER_ADMIN can preview/switch activeRole across all 19 portals
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeRole: "EXAMINATION_OFFICER",
      lowDataMode: true,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot read or mutate Alice's profile (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      activeRole: "SUPER_ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost fields and future timestamps
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperClaim: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lowDataMode: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Database-level RBAC blocks STUDENT/PARENT/TEACHER from unauthorized mutations", async () => {
  const studentUid = "student_rbac_001";
  await seedProfile(ALICE_UID, "SUPER_ADMIN", { activeRole: "SCHOOL_ADMIN" });
  await seedSchoolForAlice(ALICE_UID);

  // Seed school under studentUid as owner (simulating authenticated session locked to STUDENT role)
  const studentDb = await seedProfile(studentUid, "STUDENT", { linkedStudentIds: ["stu_1"] });
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const ts = pastDate(5000);
    await adminDb.collection("schools").doc("school_oakridge").set({
      ownerId: studentUid,
      name: "Oakridge International Academy",
      code: "OIA-01",
      country: "United Kingdom",
      city: "London",
      plan: "ENTERPRISE",
      subscriptionStatus: "ACTIVE",
      brandColorHex: "#0E7490",
      activeAcademicYear: "2026/2027",
      activeTerm: "Term 1",
      enabledModules: ["ACADEMICS", "FINANCE", "REPORT_CARDS"],
      studentCount: 100,
      staffCount: 20,
      monthlyRevenueUsd: 2500,
      churnRiskScore: 5,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const schoolDoc = studentDb.collection("schools").doc("school_oakridge");

  // STUDENT cannot toggle modules, enroll students, record payments, or submit report cards
  await assertFails(
    schoolDoc.update({ enabledModules: ["ACADEMICS"], updatedAt: pastDate(1000) })
  );
  await assertFails(
    schoolDoc.collection("finance").doc("fin_bad").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      academicYear: "2026/2027",
      term: "Term 1",
      ownerId: studentUid,
      entryType: "INVOICE",
      referenceNo: "INV-X",
      title: "Hack",
      partyName: "Student",
      category: "Tuition",
      amountUsd: 100,
      paidUsd: 100,
      status: "PAID",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    schoolDoc.collection("access_assignments").doc("assign_hack").set({
      schoolId: "school_oakridge",
      ownerId: studentUid,
      fullName: "Self",
      email: "student@oakridge.edu",
      primaryRole: "SCHOOL_ADMIN",
      assignedBranchIds: ["branch_main"],
      assignedClasses: [],
      assignedSubjects: [],
      linkedStudentIds: [],
      extraPermissions: ["MANAGE_MODULES"],
      active: true,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
});

test("Teacher class scoping & report workflow separation (Teacher submits, Headteacher approves)", async () => {
  const teacherUid = "teacher_scope_001";
  const teacherDb = await seedProfile(teacherUid, "TEACHER", {
    assignedClasses: ["Grade 10A"],
    assignedSubjects: ["Mathematics"],
  });

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const ts = pastDate(5000);
    await adminDb.collection("schools").doc("school_oakridge").set({
      ownerId: teacherUid,
      name: "Oakridge International Academy",
      code: "OIA-01",
      country: "United Kingdom",
      city: "London",
      plan: "ENTERPRISE",
      subscriptionStatus: "ACTIVE",
      brandColorHex: "#0E7490",
      activeAcademicYear: "2026/2027",
      activeTerm: "Term 1",
      enabledModules: ["ACADEMICS", "REPORT_CARDS"],
      studentCount: 100,
      staffCount: 20,
      monthlyRevenueUsd: 2500,
      churnRiskScore: 5,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const schoolDoc = teacherDb.collection("schools").doc("school_oakridge");
  const ts = pastDate(3000);

  // Teacher CAN create DRAFT report for assigned class Grade 10A
  const rep10A = schoolDoc.collection("report_cards").doc("rc_10a");
  await assertSucceeds(
    rep10A.set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      academicYear: "2026/2027",
      term: "Term 1",
      ownerId: teacherUid,
      studentId: "stu_101",
      studentName: "Maya Lin",
      className: "Grade 10A",
      templateId: "tpl_sec",
      templateName: "Senior Secondary",
      subjectGradesSummary: "Math: 94 (A)",
      averageScore: 94.0,
      teacherRemarks: "Strong work.",
      status: "DRAFT",
      version: 1,
      submittedBy: "Dr. Kamau",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Teacher CANNOT create report for unassigned class Grade 11B
  await assertFails(
    schoolDoc.collection("report_cards").doc("rc_11b").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      academicYear: "2026/2027",
      term: "Term 1",
      ownerId: teacherUid,
      studentId: "stu_201",
      studentName: "Amina Yusuf",
      className: "Grade 11B",
      templateId: "tpl_sec",
      templateName: "Senior Secondary",
      subjectGradesSummary: "Math: 88 (B+)",
      averageScore: 88.0,
      status: "DRAFT",
      version: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Teacher CAN submit Grade 10A draft -> SUBMITTED
  await assertSucceeds(
    rep10A.update({
      status: "SUBMITTED",
      version: 2,
      updatedAt: pastDate(1500),
    })
  );

  // Teacher CANNOT approve or publish SUBMITTED report
  await assertFails(
    rep10A.update({
      status: "APPROVED",
      version: 3,
      updatedAt: pastDate(1000),
    })
  );
});

test("Delegated extraPermissions grant capability without changing primaryRole", async () => {
  const adminUid = "ict_admin_001";
  const adminDb = await seedProfile(adminUid, "ICT_ADMIN");
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = pastDate(5000);
    await db.collection("schools").doc("school_oakridge").set({
      ownerId: adminUid,
      name: "Oakridge International Academy",
      code: "OIA-01",
      country: "United Kingdom",
      city: "London",
      plan: "ENTERPRISE",
      subscriptionStatus: "ACTIVE",
      brandColorHex: "#0E7490",
      activeAcademicYear: "2026/2027",
      activeTerm: "Term 1",
      enabledModules: ["ICT_SYSTEMS", "ANALYTICS"],
      studentCount: 100,
      staffCount: 20,
      monthlyRevenueUsd: 2500,
      churnRiskScore: 5,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const assignRef = adminDb
    .collection("schools")
    .doc("school_oakridge")
    .collection("access_assignments")
    .doc("assign_teacher_1");

  await assertSucceeds(
    assignRef.set({
      schoolId: "school_oakridge",
      ownerId: adminUid,
      fullName: "Dr. Samuel Kamau",
      email: "kamau@oakridge.edu",
      primaryRole: "TEACHER",
      assignedBranchIds: ["branch_main"],
      assignedClasses: ["Grade 10A"],
      assignedSubjects: ["Mathematics"],
      linkedStudentIds: [],
      extraPermissions: ["VIEW_ANALYTICS_CHARTS"],
      active: true,
      updatedBy: "ICT Admin",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // ICT Admin can update delegated extraPermissions without changing primaryRole
  await assertSucceeds(
    assignRef.update({
      extraPermissions: ["VIEW_ANALYTICS_CHARTS", "MODERATE_EXAMS"],
      updatedBy: "ICT Admin",
      updatedAt: pastDate(1000),
    })
  );

  // Primary role tampering in access_assignments update is rejected
  await assertFails(
    assignRef.update({
      primaryRole: "SCHOOL_ADMIN",
      updatedAt: pastDate(500),
    })
  );
});

test("Report card workflow (DRAFT -> SUBMITTED -> APPROVED -> PUBLISHED) and terminal lock", async () => {
  const aliceDb = await seedProfile(ALICE_UID, "SUPER_ADMIN", { activeRole: "SCHOOL_ADMIN" });
  await seedSchoolForAlice(ALICE_UID);
  const ts = pastDate(5000);

  const reportRef = aliceDb
    .collection("schools")
    .doc("school_oakridge")
    .collection("report_cards")
    .doc("rc_001");

  await assertSucceeds(
    reportRef.set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      academicYear: "2026/2027",
      term: "Term 1",
      ownerId: ALICE_UID,
      studentId: "stu_101",
      studentName: "Maya Lin",
      className: "Grade 10A",
      templateId: "tpl_sec",
      templateName: "Senior Secondary Standard",
      subjectGradesSummary: "Math: 91 (A), Physics: 88 (A-), English: 86 (B+)",
      averageScore: 88.3,
      teacherRemarks: "Consistent analytical rigor.",
      aiSummary: "Strong STEM progression.",
      status: "DRAFT",
      version: 1,
      submittedBy: "Mr. Kamau",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    reportRef.update({
      status: "SUBMITTED",
      version: 2,
      updatedAt: pastDate(3000),
    })
  );

  await assertSucceeds(
    reportRef.update({
      status: "APPROVED",
      headteacherRemarks: "Promoted with distinction.",
      reviewedBy: "Principal Vance",
      version: 3,
      updatedAt: pastDate(2000),
    })
  );

  await assertSucceeds(
    reportRef.update({
      status: "PUBLISHED",
      version: 4,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: PUBLISHED report card cannot be altered
  await assertFails(
    reportRef.update({
      averageScore: 40.0,
      updatedAt: pastDate(500),
    })
  );
});

test("Leave request workflow, terminal lock after approval, and audit log immutability", async () => {
  const aliceDb = await seedProfile(ALICE_UID, "SUPER_ADMIN", { activeRole: "HR_MANAGER" });
  await seedSchoolForAlice(ALICE_UID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(4000);

  const leaveRef = aliceDb
    .collection("schools")
    .doc("school_oakridge")
    .collection("leave_requests")
    .doc("leave_001");

  await assertSucceeds(
    leaveRef.set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      ownerId: ALICE_UID,
      staffId: "stf_01",
      staffName: "Elena Rostova",
      roleTitle: "Senior Physics Teacher",
      leaveType: "ANNUAL",
      startDate: "2026-12-01",
      endDate: "2026-12-05",
      daysCount: 5,
      reason: "Family travel during regional break.",
      status: "PENDING",
      aiConflictNote: "2 lab sessions require cover by Mr. Patel.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    leaveRef.update({
      status: "APPROVED",
      reviewedBy: "HR Director",
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(
    leaveRef.update({
      status: "REJECTED",
      updatedAt: pastDate(500),
    })
  );

  const auditRef = aliceDb
    .collection("schools")
    .doc("school_oakridge")
    .collection("audit_logs")
    .doc("log_001");

  await assertSucceeds(
    auditRef.set({
      schoolId: "school_oakridge",
      ownerId: ALICE_UID,
      actorName: "HR Director",
      actorRole: "HR_MANAGER",
      actionType: "LEAVE_APPROVED",
      moduleName: "LEAVE_MANAGEMENT",
      summary: "Approved 5 days annual leave for Elena Rostova",
      aiAction: false,
      createdAt: pastDate(1000),
    })
  );

  await assertFails(auditRef.update({ summary: "Tampered" }));
  await assertFails(auditRef.delete());
  await assertFails(
    bobDb
      .collection("schools")
      .doc("school_oakridge")
      .collection("audit_logs")
      .where("ownerId", "==", ALICE_UID)
      .get()
  );
});

test("Subcollection Kotlin-aligned writes for procurement, stores, counselling, front desk, ICT", async () => {
  const aliceDb = await seedProfile(ALICE_UID, "SUPER_ADMIN", { activeRole: "SCHOOL_ADMIN" });
  await seedSchoolForAlice(ALICE_UID);
  const ts = pastDate(3000);
  const schoolDoc = aliceDb.collection("schools").doc("school_oakridge");

  for (const [id, moduleType, title] of [
    ["op_proc", "PROCUREMENT", "RFQ: Lab Microscopes (20 units)"],
    ["op_stock", "STOCK_MOVEMENT", "Chemistry Reagents Reorder"],
    ["op_couns", "COUNSELLING", "Pastoral Follow-up: Liam O'Connor"],
    ["op_desk", "FRONT_DESK", "Parent Inquiry Desk Pass"],
    ["op_ict", "ICT_TICKET", "Campus Wi-Fi 6 Upgrade"],
  ]) {
    await assertSucceeds(
      schoolDoc.collection("operations").doc(id).set({
        schoolId: "school_oakridge",
        branchId: "branch_main",
        ownerId: ALICE_UID,
        moduleType,
        title,
        subtitle: "Verified operational entry",
        partyName: "Operations Team",
        status: "ACTIVE",
        metricLabel: "Tracked",
        notes: "Seeded test",
        createdAt: ts,
        updatedAt: ts,
      })
    );
  }
});
