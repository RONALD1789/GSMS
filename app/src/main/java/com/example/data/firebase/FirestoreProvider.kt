package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

/** Creates the configured Firestore client used by the Android data layer. */
internal object FirestoreProvider {
    fun create(context: Context): FirebaseFirestore {
        val databaseId = context.getString(R.string.firestore_database_id)
        val instance = FirebaseFirestore.getInstance(databaseId)
        try {
            instance.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
        } catch (_: IllegalStateException) {
            // Settings have already been applied to this Firestore instance.
        }
        return instance
    }
}