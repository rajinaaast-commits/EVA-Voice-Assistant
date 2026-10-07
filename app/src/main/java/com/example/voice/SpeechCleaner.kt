package com.example.voice

import java.util.Locale

object SpeechCleaner {
    // Pure filler tokens that can always be safely removed
    private val pureDisfluencies = setOf(
        "umm", "um", "uh", "uhh", "ah", "ahh", "hmm", "hmmm", "er", "err"
    )

    fun clean(text: String): String {
        var processed = text.trim()
        if (processed.isEmpty()) return ""

        // Normalize ellipsis and repeated punctuation
        processed = processed.replace(Regex("\\.{2,}"), " ")
        processed = processed.replace(Regex("[,;]+"), " , ")

        val tokens = processed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val cleanedTokens = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val rawToken = tokens[i]
            val tokenClean = rawToken.lowercase(Locale.ROOT).replace(Regex("[^a-z']"), "")

            // 1. Check multi-word filler "you know"
            if (tokenClean == "you" && i + 1 < tokens.size) {
                val nextClean = tokens[i + 1].lowercase(Locale.ROOT).replace(Regex("[^a-z']"), "")
                if (nextClean == "know") {
                    // Check if filler (not "did you know that" or "do you know")
                    val prevClean = if (cleanedTokens.isNotEmpty()) cleanedTokens.last().lowercase(Locale.ROOT) else ""
                    if (prevClean != "do" && prevClean != "did") {
                        i += 2
                        continue
                    }
                }
            }

            // 2. Pure disfluencies
            if (pureDisfluencies.contains(tokenClean)) {
                i++
                continue
            }

            // 3. Contextual filler "like"
            // "I like YouTube" -> like is preceded by subject pronoun (I, we, they, you) -> KEEP
            // "looks like", "sounds like", "something like", "would like" -> KEEP
            // "Hey Eva, like, can you tell me..." or start of sentence "Like, tell me..." -> REMOVE
            if (tokenClean == "like") {
                val prevClean = if (cleanedTokens.isNotEmpty()) cleanedTokens.last().lowercase(Locale.ROOT).replace(Regex("[^a-z']"), "") else ""
                val subjectPronouns = setOf("i", "we", "they", "you", "he", "she", "who", "would", "looks", "feels", "sounds", "seems", "something", "anything")

                if (cleanedTokens.isEmpty() || !subjectPronouns.contains(prevClean)) {
                    // Is likely a filler word
                    i++
                    continue
                }
            }

            if (rawToken != ",") {
                cleanedTokens.add(rawToken)
            }
            i++
        }

        val result = cleanedTokens.joinToString(" ")
            .replace(Regex("\\s+([.,!?])"), "$1")
            .trim()

        return if (result.isNotEmpty()) {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            ""
        }
    }
}
