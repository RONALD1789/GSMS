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
    VIEW_ASSIGNED_STUDENTS,
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
    UPDATE_ACADEMIC_PERIOD,
    MANAGE_BRANCHES,
    MANAGE_USER_PERMISSIONS,
    MODERATE_EXAMS,
    MANAGE_PROCUREMENT,
    MANAGE_INVENTORY,
    MANAGE_ICT,
    MANAGE_COUNSELLING,
    MANAGE_HEALTH,
    MANAGE_ADMISSIONS
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
            RoleCapability.UPDATE_ACADEMIC_PERIOD,
            RoleCapability.MANAGE_BRANCHES,
            RoleCapability.MANAGE_USER_PERMISSIONS
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
            RoleCapability.UPDATE_ACADEMIC_PERIOD,
            RoleCapability.MANAGE_BRANCHES,
            RoleCapability.MANAGE_USER_PERMISSIONS
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
            RoleCapability.VIEW_ASSIGNED_STUDENTS,
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
            RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.MANAGE_HEALTH
        ),
        PortalRole.ADMISSIONS_RECEPTION to setOf(
            RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.ENROLL_STUDENT,
            RoleCapability.EDIT_OPERATIONS,
            RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.MANAGE_ADMISSIONS
        ),
        PortalRole.DEPUTY_HEADTEACHER to setOf(
            RoleCapability.VIEW_ACADEMICS, RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE, RoleCapability.MARK_ATTENDANCE,
            RoleCapability.VIEW_ALL_REPORT_CARDS, RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
            RoleCapability.VIEW_REPORT_TEMPLATES, RoleCapability.VIEW_ALL_STUDENTS,
            RoleCapability.MODERATE_EXAMS,
            RoleCapability.VIEW_STAFF_DIRECTORY, RoleCapability.VIEW_LEAVE_WORKFLOW,
            RoleCapability.VIEW_ALERTS, RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS, RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.CLASS_TEACHER to setOf(
            RoleCapability.VIEW_ACADEMICS, RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE, RoleCapability.MARK_ATTENDANCE,
            RoleCapability.FILL_SUBMIT_REPORT_CARDS, RoleCapability.VIEW_ASSIGNED_STUDENTS, RoleCapability.APPLY_LEAVE, RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.VIEW_ALERTS, RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_COMMUNICATIONS, RoleCapability.COMPOSE_COMMUNICATIONS
        ),
        PortalRole.PROCUREMENT_OFFICER to setOf(
            RoleCapability.EDIT_OPERATIONS, RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS, RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_COMMUNICATIONS, RoleCapability.COMPOSE_COMMUNICATIONS,
            RoleCapability.MANAGE_PROCUREMENT
        ),
        PortalRole.STOREKEEPER to setOf(
            RoleCapability.EDIT_OPERATIONS, RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS, RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.MANAGE_INVENTORY
        ),
        PortalRole.EXAMINATION_OFFICER to setOf(
            RoleCapability.VIEW_ACADEMICS, RoleCapability.EDIT_ACADEMICS,
            RoleCapability.VIEW_ATTENDANCE, RoleCapability.VIEW_ALL_REPORT_CARDS,
            RoleCapability.FILL_SUBMIT_REPORT_CARDS, RoleCapability.VIEW_REPORT_TEMPLATES,
            RoleCapability.MANAGE_REPORT_TEMPLATES, RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.VIEW_ALERTS, RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.MODERATE_EXAMS
        ),
        PortalRole.SCHOOL_COUNSELLOR to setOf(
            RoleCapability.VIEW_ALL_STUDENTS, RoleCapability.VIEW_ATTENDANCE,
            RoleCapability.EDIT_OPERATIONS, RoleCapability.VIEW_ALERTS,
            RoleCapability.MANAGE_ALERTS, RoleCapability.VIEW_COMMUNICATIONS,
            RoleCapability.COMPOSE_COMMUNICATIONS, RoleCapability.MANAGE_COUNSELLING
        ),
        PortalRole.ICT_ADMIN to setOf(
            RoleCapability.EDIT_OPERATIONS, RoleCapability.VIEW_AUDIT_LOGS,
            RoleCapability.VIEW_ALERTS, RoleCapability.MANAGE_ALERTS,
            RoleCapability.VIEW_ANALYTICS_CHARTS,
            RoleCapability.MANAGE_ICT, RoleCapability.MANAGE_USER_PERMISSIONS
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
            if (has(role, RoleCapability.VIEW_ALL_STUDENTS) || has(role, RoleCapability.VIEW_ASSIGNED_STUDENTS)) {
                add(if (has(role, RoleCapability.VIEW_ALL_STUDENTS)) "Students" else "My Assigned Students")
            }
            if (has(role, RoleCapability.VIEW_STAFF_DIRECTORY) && school.hasModule(GsmsModule.HR_STAFF)) {
                add("Staff & Faculty")
            }
            if (has(role, RoleCapability.VIEW_LEAVE_WORKFLOW) && school.hasModule(GsmsModule.LEAVE_MANAGEMENT)) {
                add("Leave Workflow")
            } else if (has(role, RoleCapability.APPLY_LEAVE) && school.hasModule(GsmsModule.LEAVE_MANAGEMENT)) {
                add("My Leave")
            }
        }
    }

    fun academicSubTabs(role: PortalRole, school: School?, extra: Set<RoleCapability>): List<String> {
        val tabs = academicSubTabs(role, school).toMutableList()
        if (school == null) return emptyList()
        if (RoleCapability.VIEW_ACADEMICS in extra && (school.hasModule(GsmsModule.ACADEMICS) || school.hasModule(GsmsModule.EXAMS)) && "Academics & Exams" !in tabs) tabs.add(0, "Academics & Exams")
        if (RoleCapability.VIEW_ATTENDANCE in extra && school.hasModule(GsmsModule.ATTENDANCE) && "Attendance" !in tabs) tabs.add("Attendance")
        if ((RoleCapability.VIEW_ALL_REPORT_CARDS in extra || RoleCapability.VIEW_OWN_PUBLISHED_REPORTS in extra) && school.hasModule(GsmsModule.REPORT_CARDS) && "Report Cards" !in tabs) tabs.add("Report Cards")
        if (RoleCapability.VIEW_REPORT_TEMPLATES in extra && school.hasModule(GsmsModule.REPORT_CARDS) && "Templates" !in tabs) tabs.add("Templates")
        return tabs
    }

    fun peopleSubTabs(role: PortalRole, school: School?, extra: Set<RoleCapability>): List<String> {
        val tabs = peopleSubTabs(role, school).toMutableList()
        if (RoleCapability.VIEW_ALL_STUDENTS in extra && "Students" !in tabs) tabs.add(0, "Students")
        if (RoleCapability.VIEW_ASSIGNED_STUDENTS in extra && tabs.none { it == "Students" || it == "My Assigned Students" }) tabs.add(0, "My Assigned Students")
        if (RoleCapability.VIEW_STAFF_DIRECTORY in extra && school?.hasModule(GsmsModule.HR_STAFF) == true && "Staff & Faculty" !in tabs) tabs.add("Staff & Faculty")
        if (RoleCapability.VIEW_LEAVE_WORKFLOW in extra && school?.hasModule(GsmsModule.LEAVE_MANAGEMENT) == true && "Leave Workflow" !in tabs) tabs.add("Leave Workflow")
        else if (RoleCapability.APPLY_LEAVE in extra && school?.hasModule(GsmsModule.LEAVE_MANAGEMENT) == true && "My Leave" !in tabs && "Leave Workflow" !in tabs) tabs.add("My Leave")
        return tabs
    }

    fun operationSections(role: PortalRole, school: School?): List<String> {
        if (school == null) return emptyList()
        val allowedByRole: Set<String> = when (role) {
            PortalRole.SUPER_ADMIN,
            PortalRole.SCHOOL_ADMIN -> setOf(
                "Finance", "Procurement", "Stores", "Library", "Transport", "Welfare",
                "Counselling", "Discipline", "Assets", "Hostel", "Front Desk", "Admissions", "Events", "Examinations", "ICT Systems"
            )
            PortalRole.HEADTEACHER,
            PortalRole.DEPUTY_HEADTEACHER -> setOf("Welfare", "Counselling", "Discipline", "Admissions", "Events", "Examinations")
            PortalRole.CLASS_TEACHER -> emptySet()
            PortalRole.PROCUREMENT_OFFICER -> setOf("Procurement", "Stores")
            PortalRole.STOREKEEPER -> setOf("Procurement", "Stores")
            PortalRole.EXAMINATION_OFFICER -> setOf("Examinations")
            PortalRole.SCHOOL_COUNSELLOR -> setOf("Welfare", "Counselling", "Discipline")
            PortalRole.ICT_ADMIN -> setOf("Stores", "ICT Systems")
            PortalRole.FINANCE_OFFICER -> setOf("Finance", "Procurement")
            PortalRole.PARENT -> setOf("Finance")
            PortalRole.HR_MANAGER -> setOf("Finance")
            PortalRole.LIBRARIAN -> setOf("Library")
            PortalRole.TRANSPORT_MANAGER -> setOf("Transport")
            PortalRole.WELFARE_NURSE -> setOf("Welfare", "Counselling", "Discipline", "Hostel")
            PortalRole.ADMISSIONS_RECEPTION -> setOf("Front Desk", "Admissions", "Events")
            PortalRole.TEACHER,
            PortalRole.STUDENT -> emptySet()
            else -> emptySet()
        }
        return buildList {
            if ("Finance" in allowedByRole && school.hasModule(GsmsModule.FINANCE)) add("Finance")
            if ("Procurement" in allowedByRole && school.hasModule(GsmsModule.PROCUREMENT)) add("Procurement")
            if ("Stores" in allowedByRole && school.hasModule(GsmsModule.INVENTORY_ASSETS)) add("Stores")
            if ("Library" in allowedByRole && school.hasModule(GsmsModule.LIBRARY)) add("Library")
            if ("Transport" in allowedByRole && school.hasModule(GsmsModule.TRANSPORT)) add("Transport")
            if ("Welfare" in allowedByRole && school.hasModule(GsmsModule.HEALTH_WELFARE)) add("Welfare")
            if ("Counselling" in allowedByRole && school.hasModule(GsmsModule.DISCIPLINE)) add("Counselling")
            if ("Discipline" in allowedByRole && school.hasModule(GsmsModule.DISCIPLINE)) add("Discipline")
            if ("Assets" in allowedByRole && school.hasModule(GsmsModule.INVENTORY_ASSETS)) add("Assets")
            if ("Hostel" in allowedByRole && school.hasModule(GsmsModule.HOSTEL_BOARDING)) add("Hostel")
            if ("Front Desk" in allowedByRole && school.hasModule(GsmsModule.ADMISSIONS)) add("Front Desk")
            if ("Admissions" in allowedByRole && school.hasModule(GsmsModule.ADMISSIONS)) add("Admissions")
            if ("Events" in allowedByRole && school.hasModule(GsmsModule.EVENTS)) add("Events")
            if ("Examinations" in allowedByRole && school.hasModule(GsmsModule.EXAMS)) add("Examinations")
            if ("ICT Systems" in allowedByRole && school.hasModule(GsmsModule.ICT_SYSTEMS)) add("ICT Systems")
        }
    }

    fun operationSections(role: PortalRole, school: School?, extra: Set<RoleCapability>): List<String> {
        val base = operationSections(role, school).toMutableList()
        if (school == null) return emptyList()
        if (RoleCapability.MANAGE_PROCUREMENT in extra && school.hasModule(GsmsModule.PROCUREMENT) && "Procurement" !in base) base.add("Procurement")
        if (RoleCapability.MANAGE_INVENTORY in extra && school.hasModule(GsmsModule.INVENTORY_ASSETS) && "Stores" !in base) base.add("Stores")
        if (RoleCapability.MANAGE_ICT in extra && school.hasModule(GsmsModule.ICT_SYSTEMS) && "ICT Systems" !in base) base.add("ICT Systems")
        if (RoleCapability.MODERATE_EXAMS in extra && school.hasModule(GsmsModule.EXAMS) && "Examinations" !in base) base.add("Examinations")
        return base
    }

    fun canManageOperationSection(
        role: PortalRole,
        section: String,
        extra: Set<RoleCapability> = emptySet()
    ): Boolean {
        val capability = when (section) {
            "Procurement" -> RoleCapability.MANAGE_PROCUREMENT
            "Stores", "Assets" -> RoleCapability.MANAGE_INVENTORY
            "Examinations" -> RoleCapability.MODERATE_EXAMS
            "ICT", "ICT Systems" -> RoleCapability.MANAGE_ICT
            "Counselling", "Discipline" -> RoleCapability.MANAGE_COUNSELLING
            "Welfare", "Hostel" -> RoleCapability.MANAGE_HEALTH
            "Front Desk", "Admissions" -> RoleCapability.MANAGE_ADMISSIONS
            else -> null
        }
        if (role == PortalRole.SCHOOL_ADMIN || role == PortalRole.HEADTEACHER) return true
        if (capability != null && (has(role, capability) || capability in extra)) return true
        return section in setOf("Library", "Transport", "Events") && has(role, RoleCapability.EDIT_OPERATIONS)
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


    fun baseCapabilities(role: PortalRole): Set<RoleCapability> = capabilitiesByRole[role].orEmpty()

    fun analyticsSubTabs(role: PortalRole, school: School?, extra: Set<RoleCapability>): List<String> =
        analyticsSubTabs(role, school).let { tabs ->
            if (RoleCapability.VIEW_ANALYTICS_CHARTS in extra && school?.hasModule(GsmsModule.ANALYTICS) == true && "Analytics & Alerts" !in tabs) {
                listOf("Analytics & Alerts") + tabs
            } else if (RoleCapability.VIEW_COMMUNICATIONS in extra && school?.hasModule(GsmsModule.COMMUNICATION) == true && "Communications" !in tabs) {
                tabs + "Communications"
            } else tabs
        }

    fun visibleTabs(role: PortalRole, school: School?, extra: Set<RoleCapability>): List<GsmsTab> =
        buildList {
            add(GsmsTab.DASHBOARD)
            if (academicSubTabs(role, school, extra).isNotEmpty()) add(GsmsTab.ACADEMICS)
            if (peopleSubTabs(role, school, extra).isNotEmpty()) add(GsmsTab.PEOPLE)
            if (operationSections(role, school, extra).isNotEmpty() || RoleCapability.EDIT_OPERATIONS in extra || RoleCapability.VIEW_FINANCE_ALL in extra) add(GsmsTab.OPERATIONS)
            if (analyticsSubTabs(role, school, extra).isNotEmpty()) add(GsmsTab.ANALYTICS)
            add(GsmsTab.AI_GOVERNANCE)
        }

    fun filterStudentsForRole(
        role: PortalRole,
        students: List<Student>,
        assignedClasses: List<String>,
        linkedStudentIds: List<String>
    ): List<Student> = when {
        role == PortalRole.PARENT || role == PortalRole.STUDENT ->
            students.filter { it.id in linkedStudentIds }
        assignedClasses.isNotEmpty() && !has(role, RoleCapability.VIEW_ALL_STUDENTS) ->
            students.filter { it.className in assignedClasses }
        else -> students
    }

    fun filterAlertsForRole(role: PortalRole, alerts: List<EarlyWarningAlert>): List<EarlyWarningAlert> {
        return when (role) {
            PortalRole.SUPER_ADMIN,
            PortalRole.SCHOOL_ADMIN,
            PortalRole.HEADTEACHER,
            PortalRole.DEPUTY_HEADTEACHER -> alerts
            PortalRole.CLASS_TEACHER,
            PortalRole.TEACHER -> alerts.filter { it.category in setOf("ACADEMIC", "ATTENDANCE", "BEHAVIOR") }
            PortalRole.PROCUREMENT_OFFICER -> alerts.filter { it.category in setOf("PROCUREMENT", "FINANCE", "STOCK") }
            PortalRole.STOREKEEPER -> alerts.filter { it.category in setOf("STOCK", "ASSET", "PROCUREMENT") }
            PortalRole.EXAMINATION_OFFICER -> alerts.filter { it.category in setOf("ACADEMIC", "EXAM", "ATTENDANCE") }
            PortalRole.SCHOOL_COUNSELLOR -> alerts.filter { it.category in setOf("WELFARE", "ATTENDANCE", "BEHAVIOR", "SAFEGUARDING") }
            PortalRole.ICT_ADMIN -> alerts.filter { it.category in setOf("ICT", "SYSTEM", "SECURITY") }
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
                val linkedNames = listOfNotNull(
                    focusStudent?.fullName?.takeIf { it.isNotBlank() },
                    focusStudent?.parentName?.takeIf { it.isNotBlank() }
                ).toSet()
                if (linkedNames.isEmpty()) return emptyList()
                entries.filter {
                    it.entryType == "INVOICE" && linkedNames.any { name -> it.partyName.equals(name, ignoreCase = true) }
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
                PortalRole.PROCUREMENT_OFFICER -> "Procurement"
                PortalRole.STOREKEEPER -> "Stores"
                PortalRole.EXAMINATION_OFFICER -> "Examinations"
                PortalRole.SCHOOL_COUNSELLOR -> "Pastoral Care"
                PortalRole.ICT_ADMIN -> "ICT Systems"
                PortalRole.DEPUTY_HEADTEACHER -> "School Operations"
                PortalRole.CLASS_TEACHER -> "Classroom"
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
            PortalRole.DEPUTY_HEADTEACHER -> "Curriculum and timetable coordination, exam moderation, attendance oversight, delegated report review and school operations."
            PortalRole.CLASS_TEACHER -> "Assigned-class roster, daily attendance, class progress, report drafting, parent communication and learner follow-up."
            PortalRole.PROCUREMENT_OFFICER -> "Purchase requisitions, vendor records, quotation review, purchase orders, approval tracking and supplier follow-up."
            PortalRole.STOREKEEPER -> "Stock register, goods received, stock issues, asset custody, low-stock alerts and reorder tracking."
            PortalRole.EXAMINATION_OFFICER -> "Exam calendar, invigilation schedules, mark moderation, grading oversight and report-template administration."
            PortalRole.SCHOOL_COUNSELLOR -> "Pastoral cases, learner wellbeing, safeguarding referrals, intervention notes and follow-up records."
            PortalRole.ICT_ADMIN -> "ICT support tickets, campus devices, system incidents, access provisioning and security audit oversight."
            PortalRole.TEACHER -> "Timetables, exams/marks, attendance roll call, online report drafting/submission & personal leave applications."
            PortalRole.STUDENT -> "Read-only personal academic schedule, attendance summary, published report cards & school notices."
            PortalRole.PARENT -> "Read-only child progress, attendance, published PDF report cards, fee invoices & parent notices."
            PortalRole.FINANCE_OFFICER -> "Fee structures, invoices, payment posting, payroll ledger, overdue alerts & finance notices."
            PortalRole.LIBRARIAN -> "Library catalog, book borrowing/overdue tracking & reader notices."
            PortalRole.TRANSPORT_MANAGER -> "Fleet readiness, route management, trip logs & transport delay notices."
            PortalRole.HR_MANAGER -> "Staff directory, leave conflict checks, leave approvals, payroll view & staffing alerts."
            PortalRole.WELFARE_NURSE -> "Student health/welfare cases, discipline & hostel logs, attendance wellness alerts & parent outreach."
            PortalRole.ADMISSIONS_RECEPTION -> "Student enrollment, admissions pipeline, campus events & prospective parent outreach."
            else -> "${role.shortTitle} access is limited to assigned modules and records."
        }
    }
}

fun PortalRole.has(capability: RoleCapability): Boolean = RolePolicy.has(this, capability)
fun PortalRole.has(capability: RoleCapability, extra: Set<RoleCapability>): Boolean =
    RolePolicy.has(this, capability) || capability in extra

val RoleCapability.label: String
    get() = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

val RoleCapability.category: String
    get() = when (this) {
        RoleCapability.VIEW_ACADEMICS, RoleCapability.EDIT_ACADEMICS,
        RoleCapability.VIEW_ATTENDANCE, RoleCapability.MARK_ATTENDANCE,
        RoleCapability.VIEW_ALL_REPORT_CARDS, RoleCapability.VIEW_OWN_PUBLISHED_REPORTS,
        RoleCapability.FILL_SUBMIT_REPORT_CARDS, RoleCapability.APPROVE_PUBLISH_REPORT_CARDS,
        RoleCapability.VIEW_REPORT_TEMPLATES, RoleCapability.MANAGE_REPORT_TEMPLATES -> "Academics & Reports"
        RoleCapability.VIEW_ALL_STUDENTS, RoleCapability.VIEW_ASSIGNED_STUDENTS, RoleCapability.ENROLL_STUDENT,
        RoleCapability.VIEW_STAFF_DIRECTORY, RoleCapability.VIEW_LEAVE_WORKFLOW,
        RoleCapability.APPLY_LEAVE, RoleCapability.APPROVE_LEAVE -> "People & HR"
        RoleCapability.VIEW_FINANCE_ALL, RoleCapability.VIEW_PAYROLL_ONLY,
        RoleCapability.VIEW_OWN_INVOICES_ONLY, RoleCapability.RECORD_FINANCE_PAYMENT,
        RoleCapability.EDIT_OPERATIONS, RoleCapability.MODERATE_EXAMS, RoleCapability.MANAGE_PROCUREMENT, RoleCapability.MANAGE_INVENTORY, RoleCapability.MANAGE_ICT, RoleCapability.MANAGE_COUNSELLING, RoleCapability.MANAGE_HEALTH, RoleCapability.MANAGE_ADMISSIONS -> "Operations & Finance"
        RoleCapability.VIEW_ANALYTICS_CHARTS, RoleCapability.VIEW_ALERTS,
        RoleCapability.MANAGE_ALERTS, RoleCapability.VIEW_COMMUNICATIONS,
        RoleCapability.COMPOSE_COMMUNICATIONS, RoleCapability.VIEW_AUDIT_LOGS -> "Analytics & Communications"
        else -> "Governance"
    }
