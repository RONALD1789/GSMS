package com.example.model

inline fun <reified T : Enum<T>> enumByNameOrDefault(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}

enum class PortalRole(
    val displayName: String,
    val shortTitle: String,
    val scopeDescription: String,
    val aiCapabilities: List<String>
) {
    SUPER_ADMIN(
        displayName = "Super Admin Portal",
        shortTitle = "Super Admin",
        scopeDescription = "Platform-wide multi-school governance, billing & churn intelligence",
        aiCapabilities = listOf(
            "Executive Multi-School Report",
            "Churn & Revenue Forecast",
            "Cross-Tenant Anomaly Scan"
        )
    ),
    SCHOOL_ADMIN(
        displayName = "School Admin Portal",
        shortTitle = "School Admin",
        scopeDescription = "Single-school operations, module registry, timetable & circulars",
        aiCapabilities = listOf(
            "AI Timetable Generator",
            "Draft School Circular",
            "Staff Workload Balance"
        )
    ),
    HEADTEACHER(
        displayName = "Headteacher / Principal Portal",
        shortTitle = "Principal",
        scopeDescription = "Academic leadership, report approvals & learner risk interventions",
        aiCapabilities = listOf(
            "Summarize Submitted Reports",
            "Learner Intervention Plan",
            "Department Performance Review"
        )
    ),
    TEACHER(
        displayName = "Teacher Portal",
        shortTitle = "Teacher",
        scopeDescription = "Lesson plans, assessments, marks & online report submissions",
        aiCapabilities = listOf(
            "Generate Lesson Plan",
            "Generate Quiz & Rubric",
            "Draft Report Card Comment"
        )
    ),
    STUDENT(
        displayName = "Student Portal",
        shortTitle = "Student",
        scopeDescription = "Personalized learning, timetable, assignments & study companion",
        aiCapabilities = listOf(
            "Personal Revision Timetable",
            "Weak-Topic Explainer",
            "Exam Prep Practice Quiz"
        )
    ),
    PARENT(
        displayName = "Parent Portal",
        shortTitle = "Parent",
        scopeDescription = "Child progress, attendance, fee status & home support insights",
        aiCapabilities = listOf(
            "Explain Grades Simply",
            "Home Support Checklist",
            "Translate School Notice"
        )
    ),
    FINANCE_OFFICER(
        displayName = "Finance Officer Portal",
        shortTitle = "Finance",
        scopeDescription = "Fees structures, invoices, collections, payroll & cashflow",
        aiCapabilities = listOf(
            "Fee Default Risk Forecast",
            "Draft Unpaid Fee Reminder",
            "Cashflow & Expense Summary"
        )
    ),
    LIBRARIAN(
        displayName = "Librarian Portal",
        shortTitle = "Librarian",
        scopeDescription = "Catalog management, borrowings, overdue tracking & reading trends",
        aiCapabilities = listOf(
            "Reading Recommendations",
            "Overdue Risk Prediction",
            "Auto-Categorize Titles"
        )
    ),
    TRANSPORT_MANAGER(
        displayName = "Transport Manager Portal",
        shortTitle = "Transport",
        scopeDescription = "Fleet readiness, route optimization, daily trips & delay alerts",
        aiCapabilities = listOf(
            "Optimize Bus Routes",
            "Delay & Maintenance Alert",
            "Fuel Efficiency Summary"
        )
    ),
    HR_MANAGER(
        displayName = "HR Portal",
        shortTitle = "HR & Staff",
        scopeDescription = "Workforce directory, leave approvals, balances & substitute staffing",
        aiCapabilities = listOf(
            "Detect Leave Conflicts",
            "Suggest Substitute Cover",
            "Draft HR Policy Memo"
        )
    ),
    WELFARE_NURSE(
        displayName = "Welfare / Nurse Portal",
        shortTitle = "Welfare",
        scopeDescription = "Student health visits, safeguarding cases & follow-up alerts",
        aiCapabilities = listOf(
            "Welfare Trend Summary",
            "Draft Case Follow-Up Plan",
            "Absenteeism Pattern Check"
        )
    ),
    ADMISSIONS_RECEPTION(
        displayName = "Admissions / Reception Portal",
        shortTitle = "Admissions",
        scopeDescription = "Enrollment pipeline, inquiries, checklists & parent follow-ups",
        aiCapabilities = listOf(
            "Enrollment Forecast",
            "Admission Checklist Builder",
            "Prospective Parent Reply"
        )
    )
}

enum class GsmsModule(val label: String, val category: String) {
    ACADEMICS("Academics", "Core"),
    ADMISSIONS("Admissions", "Enrollment"),
    ATTENDANCE("Attendance", "Core"),
    EXAMS("Exams", "Core"),
    REPORT_CARDS("Report Cards", "Core"),
    FINANCE("Fees / Finance", "Operations"),
    HR_STAFF("HR / Staff", "Operations"),
    LEAVE_MANAGEMENT("Leave Management", "Operations"),
    COMMUNICATION("Communication", "Engagement"),
    LIBRARY("Library", "Campus"),
    TRANSPORT("Transport", "Campus"),
    HEALTH_WELFARE("Health / Welfare", "Campus"),
    INVENTORY_ASSETS("Inventory / Assets", "Campus"),
    HOSTEL_BOARDING("Hostel / Boarding", "Campus"),
    DISCIPLINE("Discipline", "Campus"),
    EVENTS("Events", "Engagement"),
    ANALYTICS("Analytics", "Intelligence"),
    AI_TOOLS("AI Tools", "Intelligence"),
    DOCUMENTS_TEMPLATES("Documents / Templates", "Administration");

    companion object {
        val defaultEnabledNames: List<String> = entries.map { it.name }
    }
}

enum class ReportStatus(val label: String) {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    RETURNED("Returned"),
    APPROVED("Approved"),
    PUBLISHED("Published")
}

enum class LeaveStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class RiskLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}
