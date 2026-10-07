package com.example.voice

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class WakeWordMatchResult(
    val detected: Boolean,
    val matchedPhrase: String?,
    val cleanCommand: String
)

object WakeWordDetector {
    private val primaryPhrases = listOf(
        "hey eva",
        "wake eva",
        "hey eevva",
        "hey eeva",
        "hey eva",
        "wake up eva",
        "hi eva",
        "ok eva",
        "okay eva"
    )

    fun detectAndStrip(input: String): WakeWordMatchResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return WakeWordMatchResult(detected = false, matchedPhrase = null, cleanCommand = "")
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check exact prefix/containment match
        for (phrase in primaryPhrases) {
            if (lower.startsWith(phrase)) {
                val command = trimmed.substring(phrase.length).trim().removePrefix(",").trim()
                return WakeWordMatchResult(
                    detected = true,
                    matchedPhrase = phrase,
                    cleanCommand = command
                )
            }
        }

        // 2. Fuzzy phonetic check on the first 2 or 3 words
        val words = trimmed.split(Regex("\\s+"))
        if (words.size >= 2) {
            val twoWords = "${words[0]} ${words[1]}".lowercase(Locale.ROOT)
            val threeWords = if (words.size >= 3) "${words[0]} ${words[1]} ${words[2]}".lowercase(Locale.ROOT) else ""

            for (phrase in primaryPhrases) {
                if (isPhoneticallySimilar(twoWords, phrase)) {
                    val remaining = words.drop(2).joinToString(" ").removePrefix(",").trim()
                    return WakeWordMatchResult(
                        detected = true,
                        matchedPhrase = twoWords,
                        cleanCommand = remaining
                    )
                }
                if (threeWords.isNotEmpty() && isPhoneticallySimilar(threeWords, phrase)) {
                    val remaining = words.drop(3).joinToString(" ").removePrefix(",").trim()
                    return WakeWordMatchResult(
                        detected = true,
                        matchedPhrase = threeWords,
                        cleanCommand = remaining
                    )
                }
            }
        }

        // Just single word "eva" or fuzzy "eva" at start
        if (words.isNotEmpty()) {
            val first = words[0].lowercase(Locale.ROOT)
            if (first == "eva" || first == "eevva" || first == "eeva") {
                val remaining = words.drop(1).joinToString(" ").removePrefix(",").trim()
                return WakeWordMatchResult(
                    detected = true,
                    matchedPhrase = first,
                    cleanCommand = remaining
                )
            }
        }

        return WakeWordMatchResult(
            detected = false,
            matchedPhrase = null,
            cleanCommand = trimmed
        )
    }

    private fun isPhoneticallySimilar(a: String, b: String): Boolean {
        val normA = normalizePhonetic(a)
        val normB = normalizePhonetic(b)
        if (normA == normB) return true
        val dist = levenshteinDistance(normA, normB)
        val maxLen = max(normA.length, normB.length)
        if (maxLen == 0) return true
        val similarity = 1.0 - (dist.toDouble() / maxLen)
        return similarity >= 0.75
    }

    private fun normalizePhonetic(str: String): String {
        return str.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
            .replace(Regex("ee+"), "e")
            .replace(Regex("vv+"), "v")
            .replace(Regex("aa+"), "a")
            .replace(Regex("ph"), "f")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
