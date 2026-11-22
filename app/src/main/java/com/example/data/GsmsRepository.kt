package com.example.data

import android.content.Context
import com.example.data.firebase.FirestoreProvider
import com.example.data.seed.GsmsSeedData
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GsmsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirestoreProvider.create(context.applicationContext)
    )

    private val auth = Firebase.auth
    private val streams = SchoolStreamsRepository(db)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireActorCapability(
        actorRole: PortalRole,
        capability: RoleCapability,
        extra: Set<RoleCapability>,
        actionLabel: String
    ) {
        if (!actorRole.has(capability, extra)) {
            throw SecurityException("RBAC denied: ${actorRole.shortTitle} is not authorized to $actionLabel.")
        }
    }

    suspend fun ensureWorkspaceInitialized(
        uid: String,
        displayName: String,
        email: String?
    ): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Authenticated UID mismatch." }
            val existingSchools = db.collection("schools")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
            if (existingSchools.isEmpty) {
                GsmsSeedData.seedInitialMultiTenantWorkspace(db, uid, displayName, email)
            } else {
                // Backfill legacy user profiles & access_assignments cleanly
                val firstSchoolId = existingSchools.documents.first().id
                val userDoc = db.collection("users").document(uid).get().await()
                if (userDoc.exists() && !userDoc.contains("primaryRole")) {
                    db.collection("users").document(uid).update(
                        mapOf(
                            "primaryRole" to PortalRole.SUPER_ADMIN.name,
                            "assignedSchoolId" to (userDoc.getString("activeSchoolId") ?: firstSchoolId),
                            "assignedBranchIds" to listOf("branch_main", "branch_north"),
                            "assignedClasses" to listOf("Grade 10A"),
                            "assignedSubjects" to listOf("Mathematics", "Physics"),
                            "linkedStudentIds" to listOf("stu_1"),
                            "extraPermissions" to emptyList<String>(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                val assignSnap = db.collection("schools").document(firstSchoolId)
                    .collection("access_assignments")
                    .whereEqualTo("ownerId", uid)
                    .limit(1)
                    .get()
                    .await()
                if (assignSnap.isEmpty) {
                    GsmsSeedData.seedAccessAssignmentsForSchool(db, uid, firstSchoolId)
                }
                // Ensure newly added modules (PROCUREMENT, ICT_SYSTEMS) exist on existing primary school
                val firstModules = existingSchools.documents.first().get("enabledModules") as? List<*>
                if (firstModules != null && (!firstModules.contains(GsmsModule.PROCUREMENT.name) || !firstModules.contains(GsmsModule.ICT_SYSTEMS.name))) {
                    val updated = (firstModules.filterIsInstance<String>() + listOf(GsmsModule.PROCUREMENT.name, GsmsModule.ICT_SYSTEMS.name)).distinct()
                    db.collection("schools").document(firstSchoolId).update(
                        mapOf("enabledModules" to updated, "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "schools")
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = streams.observeUserProfile(uid)
    fun observeSchools(uid: String): Flow<List<School>> = streams.observeSchools(uid)
    fun observeAccessAssignments(uid: String, schoolId: String): Flow<List<UserAccessAssignment>> = streams.observeAccessAssignments(uid, schoolId)
    fun observeBranches(uid: String, schoolId: String): Flow<List<Branch>> = streams.observeBranches(uid, schoolId)
    fun observeStudents(uid: String, schoolId: String): Flow<List<Student>> = streams.observeStudents(uid, schoolId)
    fun observeStaff(uid: String, schoolId: String): Flow<List<StaffMember>> = streams.observeStaff(uid, schoolId)
    fun observeAcademics(uid: String, schoolId: String): Flow<List<AcademicRecord>> = streams.observeAcademics(uid, schoolId)
    fun observeAttendance(uid: String, schoolId: String): Flow<List<AttendanceRecord>> = streams.observeAttendance(uid, schoolId)
    fun observeReportTemplates(uid: String, schoolId: String): Flow<List<ReportTemplate>> = streams.observeReportTemplates(uid, schoolId)
    fun observeReportCards(uid: String, schoolId: String): Flow<List<ReportCard>> = streams.observeReportCards(uid, schoolId)
    fun observeFinance(uid: String, schoolId: String): Flow<List<FinanceEntry>> = streams.observeFinance(uid, schoolId)
    fun observeLeaveRequests(uid: String, schoolId: String): Flow<List<LeaveRequest>> = streams.observeLeaveRequests(uid, schoolId)
    fun observeCommunications(uid: String, schoolId: String): Flow<List<CommunicationItem>> = streams.observeCommunications(uid, schoolId)
    fun observeOperations(uid: String, schoolId: String): Flow<List<OperationRecord>> = streams.observeOperations(uid, schoolId)
    fun observeAlerts(uid: String, schoolId: String): Flow<List<EarlyWarningAlert>> = streams.observeAlerts(uid, schoolId)
    fun observeAuditLogs(uid: String, schoolId: String): Flow<List<AuditLog>> = streams.observeAuditLogs(uid, schoolId)

    // --- USER & TENANT GOVERNANCE MUTATIONS ---

    suspend fun updateUserSessionPreferences(
        role: PortalRole? = null,
        schoolId: String? = null,
        branchId: String? = null,
        lowDataMode: Boolean? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (role != null) updates["activeRole"] = role.name
            if (!schoolId.isNullOrBlank()) updates["activeSchoolId"] = schoolId
            if (!branchId.isNullOrBlank()) updates["activeBranchId"] = branchId
            if (lowDataMode != null) updates["lowDataMode"] = lowDataMode
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserScopePreferences(
        assignedClasses: List<String>? = null,
        assignedSubjects: List<String>? = null,
        linkedStudentIds: List<String>? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (assignedClasses != null) updates["assignedClasses"] = assignedClasses
            if (assignedSubjects != null) updates["assignedSubjects"] = assignedSubjects
            if (linkedStudentIds != null) updates["linkedStudentIds"] = linkedStudentIds
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun delegateUserPermissions(
        schoolId: String,
        assignment: UserAccessAssignment,
        newExtraPermissions: List<String>,
        newAssignedClasses: List<String>,
        newAssignedSubjects: List<String>,
        newLinkedStudentIds: List<String>,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/access_assignments/${assignment.id}"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_USER_PERMISSIONS, actorExtra, "delegate user permissions")
            // Primary role is NEVER altered by permission delegation
            db.collection("schools").document(schoolId)
                .collection("access_assignments").document(assignment.id)
                .update(
                    mapOf(
                        "extraPermissions" to newExtraPermissions.distinct().take(45),
                        "assignedClasses" to newAssignedClasses.distinct().take(30),
                        "assignedSubjects" to newAssignedSubjects.distinct().take(30),
                        "linkedStudentIds" to newLinkedStudentIds.distinct().take(20),
                        "updatedBy" to actorName.take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            appendAuditLog(
                schoolId = schoolId,
                uid = uid,
                actorName = actorName,
                actorRole = actorRole.name,
                actionType = "PERMISSIONS_DELEGATED",
                moduleName = "ICT_SYSTEMS",
                summary = "Updated delegated permissions (${newExtraPermissions.size}) for ${assignment.fullName} without altering primaryRole (${assignment.primaryRole})",
                aiAction = false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
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
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/access_assignments"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_USER_PERMISSIONS, actorExtra, "create user access assignment")
            require(primaryRole != PortalRole.SUPER_ADMIN) { "Cannot assign SUPER_ADMIN at school level." }
            require(fullName.trim().length in 2..100) { "Full name must be 2–100 characters." }
            require(email.trim().length in 3..120) { "Valid email is required." }
            db.collection("schools").document(schoolId)
                .collection("access_assignments").document()
                .set(
                    mapOf(
                        "schoolId" to schoolId,
                        "ownerId" to uid,
                        "fullName" to fullName.trim(),
                        "email" to email.trim(),
                        "primaryRole" to primaryRole.name,
                        "assignedBranchIds" to listOf("branch_main"),
                        "assignedClasses" to assignedClasses.distinct().take(30),
                        "assignedSubjects" to assignedSubjects.distinct().take(30),
                        "linkedStudentIds" to linkedStudentIds.distinct().take(20),
                        "extraPermissions" to extraPermissions.distinct().take(45),
                        "active" to true,
                        "updatedBy" to actorName.take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            appendAuditLog(
                schoolId, uid, actorName, actorRole.name,
                "USER_ACCESS_PROVISIONED", "ICT_SYSTEMS",
                "Provisioned ${primaryRole.shortTitle} access for $fullName with ${extraPermissions.size} extra permissions",
                false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSchool(
        name: String,
        code: String,
        city: String,
        country: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            requireActorCapability(actorRole, RoleCapability.CREATE_SCHOOL_TENANT, actorExtra, "provision new schools")
            require(name.trim().length in 2..120) { "School name must be 2–120 characters." }
            require(code.trim().length in 2..24) { "School code must be 2–24 characters." }
            val docRef = db.collection("schools").document()
            docRef.set(
                mapOf(
                    "ownerId" to uid,
                    "name" to name.trim(),
                    "code" to code.trim().uppercase(),
                    "country" to country.trim().ifBlank { "Global" },
                    "city" to city.trim().ifBlank { "Main City" },
                    "plan" to "ENTERPRISE",
                    "subscriptionStatus" to "ACTIVE",
                    "brandColorHex" to "#1D4ED8",
                    "activeAcademicYear" to "2026/2027",
                    "activeTerm" to "Term 1",
                    "enabledModules" to GsmsModule.defaultEnabledNames,
                    "studentCount" to 0L,
                    "staffCount" to 0L,
                    "monthlyRevenueUsd" to 2500.0,
                    "churnRiskScore" to 5L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            docRef.collection("branches").document("branch_main").set(
                mapOf(
                    "schoolId" to docRef.id,
                    "ownerId" to uid,
                    "name" to "Main Campus",
                    "code" to "BR-01",
                    "city" to city.trim().ifBlank { "Main City" },
                    "mainCampus" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(docRef.id, uid, actorName, actorRole.name, "SCHOOL_CREATED", "SYSTEM", "Created school tenant $name", false)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "schools")
            Result.failure(e)
        }
    }

    suspend fun createBranch(
        schoolId: String,
        name: String,
        code: String,
        city: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/branches"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_BRANCHES, actorExtra, "create campus branches")
            require(name.trim().length in 2..100) { "Branch name must be 2–100 characters." }
            require(code.trim().length in 2..24) { "Branch code must be 2–24 characters." }
            db.collection("schools").document(schoolId).collection("branches").document().set(
                mapOf(
                    "schoolId" to schoolId,
                    "ownerId" to uid,
                    "name" to name.trim(),
                    "code" to code.trim().uppercase(),
                    "city" to city.trim().ifBlank { "Campus City" },
                    "mainCampus" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(schoolId, uid, actorName, actorRole.name, "BRANCH_CREATED", "SYSTEM", "Added campus branch $name ($code)", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleSchoolModule(
        school: School,
        module: GsmsModule,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_MODULES, actorExtra, "toggle school modules")
            val updatedModules = if (school.enabledModules.contains(module.name)) {
                school.enabledModules.filterNot { it == module.name }
            } else {
                (school.enabledModules + module.name).take(30)
            }
            db.collection("schools").document(school.id).update(
                mapOf(
                    "enabledModules" to updatedModules,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val stateWord = if (updatedModules.contains(module.name)) "Enabled" else "Disabled"
            appendAuditLog(
                school.id,
                uid,
                actorName,
                actorRole.name,
                "MODULE_TOGGLED",
                module.name,
                "$stateWord module ${module.label} for ${school.name}",
                false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolPeriod(
        schoolId: String,
        academicYear: String,
        term: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            requireActorCapability(actorRole, RoleCapability.UPDATE_ACADEMIC_PERIOD, actorExtra, "update academic period")
            db.collection("schools").document(schoolId).update(
                mapOf(
                    "activeAcademicYear" to academicYear.trim(),
                    "activeTerm" to term.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(schoolId, uid, actorName, actorRole.name, "ACADEMIC_PERIOD_UPDATED", "ACADEMICS", "Set active period to $academicYear ($term)", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "schools/$schoolId")
            Result.failure(e)
        }
    }

    // --- STUDENTS, ACADEMICS & ATTENDANCE ---

    suspend fun addStudent(
        school: School,
        branchId: String,
        admissionNo: String,
        fullName: String,
        className: String,
        parentName: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/students"
        return try {
            requireActorCapability(actorRole, RoleCapability.ENROLL_STUDENT, actorExtra, "enroll students")
            require(fullName.trim().length in 2..100) { "Student name must be 2–100 characters." }
            require(admissionNo.trim().length in 2..32) { "Admission number must be 2–32 characters." }
            db.collection("schools").document(school.id).collection("students").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "branchId" to branchId,
                    "ownerId" to uid,
                    "admissionNo" to admissionNo.trim(),
                    "fullName" to fullName.trim(),
                    "className" to className.trim().ifBlank { "Grade 10A" },
                    "stream" to "General",
                    "parentName" to parentName.trim().ifBlank { "Guardian" },
                    "attendanceRate" to 100.0,
                    "averageScore" to 85.0,
                    "feeBalanceUsd" to 1200.0,
                    "riskLevel" to RiskLevel.LOW.name,
                    "welfareStatus" to "Cleared",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("schools").document(school.id).update(
                mapOf(
                    "studentCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(school.id, uid, actorName, actorRole.name, "STUDENT_ADMITTED", "ADMISSIONS", "Enrolled $fullName ($admissionNo)", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordAttendance(
        school: School,
        branchId: String,
        targetName: String,
        className: String,
        status: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/attendance"
        return try {
            requireActorCapability(actorRole, RoleCapability.MARK_ATTENDANCE, actorExtra, "mark attendance")
            db.collection("schools").document(school.id).collection("attendance").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "branchId" to branchId,
                    "academicYear" to school.activeAcademicYear,
                    "term" to school.activeTerm,
                    "ownerId" to uid,
                    "targetType" to "STUDENT",
                    "targetName" to targetName.trim(),
                    "className" to className.trim().ifBlank { "Grade 10A" },
                    "dateIso" to "2026-11-21",
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(school.id, uid, actorName, actorRole.name, "ATTENDANCE_MARKED", "ATTENDANCE", "Marked $targetName as $status", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
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
        score: Double,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/academics"
        return try {
            requireActorCapability(actorRole, RoleCapability.EDIT_ACADEMICS, actorExtra, "add academic entries")
            require(title.trim().length in 1..140) { "Title is required." }
            val grade = when {
                score >= 90 -> "A"
                score >= 80 -> "B"
                score >= 70 -> "C"
                score >= 60 -> "D"
                else -> "F"
            }
            db.collection("schools").document(school.id).collection("academics").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "branchId" to branchId,
                    "academicYear" to school.activeAcademicYear,
                    "term" to school.activeTerm,
                    "ownerId" to uid,
                    "recordType" to recordType,
                    "title" to title.trim(),
                    "className" to className.trim().ifBlank { "Grade 10A" },
                    "subjectName" to subjectName.trim().ifBlank { "General" },
                    "teacherName" to teacherName.trim().ifBlank { "Faculty" },
                    "scheduleSlot" to scheduleSlot.trim().ifBlank { "Term 1" },
                    "score" to score.coerceIn(0.0, 1000.0),
                    "maxScore" to 100.0,
                    "grade" to grade,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- REPORT TEMPLATE & APPROVAL WORKFLOW ---

    suspend fun createReportTemplate(
        school: School,
        name: String,
        classLevel: String,
        sections: List<String>,
        gradingScale: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/report_templates"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_REPORT_TEMPLATES, actorExtra, "create report templates")
            require(name.trim().length in 2..100) { "Template name must be 2–100 characters." }
            db.collection("schools").document(school.id).collection("report_templates").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "ownerId" to uid,
                    "name" to name.trim(),
                    "classLevel" to classLevel.trim().ifBlank { "All Classes" },
                    "sections" to sections.take(15),
                    "gradingScale" to gradingScale.trim().ifBlank { "A-F Percentage" },
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(school.id, uid, actorName, actorRole.name, "TEMPLATE_CREATED", "REPORT_CARDS", "Uploaded template $name", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrSubmitReportCard(
        school: School,
        branchId: String,
        student: Student,
        template: ReportTemplate,
        subjectGradesSummary: String,
        averageScore: Double,
        teacherRemarks: String,
        aiSummary: String?,
        submitNow: Boolean,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/report_cards"
        return try {
            requireActorCapability(actorRole, RoleCapability.FILL_SUBMIT_REPORT_CARDS, actorExtra, "fill or submit report cards")
            val status = if (submitNow) ReportStatus.SUBMITTED.name else ReportStatus.DRAFT.name
            val payload = mutableMapOf<String, Any>(
                "schoolId" to school.id,
                "branchId" to branchId,
                "academicYear" to school.activeAcademicYear,
                "term" to school.activeTerm,
                "ownerId" to uid,
                "studentId" to student.id,
                "studentName" to student.fullName,
                "className" to student.className,
                "templateId" to template.id,
                "templateName" to template.name,
                "subjectGradesSummary" to subjectGradesSummary.trim().take(1200),
                "averageScore" to averageScore.coerceIn(0.0, 100.0),
                "teacherRemarks" to teacherRemarks.trim().take(800),
                "status" to status,
                "version" to 1L,
                "submittedBy" to actorName.take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!aiSummary.isNullOrBlank()) {
                payload["aiSummary"] = aiSummary.trim().take(1200)
            }
            db.collection("schools").document(school.id).collection("report_cards").document().set(payload).await()
            appendAuditLog(
                school.id,
                uid,
                actorName,
                actorRole.name,
                if (submitNow) "REPORT_SUBMITTED" else "REPORT_SAVED",
                "REPORT_CARDS",
                "${if (submitNow) "Submitted" else "Saved draft"} report card for ${student.fullName} (v1)",
                !aiSummary.isNullOrBlank()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun transitionReportCard(
        schoolId: String,
        report: ReportCard,
        nextStatus: ReportStatus,
        headteacherRemarks: String?,
        reviewerName: String,
        reviewerRole: PortalRole,
        reviewerExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/report_cards/${report.id}"
        return try {
            val required = if (nextStatus == ReportStatus.SUBMITTED) {
                RoleCapability.FILL_SUBMIT_REPORT_CARDS
            } else {
                RoleCapability.APPROVE_PUBLISH_REPORT_CARDS
            }
            requireActorCapability(reviewerRole, required, reviewerExtra, "move report cards to ${nextStatus.label}")
            val updates = mutableMapOf<String, Any>(
                "status" to nextStatus.name,
                "version" to (report.version + 1L),
                "reviewedBy" to reviewerName.take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!headteacherRemarks.isNullOrBlank()) {
                updates["headteacherRemarks"] = headteacherRemarks.trim().take(800)
            }
            db.collection("schools").document(schoolId).collection("report_cards")
                .document(report.id).update(updates).await()
            appendAuditLog(
                schoolId,
                uid,
                reviewerName,
                reviewerRole.name,
                "REPORT_${nextStatus.name}",
                "REPORT_CARDS",
                "Moved ${report.studentName} report card to ${nextStatus.label} (v${report.version + 1})",
                false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- LEAVE MANAGEMENT WORKFLOW ---

    suspend fun submitLeaveRequest(
        school: School,
        branchId: String,
        staff: StaffMember,
        leaveType: String,
        startDate: String,
        endDate: String,
        daysCount: Long,
        reason: String,
        aiConflictNote: String?,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/leave_requests"
        return try {
            requireActorCapability(actorRole, RoleCapability.APPLY_LEAVE, actorExtra, "apply for leave")
            require(reason.trim().length in 2..500) { "Please provide a leave reason (2–500 chars)." }
            val payload = mutableMapOf<String, Any>(
                "schoolId" to school.id,
                "branchId" to branchId,
                "ownerId" to uid,
                "staffId" to staff.id,
                "staffName" to staff.fullName,
                "roleTitle" to staff.roleTitle,
                "leaveType" to leaveType,
                "startDate" to startDate.trim(),
                "endDate" to endDate.trim(),
                "daysCount" to daysCount.coerceIn(1L, 365L),
                "reason" to reason.trim(),
                "status" to LeaveStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!aiConflictNote.isNullOrBlank()) {
                payload["aiConflictNote"] = aiConflictNote.trim().take(600)
            }
            db.collection("schools").document(school.id).collection("leave_requests").document().set(payload).await()
            appendAuditLog(school.id, uid, actorName, actorRole.name, "LEAVE_APPLIED", "LEAVE_MANAGEMENT", "${staff.fullName} requested $daysCount day(s) $leaveType leave", !aiConflictNote.isNullOrBlank())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewLeaveRequest(
        schoolId: String,
        leave: LeaveRequest,
        staffMember: StaffMember?,
        approve: Boolean,
        reviewerName: String,
        reviewerRole: PortalRole,
        reviewerExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/leave_requests/${leave.id}"
        return try {
            requireActorCapability(reviewerRole, RoleCapability.APPROVE_LEAVE, reviewerExtra, "approve or reject leave requests")
            val nextStatus = if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED
            val batch = db.batch()
            val leaveRef = db.collection("schools").document(schoolId)
                .collection("leave_requests").document(leave.id)
            batch.update(
                leaveRef,
                mapOf(
                    "status" to nextStatus.name,
                    "reviewedBy" to reviewerName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve && staffMember != null) {
                val staffRef = db.collection("schools").document(schoolId)
                    .collection("staff").document(staffMember.id)
                val newBalance = (staffMember.leaveBalanceDays - leave.daysCount).coerceAtLeast(0L)
                val newUsed = (staffMember.leaveUsedDays + leave.daysCount).coerceAtMost(365L)
                batch.update(
                    staffRef,
                    mapOf(
                        "onLeave" to true,
                        "leaveBalanceDays" to newBalance,
                        "leaveUsedDays" to newUsed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            appendAuditLog(
                schoolId,
                uid,
                reviewerName,
                reviewerRole.name,
                "LEAVE_${nextStatus.name}",
                "LEAVE_MANAGEMENT",
                "${nextStatus.label} ${leave.daysCount} day(s) leave for ${leave.staffName}",
                false
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FINANCE, COMMUNICATIONS, OPERATIONS & ALERTS ---

    suspend fun recordFinancePayment(
        schoolId: String,
        entry: FinanceEntry,
        additionalPaymentUsd: Double,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/finance/${entry.id}"
        return try {
            requireActorCapability(actorRole, RoleCapability.RECORD_FINANCE_PAYMENT, actorExtra, "record fee payments")
            val newPaid = (entry.paidUsd + additionalPaymentUsd).coerceAtMost(entry.amountUsd)
            val nextStatus = if (newPaid >= entry.amountUsd) "PAID" else "PARTIAL"
            db.collection("schools").document(schoolId).collection("finance").document(entry.id)
                .update(
                    mapOf(
                        "paidUsd" to newPaid,
                        "status" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            appendAuditLog(schoolId, uid, actorName, actorRole.name, "PAYMENT_RECORDED", "FINANCE", "Recorded USD ${"%.2f".format(additionalPaymentUsd)} for ${entry.referenceNo}", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendCommunication(
        school: School,
        branchId: String,
        channel: String,
        audience: String,
        title: String,
        body: String,
        senderName: String,
        senderRole: PortalRole,
        senderExtra: Set<RoleCapability>,
        aiDrafted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/communications"
        return try {
            requireActorCapability(senderRole, RoleCapability.COMPOSE_COMMUNICATIONS, senderExtra, "compose communications")
            require(title.trim().length in 1..140) { "Message title required." }
            require(body.trim().length in 1..2000) { "Message body required." }
            db.collection("schools").document(school.id).collection("communications").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "branchId" to branchId,
                    "ownerId" to uid,
                    "channel" to channel,
                    "audience" to audience.trim().ifBlank { "All Users" },
                    "title" to title.trim(),
                    "body" to body.trim(),
                    "senderName" to senderName.take(100),
                    "senderRole" to senderRole.name,
                    "aiDrafted" to aiDrafted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(school.id, uid, senderName, senderRole.name, "MESSAGE_DISPATCHED", "COMMUNICATION", "Sent $channel notice: $title", aiDrafted)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
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
        notes: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/operations"
        return try {
            val sectionName = when (moduleType) {
                "LIBRARY_BOOK", "LIBRARY_BORROW" -> "Library"
                "VEHICLE", "ROUTE", "TRIP" -> "Transport"
                "WELFARE" -> "Welfare"
                "COUNSELLING" -> "Counselling"
                "DISCIPLINE" -> "Discipline"
                "PROCUREMENT" -> "Procurement"
                "INVENTORY", "ASSET", "STOCK_MOVEMENT" -> "Stores"
                "HOSTEL" -> "Hostel"
                "FRONT_DESK" -> "Front Desk"
                "ADMISSION" -> "Admissions"
                "ICT_TICKET" -> "ICT Systems"
                else -> "Events"
            }
            val allowedBySection = sectionName in RolePolicy.operationSections(actorRole, school, actorExtra)
            val hasEditCap = actorRole.has(RoleCapability.EDIT_OPERATIONS, actorExtra) ||
                actorRole.has(RoleCapability.MANAGE_PROCUREMENT, actorExtra) ||
                actorRole.has(RoleCapability.MANAGE_STORES, actorExtra) ||
                actorRole.has(RoleCapability.MANAGE_COUNSELLING, actorExtra) ||
                actorRole.has(RoleCapability.MANAGE_FRONT_DESK, actorExtra) ||
                actorRole.has(RoleCapability.MANAGE_ICT_SYSTEMS, actorExtra)
            if (!allowedBySection || !hasEditCap) {
                throw SecurityException("RBAC denied: ${actorRole.shortTitle} cannot add records in $sectionName.")
            }
            require(title.trim().length in 1..140) { "Title required." }
            db.collection("schools").document(school.id).collection("operations").document().set(
                mapOf(
                    "schoolId" to school.id,
                    "branchId" to branchId,
                    "ownerId" to uid,
                    "moduleType" to moduleType,
                    "title" to title.trim(),
                    "subtitle" to subtitle.trim().take(200),
                    "partyName" to partyName.trim().take(100),
                    "status" to status.trim().ifBlank { "ACTIVE" },
                    "metricLabel" to metricLabel.trim().take(80),
                    "notes" to notes.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(school.id, uid, actorName, actorRole.name, "OPERATION_LOGGED", moduleType, "Added $moduleType record: $title", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAlertStatus(
        schoolId: String,
        alert: EarlyWarningAlert,
        newStatus: String,
        actorName: String,
        actorRole: PortalRole,
        actorExtra: Set<RoleCapability>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/alerts/${alert.id}"
        return try {
            requireActorCapability(actorRole, RoleCapability.MANAGE_ALERTS, actorExtra, "update early-warning alerts")
            db.collection("schools").document(schoolId).collection("alerts").document(alert.id)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            appendAuditLog(schoolId, uid, actorName, actorRole.name, "ALERT_UPDATED", "ANALYTICS", "Updated alert for ${alert.targetName} -> $newStatus", false)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logAiAudit(
        schoolId: String,
        actorName: String,
        actorRole: PortalRole,
        actionType: String,
        summary: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        try {
            appendAuditLog(schoolId, uid, actorName, actorRole.name, actionType, "AI_TOOLS", summary.take(380), true)
        } catch (_: Exception) {
            // Non-blocking audit helper
        }
    }

    private suspend fun appendAuditLog(
        schoolId: String,
        uid: String,
        actorName: String,
        actorRole: String,
        actionType: String,
        moduleName: String,
        summary: String,
        aiAction: Boolean
    ) {
        db.collection("schools").document(schoolId).collection("audit_logs").document().set(
            mapOf(
                "schoolId" to schoolId,
                "ownerId" to uid,
                "actorName" to actorName.ifBlank { "GSMS User" }.take(100),
                "actorRole" to actorRole.take(60),
                "actionType" to actionType.take(60),
                "moduleName" to moduleName.take(60),
                "summary" to summary.take(400),
                "aiAction" to aiAction,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

}
