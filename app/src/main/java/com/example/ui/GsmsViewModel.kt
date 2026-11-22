package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.supabase.SupabaseSyncRepository
import com.example.data.supabase.SupabaseWorkspaceController
import com.example.model.*
import kotlinx.coroutines.CancellationException
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
    val accessAssignments: List<UserAccessAssignment> = emptyList(),
    val activeExtraCapabilities: Set<RoleCapability> = emptySet(),
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),
    val linkedStudentIds: List<String> = emptyList(),
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

class GsmsViewModel(
    private val aiRepository: AiRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val supabaseRepository: SupabaseSyncRepository
) : ViewModel() {
    private val workspaceController = SupabaseWorkspaceController(
        supabase = supabaseRepository,
        userId = currentUserId,
        displayName = userDisplayName,
        userEmail = userEmail
    )

    val uiState: StateFlow<UiState<GsmsWorkspaceState>> = workspaceController.uiState
    val supabaseState = supabaseRepository.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()
    private val _focusStudentId = MutableStateFlow<String?>(null)
    val focusStudentId: StateFlow<String?> = _focusStudentId.asStateFlow()

    @Volatile private var currentActiveRole: PortalRole = PortalRole.SCHOOL_ADMIN

    init {
        viewModelScope.launch {
            workspaceController.boot().onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not load the Supabase workspace."
            }
        }
        viewModelScope.launch {
            uiState.collect { state ->
                if (state is UiState.Success) currentActiveRole = state.data.activeRole
            }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun clearBanner() { _statusBanner.value = null }
    fun showBanner(message: String) { _statusBanner.value = message }

    fun selectFocusStudent(student: Student) {
        if (!RolePolicy.requiresLearnerScope(currentActiveRole)) {
            _statusBanner.value = "Access denied: ${currentActiveRole.shortTitle} is not scoped to a single learner."
            return
        }
        _focusStudentId.value = student.id
        _statusBanner.value = "Viewing ${student.fullName}"
    }

    fun switchRole(role: PortalRole) {
        val ws = (uiState.value as? UiState.Success)?.data
        if (ws?.userProfile?.resolvedPrimaryRole() != PortalRole.SUPER_ADMIN) {
            _statusBanner.value = "Access denied: only a platform/school owner may switch portal roles."
            return
        }
        launchMutation { workspaceController.switchRole(role) }
    }

    fun switchSchool(school: School) {
        if (!requireCapability(RoleCapability.SWITCH_SCHOOL_TENANT, "switch school tenants")) return
        launchMutation { workspaceController.switchSchool(school) }
    }

    fun switchBranch(branch: Branch) {
        if (!requireCapability(RoleCapability.SWITCH_BRANCH, "switch school branches")) return
        launchMutation { workspaceController.switchBranch(branch) }
    }

    fun toggleLowDataMode(enabled: Boolean) {
        launchMutation { workspaceController.toggleLowDataMode(enabled) }
    }

    fun createBranch(schoolId: String, name: String, code: String, city: String) {
        if (!requireCapability(RoleCapability.MANAGE_BRANCHES, "create branches")) return
        launchMutation { workspaceController.createBranch(schoolId, name, code, city, currentActorName(), currentActiveRole) }
    }

    fun delegateUserPermissions(
        schoolId: String,
        assignment: UserAccessAssignment,
        extraPermissions: List<String>,
        assignedClasses: List<String>,
        assignedSubjects: List<String>,
        linkedStudentIds: List<String>
    ) {
        if (!requireCapability(RoleCapability.MANAGE_USER_PERMISSIONS, "delegate user permissions")) return
        launchMutation {
            workspaceController.delegatePermissions(
                schoolId, assignment, extraPermissions, assignedClasses,
                assignedSubjects, linkedStudentIds, currentActorName()
            )
        }
    }

    fun createAccessAssignment(
        schoolId: String,
        fullName: String,
        email: String,
        primaryRole: PortalRole,
        assignedClasses: List<String>,
        assignedSubjects: List<String>,
        linkedStudentIds: List<String>,
        extraPermissions: List<String>
    ) {
        if (!requireCapability(RoleCapability.MANAGE_USER_PERMISSIONS, "provision portal access")) return
        launchMutation {
            workspaceController.createAccessAssignment(
                schoolId, fullName, email, primaryRole, assignedClasses,
                assignedSubjects, linkedStudentIds, extraPermissions, currentActorName()
            )
        }
    }

    fun signOutSupabase() {
        viewModelScope.launch {
            supabaseRepository.signOut().onFailure {
                _statusBanner.value = it.localizedMessage ?: "Supabase sign-out failed."
            }
        }
    }

    fun signInSupabase(email: String, password: String) {
        viewModelScope.launch {
            supabaseRepository.signIn(email, password).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Supabase sign-in failed."
            }.onSuccess { _statusBanner.value = it }
        }
    }

    fun signUpSupabase(email: String, password: String) {
        viewModelScope.launch {
            supabaseRepository.signUp(email, password).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Supabase registration failed."
            }.onSuccess { _statusBanner.value = it }
        }
    }

    fun testSupabaseConnection() {
        viewModelScope.launch {
            supabaseRepository.testConnection().onFailure {
                _statusBanner.value = it.localizedMessage ?: "Supabase connection test failed."
            }.onSuccess { _statusBanner.value = it }
        }
    }

    fun pushWorkspaceToSupabase() {
        val ws = (uiState.value as? UiState.Success)?.data ?: run {
            _statusBanner.value = "Workspace is not ready to sync."
            return
        }
        viewModelScope.launch {
            supabaseRepository.pushWorkspace(
                ws, currentUserId, ws.activeRole, ws.activeExtraCapabilities
            ).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Supabase sync failed."
            }.onSuccess { _statusBanner.value = it }
        }
    }

    fun refreshSupabaseCounts() {
        viewModelScope.launch {
            ((uiState.value as? UiState.Success)?.data?.activeSchool?.id
                ?.let { supabaseRepository.pullSchoolCounts(it) }
                ?: Result.failure(IllegalStateException("No active school is selected."))).onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not refresh Supabase counts."
            }.onSuccess { counts -> _statusBanner.value = "Verified counts for ${counts.size} Supabase tables." }
        }
    }

    fun toggleSupabaseAutoMirror(enabled: Boolean) {
        supabaseRepository.toggleAutoMirror(enabled)
            .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not change auto-sync setting." }
            .onSuccess { active ->
                _statusBanner.value = if (active) "Supabase auto-sync enabled." else "Supabase auto-sync paused."
            }
    }

    fun toggleModule(school: School, module: GsmsModule, role: PortalRole) {
        if (!requireCapability(RoleCapability.MANAGE_MODULES, "modify the Module Registry", role)) return
        launchMutation { workspaceController.toggleModule(school, module, currentActorName(), role) }
    }

    fun createSchool(name: String, code: String, city: String, country: String) {
        if (!requireCapability(RoleCapability.CREATE_SCHOOL_TENANT, "provision new schools")) return
        launchMutation {
            workspaceController.createSchool(name, code, city, country, currentActorName(), currentActiveRole)
        }
    }

    fun updateAcademicPeriod(schoolId: String, year: String, term: String) {
        if (!requireCapability(RoleCapability.UPDATE_ACADEMIC_PERIOD, "change academic year/term")) return
        val school = (uiState.value as? UiState.Success)?.data?.schools?.firstOrNull { it.id == schoolId }
        if (school == null) { _statusBanner.value = "School not found."; return }
        launchMutation { workspaceController.updatePeriod(school, year, term) }
    }

    fun addStudent(school: School, branchId: String, admissionNo: String, fullName: String, className: String, parentName: String) {
        if (!requireCapability(RoleCapability.ENROLL_STUDENT, "enroll students")) return
        launchMutation { workspaceController.addStudent(school, branchId, admissionNo, fullName, className, parentName) }
    }

    fun recordAttendance(school: School, branchId: String, student: Student, status: String) {
        if (!requireCapability(RoleCapability.MARK_ATTENDANCE, "mark attendance")) return
        launchMutation { workspaceController.recordAttendance(school, branchId, student, status) }
    }

    fun addAcademicRecord(school: School, branchId: String, recordType: String, title: String, className: String,
        subjectName: String, teacherName: String, scheduleSlot: String, score: Double) {
        if (!requireCapability(RoleCapability.EDIT_ACADEMICS, "add academic entries")) return
        launchMutation { workspaceController.addAcademicRecord(school, branchId, recordType, title, className, subjectName, teacherName, scheduleSlot, score) }
    }

    fun createReportTemplate(school: School, name: String, classLevel: String, sectionsCsv: String, gradingScale: String) {
        if (!requireCapability(RoleCapability.MANAGE_REPORT_TEMPLATES, "create report templates")) return
        val sections = sectionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Core Subjects", "Attendance", "Teacher Remarks", "Principal Sign-Off") }
        launchMutation { workspaceController.createReportTemplate(school, name, classLevel, sections, gradingScale) }
    }

    fun saveOrSubmitReportCard(school: School, branchId: String, student: Student, template: ReportTemplate,
        gradesSummary: String, averageScore: Double, teacherRemarks: String, aiSummary: String?, submitNow: Boolean) {
        if (!requireCapability(RoleCapability.FILL_SUBMIT_REPORT_CARDS, "fill or submit report cards")) return
        launchMutation {
            workspaceController.saveOrSubmitReportCard(school, branchId, student, template, gradesSummary,
                averageScore, teacherRemarks, aiSummary, submitNow, currentActorName())
        }
    }

    fun reviewReportCard(schoolId: String, report: ReportCard, nextStatus: ReportStatus,
        headteacherRemarks: String, reviewerRole: PortalRole) {
        val required = if (nextStatus == ReportStatus.SUBMITTED) RoleCapability.FILL_SUBMIT_REPORT_CARDS
            else RoleCapability.APPROVE_PUBLISH_REPORT_CARDS
        if (!requireCapability(required, "move report cards to ${nextStatus.label}", reviewerRole)) return
        launchMutation { workspaceController.reviewReportCard(schoolId, report, nextStatus, headteacherRemarks, currentActorName()) }
    }

    fun submitLeaveRequest(school: School, branchId: String, staff: StaffMember, leaveType: String,
        startDate: String, endDate: String, daysCount: Long, reason: String, aiConflictNote: String?) {
        if (!requireCapability(RoleCapability.APPLY_LEAVE, "submit leave requests")) return
        launchMutation { workspaceController.submitLeaveRequest(school, branchId, staff, leaveType, startDate, endDate, daysCount, reason, aiConflictNote) }
    }

    fun reviewLeaveRequest(schoolId: String, leave: LeaveRequest, staffMember: StaffMember?, approve: Boolean, reviewerRole: PortalRole) {
        if (!requireCapability(RoleCapability.APPROVE_LEAVE, "approve or reject leave requests", reviewerRole)) return
        launchMutation { workspaceController.reviewLeaveRequest(schoolId, leave, staffMember, approve, currentActorName()) }
    }

    fun createFinanceInvoice(
        school: School,
        branchId: String,
        student: Student,
        title: String,
        category: String,
        amountUsd: Double,
        role: PortalRole
    ) {
        if (!requireCapability(RoleCapability.CREATE_FINANCE_INVOICE, "create fee invoices", role)) return
        if (amountUsd <= 0.0) {
            _statusBanner.value = "Invoice amount must be greater than zero."
            return
        }
        launchMutation {
            workspaceController.createFinanceInvoice(school, branchId, student, title, category, amountUsd)
        }
    }

    fun recordFinancePayment(schoolId: String, entry: FinanceEntry, amountUsd: Double) {
        if (!requireCapability(RoleCapability.RECORD_FINANCE_PAYMENT, "record fee payments")) return
        launchMutation { workspaceController.recordFinancePayment(schoolId, entry, amountUsd) }
    }

    fun sendCommunication(school: School, branchId: String, channel: String, audience: String, title: String, body: String,
        senderRole: PortalRole, aiDrafted: Boolean) {
        if (!requireCapability(RoleCapability.COMPOSE_COMMUNICATIONS, "send school notices", senderRole)) return
        launchMutation { workspaceController.sendCommunication(school, branchId, channel, audience, title, body, currentActorName(), senderRole, aiDrafted) }
    }

    fun addOperationRecord(school: School, branchId: String, moduleType: String, title: String, subtitle: String,
        partyName: String, status: String, metricLabel: String, notes: String, actorRole: PortalRole) {
        val sectionName = when (moduleType) {
            "LIBRARY_BOOK", "LIBRARY_BORROW" -> "Library"
            "VEHICLE", "ROUTE", "TRIP" -> "Transport"
            "WELFARE" -> "Welfare"
            "DISCIPLINE" -> "Discipline"
            "COUNSELLING", "INTERVENTION", "SAFEGUARDING" -> "Counselling"
            "INVENTORY", "ASSET" -> "Assets"
            "STOCK", "STOCK_ISSUE", "STOCK_RECEIPT" -> "Stores"
            "HOSTEL" -> "Hostel"
            "ADMISSION" -> "Admissions"
            "FRONT_DESK" -> "Front Desk"
            "PROCUREMENT", "REQUISITION", "PURCHASE_ORDER", "VENDOR", "RFQ" -> "Procurement"
            "EXAM", "EXAM_TIMETABLE", "INVIGILATION", "MARK_MODERATION" -> "Examinations"
            "ICT_TICKET", "ICT_ASSET", "SYSTEM_INCIDENT", "ACCESS_REQUEST" -> "ICT Systems"
            else -> "Events"
        }
        val extra = (uiState.value as? UiState.Success)?.data
            ?.takeIf { it.activeRole == actorRole }?.activeExtraCapabilities.orEmpty()
        if (!RolePolicy.canManageOperationSection(actorRole, sectionName, extra) ||
            sectionName !in RolePolicy.operationSections(actorRole, school, extra)) {
            _statusBanner.value = "Access denied: ${actorRole.shortTitle} cannot edit $sectionName records."
            return
        }
        launchMutation { workspaceController.addOperationRecord(school, branchId, moduleType, title, subtitle, partyName, status, metricLabel, notes) }
    }

    fun updateOperationRecord(
        schoolId: String,
        operation: OperationRecord,
        title: String,
        subtitle: String,
        partyName: String,
        metricLabel: String,
        notes: String,
        role: PortalRole
    ) {
        val section = when (operation.moduleType) {
            "LIBRARY_BOOK", "LIBRARY_BORROW" -> "Library"
            "VEHICLE", "ROUTE", "TRIP" -> "Transport"
            "WELFARE" -> "Welfare"
            "DISCIPLINE" -> "Discipline"
            "COUNSELLING", "INTERVENTION", "SAFEGUARDING" -> "Counselling"
            "INVENTORY", "ASSET" -> "Assets"
            "STOCK", "STOCK_ISSUE", "STOCK_RECEIPT" -> "Stores"
            "HOSTEL" -> "Hostel"
            "ADMISSION" -> "Admissions"
            "FRONT_DESK" -> "Front Desk"
            "PROCUREMENT", "REQUISITION", "PURCHASE_ORDER", "VENDOR", "RFQ" -> "Procurement"
            "EXAM", "EXAM_TIMETABLE", "INVIGILATION", "MARK_MODERATION" -> "Examinations"
            "ICT_TICKET", "ICT_ASSET", "SYSTEM_INCIDENT", "ACCESS_REQUEST" -> "ICT Systems"
            else -> "Events"
        }
        val extra = (uiState.value as? UiState.Success)?.data
            ?.takeIf { it.activeRole == role }?.activeExtraCapabilities.orEmpty()
        if (!RolePolicy.canManageOperationSection(role, section, extra)) {
            _statusBanner.value = "Access denied: ${role.shortTitle} cannot edit $section records."
            return
        }
        launchMutation {
            workspaceController.updateOperationRecord(schoolId, operation, title, subtitle, partyName, metricLabel, notes)
        }
    }

    fun updateOperationStatus(schoolId: String, operation: OperationRecord, status: String, role: PortalRole) {
        val section = when (operation.moduleType) {
            "LIBRARY_BOOK", "LIBRARY_BORROW" -> "Library"
            "VEHICLE", "ROUTE", "TRIP" -> "Transport"
            "WELFARE" -> "Welfare"
            "DISCIPLINE" -> "Discipline"
            "COUNSELLING", "INTERVENTION", "SAFEGUARDING" -> "Counselling"
            "INVENTORY", "ASSET" -> "Assets"
            "STOCK", "STOCK_ISSUE", "STOCK_RECEIPT" -> "Stores"
            "HOSTEL" -> "Hostel"
            "ADMISSION" -> "Admissions"
            "FRONT_DESK" -> "Front Desk"
            "PROCUREMENT", "REQUISITION", "PURCHASE_ORDER", "VENDOR", "RFQ" -> "Procurement"
            "EXAM", "EXAM_TIMETABLE", "INVIGILATION", "MARK_MODERATION" -> "Examinations"
            "ICT_TICKET", "ICT_ASSET", "SYSTEM_INCIDENT", "ACCESS_REQUEST" -> "ICT Systems"
            else -> "Events"
        }
        val extra = (uiState.value as? UiState.Success)?.data
            ?.takeIf { it.activeRole == role }?.activeExtraCapabilities.orEmpty()
        if (!RolePolicy.canManageOperationSection(role, section, extra)) {
            _statusBanner.value = "Access denied: ${role.shortTitle} cannot update $section records."
            return
        }
        launchMutation { workspaceController.updateOperationStatus(schoolId, operation, status) }
    }

    fun updateAlertStatus(schoolId: String, alert: EarlyWarningAlert, status: String, role: PortalRole) {
        if (!requireCapability(RoleCapability.MANAGE_ALERTS, "update early warning alerts", role)) return
        launchMutation { workspaceController.updateAlertStatus(schoolId, alert, status) }
    }

    private fun requireCapability(capability: RoleCapability, actionLabel: String, role: PortalRole = currentActiveRole): Boolean {
        if (!role.has(capability)) {
            _statusBanner.value = "Access denied: ${role.shortTitle} cannot $actionLabel."
            return false
        }
        return true
    }

    private fun currentActorName(): String = userDisplayName.ifBlank { "GSMS User" }

    private fun launchMutation(block: suspend () -> Result<String>) {
        viewModelScope.launch {
            block().onFailure { _statusBanner.value = it.localizedMessage ?: "Operation failed." }
                .onSuccess { _statusBanner.value = it }
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
                        workspaceController.logAiAudit(
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
                    workspaceController.logAiAudit(activeSchool.id, currentActorName(), workspace.activeRole, actionType, "Generated AI workflow content ($actionType)")
                }
                onSuccess(text)
            }.onFailure { err ->
                _statusBanner.value = "AI Error: ${err.localizedMessage ?: "Generation failed"}"
            }
        }
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
                val focusStudent = RolePolicy.resolveFocusStudent(w.students, _focusStudentId.value)
                if (focusStudent != null) {
                    sb.appendLine("Student Focus: ${focusStudent.fullName} (${focusStudent.className}), Avg ${focusStudent.averageScore}%, Attendance ${focusStudent.attendanceRate}%")
                }
                w.reportCards.filter { it.status == ReportStatus.PUBLISHED.name && (focusStudent == null || it.studentId == focusStudent.id) }.forEach {
                    sb.appendLine("Published Report: ${it.studentName} (${it.term}) Avg ${it.averageScore}% - ${it.subjectGradesSummary}")
                }
            }
            PortalRole.PARENT -> {
                val focusStudent = RolePolicy.resolveFocusStudent(w.students, _focusStudentId.value)
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
            else -> {
                sb.appendLine("Role: ${w.activeRole.displayName}")
                sb.appendLine("Available scoped records: students=${w.students.size}, staff=${w.staff.size}, operations=${w.operations.size}, messages=${w.communications.size}")
            }
        }
        return sb.toString()
    }

    private companion object { const val TAG = "GsmsViewModel" }
}
