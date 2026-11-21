package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val activeSchoolId: String? = null,
    val activeBranchId: String? = null,
    val activeRole: String = PortalRole.SCHOOL_ADMIN.name,
    val lowDataMode: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "email" to email,
        "activeSchoolId" to activeSchoolId,
        "activeBranchId" to activeBranchId,
        "activeRole" to activeRole,
        "lowDataMode" to lowDataMode,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class School(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val code: String = "",
    val country: String = "",
    val city: String = "",
    val plan: String = "ENTERPRISE",
    val subscriptionStatus: String = "ACTIVE",
    val brandColorHex: String = "#1D4ED8",
    val activeAcademicYear: String = "2026/2027",
    val activeTerm: String = "Term 1",
    val enabledModules: List<String> = GsmsModule.defaultEnabledNames,
    val studentCount: Long = 0L,
    val staffCount: Long = 0L,
    val monthlyRevenueUsd: Double = 0.0,
    val churnRiskScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasModule(module: GsmsModule): Boolean = enabledModules.contains(module.name)
}

@Keep
@IgnoreExtraProperties
data class Branch(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val code: String = "",
    val city: String = "",
    val mainCampus: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val admissionNo: String = "",
    val fullName: String = "",
    val className: String = "",
    val stream: String? = null,
    val parentName: String = "",
    val parentContact: String? = null,
    val attendanceRate: Double = 95.0,
    val averageScore: Double = 80.0,
    val feeBalanceUsd: Double = 0.0,
    val riskLevel: String = RiskLevel.LOW.name,
    val welfareStatus: String = "Cleared",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val staffNo: String = "",
    val fullName: String = "",
    val roleTitle: String = "",
    val department: String = "",
    val subjectsSummary: String? = null,
    val leaveBalanceDays: Long = 21L,
    val leaveUsedDays: Long = 0L,
    val onLeave: Boolean = false,
    val workloadHours: Long = 24L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AcademicRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val academicYear: String = "2026/2027",
    val term: String = "Term 1",
    val ownerId: String = "",
    val recordType: String = "TIMETABLE",
    val title: String = "",
    val className: String = "",
    val subjectName: String? = null,
    val teacherName: String? = null,
    val scheduleSlot: String? = null,
    val score: Double = 0.0,
    val maxScore: Double = 100.0,
    val grade: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val academicYear: String = "2026/2027",
    val term: String = "Term 1",
    val ownerId: String = "",
    val targetType: String = "STUDENT",
    val targetName: String = "",
    val className: String? = null,
    val dateIso: String = "2026-11-21",
    val status: String = "PRESENT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportTemplate(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val classLevel: String = "",
    val sections: List<String> = emptyList(),
    val gradingScale: String = "A-F Percentage",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportCard(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val academicYear: String = "2026/2027",
    val term: String = "Term 1",
    val ownerId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val templateId: String = "",
    val templateName: String = "",
    val subjectGradesSummary: String = "",
    val averageScore: Double = 0.0,
    val teacherRemarks: String? = null,
    val aiSummary: String? = null,
    val headteacherRemarks: String? = null,
    val status: String = ReportStatus.DRAFT.name,
    val version: Long = 1L,
    val submittedBy: String? = null,
    val reviewedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinanceEntry(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val academicYear: String = "2026/2027",
    val term: String = "Term 1",
    val ownerId: String = "",
    val entryType: String = "INVOICE",
    val referenceNo: String = "",
    val title: String = "",
    val partyName: String = "",
    val category: String = "Tuition",
    val amountUsd: Double = 0.0,
    val paidUsd: Double = 0.0,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaveRequest(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val roleTitle: String = "",
    val leaveType: String = "ANNUAL",
    val startDate: String = "",
    val endDate: String = "",
    val daysCount: Long = 1L,
    val reason: String = "",
    val attachmentNote: String? = null,
    val status: String = LeaveStatus.PENDING.name,
    val reviewedBy: String? = null,
    val aiConflictNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunicationItem(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val channel: String = "ANNOUNCEMENT",
    val audience: String = "All Parents & Staff",
    val title: String = "",
    val body: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val aiDrafted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OperationRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val moduleType: String = "EVENT",
    val title: String = "",
    val subtitle: String? = null,
    val partyName: String? = null,
    val status: String = "ACTIVE",
    val metricLabel: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EarlyWarningAlert(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val branchId: String = "",
    val ownerId: String = "",
    val category: String = "ACADEMIC",
    val riskLevel: String = RiskLevel.HIGH.name,
    val targetName: String = "",
    val reason: String = "",
    val recommendedAction: String = "",
    val assignedRole: String = PortalRole.HEADTEACHER.name,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val moduleName: String = "",
    val summary: String = "",
    val aiAction: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
