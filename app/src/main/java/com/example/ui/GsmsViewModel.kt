package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.GsmsRepository
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class GsmsWorkspaceState(
    val userProfile: UserProfile? = null,
    val schools: List<School> = emptyList(),
    val activeSchool: School? = null,
    val branches: List<Branch> = emptyList(),
    val activeBranchId: String = "branch_main",
    val activeRole: PortalRole = PortalRole.SCHOOL_ADMIN,
    val students: List<Student> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val academics: List<AcademicRecord> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val reportTemplates: List<ReportTemplate> = emptyList(),
    val reportCards: List<ReportCard> = emptyList(),
    val finance: List<FinanceEntry> = emptyList(),
    val leaveRequests: List<LeaveRequest> = emptyList(),
    val communications: List<CommunicationItem> = emptyList(),
    val operations: List<OperationRecord> = emptyList(),
    val alerts: List<EarlyWarningAlert> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class GsmsViewModel(
    private val repository: GsmsRepository,
    private val aiRepository: AiRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _bootError = MutableStateFlow<String?>(null)

    @Volatile
    private var currentActiveRole: PortalRole = PortalRole.SCHOOL_ADMIN

    init {
        viewModelScope.launch {
            val res = repository.ensureWorkspaceInitialized(currentUserId, userDisplayName, userEmail)
            res.onFailure { err ->
                Log.e(TAG, "Failed to initialize GSMS workspace", err)
                _bootError.value = err.localizedMessage ?: "Failed to initialize school workspace."
            }
        }
    }

    private val profileFlow = repository.observeUserProfile(currentUserId)
    private val schoolsFlow = repository.observeSchools(currentUserId)

    private val activeSchoolIdFlow: Flow<String> = combine(profileFlow, schoolsFlow) { profile, schools ->
        val preferred = profile?.activeSchoolId
        when {
            !preferred.isNullOrBlank() && schools.any { it.id == preferred } -> preferred
            schools.isNotEmpty() -> schools.first().id
            else -> ""
        }
    }.distinctUntilChanged()

    private val schoolDetailsFlow = activeSchoolIdFlow.flatMapLatest { schoolId ->
        if (schoolId.isBlank()) {
            flowOf(SchoolSubData())
        } else {
            val part1 = combine(
                repository.observeBranches(currentUserId, schoolId),
                repository.observeStudents(currentUserId, schoolId),
                repository.observeStaff(currentUserId, schoolId),
                repository.observeAcademics(currentUserId, schoolId),
                repository.observeAttendance(currentUserId, schoolId)
            ) { branches, students, staff, academics, attendance ->
                Part1Data(branches, students, staff, academics, attendance)
            }
            val part2 = combine(
                repository.observeReportTemplates(currentUserId, schoolId),
                repository.observeReportCards(currentUserId, schoolId),
                repository.observeFinance(currentUserId, schoolId),
                repository.observeLeaveRequests(currentUserId, schoolId),
                repository.observeCommunications(currentUserId, schoolId)
            ) { templates, reports, finance, leaves, comms ->
                Part2Data(templates, reports, finance, leaves, comms)
            }
            val part3 = combine(
                repository.observeOperations(currentUserId, schoolId),
                repository.observeAlerts(currentUserId, schoolId),
                repository.observeAuditLogs(currentUserId, schoolId)
            ) { ops, alerts, logs ->
                Part3Data(ops, alerts, logs)
            }
            combine(part1, part2, part3) { p1, p2, p3 ->
                SchoolSubData(
                    branches = p1.branches,
                    students = p1.students,
                    staff = p1.staff,
                    academics = p1.academics,
                    attendance = p1.attendance,
                    reportTemplates = p2.templates,
                    reportCards = p2.reports,
                    finance = p2.finance,
                    leaveRequests = p2.leaves,
                    communications = p2.comms,
                    operations = p3.ops,
                    alerts = p3.alerts,
                    auditLogs = p3.logs
                )
            }
        }
    }

    val uiState: StateFlow<UiState<GsmsWorkspaceState>> = combine(
        profileFlow,
        schoolsFlow,
        schoolDetailsFlow,
        _bootError
    ) { profile, schools, sub, bootErr ->
        if (bootErr != null && schools.isEmpty()) {
            UiState.Error(bootErr)
        } else if (schools.isEmpty() && profile == null) {
            UiState.Loading
        } else {
            val activeSchool = schools.firstOrNull { it.id == profile?.activeSchoolId } ?: schools.firstOrNull()
            val role = enumByNameOrDefault(profile?.activeRole, PortalRole.SCHOOL_ADMIN)
            currentActiveRole = role
            val branchId = profile?.activeBranchId?.takeIf { id ->
                sub.branches.any { it.id == id }
            } ?: sub.branches.firstOrNull()?.id ?: "branch_main"
            UiState.Success(
                GsmsWorkspaceState(
                    userProfile = profile,
                    schools = schools,
                    activeSchool = activeSchool,
                    branches = sub.branches,
                    activeBranchId = branchId,
                    activeRole = role,
                    students = sub.students,
                    staff = sub.staff,
                    academics = sub.academics,
                    attendance = sub.attendance,
                    reportTemplates = sub.reportTemplates,
                    reportCards = sub.reportCards,
                    finance = sub.finance,
                    leaveRequests = sub.leaveRequests,
                    communications = sub.communications,
                    operations = sub.operations,
                    alerts = sub.alerts,
                    auditLogs = sub.auditLogs
                )
            )
        }
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing GSMS workspace", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load school workspace"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun switchRole(role: PortalRole) {
        launchMutation {
            repository.updateUserSessionPreferences(role = role)
                .onSuccess { _statusBanner.value = "Switched to ${role.displayName}" }
        }
    }

    fun switchSchool(school: School) {
        if (!requireCapability(RoleCapability.SWITCH_SCHOOL_TENANT, "switch school tenants")) return
        launchMutation {
            repository.updateUserSessionPreferences(schoolId = school.id, branchId = "branch_main")
                .onSuccess { _statusBanner.value = "Active tenant: ${school.name}" }
        }
    }

    fun switchBranch(branch: Branch) {
        if (!requireCapability(RoleCapability.SWITCH_BRANCH, "switch school branches")) return
        launchMutation {
            repository.updateUserSessionPreferences(branchId = branch.id)
                .onSuccess { _statusBanner.value = "Active branch: ${branch.name}" }
        }
    }

    fun toggleLowDataMode(enabled: Boolean) {
        launchMutation {
            repository.updateUserSessionPreferences(lowDataMode = enabled)
                .onSuccess {
                    _statusBanner.value = if (enabled) "Low-Data Mode enabled" else "Standard Mode enabled"
                }
        }
    }

    fun toggleModule(school: School, module: GsmsModule, role: PortalRole) {
        if (!requireCapability(RoleCapability.MANAGE_MODULES, "modify the Module Registry", role)) return
        launchMutation {
            repository.toggleSchoolModule(school, module, currentActorName(), role)
                .onSuccess { _statusBanner.value = "Updated module: ${module.label}" }
        }
    }

    fun createSchool(name: String, code: String, city: String, country: String) {
        if (!requireCapability(RoleCapability.CREATE_SCHOOL_TENANT, "provision new schools")) return
        launchMutation {
            repository.createSchool(name, code, city, country, currentActorName())
                .onSuccess { newId ->
                    repository.updateUserSessionPreferences(schoolId = newId, branchId = "branch_main")
                    _statusBanner.value = "Provisioned new school tenant: $name"
                }
        }
    }

    fun updateAcademicPeriod(schoolId: String, year: String, term: String) {
        if (!requireCapability(RoleCapability.UPDATE_ACADEMIC_PERIOD, "change academic year/term")) return
        launchMutation {
            repository.updateSchoolPeriod(schoolId, year, term, currentActorName())
                .onSuccess { _statusBanner.value = "Active period updated to $year ($term)" }
        }
    }

    fun addStudent(
        school: School,
        branchId: String,
        admissionNo: String,
        fullName: String,
        className: String,
        parentName: String
    ) {
        if (!requireCapability(RoleCapability.ENROLL_STUDENT, "enroll students")) return
        launchMutation {
            repository.addStudent(school, branchId, admissionNo, fullName, className, parentName, currentActorName())
                .onSuccess { _statusBanner.value = "Enrolled $fullName" }
        }
    }

    fun recordAttendance(school: School, branchId: String, student: Student, status: String) {
        if (!requireCapability(RoleCapability.MARK_ATTENDANCE, "mark attendance")) return
        launchMutation {
            repository.recordAttendance(school, branchId, student.fullName, student.className, status, currentActorName())
                .onSuccess { _statusBanner.value = "${student.fullName} marked $status" }
        }
    }

    fun addAcademicRecord(
        school: School,
        branchId: String,
        recordType: String,
        title: String,
        className: String,
        subjectName: String,
        teacherName: String,
        scheduleSlot: String,
        score: Double
    ) {
        if (!requireCapability(RoleCapability.EDIT_ACADEMICS, "add academic entries")) return
        launchMutation {
            repository.addAcademicRecord(
                school, branchId, recordType, title, className, subjectName, teacherName, scheduleSlot, score
            ).onSuccess { _statusBanner.value = "Saved $recordType: $title" }
        }
    }

    fun createReportTemplate(school: School, name: String, classLevel: String, sectionsCsv: String, gradingScale: String) {
        if (!requireCapability(RoleCapability.MANAGE_REPORT_TEMPLATES, "create report templates")) return
        val sections = sectionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Core Subjects", "Attendance", "Teacher Remarks", "Principal Sign-Off") }
        launchMutation {
            repository.createReportTemplate(school, name, classLevel, sections, gradingScale, currentActorName())
                .onSuccess { _statusBanner.value = "Created report template: $name" }
        }
    }

    fun saveOrSubmitReportCard(
        school: School,
        branchId: String,
        student: Student,
        template: ReportTemplate,
        gradesSummary: String,
        averageScore: Double,
        teacherRemarks: String,
        aiSummary: String?,
        submitNow: Boolean
    ) {
        if (!requireCapability(RoleCapability.FILL_SUBMIT_REPORT_CARDS, "fill or submit report cards")) return
        launchMutation {
            repository.createOrSubmitReportCard(
                school, branchId, student, template, gradesSummary, averageScore,
                teacherRemarks, aiSummary, submitNow, currentActorName()
            ).onSuccess {
                _statusBanner.value = if (submitNow) "Report submitted for Headteacher review" else "Draft saved"
            }
        }
    }

    fun reviewReportCard(
        schoolId: String,
        report: ReportCard,
        nextStatus: ReportStatus,
        headteacherRemarks: String,
        reviewerRole: PortalRole
    ) {
        val required = if (nextStatus == ReportStatus.SUBMITTED) {
            RoleCapability.FILL_SUBMIT_REPORT_CARDS
        } else {
            RoleCapability.APPROVE_PUBLISH_REPORT_CARDS
        }
        if (!requireCapability(required, "move report cards to ${nextStatus.label}", reviewerRole)) return
        launchMutation {
            repository.transitionReportCard(schoolId, report, nextStatus, headteacherRemarks, currentActorName(), reviewerRole)
                .onSuccess { _statusBanner.value = "Report card marked ${nextStatus.label}" }
        }
    }

    fun submitLeaveRequest(
        school: School,
        branchId: String,
        staff: StaffMember,
        leaveType: String,
        startDate: String,
        endDate: String,
        daysCount: Long,
        reason: String,
        aiConflictNote: String?
    ) {
        if (!requireCapability(RoleCapability.APPLY_LEAVE, "submit leave requests")) return
        launchMutation {
            repository.submitLeaveRequest(
                school, branchId, staff, leaveType, startDate, endDate, daysCount, reason, aiConflictNote, currentActorName()
            ).onSuccess { _statusBanner.value = "Leave request submitted for ${staff.fullName}" }
        }
    }

    fun reviewLeaveRequest(
        schoolId: String,
        leave: LeaveRequest,
        staffMember: StaffMember?,
        approve: Boolean,
        reviewerRole: PortalRole
    ) {
        if (!requireCapability(RoleCapability.APPROVE_LEAVE, "approve or reject leave requests", reviewerRole)) return
        launchMutation {
            repository.reviewLeaveRequest(schoolId, leave, staffMember, approve, currentActorName(), reviewerRole)
                .onSuccess {
                    _statusBanner.value = if (approve) {
                        "Approved leave: ${leave.staffName} is now marked On Leave"
                    } else {
                        "Rejected leave request for ${leave.staffName}"
                    }
                }
        }
    }

    fun recordFinancePayment(schoolId: String, entry: FinanceEntry, amountUsd: Double) {
        if (!requireCapability(RoleCapability.RECORD_FINANCE_PAYMENT, "record fee payments")) return
        launchMutation {
            repository.recordFinancePayment(schoolId, entry, amountUsd, currentActorName())
                .onSuccess { _statusBanner.value = "Payment recorded for ${entry.referenceNo}" }
        }
    }

    fun sendCommunication(
        school: School,
        branchId: String,
        channel: String,
        audience: String,
        title: String,
        body: String,
        senderRole: PortalRole,
        aiDrafted: Boolean
    ) {
        if (!requireCapability(RoleCapability.COMPOSE_COMMUNICATIONS, "send school notices", senderRole)) return
        launchMutation {
            repository.sendCommunication(
                school, branchId, channel, audience, title, body, currentActorName(), senderRole, aiDrafted
            ).onSuccess { _statusBanner.value = "Dispatched $channel message" }
        }
    }

    fun addOperationRecord(
        school: School,
        branchId: String,
        moduleType: String,
        title: String,
        subtitle: String,
        partyName: String,
        status: String,
        metricLabel: String,
        notes: String,
        actorRole: PortalRole
    ) {
        val sectionName = when (moduleType) {
            "LIBRARY_BOOK", "LIBRARY_BORROW" -> "Library"
            "VEHICLE", "ROUTE", "TRIP" -> "Transport"
            "WELFARE" -> "Welfare"
            "DISCIPLINE" -> "Discipline"
            "INVENTORY", "ASSET" -> "Assets"
            "HOSTEL" -> "Hostel"
            "ADMISSION" -> "Admissions"
            else -> "Events"
        }
        if (!requireCapability(RoleCapability.EDIT_OPERATIONS, "add $sectionName records", actorRole) ||
            sectionName !in RolePolicy.operationSections(actorRole, school)
        ) {
            _statusBanner.value = "Access denied: ${actorRole.shortTitle} cannot edit $sectionName records."
            return
        }
        launchMutation {
            repository.addOperationRecord(
                school, branchId, moduleType, title, subtitle, partyName, status, metricLabel, notes, currentActorName(), actorRole
            ).onSuccess { _statusBanner.value = "Saved $moduleType record" }
        }
    }

    fun updateAlertStatus(schoolId: String, alert: EarlyWarningAlert, status: String, role: PortalRole) {
        if (!requireCapability(RoleCapability.MANAGE_ALERTS, "update early warning alerts", role)) return
        launchMutation {
            repository.updateAlertStatus(schoolId, alert, status, currentActorName(), role)
                .onSuccess { _statusBanner.value = "Alert updated to $status" }
        }
    }

    // --- GEMINI AI LOGIC WORKFLOWS ---

    fun askAiCopilot(workspace: GsmsWorkspaceState, prompt: String) {
        val activeSchool = workspace.activeSchool
        if (activeSchool != null && !activeSchool.hasModule(GsmsModule.AI_TOOLS)) {
            _aiState.value = AiUiState.Error("AI Tools module is currently disabled for ${activeSchool.name}.")
            return
        }
        val contextSummary = buildRoleScopedSummary(workspace)
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                aiRepository.streamAssistantResponse(
                    role = workspace.activeRole,
                    activeSchool = activeSchool,
                    dataSummary = contextSummary,
                    userPrompt = prompt
                ).collect { partial ->
                    _aiState.value = AiUiState.Streaming(partial)
                }
                val final = (_aiState.value as? AiUiState.Streaming)?.text
                if (!final.isNullOrBlank()) {
                    _aiState.value = AiUiState.Success(final)
                    if (activeSchool != null) {
                        repository.logAiAudit(
                            activeSchool.id,
                            currentActorName(),
                            workspace.activeRole,
                            "AI_COPILOT_QUERY",
                            "AI prompt in ${workspace.activeRole.shortTitle}: ${prompt.take(120)}"
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiUiState.Error(e.localizedMessage ?: "AI request failed.")
            }
        }
    }

    fun generateQuickAiDraft(
        workspace: GsmsWorkspaceState,
        taskPrompt: String,
        actionType: String,
        onSuccess: (String) -> Unit
    ) {
        val activeSchool = workspace.activeSchool
        if (activeSchool != null && !activeSchool.hasModule(GsmsModule.AI_TOOLS)) {
            _statusBanner.value = "Enable AI Tools in Module Registry to run AI generation."
            return
        }
        viewModelScope.launch {
            _statusBanner.value = "Gemini AI generating..."
            val res = aiRepository.generateWorkflowText(
                role = workspace.activeRole,
                activeSchool = activeSchool,
                dataSummary = buildRoleScopedSummary(workspace),
                taskPrompt = taskPrompt
            )
            res.onSuccess { text ->
                _statusBanner.value = "AI draft ready"
                if (activeSchool != null) {
                    repository.logAiAudit(activeSchool.id, currentActorName(), workspace.activeRole, actionType, "Generated AI workflow content ($actionType)")
                }
                onSuccess(text)
            }.onFailure { err ->
                _statusBanner.value = "AI Error: ${err.localizedMessage ?: "Generation failed"}"
            }
        }
    }

    private fun requireCapability(
        capability: RoleCapability,
        actionLabel: String,
        role: PortalRole = currentActiveRole
    ): Boolean {
        if (!role.has(capability)) {
            _statusBanner.value = "Access denied: ${role.shortTitle} cannot $actionLabel."
            return false
        }
        return true
    }

    private fun buildRoleScopedSummary(w: GsmsWorkspaceState): String {
        val school = w.activeSchool
        val sb = StringBuilder()
        when (w.activeRole) {
            PortalRole.SUPER_ADMIN -> {
                sb.appendLine("Platform Schools (${w.schools.size}):")
                w.schools.forEach {
                    sb.appendLine("- ${it.name} (${it.city}, ${it.country}): ${it.studentCount} students, ${it.staffCount} staff, USD ${it.monthlyRevenueUsd.toInt()}/mo, status=${it.subscriptionStatus}, churnRisk=${it.churnRiskScore}%")
                }
            }
            PortalRole.SCHOOL_ADMIN, PortalRole.HEADTEACHER -> {
                sb.appendLine("School: ${school?.name ?: "None"}, Students: ${w.students.size} tracked (total ${school?.studentCount ?: 0}), Staff: ${w.staff.size}")
                sb.appendLine("At-Risk Students: ${w.students.filter { it.riskLevel == RiskLevel.HIGH.name || it.riskLevel == RiskLevel.CRITICAL.name }.joinToString { "${it.fullName} (${it.riskLevel}, Avg ${it.averageScore}%, Att ${it.attendanceRate}%)" }}")
                sb.appendLine("Reports: ${w.reportCards.groupBy { it.status }.mapValues { it.value.size }}")
                sb.appendLine("Pending Leaves: ${w.leaveRequests.count { it.status == LeaveStatus.PENDING.name }}, Open Alerts: ${w.alerts.count { it.status != "RESOLVED" }}")
            }
            PortalRole.TEACHER -> {
                sb.appendLine("School: ${school?.name ?: "None"}, Teaching Timetable & Exams: ${w.academics.take(6).joinToString { "${it.title} (${it.className})" }}")
                sb.appendLine("Students (${w.students.size}): ${w.students.take(5).joinToString { "${it.fullName} (${it.className}, Avg ${it.averageScore}%, Att ${it.attendanceRate}%)" }}")
                sb.appendLine("Report Cards: ${w.reportCards.groupBy { it.status }.mapValues { it.value.size }}")
            }
            PortalRole.STUDENT -> {
                val focusStudent = w.students.firstOrNull()
                if (focusStudent != null) {
                    sb.appendLine("Student Focus: ${focusStudent.fullName} (${focusStudent.className}), Avg ${focusStudent.averageScore}%, Attendance ${focusStudent.attendanceRate}%")
                }
                w.reportCards.filter { it.status == ReportStatus.PUBLISHED.name && (focusStudent == null || it.studentId == focusStudent.id) }.forEach {
                    sb.appendLine("Published Report: ${it.studentName} (${it.term}) Avg ${it.averageScore}% - ${it.subjectGradesSummary}")
                }
            }
            PortalRole.PARENT -> {
                val focusStudent = w.students.firstOrNull()
                if (focusStudent != null) {
                    sb.appendLine("Child Focus: ${focusStudent.fullName} (${focusStudent.className}), Avg ${focusStudent.averageScore}%, Attendance ${focusStudent.attendanceRate}%, Fee Balance USD ${focusStudent.feeBalanceUsd.toInt()}")
                }
                w.reportCards.filter { it.status == ReportStatus.PUBLISHED.name && (focusStudent == null || it.studentId == focusStudent.id) }.forEach {
                    sb.appendLine("Published Report: ${it.studentName} (${it.term}) Avg ${it.averageScore}% - ${it.subjectGradesSummary}")
                }
            }
            PortalRole.FINANCE_OFFICER -> {
                val billed = w.finance.sumOf { it.amountUsd }
                val paid = w.finance.sumOf { it.paidUsd }
                sb.appendLine("Finance Ledger: Total Billed USD ${"%.0f".format(billed)}, Total Paid USD ${"%.0f".format(paid)}, Outstanding USD ${"%.0f".format(billed - paid)}")
                w.finance.take(8).forEach {
                    sb.appendLine("- [${it.entryType}] ${it.referenceNo} ${it.title}: USD ${"%.0f".format(it.paidUsd)} / USD ${"%.0f".format(it.amountUsd)} (${it.status})")
                }
            }
            PortalRole.HR_MANAGER -> {
                val onLeave = w.staff.filter { it.onLeave }
                sb.appendLine("Staff Count: ${w.staff.size}, Currently On Leave: ${onLeave.size}, Pending Leaves: ${w.leaveRequests.count { it.status == LeaveStatus.PENDING.name }}")
                w.leaveRequests.forEach {
                    sb.appendLine("- Leave: ${it.staffName} (${it.leaveType}, ${it.daysCount}d, ${it.startDate}..${it.endDate}) status=${it.status}, note=${it.aiConflictNote ?: "None"}")
                }
            }
            PortalRole.LIBRARIAN -> {
                val libOps = w.operations.filter { it.moduleType in setOf("LIBRARY_BOOK", "LIBRARY_BORROW") }
                sb.appendLine("Library Records (${libOps.size}): ${libOps.joinToString { "${it.title} [${it.status}]" }}")
            }
            PortalRole.TRANSPORT_MANAGER -> {
                val transOps = w.operations.filter { it.moduleType in setOf("VEHICLE", "ROUTE", "TRIP") }
                sb.appendLine("Transport Fleet & Routes (${transOps.size}): ${transOps.joinToString { "${it.title} (${it.subtitle ?: ""}) [${it.status}]" }}")
            }
            PortalRole.WELFARE_NURSE -> {
                val welfareOps = w.operations.filter { it.moduleType in setOf("WELFARE", "DISCIPLINE", "HOSTEL") }
                sb.appendLine("Welfare & Safeguarding (${welfareOps.size}): ${welfareOps.joinToString { "${it.title} [${it.status}]" }}")
                sb.appendLine("Welfare/Attendance Alerts: ${RolePolicy.filterAlertsForRole(PortalRole.WELFARE_NURSE, w.alerts).joinToString { "${it.targetName}: ${it.reason}" }}")
            }
            PortalRole.ADMISSIONS_RECEPTION -> {
                val admOps = w.operations.filter { it.moduleType in setOf("ADMISSION", "EVENT") }
                sb.appendLine("Admissions & Events (${admOps.size}): ${admOps.joinToString { "${it.title} [${it.status}]" }}")
                sb.appendLine("Enrolled Students Tracked: ${w.students.size}")
            }
        }
        return sb.toString()
    }

    private fun currentActorName(): String = userDisplayName.ifBlank { "GSMS User" }

    private fun launchMutation(block: suspend () -> Result<Any?>) {
        viewModelScope.launch {
            val res = block()
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Operation failed."
            }
        }
    }

    private data class Part1Data(
        val branches: List<Branch>,
        val students: List<Student>,
        val staff: List<StaffMember>,
        val academics: List<AcademicRecord>,
        val attendance: List<AttendanceRecord>
    )

    private data class Part2Data(
        val templates: List<ReportTemplate>,
        val reports: List<ReportCard>,
        val finance: List<FinanceEntry>,
        val leaves: List<LeaveRequest>,
        val comms: List<CommunicationItem>
    )

    private data class Part3Data(
        val ops: List<OperationRecord>,
        val alerts: List<EarlyWarningAlert>,
        val logs: List<AuditLog>
    )

    private data class SchoolSubData(
        val branches: List<Branch> = emptyList(),
        val students: List<Student> = emptyList(),
        val staff: List<StaffMember> = emptyList(),
        val academics: List<AcademicRecord> = emptyList(),
        val attendance: List<AttendanceRecord> = emptyList(),
        val reportTemplates: List<ReportTemplate> = emptyList(),
        val reportCards: List<ReportCard> = emptyList(),
        val finance: List<FinanceEntry> = emptyList(),
        val leaveRequests: List<LeaveRequest> = emptyList(),
        val communications: List<CommunicationItem> = emptyList(),
        val operations: List<OperationRecord> = emptyList(),
        val alerts: List<EarlyWarningAlert> = emptyList(),
        val auditLogs: List<AuditLog> = emptyList()
    )

    private companion object {
        const val TAG = "GsmsViewModel"
    }
}
