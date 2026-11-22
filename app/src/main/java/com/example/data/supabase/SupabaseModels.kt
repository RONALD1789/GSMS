package com.example.data.supabase

import androidx.annotation.Keep
import com.example.model.*
import com.squareup.moshi.Json

@Keep
data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

@Keep
data class SupabaseUserMetadata(
    @Json(name = "full_name") val fullName: String? = null
)

@Keep
data class SupabaseIdTokenRequest(
    val provider: String = "google",
    @Json(name = "id_token") val idToken: String
)

@Keep
data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: SupabaseUserMetadata? = null
)

@Keep
data class SupabaseRecoverRequest(
    val email: String
)

@Keep
data class SupabaseAuthUser(
    val id: String = "",
    val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: SupabaseUserMetadata? = null
)

@Keep
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: SupabaseAuthUser? = null
)

@Keep
data class SupabaseErrorBody(
    val message: String? = null,
    val msg: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    val hint: String? = null,
    val code: String? = null
) {
    fun bestMessage(): String? =
        listOfNotNull(message, msg, errorDescription, hint).firstOrNull { it.isNotBlank() }
}

@Keep
data class SbIdOnlyRow(
    val id: String = ""
)

@Keep
data class SbSchoolRow(
    val id: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "supabase_owner_uid") val supabaseOwnerUid: String? = null,
    val name: String,
    val code: String,
    val country: String,
    val city: String,
    val plan: String,
    @Json(name = "subscription_status") val subscriptionStatus: String,
    @Json(name = "brand_color_hex") val brandColorHex: String,
    @Json(name = "active_academic_year") val activeAcademicYear: String,
    @Json(name = "active_term") val activeTerm: String,
    @Json(name = "enabled_modules") val enabledModules: List<String>,
    @Json(name = "student_count") val studentCount: Long,
    @Json(name = "staff_count") val staffCount: Long,
    @Json(name = "monthly_revenue_usd") val monthlyRevenueUsd: Double,
    @Json(name = "churn_risk_score") val churnRiskScore: Long
)

@Keep
data class SbBranchRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "owner_id") val ownerId: String,
    val name: String,
    val code: String,
    val city: String,
    @Json(name = "main_campus") val mainCampus: Boolean
)

@Keep
data class SbAccessAssignmentRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    @Json(name = "primary_role") val primaryRole: String,
    @Json(name = "assigned_branch_ids") val assignedBranchIds: List<String>,
    @Json(name = "assigned_classes") val assignedClasses: List<String>,
    @Json(name = "assigned_subjects") val assignedSubjects: List<String>,
    @Json(name = "linked_student_ids") val linkedStudentIds: List<String>,
    @Json(name = "extra_permissions") val extraPermissions: List<String>,
    val active: Boolean,
    @Json(name = "updated_by") val updatedBy: String?
)

@Keep
data class SbStudentRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "admission_no") val admissionNo: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "class_name") val className: String,
    val stream: String?,
    @Json(name = "parent_name") val parentName: String,
    @Json(name = "parent_contact") val parentContact: String?,
    @Json(name = "attendance_rate") val attendanceRate: Double,
    @Json(name = "average_score") val averageScore: Double,
    @Json(name = "fee_balance_usd") val feeBalanceUsd: Double,
    @Json(name = "risk_level") val riskLevel: String,
    @Json(name = "welfare_status") val welfareStatus: String
)

@Keep
data class SbStaffRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "staff_no") val staffNo: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role_title") val roleTitle: String,
    val department: String,
    @Json(name = "subjects_summary") val subjectsSummary: String?,
    @Json(name = "leave_balance_days") val leaveBalanceDays: Long,
    @Json(name = "leave_used_days") val leaveUsedDays: Long,
    @Json(name = "on_leave") val onLeave: Boolean,
    @Json(name = "workload_hours") val workloadHours: Long
)

@Keep
data class SbAcademicRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "academic_year") val academicYear: String,
    val term: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "record_type") val recordType: String,
    val title: String,
    @Json(name = "class_name") val className: String,
    @Json(name = "subject_name") val subjectName: String?,
    @Json(name = "teacher_name") val teacherName: String?,
    @Json(name = "schedule_slot") val scheduleSlot: String?,
    val score: Double,
    @Json(name = "max_score") val maxScore: Double,
    val grade: String?
)

