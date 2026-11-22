package com.example.data.supabase

import com.example.BuildConfig
import java.net.URI

data class SupabaseConfig(
    val rawUrl: String,
    val rawAnonKey: String
) {
    val trimmedUrl: String = rawUrl.trim().trimEnd('/')
    val trimmedAnonKey: String = rawAnonKey.trim()

    val validationError: String? = validate(trimmedUrl, trimmedAnonKey)
    val isConfigured: Boolean = validationError == null

    val normalizedBaseUrl: String? = if (isConfigured) "$trimmedUrl/" else null

    val maskedHost: String = maskEndpoint(trimmedUrl)

    companion object {
        private val SCHOOL_ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,128}$")
        private val IPV4_REGEX = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")

        fun fromBuildConfig(): SupabaseConfig = SupabaseConfig(
            rawUrl = BuildConfig.SUPABASE_URL,
            rawAnonKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank { BuildConfig.SUPABASE_ANON_KEY }
        )

        fun isValidSchoolId(schoolId: String?): Boolean =
            !schoolId.isNullOrBlank() && SCHOOL_ID_REGEX.matches(schoolId)

        fun validate(rawUrl: String, rawAnonKey: String): String? {
            val url = rawUrl.trim().trimEnd('/')
            val key = rawAnonKey.trim()

            if (url.isEmpty() || url.contains("YOUR_PROJECT_REF", ignoreCase = true) ||
                url.contains("YOUR_SUPABASE", ignoreCase = true)
            ) {
                return "Set SUPABASE_URL in the AI Studio Secrets panel (currently placeholder/empty)."
            }
            if (key.isEmpty() || key.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true) ||
                key.contains("YOUR_SUPABASE_PUBLISHABLE_KEY", ignoreCase = true) ||
                key.contains("MY_SUPABASE", ignoreCase = true)
            ) {
                return "Set SUPABASE_PUBLISHABLE_KEY (or legacy SUPABASE_ANON_KEY) in the AI Studio Secrets panel."
            }
            if (key.startsWith("sb_secret_", ignoreCase = true)) {
                return "A Supabase secret/service-role key must never be used in the Android client. Use the publishable key."
            }
            if (key.length < 20) {
                return "SUPABASE_PUBLISHABLE_KEY appears invalid or truncated."
            }

            val uri = try {
                URI(url)
            } catch (_: Exception) {
                return "SUPABASE_URL is not a valid URI."
            }

            if (!"https".equals(uri.scheme, ignoreCase = true)) {
                return "SUPABASE_URL must use HTTPS."
            }
            if (!uri.userInfo.isNullOrBlank()) {
                return "SUPABASE_URL must not contain embedded credentials."
            }
            if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {
                return "SUPABASE_URL must be a clean base origin without query or fragment."
            }

            val host = uri.host?.lowercase()?.trimEnd('.') ?: return "SUPABASE_URL is missing a host."
            if (isBlockedHost(host)) {
                return "SUPABASE_URL points to a blocked local or private network host."
            }
            return null
        }

        private fun isBlockedHost(host: String): Boolean {
            if (host == "localhost" || host == "::1" || host == "[::1]" || host == "0.0.0.0" ||
                host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".localhost")
            ) {
                return true
            }
            val match = IPV4_REGEX.matchEntire(host) ?: return false
            val o1 = match.groupValues[1].toIntOrNull() ?: return true
            val o2 = match.groupValues[2].toIntOrNull() ?: return true
            val o3 = match.groupValues[3].toIntOrNull() ?: return true
            val o4 = match.groupValues[4].toIntOrNull() ?: return true
            if (o1 > 255 || o2 > 255 || o3 > 255 || o4 > 255) return true
            return when {
                o1 == 0 -> true
                o1 == 10 -> true
                o1 == 127 -> true
                o1 == 169 && o2 == 254 -> true
                o1 == 172 && o2 in 16..31 -> true
                o1 == 192 && o2 == 168 -> true
                o1 == 100 && o2 in 64..127 -> true
                else -> false
            }
        }

        private fun maskEndpoint(url: String): String {
            if (url.isBlank() || url.contains("YOUR_PROJECT_REF", ignoreCase = true)) {
                return "Not configured"
            }
            return try {
                val uri = URI(url)
                val host = uri.host ?: return "Configured"
                val firstDot = host.indexOf('.')
                if (firstDot > 4) {
                    val ref = host.substring(0, firstDot)
                    val suffix = host.substring(firstDot)
                    "https://${ref.take(4)}***$suffix"
                } else {
                    "https://$host"
                }
            } catch (_: Exception) {
                "Configured"
            }
        }
    }
}
