package com.aegisfocus.app.domain.blocker

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Reads the video title and channel from the YouTube watch page.
 *
 * It does not depend on YouTube's internal view ids for the text: it finds the video player (a
 * 16:9 block at the top) and takes the first few texts below it. The player lookup is best-effort
 * and the app shows what was read ("Last check") so it can be tuned on a real device.
 */
object YouTubeScreenReader {

    data class Reading(val playerFound: Boolean, val texts: List<String>)

    private const val MAX_NODES = 800

    private val NOISE = setOf(
        "subscribe", "subscribed", "like", "dislike", "share", "download", "save", "clip", "remix",
        "comments", "more", "join", "ask", "thanks", "description", "show more", "...more", "…more",
        "shop", "report", "hype", "sponsor", "live chat", "fullscreen", "minimize", "close"
    )
    private val STATS = Regex("^[0-9.,]+\\s?[kmb]?\\s?(views|likes|subscribers|watching|comments).*", RegexOption.IGNORE_CASE)

    private class Item(val text: String, val top: Int, val bottom: Int)

    fun read(root: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Reading {
        val items = ArrayList<Item>()
        var playerBottom = -1

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val rect = Rect()
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            node.getBoundsInScreen(rect)

            val id = node.viewIdResourceName.orEmpty().lowercase()
            val desc = node.contentDescription?.toString().orEmpty()
            val text = node.text?.toString().orEmpty()

            if ((id.contains("player") || desc.lowercase().contains("player")) && looksLikePlayer(rect, screenWidth, screenHeight)) {
                if (playerBottom < 0 || rect.bottom < playerBottom) playerBottom = rect.bottom
            }

            val label = when {
                text.isNotBlank() -> text
                desc.length >= 12 && node.childCount == 0 -> desc
                else -> ""
            }
            if (label.isNotBlank()) items.add(Item(label.trim(), rect.top, rect.bottom))

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        if (playerBottom < 0) return Reading(playerFound = false, texts = emptyList())

        val texts = items
            .filter { it.top >= playerBottom - 4 && it.bottom <= screenHeight }
            .sortedBy { it.top }
            .map { it.text }
            .filter { it.length >= 5 && it.lowercase() !in NOISE && !STATS.matches(it) }
            .distinct()
            .take(5)

        return Reading(playerFound = true, texts = texts)
    }

    private fun looksLikePlayer(rect: Rect, screenWidth: Int, screenHeight: Int): Boolean {
        val width = rect.width()
        val height = rect.height()
        if (screenWidth <= 0 || screenWidth > screenHeight) return false // portrait only
        return width >= screenWidth * 0.9 &&
            height >= width * 0.45 && height <= width * 0.75 &&
            rect.top < screenHeight * 0.3
    }
}