@Keep
data class SbAttendanceRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "academic_year") val academicYear: String,
    val term: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "target_type") val targetType: String,
    @Json(name = "target_name") val targetName: String,
    @Json(name = "class_name") val className: String?,
    @Json(name = "date_iso") val dateIso: String,
    val status: String
)

@Keep
data class SbReportTemplateRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "owner_id") val ownerId: String,
    val name: String,
    @Json(name = "class_level") val classLevel: String,
    val sections: List<String>,
    @Json(name = "grading_scale") val gradingScale: String,
    val active: Boolean
)

@Keep
data class SbReportCardRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "academic_year") val academicYear: String,
    val term: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "student_id") val studentId: String,
    @Json(name = "student_name") val studentName: String,
    @Json(name = "class_name") val className: String,
    @Json(name = "template_id") val templateId: String,
    @Json(name = "template_name") val templateName: String,
    @Json(name = "subject_grades_summary") val subjectGradesSummary: String,
    @Json(name = "average_score") val averageScore: Double,
    @Json(name = "teacher_remarks") val teacherRemarks: String?,
    @Json(name = "ai_summary") val aiSummary: String?,
    @Json(name = "headteacher_remarks") val headteacherRemarks: String?,
    val status: String,
    val version: Long,
    @Json(name = "submitted_by") val submittedBy: String?,
    @Json(name = "reviewed_by") val reviewedBy: String?
)

@Keep
data class SbFinanceRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "academic_year") val academicYear: String,
    val term: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "entry_type") val entryType: String,
    @Json(name = "reference_no") val referenceNo: String,
    val title: String,
    @Json(name = "party_name") val partyName: String,
    val category: String,
    @Json(name = "amount_usd") val amountUsd: Double,
    @Json(name = "paid_usd") val paidUsd: Double,
    val status: String
)

@Keep
data class SbLeaveRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "staff_id") val staffId: String,
    @Json(name = "staff_name") val staffName: String,
    @Json(name = "role_title") val roleTitle: String,
    @Json(name = "leave_type") val leaveType: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "days_count") val daysCount: Long,
    val reason: String,
    @Json(name = "attachment_note") val attachmentNote: String?,
    val status: String,
    @Json(name = "reviewed_by") val reviewedBy: String?,
    @Json(name = "ai_conflict_note") val aiConflictNote: String?
)

@Keep
data class SbCommRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    val channel: String,
    val audience: String,
    val title: String,
    val body: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_role") val senderRole: String,
    @Json(name = "ai_drafted") val aiDrafted: Boolean
)

@Keep
data class SbOperationRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "module_type") val moduleType: String,
    val title: String,
    val subtitle: String?,
    @Json(name = "party_name") val partyName: String?,
    val status: String,
    @Json(name = "metric_label") val metricLabel: String?,
    val notes: String?
)

@Keep
data class SbAlertRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "branch_id") val branchId: String,
    @Json(name = "owner_id") val ownerId: String,
    val category: String,
    @Json(name = "risk_level") val riskLevel: String,
    @Json(name = "target_name") val targetName: String,
    val reason: String,
    @Json(name = "recommended_action") val recommendedAction: String,
    @Json(name = "assigned_role") val assignedRole: String,
    val status: String
)

@Keep
data class SbAuditLogRow(
    val id: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "actor_name") val actorName: String,
    @Json(name = "actor_role") val actorRole: String,
    @Json(name = "action_type") val actionType: String,
    @Json(name = "module_name") val moduleName: String,
    val summary: String,
    @Json(name = "ai_action") val aiAction: Boolean
)

fun School.toSupabaseRow(fallbackOwnerUid: String, supabaseUserUid: String?): SbSchoolRow = SbSchoolRow(
    id = id,
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    supabaseOwnerUid = supabaseUserUid?.takeIf { it.isNotBlank() },
    name = name,
    code = code,
    country = country,
    city = city,
    plan = plan,
    subscriptionStatus = subscriptionStatus,
    brandColorHex = brandColorHex,
    activeAcademicYear = activeAcademicYear,
    activeTerm = activeTerm,
    enabledModules = enabledModules,
    studentCount = studentCount,
    staffCount = staffCount,
    monthlyRevenueUsd = monthlyRevenueUsd,
    churnRiskScore = churnRiskScore
)

fun Branch.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbBranchRow = SbBranchRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    name = name,
    code = code,
    city = city,
    mainCampus = mainCampus
)

