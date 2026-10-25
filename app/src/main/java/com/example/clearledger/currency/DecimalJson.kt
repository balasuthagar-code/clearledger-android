package com.example.clearledger.currency

internal data class JsonNumber(val text: String)
/** Bounded strict JSON parser that retains numeric lexemes; no floating-point intermediate. */
internal class DecimalJson private constructor(private val text: String) {
    private var i = 0
    private var nodes = 0
    private fun ws() { while (i < text.length && text[i] in " \t\r\n") i++ }
    private fun take(c: Char): Boolean { ws(); return if (i < text.length && text[i] == c) { i++; true } else false }
    private fun expect(c: Char) { if (!take(c)) fail(QuoteFailure.MALFORMED) }
    private fun value(depth: Int): Any? {
        if (depth > 8 || ++nodes > 4096) fail(QuoteFailure.MALFORMED)
        ws(); if (i >= text.length) fail(QuoteFailure.MALFORMED)
        return when (text[i]) {
            '{' -> {
                i++; val map = linkedMapOf<String, Any?>()
                if (!take('}')) do {
                    ws(); val key = string(); expect(':')
                    if (map.containsKey(key)) fail(QuoteFailure.MALFORMED)
                    map[key] = value(depth + 1)
                    if (take('}')) break
                    expect(',')
                } while (true)
                map
            }
            '[' -> {
                i++; val list = mutableListOf<Any?>()
                if (!take(']')) do {
                    list += value(depth + 1)
                    if (take(']')) break
                    expect(',')
                } while (true)
                list
            }
            '"' -> string()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> {
                val start = i
                while (i < text.length && text[i] in "-+0123456789.eE") i++
                val token = text.substring(start, i)
                if (token.length !in 1..64 || !NUMBER.matches(token)) fail(QuoteFailure.MALFORMED)
                JsonNumber(token)
            }
        }
    }
    private fun literal(token: String, result: Any?): Any? {
        if (!text.startsWith(token, i)) fail(QuoteFailure.MALFORMED)
        i += token.length; return result
    }
    private fun string(): String {
        expect('"'); val result = StringBuilder()
        while (i < text.length) {
            val c = text[i++]
            if (c == '"') return result.toString()
            if (c < ' ') fail(QuoteFailure.MALFORMED)
            if (c == '\\') {
                if (i >= text.length) fail(QuoteFailure.MALFORMED)
                result.append(when (val e = text[i++]) {
                    '"', '\\', '/' -> e
                    'b' -> '\b'; 'f' -> '\u000C'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                    'u' -> {
                        if (i + 4 > text.length) fail(QuoteFailure.MALFORMED)
                        val hex = text.substring(i, i + 4)
                        if (!hex.all { it in "0123456789abcdefABCDEF" }) fail(QuoteFailure.MALFORMED)
                        i += 4; hex.toInt(16).toChar()
                    }
                    else -> fail(QuoteFailure.MALFORMED)
                })
            } else result.append(c)
            if (result.length > 4096) fail(QuoteFailure.MALFORMED)
        }
        fail(QuoteFailure.MALFORMED)
    }
    companion object {
        private val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
        fun parse(text: String): Any? {
            if (text.length > 65_536) fail(QuoteFailure.MALFORMED)
            val parser = DecimalJson(text)
            val value = parser.value(0); parser.ws()
            if (parser.i != text.length) fail(QuoteFailure.MALFORMED)
            return value
        }
    }
}
