package com.example.model

enum class GsmsTab(val label: String) {
    DASHBOARD("Dashboard"),
    ACADEMICS("Academics"),
    PEOPLE("People & HR"),
    OPERATIONS("Operations"),
    ANALYTICS("Analytics"),
    AI_GOVERNANCE("AI & Profile")
}

enum class RoleCapability {
    // Academics & Reports
    VIEW_ACADEMICS,
    EDIT_ACADEMICS,
    VIEW_ATTENDANCE,
    MARK_ATTENDANCE,
    VIEW_ALL_REPORT_CARDS,
    VIEW_OWN_PUBLISHED_REPORTS,
    FILL_SUBMIT_REPORT_CARDS,
    APPROVE_PUBLISH_REPORT_CARDS,
    VIEW_REPORT_TEMPLATES,
    MANAGE_REPORT_TEMPLATES,

    // People & Leave
    VIEW_ALL_STUDENTS,
    ENROLL_STUDENT,
    VIEW_STAFF_DIRECTORY,
    VIEW_LEAVE_WORKFLOW,
    APPLY_LEAVE,
    APPROVE_LEAVE,

    // Operations & Finance
    VIEW_FINANCE_ALL,
    VIEW_PAYROLL_ONLY,
    VIEW_OWN_INVOICES_ONLY,
    RECORD_FINANCE_PAYMENT,
    EDIT_OPERATIONS,

    // Analytics, Communications & Audit
    VIEW_ANALYTICS_CHARTS,
    VIEW_ALERTS,
    MANAGE_ALERTS,
    VIEW_COMMUNICATIONS,
    COMPOSE_COMMUNICATIONS,
    VIEW_AUDIT_LOGS,

    // Tenant & Platform Governance
    CREATE_SCHOOL_TENANT,
    SWITCH_SCHOOL_TENANT,
    SWITCH_BRANCH,
    MANAGE_MODULES,
    UPDATE_ACADEMIC_PERIOD
}

object RolePolicy {

