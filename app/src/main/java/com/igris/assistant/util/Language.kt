package com.igris.assistant.util

enum class Lang { EN, BN, MIXED }

enum class ResponseStyle { NEUTRAL, CONCISE, DETAILED, SIMPLE, EMPATHETIC }

/** Detects Bangla (Unicode range U+0980–U+09FF) vs English and mixed Banglish. */
object LanguageDetector {
    fun detect(text: String): Lang {
        var bn = 0
        var en = 0
        for (ch in text) {
            when {
                ch.code in 0x0980..0x09FF -> bn++
                ch.isLetter() -> en++
                else -> {}
            }
        }
        return when {
            bn == 0 -> Lang.EN
            en == 0 -> Lang.BN
            else -> Lang.MIXED
        }
    }

    val BANGISH_MARKERS = listOf(
        "koro", "korben", "bolben", "bolo", "dao", "dey", "lagao", "on", "off",
        "ki", "kichu", "amar", "tomar", "bhai", "apni", "thik", "acha", "shuru"
    )
}

/** Lightweight conversational-tone heuristics (never used for sensitive inference). */
object ToneAnalyzer {
    fun style(text: String): ResponseStyle {
        val t = text.lowercase()
        val confused = listOf("?", "bujhi na", "bujhte parina", "dont understand", "confused", "mone hocche na")
        val quick = listOf("short", "briefly", "quickly", "one line", "songoche", "short e", "concise")
        val detailed = listOf("explain", "details", "bistarito", "step by step", "why", "kotha", "how does")
        val empath = listOf("sad", "tired", "stress", "khub kharap", "bhalo lagche na", "frustrated", "angry")
        return when {
            empath.any { t.contains(it) } -> ResponseStyle.EMPATHETIC
            quick.any { t.contains(it) } -> ResponseStyle.CONCISE
            confused.any { t.contains(it) } -> ResponseStyle.SIMPLE
            detailed.any { t.contains(it) } -> ResponseStyle.DETAILED
            else -> ResponseStyle.NEUTRAL
        }
    }
}

object Texts {
    fun extractNumbers(text: String): List<Double> {
        val nums = mutableListOf<Double>()
        val bnDigits = mapOf('০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9')
        val normalized = text.map { bnDigits[it] ?: it }.joinToString("")
        val en = Regex("\\d+(?:\\.\\d+)?").findAll(normalized).mapNotNull { it.value.toDoubleOrNull() }
        nums.addAll(en)
        // Bangla word numbers for common values
        val words = mapOf(
            "ek" to 1.0, "one" to 1.0, "dui" to 2.0, "two" to 2.0, "tin" to 3.0, "three" to 3.0,
            "char" to 4.0, "four" to 4.0, "panch" to 5.0, "five" to 5.0, "choy" to 6.0, "six" to 6.0,
            "sat" to 7.0, "seven" to 7.0, "at" to 8.0, "eight" to 8.0, "noy" to 9.0, "nine" to 9.0,
            "ten" to 10.0, "dash" to 10.0, "fifteen" to 15.0, "twenty" to 20.0, "thirty" to 30.0
        )
        val lower = normalized.lowercase()
        words.forEach { (w, v) -> if (Regex("\\b$w\\b").containsMatchIn(lower)) nums.add(v) }
        return nums.distinct()
    }

    fun keywords(text: String): List<String> =
        text.lowercase().split(Regex("[^a-z0-9\u0980-\u09FF]+")).filter { it.length > 2 }
}
