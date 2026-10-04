/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestScheduler
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CommonEnvironmentTest {
    private fun createEnvironment(
        variables: Map<String, String>? = emptyMap(),
        onWarn: (String) -> Unit = {},
    ): Environment {
        val directory = Files.createTempDirectory("environment-test")
        if (variables != null) {
            directory.resolve(".env").writeText(
                variables.entries.joinToString(separator = "\n") { (key, value) -> "$key=$value" }
            )
        }

        val gradeway = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = onWarn, onError = {}, onPanic = {}),
            scheduler = TestScheduler(),
            directory = directory,
            defaultPlatformConfig = TestPlatformConfig(),
            platformConfigSerializer = TestPlatformConfig.serializer(),
        )
        return CommonEnvironment(gradeway)
    }

    @Test
    fun `string int long double and boolean read variables declared in the env file`() {
        val environment = createEnvironment(
            mapOf(
                "TEST_STRING" to "hello",
                "TEST_INT" to "42",
                "TEST_LONG" to "123456789012",
                "TEST_DOUBLE" to "3.14",
                "TEST_BOOL" to "true",
            )
        )

        assertEquals("hello", environment.string("TEST_STRING"))
        assertEquals(42, environment.int("TEST_INT"))
        assertEquals(123456789012L, environment.long("TEST_LONG"))
        assertEquals(3.14, environment.double("TEST_DOUBLE"))
        assertEquals(true, environment.boolean("TEST_BOOL"))
    }

    @Test
    fun `an unset variable resolves to null and required variants throw`() {
        val environment = createEnvironment()

        assertNull(environment.string("GRADEWAY_TEST_MISSING"))
        assertNull(environment.int("GRADEWAY_TEST_MISSING"))
        assertFailsWith<IllegalStateException> { environment.stringRequired("GRADEWAY_TEST_MISSING") }
        assertFailsWith<IllegalStateException> { environment.intRequired("GRADEWAY_TEST_MISSING") }
    }

    @Test
    fun `an unparsable value returns null instead of throwing`() {
        val environment = createEnvironment(mapOf("NOT_A_NUMBER" to "abc"))

        assertEquals("abc", environment.string("NOT_A_NUMBER"))
        assertNull(environment.int("NOT_A_NUMBER"))
    }

    @Test
    fun `defaults are used only when the variable is missing`() {
        val environment = createEnvironment(mapOf("SET_VAR" to "10"))

        assertEquals(10, environment.intDefault("SET_VAR", default = 99))
        assertEquals(99, environment.intDefault("GRADEWAY_TEST_UNSET_VAR", default = 99))
    }

    @Test
    fun `names are searched in order and the first match wins`() {
        val environment = createEnvironment(mapOf("SECOND" to "second-value"))

        assertEquals("second-value", environment.string("GRADEWAY_TEST_FIRST", "SECOND"))
    }

    @Test
    fun `system properties are consulted when the env file does not declare the variable`() {
        val propertyName = "gradeway.test.${System.nanoTime()}"
        System.setProperty(propertyName, "from-system-property")
        try {
            val environment = createEnvironment()

            assertEquals("from-system-property", environment.string(propertyName))
        } finally {
            System.clearProperty(propertyName)
        }
    }

    @Test
    fun `env file variables take precedence over system properties`() {
        val propertyName = "GRADEWAY_TEST_${System.nanoTime()}"
        System.setProperty(propertyName, "from-system-property")
        try {
            val environment = createEnvironment(mapOf(propertyName to "from-env-file"))

            assertEquals("from-env-file", environment.string(propertyName))
        } finally {
            System.clearProperty(propertyName)
        }
    }

    @Test
    fun `a missing env file logs a warning`() {
        val warnings = mutableListOf<String>()
        createEnvironment(variables = null, onWarn = { warnings.add(it) })

        assertEquals(1, warnings.size)
    }
}
