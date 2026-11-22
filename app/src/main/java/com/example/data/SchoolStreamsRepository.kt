package com.example.data

import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Owns read-only Firestore snapshot streams for user and school-scoped records.
 * Authorization remains enforced by Firestore Security Rules; ownerId filters are
 * retained as query constraints and are not treated as a replacement for rules.
 */
internal class SchoolStreamsRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

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

    fun observeAccessAssignments(uid: String, schoolId: String): Flow<List<UserAccessAssignment>> =
        observeSchoolCollection(uid, schoolId, "access_assignments", UserAccessAssignment::class.java)

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

    private fun handleStreamError(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
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
}