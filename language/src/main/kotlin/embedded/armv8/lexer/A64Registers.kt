package com.asulcons.embedded.armv8.lexer

/**
 * Recognises AArch64 register spellings straight off the character buffer.
 *
 * This lives in the lexer rather than in an annotator because a register can carry a suffix that would
 * otherwise be shredded into unusable pieces: `v0.16b` would lex as identifier + `.` + number + identifier,
 * and `p0/z` as identifier + division. Matching the whole thing here keeps the operand grammar sane.
 */
object A64Registers {

    /** Architectural aliases that are not `<letter><number>` shaped. */
    val NAMED: Set<String> = setOf("sp", "wsp", "xzr", "wzr", "lr", "fp", "pc", "ip0", "ip1")

    /** Highest register index per bank; anything above is lexed as a register but reported by inspection. */
    fun maxIndexFor(kind: Char): Int = when (kind.lowercaseChar()) {
        'x', 'w' -> 30 // 31 is `sp`/`xzr` depending on the instruction, never spelled `x31`
        'b', 'h', 's', 'd', 'q', 'v', 'z' -> 31
        'p' -> 15
        else -> -1
    }

    /**
     * @param wordEnd end of the plain `[A-Za-z0-9_$]+` run starting at [start]
     * @return the end offset of the register token including any `.arrangement` / `/z` suffix,
     *         or `-1` when the word is not a register at all
     */
    fun match(text: CharSequence, start: Int, wordEnd: Int, limit: Int): Int {
        val length = wordEnd - start
        if (length < 2 || length > 4) return -1

        val kind = text[start].lowercaseChar()
        val indexed = length in 2..3 && matchIndex(text, start + 1, wordEnd, kind) >= 0
        if (!indexed) {
            val name = text.subSequence(start, wordEnd).toString().lowercase()
            return if (name in NAMED) wordEnd else -1
        }

        var end = wordEnd
        if (kind == 'v' || kind == 'z' || kind == 'p') {
            end = matchArrangement(text, end, limit)
        }
        if (kind == 'p') {
            end = matchPredication(text, end, limit)
        }
        return end
    }

    /** `x0`..`x31` style suffix; returns the register number or `-1`. Leading zeros are rejected. */
    private fun matchIndex(text: CharSequence, from: Int, to: Int, kind: Char): Int {
        if (maxIndexFor(kind) < 0) return -1
        if (to - from !in 1..2) return -1
        if (to - from == 2 && text[from] == '0') return -1
        var value = 0
        for (i in from until to) {
            val c = text[i]
            if (c < '0' || c > '9') return -1
            value = value * 10 + (c - '0')
        }
        // `x31`/`p16` are out of range but still lexed as registers so that the inspection can explain why.
        return if (value <= 31) value else -1
    }

    /** `.16b`, `.4s`, `.d`, … */
    private fun matchArrangement(text: CharSequence, from: Int, limit: Int): Int {
        if (from >= limit || text[from] != '.') return from
        var i = from + 1
        var digits = 0
        while (i < limit && text[i].isDigit() && digits < 2) {
            i++
            digits++
        }
        if (i >= limit || text[i].lowercaseChar() !in "bhsdq") return from
        i++
        if (i < limit && isIdentifierPart(text[i])) return from
        return i
    }

    /** SVE predicate qualifier `/z` (zeroing) or `/m` (merging). */
    private fun matchPredication(text: CharSequence, from: Int, limit: Int): Int {
        if (from + 1 >= limit || text[from] != '/') return from
        if (text[from + 1].lowercaseChar() !in "zm") return from
        if (from + 2 < limit && isIdentifierPart(text[from + 2])) return from
        return from + 2
    }

    fun isValidRegisterName(name: String): Boolean {
        val lower = name.lowercase()
        if (lower in NAMED) return true
        val base = lower.substringBefore('.').substringBefore('/')
        if (base.length < 2) return false
        val max = maxIndexFor(base[0])
        if (max < 0) return false
        val index = base.substring(1).toIntOrNull() ?: return false
        if (base.length > 2 && base[1] == '0') return false
        return index <= max
    }
}

internal fun isIdentifierStart(c: Char): Boolean = c.isLetter() || c == '_' || c == '$'

internal fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

internal fun isHexDigit(c: Char): Boolean =
    c.isDigit() || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
