package com.example.data

import android.util.Log
import com.example.model.PortalRole
import com.example.model.School
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class AiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

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
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI streaming timed out", e)
            throw IllegalStateException("AI request timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI streaming failed", e)
            throw e
        }
    }

    suspend fun generateWorkflowText(
        role: PortalRole,
        activeSchool: School?,
        dataSummary: String,
        taskPrompt: String
    ): Result<String> {
        val fullPrompt = "${buildRoleScopedContext(role, activeSchool, dataSummary)}\n\nTask:\n$taskPrompt"
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(fullPrompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            Result.failure(IllegalStateException("AI generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "GsmsAiRepository"
    }
}
