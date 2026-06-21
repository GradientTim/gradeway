/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.extensions.get
import java.io.File
import java.util.*

class CommonEnvironment(val gradeway: CommonGradeway) : Environment {
    private val config: GradewayConfig = gradeway.configs.config
    private val variables = mutableMapOf<String, Any>()

    init {
        config.database.variables.forEach { (key, value) -> variables[key] = value }
        config.messaging.variables.forEach { (key, value) -> variables[key] = value }

        if (config.env.readFromFile) {
            loadFromFile(File(config.env.file))
        }
    }

    @Suppress("ForbiddenComment")
    private fun loadFromFile(file: File) {
        if (!file.exists()) return
        if (!file.canRead()) {
            gradeway.logger.warn("Cannot read content from env file '${file.absolutePath}'")
            return
        }

        // TODO: replace with a dotenv library to support advanced edge cases like VAR="${OTHER}"
        val properties = Properties()
        file.inputStream().use { properties.load(it) }

        properties.forEach { (key, value) -> variables[key.toString()] = value }
    }

    override fun int(vararg names: String): Int? = string(*names)?.toIntOrNull()
    override fun long(vararg names: String): Long? = string(*names)?.toLongOrNull()
    override fun double(vararg names: String): Double? = string(*names)?.toDoubleOrNull()
    override fun string(vararg names: String): String? = get(*names, transform = { it.toString() })
    override fun boolean(vararg names: String): Boolean? = string(*names)?.toBooleanStrictOrNull()

    override fun intRequired(vararg names: String): Int = int(*names) ?: missingVariablesError(*names)
    override fun longRequired(vararg names: String): Long = long(*names) ?: missingVariablesError(*names)
    override fun doubleRequired(vararg names: String): Double = double(*names) ?: missingVariablesError(*names)
    override fun stringRequired(vararg names: String): String = string(*names) ?: missingVariablesError(*names)
    override fun booleanRequired(vararg names: String): Boolean = boolean(*names) ?: missingVariablesError(*names)

    internal fun resolveVariableValue(name: String): Any? {
        variables[name]?.let { return it }
        if (config.env.readFromProperties) {
            System.getProperty(name)?.let { return it }
        }
        if (config.env.readFromSystem) {
            System.getenv(name)?.let { return it }
        }
        return null
    }

    internal fun missingVariablesError(vararg names: String): Nothing =
        error("Variables '${names.joinToString(", ")}' are not defined but one of them is required.")
}
