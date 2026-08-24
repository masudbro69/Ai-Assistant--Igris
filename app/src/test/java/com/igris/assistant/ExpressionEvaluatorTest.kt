package com.igris.assistant

import com.igris.assistant.util.ExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {
    @Test fun basic() {
        assertEquals(7.0, ExpressionEvaluator().evaluate("3+4"), 1e-9)
        assertEquals(14.0, ExpressionEvaluator().evaluate("2+3*4"), 1e-9)
        assertEquals(20.0, ExpressionEvaluator().evaluate("(2+3)*4"), 1e-9)
        assertEquals(8.0, ExpressionEvaluator().evaluate("2^3"), 1e-9)
        assertEquals(2.5, ExpressionEvaluator().evaluate("5/2"), 1e-9)
    }

    @Test fun functions() {
        assertEquals(4.0, ExpressionEvaluator().evaluate("sqrt(16)"), 1e-9)
        assertEquals(90.0, ExpressionEvaluator().evaluate("sin(90)"), 1e-9)
        assertEquals(2.0, ExpressionEvaluator().evaluate("log(100)"), 1e-9)
    }

    @Test fun extract() {
        val e = ExpressionEvaluator.extract("what is 12 plus 8?")!!.replace("plus", "+")
        assertTrue(e.contains("12"))
        val v = ExpressionEvaluator().evaluate(ExpressionEvaluator.extract("calculate 6*7")!!)
        assertEquals(42.0, v, 1e-9)
    }
}
