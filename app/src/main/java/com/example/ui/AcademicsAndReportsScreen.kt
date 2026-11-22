package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PdfExportHelper
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun AcademicsAndReportsScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    viewModel: GsmsViewModel,
    onBannerMessage: (String) -> Unit
) {
    val school = workspace.activeSchool ?: return
    val role = workspace.activeRole
    val context = LocalContext.current
    val isStudentOrParent = RolePolicy.requiresLearnerScope(role)
    val focusStudentId by viewModel.focusStudentId.collectAsStateWithLifecycle()
    val focusStudent = if (isStudentOrParent) {
        RolePolicy.resolveFocusStudent(workspace.students, focusStudentId)
    } else {
        null
    }

    val tabs = remember(role, school.enabledModules) {
        RolePolicy.academicSubTabs(role, school, workspace.activeExtraCapabilities)
    }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }
    var showAddAcademicDialog by remember { mutableStateOf(false) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    var showNewReportDialog by remember { mutableStateOf(false) }
    var reviewTarget by remember { mutableStateOf<ReportCard?>(null) }

    if (tabs.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                Icons.Rounded.Lock,
                "Academics Restricted or Disabled",
                "${role.shortTitle} has no enabled academic modules in this view."
            )
        }
        return
    }

    if (isStudentOrParent && focusStudent == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LearnerScopePicker(
                students = workspace.students,
                focusStudentId = focusStudentId,
                onSelect = viewModel::selectFocusStudent
            )
            EmptyStateCard(
                Icons.Rounded.Person,
                "Select a learner",
                "${role.shortTitle} records are scoped to a single learner. Choose one to see their academics, attendance and report cards."
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isStudentOrParent) {
            LearnerScopePicker(
                students = workspace.students,
                focusStudentId = focusStudentId,
                onSelect = viewModel::selectFocusStudent,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }
        ScrollableTabRow(selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex)) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        when (tabs.getOrNull(selectedTab)) {
            "Academics & Exams" -> {
                val canEditAcademics = role.has(RoleCapability.EDIT_ACADEMICS)
                val items = workspace.academics.filter { rec ->
                    val classMatch = focusStudent == null || rec.className.equals(focusStudent.className, ignoreCase = true)
                    val queryMatch = searchQuery.isBlank() || rec.title.contains(searchQuery, true) || rec.className.contains(searchQuery, true)
                    classMatch && queryMatch
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isStudentOrParent) "Class Timetable & Exams" else "Timetables, Exams & Marks",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                if (!canEditAcademics) {
                                    Text(
                                        "Read-only view for ${role.shortTitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (canEditAcademics) {
                                FilledTonalButton(
                                    onClick = { showAddAcademicDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("add_academic_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Entry")
                                }
                            }
                        }
                    }
                    items(items) { rec ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${rec.className} • ${rec.subjectName ?: ""} • ${rec.teacherName ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${rec.scheduleSlot ?: ""} • Score: ${rec.score.toInt()}/${rec.maxScore.toInt()} (${rec.grade ?: "-"})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                StatusPill(rec.recordType, CobaltBlue)
                            }
                        }
                    }
                }
            }

            "Attendance" -> {
                val canMarkAttendance = role.has(RoleCapability.MARK_ATTENDANCE)
                val visibleStudents = if (focusStudent != null) listOf(focusStudent) else workspace.students
                val visibleAttendance = if (focusStudent != null) {
                    workspace.attendance.filter { it.targetName.equals(focusStudent.fullName, ignoreCase = true) }
                } else {
                    workspace.attendance
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = when {
                                isStudentOrParent -> "Personal Attendance Record"
                                canMarkAttendance -> "Quick Attendance Roll Call"
                                else -> "Attendance Oversight (Read-Only)"
                            },
                            subtitle = when {
                                isStudentOrParent -> "Live attendance synced from class roll call"
                                canMarkAttendance -> "Tap Present, Late, or Absent to sync live to Firestore"
                                else -> "View-only attendance metrics for ${role.shortTitle}"
                            }
                        )
                    }
                    items(visibleStudents) { student ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text("${student.className} • Rate: ${student.attendanceRate}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(student.riskLevel, riskColor(student.riskLevel))
                                }
                                if (canMarkAttendance) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("PRESENT" to EmeraldTeal, "LATE" to WarmAmber, "ABSENT" to CrimsonRed).forEach { (status, color) ->
                                            OutlinedButton(
                                                onClick = { viewModel.recordAttendance(school, workspace.activeBranchId, student, status) },
                                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                            ) {
                                                Text(status, color = color, style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SectionHeader(title = "Recent Attendance Log", subtitle = "${visibleAttendance.size} records synced")
                    }
                    items(visibleAttendance.take(10)) { att ->
                        OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${att.targetName} (${att.dateIso})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                StatusPill(att.status, if (att.status == "PRESENT") EmeraldTeal else if (att.status == "LATE") WarmAmber else CrimsonRed)
                            }
                        }
                    }
                }
            }

            "Report Cards" -> {
                val canFillSubmit = role.has(RoleCapability.FILL_SUBMIT_REPORT_CARDS)
                val canApprovePublish = role.has(RoleCapability.APPROVE_PUBLISH_REPORT_CARDS)
                val viewAllReports = role.has(RoleCapability.VIEW_ALL_REPORT_CARDS)
                val reports = workspace.reportCards.filter { report ->
                    val roleScopeMatch = if (viewAllReports) {
                        true
                    } else {
                        report.status == ReportStatus.PUBLISHED.name && (focusStudent == null || report.studentId == focusStudent.id)
                    }
                    val queryMatch = searchQuery.isBlank() || report.studentName.contains(searchQuery, true)
                    roleScopeMatch && queryMatch
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isStudentOrParent) "Published Report Cards" else "Report Card Workflow",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    if (isStudentOrParent) {
                                        "Only Principal-published reports are visible"
                                    } else {
                                        "Fill → Submit → Principal Review → Publish PDF"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canFillSubmit) {
                                Button(
                                    onClick = { showNewReportDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("new_report_card_button")
                                ) {
                                    Icon(Icons.Rounded.EditNote, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Fill Report")
                                }
                            }
                        }
                    }
                    if (reports.isEmpty()) {
                        item {
                            EmptyStateCard(
                                Icons.Rounded.Description,
                                "No Report Cards Available",
                                if (isStudentOrParent) "Published report cards will appear here once approved." else "No report cards match your filter."
                            )
                        }
                    }
                    items(reports) { report ->
                        val statusColor = when (report.status) {
                            ReportStatus.PUBLISHED.name -> EmeraldTeal
                            ReportStatus.APPROVED.name -> CobaltBlue
                            ReportStatus.SUBMITTED.name -> WarmAmber
                            ReportStatus.RETURNED.name -> CrimsonRed
                            else -> InkSecondary
                        }
                        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${report.studentName} • ${report.className}", style = MaterialTheme.typography.titleMedium)
                                        Text("${report.templateName} • v${report.version} • Avg ${"%.1f".format(report.averageScore)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(report.status, statusColor)
                                }
                                Text(report.subjectGradesSummary, style = MaterialTheme.typography.bodySmall)
                                if (!report.teacherRemarks.isNullOrBlank()) {
                                    Text("Teacher: ${report.teacherRemarks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!report.aiSummary.isNullOrBlank() && viewAllReports) {
                                    Surface(color = VioletSoft.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp)) {
                                        Text("AI Insight: ${report.aiSummary}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                if (!report.headteacherRemarks.isNullOrBlank()) {
                                    Text("Principal: ${report.headteacherRemarks}", style = MaterialTheme.typography.bodySmall, color = CobaltBlue)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (canFillSubmit && (report.status == ReportStatus.DRAFT.name || report.status == ReportStatus.RETURNED.name)) {
                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.reviewReportCard(school.id, report, ReportStatus.SUBMITTED, "", workspace.activeRole)
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) { Text("Submit") }
                                    }
                                    if (canApprovePublish && report.status == ReportStatus.SUBMITTED.name) {
                                        Button(
                                            onClick = { reviewTarget = report },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) { Text("Review & Approve") }
                                    }
                                    if (canApprovePublish && report.status == ReportStatus.APPROVED.name) {
                                        Button(
                                            onClick = {
                                                viewModel.reviewReportCard(school.id, report, ReportStatus.PUBLISHED, report.headteacherRemarks ?: "", workspace.activeRole)
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) { Text("Publish to Parent") }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            PdfExportHelper.exportReportCardPdf(context, school, report)
                                                .onSuccess { intent -> context.startActivity(intent) }
                                                .onFailure { err -> onBannerMessage(err.localizedMessage ?: "PDF export failed") }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("PDF")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Templates" -> {
                val canManageTemplates = role.has(RoleCapability.MANAGE_REPORT_TEMPLATES)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("School Report Templates", style = MaterialTheme.typography.titleLarge)
                                if (!canManageTemplates) {
                                    Text("Read-only template reference for ${role.shortTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (canManageTemplates) {
                                FilledTonalButton(
                                    onClick = { showNewTemplateDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("New Template")
                                }
                            }
                        }
                    }
                    items(workspace.reportTemplates) { tpl ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tpl.name, style = MaterialTheme.typography.titleMedium)
                                    StatusPill(if (tpl.active) "ACTIVE" else "INACTIVE", EmeraldTeal)
                                }
                                Text("Level: ${tpl.classLevel} • Scale: ${tpl.gradingScale}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sections: ${tpl.sections.joinToString(" | ")}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAcademicDialog) {
        AddAcademicEntryDialog(
            onDismiss = { showAddAcademicDialog = false },
            onConfirm = { type, title, cls, subj, teacher, slot, score ->
                viewModel.addAcademicRecord(school, workspace.activeBranchId, type, title, cls, subj, teacher, slot, score)
                showAddAcademicDialog = false
            }
        )
    }

    if (showNewTemplateDialog) {
        NewReportTemplateDialog(
            onDismiss = { showNewTemplateDialog = false },
            onConfirm = { name, level, sections, scale ->
                viewModel.createReportTemplate(school, name, level, sections, scale)
                showNewTemplateDialog = false
            }
        )
    }

    if (showNewReportDialog) {
        val defaultStudent = workspace.students.firstOrNull()
        val defaultTemplate = workspace.reportTemplates.firstOrNull()
        if (defaultStudent != null && defaultTemplate != null) {
            FillReportCardDialog(
                students = workspace.students,
                templates = workspace.reportTemplates,
                onGenerateAiComment = { studentName, grades, onResult ->
                    viewModel.generateQuickAiDraft(
                        workspace,
                        "Write a concise, encouraging 2-sentence teacher report comment for $studentName based on grades: $grades.",
                        "AI_REPORT_COMMENT",
                        onResult
                    )
                },
                onDismiss = { showNewReportDialog = false },
                onSubmit = { student, template, grades, avg, teacherRem, aiSum, submitNow ->
                    viewModel.saveOrSubmitReportCard(
                        school, workspace.activeBranchId, student, template, grades, avg, teacherRem, aiSum, submitNow
                    )
                    showNewReportDialog = false
                }
            )
        } else {
            showNewReportDialog = false
            onBannerMessage("Add at least one student and report template first.")
        }
    }

    reviewTarget?.let { target ->
        ReviewReportDialog(
            report = target,
            onGenerateAiSummary = { onResult ->
                viewModel.generateQuickAiDraft(
                    workspace,
                    "As Principal, review this submitted report for ${target.studentName} (${target.subjectGradesSummary}, Teacher: ${target.teacherRemarks}). Summarize strengths, missing details if any, and suggest a 1-sentence principal remark.",
                    "AI_REPORT_REVIEW",
                    onResult
                )
            },
            onDismiss = { reviewTarget = null },
            onDecision = { status, remarks ->
                viewModel.reviewReportCard(school.id, target, status, remarks, workspace.activeRole)
                reviewTarget = null
            }
        )
    }
}

@Composable
private fun AddAcademicEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, Double) -> Unit
) {
    var recordType by remember { mutableStateOf("EXAM") }
    var title by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("Grade 10A") }
    var subject by remember { mutableStateOf("Mathematics") }
    var teacher by remember { mutableStateOf("Dr. Samuel Kamau") }
    var slot by remember { mutableStateOf("Mon 09:00") }
    var scoreText by remember { mutableStateOf("85") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Academic Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TIMETABLE", "EXAM", "MARK").forEach { t ->
                        FilterChip(selected = recordType == t, onClick = { recordType = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Class") }, singleLine = true)
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("Teacher") }, singleLine = true)
                OutlinedTextField(value = slot, onValueChange = { slot = it }, label = { Text("Time Slot / Date") }, singleLine = true)
                OutlinedTextField(value = scoreText, onValueChange = { scoreText = it }, label = { Text("Score (0-100)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onConfirm(recordType, title, className, subject, teacher, slot, scoreText.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NewReportTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var classLevel by remember { mutableStateOf("Grade 9 - Grade 12") }
    var sections by remember { mutableStateOf("Core Subjects, Attendance, Teacher Remarks, Principal Endorsement") }
    var scale by remember { mutableStateOf("A (90+), B (80+), C (70+), D (60+)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Report Card Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Template Name") })
                OutlinedTextField(value = classLevel, onValueChange = { classLevel = it }, label = { Text("Class Level") })
                OutlinedTextField(value = sections, onValueChange = { sections = it }, label = { Text("Sections (comma separated)") })
                OutlinedTextField(value = scale, onValueChange = { scale = it }, label = { Text("Grading Scale") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.trim().length >= 2) onConfirm(name, classLevel, sections, scale) }) {
                Text("Create Template")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FillReportCardDialog(
    students: List<Student>,
    templates: List<ReportTemplate>,
    onGenerateAiComment: (String, String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (Student, ReportTemplate, String, Double, String, String?, Boolean) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.first()) }
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var grades by remember { mutableStateOf("Math: 88 (B+), Physics: 91 (A), English: 85 (B)") }
    var avgText by remember { mutableStateOf("88.0") }
    var remarks by remember { mutableStateOf("") }
    var aiSummary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teacher Online Report Form") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Student: ${selectedStudent.fullName} (${selectedStudent.className})", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    students.take(3).forEach { st ->
                        FilterChip(
                            selected = st.id == selectedStudent.id,
                            onClick = { selectedStudent = st },
                            label = { Text(st.fullName.substringBefore(" ")) }
                        )
                    }
                }
                Text("Template: ${selectedTemplate.name}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = grades, onValueChange = { grades = it }, label = { Text("Subject Grades") })
                OutlinedTextField(value = avgText, onValueChange = { avgText = it }, label = { Text("Average %") }, singleLine = true)
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Teacher Remarks") })
                OutlinedButton(onClick = {
                    onGenerateAiComment(selectedStudent.fullName, grades) { generated ->
                        remarks = generated
                        aiSummary = "AI Assisted Comment Applied"
                    }
                }) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Generate Comment")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = {
                    onSubmit(selectedStudent, selectedTemplate, grades, avgText.toDoubleOrNull() ?: 85.0, remarks, aiSummary.ifBlank { null }, false)
                }) { Text("Save Draft") }
                Button(onClick = {
                    onSubmit(selectedStudent, selectedTemplate, grades, avgText.toDoubleOrNull() ?: 85.0, remarks, aiSummary.ifBlank { null }, true)
                }) { Text("Submit") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReviewReportDialog(
    report: ReportCard,
    onGenerateAiSummary: ((String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onDecision: (ReportStatus, String) -> Unit
) {
    var remarks by remember { mutableStateOf(report.headteacherRemarks ?: "") }
    var aiReviewNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Principal Review: ${report.studentName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grades: ${report.subjectGradesSummary}", style = MaterialTheme.typography.bodySmall)
                Text("Teacher: ${report.teacherRemarks ?: "None"}", style = MaterialTheme.typography.bodySmall)
                if (aiReviewNote.isNotBlank()) {
                    Surface(color = VioletSoft.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                        Text(aiReviewNote, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                    }
                }
                OutlinedButton(onClick = {
                    onGenerateAiSummary { generated ->
                        aiReviewNote = generated
                        if (remarks.isBlank()) remarks = generated.take(240)
                    }
                }) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Review Summary")
                }
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Headteacher / Principal Remarks") })
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onDecision(ReportStatus.RETURNED, remarks) }) { Text("Return") }
                Button(onClick = { onDecision(ReportStatus.APPROVED, remarks) }) { Text("Approve") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