    private val capabilitiesByRole: Map<PortalRole, Set<RoleCapability>> = mapOf(
        PortalRole.SUPER_ADMIN to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.VIEW_ALL_REPORT_CARDS,
            RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.VIEW_STAFF_DIRECTORY,
            RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.VIEW_FINANCE_ALL,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.VIEW_AUDIT_LOGS,
            RoleCapability.CREATE_SCHOOL_TENANT,
            RoleCapability.SWITCH_SCHOOL_TENANT,
            RoleCapability.SWITCH_BRANCH,
            RoleCapability.MANAGE_MODULES,
            RoleCapability.UPDATE_ACADEMIC_PERIOD
        ),
        PortalRole.SCHOOL_ADMIN to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.MARK_ATTENDANCE,
            RoleCapability.VIEW_ALL_REPORT_CARDS,
            RoleCapability.FILL_SUBMIT_REPORT_CARDS,
            RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
            RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.MANAGE_REPORT_TEMPLATES,
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.ENROLL_STUDENT,
            RoleCapability.VIEW_STAFF_DIRECTORY,
            RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.APPLY_LEAVE,
            RoleCapability.APPROVE_LEAVE,
            RoleCapability.VIEW_FINANCE_ALL,
            RoleCapability.RECORD_FINANCE_PAYMENT,
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.VIEW_AUDIT_LOGS,
            RoleCapability.SWITCH_SCHOOL_TENANT,
            RoleCapability.SWITCH_BRANCH,
            RoleCapability.MANAGE_MODULES,
            RoleCapability.UPDATE_ACADEMIC_PERIOD
        ),
        PortalRole.HEADTEACHER to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.MARK_ATTENDANCE,
            RoleCapability.VIEW_ALL_REPORT_CARDS,
            RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
            RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.MANAGE_REPORT_TEMPLATES,
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.VIEW_STAFF_DIRECTORY,
            RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.APPLY_LEAVE,
            RoleCapability.APPROVE_LEAVE,
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.VIEW_AUDIT_LOGS,
            RoleCapability.SWITCH_BRANCH
        ),
        PortalRole.TEACHER to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.MARK_ATTENDANCE,
            RoleCapability.VIEW_ALL_REPORT_CARDS,
            RoleCapability.FILL_SUBMIT_REPORT_CARDS,
            RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.VIEW_STAFF_DIRECTORY,
            RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.APPLY_LEAVE,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.STUDENT to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.VIEW_OWN_PUBLISHED_REPORTS,
            RoleCapability.VIEW_COMMUNICATIONS
        ),
        PortalRole.PARENT to setOf(
            RoleCapability.VIEW_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.VIEW_OWN_PUBLISHED_REPORTS,
            RoleCapability.VIEW_OWN_INVOICES_ONLY,
            RoleCapability.VIEW_COMMUNICATIONS
        ),
        PortalRole.FINANCE_OFFICER to setOf(
            RoleCapability.VIEW_FINANCE_ALL,
            RoleCapability.RECORD_FINANCE_PAYMENT,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.LIBRARIAN to setOf(
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.TRANSPORT_MANAGER to setOf(
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.HR_MANAGER to setOf(
            RoleCapability.VIEW_STAFF_DIRECTORY,
            RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.APPLY_LEAVE,
            RoleCapability.APPROVE_LEAVE,
            RoleCapability.VIEW_PAYROLL_ONLY,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.WELFARE_NURSE to setOf(
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.ADMISSIONS_RECEPTION to setOf(
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.ENROLL_STUDENT,
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS
        )
    )

    fun has(role: PortalRole, capability: RoleCapability): Boolean =
        capabilitiesByRole[role]?.contains(capability) == true

    /**
     * True when the role is only ever allowed to see a single learner's records
     * (a student's own record, or a parent's child). Such roles must never be
     * silently shown another learner's data.
     */
    fun requiresLearnerScope(role: PortalRole): Boolean =
        has(role, RoleCapability.VIEW_OWN_PUBLISHED_REPORTS) && !has(role, RoleCapability.VIEW_ALL_STUDENTS)

    /**
     * Resolves the learner a scoped role has explicitly selected.
     * Returns null when nothing is selected, so callers show a "select a learner"
     * state instead of falling back to an arbitrary learner record.
     */
    fun resolveFocusStudent(students: List<Student>, focusStudentId: String?): Student? {
        val id = focusStudentId?.takeIf { it.isNotBlank() } ?: return null
        return students.firstOrNull { it.id == id }
    }

    /** Fee balances are finance data: only finance-capable roles may read them. */
    fun canViewLearnerFeeBalances(role: PortalRole): Boolean =
        has(role, RoleCapability.VIEW_FINANCE_ALL) || has(role, RoleCapability.VIEW_OWN_INVOICES_ONLY)

    /** Staff leave balances are HR data: only leave approvers may read them. */
    fun canViewStaffLeaveBalances(role: PortalRole): Boolean =
        has(role, RoleCapability.APPROVE_LEAVE)

    fun academicSubTabs(role: PortalRole, school: School?): List<String> {
        if (school == null) return emptyList()
        return buildList {
            if (has(role, RoleCapability.VIEW_ACADEMICS) &&
                (school.hasModule(GsmsModule.ACADEMICS) || school.hasModule(GsmsModule.EXAMS))
            ) {
                add("Academics & Exams")
            }
            if (has(role, RoleCapability.VIEW_ATTENDANCE) && school.hasModule(GsmsModule.ATTENDANCE)) {
                add("Attendance")
            }
            if ((has(role, RoleCapability.VIEW_ALL_REPORT_CARDS) || has(role, RoleCapability.VIEW_OWN_PUBLISHED_REPORTS)) &&
                school.hasModule(GsmsModule.REPORT_CARDS)
            ) {
                add("Report Cards")
            }
            if (has(role, RoleCapability.VIEW_REPORT_TEMPLATES) && school.hasModule(GsmsModule.REPORT_CARDS)) {
                add("Templates")
            }
        }
    }

    fun peopleSubTabs(role: PortalRole, school: School?): List<String> {
        if (school == null) return emptyList()
        return buildList {
            if (has(role, RoleCapability.VIEW_ALL_STUDENTS)) {
                add("Students")
            }
            if (has(role, RoleCapability.VIEW_STAFF_DIRECTORY) && school.hasModule(GsmsModule.HR_STAFF)) {
                add("Staff & Faculty")
            }
            if (has(role, RoleCapability.VIEW_LEAVE_WORKFLOW) && school.hasModule(GsmsModule.LEAVE_MANAGEMENT)) {
                add("Leave Workflow")
            }
        }
    }

    fun operationSections(role: PortalRole, school: School?): List<String> {
        if (school == null) return emptyList()
        val allowedByRole: Set<String> = when (role) {
            PortalRole.SUPER_ADMIN,
            PortalRole.SCHOOL_ADMIN -> setOf(
                "Finance", "Library", "Transport", "Welfare",
                "Discipline", "Assets", "Hostel", "Admissions", "Events"
            )
            PortalRole.HEADTEACHER -> setOf("Welfare", "Discipline", "Admissions", "Events")
            PortalRole.FINANCE_OFFICER -> setOf("Finance")
            PortalRole.PARENT -> setOf("Finance")
            PortalRole.HR_MANAGER -> setOf("Finance")
            PortalRole.LIBRARIAN -> setOf("Library")
            PortalRole.TRANSPORT_MANAGER -> setOf("Transport")
            PortalRole.WELFARE_NURSE -> setOf("Welfare", "Discipline", "Hostel")
            PortalRole.ADMISSIONS_RECEPTION -> setOf("Admissions", "Events")
            PortalRole.TEACHER,
            PortalRole.STUDENT -> emptySet()
        }
        return buildList {
            if ("Finance" in allowedByRole && school.hasModule(GsmsModule.FINANCE)) add("Finance")
            if ("Library" in allowedByRole && school.hasModule(GsmsModule.LIBRARY)) add("Library")
            if ("Transport" in allowedByRole && school.hasModule(GsmsModule.TRANSPORT)) add("Transport")
            if ("Welfare" in allowedByRole && school.hasModule(GsmsModule.HEALTH_WELFARE)) add("Welfare")
            if ("Discipline" in allowedByRole && school.hasModule(GsmsModule.DISCIPLINE)) add("Discipline")
            if ("Assets" in allowedByRole && school.hasModule(GsmsModule.INVENTORY_ASSETS)) add("Assets")
            if ("Hostel" in allowedByRole && school.hasModule(GsmsModule.HOSTEL_BOARDING)) add("Hostel")
            if ("Admissions" in allowedByRole && school.hasModule(GsmsModule.ADMISSIONS)) add("Admissions")
            if ("Events" in allowedByRole && school.hasModule(GsmsModule.EVENTS)) add("Events")
        }
    }

    fun analyticsSubTabs(role: PortalRole, school: School?): List<String> {
        if (school == null) return emptyList()
        return buildList {
            if ((has(role, RoleCapability.VIEW_ANALYTICS_CHARTS) || has(role, RoleCapability.VIEW_ALERTS)) &&
                school.hasModule(GsmsModule.ANALYTICS)
            ) {
                add(if (has(role, RoleCapability.VIEW_ANALYTICS_CHARTS)) "Analytics & Alerts" else "Role Alerts")
            }
            if (has(role, RoleCapability.VIEW_COMMUNICATIONS) && school.hasModule(GsmsModule.COMMUNICATION)) {
                add("Communications")
            }
            if (has(role, RoleCapability.VIEW_AUDIT_LOGS)) {
                add("Audit Logs")
            }
        }
    }

    fun visibleTabs(role: PortalRole, school: School?): List<GsmsTab> {
        return buildList {
            add(GsmsTab.DASHBOARD)
            if (academicSubTabs(role, school).isNotEmpty()) add(GsmsTab.ACADEMICS)
            if (peopleSubTabs(role, school).isNotEmpty()) add(GsmsTab.PEOPLE)
            if (operationSections(role, school).isNotEmpty()) add(GsmsTab.OPERATIONS)
            if (analyticsSubTabs(role, school).isNotEmpty()) add(GsmsTab.ANALYTICS)
            add(GsmsTab.AI_GOVERNANCE)
        }
    }

    fun filterAlertsForRole(role: PortalRole, alerts: List<EarlyWarningAlert>): List<EarlyWarningAlert> {
        return when (role) {
            PortalRole.SUPER_ADMIN,
            PortalRole.SCHOOL_ADMIN,
            PortalRole.HEADTEACHER -> alerts
            PortalRole.TEACHER -> alerts.filter { it.category in setOf("ACADEMIC", "ATTENDANCE", "BEHAVIOR") }
            PortalRole.FINANCE_OFFICER -> alerts.filter { it.category == "FINANCE" }
            PortalRole.HR_MANAGER -> alerts.filter { it.category == "STAFFING" }
            PortalRole.WELFARE_NURSE -> alerts.filter { it.category in setOf("WELFARE", "ATTENDANCE", "BEHAVIOR") }
            PortalRole.TRANSPORT_MANAGER -> alerts.filter { it.category in setOf("TRANSPORT", "ATTENDANCE") }
            else -> emptyList()
        }
    }

    fun filterFinanceForRole(
        role: PortalRole,
        entries: List<FinanceEntry>,
        focusStudent: Student?
    ): List<FinanceEntry> {
        return when (role) {
            PortalRole.SUPER_ADMIN,
            PortalRole.SCHOOL_ADMIN,
            PortalRole.FINANCE_OFFICER -> entries
            PortalRole.HR_MANAGER -> entries.filter { it.entryType == "PAYROLL" }
            PortalRole.PARENT -> {
                // No learner selected means no invoices: never fall back to the whole ledger.
                val studentName = focusStudent?.fullName ?: return emptyList()
                entries.filter {
                    it.entryType == "INVOICE" && it.partyName.equals(studentName, ignoreCase = true)
                }
            }
            else -> emptyList()
        }
    }

    fun filterCommunicationsForRole(role: PortalRole, items: List<CommunicationItem>): List<CommunicationItem> {
        return when (role) {
            PortalRole.STUDENT -> items.filter {
                it.audience.contains("All", ignoreCase = true) ||
                    it.audience.contains("Student", ignoreCase = true) ||
                    it.audience.contains("Grade", ignoreCase = true)
            }
            PortalRole.PARENT -> items.filter {
                it.audience.contains("All", ignoreCase = true) ||
                    it.audience.contains("Parent", ignoreCase = true) ||
                    it.audience.contains("Guardian", ignoreCase = true)
            }
            else -> items
        }
    }

    fun tabDisplayLabel(role: PortalRole, tab: GsmsTab): String {
        return when (tab) {
            GsmsTab.OPERATIONS -> when (role) {
                PortalRole.PARENT -> "Fees"
                PortalRole.FINANCE_OFFICER -> "Finance"
                PortalRole.LIBRARIAN -> "Library"
                PortalRole.TRANSPORT_MANAGER -> "Transport"
                PortalRole.WELFARE_NURSE -> "Welfare"
                PortalRole.ADMISSIONS_RECEPTION -> "Admissions"
                PortalRole.HR_MANAGER -> "Payroll"
                else -> tab.label
            }
            GsmsTab.ANALYTICS -> when (role) {
                PortalRole.STUDENT, PortalRole.PARENT -> "Notices"
                else -> tab.label
            }
            GsmsTab.AI_GOVERNANCE -> when (role) {
                PortalRole.SUPER_ADMIN, PortalRole.SCHOOL_ADMIN -> "AI & Admin"
                else -> "AI & Profile"
            }
            else -> tab.label
        }
    }

    fun accessSummary(role: PortalRole): String {
        return when (role) {
            PortalRole.SUPER_ADMIN -> "Global tenant provisioning, module governance, cross-school KPIs & audit trails (operational edits delegated to School Admin)."
            PortalRole.SCHOOL_ADMIN -> "Full single-school administration, module toggles, period settings, staff/student directories, finance & approvals."
            PortalRole.HEADTEACHER -> "Academic leadership, report card approvals & publishing, leave sign-off, early-warning interventions & audit logs."
            PortalRole.TEACHER -> "Timetables, exams/marks, attendance roll call, online report drafting/submission & personal leave applications."
            PortalRole.STUDENT -> "Read-only personal academic schedule, attendance summary, published report cards & school notices."
            PortalRole.PARENT -> "Read-only child progress, attendance, published PDF report cards, fee invoices & parent notices."
            PortalRole.FINANCE_OFFICER -> "Fee structures, invoices, payment posting, payroll ledger, overdue alerts & finance notices."
            PortalRole.LIBRARIAN -> "Library catalog, book borrowing/overdue tracking & reader notices."
            PortalRole.TRANSPORT_MANAGER -> "Fleet readiness, route management, trip logs & transport delay notices."
            PortalRole.HR_MANAGER -> "Staff directory, leave conflict checks, leave approvals, payroll view & staffing alerts."
            PortalRole.WELFARE_NURSE -> "Student health/welfare cases, discipline & hostel logs, attendance wellness alerts & parent outreach."
            PortalRole.ADMISSIONS_RECEPTION -> "Student enrollment, admissions pipeline, campus events & prospective parent outreach."
        }
    }
}

fun PortalRole.has(capability: RoleCapability): Boolean = RolePolicy.has(this, capability)
