package com.aegisfocus.app.domain.intent

/** A topic the user wants to consume, with the words that describe it. */
data class IntentTopic(val name: String, val keywords: List<String>)

data class IntentConfig(
    val topics: List<IntentTopic>,
    val allowedChannels: List<String>,
    val blockedWords: List<String>
)

data class MatchResult(val allowed: Boolean, val reason: String)

/**
 * Decides whether a video (its title/channel text) fits what the user said they want to watch.
 * Pure Kotlin so it can be unit tested.
 */
object IntentMatcher {

    fun evaluate(text: String, config: IntentConfig): MatchResult {
        val haystack = normalize(text)
        if (haystack.isBlank()) return MatchResult(true, "Nothing to check")

        config.blockedWords.firstOrNull { containsWord(haystack, it) }?.let {
            return MatchResult(false, "Contains blocked word \"$it\"")
        }
        config.allowedChannels.firstOrNull { containsWord(haystack, it) }?.let {
            return MatchResult(true, "Allowed channel \"$it\"")
        }
        for (topic in config.topics) {
            val hit = topic.keywords.firstOrNull { containsWord(haystack, it) }
            if (hit != null) return MatchResult(true, "Topic \"${topic.name}\" (matched \"$hit\")")
        }
        return MatchResult(false, "Does not match any of your topics")
    }

    /**
     * Weighted check for what was read on the video page: the first two texts (title and channel)
     * count as strong evidence. Topic words found only further down need at least two different hits.
     * Small spelling mistakes and word endings (trader / trading) are tolerated.
     */
    fun evaluate(texts: List<String>, config: IntentConfig): MatchResult {
        val cleaned = texts.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return MatchResult(true, "Nothing to check")
        val all = cleaned.joinToString(" | ")
        val strong = cleaned.take(2).joinToString(" | ")

        config.blockedWords.firstOrNull { matches(all, it) }?.let {
            return MatchResult(false, "Contains blocked word \"$it\"")
        }
        config.allowedChannels.firstOrNull { matches(all, it) }?.let {
            return MatchResult(true, "Allowed channel \"$it\"")
        }
        for (topic in config.topics) {
            val hit = topic.keywords.firstOrNull { matches(strong, it) }
            if (hit != null) return MatchResult(true, "Topic \"${topic.name}\" (matched \"$hit\")")
        }
        val weakHits = config.topics.flatMap { it.keywords }.filter { matches(all, it) }.distinct()
        if (weakHits.size >= 2) {
            return MatchResult(true, "Several topic words (${weakHits.take(2).joinToString(", ")})")
        }
        return MatchResult(false, "Does not match any of your topics")
    }

    private fun matches(haystack: String, keyword: String): Boolean =
        containsWord(haystack, keyword) || fuzzyMatch(haystack, keyword)

    private fun fuzzyMatch(haystack: String, keyword: String): Boolean {
        val k = normalize(keyword)
        if (k.length < 5 || k.contains(' ') || !k.all { it.code < 128 && it.isLetter() }) return false
        val stemmedKeyword = stem(k)
        return haystack.split(Regex("[^a-z0-9]+")).any { token ->
            token.length >= 5 && (
                (k.length >= 6 && stem(token) == stemmedKeyword) ||
                    (token[0] == k[0] && withinOneEdit(token, k))
                )
        }
    }

    /** Cuts common English endings: trader, trading -> trad. */
    fun stem(word: String): String {
        for (suffix in listOf("ing", "ers", "er", "ed", "es", "s")) {
            if (word.length > suffix.length + 3 && word.endsWith(suffix)) return word.dropLast(suffix.length)
        }
        return word
    }

    private fun withinOneEdit(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
            } else {
                edits++
                if (edits > 1) return false
                when {
                    a.length > b.length -> i++
                    a.length < b.length -> j++
                    else -> {
                        i++
                        j++
                    }
                }
            }
        }
        edits += (a.length - i) + (b.length - j)
        return edits <= 1
    }

    fun normalize(text: String): String =
        text.lowercase().replace(Regex("\\s+"), " ").trim()

    /** Short ASCII keywords (like "ai", "sql", "react") must match as whole words; longer ones as substrings. */
    fun containsWord(haystack: String, keyword: String): Boolean {
        val k = normalize(keyword)
        if (k.isEmpty()) return false
        val needsBoundary = k.length <= 5 && k.all { it.code < 128 && it.isLetterOrDigit() }
        if (!needsBoundary) return haystack.contains(k)

        var from = 0
        while (true) {
            val index = haystack.indexOf(k, from)
            if (index < 0) return false
            val before = if (index == 0) ' ' else haystack[index - 1]
            val afterIndex = index + k.length
            val after = if (afterIndex >= haystack.length) ' ' else haystack[afterIndex]
            if (!isAsciiWordChar(before) && !isAsciiWordChar(after)) return true
            from = index + 1
        }
    }

    private fun isAsciiWordChar(c: Char) = c.code < 128 && c.isLetterOrDigit()

    // ─── Text <-> config, so the user can edit everything in plain text ──────

    /** One topic per line: "Name: keyword one, keyword two". */
    fun parseTopics(text: String): List<IntentTopic> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
            val colon = line.indexOf(':')
            if (colon > 0) {
                IntentTopic(
                    name = line.substring(0, colon).trim(),
                    keywords = splitList(line.substring(colon + 1))
                )
            } else {
                IntentTopic(name = line, keywords = listOf(line))
            }
        }

    fun splitList(text: String): List<String> =
        text.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
}

/** Starting topics, English + Banglish + a few Bangla words. The user can edit all of it. */
object IntentDefaults {
    val TOPICS_TEXT = """
Trading: trading, trader, forex, futures, option chain, options trading, candlestick, price action, technical analysis, support and resistance, chart pattern, scalping, swing trading, smart money, ict, smc, risk management, crypto, bitcoin, stock market, ট্রেডিং, ফরেক্স
Investment: investment, investing, invest, mutual fund, index fund, etf, stocks, portfolio, dividend, personal finance, compound interest, wealth, financial freedom, share market, dse, বিনিয়োগ, শেয়ার বাজার
CSE: programming, coding, software, developer, computer science, algorithm, data structure, dsa, leetcode, python, java, kotlin, android, web development, javascript, react, machine learning, deep learning, artificial intelligence, ai, database, sql, operating system, computer network, cybersecurity, git, linux, system design, c++, cse, প্রোগ্রামিং
English: english, ielts, toefl, grammar, vocabulary, spoken english, speaking, pronunciation, listening, writing skills, phrasal verbs, idioms, ইংরেজি
Entrepreneurship: entrepreneur, entrepreneurship, startup, start a business, business idea, founder, bootstrapping, side hustle, freelancing, marketing, sales, branding, ecommerce, leadership, ব্যবসা
""".trim()

    const val CHANNELS_TEXT = ""
    const val BLOCKED_TEXT = "prank, reaction, gossip, drama, roast"
}