fun UserAccessAssignment.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbAccessAssignmentRow = SbAccessAssignmentRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    fullName = fullName,
    email = email,
    primaryRole = primaryRole,
    assignedBranchIds = assignedBranchIds,
    assignedClasses = assignedClasses,
    assignedSubjects = assignedSubjects,
    linkedStudentIds = linkedStudentIds,
    extraPermissions = extraPermissions,
    active = active,
    updatedBy = updatedBy
)

fun Student.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbStudentRow = SbStudentRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    admissionNo = admissionNo,
    fullName = fullName,
    className = className,
    stream = stream,
    parentName = parentName,
    parentContact = parentContact,
    attendanceRate = attendanceRate,
    averageScore = averageScore,
    feeBalanceUsd = feeBalanceUsd,
    riskLevel = riskLevel,
    welfareStatus = welfareStatus
)

fun StaffMember.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbStaffRow = SbStaffRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    staffNo = staffNo,
    fullName = fullName,
    roleTitle = roleTitle,
    department = department,
    subjectsSummary = subjectsSummary,
    leaveBalanceDays = leaveBalanceDays,
    leaveUsedDays = leaveUsedDays,
    onLeave = onLeave,
    workloadHours = workloadHours
)

fun AcademicRecord.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbAcademicRow = SbAcademicRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    academicYear = academicYear,
    term = term,
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    recordType = recordType,
    title = title,
    className = className,
    subjectName = subjectName,
    teacherName = teacherName,
    scheduleSlot = scheduleSlot,
    score = score,
    maxScore = maxScore,
    grade = grade
)

fun AttendanceRecord.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbAttendanceRow = SbAttendanceRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    academicYear = academicYear,
    term = term,
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    targetType = targetType,
    targetName = targetName,
    className = className,
    dateIso = dateIso,
    status = status
)

fun ReportTemplate.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbReportTemplateRow = SbReportTemplateRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    name = name,
    classLevel = classLevel,
    sections = sections,
    gradingScale = gradingScale,
    active = active
)

fun ReportCard.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbReportCardRow = SbReportCardRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    academicYear = academicYear,
    term = term,
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    studentId = studentId,
    studentName = studentName,
    className = className,
    templateId = templateId,
    templateName = templateName,
    subjectGradesSummary = subjectGradesSummary,
    averageScore = averageScore,
    teacherRemarks = teacherRemarks,
    aiSummary = aiSummary,
    headteacherRemarks = headteacherRemarks,
    status = status,
    version = version,
    submittedBy = submittedBy,
    reviewedBy = reviewedBy
)

fun FinanceEntry.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbFinanceRow = SbFinanceRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    academicYear = academicYear,
    term = term,
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    entryType = entryType,
    referenceNo = referenceNo,
    title = title,
    partyName = partyName,
    category = category,
    amountUsd = amountUsd,
    paidUsd = paidUsd,
    status = status
)

fun LeaveRequest.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbLeaveRow = SbLeaveRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    staffId = staffId,
    staffName = staffName,
    roleTitle = roleTitle,
    leaveType = leaveType,
    startDate = startDate,
    endDate = endDate,
    daysCount = daysCount,
    reason = reason,
    attachmentNote = attachmentNote,
    status = status,
    reviewedBy = reviewedBy,
    aiConflictNote = aiConflictNote
)

fun CommunicationItem.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbCommRow = SbCommRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    channel = channel,
    audience = audience,
    title = title,
    body = body,
    senderName = senderName,
    senderRole = senderRole,
    aiDrafted = aiDrafted
)

fun OperationRecord.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbOperationRow = SbOperationRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    moduleType = moduleType,
    title = title,
    subtitle = subtitle,
    partyName = partyName,
    status = status,
    metricLabel = metricLabel,
    notes = notes
)

fun EarlyWarningAlert.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbAlertRow = SbAlertRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    branchId = branchId.ifBlank { "branch_main" },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    category = category,
    riskLevel = riskLevel,
    targetName = targetName,
    reason = reason,
    recommendedAction = recommendedAction,
    assignedRole = assignedRole,
    status = status
)

fun AuditLog.toSupabaseRow(fallbackOwnerUid: String, activeSchoolId: String): SbAuditLogRow = SbAuditLogRow(
    id = id,
    schoolId = schoolId.ifBlank { activeSchoolId },
    ownerId = ownerId.ifBlank { fallbackOwnerUid },
    actorName = actorName,
    actorRole = actorRole,
    actionType = actionType,
    moduleName = moduleName,
    summary = summary,
    aiAction = aiAction
)
