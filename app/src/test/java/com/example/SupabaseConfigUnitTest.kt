package com.example

import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseSyncRepository
import com.example.data.supabase.toDomain
import com.example.data.supabase.toSupabaseRow
import com.example.model.PortalRole
import com.example.model.RoleCapability
import com.example.model.School
import com.example.model.Student
import com.example.model.UserAccessAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseConfigUnitTest {

    private val validAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid_sample_payload_signature"

    @Test
    fun validHttpsSupabaseConfig_isAcceptedAndMasked() {
        val cfg = SupabaseConfig("https://abcdefghijklm.supabase.co/", validAnonKey)
        assertTrue(cfg.isConfigured)
        assertNull(cfg.validationError)
        assertEquals("https://abcdefghijklm.supabase.co/", cfg.normalizedBaseUrl)
        assertEquals("https://abcd***.supabase.co", cfg.maskedHost)
    }

    @Test
    fun placeholderAndEmptyValues_areRejected() {
        val placeholder = SupabaseConfig("https://YOUR_PROJECT_REF.supabase.co", "YOUR_SUPABASE_ANON_KEY")
        assertFalse(placeholder.isConfigured)

        val emptyUrl = SupabaseConfig("", validAnonKey)
        assertFalse(emptyUrl.isConfigured)

        val shortKey = SupabaseConfig("https://abcdefghijklm.supabase.co", "short")
        assertFalse(shortKey.isConfigured)
    }

    @Test
    fun supabasePublishableKey_isAcceptedAndSecretKeyIsRejected() {
        val publishable = SupabaseConfig(
            "https://abcdefghijklm.supabase.co",
            "sb_publishable_123456789012345678901234"
        )
        assertTrue(publishable.isConfigured)

        val secret = SupabaseConfig(
            "https://abcdefghijklm.supabase.co",
            "sb_secret_123456789012345678901234"
        )
        assertFalse(secret.isConfigured)
    }

    @Test
    fun insecureAndLocalPrivateHosts_areBlocked() {
        val httpConfig = SupabaseConfig("http://abcdefghijklm.supabase.co", validAnonKey)
        assertFalse(httpConfig.isConfigured)

        listOf(
            "https://localhost:54321",
            "https://127.0.0.1",
            "https://10.0.0.5",
            "https://192.168.1.10",
            "https://172.16.0.1",
            "https://169.254.169.254",
            "https://user:pass@abcdefghijklm.supabase.co",
            "https://abcdefghijklm.supabase.co?foo=bar"
        ).forEach { badUrl ->
            assertFalse("Expected $badUrl to fail validation", SupabaseConfig(badUrl, validAnonKey).isConfigured)
        }
    }

    @Test
    fun schoolIdFilterValidation_blocksInjectionCharacters() {
        assertTrue(SupabaseConfig.isValidSchoolId("school_greenwood_01"))
        assertTrue(SupabaseConfig.isValidSchoolId("tenant-123"))
        assertFalse(SupabaseConfig.isValidSchoolId(""))
        assertFalse(SupabaseConfig.isValidSchoolId("school_1,id.neq.0"))
        assertFalse(SupabaseConfig.isValidSchoolId("school 1"))
    }

    @Test
    fun rbacSyncGate_allowsAuthorizedRolesAndDelegationsOnly() {
        assertTrue(SupabaseSyncRepository.canSyncSupabase(PortalRole.SUPER_ADMIN))
        assertTrue(SupabaseSyncRepository.canSyncSupabase(PortalRole.SCHOOL_ADMIN))
        assertTrue(SupabaseSyncRepository.canSyncSupabase(PortalRole.ICT_ADMIN))

        assertFalse(SupabaseSyncRepository.canSyncSupabase(PortalRole.TEACHER))
        assertFalse(SupabaseSyncRepository.canSyncSupabase(PortalRole.PARENT))
        assertFalse(SupabaseSyncRepository.canSyncSupabase(PortalRole.STUDENT))
        assertFalse(SupabaseSyncRepository.canSyncSupabase(PortalRole.FINANCE_OFFICER))
        assertFalse(SupabaseSyncRepository.canSyncSupabase(PortalRole.HEADTEACHER))

        // Delegated permission grants allow sync without changing primary role
        assertTrue(
            SupabaseSyncRepository.canSyncSupabase(
                PortalRole.HEADTEACHER,
                setOf(RoleCapability.MANAGE_MODULES)
            )
        )
    }

    @Test
    fun domainMappers_preserveMultiTenantColumns() {
        val school = School(
            id = "school_1",
            ownerId = "legacy_owner_uid_1",
            name = "Greenwood Academy",
            code = "GWA-01",
            country = "Kenya",
            city = "Nairobi"
        )
        val sbSchool = school.toSupabaseRow(fallbackOwnerUid = "fallback", supabaseUserUid = "sb-uid-99")
        assertEquals("school_1", sbSchool.id)
        assertEquals("sb-uid-99", sbSchool.supabaseOwnerUid)
        assertEquals("Greenwood Academy", sbSchool.name)

        val student = Student(
            id = "stu_1",
            schoolId = "",
            branchId = "",
            ownerId = "",
            admissionNo = "ADM-101",
            fullName = "Amina Patel",
            className = "Grade 10A"
        )
        val sbStudent = student.toSupabaseRow(fallbackOwnerUid = "owner_1", activeSchoolId = "school_1")
        assertEquals("school_1", sbStudent.schoolId)
        assertEquals("branch_main", sbStudent.branchId)
        assertEquals("owner_1", sbStudent.ownerId)

        val grant = UserAccessAssignment(
            id = "grant_1",
            schoolId = "school_1",
            ownerId = "owner_1",
            fullName = "Grace Wanjiku",
            email = "grace@greenwood.edu",
            primaryRole = PortalRole.TEACHER.name,
            assignedClasses = listOf("Grade 10A"),
            extraPermissions = listOf(RoleCapability.APPROVE_LEAVE.name)
        )
        val sbGrant = grant.toSupabaseRow("owner_1", "school_1")
        assertEquals("TEACHER", sbGrant.primaryRole)
        assertEquals(listOf("Grade 10A"), sbGrant.assignedClasses)
        assertEquals(listOf("APPROVE_LEAVE"), sbGrant.extraPermissions)

        val roundTripSchool = sbSchool.toDomain()
        assertEquals(school.id, roundTripSchool.id)
        assertEquals(school.name, roundTripSchool.name)

        val roundTripStudent = sbStudent.toDomain()
        assertEquals("school_1", roundTripStudent.schoolId)
        assertEquals("Amina Patel", roundTripStudent.fullName)

        val roundTripGrant = sbGrant.toDomain()
        assertEquals(PortalRole.TEACHER, roundTripGrant.resolvedRole())
        assertEquals(setOf(RoleCapability.APPROVE_LEAVE), roundTripGrant.resolvedExtraCapabilities())
    }

    @Test
    fun emailFormatValidation_acceptsValidAndRejectsMalformed() {
        assertTrue(SupabaseSyncRepository.isValidEmailFormat("principal@oakridge.edu"))
        assertTrue(SupabaseSyncRepository.isValidEmailFormat("  teacher.name+01@school.ac.uk  "))
        assertFalse(SupabaseSyncRepository.isValidEmailFormat(""))
        assertFalse(SupabaseSyncRepository.isValidEmailFormat("notanemail"))
        assertFalse(SupabaseSyncRepository.isValidEmailFormat("user@localhost"))
        assertFalse(SupabaseSyncRepository.isValidEmailFormat("@domain.com"))
    }

    @Test
    fun seedWorkspaceForEmail_populatesMultiTenantStructure() {
        val ws = com.example.data.supabase.SupabaseSeedData.buildInitialWorkspaceState(
            uid = "sb-user-123456",
            displayName = "Ronald",
            email = "ronald@school.edu"
        )
        assertEquals(2, ws.schools.size)
        assertTrue(ws.students.isNotEmpty())
        assertTrue(ws.staff.isNotEmpty())
        assertTrue(ws.accessAssignments.isNotEmpty())
        assertEquals("ronald@school.edu", ws.userProfile?.email)
        assertEquals(PortalRole.SUPER_ADMIN, ws.userProfile?.resolvedPrimaryRole())
    }
}
