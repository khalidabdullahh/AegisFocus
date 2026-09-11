package com.aegisfocus.app.data.remote

/**
 * Centralized Supabase Configuration.
 */
object SupabaseConfig {
    const val PROJECT_URL = "https://owroghakrlwuqtmsozzf.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93cm9naGFrcmx3dXF0bXNvenpmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxMjE0NTYsImV4cCI6MjEwNDY5NzQ1Nn0.bKl8PmFCMK1rogsYpUQYihWKVkIr_zfQ4FIacVkxZ7s"

    const val AUTH_ENDPOINT = "$PROJECT_URL/auth/v1"
    const val REST_ENDPOINT = "$PROJECT_URL/rest/v1"
}
