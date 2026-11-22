package com.example.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val primaryRole: String = PortalRole.SUPER_ADMIN.name,
    val activeSchoolId: String? = null,
    val activeBranchId: String? = null,
    val activeRole: String = PortalRole.SCHOOL_ADMIN.name,
    val assignedSchoolId: String? = null,
    val assignedBranchIds: List<String> = listOf("branch_main"),
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),
    val linkedStudentIds: List<String> = emptyList(),
    val extraPermissions: List<String> = emptyList(),
    val lowDataMode: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun resolvedPrimaryRole(): PortalRole =
        enumByNameOrDefault(primaryRole, PortalRole.SUPER_ADMIN)

    fun resolvedEffectiveRole(): PortalRole {
        val primary = resolvedPrimaryRole()
        return if (primary == PortalRole.SUPER_ADMIN) {
            enumByNameOrDefault(activeRole, PortalRole.SCHOOL_ADMIN)
        } else {
            primary
        }
    }

    fun resolvedExtraCapabilities(): Set<RoleCapability> =
        extraPermissions.mapNotNull { raw ->
            enumValues<RoleCapability>().firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }.toSet()

}

@Keep
data class UserAccessAssignment(
    val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val fullName: String = "",
    val email: String = "",
    val primaryRole: String = PortalRole.TEACHER.name,
    val assignedBranchIds: List<String> = listOf("branch_main"),
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),
    val linkedStudentIds: List<String> = emptyList(),
    val extraPermissions: List<String> = emptyList(),
    val active: Boolean = true,
    val updatedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun resolvedRole(): PortalRole = enumByNameOrDefault(primaryRole, PortalRole.TEACHER)

    fun resolvedExtraCapabilities(): Set<RoleCapability> =
        extraPermissions.mapNotNull { raw ->
            enumValues<RoleCapability>().firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }.toSet()
}

@Keep
data class School(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun hasModule(module: GsmsModule): Boolean = enabledModules.contains(module.name)
}

@Keep
data class Branch(
    val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val code: String = "",
    val city: String = "",
    val mainCampus: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class Student(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class StaffMember(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class AcademicRecord(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class AttendanceRecord(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val studentId: String? = null
)

@Keep
data class ReportTemplate(
    val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val classLevel: String = "",
    val sections: List<String> = emptyList(),
    val gradingScale: String = "A-F Percentage",
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class ReportCard(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class FinanceEntry(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val studentId: String? = null
)

@Keep
data class LeaveRequest(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class CommunicationItem(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val targetClass: String? = null
)

@Keep
data class OperationRecord(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class EarlyWarningAlert(
    val id: String = "",
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
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Keep
data class AuditLog(
    val id: String = "",
    val schoolId: String = "",
    val ownerId: String = "",
    val actorName: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val moduleName: String = "",
    val summary: String = "",
    val aiAction: Boolean = false,
    val createdAt: String? = null
)
