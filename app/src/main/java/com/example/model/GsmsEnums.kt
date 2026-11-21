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
        displayName = "Super Administrator Portal",
        shortTitle = "Super Admin",
        scopeDescription = "Overall GSMS platform governance, tenants, SaaS billing & global audit",
        aiCapabilities = listOf(
            "Executive Multi-School Report",
            "Churn & Revenue Forecast",
            "Cross-Tenant Anomaly Scan"
        )
    ),
    SCHOOL_ADMIN(
        displayName = "School Administrator Portal",
        shortTitle = "School Admin",
        scopeDescription = "Assigned school & branch administration, module registry & permissions",
        aiCapabilities = listOf(
            "AI Timetable Generator",
            "Draft School Circular",
            "Staff Workload Balance"
        )
    ),
    HEADTEACHER(
        displayName = "Headteacher / Principal Portal",
        shortTitle = "Headteacher",
        scopeDescription = "Academic leadership, authoritative report approvals & risk interventions",
        aiCapabilities = listOf(
            "Summarize Submitted Reports",
            "Learner Intervention Plan",
            "Department Performance Review"
        )
    ),
    DEPUTY_HEADTEACHER(
        displayName = "Deputy Headteacher Portal",
        shortTitle = "Deputy Head",
        scopeDescription = "Curriculum coordination, timetables, discipline & delegated report review",
        aiCapabilities = listOf(
            "Lesson Cover Optimization",
            "Discipline Pattern Digest",
            "Class Academic Audit"
        )
    ),
    TEACHER(
        displayName = "Teacher Portal",
        shortTitle = "Teacher",
        scopeDescription = "Assigned classes & subjects, marks, attendance & report card drafts",
        aiCapabilities = listOf(
            "Generate Lesson Plan",
            "Generate Quiz & Rubric",
            "Draft Report Card Comment"
        )
    ),
    CLASS_TEACHER(
        displayName = "Class Teacher Portal",
        shortTitle = "Class Teacher",
        scopeDescription = "Homeroom class pastoral oversight, daily roll call & term report compilation",
        aiCapabilities = listOf(
            "Homeroom Attendance Digest",
            "Compile Class Report Summary",
            "Parent Check-In Note"
        )
    ),
    PARENT(
        displayName = "Parent / Guardian Portal",
        shortTitle = "Parent",
        scopeDescription = "Linked children progress, attendance, fee invoices & published reports",
        aiCapabilities = listOf(
            "Explain Grades Simply",
            "Home Support Checklist",
            "Translate School Notice"
        )
    ),
    STUDENT(
        displayName = "Student / Learner Portal",
        shortTitle = "Student",
        scopeDescription = "Own class timetable, attendance, published reports & study companion",
        aiCapabilities = listOf(
            "Personal Revision Timetable",
            "Weak-Topic Explainer",
            "Exam Prep Practice Quiz"
        )
    ),
    FINANCE_OFFICER(
        displayName = "Accountant / Finance Officer Portal",
        shortTitle = "Accountant",
        scopeDescription = "Authorized financial modules, fee collections, invoices & payroll",
        aiCapabilities = listOf(
            "Fee Default Risk Forecast",
            "Draft Unpaid Fee Reminder",
            "Cashflow & Expense Summary"
        )
    ),
    HR_MANAGER(
        displayName = "Human Resource Officer Portal",
        shortTitle = "HR Officer",
        scopeDescription = "Staff directory, leave approvals, balances & substitute cover planning",
        aiCapabilities = listOf(
            "Detect Leave Conflicts",
            "Suggest Substitute Cover",
            "Draft HR Policy Memo"
        )
    ),
    LIBRARIAN(
        displayName = "Librarian Portal",
        shortTitle = "Librarian",
        scopeDescription = "Library catalog, borrowings, overdue tracking & reading outreach",
        aiCapabilities = listOf(
            "Reading Recommendations",
            "Overdue Risk Prediction",
            "Auto-Categorize Titles"
        )
    ),
    TRANSPORT_MANAGER(
        displayName = "Transport Manager Portal",
        shortTitle = "Transport",
        scopeDescription = "Fleet readiness, bus routes, daily trips & route delay alerts",
        aiCapabilities = listOf(
            "Optimize Bus Routes",
            "Delay & Maintenance Alert",
            "Fuel Efficiency Summary"
        )
    ),
    WELFARE_NURSE(
        displayName = "School Nurse / Medical Officer Portal",
        shortTitle = "School Nurse",
        scopeDescription = "Student medical visits, clinic logs, immunization & health alerts",
        aiCapabilities = listOf(
            "Medical Trend Summary",
            "Draft Parent Health Advisory",
            "Absenteeism Health Check"
        )
    ),
    ADMISSIONS_RECEPTION(
        displayName = "Receptionist / Front Desk Officer Portal",
        shortTitle = "Reception",
        scopeDescription = "Front desk visitor logs, inquiries, admissions intake & campus events",
        aiCapabilities = listOf(
            "Enrollment Forecast",
            "Admission Checklist Builder",
            "Visitor & Parent Reply"
        )
    ),
    PROCUREMENT_OFFICER(
        displayName = "Procurement Officer Portal",
        shortTitle = "Procurement",
        scopeDescription = "Requisitions, vendor RFQs, purchase orders & supplier spend control",
        aiCapabilities = listOf(
            "Vendor Quote Comparison",
            "Draft Purchase Order Memo",
            "Quarterly Spend Forecast"
        )
    ),
    STOREKEEPER(
        displayName = "Storekeeper Portal",
        shortTitle = "Storekeeper",
        scopeDescription = "Campus storeroom stock, asset custody, issuances & reorder thresholds",
        aiCapabilities = listOf(
            "Low-Stock Reorder Plan",
            "Asset Depreciation Audit",
            "Term Supply Allocation"
        )
    ),
    EXAMINATION_OFFICER(
        displayName = "Examination Officer Portal",
        shortTitle = "Exam Officer",
        scopeDescription = "Exam timetables, grading scales, mark moderation & report templates",
        aiCapabilities = listOf(
            "Detect Exam Clash Schedule",
            "Grade Distribution Analysis",
            "Draft Exam Invigilation Roster"
        )
    ),
    SCHOOL_COUNSELLOR(
        displayName = "School Counsellor Portal",
        shortTitle = "Counsellor",
        scopeDescription = "Student guidance, behavioral wellbeing, safeguarding & intervention plans",
        aiCapabilities = listOf(
            "Draft Pastoral Care Plan",
            "Behavioral Risk Triage",
            "Parent Counselling Summary"
        )
    ),
    ICT_ADMIN(
        displayName = "ICT Administrator Portal",
        shortTitle = "ICT Admin",
        scopeDescription = "User permission delegation, campus ICT assets, system tickets & audit",
        aiCapabilities = listOf(
            "RBAC Permission Audit",
            "ICT Support Triage",
            "Network & Lab Readiness"
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
    PROCUREMENT("Procurement", "Operations"),
    COMMUNICATION("Communication", "Engagement"),
    LIBRARY("Library", "Campus"),
    TRANSPORT("Transport", "Campus"),
    HEALTH_WELFARE("Health / Welfare", "Campus"),
    INVENTORY_ASSETS("Inventory / Stores", "Campus"),
    HOSTEL_BOARDING("Hostel / Boarding", "Campus"),
    DISCIPLINE("Discipline & Guidance", "Campus"),
    EVENTS("Events", "Engagement"),
    ANALYTICS("Analytics", "Intelligence"),
    AI_TOOLS("AI Tools", "Intelligence"),
    DOCUMENTS_TEMPLATES("Documents / Templates", "Administration"),
    ICT_SYSTEMS("ICT & Access Control", "Administration");

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
