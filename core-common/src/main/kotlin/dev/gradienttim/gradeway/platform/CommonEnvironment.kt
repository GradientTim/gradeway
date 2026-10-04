/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.extensions.get
import io.github.cdimascio.dotenv.Dotenv
import java.nio.file.Files
import kotlin.io.path.isReadable
import kotlin.io.path.pathString

class CommonEnvironment(val gradeway: CommonGradeway<*>) : Environment {
    private val variables = mutableMapOf<String, Any>()

    init {
        val file = gradeway.directory.resolve(".env")
        if (file.isReadable() && Files.exists(file)) {
            val dotenv = Dotenv.configure()
                .directory(gradeway.directory.pathString)
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load()

            dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE).forEach { entry ->
                variables[entry.key] = entry.value
            }
        } else {
            gradeway.logger.warn(
                "Cannot read content from env file '${file.pathString}': " +
                        "Not readable or the file does not exists."
            )
        }
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
        System.getProperty(name)?.let { return it }
        System.getenv(name)?.let { return it }
        return null
    }

    internal fun missingVariablesError(vararg names: String): Nothing =
        error("Variables '${names.joinToString(", ")}' are not defined but one of them is required.")
}
