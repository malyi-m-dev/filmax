package com.filmax.detekt.rules

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SingleLetterNameTest {

    @Test
    fun `flags a single-letter lambda parameter`() {
        val code = """
            fun lengths(words: List<String>) = words.map { s -> s.length }
        """.trimIndent()
        assertEquals(1, SingleLetterName().compileAndLint(code).size)
    }

    @Test
    fun `flags each single-letter part of a destructuring`() {
        val code = """
            fun sums(pairs: List<Pair<Int, Int>>) = pairs.map { (a, b) -> a + b }
        """.trimIndent()
        assertEquals(2, SingleLetterName().compileAndLint(code).size)
    }

    @Test
    fun `flags single-letter local variables and loop variables`() {
        val code = """
            fun total(prices: List<Int>): Int {
                var t = 0
                for (p in prices) t += p
                return t
            }
        """.trimIndent()
        assertEquals(2, SingleLetterName().compileAndLint(code).size)
    }

    @Test
    fun `flags single-letter function and catch parameters`() {
        val code = """
            fun parse(s: String): Int = try {
                s.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        """.trimIndent()
        assertEquals(2, SingleLetterName().compileAndLint(code).size)
    }

    @Test
    fun `allows it, underscore, type parameters and real names`() {
        val code = """
            fun <T> firstOrDefault(items: List<T>, fallback: T): T = items.firstOrNull() ?: fallback

            fun lengths(words: List<String>) = words.map { it.length }

            fun keys(entries: Map<String, Int>) = entries.map { (key, _) -> key }

            fun visible(items: List<String>) = items.filter { item -> item.isNotBlank() }
        """.trimIndent()
        assertTrue(SingleLetterName().compileAndLint(code).isEmpty())
    }
}
