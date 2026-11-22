package com.example.data.supabase

import com.example.model.*
import com.example.ui.GsmsWorkspaceState
import com.example.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class SupabaseWorkspaceController(
    private val supabase: SupabaseSyncRepository,
    private val userId: String,
    private val displayName: String,
    private val userEmail: String?
) {
    private val _uiState = MutableStateFlow<UiState<GsmsWorkspaceState>>(UiState.Loading)
    val uiState: StateFlow<UiState<GsmsWorkspaceState>> = _uiState.asStateFlow()

    private fun currentWorkspace(): GsmsWorkspaceState? = (_uiState.value as? UiState.Success)?.data

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"

    suspend fun boot(): Result<Unit> {
        _uiState.value = UiState.Loading
        val session = SupabaseSession(
            userId = userId,
            email = userEmail.orEmpty(),
            displayName = displayName
        )
        return supabase.loadOrSeedWorkspaceForSession(session)
            .onSuccess { ws -> _uiState.value = UiState.Success(ws) }
            .onFailure { err ->
                _uiState.value = UiState.Error(err.localizedMessage ?: "Failed to load email workspace from Supabase.")
            }
            .map { }
    }

    suspend fun switchRole(role: PortalRole): Result<String> {
        val ws = currentWorkspace() ?: return Result.failure(IllegalStateException("Workspace not loaded"))
        val updated = ws.copy(
            activeRole = role,
            userProfile = ws.userProfile?.copy(activeRole = role.name)
        )
        _uiState.value = UiState.Success(updated)
        return Result.success("Switched to ${role.displayName}")
    }

    suspend fun switchSchool(school: School): Result<String> {
        val ws = currentWorkspace() ?: return Result.failure(IllegalStateException("Workspace not loaded"))
        val session = SupabaseSession(userId, userEmail.orEmpty(), displayName)
        return supabase.loadSchoolWorkspace(
            session = session,
            schools = ws.schools,
            targetSchoolId = school.id,
            activeBranchId = "branch_main",
            roleOverride = ws.activeRole
        ).onSuccess { loaded ->
            _uiState.value = UiState.Success(loaded)
        }.map { "Active tenant: ${school.name}" }
    }

    suspend fun switchBranch(branch: Branch): Result<String> {
        val ws = currentWorkspace() ?: return Result.failure(IllegalStateException("Workspace not loaded"))
        _uiState.value = UiState.Success(
            ws.copy(
                activeBranchId = branch.id,
                userProfile = ws.userProfile?.copy(activeBranchId = branch.id)
            )
        )
        return Result.success("Active branch: ${branch.name}")
    }

    fun toggleLowDataMode(enabled: Boolean): Result<String> {
        val ws = currentWorkspace() ?: return Result.failure(IllegalStateException("Workspace not loaded"))
        _uiState.value = UiState.Success(
            ws.copy(userProfile = ws.userProfile?.copy(lowDataMode = enabled))
        )
        return Result.success(if (enabled) "Low-Data Mode enabled" else "Standard Mode enabled")
    }

    suspend fun toggleModule(
        school: School,
        module: GsmsModule,
        actorName: String,
        role: PortalRole
    ): Result<String> {
        val updatedModules = if (school.enabledModules.contains(module.name)) {
            school.enabledModules.filterNot { it == module.name }
        } else {
            (school.enabledModules + module.name).take(30)
        }
        val updatedSchool = school.copy(enabledModules = updatedModules)
        supabase.saveSchool(updatedSchool, userId).getOrThrow()
        val audit = AuditLog(
            id = newId("aud"), schoolId = school.id, ownerId = userId,
            actorName = actorName, actorRole = role.name, actionType = "MODULE_TOGGLED",
            moduleName = module.name, summary = "${if (updatedModules.contains(module.name)) "Enabled" else "Disabled"} ${module.label}"
        )
        supabase.appendAuditLog(audit, userId, school.id)
        mutateSuccess { ws ->
            ws.copy(
                activeSchool = updatedSchool,
                schools = ws.schools.map { if (it.id == school.id) updatedSchool else it },
                auditLogs = listOf(audit) + ws.auditLogs
            )
        }
        return Result.success("Updated module: ${module.label}")
    }

    suspend fun createSchool(
        name: String,
        code: String,
        city: String,
        country: String,
        actorName: String,
        role: PortalRole
    ): Result<String> {
        val schoolId = newId("school")
        val school = School(
            id = schoolId, ownerId = userId, name = name.trim(), code = code.trim().uppercase(),
            country = country.trim().ifBlank { "Global" }, city = city.trim().ifBlank { "Main City" },
            monthlyRevenueUsd = 2500.0, churnRiskScore = 5L
        )
        val mainBranch = Branch(
            id = newId("branch"), schoolId = schoolId, ownerId = userId,
            name = "Main Campus", code = "BR-01", city = school.city, mainCampus = true
        )
        supabase.saveSchool(school, userId).getOrThrow()
        supabase.saveBranch(mainBranch, userId, schoolId).getOrThrow()
        val ws = currentWorkspace()
        if (ws != null) {
            val nextSchools = ws.schools + school
            _uiState.value = UiState.Success(
                ws.copy(
                    schools = nextSchools,
                    activeSchool = school,
                    branches = listOf(mainBranch),
                    activeBranchId = mainBranch.id,
                    students = emptyList(),
                    staff = emptyList(),
                    academics = emptyList(),
                    attendance = emptyList(),
                    reportTemplates = emptyList(),
                    reportCards = emptyList(),
                    finance = emptyList(),
                    leaveRequests = emptyList(),
                    communications = emptyList(),
                    operations = emptyList(),
                    alerts = emptyList(),
                    auditLogs = emptyList()
                )
            )
        }
        return Result.success("Provisioned new school tenant: $name")
    }

    suspend fun createBranch(
        schoolId: String,
        name: String,
        code: String,
        city: String,
        actorName: String,
        role: PortalRole
    ): Result<String> {
        val branch = Branch(
            id = newId("branch"), schoolId = schoolId, ownerId = userId,
            name = name.trim(), code = code.trim().uppercase(),
            city = city.trim().ifBlank { "Campus City" }, mainCampus = false
        )
        supabase.saveBranch(branch, userId, schoolId).getOrThrow()
        mutateSuccess { ws -> ws.copy(branches = ws.branches + branch) }
        return Result.success("Added branch: $name")
    }

    suspend fun updatePeriod(school: School, year: String, term: String): Result<String> {
        val updated = school.copy(activeAcademicYear = year.trim(), activeTerm = term.trim())
        supabase.saveSchool(updated, userId).getOrThrow()
        mutateSuccess { ws ->
            ws.copy(
                activeSchool = updated,
                schools = ws.schools.map { if (it.id == school.id) updated else it }
            )
        }
        return Result.success("Active period updated to $year ($term)")
    }

    suspend fun delegatePermissions(
        schoolId: String,
        assignment: UserAccessAssignment,
        extraPermissions: List<String>,
        assignedClasses: List<String>,
        assignedSubjects: List<String>,
        linkedStudentIds: List<String>,
        actorName: String
    ): Result<String> {
        val updated = assignment.copy(
            extraPermissions = extraPermissions.distinct().take(45),
            assignedClasses = assignedClasses.distinct().take(30),
            assignedSubjects = assignedSubjects.distinct().take(30),
            linkedStudentIds = linkedStudentIds.distinct().take(20),
            updatedBy = actorName.take(100)
        )
        supabase.saveAccessAssignment(updated, userId, schoolId).getOrThrow()
        mutateSuccess { ws ->
            ws.copy(accessAssignments = ws.accessAssignments.map { if (it.id == updated.id) updated else it })
        }
        return Result.success("Updated permissions (${extraPermissions.size} extra) for ${assignment.fullName}")
    }

    suspend fun createAccessAssignment(
        schoolId: String,
        fullName: String,
        email: String,
        primaryRole: PortalRole,
        assignedClasses: List<String>,
        assignedSubjects: List<String>,
        linkedStudentIds: List<String>,
        extraPermissions: List<String>,
        actorName: String
    ): Result<String> {
        if (primaryRole == PortalRole.SUPER_ADMIN) {
            return Result.failure(IllegalArgumentException("SUPER_ADMIN is reserved for the verified school owner."))
        }
        val activeWorkspace = currentWorkspace()?.takeIf { it.activeSchool?.id == schoolId }
        val mainBranchId = activeWorkspace?.branches?.firstOrNull { it.mainCampus }?.id ?: "branch_main"
        val selectedBranchId = activeWorkspace?.activeBranchId
            ?.takeIf { selected -> activeWorkspace.branches.any { it.id == selected } }
            ?: mainBranchId
        val linkedStudentBranches = if (primaryRole == PortalRole.PARENT || primaryRole == PortalRole.STUDENT) {
            activeWorkspace?.students
                ?.filter { it.id in linkedStudentIds }
                ?.map { it.branchId }
                .orEmpty()
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        val assignedBranchIds = linkedStudentBranches.distinct().ifEmpty { listOf(selectedBranchId) }
        val item = UserAccessAssignment(
            id = newId("assign"), schoolId = schoolId, ownerId = userId,
            fullName = fullName.trim(), email = email.trim(), primaryRole = primaryRole.name,
            assignedBranchIds = assignedBranchIds, assignedClasses = assignedClasses,
            assignedSubjects = assignedSubjects, linkedStudentIds = linkedStudentIds,
            extraPermissions = extraPermissions, active = true, updatedBy = actorName
        )
        supabase.saveAccessAssignment(item, userId, schoolId).getOrThrow()
        mutateSuccess { ws -> ws.copy(accessAssignments = ws.accessAssignments + item) }
        return Result.success("Provisioned ${primaryRole.shortTitle} portal access for $fullName")
    }

    suspend fun addStudent(
        school: School,
        branchId: String,
        admissionNo: String,
        fullName: String,
        className: String,
        parentName: String
    ): Result<String> {
        val student = Student(
            id = newId("stu"), schoolId = school.id, branchId = branchId, ownerId = userId,
            admissionNo = admissionNo.trim(), fullName = fullName.trim(),
            className = className.trim().ifBlank { "Grade 10A" }, stream = "General",
            parentName = parentName.trim().ifBlank { "Guardian" },
            attendanceRate = 100.0, averageScore = 85.0, feeBalanceUsd = 1200.0
        )
        supabase.saveStudent(student, userId, school.id).getOrThrow()
        val updatedSchool = school.copy(studentCount = school.studentCount + 1L)
        supabase.saveSchool(updatedSchool, userId)
        mutateSuccess { ws ->
            ws.copy(
                activeSchool = updatedSchool,
                schools = ws.schools.map { if (it.id == school.id) updatedSchool else it },
                students = ws.students + student
            )
        }
        return Result.success("Enrolled $fullName")
    }

    suspend fun recordAttendance(
        school: School,
        branchId: String,
        student: Student,
        status: String
    ): Result<String> {
        val record = AttendanceRecord(
            id = newId("att"), schoolId = school.id, branchId = branchId,
            academicYear = school.activeAcademicYear, term = school.activeTerm,
            ownerId = userId, targetType = "STUDENT", targetName = student.fullName,
            className = student.className, dateIso = "2026-11-21", status = status
        )
        supabase.saveAttendance(record, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(attendance = listOf(record) + ws.attendance) }
        return Result.success("${student.fullName} marked $status")
    }

    suspend fun addAcademicRecord(
        school: School,
        branchId: String,
        recordType: String,
        title: String,
        className: String,
        subjectName: String,
        teacherName: String,
        scheduleSlot: String,
        score: Double
    ): Result<String> {
        val grade = when {
            score >= 90 -> "A"
            score >= 80 -> "B"
            score >= 70 -> "C"
            score >= 60 -> "D"
            else -> "F"
        }
        val rec = AcademicRecord(
            id = newId("acad"), schoolId = school.id, branchId = branchId,
            academicYear = school.activeAcademicYear, term = school.activeTerm,
            ownerId = userId, recordType = recordType, title = title.trim(),
            className = className.trim().ifBlank { "Grade 10A" },
            subjectName = subjectName.trim().ifBlank { "General" },
            teacherName = teacherName.trim().ifBlank { "Faculty" },
            scheduleSlot = scheduleSlot.trim().ifBlank { "Term 1" },
            score = score.coerceIn(0.0, 1000.0), maxScore = 100.0, grade = grade
        )
        supabase.saveAcademic(rec, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(academics = listOf(rec) + ws.academics) }
        return Result.success("Saved $recordType: $title")
    }

    suspend fun createReportTemplate(
        school: School,
        name: String,
        classLevel: String,
        sections: List<String>,
        gradingScale: String
    ): Result<String> {
        val tpl = ReportTemplate(
            id = newId("tpl"), schoolId = school.id, ownerId = userId,
            name = name.trim(), classLevel = classLevel.trim().ifBlank { "All Classes" },
            sections = sections, gradingScale = gradingScale.trim().ifBlank { "A-F Percentage" },
            active = true
        )
        supabase.saveTemplate(tpl, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(reportTemplates = ws.reportTemplates + tpl) }
        return Result.success("Created report template: $name")
    }

    suspend fun saveOrSubmitReportCard(
        school: School,
        branchId: String,
        student: Student,
        template: ReportTemplate,
        gradesSummary: String,
        averageScore: Double,
        teacherRemarks: String,
        aiSummary: String?,
        submitNow: Boolean,
        actorName: String
    ): Result<String> {
        val report = ReportCard(
            id = newId("rep"), schoolId = school.id, branchId = branchId,
            academicYear = school.activeAcademicYear, term = school.activeTerm,
            ownerId = userId, studentId = student.id, studentName = student.fullName,
            className = student.className, templateId = template.id, templateName = template.name,
            subjectGradesSummary = gradesSummary.trim().take(1200),
            averageScore = averageScore.coerceIn(0.0, 100.0),
            teacherRemarks = teacherRemarks.trim().take(800),
            aiSummary = aiSummary?.trim()?.take(1200),
            status = if (submitNow) ReportStatus.SUBMITTED.name else ReportStatus.DRAFT.name,
            version = 1L, submittedBy = actorName.take(100)
        )
        supabase.saveReportCard(report, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(reportCards = listOf(report) + ws.reportCards) }
        return Result.success(if (submitNow) "Report submitted for Headteacher review" else "Draft saved")
    }

    suspend fun reviewReportCard(
        schoolId: String,
        report: ReportCard,
        nextStatus: ReportStatus,
        headteacherRemarks: String,
        actorName: String
    ): Result<String> {
        val updated = report.copy(
            status = nextStatus.name,
            version = report.version + 1L,
            reviewedBy = actorName.take(100),
            headteacherRemarks = headteacherRemarks.trim().ifBlank { report.headteacherRemarks }
        )
        supabase.saveReportCard(updated, userId, schoolId).getOrThrow()
        mutateSuccess { ws ->
            ws.copy(reportCards = ws.reportCards.map { if (it.id == updated.id) updated else it })
        }
        return Result.success("Report card marked ${nextStatus.label}")
    }

    suspend fun submitLeaveRequest(
        school: School,
        branchId: String,
        staff: StaffMember,
        leaveType: String,
        startDate: String,
        endDate: String,
        daysCount: Long,
        reason: String,
        aiConflictNote: String?
    ): Result<String> {
        val leave = LeaveRequest(
            id = newId("lv"), schoolId = school.id, branchId = branchId, ownerId = userId,
            staffId = staff.id, staffName = staff.fullName, roleTitle = staff.roleTitle,
            leaveType = leaveType, startDate = startDate.trim(), endDate = endDate.trim(),
            daysCount = daysCount.coerceIn(1L, 365L), reason = reason.trim(),
            status = LeaveStatus.PENDING.name, aiConflictNote = aiConflictNote?.trim()
        )
        supabase.saveLeaveRequest(leave, null, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(leaveRequests = listOf(leave) + ws.leaveRequests) }
        return Result.success("Leave request submitted for ${staff.fullName}")
    }

    suspend fun reviewLeaveRequest(
        schoolId: String,
        leave: LeaveRequest,
        staffMember: StaffMember?,
        approve: Boolean,
        actorName: String
    ): Result<String> {
        val nextStatus = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED
        val updatedLeave = leave.copy(status = nextStatus.name, reviewedBy = actorName.take(100))
        val updatedStaff = if (approve && staffMember != null) {
            staffMember.copy(
                onLeave = true,
                leaveBalanceDays = (staffMember.leaveBalanceDays - leave.daysCount).coerceAtLeast(0L),
                leaveUsedDays = (staffMember.leaveUsedDays + leave.daysCount).coerceAtMost(365L)
            )
        } else null
        supabase.saveLeaveRequest(updatedLeave, updatedStaff, userId, schoolId).getOrThrow()
        mutateSuccess { ws ->
            ws.copy(
                leaveRequests = ws.leaveRequests.map { if (it.id == updatedLeave.id) updatedLeave else it },
                staff = if (updatedStaff != null) ws.staff.map { if (it.id == updatedStaff.id) updatedStaff else it } else ws.staff
            )
        }
        return Result.success(
            if (approve) "Approved leave: ${leave.staffName} is now marked On Leave"
            else "Rejected leave request for ${leave.staffName}"
        )
    }

    suspend fun recordFinancePayment(schoolId: String, entry: FinanceEntry, amountUsd: Double): Result<String> {
        val newPaid = (entry.paidUsd + amountUsd).coerceAtMost(entry.amountUsd)
        val nextStatus = if (newPaid >= entry.amountUsd) "PAID" else "PARTIAL"
        val updated = entry.copy(paidUsd = newPaid, status = nextStatus)
        supabase.saveFinanceEntry(updated, userId, schoolId).getOrThrow()
        mutateSuccess { ws -> ws.copy(finance = ws.finance.map { if (it.id == updated.id) updated else it }) }
        return Result.success("Payment recorded for ${entry.referenceNo}")
    }

    suspend fun sendCommunication(
        school: School,
        branchId: String,
        channel: String,
        audience: String,
        title: String,
        body: String,
        actorName: String,
        senderRole: PortalRole,
        aiDrafted: Boolean
    ): Result<String> {
        val item = CommunicationItem(
            id = newId("comm"), schoolId = school.id, branchId = branchId, ownerId = userId,
            channel = channel, audience = audience, title = title.trim(), body = body.trim(),
            senderName = actorName, senderRole = senderRole.name, aiDrafted = aiDrafted
        )
        supabase.saveCommunication(item, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(communications = listOf(item) + ws.communications) }
        return Result.success("Dispatched $channel message")
    }

    suspend fun addOperationRecord(
        school: School,
        branchId: String,
        moduleType: String,
        title: String,
        subtitle: String,
        partyName: String,
        status: String,
        metricLabel: String,
        notes: String
    ): Result<String> {
        val op = OperationRecord(
            id = newId("op"), schoolId = school.id, branchId = branchId, ownerId = userId,
            moduleType = moduleType, title = title.trim(),
            subtitle = subtitle.trim().ifBlank { null },
            partyName = partyName.trim().ifBlank { null },
            status = status, metricLabel = metricLabel.trim().ifBlank { null },
            notes = notes.trim().ifBlank { null }
        )
        supabase.saveOperation(op, userId, school.id).getOrThrow()
        mutateSuccess { ws -> ws.copy(operations = listOf(op) + ws.operations) }
        return Result.success("Saved $moduleType record")
    }

    suspend fun updateAlertStatus(schoolId: String, alert: EarlyWarningAlert, status: String): Result<String> {
        val updated = alert.copy(status = status)
        supabase.saveAlert(updated, userId, schoolId).getOrThrow()
        mutateSuccess { ws -> ws.copy(alerts = ws.alerts.map { if (it.id == updated.id) updated else it }) }
        return Result.success("Alert updated to $status")
    }

    suspend fun logAiAudit(
        schoolId: String,
        actorName: String,
        role: PortalRole,
        actionType: String,
        summary: String
    ) {
        val log = AuditLog(
            id = newId("aud"), schoolId = schoolId, ownerId = userId,
            actorName = actorName, actorRole = role.name, actionType = actionType,
            moduleName = "AI_TOOLS", summary = summary, aiAction = true
        )
        supabase.appendAuditLog(log, userId, schoolId)
        mutateSuccess { ws -> ws.copy(auditLogs = listOf(log) + ws.auditLogs) }
    }

    private fun mutateSuccess(transform: (GsmsWorkspaceState) -> GsmsWorkspaceState) {
        val current = currentWorkspace() ?: return
        _uiState.value = UiState.Success(transform(current))
    }
}
