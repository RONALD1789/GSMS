package com.example.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Writes append-only audit events to a school's audit_logs subcollection. */
internal class AuditLogRepository(private val db: FirebaseFirestore) {
    suspend fun append(
        schoolId: String,
        uid: String,
        actorName: String,
        actorRole: String,
        actionType: String,
        moduleName: String,
        summary: String,
        aiAction: Boolean
    ) {
        db.collection("schools").document(schoolId).collection("audit_logs").document().set(
            mapOf(
                "schoolId" to schoolId,
                "ownerId" to uid,
                "actorName" to actorName.ifBlank { "GSMS User" }.take(100),
                "actorRole" to actorRole.take(60),
                "actionType" to actionType.take(60),
                "moduleName" to moduleName.take(60),
                "summary" to summary.take(400),
                "aiAction" to aiAction,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}