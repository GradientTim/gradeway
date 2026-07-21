/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.managers.CommonConfigManager
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CommonEnvironmentTest {
    private var gradeway: CommonGradeway? = null

    /**
     * [CommonGradeway.environment] is created lazily on first access and, once created, keeps
     * whatever [GradewayConfig] was current at that moment - so the config must be swapped in
     * *before* [CommonGradeway.environment] is ever touched (which normally happens the first
     * time a database driver is enabled). Loading (without enabling) leaves it untouched, so we
     * can safely overwrite [dev.gradienttim.gradeway.managers.ConfigManager.config] here first.
     */
    private fun createEnvironment(config: GradewayConfig): Environment {
        val instance = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
            directory = Files.createTempDirectory("environment-test").toFile(),
        )
        instance.load().getOrElse { error(it.toString()) }
        (instance.configs as CommonConfigManager).config = config
        gradeway = instance
        return instance.environment
    }

    @AfterTest
    fun tearDown() {
        gradeway?.unload()?.getOrElse { error(it.toString()) }
        gradeway = null
    }

    @Test
    fun `string int long double and boolean read config-declared variables`() {
        val environment = createEnvironment(
            GradewayConfig(
                database = GradewayConfig.DatabaseConfig(
                    variables = mapOf(
                        "TEST_STRING" to "hello",
                        "TEST_INT" to "42",
                        "TEST_LONG" to "123456789012",
                        "TEST_DOUBLE" to "3.14",
                        "TEST_BOOL" to "true",
                    )
                ),
                env = GradewayConfig.EnvConfig(readFromFile = false),
            )
        )

        assertEquals("hello", environment.string("TEST_STRING"))
        assertEquals(42, environment.int("TEST_INT"))
        assertEquals(123456789012L, environment.long("TEST_LONG"))
        assertEquals(3.14, environment.double("TEST_DOUBLE"))
        assertEquals(true, environment.boolean("TEST_BOOL"))
    }

    @Test
    fun `database and messaging variables are both consulted`() {
        val environment = createEnvironment(
            GradewayConfig(
                database = GradewayConfig.DatabaseConfig(variables = mapOf("FROM_DATABASE" to "db-value")),
                messaging = GradewayConfig.MessagingConfig(variables = mapOf("FROM_MESSAGING" to "msg-value")),
                env = GradewayConfig.EnvConfig(readFromFile = false),
            )
        )

        assertEquals("db-value", environment.string("FROM_DATABASE"))
        assertEquals("msg-value", environment.string("FROM_MESSAGING"))
    }

    @Test
    fun `an unset variable resolves to null and required variants throw`() {
        val environment = createEnvironment(GradewayConfig(env = GradewayConfig.EnvConfig(readFromFile = false)))

        assertNull(environment.string("MISSING"))
        assertNull(environment.int("MISSING"))
        assertFailsWith<IllegalStateException> { environment.stringRequired("MISSING") }
        assertFailsWith<IllegalStateException> { environment.intRequired("MISSING") }
    }

    @Test
    fun `an unparsable value returns null instead of throwing`() {
        val environment = createEnvironment(
            GradewayConfig(
                database = GradewayConfig.DatabaseConfig(variables = mapOf("NOT_A_NUMBER" to "abc")),
                env = GradewayConfig.EnvConfig(readFromFile = false),
            )
        )

        assertEquals("abc", environment.string("NOT_A_NUMBER"))
        assertNull(environment.int("NOT_A_NUMBER"))
    }

    @Test
    fun `defaults are used only when the variable is missing`() {
        val environment = createEnvironment(
            GradewayConfig(
                database = GradewayConfig.DatabaseConfig(variables = mapOf("SET_VAR" to "10")),
                env = GradewayConfig.EnvConfig(readFromFile = false),
            )
        )

        assertEquals(10, environment.intDefault("SET_VAR", default = 99))
        assertEquals(99, environment.intDefault("UNSET_VAR", default = 99))
    }

    @Test
    fun `names are searched in order and the first match wins`() {
        val environment = createEnvironment(
            GradewayConfig(
                database = GradewayConfig.DatabaseConfig(variables = mapOf("SECOND" to "second-value")),
                env = GradewayConfig.EnvConfig(readFromFile = false),
            )
        )

        assertEquals("second-value", environment.string("FIRST", "SECOND"))
    }

    @Test
    fun `readFromProperties consults JVM system properties when enabled`() {
        val propertyName = "gradeway.test.${System.nanoTime()}"
        System.setProperty(propertyName, "from-system-property")
        try {
            val environment = createEnvironment(
                GradewayConfig(env = GradewayConfig.EnvConfig(readFromFile = false, readFromProperties = true))
            )

            assertEquals("from-system-property", environment.string(propertyName))
        } finally {
            System.clearProperty(propertyName)
        }
    }

    @Test
    fun `config-declared variables take precedence over system properties`() {
        val propertyName = "gradeway.test.${System.nanoTime()}"
        System.setProperty(propertyName, "from-system-property")
        try {
            val environment = createEnvironment(
                GradewayConfig(
                    database = GradewayConfig.DatabaseConfig(variables = mapOf(propertyName to "from-config")),
                    env = GradewayConfig.EnvConfig(readFromFile = false, readFromProperties = true),
                )
            )

            assertEquals("from-config", environment.string(propertyName))
        } finally {
            System.clearProperty(propertyName)
        }
    }
}
