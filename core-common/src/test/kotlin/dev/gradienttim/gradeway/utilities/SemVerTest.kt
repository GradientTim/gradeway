/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SemVerTest {
    @Test
    fun `parse extracts all components`() {
        assertEquals(SemVer(1, 2, 3, build = "build.5", preRelease = "rc.1"), SemVer.parse("1.2.3-rc.1+build.5"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
    }

    @Test
    fun `parse rejects invalid versions`() {
        listOf("1.2", "01.2.3", "1.2.3-", "1.2.3-01", "1.2.3+", "1.2.3-a..b", "99999999999.0.0").forEach {
            assertFailsWith<IllegalArgumentException>(it) { SemVer.parse(it) }
        }
    }

    @Test
    fun `constructor rejects invalid pre-release and build`() {
        assertFailsWith<IllegalArgumentException> { SemVer(1, 0, 0, preRelease = "foo..bar") }
        assertFailsWith<IllegalArgumentException> { SemVer(1, 0, 0, build = "a+b") }
        assertFailsWith<IllegalArgumentException> { SemVer(-1, 0, 0) }
    }

    @Test
    fun `compareTo follows spec precedence`() {
        val ordered = listOf(
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
            "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0", "2.0.0"
        ).map(SemVer::parse)
        ordered.zipWithNext().forEach { (lower, higher) -> assertTrue(lower < higher, "$lower < $higher") }
        assertEquals(ordered, ordered.shuffled().sorted())
    }

    @Test
    fun `compareTo ignores build metadata`() {
        assertEquals(0, SemVer.parse("1.0.0+a").compareTo(SemVer.parse("1.0.0+b")))
    }
}
