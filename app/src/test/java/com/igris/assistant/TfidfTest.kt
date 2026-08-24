package com.igris.assistant

import com.igris.assistant.knowledge.TfidfIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class TfidfTest {
    @Test fun ranks() {
        val idx = TfidfIndex()
        idx.add("perfume", "perfume pricing is 1200 taka per bottle")
        idx.add("study", "physics notes on newton laws motion")
        val res = idx.search("perfume pricing", 2)
        assertEquals("perfume", res.first().first)
    }

    @Test fun bangla() {
        val idx = TfidfIndex()
        idx.add("a", "আমার সোনার বাংলা আমি তোমায় ভালোবাসি")
        idx.add("b", "english text about java")
        val res = idx.search("বাংলা ভালোবাসি", 2)
        assertEquals("a", res.first().first)
    }
}
