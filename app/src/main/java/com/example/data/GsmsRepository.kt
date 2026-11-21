package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GsmsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
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
                .limit(1)
                .get()
                .await()
            if (existingSchools.isEmpty) {
                GsmsSeedData.seedInitialMultiTenantWorkspace(db, uid, displayName, email)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "schools")
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> handleStreamError(error, OperationType.GET, "users/$uid", uid) }

    fun observeSchools(uid: String): Flow<List<School>> =
        db.collection("schools")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(School::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> handleStreamError(error, OperationType.LIST, "schools", uid) }

    fun observeBranches(uid: String, schoolId: String): Flow<List<Branch>> =
        observeSchoolCollection(uid, schoolId, "branches", Branch::class.java)

    fun observeStudents(uid: String, schoolId: String): Flow<List<Student>> =
        observeSchoolCollection(uid, schoolId, "students", Student::class.java)

    fun observeStaff(uid: String, schoolId: String): Flow<List<StaffMember>> =
        observeSchoolCollection(uid, schoolId, "staff", StaffMember::class.java)

    fun observeAcademics(uid: String, schoolId: String): Flow<List<AcademicRecord>> =
        observeSchoolCollection(uid, schoolId, "academics", AcademicRecord::class.java)

    fun observeAttendance(uid: String, schoolId: String): Flow<List<AttendanceRecord>> =
        observeSchoolCollection(uid, schoolId, "attendance", AttendanceRecord::class.java)

    fun observeReportTemplates(uid: String, schoolId: String): Flow<List<ReportTemplate>> =
        observeSchoolCollection(uid, schoolId, "report_templates", ReportTemplate::class.java)

    fun observeReportCards(uid: String, schoolId: String): Flow<List<ReportCard>> =
        observeSchoolCollection(uid, schoolId, "report_cards", ReportCard::class.java)

    fun observeFinance(uid: String, schoolId: String): Flow<List<FinanceEntry>> =
        observeSchoolCollection(uid, schoolId, "finance", FinanceEntry::class.java)

    fun observeLeaveRequests(uid: String, schoolId: String): Flow<List<LeaveRequest>> =
        observeSchoolCollection(uid, schoolId, "leave_requests", LeaveRequest::class.java)

    fun observeCommunications(uid: String, schoolId: String): Flow<List<CommunicationItem>> =
        observeSchoolCollection(uid, schoolId, "communications", CommunicationItem::class.java)

    fun observeOperations(uid: String, schoolId: String): Flow<List<OperationRecord>> =
        observeSchoolCollection(uid, schoolId, "operations", OperationRecord::class.java)

    fun observeAlerts(uid: String, schoolId: String): Flow<List<EarlyWarningAlert>> =
        observeSchoolCollection(uid, schoolId, "alerts", EarlyWarningAlert::class.java)

    fun observeAuditLogs(uid: String, schoolId: String): Flow<List<AuditLog>> =
        observeSchoolCollection(uid, schoolId, "audit_logs", AuditLog::class.java)

    private fun <T> observeSchoolCollection(
        uid: String,
        schoolId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        if (schoolId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "schools/$schoolId/$collectionName"
        db.collection("schools").document(schoolId).collection(collectionName)
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(clazz, ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
            .collect { emit(it) }
    }

    private fun handleStreamError(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

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

    suspend fun createSchool(
        name: String,
        code: String,
        city: String,
        country: String,
        actorName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
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
            appendAuditLog(docRef.id, uid, actorName, PortalRole.SUPER_ADMIN.name, "SCHOOL_CREATED", "SYSTEM", "Created school tenant $name", false)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "schools")
            Result.failure(e)
        }
    }

    suspend fun toggleSchoolModule(
        school: School,
        module: GsmsModule,
        actorName: String,
        actorRole: PortalRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}"
        return try {
            val updatedModules = if (school.enabledModules.contains(module.name)) {
                school.enabledModules.filterNot { it == module.name }
            } else {
                (school.enabledModules + module.name).take(25)
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            db.collection("schools").document(schoolId).update(
                mapOf(
                    "activeAcademicYear" to academicYear.trim(),
                    "activeTerm" to term.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            appendAuditLog(schoolId, uid, actorName, PortalRole.SCHOOL_ADMIN.name, "ACADEMIC_PERIOD_UPDATED", "ACADEMICS", "Set active period to $academicYear ($term)", false)
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/students"
        return try {
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
            appendAuditLog(school.id, uid, actorName, PortalRole.ADMISSIONS_RECEPTION.name, "STUDENT_ADMITTED", "ADMISSIONS", "Enrolled $fullName ($admissionNo)", false)
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/attendance"
        return try {
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
            appendAuditLog(school.id, uid, actorName, PortalRole.TEACHER.name, "ATTENDANCE_MARKED", "ATTENDANCE", "Marked $targetName as $status", false)
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
        score: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/academics"
        return try {
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/report_templates"
        return try {
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
            appendAuditLog(school.id, uid, actorName, PortalRole.SCHOOL_ADMIN.name, "TEMPLATE_CREATED", "REPORT_CARDS", "Uploaded template $name", false)
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/report_cards"
        return try {
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
                PortalRole.TEACHER.name,
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
        reviewerRole: PortalRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/report_cards/${report.id}"
        return try {
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/leave_requests"
        return try {
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
            appendAuditLog(school.id, uid, actorName, PortalRole.TEACHER.name, "LEAVE_APPLIED", "LEAVE_MANAGEMENT", "${staff.fullName} requested $daysCount day(s) $leaveType leave", !aiConflictNote.isNullOrBlank())
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
        reviewerRole: PortalRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/leave_requests/${leave.id}"
        return try {
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
        actorName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/finance/${entry.id}"
        return try {
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
            appendAuditLog(schoolId, uid, actorName, PortalRole.FINANCE_OFFICER.name, "PAYMENT_RECORDED", "FINANCE", "Recorded USD ${"%.2f".format(additionalPaymentUsd)} for ${entry.referenceNo}", false)
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
        aiDrafted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/communications"
        return try {
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
        actorRole: PortalRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/${school.id}/operations"
        return try {
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
        actorRole: PortalRole
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "schools/$schoolId/alerts/${alert.id}"
        return try {
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

    companion object {
        private fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            try {
                instance.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: IllegalStateException) {
                // Settings already applied on existing instance
            }
            return instance
        }
    }
}
