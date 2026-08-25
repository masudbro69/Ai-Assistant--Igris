package com.igris.assistant.util

/** Recursive-descent arithmetic evaluator: + - * / % ^, parentheses, functions, constants. */
class ExpressionEvaluator {
    private var src = ""
    private var pos = 0

    fun evaluate(expr: String): Double {
        src = expr.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        pos = 0
        if (src.isEmpty()) throw IllegalArgumentException("empty")
        val v = parseExpression()
        if (pos != src.length) throw IllegalArgumentException("trailing: ${src.substring(pos)}")
        return v
    }

    private fun peek(): Char? = if (pos < src.length) src[pos] else null
    private fun eat(c: Char): Boolean = if (peek() == c) { pos++; true } else false

    private fun parseExpression(): Double {
        var v = parseTerm()
        while (true) {
            when {
                eat('+') -> v += parseTerm()
                eat('-') -> v -= parseTerm()
                else -> return v
            }
        }
    }

    private fun parseTerm(): Double {
        var v = parseFactor()
        while (true) {
            when {
                eat('*') -> v *= parseFactor()
                eat('/') -> v /= parseFactor()
                eat('%') -> v %= parseFactor()
                else -> return v
            }
        }
    }

    private fun parseFactor(): Double {
        if (eat('-')) return -parseFactor()
        if (eat('+')) return parseFactor()
        val base = parseAtom()
        if (eat('^')) return Math.pow(base, parseFactor())
        return base
    }

    private fun parseAtom(): Double {
        if (eat('(')) {
            val v = parseExpression()
            if (!eat(')')) throw IllegalArgumentException("missing )")
            return v
        }
        // number
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        if (pos > start) return src.substring(start, pos).toDouble()
        // identifier (function / constant)
        val idStart = pos
        while (pos < src.length && src[pos].isLetter()) pos++
        val id = src.substring(idStart, pos).lowercase()
        if (id.isEmpty()) throw IllegalArgumentException("unexpected at $pos")
        return when (id) {
            "pi" -> Math.PI
            "e" -> Math.E
            "tau" -> 2 * Math.PI
            else -> {
                if (!eat('(')) throw IllegalArgumentException("unknown: $id")
                val arg = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("missing )")
                when (id) {
                    "sqrt" -> Math.sqrt(arg)
                    "abs" -> Math.abs(arg)
                    "sin" -> Math.sin(Math.toRadians(arg))
                    "cos" -> Math.cos(Math.toRadians(arg))
                    "tan" -> Math.tan(Math.toRadians(arg))
                    "log" -> Math.log10(arg)
                    "ln" -> Math.log(arg)
                    "round" -> Math.round(arg).toDouble()
                    "floor" -> Math.floor(arg)
                    "ceil" -> Math.ceil(arg)
                    else -> throw IllegalArgumentException("unknown fn: $id")
                }
            }
        }
    }

    companion object {
        /** Extracts the longest arithmetic-looking substring from a sentence. */
        fun extract(raw: String): String? {
            val cleaned = raw.lowercase()
                .replace("what is", "").replace("calculate", "").replace("compute", "")
                .replace("solve", "").replace("hisab", "").replace("koro", "")
                .replace("plus", "+").replace("minus", "-").replace("times", "*")
                .replace("multiplied by", "*").replace("multiply", "*")
                .replace("divided by", "/").replace("over", "/")
                .replace("to the power", "^").replace("power", "^")
                .replace("=", " ")
            return Regex("[0-9()+\\-*/%.^\\s]+").findAll(cleaned)
                .mapNotNull { r ->
                    val v = r.value.trim()
                    v.takeIf {
                        it.length >= 3 && it.any { c -> c.isDigit() } && it.any { c -> "+-*/%^".contains(c) }
                    }
                }
                .maxByOrNull { it.length }
        }
    }
}
