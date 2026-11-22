package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class RolePolicyUnitTest {

    private val fullSchool = School(
        id = "school_1",
        name = "Oakridge",
        enabledModules = GsmsModule.defaultEnabledNames
    )

    @Test
    fun allNineteenPortals_haveUniqueDashboardsAndBaseCapabilities() {
        assertEquals(19, PortalRole.entries.size)
        PortalRole.entries.forEach { role ->
            assertTrue("Missing capabilities for $role", RolePolicy.baseCapabilities(role).isNotEmpty())
            assertTrue("Missing visible tabs for $role", RolePolicy.visibleTabs(role, fullSchool).isNotEmpty())
            assertTrue("Missing access summary for $role", RolePolicy.accessSummary(role).isNotBlank())
        }
    }

    @Test
    fun superAdmin_hasGlobalTenantAndAuditAccess_withoutOperationalEdits() {
        val role = PortalRole.SUPER_ADMIN
        assertTrue(role.has(RoleCapability.CREATE_SCHOOL_TENANT))
        assertTrue(role.has(RoleCapability.MANAGE_MODULES))
        assertTrue(role.has(RoleCapability.MANAGE_USER_PERMISSIONS))
        assertTrue(role.has(RoleCapability.VIEW_AUDIT_LOGS))
        assertFalse(role.has(RoleCapability.APPROVE_PUBLISH_REPORT_CARDS))
        assertFalse(role.has(RoleCapability.APPROVE_LEAVE))
        assertFalse(role.has(RoleCapability.RECORD_FINANCE_PAYMENT))
    }

    @Test
    fun teacher_and_classTeacher_areScopedToAssignedClassesAndSubjects() {
        val role = PortalRole.TEACHER
        assertTrue(role.has(RoleCapability.MARK_ATTENDANCE))
        assertTrue(role.has(RoleCapability.FILL_SUBMIT_REPORT_CARDS))
        assertTrue(role.has(RoleCapability.APPLY_LEAVE))

        assertFalse(role.has(RoleCapability.APPROVE_PUBLISH_REPORT_CARDS))
        assertFalse(role.has(RoleCapability.MANAGE_REPORT_TEMPLATES))
        assertFalse(role.has(RoleCapability.APPROVE_LEAVE))
        assertFalse(role.has(RoleCapability.MANAGE_MODULES))
        assertFalse(role.has(RoleCapability.VIEW_AUDIT_LOGS))
        assertFalse(GsmsTab.OPERATIONS in RolePolicy.visibleTabs(role, fullSchool))

        assertFalse(GsmsTab.OPERATIONS in RolePolicy.visibleTabs(PortalRole.CLASS_TEACHER, fullSchool))
        assertEquals(emptyList<String>(), RolePolicy.operationSections(PortalRole.CLASS_TEACHER, fullSchool))

        val students = listOf(
            Student(id = "stu_1", fullName = "Maya Lin", className = "Grade 10A"),
            Student(id = "stu_3", fullName = "Amina Yusuf", className = "Grade 11B")
        )
        val scopedStudents = RolePolicy.filterStudentsForRole(PortalRole.TEACHER, students, listOf("Grade 10A"))
        assertEquals(1, scopedStudents.size)
        assertEquals("stu_1", scopedStudents.first().id)

        val academics = listOf(
            AcademicRecord(id = "a1", title = "Calculus", className = "Grade 10A", subjectName = "Mathematics"),
            AcademicRecord(id = "a2", title = "Literature", className = "Grade 10A", subjectName = "English"),
            AcademicRecord(id = "a3", title = "Biology", className = "Grade 11B", subjectName = "Biology")
        )
        val teacherAcademics = RolePolicy.filterAcademicsForRole(
            PortalRole.TEACHER,
            academics,
            assignedClasses = listOf("Grade 10A"),
            assignedSubjects = listOf("Mathematics")
        )
        assertEquals(1, teacherAcademics.size)
        assertEquals("a1", teacherAcademics.first().id)
    }

    @Test
    fun student_and_parent_areStrictlyReadOnly_andScopedToLinkedChildren() {
        val studentTabs = RolePolicy.visibleTabs(PortalRole.STUDENT, fullSchool)
        assertEquals(
            listOf(GsmsTab.DASHBOARD, GsmsTab.ACADEMICS, GsmsTab.ANALYTICS, GsmsTab.AI_GOVERNANCE),
            studentTabs
        )
        assertFalse(PortalRole.STUDENT.has(RoleCapability.COMPOSE_COMMUNICATIONS))
        assertFalse(PortalRole.STUDENT.has(RoleCapability.EDIT_ACADEMICS))
        assertFalse(PortalRole.STUDENT.has(RoleCapability.VIEW_ALL_REPORT_CARDS))
        assertTrue(PortalRole.STUDENT.has(RoleCapability.VIEW_OWN_PUBLISHED_REPORTS))

        val parentTabs = RolePolicy.visibleTabs(PortalRole.PARENT, fullSchool)
        assertTrue(GsmsTab.OPERATIONS in parentTabs)
        assertEquals(listOf("Finance"), RolePolicy.operationSections(PortalRole.PARENT, fullSchool))
        assertFalse(PortalRole.PARENT.has(RoleCapability.RECORD_FINANCE_PAYMENT))
        assertFalse(PortalRole.PARENT.has(RoleCapability.VIEW_AUDIT_LOGS))

        val linkedChild = Student(id = "stu_1", fullName = "Maya Lin", parentName = "Wei Lin", className = "Grade 10A")
        val allStudents = listOf(linkedChild, Student(id = "stu_2", fullName = "Liam O'Connor", parentName = "Claire O'Connor", className = "Grade 10A"))
        val filteredChildren = RolePolicy.filterStudentsForRole(PortalRole.PARENT, allStudents, linkedStudentIds = listOf("stu_1"))
        assertEquals(listOf("stu_1"), filteredChildren.map { it.id })

        val financeEntries = listOf(
            FinanceEntry(id = "1", entryType = "INVOICE", partyName = "Wei Lin", title = "Term 1 Tuition - Maya Lin"),
            FinanceEntry(id = "2", entryType = "INVOICE", partyName = "Claire O'Connor", title = "Term 1 Tuition - Liam O'Connor"),
            FinanceEntry(id = "3", entryType = "PAYROLL", partyName = "Dr. Samuel")
        )
        val parentFinance = RolePolicy.filterFinanceForRole(PortalRole.PARENT, financeEntries, filteredChildren)
        assertEquals(1, parentFinance.size)
        assertEquals("Wei Lin", parentFinance.first().partyName)

        val reports = listOf(
            ReportCard(id = "r1", studentId = "stu_1", status = ReportStatus.PUBLISHED.name),
            ReportCard(id = "r2", studentId = "stu_1", status = ReportStatus.SUBMITTED.name),
            ReportCard(id = "r3", studentId = "stu_2", status = ReportStatus.PUBLISHED.name)
        )
        val visibleParentReports = RolePolicy.filterReportCardsForRole(PortalRole.PARENT, reports, linkedStudents = filteredChildren)
        assertEquals(listOf("r1"), visibleParentReports.map { it.id })
    }

    @Test
    fun specialistAndNewlyAddedPortals_onlySeeTheirAuthorizedModules() {
        assertEquals(listOf("Finance", "Procurement"), RolePolicy.operationSections(PortalRole.FINANCE_OFFICER, fullSchool))
        assertEquals(listOf("Library"), RolePolicy.operationSections(PortalRole.LIBRARIAN, fullSchool))
        assertEquals(listOf("Transport"), RolePolicy.operationSections(PortalRole.TRANSPORT_MANAGER, fullSchool))
        assertEquals(listOf("Welfare", "Counselling", "Discipline", "Hostel"), RolePolicy.operationSections(PortalRole.WELFARE_NURSE, fullSchool))
        assertEquals(listOf("Front Desk", "Admissions", "Events"), RolePolicy.operationSections(PortalRole.ADMISSIONS_RECEPTION, fullSchool))
        assertEquals(listOf("Finance", "Procurement", "Stores"), RolePolicy.operationSections(PortalRole.PROCUREMENT_OFFICER, fullSchool))
        assertEquals(listOf("Procurement", "Stores"), RolePolicy.operationSections(PortalRole.STOREKEEPER, fullSchool))
        assertEquals(listOf("Welfare", "Counselling", "Discipline"), RolePolicy.operationSections(PortalRole.SCHOOL_COUNSELLOR, fullSchool))
        assertEquals(listOf("Stores", "ICT Systems"), RolePolicy.operationSections(PortalRole.ICT_ADMIN, fullSchool))

        assertTrue(PortalRole.DEPUTY_HEADTEACHER.has(RoleCapability.MODERATE_EXAMS))
        assertTrue(PortalRole.EXAMINATION_OFFICER.has(RoleCapability.MANAGE_REPORT_TEMPLATES))
        assertFalse(PortalRole.EXAMINATION_OFFICER.has(RoleCapability.APPROVE_PUBLISH_REPORT_CARDS))
        assertTrue(PortalRole.ICT_ADMIN.has(RoleCapability.MANAGE_USER_PERMISSIONS))
        assertFalse(PortalRole.ICT_ADMIN.has(RoleCapability.RECORD_FINANCE_PAYMENT))
    }

    @Test
    fun delegatedExtraPermissions_grantAdditionalAccessWithoutChangingPrimaryRole() {
        val teacherProfile = UserProfile(
            userId = "u1",
            primaryRole = PortalRole.TEACHER.name,
            activeRole = PortalRole.SCHOOL_ADMIN.name, // Attempting mismatch should not override non-SUPER_ADMIN
            extraPermissions = listOf(RoleCapability.APPROVE_LEAVE.name, RoleCapability.MANAGE_PROCUREMENT.name)
        )
        // Effective role MUST stay locked to primaryRole TEACHER
        assertEquals(PortalRole.TEACHER, teacherProfile.resolvedEffectiveRole())
        val extra = teacherProfile.resolvedExtraCapabilities()
        assertTrue(teacherProfile.resolvedEffectiveRole().has(RoleCapability.APPROVE_LEAVE, extra))
        assertTrue(teacherProfile.resolvedEffectiveRole().has(RoleCapability.MANAGE_PROCUREMENT, extra))
        assertFalse(teacherProfile.resolvedEffectiveRole().has(RoleCapability.MANAGE_MODULES, extra))

        // Delegated capability unlocks Procurement operations tab while preserving TEACHER primary role
        val teacherTabsWithExtra = RolePolicy.visibleTabs(PortalRole.TEACHER, fullSchool, extra)
        assertTrue(GsmsTab.OPERATIONS in teacherTabsWithExtra)
        assertEquals(listOf("Procurement"), RolePolicy.operationSections(PortalRole.TEACHER, fullSchool, extra))
    }

    @Test
    fun disabledModules_hideTabsEvenForPermittedRoles() {
        val minimalSchool = fullSchool.copy(
            enabledModules = listOf(GsmsModule.COMMUNICATION.name)
        )
        val librarianTabs = RolePolicy.visibleTabs(PortalRole.LIBRARIAN, minimalSchool)
        assertFalse(GsmsTab.OPERATIONS in librarianTabs)
        assertTrue(GsmsTab.ANALYTICS in librarianTabs)
    }

    @Test
    fun learnerScopedRoles_requireAnExplicitLearnerSelection() {
        assertTrue(RolePolicy.requiresLearnerScope(PortalRole.STUDENT))
        assertTrue(RolePolicy.requiresLearnerScope(PortalRole.PARENT))

        assertFalse(RolePolicy.requiresLearnerScope(PortalRole.TEACHER))
        assertFalse(RolePolicy.requiresLearnerScope(PortalRole.SCHOOL_ADMIN))
        assertFalse(RolePolicy.requiresLearnerScope(PortalRole.WELFARE_NURSE))

        val students = listOf(
            Student(id = "stu_1", fullName = "Amina Patel", className = "Grade 10A"),
            Student(id = "stu_2", fullName = "Brian Otieno", className = "Grade 10B")
        )
        assertNull(RolePolicy.resolveFocusStudent(students, null))
        assertNull(RolePolicy.resolveFocusStudent(students, "   "))
        assertNull(RolePolicy.resolveFocusStudent(students, "stu_unknown"))
        assertEquals("Amina Patel", RolePolicy.resolveFocusStudent(students, "stu_1")?.fullName)
    }

    @Test
    fun parentFinance_isEmptyWithoutALearner_andScopedWithOne() {
        val entries = listOf(
            FinanceEntry(id = "1", entryType = "INVOICE", partyName = "Amina Patel"),
            FinanceEntry(id = "2", entryType = "INVOICE", partyName = "Brian Otieno"),
            FinanceEntry(id = "3", entryType = "PAYROLL", partyName = "Brian Otieno")
        )
        assertTrue(RolePolicy.filterFinanceForRole(PortalRole.PARENT, entries, null).isEmpty())

        val scoped = RolePolicy.filterFinanceForRole(
            PortalRole.PARENT,
            entries,
            Student(id = "stu_2", fullName = "Brian Otieno")
        )
        assertEquals(1, scoped.size)
        assertEquals("Brian Otieno", scoped.first().partyName)

        assertTrue(
            RolePolicy.filterFinanceForRole(
                PortalRole.STUDENT,
                entries,
                Student(id = "stu_1", fullName = "Amina Patel")
            ).isEmpty()
        )
    }

    @Test
    fun studentAndParentNotices_neverFallBackToUnrelatedAnnouncements() {
        val items = listOf(
            CommunicationItem(id = "c1", audience = "Staff Only", title = "Payroll cut-off"),
            CommunicationItem(id = "c2", audience = "All Parents", title = "Sports day")
        )
        assertTrue(RolePolicy.filterCommunicationsForRole(PortalRole.STUDENT, items).none { it.id == "c1" })
        assertTrue(RolePolicy.filterCommunicationsForRole(PortalRole.PARENT, items).none { it.id == "c1" })

        val parentVisible = RolePolicy.filterCommunicationsForRole(PortalRole.PARENT, items)
        assertEquals(1, parentVisible.size)
        assertEquals("c2", parentVisible.first().id)

        assertEquals(2, RolePolicy.filterCommunicationsForRole(PortalRole.TEACHER, items).size)
    }

    @Test
    fun feeAndLeaveBalances_followCapabilities_notHardcodedRoles() {
        assertTrue(RolePolicy.canViewLearnerFeeBalances(PortalRole.SCHOOL_ADMIN))
        assertTrue(RolePolicy.canViewLearnerFeeBalances(PortalRole.FINANCE_OFFICER))
        assertTrue(RolePolicy.canViewLearnerFeeBalances(PortalRole.PARENT))
        assertFalse(RolePolicy.canViewLearnerFeeBalances(PortalRole.ADMISSIONS_RECEPTION))
        assertFalse(RolePolicy.canViewLearnerFeeBalances(PortalRole.TEACHER))
        assertFalse(RolePolicy.canViewLearnerFeeBalances(PortalRole.WELFARE_NURSE))

        assertTrue(RolePolicy.canViewStaffLeaveBalances(PortalRole.HR_MANAGER))
        assertTrue(RolePolicy.canViewStaffLeaveBalances(PortalRole.HEADTEACHER))
        assertFalse(RolePolicy.canViewStaffLeaveBalances(PortalRole.TEACHER))
        assertFalse(RolePolicy.canViewStaffLeaveBalances(PortalRole.LIBRARIAN))
    }
}
