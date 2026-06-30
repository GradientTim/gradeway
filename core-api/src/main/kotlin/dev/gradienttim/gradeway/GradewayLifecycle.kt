/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import dev.gradienttim.gradeway.managers.ConfigManager
import dev.gradienttim.gradeway.managers.DatabaseManager
import dev.gradienttim.gradeway.managers.DriverManager
import dev.gradienttim.gradeway.managers.LanguageManager
import dev.gradienttim.gradeway.platform.Environment
import dev.gradienttim.gradeway.platform.Logger
import dev.gradienttim.gradeway.utilities.Loadable
import dev.gradienttim.gradeway.utilities.Reloadable
import dev.gradienttim.gradeway.utilities.Unloadable
import java.io.File

/**
 * Represents a lifecycle-aware extension of the Gradeway interface, adding support for
 * initialization and cleanup operations.
 *
 * This interface defines methods for explicitly managing the lifecycle of system components
 * associated with the Gradeway infrastructure. Implementations of this interface are expected
 * to handle tasks such as preparing the necessary resources during initialization and releasing them
 * during shutdown, ensuring correct and efficient usage of resources.
 *
 * Typical use cases include contexts where controlled lifecycle management is crucial, such as
 * in applications with dependency injection frameworks or systems managing multiple Gradeway
 * instances. By coupling lifecycle operations with the standard Gradeway capabilities,
 * this interface facilitates clean and predictable resource management practices.
 */
interface GradewayLifecycle : Gradeway, Loadable, Unloadable, Reloadable {
    val logger: Logger
    val directory: File
    val environment: Environment

    val databases: DatabaseManager
    val languages: LanguageManager
    val drivers: DriverManager
    val configs: ConfigManager

    val state: GradewayState

    /**
     * Initializes and prepares the implementing instance of the `GradewayLifecycle` interface.
     *
     * This method is used to bootstrap the required services and perform necessary setup operations
     * to make the instance fully functional. It should typically be called before any other methods
     * or operations of the implementing instance are invoked.
     *
     * Implementers can define custom logic for initializing resources such as connections,
     * dependencies, or any other preparatory steps required to ensure the Gradeway ecosystem
     * is fully operational.
     *
     * Note: It is the responsibility of the caller to ensure this method is called at the correct
     * lifecycle stage where initialization is appropriate.
     *
     * @return `true` when Gradway was successfully loaded, `false` otherwise.
     */
    override fun load(): Either<Throwable, Unit>

    /**
     * Cleans up and releases resources used by the implementing instance of the `GradewayLifecycle` interface.
     *
     * This method is used to properly shut down the instance by disposing of any acquired resources,
     * closing connections, and performing necessary teardown operations to ensure the system
     * transitions into a clean state. It is typically called when the instance is no longer needed
     * or before the application terminates.
     *
     * Implementers can define custom logic for safely releasing resources such as cached data,
     * external integrations, or any other dependencies that may need explicit cleanup.
     *
     * Note: It is the caller's responsibility to ensure that this method is invoked at the appropriate
     * lifecycle stage to avoid resource leaks.
     *
     * @return `true` when Gradway was successfully unloaded, `false` otherwise.
     */
    override fun unload(): Either<Throwable, Unit>

    /**
     * Reloads the Gradeway system, refreshing its current state by reinitializing its components.
     *
     * This method is used to restart or reset the internal state of the Gradeway system, ensuring
     * that the latest configurations, dependencies, and resources are loaded. It typically involves
     * unloading and reloading the system's components, such as services, databases, or configuration
     * files.
     *
     * @return `Either` containing a `Throwable` if an error occurred during the reload operation,
     *         or `Unit` if the operation completed successfully.
     */
    override fun reload(): Either<Throwable, Unit>
}
