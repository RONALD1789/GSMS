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

async function seedSchoolForAlice() {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(5000);
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_oakridge").set({
      ownerId: ALICE_UID,
      name: "Oakridge International Academy",
      code: "OIA-01",
      country: "United Kingdom",
      city: "London",
      plan: "ENTERPRISE",
      subscriptionStatus: "ACTIVE",
      brandColorHex: "#0E7490",
      activeAcademicYear: "2026/2027",
      activeTerm: "Term 1",
      enabledModules: ["ACADEMICS", "ATTENDANCE", "REPORT_CARDS", "FINANCE", "LEAVE_MANAGEMENT", "AI_TOOLS"],
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

test("User profile isolation and strict schema enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Dr. Alice Morgan",
      email: "alice@oakridge.edu",
      activeSchoolId: "school_oakridge",
      activeBranchId: "branch_main",
      activeRole: "SCHOOL_ADMIN",
      lowDataMode: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update on activeRole and lowDataMode
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeRole: "HEADTEACHER",
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

test("Tenant isolation and query alignment on /schools and subcollections", async () => {
  const aliceDb = await seedSchoolForAlice();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Filtered query by ownerId succeeds
  await assertSucceeds(
    aliceDb.collection("schools").where("ownerId", "==", ALICE_UID).get()
  );
  // Unfiltered query on /schools fails (Query Enforcer rule)
  await assertFails(aliceDb.collection("schools").get());

  // Cross-tenant access denied for Bob
  await assertFails(bobDb.collection("schools").doc("school_oakridge").get());
  await assertFails(
    bobDb.collection("schools").where("ownerId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("schools").doc("school_oakridge").update({
      name: "Hijacked Academy",
      updatedAt: pastDate(1000),
    })
  );

  // Module registry update by owner succeeds
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_oakridge").update({
      enabledModules: ["ACADEMICS", "FINANCE", "AI_TOOLS"],
      updatedAt: pastDate(1000),
    })
  );
});

test("Report card workflow (DRAFT -> SUBMITTED -> APPROVED -> PUBLISHED) and terminal lock", async () => {
  const aliceDb = await seedSchoolForAlice();
  const ts = pastDate(5000);

  const reportRef = aliceDb
    .collection("schools")
    .doc("school_oakridge")
    .collection("report_cards")
    .doc("rc_001");

  // Teacher creates DRAFT report card (NO id field)
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

  // Submit for review
  await assertSucceeds(
    reportRef.update({
      status: "SUBMITTED",
      version: 2,
      updatedAt: pastDate(3000),
    })
  );

  // Headteacher approves & adds remarks
  await assertSucceeds(
    reportRef.update({
      status: "APPROVED",
      headteacherRemarks: "Promoted with distinction.",
      reviewedBy: "Principal Vance",
      version: 3,
      updatedAt: pastDate(2000),
    })
  );

  // Publish to parent & student
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
  const aliceDb = await seedSchoolForAlice();
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

  // Approve leave
  await assertSucceeds(
    leaveRef.update({
      status: "APPROVED",
      reviewedBy: "HR Director",
      updatedAt: pastDate(1000),
    })
  );

  // Cannot modify an already approved leave request
  await assertFails(
    leaveRef.update({
      status: "REJECTED",
      updatedAt: pastDate(500),
    })
  );

  // Audit log creation succeeds, update and delete strictly denied
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

test("Subcollection Kotlin-aligned writes for students, staff, finance, operations, alerts", async () => {
  const aliceDb = await seedSchoolForAlice();
  const ts = pastDate(3000);
  const schoolDoc = aliceDb.collection("schools").doc("school_oakridge");

  await assertSucceeds(
    schoolDoc.collection("students").doc("stu_1").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      ownerId: ALICE_UID,
      admissionNo: "ADM-2026-001",
      fullName: "Aarav Sharma",
      className: "Grade 10A",
      stream: "STEM",
      parentName: "Priya Sharma",
      parentContact: "+44 7700 900123",
      attendanceRate: 96.5,
      averageScore: 91.2,
      feeBalanceUsd: 0.0,
      riskLevel: "LOW",
      welfareStatus: "Cleared",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    schoolDoc.collection("finance").doc("fin_1").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      academicYear: "2026/2027",
      term: "Term 1",
      ownerId: ALICE_UID,
      entryType: "INVOICE",
      referenceNo: "INV-9001",
      title: "Term 1 Tuition & Lab Fee",
      partyName: "Aarav Sharma",
      category: "Tuition",
      amountUsd: 1850.0,
      paidUsd: 1850.0,
      status: "PAID",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    schoolDoc.collection("operations").doc("op_1").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      ownerId: ALICE_UID,
      moduleType: "VEHICLE",
      title: "Bus GSMS-04 (North Route)",
      subtitle: "Driver: Samuel Osei • 42 Seats",
      partyName: "North Suburb Express",
      status: "ON_ROUTE",
      metricLabel: "98% On-Time",
      notes: "GPS telemetry normal",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    schoolDoc.collection("alerts").doc("alt_1").set({
      schoolId: "school_oakridge",
      branchId: "branch_main",
      ownerId: ALICE_UID,
      category: "ATTENDANCE",
      riskLevel: "HIGH",
      targetName: "Liam O'Connor",
      reason: "Attendance fell to 74% over last 15 school days.",
      recommendedAction: "Schedule counselor & parent check-in within 48 hours.",
      assignedRole: "WELFARE_NURSE",
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered queries matching repository layer
  await assertSucceeds(
    schoolDoc.collection("students").where("ownerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    schoolDoc.collection("finance").where("ownerId", "==", ALICE_UID).get()
  );
  // Unfiltered subcollection query must fail
  await assertFails(schoolDoc.collection("students").get());
});
