package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.intent.IntentConfig
import com.aegisfocus.app.domain.intent.IntentDefaults
import com.aegisfocus.app.domain.intent.IntentMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentMatcherTest {

    private val config = IntentConfig(
        topics = IntentMatcher.parseTopics(IntentDefaults.TOPICS_TEXT),
        allowedChannels = listOf("freeCodeCamp"),
        blockedWords = IntentMatcher.splitList(IntentDefaults.BLOCKED_TEXT)
    )

    private fun allowed(text: String) = IntentMatcher.evaluate(text, config).allowed

    @Test
    fun parsesAllFiveDefaultTopics() {
        assertEquals(5, config.topics.size)
        assertEquals("Trading", config.topics[0].name)
        assertTrue(config.topics[0].keywords.contains("forex"))
    }

    @Test
    fun allowsOnTopicTitles() {
        assertTrue(allowed("Forex price action strategy for beginners"))
        assertTrue(allowed("How to start investing in mutual funds"))
        assertTrue(allowed("Data Structures and Algorithms full course | Apna College"))
        assertTrue(allowed("Spoken English practice every day"))
        assertTrue(allowed("How I built my startup from zero"))
    }

    @Test
    fun allowsBanglaKeywords() {
        assertTrue(allowed("ফরেক্স ট্রেডিং শেখার সহজ উপায়"))
    }

    @Test
    fun blocksOffTopicTitles() {
        assertFalse(allowed("Funny cat compilation 2026"))
        assertFalse(allowed("Top 10 movie scenes ever | Cinema Club"))
    }

    @Test
    fun blockedWordsWinOverTopics() {
        assertFalse(allowed("Forex trading prank gone wrong"))
    }

    @Test
    fun allowedChannelPassesEvenWithoutKeyword() {
        assertTrue(allowed("Weekend special | FreeCodeCamp.org"))
    }

    @Test
    fun shortKeywordsNeedWholeWords() {
        // "ai" must not match inside "rain" / "mail"
        assertFalse(allowed("Heavy rain in the city today"))
        assertTrue(allowed("AI tools you must know"))
    }

    @Test
    fun emptyTextIsNotBlocked() {
        assertTrue(allowed("   "))
    }
}
