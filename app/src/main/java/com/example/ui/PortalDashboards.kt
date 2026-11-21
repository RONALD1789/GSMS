package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.PdfExportHelper
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PortalDashboardScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    isWideScreen: Boolean,
    onRunAiCapability: (String) -> Unit,
    onOpenAiTab: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onSelectSchool: (School) -> Unit,
    onBannerMessage: (String) -> Unit
) {
    val role = workspace.activeRole
    val school = workspace.activeSchool
    val lowData = workspace.userProfile?.lowDataMode == true
    val aiEnabled = school?.hasModule(GsmsModule.AI_TOOLS) != false

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AiCopilotBannerCard(
                role = role,
                lowDataMode = lowData,
                aiEnabled = aiEnabled,
                onPromptClick = { capability ->
                    onRunAiCapability("Perform '$capability' for ${role.displayName} using current school data.")
                    onOpenAiTab()
                },
                onOpenFullAi = onOpenAiTab
            )
        }

        when (role) {
            PortalRole.SUPER_ADMIN -> {
                val totalStudents = workspace.schools.sumOf { it.studentCount }
                val totalStaff = workspace.schools.sumOf { it.staffCount }
                val totalRevenue = workspace.schools.sumOf { it.monthlyRevenueUsd }
                val activeSubs = workspace.schools.count { it.subscriptionStatus == "ACTIVE" }
                val atRiskSchools = workspace.schools.filter { it.churnRiskScore >= 50 || it.subscriptionStatus == "AT_RISK" }

                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Total Schools", "${workspace.schools.size}", "Multi-Tenant", Icons.Rounded.Domain, CobaltBlue),
                            KpiData("Active Subscriptions", "$activeSubs/${workspace.schools.size}", "SaaS", Icons.Rounded.Verified, EmeraldTeal),
                            KpiData("Total Students", "$totalStudents", "${totalStaff} Staff", Icons.Rounded.Groups, VioletAi),
                            KpiData("Platform MRR", "$${totalRevenue.toInt()}", "USD/mo", Icons.Rounded.Payments, WarmAmber)
                        )
                    )
                }
                item {
                    BarChartCard(
                        title = "Tenant Revenue & Activity Comparison",
                        subtitle = "Monthly subscription strength across schools",
                        bars = workspace.schools.map {
                            it.code to ((it.monthlyRevenueUsd / 5000.0) * 100.0).toFloat().coerceIn(10f, 100f)
                        },
                        barColor = CobaltBlue
                    )
                }
                item {
                    SectionHeader(
                        title = "Schools & Churn Risk Watchlist",
                        subtitle = "${atRiskSchools.size} school(s) flagged for retention follow-up",
                        actionLabel = "Manage Tenants",
                        onActionClick = { onNavigateToTab(5) }
                    )
                }
                items(workspace.schools.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                }) { tenant ->
                    ElevatedCard(
                        onClick = { onSelectSchool(tenant) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tenant.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${tenant.city}, ${tenant.country} • ${tenant.studentCount} students • $${tenant.monthlyRevenueUsd.toInt()}/mo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = "Churn ${tenant.churnRiskScore}%",
                                color = if (tenant.churnRiskScore >= 50) CrimsonRed else EmeraldTeal
                            )
                        }
                    }
                }
            }

            PortalRole.SCHOOL_ADMIN -> {
                val billed = workspace.finance.sumOf { it.amountUsd }
                val paid = workspace.finance.sumOf { it.paidUsd }
                val pendingApprovals = workspace.reportCards.count { it.status == ReportStatus.SUBMITTED.name } +
                    workspace.leaveRequests.count { it.status == LeaveStatus.PENDING.name }
                val attPresent = workspace.attendance.count { it.status == "PRESENT" }
                val attPct = if (workspace.attendance.isEmpty()) 94 else (attPresent * 100) / workspace.attendance.size

                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Students", "${school?.studentCount ?: workspace.students.size}", "Active", Icons.Rounded.School, CobaltBlue),
                            KpiData("Staff Count", "${school?.staffCount ?: workspace.staff.size}", "${workspace.staff.count { it.onLeave }} on leave", Icons.Rounded.Badge, EmeraldTeal),
                            KpiData("Fees Collected", "$${paid.toInt()}", "of $${billed.toInt()}", Icons.Rounded.AccountBalanceWallet, WarmAmber),
                            KpiData("Pending Approvals", "$pendingApprovals", "$attPct% Att.", Icons.Rounded.PendingActions, CrimsonRed)
                        )
                    )
                }
                item {
                    QuickActionStrip(
                        actions = listOf(
                            "Module Registry" to { onNavigateToTab(5) },
                            "Report Templates" to { onNavigateToTab(1) },
                            "Leave Approvals" to { onNavigateToTab(2) },
                            "Send Notice" to { onNavigateToTab(4) }
                        )
                    )
                }
                item {
                    AlertSnippetList(
                        title = "AI Action Recommendations & Alerts",
                        alerts = workspace.alerts,
                        onSeeAll = { onNavigateToTab(4) }
                    )
                }
            }

            PortalRole.HEADTEACHER -> {
                val atRisk = workspace.students.filter { it.riskLevel == RiskLevel.HIGH.name || it.riskLevel == RiskLevel.CRITICAL.name }
                val pendingReports = workspace.reportCards.count { it.status == ReportStatus.SUBMITTED.name }
                val onLeaveCount = workspace.staff.count { it.onLeave }
                val avgScore = if (workspace.students.isEmpty()) 0.0 else workspace.students.map { it.averageScore }.average()

                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("School Mean", "${"%.1f".format(avgScore)}%", "Term 1", Icons.Rounded.TrendingUp, CobaltBlue),
                            KpiData("Students at Risk", "${atRisk.size}", "Early Warning", Icons.Rounded.WarningAmber, CrimsonRed),
                            KpiData("Reports to Review", "$pendingReports", "Submitted", Icons.Rounded.FactCheck, WarmAmber),
                            KpiData("Staff on Leave", "$onLeaveCount", "Active Cover", Icons.Rounded.EventBusy, EmeraldTeal)
                        )
                    )
                }
                item {
                    BarChartCard(
                        title = "Class & Student Performance Overview",
                        subtitle = "Average term scores across tracked students",
                        bars = workspace.students.take(5).map {
                            it.fullName.substringBefore(" ") to it.averageScore.toFloat()
                        },
                        barColor = CobaltBlue
                    )
                }
                item {
                    SectionHeader(
                        title = "Pending Report Approvals",
                        subtitle = "Review teacher submissions and publish to parents",
                        actionLabel = "Review Queue",
                        onActionClick = { onNavigateToTab(1) }
                    )
                }
                items(workspace.reportCards.filter { it.status == ReportStatus.SUBMITTED.name }) { report ->
                    ReportCardSummaryRow(report = report, onClick = { onNavigateToTab(1) })
                }
            }

            PortalRole.TEACHER -> {
                val lessonsToday = workspace.academics.count { it.recordType == "TIMETABLE" }
                val draftReports = workspace.reportCards.count { it.status == ReportStatus.DRAFT.name || it.status == ReportStatus.RETURNED.name }
                val ungraded = workspace.academics.count { it.recordType == "EXAM" }

                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Lessons Today", "$lessonsToday", "Grade 10A", Icons.Rounded.MenuBook, CobaltBlue),
                            KpiData("Attendance", "${workspace.attendance.size} logged", "Today", Icons.Rounded.HowToReg, EmeraldTeal),
                            KpiData("Marking Queue", "$ungraded", "Assessments", Icons.Rounded.Grading, WarmAmber),
                            KpiData("Report Drafts", "$draftReports", "Due Soon", Icons.Rounded.Assignment, VioletAi)
                        )
                    )
                }
                item {
                    QuickActionStrip(
                        actions = listOf(
                            "Mark Attendance" to { onNavigateToTab(1) },
                            "Fill Report Cards" to { onNavigateToTab(1) },
                            "Apply for Leave" to { onNavigateToTab(2) },
                            "AI Lesson Plan" to {
                                onRunAiCapability("Generate a 45-minute interactive lesson plan for Grade 10A Physics on Kinematics with formative quiz questions.")
                                onOpenAiTab()
                            }
                        )
                    )
                }
                item {
                    SectionHeader(title = "Today's Teaching Schedule", actionLabel = "Full Timetable", onActionClick = { onNavigateToTab(1) })
                }
                items(workspace.academics.filter { it.recordType == "TIMETABLE" }) { slot ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(slot.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${slot.className} • ${slot.subjectName ?: ""} • ${slot.scheduleSlot ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(slot.className, CobaltBlue)
                        }
                    }
                }
            }

            PortalRole.STUDENT -> {
                val student = workspace.students.firstOrNull()
                val publishedReports = workspace.reportCards.filter { it.status == ReportStatus.PUBLISHED.name }
                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Performance Avg", "${student?.averageScore ?: 92.0}%", "Top Tier", Icons.Rounded.Stars, EmeraldTeal),
                            KpiData("Attendance", "${student?.attendanceRate ?: 97.5}%", "On Track", Icons.Rounded.CheckCircle, CobaltBlue),
                            KpiData("Exams Upcoming", "${workspace.academics.count { it.recordType == "EXAM" }}", "This Term", Icons.Rounded.EventNote, WarmAmber),
                            KpiData("Report Cards", "${publishedReports.size}", "Published", Icons.Rounded.PictureAsPdf, VioletAi)
                        )
                    )
                }
                item {
                    TrendSparklineCard(
                        title = "Personal Mastery Trajectory",
                        headlineValue = "${student?.averageScore ?: 92.4}%",
                        points = listOf(81f, 84f, 87f, 89f, (student?.averageScore ?: 92.4).toFloat()),
                        lineColor = EmeraldTeal
                    )
                }
                item {
                    PublishedReportDownloadSection(
                        school = school,
                        reports = publishedReports,
                        onBannerMessage = onBannerMessage
                    )
                }
            }

            PortalRole.PARENT -> {
                val child = workspace.students.firstOrNull()
                val publishedReports = workspace.reportCards.filter { it.status == ReportStatus.PUBLISHED.name }
                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Child Average", "${child?.averageScore ?: 92.4}%", child?.className ?: "Grade 10A", Icons.Rounded.School, CobaltBlue),
                            KpiData("Attendance", "${child?.attendanceRate ?: 97.5}%", "Term 1", Icons.Rounded.Verified, EmeraldTeal),
                            KpiData("Fee Balance", "$${(child?.feeBalanceUsd ?: 0.0).toInt()}", "Account", Icons.Rounded.Payments, if ((child?.feeBalanceUsd ?: 0.0) > 0) WarmAmber else EmeraldTeal),
                            KpiData("Messages", "${workspace.communications.size}", "School Updates", Icons.Rounded.MarkEmailUnread, VioletAi)
                        )
                    )
                }
                item {
                    PublishedReportDownloadSection(
                        school = school,
                        reports = publishedReports,
                        onBannerMessage = onBannerMessage
                    )
                }
                item {
                    SectionHeader(title = "Recent School Announcements", actionLabel = "Messages", onActionClick = { onNavigateToTab(4) })
                }
                items(workspace.communications.take(3)) { msg ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(msg.title, style = MaterialTheme.typography.titleMedium)
                            Text(msg.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            PortalRole.FINANCE_OFFICER -> {
                val billed = workspace.finance.filter { it.entryType == "INVOICE" }.sumOf { it.amountUsd }
                val paid = workspace.finance.filter { it.entryType == "INVOICE" }.sumOf { it.paidUsd }
                val expenses = workspace.finance.filter { it.entryType == "EXPENSE" }.sumOf { it.amountUsd }
                val payroll = workspace.finance.filter { it.entryType == "PAYROLL" }.sumOf { it.amountUsd }
                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Total Billed", "$${billed.toInt()}", "Invoices", Icons.Rounded.ReceiptLong, CobaltBlue),
                            KpiData("Total Paid", "$${paid.toInt()}", "Collected", Icons.Rounded.Paid, EmeraldTeal),
                            KpiData("Outstanding", "$${(billed - paid).coerceAtLeast(0.0).toInt()}", "Overdue Watch", Icons.Rounded.Warning, CrimsonRed),
                            KpiData("Payroll & Exp.", "$${(expenses + payroll).toInt()}", "Outflow", Icons.Rounded.AccountBalance, WarmAmber)
                        )
                    )
                }
                item {
                    QuickActionStrip(
                        actions = listOf(
                            "Open Fee Ledger" to { onNavigateToTab(3) },
                            "Overdue Alerts" to { onNavigateToTab(4) }
                        )
                    )
                }
                item {
                    SectionHeader(title = "Recent Invoices & Payroll", actionLabel = "Finance Hub", onActionClick = { onNavigateToTab(3) })
                }
                items(workspace.finance.take(4)) { entry ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.referenceNo} • ${entry.title}", style = MaterialTheme.typography.titleSmall)
                                Text("${entry.partyName} • $${entry.paidUsd.toInt()}/$${entry.amountUsd.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(entry.status, if (entry.status == "PAID") EmeraldTeal else CrimsonRed)
                        }
                    }
                }
            }

            PortalRole.HR_MANAGER -> {
                val pendingLeaves = workspace.leaveRequests.count { it.status == LeaveStatus.PENDING.name }
                val onLeave = workspace.staff.count { it.onLeave }
                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Total Staff", "${workspace.staff.size}", "Active Directory", Icons.Rounded.Badge, CobaltBlue),
                            KpiData("Pending Leaves", "$pendingLeaves", "Needs Review", Icons.Rounded.PendingActions, WarmAmber),
                            KpiData("On Leave Today", "$onLeave", "Auto-Tracked", Icons.Rounded.BeachAccess, CrimsonRed),
                            KpiData("Avg Workload", "${workspace.staff.map { it.workloadHours }.average().toInt()}h", "Weekly", Icons.Rounded.Schedule, EmeraldTeal)
                        )
                    )
                }
                item {
                    SectionHeader(title = "Leave Approval Queue", actionLabel = "Open HR", onActionClick = { onNavigateToTab(2) })
                }
                items(workspace.leaveRequests) { lv ->
                    ElevatedCard(onClick = { onNavigateToTab(2) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${lv.staffName} (${lv.leaveType})", style = MaterialTheme.typography.titleSmall)
                                Text("${lv.startDate} → ${lv.endDate} (${lv.daysCount}d)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(lv.status, if (lv.status == "APPROVED") EmeraldTeal else if (lv.status == "PENDING") WarmAmber else CrimsonRed)
                        }
                    }
                }
            }

            PortalRole.LIBRARIAN,
            PortalRole.TRANSPORT_MANAGER,
            PortalRole.WELFARE_NURSE,
            PortalRole.ADMISSIONS_RECEPTION -> {
                val targetTypes = when (role) {
                    PortalRole.LIBRARIAN -> setOf("LIBRARY_BOOK", "LIBRARY_BORROW")
                    PortalRole.TRANSPORT_MANAGER -> setOf("VEHICLE", "ROUTE", "TRIP")
                    PortalRole.WELFARE_NURSE -> setOf("WELFARE", "DISCIPLINE")
                    else -> setOf("ADMISSION", "EVENT")
                }
                val filteredOps = workspace.operations.filter { it.moduleType in targetTypes }
                item {
                    KpiGrid(
                        isWideScreen = isWideScreen,
                        cards = listOf(
                            KpiData("Active Records", "${filteredOps.size}", role.shortTitle, Icons.Rounded.Inventory2, CobaltBlue),
                            KpiData("Attention Needed", "${filteredOps.count { it.status.contains("OVERDUE") || it.status.contains("DELAY") || it.status.contains("FOLLOW") }}", "Live", Icons.Rounded.NotificationImportant, WarmAmber),
                            KpiData("Students Linked", "${workspace.students.size}", "Campus", Icons.Rounded.Groups, EmeraldTeal),
                            KpiData("Open Alerts", "${workspace.alerts.count { it.status != "RESOLVED" }}", "Early Warning", Icons.Rounded.Shield, CrimsonRed)
                        )
                    )
                }
                item {
                    SectionHeader(title = "${role.shortTitle} Operations", actionLabel = "Manage All", onActionClick = { onNavigateToTab(3) })
                }
                items(filteredOps) { op ->
                    ElevatedCard(onClick = { onNavigateToTab(3) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(op.title, style = MaterialTheme.typography.titleSmall)
                                Text(op.subtitle ?: op.notes ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(op.status, CobaltBlue)
                        }
                    }
                }
            }
        }
    }
}

