package com.aegisfocus.app.data.local

import android.content.Context
import com.aegisfocus.app.domain.intent.IntentConfig
import com.aegisfocus.app.domain.intent.IntentDefaults
import com.aegisfocus.app.domain.intent.IntentMatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The user's "what I want to watch" settings for YouTube Intent Mode (stored on-device). */
class IntentStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_intent", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun topicsText(): String = prefs.getString(KEY_TOPICS, IntentDefaults.TOPICS_TEXT) ?: IntentDefaults.TOPICS_TEXT
    fun channelsText(): String = prefs.getString(KEY_CHANNELS, IntentDefaults.CHANNELS_TEXT) ?: ""
    fun blockedText(): String = prefs.getString(KEY_BLOCKED, IntentDefaults.BLOCKED_TEXT) ?: ""

    fun save(topics: String, channels: String, blocked: String) {
        prefs.edit()
            .putString(KEY_TOPICS, topics)
            .putString(KEY_CHANNELS, channels)
            .putString(KEY_BLOCKED, blocked)
            .apply()
    }

    fun getConfig(): IntentConfig = IntentConfig(
        topics = IntentMatcher.parseTopics(topicsText()),
        allowedChannels = IntentMatcher.splitList(channelsText()),
        blockedWords = IntentMatcher.splitList(blockedText())
    )

    // "Allow this video" exceptions (reason is logged)

    fun isAllowedOnce(title: String): Boolean = allowedTitles.contains(IntentMatcher.normalize(title))

    fun allowOnce(title: String, reason: String) {
        allowedTitles.add(IntentMatcher.normalize(title))
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val log = (listOf("$stamp | ${title.take(60)} | ${reason.take(120)}") + exceptionLog()).take(MAX_LOG)
        prefs.edit().putString(KEY_LOG, log.joinToString("\n")).apply()
    }

    fun exceptionLog(): List<String> =
        (prefs.getString(KEY_LOG, null) ?: "").lines().filter { it.isNotBlank() }

    /** What the last check read and decided, shown in the app to help tune matching. */
    fun lastCheck(): String = lastCheckText
    fun setLastCheck(text: String) {
        lastCheckText = text
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_TOPICS = "topics"
        private const val KEY_CHANNELS = "channels"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_LOG = "exception_log"
        private const val MAX_LOG = 30

        private val allowedTitles = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

        @Volatile
        private var lastCheckText: String = "No YouTube video checked yet"
    }
}
