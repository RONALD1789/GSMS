package com.example.data

import com.example.data.supabase.SupabaseSyncRepository
import com.example.model.PortalRole
import com.example.model.School
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiRepository(
    private val supabaseRepository: SupabaseSyncRepository
) {
    fun buildRoleScopedContext(
        role: PortalRole,
        activeSchool: School?,
        dataSummary: String
    ): String {
        val schoolHeader = if (activeSchool != null) {
            "School: ${activeSchool.name} (${activeSchool.code}), ${activeSchool.city}, ${activeSchool.country}. " +
                "Academic Year: ${activeSchool.activeAcademicYear}, Term: ${activeSchool.activeTerm}. " +
                "Enabled Modules: ${activeSchool.enabledModules.joinToString(", ")}."
        } else {
            "Multi-School Platform Scope."
        }
        return """
            You are the Global School Management System (GSMS) AI Assistant.
            Active Portal: ${role.displayName}
            Role Scope: ${role.scopeDescription}
            Tenant Context: $schoolHeader

            STRICT GOVERNANCE RULES:
            1. Only reference facts present in the Verified School Data Context below. Never invent student names, grades, or financial figures.
            2. Respect role permissions for ${role.displayName}. Do not expose data outside this role's scope.
            3. Provide crisp, structured, actionable institutional output.

            Verified School Data Context:
            $dataSummary
        """.trimIndent()
    }

    fun streamAssistantResponse(
        role: PortalRole,
        activeSchool: School?,
        dataSummary: String,
        userPrompt: String
    ): Flow<String> = flow {
        val fullPrompt = "${buildRoleScopedContext(role, activeSchool, dataSummary)}\n\nUser Request:\n$userPrompt"
        emit(supabaseRepository.generateAiText(fullPrompt).getOrThrow())
    }

    suspend fun generateWorkflowText(
        role: PortalRole,
        activeSchool: School?,
        dataSummary: String,
        taskPrompt: String
    ): Result<String> {
        val fullPrompt = "${buildRoleScopedContext(role, activeSchool, dataSummary)}\n\nTask:\n$taskPrompt"
        return supabaseRepository.generateAiText(fullPrompt)
    }
}
