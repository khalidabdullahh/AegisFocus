package com.aegisfocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.aegisfocus.app.data.local.IntentStore
import com.aegisfocus.app.domain.blocker.YouTubeScreenReader
import com.aegisfocus.app.domain.intent.IntentMatcher
import com.aegisfocus.app.presentation.intent.IntentBlockedActivity

/** YouTube Intent Mode: blocks watch pages that don't match what the user said they want to watch. */
class YouTubeIntentController(
    private val service: AccessibilityService,
    private val store: IntentStore
) {
    private var lastCheckMs = 0L
    private var lastBlockMs = 0L

    /** Returns true if a video was blocked. */
    fun check(root: AccessibilityNodeInfo?): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCheckMs < CHECK_INTERVAL_MS) return false
        lastCheckMs = now
        if (root == null) return false

        val metrics = service.resources.displayMetrics
        val reading = YouTubeScreenReader.read(root, metrics.widthPixels, metrics.heightPixels)
        if (!reading.playerFound) {
            store.setLastCheck("No video page detected (feed, search or fullscreen)")
            return false
        }
        if (reading.texts.isEmpty()) {
            store.setLastCheck("Video page found, but no title text could be read")
            return false
        }

        val title = reading.texts.first()
        if (store.isAllowedOnce(title)) {
            store.setLastCheck("Allowed by you: $title")
            return false
        }

        val result = IntentMatcher.evaluate(reading.texts.joinToString(" | "), store.getConfig())
        store.setLastCheck(
            (if (result.allowed) "ALLOWED" else "BLOCKED") + " - " + result.reason +
                "\nRead: " + reading.texts.joinToString(" | ")
        )
        if (result.allowed) return false

        if (now - lastBlockMs > BLOCK_COOLDOWN_MS) {
            lastBlockMs = now
            // Leave the watch page, then show the reason on top
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            val intent = Intent(service, IntentBlockedActivity::class.java).apply {
                putExtra(IntentBlockedActivity.EXTRA_TITLE, title)
                putExtra(IntentBlockedActivity.EXTRA_REASON, result.reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.startActivity(intent)
        }
        return true
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 800L
        private const val BLOCK_COOLDOWN_MS = 2500L
    }
}
