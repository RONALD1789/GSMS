package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.GsmsAppNavigation
import com.example.data.supabase.SupabaseSyncRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SupabaseSyncRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            GsmsAppNavigation()
        }
    }
}
