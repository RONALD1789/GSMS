package com.example.data.supabase

import com.example.model.*

fun SbSchoolRow.toDomain(): School = School(
    id = id,
    ownerId = ownerId,
    name = name,
    code = code,
    country = country,
    city = city,
    plan = plan,
    subscriptionStatus = subscriptionStatus,
    brandColorHex = brandColorHex,
    activeAcademicYear = activeAcademicYear,
    activeTerm = activeTerm,
    enabledModules = enabledModules.ifEmpty { GsmsModule.defaultEnabledNames },
    studentCount = studentCount,
    staffCount = staffCount,
    monthlyRevenueUsd = monthlyRevenueUsd,
    churnRiskScore = churnRiskScore
)

fun SbBranchRow.toDomain(): Branch = Branch(
    id = id,
    schoolId = schoolId,
    ownerId = ownerId,
    name = name,
    code = code,
    city = city,
    mainCampus = mainCampus
)

fun SbAccessAssignmentRow.toDomain(): UserAccessAssignment = UserAccessAssignment(
    id = id,
    schoolId = schoolId,
    ownerId = ownerId,
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

fun SbParentStudentRow.toDomain(): Student = Student(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
    admissionNo = admissionNo,
    fullName = fullName,
    className = className,
    stream = stream,
    parentName = parentName,
    parentContact = null,
    attendanceRate = attendanceRate,
    averageScore = averageScore,
    feeBalanceUsd = feeBalanceUsd,
    riskLevel = "LOW",
    welfareStatus = "Not disclosed"
)

fun SbStudentRow.toDomain(): Student = Student(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
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

fun SbStaffRow.toDomain(): StaffMember = StaffMember(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
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

fun SbAcademicRow.toDomain(): AcademicRecord = AcademicRecord(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    academicYear = academicYear,
    term = term,
    ownerId = ownerId,
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

fun SbAttendanceRow.toDomain(): AttendanceRecord = AttendanceRecord(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    academicYear = academicYear,
    term = term,
    ownerId = ownerId,
    targetType = targetType,
    targetName = targetName,
    studentId = studentId,
    className = className,
    dateIso = dateIso,
    status = status
)

fun SbReportTemplateRow.toDomain(): ReportTemplate = ReportTemplate(
    id = id,
    schoolId = schoolId,
    ownerId = ownerId,
    name = name,
    classLevel = classLevel,
    sections = sections,
    gradingScale = gradingScale,
    active = active
)

fun SbReportCardRow.toDomain(): ReportCard = ReportCard(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    academicYear = academicYear,
    term = term,
    ownerId = ownerId,
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

fun SbFinanceRow.toDomain(): FinanceEntry = FinanceEntry(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    academicYear = academicYear,
    term = term,
    ownerId = ownerId,
    entryType = entryType,
    referenceNo = referenceNo,
    title = title,
    partyName = partyName,
    category = category,
    amountUsd = amountUsd,
    paidUsd = paidUsd,
    status = status,
    studentId = studentId
)

fun SbLeaveRow.toDomain(): LeaveRequest = LeaveRequest(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
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

fun SbCommRow.toDomain(): CommunicationItem = CommunicationItem(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
    channel = channel,
    audience = audience,
    title = title,
    body = body,
    senderName = senderName,
    senderRole = senderRole,
    aiDrafted = aiDrafted,
    targetClass = targetClass
)

fun SbOperationRow.toDomain(): OperationRecord = OperationRecord(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
    moduleType = moduleType,
    title = title,
    subtitle = subtitle,
    partyName = partyName,
    status = status,
    metricLabel = metricLabel,
    notes = notes
)

fun SbAlertRow.toDomain(): EarlyWarningAlert = EarlyWarningAlert(
    id = id,
    schoolId = schoolId,
    branchId = branchId,
    ownerId = ownerId,
    category = category,
    riskLevel = riskLevel,
    targetName = targetName,
    reason = reason,
    recommendedAction = recommendedAction,
    assignedRole = assignedRole,
    status = status
)

fun SbAuditLogRow.toDomain(): AuditLog = AuditLog(
    id = id,
    schoolId = schoolId,
    ownerId = ownerId,
    actorName = actorName,
    actorRole = actorRole,
    actionType = actionType,
    moduleName = moduleName,
    summary = summary,
    aiAction = aiAction
)
