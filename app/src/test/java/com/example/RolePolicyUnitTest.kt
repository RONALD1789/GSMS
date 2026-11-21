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
}
