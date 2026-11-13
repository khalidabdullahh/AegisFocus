package com.aegisfocus.app.domain.blocker

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects short-form video screens (YouTube Shorts, Instagram Reels, Facebook Reels, TikTok,
 * and the same pages in common browsers).
 *
 * Apps change their internal view ids between releases, so detection combines several signals
 * (view ids, the selected bottom tab, and browser URL text). All the id/label lists below live in
 * one place so they can be updated quickly after checking the live app with
 * `adb shell uiautomator dump` or Android Studio's Layout Inspector.
 *
 * NOTE: the ids/labels are best-effort and must be verified on a real device.
 */
object ShortsBlockerEngine {

    private const val PKG_YOUTUBE = "com.google.android.youtube"
    private const val PKG_INSTAGRAM = "com.instagram.android"
    private const val PKG_FACEBOOK = "com.facebook.katana"
    private const val PKG_FACEBOOK_LITE = "com.facebook.lite"

    /** TikTok is short-video only, so the whole app is closed when it opens. */
    private val TIKTOK_PACKAGES = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    )

    private val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser"
    )

    // View id fragments (case-insensitive "contains" match on viewIdResourceName).
    private val YOUTUBE_ID_FRAGMENTS = listOf("reel_recycler", "reel_player", "reel_watch")
    private val INSTAGRAM_ID_FRAGMENTS = listOf("clips_viewer", "clips_video_container")
    private val FACEBOOK_ID_FRAGMENTS = listOf("reels_viewer", "reel_viewer", "fbreels")

    // A selected bottom-bar tab with one of these labels means the user is on the short-video tab.
    private val YOUTUBE_TAB_LABELS = setOf("shorts")
    private val INSTAGRAM_TAB_LABELS = setOf("reels")
    private val FACEBOOK_TAB_LABELS = setOf("reels")

    private val URL_PATTERNS = listOf(
        "youtube.com/shorts",
        "instagram.com/reel",
        "facebook.com/reel",
        "tiktok.com"
    )

    private const val MAX_NODES = 700

    fun isSupportedPackage(packageName: String): Boolean =
        packageName == PKG_YOUTUBE ||
            packageName == PKG_INSTAGRAM ||
            packageName == PKG_FACEBOOK ||
            packageName == PKG_FACEBOOK_LITE ||
            packageName in TIKTOK_PACKAGES ||
            packageName in BROWSER_PACKAGES

    fun isBrowserPackage(packageName: String): Boolean = packageName in BROWSER_PACKAGES

    /** Text of editable fields (the browser address bar) on the current screen. */
    fun editableTexts(rootNode: AccessibilityNodeInfo): List<String> = scan(rootNode).editableTexts

    /** Returns true if short-form video is currently on screen for [packageName]. */
    fun isShortsOrReelsActive(packageName: String, rootNode: AccessibilityNodeInfo?): Boolean {
        if (packageName in TIKTOK_PACKAGES) return true
        if (rootNode == null) return false

        val snapshot = scan(rootNode)
        return when (packageName) {
            PKG_YOUTUBE -> snapshot.matches(YOUTUBE_ID_FRAGMENTS, YOUTUBE_TAB_LABELS)
            PKG_INSTAGRAM -> snapshot.matches(INSTAGRAM_ID_FRAGMENTS, INSTAGRAM_TAB_LABELS)
            PKG_FACEBOOK, PKG_FACEBOOK_LITE -> snapshot.matches(FACEBOOK_ID_FRAGMENTS, FACEBOOK_TAB_LABELS)
            in BROWSER_PACKAGES -> snapshot.editableTexts.any { text ->
                URL_PATTERNS.any { text.contains(it, ignoreCase = true) }
            }
            else -> false
        }
    }

    private class Snapshot {
        val viewIds = ArrayList<String>()
        val selectedLabels = HashSet<String>()
        val editableTexts = ArrayList<String>()

        fun matches(idFragments: List<String>, tabLabels: Set<String>): Boolean {
            if (viewIds.any { id -> idFragments.any { id.contains(it, ignoreCase = true) } }) return true
            return selectedLabels.any { it in tabLabels }
        }
    }

    /** Breadth-first scan of the window with a hard node cap so a busy screen stays cheap. */
    private fun scan(root: AccessibilityNodeInfo): Snapshot {
        val snap = Snapshot()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++

            node.viewIdResourceName?.let { snap.viewIds.add(it) }

            if (node.isSelected) {
                node.contentDescription?.toString()?.trim()?.lowercase()?.let { snap.selectedLabels.add(it) }
                node.text?.toString()?.trim()?.lowercase()?.let { snap.selectedLabels.add(it) }
            }
            if (node.isEditable) {
                node.text?.toString()?.let { snap.editableTexts.add(it) }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return snap
    }
}
