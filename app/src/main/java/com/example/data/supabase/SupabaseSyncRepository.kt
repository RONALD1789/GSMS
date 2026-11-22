package com.example.data.supabase

import android.util.Log
import android.content.Context
import com.example.model.*
import com.example.ui.GsmsWorkspaceState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class SupabaseSession(
    val userId: String,
    val email: String,
    val displayName: String
)

data class SupabaseSyncState(
    val isConfigured: Boolean = false,
    val configIssue: String? = null,
    val endpointLabel: String = "Not configured",
    val isAuthenticated: Boolean = false,
    val authEmail: String? = null,
    val autoMirrorEnabled: Boolean = false,
    val isBusy: Boolean = false,
    val connectionStatus: String = "Not tested",
    val lastLatencyMs: Long? = null,
    val lastSyncSummary: String? = null,
    val lastError: String? = null,
    val remoteCounts: Map<String, Int> = emptyMap()
)

class SupabaseSyncRepository(
    private val config: SupabaseConfig = SupabaseConfig.fromBuildConfig(),
    appContext: Context? = null
) {
    private val sessionStore = appContext?.let(::SupabaseSessionStore)
    // Strictly in-memory session state; never persisted to plaintext SharedPreferences or disk.
    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var supabaseUserId: String? = null

    private val _sessionFlow = MutableStateFlow<SupabaseSession?>(null)
    val sessionFlow: StateFlow<SupabaseSession?> = _sessionFlow.asStateFlow()

    private val _state = MutableStateFlow(
        SupabaseSyncState(
            isConfigured = config.isConfigured,
            configIssue = config.validationError,
            endpointLabel = config.maskedHost,
            connectionStatus = if (config.isConfigured) "Ready to connect" else "Secrets needed"
        )
    )
    val state: StateFlow<SupabaseSyncState> = _state.asStateFlow()

    val isConfigured: Boolean get() = config.isConfigured

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val errorAdapter by lazy {
        moshi.adapter(SupabaseErrorBody::class.java)
    }

    private val apiService: SupabaseApiService? by lazy {
        val baseUrl = config.normalizedBaseUrl ?: return@lazy null
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = accessToken ?: config.trimmedAnonKey
                val req = chain.request().newBuilder()
                    .header("apikey", config.trimmedAnonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }

    fun isAutoMirrorActive(): Boolean {
        val current = _state.value
        return current.isConfigured && current.isAuthenticated && current.autoMirrorEnabled
    }

    fun toggleAutoMirror(enabled: Boolean): Result<Boolean> {
        if (!config.isConfigured) {
            return Result.failure(IllegalStateException(config.validationError ?: "Supabase is not configured."))
        }
        _state.update { it.copy(autoMirrorEnabled = enabled, lastError = null) }
        return Result.success(enabled)
    }

    suspend fun testConnection(): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val start = System.currentTimeMillis()
            val response = withContext(Dispatchers.IO) { api.pingPostgrest() }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)

            if (response.isSuccessful) {
                val msg = "Connected to Supabase PostgREST (${elapsed}ms)"
                _state.update {
                    it.copy(
                        isBusy = false,
                        connectionStatus = "Connected",
                        lastLatencyMs = elapsed,
                        lastError = null
                    )
                }
                Result.success(msg)
            } else {
                val errMsg = formatHttpError("Connection test", response.code(), response.errorBody())
                _state.update {
                    it.copy(
                        isBusy = false,
                        connectionStatus = "HTTP ${response.code()}",
                        lastLatencyMs = elapsed,
                        lastError = errMsg
                    )
                }
                Result.failure(IllegalStateException(errMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase connection test failed: ${e.javaClass.simpleName}")
            failState("Network error reaching Supabase: ${e.localizedMessage ?: "Check connectivity"}")
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        val cleanEmail = email.trim()
        if (!isValidEmailFormat(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.signInWithPassword(body = SupabaseAuthRequest(cleanEmail, password))
            }
            handleAuthResponse("Email Sign-In", cleanEmail, null, response)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase sign-in failed: ${e.javaClass.simpleName}")
            failState("Email sign-in failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        email: String,
        displayName: String? = null
    ): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        val cleanEmail = email.trim()
        if (idToken.isBlank() || !isValidEmailFormat(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Google did not return a valid identity token and email address."))
        }
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.signInWithIdToken(body = SupabaseIdTokenRequest(idToken = idToken))
            }
            handleAuthResponse("Google Sign-In", cleanEmail, displayName, response)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase Google sign-in failed: ${e.javaClass.simpleName}")
            failState("Google sign-in failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String? = null
    ): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        val cleanEmail = email.trim()
        val cleanName = fullName?.trim()?.takeIf { it.isNotBlank() }
        if (!isValidEmailFormat(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.signUp(
                    SupabaseSignUpRequest(
                        email = cleanEmail,
                        password = password,
                        data = cleanName?.let { SupabaseUserMetadata(fullName = it) }
                    )
                )
            }
            handleAuthResponse("Email Registration", cleanEmail, cleanName, response)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase sign-up failed: ${e.javaClass.simpleName}")
            failState("Account registration failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        val cleanEmail = email.trim()
        if (!isValidEmailFormat(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Enter your account email address first."))
        }
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.recoverPassword(SupabaseRecoverRequest(cleanEmail))
            }
            if (response.isSuccessful) {
                val msg = "Password reset email requested for $cleanEmail."
                _state.update { it.copy(isBusy = false, lastError = null, lastSyncSummary = msg) }
                Result.success(msg)
            } else {
                failState(formatHttpError("Password reset", response.code(), response.errorBody()))
            }
        } catch (e: Exception) {
            failState("Password reset failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    suspend fun restoreSession(): Result<Boolean> {
        if (_sessionFlow.value != null) return Result.success(true)
        val stored = sessionStore?.load() ?: return Result.success(false)
        val api = apiService ?: return Result.failure(
            IllegalStateException(config.validationError ?: "Supabase Secrets are not configured.")
        )
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.refreshSession(body = SupabaseRefreshRequest(stored.refreshToken))
            }
            if (!response.isSuccessful) {
                sessionStore?.clear()
                accessToken = null
                supabaseUserId = null
                _sessionFlow.value = null
                _state.update { it.copy(isBusy = false, isAuthenticated = false, authEmail = null) }
                Result.failure(IllegalStateException(formatHttpError("Restore session", response.code(), response.errorBody())))
            } else {
                val body = response.body()
                if (body?.accessToken.isNullOrBlank()) {
                    sessionStore?.clear()
                    _state.update { it.copy(isBusy = false, isAuthenticated = false) }
                    Result.success(false)
                } else {
                    handleAuthResponse("Restore session", stored.email, stored.displayName, response)
                    Result.success(true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase session restoration failed: ${e.javaClass.simpleName}")
            _state.update { it.copy(isBusy = false, lastError = "Could not restore your Supabase session. Please sign in again.") }
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        val api = apiService
        val hadToken = !accessToken.isNullOrBlank()
        // Revoke the server session while the current access token is still attached.
        if (api != null && hadToken) {
            try {
                withContext(Dispatchers.IO) { api.signOut() }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase remote sign-out could not be confirmed: ${e.javaClass.simpleName}")
            }
        }
        accessToken = null
        supabaseUserId = null
        sessionStore?.clear()
        _sessionFlow.value = null
        _state.update {
            it.copy(
                isAuthenticated = false,
                authEmail = null,
                autoMirrorEnabled = false,
                remoteCounts = emptyMap(),
                lastError = null
            )
        }
        return Result.success(Unit)
    }

    suspend fun loadOrSeedWorkspaceForSession(session: SupabaseSession): Result<GsmsWorkspaceState> {
        val api = apiService ?: return Result.failure(
            IllegalStateException(config.validationError ?: "Supabase Secrets are not configured.")
        )
        if (accessToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Email session expired. Please sign in again."))
        }
        return try {
            val schoolsResp = withContext(Dispatchers.IO) { api.fetchSchools() }
            if (!schoolsResp.isSuccessful) {
                return Result.failure(
                    IllegalStateException(formatHttpError("Load schools", schoolsResp.code(), schoolsResp.errorBody()))
                )
            }
            val remoteSchools = schoolsResp.body().orEmpty().map { it.toDomain() }
            if (remoteSchools.isEmpty()) {
                return Result.failure(
                    SecurityException(
                        "No school workspace is assigned to this account. Ask your GSMS system administrator to provision your school access."
                    )
                )
            }

            val firstSchoolId = remoteSchools.first().id
            loadSchoolWorkspace(
                session = session,
                schools = remoteSchools,
                targetSchoolId = firstSchoolId,
                activeBranchId = null,
                roleOverride = null
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSchoolWorkspace(
        session: SupabaseSession,
        schools: List<School>,
        targetSchoolId: String,
        activeBranchId: String?,
        roleOverride: PortalRole?
    ): Result<GsmsWorkspaceState> {
        val api = apiService ?: return Result.failure(
            IllegalStateException(config.validationError ?: "Supabase Secrets are not configured.")
        )
        val activeSchool = schools.firstOrNull { it.id == targetSchoolId } ?: schools.firstOrNull()
            ?: return Result.failure(IllegalStateException("No accessible schools found for ${session.email}."))
        val schoolId = activeSchool.id
        if (!SupabaseConfig.isValidSchoolId(schoolId)) {
            return Result.failure(IllegalArgumentException("Invalid school identifier."))
        }

        return try {
            val filter = "eq.$schoolId"
            val workspace = withContext(Dispatchers.IO) {
                coroutineScope {
                    val branchesD = async { readList("gsms_branches", api.fetchBranches(schoolIdFilter = filter)) { it.toDomain() } }
                    val accessD = async { readList("gsms_user_access_assignments", api.fetchAccessAssignments(schoolIdFilter = filter)) { it.toDomain() } }
                    val staffD = async { readList("gsms_staff", api.fetchStaff(schoolIdFilter = filter)) { it.toDomain() } }
                    val academicsD = async { readList("gsms_academic_records", api.fetchAcademicRecords(schoolIdFilter = filter)) { it.toDomain() } }
                    val attendanceD = async { readList("gsms_attendance", api.fetchAttendance(schoolIdFilter = filter)) { it.toDomain() } }
                    val templatesD = async { readList("gsms_report_templates", api.fetchReportTemplates(schoolIdFilter = filter)) { it.toDomain() } }
                    val reportsD = async { readList("gsms_report_cards", api.fetchReportCards(schoolIdFilter = filter)) { it.toDomain() } }
                    val financeD = async { readList("gsms_finance_entries", api.fetchFinanceEntries(schoolIdFilter = filter)) { it.toDomain() } }
                    val leavesD = async { readList("gsms_leave_requests", api.fetchLeaveRequests(schoolIdFilter = filter)) { it.toDomain() } }
                    val commsD = async { readList("gsms_communications", api.fetchCommunications(schoolIdFilter = filter)) { it.toDomain() } }
                    val opsD = async { readList("gsms_operations", api.fetchOperations(schoolIdFilter = filter)) { it.toDomain() } }
                    val alertsD = async { readList("gsms_early_warning_alerts", api.fetchAlerts(schoolIdFilter = filter)) { it.toDomain() } }
                    val auditD = async { readList("gsms_audit_logs", api.fetchAuditLogs(schoolIdFilter = filter)) { it.toDomain() } }

                    val branches = branchesD.await()
                    val assignments = accessD.await()
                    val matchingAssignment = assignments.firstOrNull {
                        it.active && it.email.equals(session.email, ignoreCase = true)
                    }
                    val isOwner = activeSchool.ownerId == session.userId
                    val assignedRole = matchingAssignment?.resolvedRole()
                    val students = if (
                        !isOwner && (assignedRole == PortalRole.PARENT || assignedRole == PortalRole.STUDENT)
                    ) {
                        readList(
                            "gsms_parent_student_summaries",
                            api.fetchParentStudentSummaries(schoolIdFilter = filter)
                        ) { it.toDomain() }
                    } else {
                        readList(
                            "gsms_students",
                            api.fetchStudents(schoolIdFilter = filter)
                        ) { it.toDomain() }
                    }
                    if (!isOwner && matchingAssignment == null) {
                        throw SecurityException(
                            "Access denied: this account has no active access assignment for ${activeSchool.name}."
                        )
                    }
                    val primaryRole = if (isOwner) {
                        PortalRole.SUPER_ADMIN
                    } else {
                        matchingAssignment!!.resolvedRole()
                    }
                    // Only a verified school owner may switch between portal views.
                    // Delegated users remain constrained to their assigned primary role.
                    val effectiveRole = if (isOwner) {
                        roleOverride ?: PortalRole.SCHOOL_ADMIN
                    } else {
                        primaryRole
                    }
                    val extraCaps = matchingAssignment?.resolvedExtraCapabilities().orEmpty()
                    val assignedClasses = matchingAssignment?.assignedClasses.orEmpty()
                    val assignedSubjects = matchingAssignment?.assignedSubjects.orEmpty()
                    val linkedStudentIds = matchingAssignment?.linkedStudentIds.orEmpty()
                    val allBranchIds = branches.map { it.id }
                    val canAccessAllBranches = isOwner || primaryRole in setOf(
                        PortalRole.SCHOOL_ADMIN,
                        PortalRole.HEADTEACHER,
                        PortalRole.DEPUTY_HEADTEACHER
                    )
                    val allowedBranchIds = if (canAccessAllBranches) {
                        allBranchIds
                    } else {
                        matchingAssignment?.assignedBranchIds.orEmpty().filter { it in allBranchIds }
                    }
                    if (allowedBranchIds.isEmpty()) {
                        throw SecurityException("Access denied: no active campus branch is assigned to this account.")
                    }
                    val resolvedBranch = activeBranchId?.takeIf { it in allowedBranchIds }
                        ?: allowedBranchIds.first()

                    val profile = UserProfile(
                        id = session.userId,
                        userId = session.userId,
                        displayName = matchingAssignment?.fullName ?: session.displayName,
                        email = session.email,
                        primaryRole = primaryRole.name,
                        activeSchoolId = schoolId,
                        activeBranchId = resolvedBranch,
                        activeRole = effectiveRole.name,
                        assignedSchoolId = schoolId,
                        assignedBranchIds = allowedBranchIds,
                        assignedClasses = assignedClasses,
                        assignedSubjects = assignedSubjects,
                        linkedStudentIds = linkedStudentIds,
                        extraPermissions = matchingAssignment?.extraPermissions.orEmpty(),
                        lowDataMode = false
                    )

                    GsmsWorkspaceState(
                        userProfile = profile,
                        schools = schools,
                        activeSchool = activeSchool,
                        branches = branches,
                        activeBranchId = resolvedBranch,
                        activeRole = effectiveRole,
                        accessAssignments = assignments,
                        activeExtraCapabilities = extraCaps,
                        assignedClasses = assignedClasses,
                        assignedSubjects = assignedSubjects,
                        linkedStudentIds = linkedStudentIds,
                        students = students,
                        staff = staffD.await(),
                        academics = academicsD.await(),
                        attendance = attendanceD.await(),
                        reportTemplates = templatesD.await(),
                        reportCards = reportsD.await(),
                        finance = financeD.await(),
                        leaveRequests = leavesD.await(),
                        communications = commsD.await(),
                        operations = opsD.await(),
                        alerts = alertsD.await(),
                        auditLogs = auditD.await()
                    )
                }
            }
            Result.success(workspace)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullSchoolCounts(schoolId: String): Result<Map<String, Int>> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        if (!SupabaseConfig.isValidSchoolId(schoolId)) {
            return Result.failure(IllegalArgumentException("Invalid school identifier for PostgREST query."))
        }
        if (accessToken.isNullOrBlank()) {
            return failState("Sign in with your email / Supabase Auth first so RLS permits reading tenant rows.")
        }
        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val filter = "eq.$schoolId"
            val counts = withContext(Dispatchers.IO) {
                coroutineScope {
                    val schools = async { readCount(api.fetchSchoolIds()) }
                    val branches = async { readCount(api.fetchBranchIds(schoolIdFilter = filter)) }
                    val access = async { readCount(api.fetchAccessAssignmentIds(schoolIdFilter = filter)) }
                    val students = async { readCount(api.fetchStudentIds(schoolIdFilter = filter)) }
                    val staff = async { readCount(api.fetchStaffIds(schoolIdFilter = filter)) }
                    val academics = async { readCount(api.fetchAcademicIds(schoolIdFilter = filter)) }
                    val attendance = async { readCount(api.fetchAttendanceIds(schoolIdFilter = filter)) }
                    val templates = async { readCount(api.fetchReportTemplateIds(schoolIdFilter = filter)) }
                    val reports = async { readCount(api.fetchReportCardIds(schoolIdFilter = filter)) }
                    val finance = async { readCount(api.fetchFinanceIds(schoolIdFilter = filter)) }
                    val leaves = async { readCount(api.fetchLeaveIds(schoolIdFilter = filter)) }
                    val comms = async { readCount(api.fetchCommunicationIds(schoolIdFilter = filter)) }
                    val ops = async { readCount(api.fetchOperationIds(schoolIdFilter = filter)) }
                    val alerts = async { readCount(api.fetchAlertIds(schoolIdFilter = filter)) }
                    val audit = async { readCount(api.fetchAuditLogIds(schoolIdFilter = filter)) }

                    linkedMapOf(
                        "Schools" to schools.await().getOrThrow(),
                        "Branches" to branches.await().getOrThrow(),
                        "RBAC Grants" to access.await().getOrThrow(),
                        "Students" to students.await().getOrThrow(),
                        "Staff" to staff.await().getOrThrow(),
                        "Academics" to academics.await().getOrThrow(),
                        "Attendance" to attendance.await().getOrThrow(),
                        "Templates" to templates.await().getOrThrow(),
                        "Reports" to reports.await().getOrThrow(),
                        "Finance" to finance.await().getOrThrow(),
                        "Leaves" to leaves.await().getOrThrow(),
                        "Comms" to comms.await().getOrThrow(),
                        "Operations" to ops.await().getOrThrow(),
                        "Alerts" to alerts.await().getOrThrow(),
                        "Audit Logs" to audit.await().getOrThrow()
                    )
                }
            }
            val total = counts.values.sum()
            _state.update {
                it.copy(
                    isBusy = false,
                    connectionStatus = "Connected",
                    remoteCounts = counts,
                    lastSyncSummary = "Verified $total PostgreSQL rows across ${counts.size} tables",
                    lastError = null
                )
            }
            Result.success(counts)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase count refresh failed: ${e.javaClass.simpleName}")
            failState(e.localizedMessage ?: "Failed to query Supabase tables.")
        }
    }

    suspend fun pushWorkspace(
        workspace: GsmsWorkspaceState,
        ownerUid: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<String> {
        if (!canSyncSupabase(actorRole, actorExtra)) {
            return Result.failure(
                SecurityException("Access denied: ${actorRole.shortTitle} is not authorized to push tenant records to Supabase.")
            )
        }
        val api = apiService ?: return failState(config.validationError ?: "Supabase Secrets are not configured.")
        val activeSchool = workspace.activeSchool
            ?: return Result.failure(IllegalStateException("No active school tenant selected."))
        if (!SupabaseConfig.isValidSchoolId(activeSchool.id)) {
            return Result.failure(IllegalArgumentException("Invalid school identifier."))
        }
        if (accessToken.isNullOrBlank()) {
            return failState("Sign in to Supabase Auth first so PostgreSQL RLS policies allow tenant writes.")
        }

        _state.update { it.copy(isBusy = true, lastError = null) }
        return try {
            val start = System.currentTimeMillis()
            val schoolId = activeSchool.id
            val sbUid = supabaseUserId

            val summary = withContext(Dispatchers.IO) {
                val schoolsToSync = if (actorRole == PortalRole.SUPER_ADMIN && workspace.schools.isNotEmpty()) {
                    workspace.schools
                } else {
                    listOf(activeSchool)
                }
                ensureSuccess("gsms_schools", api.upsertSchools(schoolsToSync.map { it.toSupabaseRow(ownerUid, sbUid) }))

                if (workspace.branches.isNotEmpty()) {
                    ensureSuccess("gsms_branches", api.upsertBranches(workspace.branches.map { it.toSupabaseRow(ownerUid, schoolId) }))
                }
                if (workspace.accessAssignments.isNotEmpty()) {
                    ensureSuccess("gsms_user_access_assignments", api.upsertAccessAssignments(workspace.accessAssignments.map { it.toSupabaseRow(ownerUid, schoolId) }))
                }

                coroutineScope {
                    val tasks = listOf(
                        async { if (workspace.students.isNotEmpty()) ensureSuccess("gsms_students", api.upsertStudents(workspace.students.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.staff.isNotEmpty()) ensureSuccess("gsms_staff", api.upsertStaff(workspace.staff.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.academics.isNotEmpty()) ensureSuccess("gsms_academic_records", api.upsertAcademicRecords(workspace.academics.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.attendance.isNotEmpty()) ensureSuccess("gsms_attendance", api.upsertAttendance(workspace.attendance.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.reportTemplates.isNotEmpty()) ensureSuccess("gsms_report_templates", api.upsertReportTemplates(workspace.reportTemplates.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.reportCards.isNotEmpty()) ensureSuccess("gsms_report_cards", api.upsertReportCards(workspace.reportCards.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.finance.isNotEmpty()) ensureSuccess("gsms_finance_entries", api.upsertFinanceEntries(workspace.finance.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.leaveRequests.isNotEmpty()) ensureSuccess("gsms_leave_requests", api.upsertLeaveRequests(workspace.leaveRequests.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.communications.isNotEmpty()) ensureSuccess("gsms_communications", api.upsertCommunications(workspace.communications.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.operations.isNotEmpty()) ensureSuccess("gsms_operations", api.upsertOperations(workspace.operations.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.alerts.isNotEmpty()) ensureSuccess("gsms_early_warning_alerts", api.upsertAlerts(workspace.alerts.map { it.toSupabaseRow(ownerUid, schoolId) })) },
                        async { if (workspace.auditLogs.isNotEmpty()) ensureSuccess("gsms_audit_logs", api.insertAuditLogs(workspace.auditLogs.map { it.toSupabaseRow(ownerUid, schoolId) })) }
                    )
                    tasks.forEach { it.await() }
                }

                val syncedRows = schoolsToSync.size + workspace.branches.size + workspace.accessAssignments.size +
                    workspace.students.size + workspace.staff.size + workspace.academics.size +
                    workspace.attendance.size + workspace.reportTemplates.size + workspace.reportCards.size +
                    workspace.finance.size + workspace.leaveRequests.size + workspace.communications.size +
                    workspace.operations.size + workspace.alerts.size + workspace.auditLogs.size
                "Synced $syncedRows records for ${activeSchool.name} to Supabase PostgreSQL"
            }

            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            _state.update {
                it.copy(
                    isBusy = false,
                    connectionStatus = "Connected",
                    lastLatencyMs = elapsed,
                    lastSyncSummary = summary,
                    lastError = null
                )
            }
            pullSchoolCounts(schoolId)
            Result.success(summary)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase workspace push failed: ${e.javaClass.simpleName}")
            failState(e.localizedMessage ?: "Failed to sync tenant data to Supabase.")
        }
    }

    // --- Direct PostgREST Persistence Helpers (when logged in via Email) ---

    suspend fun saveSchool(school: School, ownerUid: String): Result<Unit> = runPostgrest("gsms_schools") { api ->
        ensureSuccess("gsms_schools", api.upsertSchools(listOf(school.toSupabaseRow(ownerUid, supabaseUserId))))
    }

    suspend fun saveBranch(branch: Branch, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_branches") { api ->
        ensureSuccess("gsms_branches", api.upsertBranches(listOf(branch.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveAccessAssignment(item: UserAccessAssignment, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_user_access_assignments") { api ->
        ensureSuccess("gsms_user_access_assignments", api.upsertAccessAssignments(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveStudent(item: Student, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_students") { api ->
        ensureSuccess("gsms_students", api.upsertStudents(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveAttendance(item: AttendanceRecord, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_attendance") { api ->
        ensureSuccess("gsms_attendance", api.upsertAttendance(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveAcademic(item: AcademicRecord, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_academic_records") { api ->
        ensureSuccess("gsms_academic_records", api.upsertAcademicRecords(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveTemplate(item: ReportTemplate, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_report_templates") { api ->
        ensureSuccess("gsms_report_templates", api.upsertReportTemplates(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveReportCard(item: ReportCard, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_report_cards") { api ->
        ensureSuccess("gsms_report_cards", api.upsertReportCards(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveLeaveRequest(item: LeaveRequest, staffUpdate: StaffMember?, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_leave_requests") { api ->
        ensureSuccess("gsms_leave_requests", api.upsertLeaveRequests(listOf(item.toSupabaseRow(ownerUid, schoolId))))
        if (staffUpdate != null) {
            ensureSuccess("gsms_staff", api.upsertStaff(listOf(staffUpdate.toSupabaseRow(ownerUid, schoolId))))
        }
    }

    suspend fun saveFinanceEntry(item: FinanceEntry, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_finance_entries") { api ->
        ensureSuccess("gsms_finance_entries", api.upsertFinanceEntries(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveCommunication(item: CommunicationItem, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_communications") { api ->
        ensureSuccess("gsms_communications", api.upsertCommunications(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveOperation(item: OperationRecord, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_operations") { api ->
        ensureSuccess("gsms_operations", api.upsertOperations(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun saveAlert(item: EarlyWarningAlert, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_early_warning_alerts") { api ->
        ensureSuccess("gsms_early_warning_alerts", api.upsertAlerts(listOf(item.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun appendAuditLog(log: AuditLog, ownerUid: String, schoolId: String): Result<Unit> = runPostgrest("gsms_audit_logs") { api ->
        ensureSuccess("gsms_audit_logs", api.insertAuditLogs(listOf(log.toSupabaseRow(ownerUid, schoolId))))
    }

    suspend fun generateAiText(prompt: String): Result<String> {
        val api = apiService ?: return failState(config.validationError ?: "Supabase is not configured.")
        if (accessToken.isNullOrBlank()) {
            return Result.failure(SecurityException("Sign in to Supabase before using GSMS AI."))
        }
        if (prompt.isBlank() || prompt.length > 24000) {
            return Result.failure(IllegalArgumentException("AI prompt must contain between 1 and 24,000 characters."))
        }
        return try {
            val response = withContext(Dispatchers.IO) {
                api.invokeGsmsAi(SupabaseAiRequest(prompt = prompt))
            }
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException(formatHttpError("GSMS AI", response.code(), response.errorBody())))
            } else {
                val generatedText = response.body()?.text?.takeIf { it.isNotBlank() }
                if (generatedText != null) {
                    Result.success(generatedText)
                } else {
                    Result.failure(IllegalStateException(response.body()?.error ?: "GSMS AI returned an empty response."))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase AI request failed: ${e.javaClass.simpleName}")
            Result.failure(IllegalStateException("GSMS AI request failed: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    private suspend fun runPostgrest(op: String, block: suspend (SupabaseApiService) -> Unit): Result<Unit> {
        val api = apiService ?: return Result.failure(IllegalStateException(config.validationError ?: "Supabase Secrets missing."))
        return try {
            withContext(Dispatchers.IO) { block(api) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "$op failed: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    private fun handleAuthResponse(
        action: String,
        email: String,
        fallbackName: String?,
        response: Response<SupabaseAuthResponse>
    ): Result<String> {
        if (!response.isSuccessful) {
            return failState(formatHttpError(action, response.code(), response.errorBody()))
        }
        val body = response.body()
        val token = body?.accessToken
        if (token.isNullOrBlank()) {
            val pendingMsg = "Verification email sent to $email. Confirm your email link, then sign in."
            _state.update { it.copy(isBusy = false, lastError = null, lastSyncSummary = pendingMsg) }
            return Result.success(pendingMsg)
        }
        val uid = body.user?.id?.takeIf { it.isNotBlank() } ?: email.lowercase()
        val verifiedEmail = body.user?.email ?: email
        val resolvedName = body.user?.userMetadata?.fullName?.takeIf { it.isNotBlank() }
            ?: fallbackName
            ?: verifiedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        accessToken = token
        supabaseUserId = uid
        body.refreshToken?.takeIf { it.isNotBlank() }?.let { refresh ->
            runCatching {
                sessionStore?.save(StoredSupabaseSession(refresh, verifiedEmail, resolvedName))
            }.onFailure { Log.w(TAG, "Could not persist encrypted Supabase session: ${it.javaClass.simpleName}") }
        }
        _sessionFlow.value = SupabaseSession(
            userId = uid,
            email = verifiedEmail,
            displayName = resolvedName
        )
        _state.update {
            it.copy(
                isBusy = false,
                isAuthenticated = true,
                authEmail = verifiedEmail,
                connectionStatus = "Authenticated",
                lastSyncSummary = "Signed in as $verifiedEmail",
                lastError = null
            )
        }
        return Result.success("Signed in as $verifiedEmail")
    }

    private fun <T> readList(
        table: String,
        response: Response<List<T>>
    ): List<T> {
        if (!response.isSuccessful) {
            throw IllegalStateException(formatHttpError("Load $table", response.code(), response.errorBody()))
        }
        return response.body().orEmpty()
    }

    private fun <T, R> readList(
        table: String,
        response: Response<List<T>>,
        mapper: (T) -> R
    ): List<R> = readList(table, response).map(mapper)

    private fun readCount(response: Response<List<SbIdOnlyRow>>): Result<Int> {
        if (!response.isSuccessful) {
            return Result.failure(
                IllegalStateException(formatHttpError("Table query", response.code(), response.errorBody()))
            )
        }
        return Result.success(response.body()?.size ?: 0)
    }

    private fun ensureSuccess(tableName: String, response: Response<ResponseBody>) {
        if (!response.isSuccessful) {
            throw IllegalStateException(formatHttpError("Sync $tableName", response.code(), response.errorBody()))
        }
    }

    private fun formatHttpError(operation: String, code: Int, errorBody: ResponseBody?): String {
        val raw = try {
            errorBody?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        val parsed = if (raw.isNotBlank()) {
            try {
                errorAdapter.fromJson(raw)
            } catch (_: Exception) {
                null
            }
        } else null

        val detail = parsed?.bestMessage()
        val pgCode = parsed?.code
        return when {
            code == 404 || pgCode == "PGRST205" ->
                "$operation failed: Supabase tables missing. Run supabase/schema.sql in your Supabase SQL Editor."
            code == 401 || code == 403 || pgCode == "42501" ->
                "$operation denied (HTTP $code): ${detail ?: "Sign in with a valid email account or check RLS ownership."}"
            detail != null -> "$operation failed (HTTP $code): $detail"
            else -> "$operation failed with HTTP $code."
        }
    }

    private fun <T> failState(message: String): Result<T> {
        _state.update { it.copy(isBusy = false, lastError = message) }
        return Result.failure(IllegalStateException(message))
    }

    companion object {
        private const val TAG = "SupabaseSyncRepository"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        @Volatile private var applicationContext: Context? = null

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }

        val shared: SupabaseSyncRepository by lazy {
            SupabaseSyncRepository(appContext = applicationContext)
        }

        fun isValidEmailFormat(email: String): Boolean {
            val trimmed = email.trim()
            return trimmed.length in 5..120 && EMAIL_REGEX.matches(trimmed)
        }

        fun canSyncSupabase(role: PortalRole, extra: Set<RoleCapability> = emptySet()): Boolean {
            return role == PortalRole.SUPER_ADMIN ||
                role == PortalRole.SCHOOL_ADMIN ||
                role == PortalRole.ICT_ADMIN ||
                role.has(RoleCapability.MANAGE_MODULES) ||
                RoleCapability.MANAGE_MODULES in extra ||
                role.has(RoleCapability.MANAGE_USER_PERMISSIONS) ||
                RoleCapability.MANAGE_USER_PERMISSIONS in extra
        }
    }
}
