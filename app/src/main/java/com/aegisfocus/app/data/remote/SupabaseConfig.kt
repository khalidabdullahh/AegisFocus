package com.aegisfocus.app.data.remote

/**
 * Centralized Supabase Configuration.
 */
object SupabaseConfig {
    const val PROJECT_URL = "https://owroghakrlwuqtmsozzf.supabase.co"
    const val SUPABASE_URL = PROJECT_URL
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im93cm9naGFrcmx3dXF0bXNvenpmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxMjE0NTYsImV4cCI6MjEwNDY5NzQ1Nn0.bKl8PmFCMK1rogsYpUQYihWKVkIr_zfQ4FIacVkxZ7s"

    // Default Web Client ID for Google Sign-In with Supabase
    const val GOOGLE_WEB_CLIENT_ID = "734353516926-m1fvrgs7hhvd1h7n6917icuhk4ldf479.apps.googleusercontent.com"

    const val AUTH_ENDPOINT = "$PROJECT_URL/auth/v1"
    const val REST_ENDPOINT = "$PROJECT_URL/rest/v1"
}
