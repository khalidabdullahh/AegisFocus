package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.intent.IntentConfig
import com.aegisfocus.app.domain.intent.IntentDefaults
import com.aegisfocus.app.domain.intent.IntentMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentMatcherWeightedTest {

    private val config = IntentConfig(
        topics = IntentMatcher.parseTopics(IntentDefaults.TOPICS_TEXT),
        allowedChannels = emptyList(),
        blockedWords = IntentMatcher.splitList(IntentDefaults.BLOCKED_TEXT)
    )

    private fun check(vararg texts: String) = IntentMatcher.evaluate(texts.toList(), config)

    @Test
    fun titleMatchIsEnough() {
        assertTrue(check("Forex trading for beginners", "Some Channel").allowed)
    }

    @Test
    fun channelNameCountsAsStrongEvidence() {
        assertTrue(check("Weekend special", "Investing Academy").allowed)
    }

    @Test
    fun oneWeakHitFurtherDownIsNotEnough() {
        // title and channel are off topic, only a later text mentions one topic word
        assertFalse(check("Funny cat video", "Cat Channel", "10 minutes", "python").allowed)
    }

    @Test
    fun twoWeakHitsAreEnough() {
        assertTrue(check("Funny video", "Some Channel", "learn python", "and sql basics").allowed)
    }

    @Test
    fun toleratesWordEndings() {
        assertTrue(check("Best traders of the year", "Market Talk").allowed)
        assertTrue(check("Programmers guide", "Dev Talk").allowed)
    }

    @Test
    fun toleratesSmallSpellingMistakes() {
        assertTrue(check("Forrex strategy that works", "Chart Lab").allowed)
        assertTrue(check("Tradng psychology", "Mind Desk").allowed)
    }

    @Test
    fun blockedWordInChannelBlocks() {
        assertFalse(check("Forex trading basics", "Prank Zone").allowed)
    }

    @Test
    fun offTopicIsStillBlocked() {
        assertFalse(check("Funny cat compilation 2026", "Cat Lovers").allowed)
        assertEquals("Does not match any of your topics", check("Cooking pasta at home", "Chef Mia").reason)
    }
}