private data class KpiData(
    val label: String,
    val value: String,
    val delta: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color
)

@Composable
private fun KpiGrid(isWideScreen: Boolean, cards: List<KpiData>) {
    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.forEach { card ->
                KpiMetricCard(
                    label = card.label,
                    value = card.value,
                    delta = card.delta,
                    icon = card.icon,
                    accentColor = card.accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { card ->
                        KpiMetricCard(
                            label = card.label,
                            value = card.value,
                            delta = card.delta,
                            icon = card.icon,
                            accentColor = card.accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionStrip(actions: List<Pair<String, () -> Unit>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.take(2).forEach { (label, onClick) ->
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
    if (actions.size > 2) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.drop(2).take(2).forEach { (label, onClick) ->
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AlertSnippetList(
    title: String,
    alerts: List<EarlyWarningAlert>,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = title, actionLabel = "Analytics", onActionClick = onSeeAll)
        alerts.take(3).forEach { alert ->
            ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${alert.category}: ${alert.targetName}", style = MaterialTheme.typography.titleSmall)
                        Text(alert.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusPill(alert.riskLevel, riskColor(alert.riskLevel))
                }
            }
        }
    }
}

@Composable
private fun ReportCardSummaryRow(report: ReportCard, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${report.studentName} • ${report.className}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Avg ${"%.1f".format(report.averageScore)}% • v${report.version} • ${report.templateName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(report.status, CobaltBlue)
        }
    }
}

@Composable
private fun PublishedReportDownloadSection(
    school: School?,
    reports: List<ReportCard>,
    onBannerMessage: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Published Report Cards", subtitle = "Approved & available for PDF export")
        if (reports.isEmpty()) {
            EmptyStateCard(Icons.Rounded.Description, "No Published Reports Yet", "Approved reports will appear here once signed off by the Principal.")
        } else {
            reports.forEach { report ->
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${report.studentName} (${report.term})", style = MaterialTheme.typography.titleMedium)
                                Text(report.subjectGradesSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill("${"%.1f".format(report.averageScore)}%", EmeraldTeal)
                        }
                        if (!report.headteacherRemarks.isNullOrBlank()) {
                            Text("Principal: ${report.headteacherRemarks}", style = MaterialTheme.typography.bodySmall)
                        }
                        FilledTonalButton(
                            onClick = {
                                if (school != null) {
                                    PdfExportHelper.exportReportCardPdf(context, school, report)
                                        .onSuccess { intent -> context.startActivity(intent) }
                                        .onFailure { err -> onBannerMessage(err.localizedMessage ?: "PDF export failed") }
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download / Share PDF")
                        }
                    }
                }
            }
        }
    }
}
