package com.aegisfocus.app.domain.blocker

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Second layer for "Block 18+ content". DNS filtering alone is bypassed by browser Secure DNS (DoH)
 * and by custom Private DNS, so this also checks the address bar of common browsers.
 *
 * Only distinctive terms are used (not generic words like "sex") to avoid false positives on
 * normal sites such as "essex.ac.uk".
 */
object AdultContentGuard {

    private val BLOCKED_TERMS = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "spankbang",
        "brazzers", "chaturbate", "stripchat", "onlyfans", "hentai", "rule34", "erome",
        "nsfw", "camsoda", "bongacams", "livejasmin", "fapello", "e621"
    )

    fun isAdultPageActive(packageName: String, rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null || !ShortsBlockerEngine.isBrowserPackage(packageName)) return false
        return ShortsBlockerEngine.editableTexts(rootNode).any { raw ->
            val text = raw.trim().lowercase()
            // Ignore text typed in search boxes that is not a web address
            looksLikeAddress(text) && BLOCKED_TERMS.any { text.contains(it) }
        }
    }

    private fun looksLikeAddress(text: String): Boolean =
        text.isNotEmpty() && !text.contains(' ') && (text.contains('.') || text.contains('/'))
}
