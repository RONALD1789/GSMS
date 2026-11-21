package com.example.data.supabase

import com.example.model.*
import com.example.ui.GsmsWorkspaceState

object SupabaseSeedData {

    fun buildInitialWorkspaceState(
        uid: String,
        displayName: String,
        email: String
    ): GsmsWorkspaceState {
        val shortUid = uid.replace("-", "").take(6).ifBlank { "gsms01" }
        val mainSchoolId = "school_oakridge_$shortUid"
        val secondSchoolId = "school_horizon_$shortUid"

        val mainSchool = School(
            id = mainSchoolId,
            ownerId = uid,
            name = "Oakridge International Academy",
            code = "OIA-UK",
            country = "United Kingdom",
            city = "London",
            plan = "ENTERPRISE",
            subscriptionStatus = "ACTIVE",
            brandColorHex = "#1D4ED8",
            activeAcademicYear = "2026/2027",
            activeTerm = "Term 1",
            enabledModules = GsmsModule.defaultEnabledNames,
            studentCount = 1240L,
            staffCount = 92L,
            monthlyRevenueUsd = 4850.0,
            churnRiskScore = 8L
        )
        val secondSchool = School(
            id = secondSchoolId,
            ownerId = uid,
            name = "Horizon STEM College",
            code = "HSC-KE",
            country = "Kenya",
            city = "Nairobi",
            plan = "ENTERPRISE",
            subscriptionStatus = "ACTIVE",
            brandColorHex = "#059669",
            activeAcademicYear = "2026/2027",
            activeTerm = "Term 1",
            enabledModules = GsmsModule.defaultEnabledNames,
            studentCount = 980L,
            staffCount = 68L,
            monthlyRevenueUsd = 3600.0,
            churnRiskScore = 14L
        )

        val branches = listOf(
            Branch(id = "branch_main", schoolId = mainSchoolId, ownerId = uid, name = "Main Campus", code = "BR-01", city = "London", mainCampus = true),
            Branch(id = "branch_north", schoolId = mainSchoolId, ownerId = uid, name = "North STEM Annex", code = "BR-02", city = "London", mainCampus = false)
        )

        val assignments = listOf(
            UserAccessAssignment(
                id = "assign_teacher_kamau_$shortUid",
                schoolId = mainSchoolId,
                ownerId = uid,
                fullName = "Dr. Samuel Kamau",
                email = "kamau@oakridge.edu",
                primaryRole = PortalRole.TEACHER.name,
                assignedBranchIds = listOf("branch_main"),
                assignedClasses = listOf("Grade 10A"),
                assignedSubjects = listOf("Mathematics", "Physics"),
                updatedBy = "GSMS Seed"
            ),
            UserAccessAssignment(
                id = "assign_parent_wei_$shortUid",
                schoolId = mainSchoolId,
                ownerId = uid,
                fullName = "Wei Lin (Guardian)",
                email = "wei.lin@family.org",
                primaryRole = PortalRole.PARENT.name,
                assignedBranchIds = listOf("branch_main"),
                linkedStudentIds = listOf("stu_1_$shortUid"),
                updatedBy = "GSMS Seed"
            ),
            UserAccessAssignment(
                id = "assign_student_maya_$shortUid",
                schoolId = mainSchoolId,
                ownerId = uid,
                fullName = "Maya Lin (Learner)",
                email = "maya.lin@oakridge.edu",
                primaryRole = PortalRole.STUDENT.name,
                assignedBranchIds = listOf("branch_main"),
                assignedClasses = listOf("Grade 10A"),
                linkedStudentIds = listOf("stu_1_$shortUid"),
                updatedBy = "GSMS Seed"
            )
        )

        val students = listOf(
            Student(
                id = "stu_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                admissionNo = "OIA-1042", fullName = "Maya Lin", className = "Grade 10A", stream = "STEM",
                parentName = "Wei Lin", parentContact = "+44 7700 900112", attendanceRate = 97.5, averageScore = 91.2,
                feeBalanceUsd = 0.0, riskLevel = RiskLevel.LOW.name, welfareStatus = "Cleared"
            ),
            Student(
                id = "stu_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                admissionNo = "OIA-1089", fullName = "Daniel Okafor", className = "Grade 10A", stream = "STEM",
                parentName = "Ngozi Okafor", parentContact = "+44 7700 900348", attendanceRate = 76.0, averageScore = 58.4,
                feeBalanceUsd = 640.0, riskLevel = RiskLevel.HIGH.name, welfareStatus = "Follow-Up"
            ),
            Student(
                id = "stu_3_$shortUid", schoolId = mainSchoolId, branchId = "branch_north", ownerId = uid,
                admissionNo = "OIA-1114", fullName = "Sofia Morales", className = "Grade 11B", stream = "Humanities",
                parentName = "Carlos Morales", parentContact = "+44 7700 900590", attendanceRate = 94.0, averageScore = 84.5,
                feeBalanceUsd = 150.0, riskLevel = RiskLevel.LOW.name, welfareStatus = "Cleared"
            )
        )

        val staff = listOf(
            StaffMember(
                id = "stf_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                staffNo = "STF-201", fullName = "Dr. Samuel Kamau", roleTitle = "Senior Mathematics Teacher",
                department = "STEM", subjectsSummary = "Mathematics, Physics", leaveBalanceDays = 18L, leaveUsedDays = 3L,
                onLeave = false, workloadHours = 24L
            ),
            StaffMember(
                id = "stf_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                staffNo = "STF-208", fullName = "Priya Nair", roleTitle = "Homeroom Class Teacher",
                department = "Languages", subjectsSummary = "English Literature", leaveBalanceDays = 20L, leaveUsedDays = 1L,
                onLeave = false, workloadHours = 22L
            )
        )

        val academics = listOf(
            AcademicRecord(
                id = "acad_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                recordType = "TIMETABLE", title = "AP Calculus & Mechanics", className = "Grade 10A",
                subjectName = "Mathematics", teacherName = "Dr. Samuel Kamau", scheduleSlot = "Mon / Wed 08:30",
                score = 90.0, maxScore = 100.0, grade = "A"
            ),
            AcademicRecord(
                id = "acad_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                recordType = "EXAM", title = "Term 1 Midterm Assessment", className = "Grade 10A",
                subjectName = "Physics", teacherName = "Dr. Samuel Kamau", scheduleSlot = "Week 6",
                score = 84.0, maxScore = 100.0, grade = "B+"
            )
        )

        val attendance = listOf(
            AttendanceRecord(
                id = "att_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                targetType = "STUDENT", targetName = "Maya Lin", className = "Grade 10A",
                dateIso = "2026-11-21", status = "PRESENT"
            ),
            AttendanceRecord(
                id = "att_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                targetType = "STUDENT", targetName = "Daniel Okafor", className = "Grade 10A",
                dateIso = "2026-11-21", status = "ABSENT"
            )
        )

        val templates = listOf(
            ReportTemplate(
                id = "tpl_1_$shortUid", schoolId = mainSchoolId, ownerId = uid,
                name = "Term 1 Holistic Report Template", classLevel = "Grade 10-12",
                sections = listOf("Core Subjects", "Attendance", "Conduct & Effort", "Headteacher Sign-Off"),
                gradingScale = "A-F Percentage", active = true
            )
        )

        val reports = listOf(
            ReportCard(
                id = "rep_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                studentId = "stu_1_$shortUid", studentName = "Maya Lin", className = "Grade 10A",
                templateId = "tpl_1_$shortUid", templateName = "Term 1 Holistic Report Template",
                subjectGradesSummary = "Mathematics: 94% (A) • Physics: 91% (A) • English: 88% (B+)",
                averageScore = 91.2, teacherRemarks = "Consistent analytical excellence across STEM labs.",
                status = ReportStatus.PUBLISHED.name, version = 2L,
                submittedBy = "Dr. Samuel Kamau", reviewedBy = "Principal"
            ),
            ReportCard(
                id = "rep_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                studentId = "stu_2_$shortUid", studentName = "Daniel Okafor", className = "Grade 10A",
                templateId = "tpl_1_$shortUid", templateName = "Term 1 Holistic Report Template",
                subjectGradesSummary = "Mathematics: 56% (D) • Physics: 61% (C-) • English: 58% (D+)",
                averageScore = 58.4, teacherRemarks = "Needs afternoon Mathematics mentoring & attendance follow-up.",
                status = ReportStatus.SUBMITTED.name, version = 1L,
                submittedBy = "Dr. Samuel Kamau"
            )
        )

        val finance = listOf(
            FinanceEntry(
                id = "fin_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                entryType = "INVOICE", referenceNo = "INV-2026-041", title = "Term 1 Tuition & Lab Fee",
                partyName = "Wei Lin", category = "Tuition", amountUsd = 2400.0, paidUsd = 2400.0, status = "PAID"
            ),
            FinanceEntry(
                id = "fin_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                entryType = "INVOICE", referenceNo = "INV-2026-078", title = "Term 1 Boarding & Tuition",
                partyName = "Ngozi Okafor", category = "Tuition", amountUsd = 2400.0, paidUsd = 1760.0, status = "PARTIAL"
            )
        )

        val leaves = listOf(
            LeaveRequest(
                id = "lv_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                staffId = "stf_2_$shortUid", staffName = "Priya Nair", roleTitle = "Homeroom Class Teacher",
                leaveType = "ANNUAL", startDate = "2026-12-02", endDate = "2026-12-04", daysCount = 3L,
                reason = "Family graduation ceremony travel", status = LeaveStatus.PENDING.name
            )
        )

        val comms = listOf(
            CommunicationItem(
                id = "comm_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                channel = "ANNOUNCEMENT", audience = "All Parents & Staff",
                title = "Term 1 Science & Innovation Showcase",
                body = "Parents are invited to the Main Campus STEM Showcase this Friday at 14:00.",
                senderName = displayName, senderRole = PortalRole.SCHOOL_ADMIN.name, aiDrafted = false
            )
        )

        val ops = listOf(
            OperationRecord(
                id = "op_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                moduleType = "LIBRARY_BOOK", title = "Advanced Calculus 5th Ed.",
                subtitle = "STEM Reserve Shelf • 14 copies", partyName = "Library Catalog",
                status = "AVAILABLE", metricLabel = "14/16 On Shelf"
            ),
            OperationRecord(
                id = "op_2_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                moduleType = "ROUTE", title = "North Express Route B",
                subtitle = "Driver: K. Mensah • Bus #04", partyName = "38 Learners Assigned",
                status = "ON_TIME", metricLabel = "07:15 Departure"
            )
        )

        val alerts = listOf(
            EarlyWarningAlert(
                id = "al_1_$shortUid", schoolId = mainSchoolId, branchId = "branch_main", ownerId = uid,
                category = "ACADEMIC", riskLevel = RiskLevel.HIGH.name, targetName = "Daniel Okafor",
                reason = "Attendance dipped to 76% alongside 58.4% midterm average.",
                recommendedAction = "Schedule parent conference and assign STEM peer tutoring.",
                assignedRole = PortalRole.CLASS_TEACHER.name, status = "OPEN"
            )
        )

        val auditLogs = listOf(
            AuditLog(
                id = "aud_1_$shortUid", schoolId = mainSchoolId, ownerId = uid,
                actorName = displayName, actorRole = PortalRole.SUPER_ADMIN.name,
                actionType = "WORKSPACE_INITIALIZED", moduleName = "SYSTEM",
                summary = "Provisioned Supabase PostgreSQL multi-tenant workspace for $email", aiAction = false
            )
        )

        val profile = UserProfile(
            id = uid,
            userId = uid,
            displayName = displayName,
            email = email,
            primaryRole = PortalRole.SUPER_ADMIN.name,
            activeSchoolId = mainSchoolId,
            activeBranchId = "branch_main",
            activeRole = PortalRole.SCHOOL_ADMIN.name,
            assignedSchoolId = mainSchoolId
        )

        return GsmsWorkspaceState(
            userProfile = profile,
            schools = listOf(mainSchool, secondSchool),
            activeSchool = mainSchool,
            branches = branches,
            activeBranchId = "branch_main",
            activeRole = PortalRole.SCHOOL_ADMIN,
            accessAssignments = assignments,
            students = students,
            staff = staff,
            academics = academics,
            attendance = attendance,
            reportTemplates = templates,
            reportCards = reports,
            finance = finance,
            leaveRequests = leaves,
            communications = comms,
            operations = ops,
            alerts = alerts,
            auditLogs = auditLogs
        )
    }
}
