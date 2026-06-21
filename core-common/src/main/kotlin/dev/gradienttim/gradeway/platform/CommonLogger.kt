/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

open class CommonLogger(
    val onInfo: (message: String) -> Unit,
    val onWarn: (message: String) -> Unit,
    val onError: (message: String) -> Unit,
) : Logger {
    override fun info(message: String) = onInfo(message)
    override fun warn(message: String) = onWarn(message)
    override fun error(message: String) = onError(message)

    companion object {
        fun fromJavaLogger(logger: java.util.logging.Logger) = CommonLogger(
            onInfo = { message -> logger.info(message) },
            onWarn = { message -> logger.warning(message) },
            onError = { message -> logger.severe(message) },
        )

        fun fromSlf4jLogger(logger: org.slf4j.Logger) = CommonLogger(
            onInfo = { message -> logger.info(message) },
            onWarn = { message -> logger.warn(message) },
            onError = { message -> logger.error(message) },
        )
    }
}
