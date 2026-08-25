package com.igris.assistant

import com.igris.assistant.util.Lang
import com.igris.assistant.util.LanguageDetector
import com.igris.assistant.util.ResponseStyle
import com.igris.assistant.util.ToneAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {
    @Test fun detect() {
        assertEquals(Lang.BN, LanguageDetector.detect("আমার নাম মাসুদ"))
        assertEquals(Lang.EN, LanguageDetector.detect("hello there"))
        assertEquals(Lang.MIXED, LanguageDetector.detect("amar name masud আমার নাম"))
    }

    @Test fun tone() {
        assertEquals(ResponseStyle.CONCISE, ToneAnalyzer.style("short e bolo"))
        assertEquals(ResponseStyle.SIMPLE, ToneAnalyzer.style("i dont understand this?"))
    }
}
