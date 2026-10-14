package com.aegisfocus.app.domain.blocker

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Surgically detects Short-form video interfaces (YouTube Shorts, Instagram Reels, Facebook Reels)
 * in the active view hierarchy without blocking the entire application.
 */
object ShortsBlockerEngine {

    private const val PKG_YOUTUBE = "com.google.android.youtube"
    private const val PKG_INSTAGRAM = "com.instagram.android"
    private const val PKG_FACEBOOK = "com.facebook.katana"
    private const val PKG_FACEBOOK_LITE = "com.facebook.lite"

    /**
     * Inspects the root accessibility node to determine if short-form video is currently dominating the screen.
     */
    fun isShortsOrReelsActive(packageName: String, rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        return when (packageName) {
            PKG_YOUTUBE -> isYouTubeShorts(rootNode)
            PKG_INSTAGRAM -> isInstagramReels(rootNode)
            PKG_FACEBOOK, PKG_FACEBOOK_LITE -> isFacebookReels(rootNode)
            else -> false
        }
    }

    private fun isYouTubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Check for dedicated Shorts container view IDs
        val shortsContainers = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler")
        if (shortsContainers.isNotEmpty()) return true

        val pivotBar = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/pivot_bar")
        if (pivotBar.isNotEmpty()) {
            val shortsTabSelected = rootNode.findAccessibilityNodeInfosByText("Shorts")
            for (node in shortsTabSelected) {
                if (node.isSelected) return true
            }
        }

        // 2. Check for Shorts player overlay or action buttons
        val shortsPlayer = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_player_page_view")
        if (shortsPlayer.isNotEmpty()) return true

        return false
    }

    private fun isInstagramReels(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Clips / Reels video player container
        val clipsContainer = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_video_container")
        if (clipsContainer.isNotEmpty()) return true

        val reelsViewer = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer_root")
        if (reelsViewer.isNotEmpty()) return true

        // 2. Reels tab indicator
        val reelsTab = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reels_tab")
        if (reelsTab.isNotEmpty() && reelsTab.any { it.isSelected }) return true

        return false
    }

    private fun isFacebookReels(rootNode: AccessibilityNodeInfo): Boolean {
        // Facebook Reels overlay / viewer check
        val reelElements = rootNode.findAccessibilityNodeInfosByText("Reels")
        if (reelElements.any { it.parent?.className?.toString()?.contains("ViewPager") == true }) return true

        val shortVideos = rootNode.findAccessibilityNodeInfosByText("Short videos")
        return shortVideos.isNotEmpty()
    }
}
