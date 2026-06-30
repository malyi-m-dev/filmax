package com.filmax.detekt.rules

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NestedIfTest {

    @Test
    fun `flags an if nested in the then-branch of another if`() {
        val code = """
            fun check(a: Boolean, b: Boolean) {
                if (a) {
                    if (b) {
                        println("nested")
                    }
                }
            }
        """.trimIndent()
        val findings = NestedIf().compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `flags a braceless nested if`() {
        val code = """
            fun check(a: Boolean, b: Boolean) {
                if (a) if (b) println("nested")
            }
        """.trimIndent()
        val findings = NestedIf().compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag an else-if chain`() {
        val code = """
            fun check(a: Boolean, b: Boolean) {
                if (a) {
                    println("a")
                } else if (b) {
                    println("b")
                }
            }
        """.trimIndent()
        val findings = NestedIf().compileAndLint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not flag a single if`() {
        val code = """
            fun check(a: Boolean) {
                if (a) {
                    println("a")
                }
            }
        """.trimIndent()
        val findings = NestedIf().compileAndLint(code)
        assertTrue(findings.isEmpty())
    }
}
