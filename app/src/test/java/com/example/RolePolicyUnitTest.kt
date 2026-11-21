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
    fun superAdmin_hasGlobalTenantAndAuditAccess_withoutOperationalEdits() {
        val role = PortalRole.SUPER_ADMIN
        assertTrue(role.has(RoleCapability.CREATE_SCHOOL_TENANT))
        assertTrue(role.has(RoleCapability.MANAGE_MODULES))
        assertTrue(role.has(RoleCapability.VIEW_AUDIT_LOGS))
        assertFalse(role.has(RoleCapability.APPROVE_PUBLISH_REPORT_CARDS))
        assertFalse(role.has(RoleCapability.APPROVE_LEAVE))
        assertFalse(role.has(RoleCapability.RECORD_FINANCE_PAYMENT))
    }

    @Test
    fun teacher_canFillReportsAndMarkAttendance_butCannotApproveReportsOrLeaveOrModules() {
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
    }

    @Test
    fun student_and_parent_areStrictlyReadOnly_andScoped() {
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
        assertTrue(GsmsTab.OPERATIONS in parentTabs) // Child fee invoices only
        assertEquals(listOf("Finance"), RolePolicy.operationSections(PortalRole.PARENT, fullSchool))
        assertFalse(PortalRole.PARENT.has(RoleCapability.RECORD_FINANCE_PAYMENT))
        assertFalse(PortalRole.PARENT.has(RoleCapability.VIEW_AUDIT_LOGS))

        val focusStudent = Student(id = "stu_1", fullName = "Amina Patel")
        val financeEntries = listOf(
            FinanceEntry(id = "1", entryType = "INVOICE", partyName = "Amina Patel"),
            FinanceEntry(id = "2", entryType = "INVOICE", partyName = "Other Student"),
            FinanceEntry(id = "3", entryType = "PAYROLL", partyName = "Dr. Samuel")
        )
        val parentFinance = RolePolicy.filterFinanceForRole(PortalRole.PARENT, financeEntries, focusStudent)
        assertEquals(1, parentFinance.size)
        assertEquals("Amina Patel", parentFinance.first().partyName)
    }

    @Test
    fun specialistRoles_onlySeeTheirDepartmentModules() {
        assertEquals(listOf("Finance"), RolePolicy.operationSections(PortalRole.FINANCE_OFFICER, fullSchool))
        assertEquals(listOf("Library"), RolePolicy.operationSections(PortalRole.LIBRARIAN, fullSchool))
        assertEquals(listOf("Transport"), RolePolicy.operationSections(PortalRole.TRANSPORT_MANAGER, fullSchool))
        assertEquals(listOf("Welfare", "Discipline", "Hostel"), RolePolicy.operationSections(PortalRole.WELFARE_NURSE, fullSchool))
        assertEquals(listOf("Admissions", "Events"), RolePolicy.operationSections(PortalRole.ADMISSIONS_RECEPTION, fullSchool))
        assertEquals(listOf("Staff & Faculty", "Leave Workflow"), RolePolicy.peopleSubTabs(PortalRole.HR_MANAGER, fullSchool))
        assertTrue(PortalRole.HR_MANAGER.has(RoleCapability.APPROVE_LEAVE))
        assertFalse(PortalRole.LIBRARIAN.has(RoleCapability.APPROVE_LEAVE))
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
