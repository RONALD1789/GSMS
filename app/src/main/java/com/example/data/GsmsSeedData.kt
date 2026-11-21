package com.example.data

import com.example.model.GsmsModule
import com.example.model.PortalRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object GsmsSeedData {

    suspend fun seedInitialMultiTenantWorkspace(
        db: FirebaseFirestore,
        uid: String,
        displayName: String,
        email: String?
    ) {
        val mainSchoolId = "school_oakridge_${uid.take(6)}"
        val secondSchoolId = "school_horizon_${uid.take(6)}"
        val thirdSchoolId = "school_pacific_${uid.take(6)}"

        // 1. Commit user profile & school root docs FIRST so exists(/schools/{id}) passes for subcollections
        val rootBatch = db.batch()
        val userRef = db.collection("users").document(uid)
        val userPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to displayName.ifBlank { "GSMS Executive" }.take(80),
            "activeSchoolId" to mainSchoolId,
            "activeBranchId" to "branch_main",
            "activeRole" to PortalRole.SCHOOL_ADMIN.name,
            "lowDataMode" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!email.isNullOrBlank()) {
            userPayload["email"] = email.take(120)
        }
        rootBatch.set(userRef, userPayload)

        val school1Ref = db.collection("schools").document(mainSchoolId)
        rootBatch.set(
            school1Ref,
            mapOf(
                "ownerId" to uid,
                "name" to "Oakridge International Academy",
                "code" to "OIA-UK",
                "country" to "United Kingdom",
                "city" to "London",
                "plan" to "ENTERPRISE",
                "subscriptionStatus" to "ACTIVE",
                "brandColorHex" to "#1D4ED8",
                "activeAcademicYear" to "2026/2027",
                "activeTerm" to "Term 1",
                "enabledModules" to GsmsModule.defaultEnabledNames,
                "studentCount" to 1240L,
                "staffCount" to 92L,
                "monthlyRevenueUsd" to 4850.0,
                "churnRiskScore" to 8L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val school2Ref = db.collection("schools").document(secondSchoolId)
        rootBatch.set(
            school2Ref,
            mapOf(
                "ownerId" to uid,
                "name" to "Horizon STEM College",
                "code" to "HSC-KE",
                "country" to "Kenya",
                "city" to "Nairobi",
                "plan" to "ENTERPRISE",
                "subscriptionStatus" to "ACTIVE",
                "brandColorHex" to "#059669",
                "activeAcademicYear" to "2026/2027",
                "activeTerm" to "Term 1",
                "enabledModules" to GsmsModule.defaultEnabledNames,
                "studentCount" to 980L,
                "staffCount" to 68L,
                "monthlyRevenueUsd" to 3600.0,
                "churnRiskScore" to 14L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val school3Ref = db.collection("schools").document(thirdSchoolId)
        rootBatch.set(
            school3Ref,
            mapOf(
                "ownerId" to uid,
                "name" to "Pacific Crest Preparatory",
                "code" to "PCP-SG",
                "country" to "Singapore",
                "city" to "Singapore",
                "plan" to "PRO",
                "subscriptionStatus" to "AT_RISK",
                "brandColorHex" to "#7C3AED",
                "activeAcademicYear" to "2026/2027",
                "activeTerm" to "Term 1",
                "enabledModules" to GsmsModule.defaultEnabledNames.filterNot { it == GsmsModule.HOSTEL_BOARDING.name },
                "studentCount" to 540L,
                "staffCount" to 41L,
                "monthlyRevenueUsd" to 1950.0,
                "churnRiskScore" to 74L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        rootBatch.commit().await()

        // 2. Seed primary school subcollections cleanly
        seedSchoolSubcollections(db, uid, mainSchoolId, "Oakridge International Academy", "London")
        // Seed light records for secondary schools so switching tenants immediately shows live isolated data
        seedSecondarySchoolBasics(db, uid, secondSchoolId, "Horizon STEM College", "Nairobi")
        seedSecondarySchoolBasics(db, uid, thirdSchoolId, "Pacific Crest Preparatory", "Singapore")
    }

    private suspend fun seedSchoolSubcollections(
        db: FirebaseFirestore,
        uid: String,
        schoolId: String,
        schoolName: String,
        city: String
    ) {
        val batch = db.batch()
        val schoolRef = db.collection("schools").document(schoolId)

        // Branches
        listOf(
            Triple("branch_main", "Main Campus ($city Central)", true),
            Triple("branch_north", "North STEM Wing", false)
        ).forEachIndexed { idx, (id, name, isMain) ->
            batch.set(
                schoolRef.collection("branches").document(id),
                mapOf(
                    "schoolId" to schoolId,
                    "ownerId" to uid,
                    "name" to name,
                    "code" to if (idx == 0) "BR-01" else "BR-02",
                    "city" to city,
                    "mainCampus" to isMain,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Students
        val students = listOf(
            listOf("stu_1", "ADM-2026-101", "Maya Lin", "Grade 10A", "STEM", "Wei Lin", "+44 7700 900111", 97.5, 92.4, 0.0, "LOW", "Cleared"),
            listOf("stu_2", "ADM-2026-102", "Liam O'Connor", "Grade 10A", "STEM", "Claire O'Connor", "+44 7700 900222", 74.0, 61.5, 1450.0, "HIGH", "Counselor Review"),
            listOf("stu_3", "ADM-2026-103", "Amina Yusuf", "Grade 11B", "Humanities", "Tariq Yusuf", "+44 7700 900333", 94.0, 86.8, 320.0, "LOW", "Cleared"),
            listOf("stu_4", "ADM-2026-104", "Mateo Silva", "Grade 9C", "General", "Sofia Silva", "+44 7700 900444", 68.5, 54.0, 2100.0, "CRITICAL", "Medical Follow-Up"),
            listOf("stu_5", "ADM-2026-105", "Zara Mensah", "Grade 12A", "IB Science", "Kwame Mensah", "+44 7700 900555", 98.2, 95.1, 0.0, "LOW", "Cleared")
        )
        students.forEach { s ->
            batch.set(
                schoolRef.collection("students").document(s[0] as String),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "ownerId" to uid,
                    "admissionNo" to s[1],
                    "fullName" to s[2],
                    "className" to s[3],
                    "stream" to s[4],
                    "parentName" to s[5],
                    "parentContact" to s[6],
                    "attendanceRate" to s[7],
                    "averageScore" to s[8],
                    "feeBalanceUsd" to s[9],
                    "riskLevel" to s[10],
                    "welfareStatus" to s[11],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Staff
        val staffList = listOf(
            listOf("stf_1", "STF-01", "Dr. Samuel Kamau", "Head of Mathematics", "STEM", "Grade 10A & 12A Math", 18L, 3L, false, 24L),
            listOf("stf_2", "STF-02", "Elena Rostova", "Senior Physics Teacher", "STEM", "Grade 10A & 11B Physics", 16L, 5L, true, 22L),
            listOf("stf_3", "STF-03", "Priya Nair", "English & Literature Lead", "Humanities", "Grade 9C & 10A English", 21L, 0L, false, 20L),
            listOf("stf_4", "STF-04", "David Vance", "Principal / Headteacher", "Leadership", "Academic Governance", 20L, 1L, false, 30L),
            listOf("stf_5", "STF-05", "Nurse Grace Okello", "Lead School Nurse", "Welfare", "Campus Health Clinic", 19L, 2L, false, 35L)
        )
        staffList.forEach { st ->
            batch.set(
                schoolRef.collection("staff").document(st[0] as String),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "ownerId" to uid,
                    "staffNo" to st[1],
                    "fullName" to st[2],
                    "roleTitle" to st[3],
                    "department" to st[4],
                    "subjectsSummary" to st[5],
                    "leaveBalanceDays" to st[6],
                    "leaveUsedDays" to st[7],
                    "onLeave" to st[8],
                    "workloadHours" to st[9],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Academic records (timetables, exams, marks)
        val academics = listOf(
            listOf("acad_1", "TIMETABLE", "Advanced Calculus Period 1", "Grade 10A", "Mathematics", "Dr. Samuel Kamau", "Mon 08:30 - 09:30", 0.0, 100.0, "-"),
            listOf("acad_2", "TIMETABLE", "Quantum & Mechanics Lab", "Grade 10A", "Physics", "Elena Rostova", "Mon 10:00 - 11:30", 0.0, 100.0, "-"),
            listOf("acad_3", "EXAM", "Term 1 Mid-Term STEM Mock", "Grade 10A", "Mathematics", "Dr. Samuel Kamau", "2026-11-28 09:00", 88.5, 100.0, "A-"),
            listOf("acad_4", "MARK", "Maya Lin • Mathematics Mid-Term", "Grade 10A", "Mathematics", "Dr. Samuel Kamau", "Graded", 94.0, 100.0, "A"),
            listOf("acad_5", "MARK", "Liam O'Connor • Physics Lab", "Grade 10A", "Physics", "Elena Rostova", "Graded", 58.0, 100.0, "C-")
        )
        academics.forEach { ac ->
            batch.set(
                schoolRef.collection("academics").document(ac[0] as String),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "academicYear" to "2026/2027",
                    "term" to "Term 1",
                    "ownerId" to uid,
                    "recordType" to ac[1],
                    "title" to ac[2],
                    "className" to ac[3],
                    "subjectName" to ac[4],
                    "teacherName" to ac[5],
                    "scheduleSlot" to ac[6],
                    "score" to ac[7],
                    "maxScore" to ac[8],
                    "grade" to ac[9],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Attendance
        listOf(
            Triple("att_1", "Maya Lin", "PRESENT"),
            Triple("att_2", "Liam O'Connor", "ABSENT"),
            Triple("att_3", "Amina Yusuf", "PRESENT"),
            Triple("att_4", "Mateo Silva", "LATE")
        ).forEach { (id, name, status) ->
            batch.set(
                schoolRef.collection("attendance").document(id),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "academicYear" to "2026/2027",
                    "term" to "Term 1",
                    "ownerId" to uid,
                    "targetType" to "STUDENT",
                    "targetName" to name,
                    "className" to "Grade 10A",
                    "dateIso" to "2026-11-21",
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Report templates
        batch.set(
            schoolRef.collection("report_templates").document("tpl_senior"),
            mapOf(
                "schoolId" to schoolId,
                "ownerId" to uid,
                "name" to "Senior Secondary Term Report",
                "classLevel" to "Grade 10 - Grade 12",
                "sections" to listOf("Core Subjects", "Attendance & Conduct", "Teacher Narrative", "Principal Endorsement"),
                "gradingScale" to "A (90-100), B (80-89), C (70-79), D (60-69)",
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Report cards across workflow statuses
        batch.set(
            schoolRef.collection("report_cards").document("rc_maya"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "academicYear" to "2026/2027",
                "term" to "Term 1",
                "ownerId" to uid,
                "studentId" to "stu_1",
                "studentName" to "Maya Lin",
                "className" to "Grade 10A",
                "templateId" to "tpl_senior",
                "templateName" to "Senior Secondary Term Report",
                "subjectGradesSummary" to "Math: 94 (A), Physics: 91 (A), English: 92 (A)",
                "averageScore" to 92.3,
                "teacherRemarks" to "Outstanding analytical precision across STEM labs.",
                "aiSummary" to "Top 5% academic trajectory; ready for Olympiad enrichment.",
                "headteacherRemarks" to "Commended for Honor Roll distinction.",
                "status" to "PUBLISHED",
                "version" to 3L,
                "submittedBy" to "Dr. Samuel Kamau",
                "reviewedBy" to "Principal David Vance",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            schoolRef.collection("report_cards").document("rc_liam"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "academicYear" to "2026/2027",
                "term" to "Term 1",
                "ownerId" to uid,
                "studentId" to "stu_2",
                "studentName" to "Liam O'Connor",
                "className" to "Grade 10A",
                "templateId" to "tpl_senior",
                "templateName" to "Senior Secondary Term Report",
                "subjectGradesSummary" to "Math: 64 (D), Physics: 58 (F), English: 71 (C)",
                "averageScore" to 64.3,
                "teacherRemarks" to "Needs structured weekly tutoring in Mechanics.",
                "aiSummary" to "Attendance drop correlates with 14% Physics decline; recommend remedial plan.",
                "status" to "SUBMITTED",
                "version" to 1L,
                "submittedBy" to "Dr. Samuel Kamau",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Finance entries
        val finances = listOf(
            listOf("fin_1", "INVOICE", "INV-2026-101", "Term 1 Tuition - Maya Lin", "Wei Lin", "Tuition", 2400.0, 2400.0, "PAID"),
            listOf("fin_2", "INVOICE", "INV-2026-102", "Term 1 Tuition - Liam O'Connor", "Claire O'Connor", "Tuition", 2400.0, 950.0, "OVERDUE"),
            listOf("fin_3", "EXPENSE", "EXP-2026-044", "STEM Physics Lab Equipment", "OptiLab UK", "Laboratory", 3200.0, 3200.0, "PAID"),
            listOf("fin_4", "PAYROLL", "PAY-2026-NOV", "November Faculty Payroll Batch", "All Teaching Staff", "Payroll", 48500.0, 48500.0, "PAID")
        )
        finances.forEach { fn ->
            batch.set(
                schoolRef.collection("finance").document(fn[0] as String),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "academicYear" to "2026/2027",
                    "term" to "Term 1",
                    "ownerId" to uid,
                    "entryType" to fn[1],
                    "referenceNo" to fn[2],
                    "title" to fn[3],
                    "partyName" to fn[4],
                    "category" to fn[5],
                    "amountUsd" to fn[6],
                    "paidUsd" to fn[7],
                    "status" to fn[8],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Leave requests
        batch.set(
            schoolRef.collection("leave_requests").document("lv_1"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "staffId" to "stf_2",
                "staffName" to "Elena Rostova",
                "roleTitle" to "Senior Physics Teacher",
                "leaveType" to "STUDY",
                "startDate" to "2026-11-19",
                "endDate" to "2026-11-23",
                "daysCount" to 5L,
                "reason" to "STEM Curriculum Accreditation Workshop",
                "status" to "APPROVED",
                "reviewedBy" to "Principal David Vance",
                "aiConflictNote" to "Covered Grade 10A lab via Dr. Samuel Kamau.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            schoolRef.collection("leave_requests").document("lv_2"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "staffId" to "stf_3",
                "staffName" to "Priya Nair",
                "roleTitle" to "English & Literature Lead",
                "leaveType" to "ANNUAL",
                "startDate" to "2026-12-02",
                "endDate" to "2026-12-05",
                "daysCount" to 4L,
                "reason" to "Family wedding out of city",
                "status" to "PENDING",
                "aiConflictNote" to "2 Grade 10A English essay workshops overlap; 2 humanities substitutes available.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Communications
        batch.set(
            schoolRef.collection("communications").document("comm_1"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "channel" to "ANNOUNCEMENT",
                "audience" to "All Parents & Teachers",
                "title" to "Term 1 Mid-Term Exams & Parent Conferences",
                "body" to "Mid-term assessments commence Nov 28. Approved report cards will publish directly to Parent Portals.",
                "senderName" to "Principal David Vance",
                "senderRole" to "HEADTEACHER",
                "aiDrafted" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Operations (Library, Transport, Welfare, Discipline, Inventory, Hostel, Admissions, Events)
        val operations = listOf(
            listOf("op_lib1", "LIBRARY_BOOK", "Linear Algebra Done Right", "Sheldon Axler • STEM Shelf B2", "Library Catalog", "AVAILABLE", "12 Copies", "High demand for Grade 12 IB"),
            listOf("op_lib2", "LIBRARY_BORROW", "Quantum Physics Primer (Overdue)", "Borrower: Liam O'Connor", "Liam O'Connor", "OVERDUE", "4 Days Late", "Auto reminder queued"),
            listOf("op_trn1", "VEHICLE", "Bus GSMS-04 • North Route", "Driver: Samuel Osei • 42 Seats", "North Wing Express", "ON_ROUTE", "98% On-Time", "Zero maintenance alerts"),
            listOf("op_trn2", "ROUTE", "Westlands & Riverside Loop", "18 Stops • 42 Students Assigned", "Transport Control", "DELAY_RISK", "+7 min traffic", "AI recommends 07:10 departure shift"),
            listOf("op_wel1", "WELFARE", "Clinic Visit: Mateo Silva", "Seasonal asthma check & inhaler log", "Nurse Grace Okello", "FOLLOW_UP", "Priority Check", "Parent notified via SMS"),
            listOf("op_dis1", "DISCIPLINE", "Lab Safety Protocol Reminder", "Grade 10A Chemistry Bench 3", "Dr. Samuel Kamau", "RESOLVED", "Minor", "Safety briefing completed"),
            listOf("op_inv1", "ASSET", "Smart Interactive Boards (x24)", "North STEM Wing Classrooms", "IT & Facilities", "OPERATIONAL", "$38,400 Value", "Annual warranty active"),
            listOf("op_hos1", "HOSTEL", "Falcon Boarding House", "Occupancy: 118 / 120 Beds", "Matron Hannah Reid", "ACTIVE", "98% Full", "Weekend curfew audit clean"),
            listOf("op_adm1", "ADMISSION", "Inquiry: Sophia Chen (Grade 9)", "Transfer from Zurich Int'l", "Admissions Desk", "INTERVIEW", "91% Likelihood", "Transcripts & passport verified"),
            listOf("op_evt1", "EVENT", "Annual Global STEM Fair 2026", "Dec 12 • Main Glass Atrium", "Events Committee", "SCHEDULED", "420 RSVPs", "Live parent stream enabled")
        )
        operations.forEach { op ->
            batch.set(
                schoolRef.collection("operations").document(op[0]),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "ownerId" to uid,
                    "moduleType" to op[1],
                    "title" to op[2],
                    "subtitle" to op[3],
                    "partyName" to op[4],
                    "status" to op[5],
                    "metricLabel" to op[6],
                    "notes" to op[7],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Early Warning AI Alerts
        val alerts = listOf(
            listOf("alt_1", "ACADEMIC", "HIGH", "Liam O'Connor", "Physics score dropped 14% alongside 74% attendance.", "Assign peer math/physics tutor & notify parent Claire O'Connor.", "HEADTEACHER", "OPEN"),
            listOf("alt_2", "FINANCE", "CRITICAL", "Invoice INV-2026-102", "Balance $1,450 overdue by 21 days.", "Send staggered installment reminder via Finance Portal.", "FINANCE_OFFICER", "OPEN"),
            listOf("alt_3", "STAFFING", "MEDIUM", "Humanities Department", "Pending leave request lv_2 overlaps 2 Grade 10A workshops.", "Assign substitute teacher before approving leave.", "HR_MANAGER", "IN_PROGRESS")
        )
        alerts.forEach { al ->
            batch.set(
                schoolRef.collection("alerts").document(al[0]),
                mapOf(
                    "schoolId" to schoolId,
                    "branchId" to "branch_main",
                    "ownerId" to uid,
                    "category" to al[1],
                    "riskLevel" to al[2],
                    "targetName" to al[3],
                    "reason" to al[4],
                    "recommendedAction" to al[5],
                    "assignedRole" to al[6],
                    "status" to al[7],
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Audit Log
        batch.set(
            schoolRef.collection("audit_logs").document("log_init"),
            mapOf(
                "schoolId" to schoolId,
                "ownerId" to uid,
                "actorName" to "GSMS Provisioning Engine",
                "actorRole" to "SUPER_ADMIN",
                "actionType" to "TENANT_INITIALIZED",
                "moduleName" to "SYSTEM",
                "summary" to "Provisioned multi-tenant workspace for $schoolName with 19 modules.",
                "aiAction" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
    }

    private suspend fun seedSecondarySchoolBasics(
        db: FirebaseFirestore,
        uid: String,
        schoolId: String,
        schoolName: String,
        city: String
    ) {
        val batch = db.batch()
        val schoolRef = db.collection("schools").document(schoolId)

        batch.set(
            schoolRef.collection("branches").document("branch_main"),
            mapOf(
                "schoolId" to schoolId,
                "ownerId" to uid,
                "name" to "$schoolName Main Campus",
                "code" to "BR-01",
                "city" to city,
                "mainCampus" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            schoolRef.collection("students").document("stu_s1"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "admissionNo" to "ADM-2026-801",
                "fullName" to if (city == "Nairobi") "Brian Kiprop" else "Ethan Tan",
                "className" to "Grade 11A",
                "stream" to "Science",
                "parentName" to if (city == "Nairobi") "Grace Kiprop" else "Mei Tan",
                "attendanceRate" to 93.4,
                "averageScore" to 84.5,
                "feeBalanceUsd" to 250.0,
                "riskLevel" to "LOW",
                "welfareStatus" to "Cleared",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            schoolRef.collection("staff").document("stf_s1"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "staffNo" to "STF-11",
                "fullName" to if (city == "Nairobi") "Prof. Wanjiku Njoroge" else "Dr. Evelyn Lim",
                "roleTitle" to "Campus Principal",
                "department" to "Leadership",
                "subjectsSummary" to "Academic Oversight",
                "leaveBalanceDays" to 21L,
                "leaveUsedDays" to 0L,
                "onLeave" to false,
                "workloadHours" to 28L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            schoolRef.collection("alerts").document("alt_s1"),
            mapOf(
                "schoolId" to schoolId,
                "branchId" to "branch_main",
                "ownerId" to uid,
                "category" to "FINANCE",
                "riskLevel" to if (city == "Singapore") "HIGH" else "MEDIUM",
                "targetName" to schoolName,
                "reason" to if (city == "Singapore") "Module engagement dropped 18% this month (churn watch)." else "Term 1 lab fee collection at 79%.",
                "recommendedAction" to "Schedule executive onboarding check-in & finance digest.",
                "assignedRole" to "SUPER_ADMIN",
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }
}
