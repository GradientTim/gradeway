/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.Either
import dev.gradienttim.gradeway.managers.*
import dev.gradienttim.gradeway.platform.Environment
import dev.gradienttim.gradeway.platform.Logger
import dev.gradienttim.gradeway.utilities.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.time.Instant

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
interface GradewayLifecycle : Gradeway, Loadable, Unloadable, Reloadable, Enableable, Disableable {
    /**
     * Retrieves the current point in time as an [Instant].
     *
     * The implementation of the lambda ensures that the returned value is accurate to the moment
     * of invocation, making it suitable for scenarios where precise timing is required.
     */
    val now: () -> Instant

    /**
     * Logger instance used to log informational, warning, and error messages within the `GradewayLifecycle` class.
     *
     * This `Logger` serves as a centralized mechanism for recording events, debugging information, and
     * system errors throughout the lifecycle management of the Gradeway ecosystem. It facilitates consistent
     * and meaningful log entries to assist with monitoring, troubleshooting, and analyzing system behavior.
     *
     * The `Logger` is essential for ensuring visibility into Gradeway's internal processes, supporting both
     * operational needs and long-term maintenance of the system.
     */
    val logger: Logger

    /**
     * A coroutine scope for fire-and-forget background side-tasks (e.g., best-effort cleanup work
     * that should not block whatever hot path triggered it) that are tied to the Gradeway lifecycle.
     *
     * The scope is recreated fresh on every [load] and canceled on every [unload], so background
     * work launched here is guaranteed to be torn down alongside the database and other resources
     * it may depend on, rather than being able to outlive them across a reload.
     */
    val backgroundScope: CoroutineScope

    /**
     * Represents the root directory for the Gradeway system's runtime files,
     * including configurations, logs, and other associated resources.
     *
     * This directory serves as a primary location for storing data generated or
     * consumed by the Gradeway ecosystem during its lifecycle. It is typically
     * initialized during the system setup phase and is expected to persist across
     * different runtime sessions.
     */
    val directory: File

    /**
     * Represents the configuration and runtime environment for the Gradeway system.
     *
     * The `environment` property provides access to environmental variables and configuration
     * values essential for the operation of the Gradeway lifecycle. It acts as an abstraction
     * layer for retrieving various types of dynamically configured settings such as integers,
     * strings, booleans, and more. These settings can be required, optional, or have default
     * values when not explicitly provided.
     *
     * This environment abstraction is particularly useful for managing application-level
     * configurations, database settings, external API keys, and other runtime-specific values.
     */
    val environment: Environment

    /**
     * Manages database interactions and operations within the Gradeway system.
     *
     * This property provides an instance of the `DatabaseManager` interface, which serves as the
     * central point of access for handling database-related tasks such as loading, unloading,
     * and managing connections. Its lifecycle is controlled as part of the larger `GradewayLifecycle`,
     * ensuring that database operations are properly initialized and terminated during the application's
     * runtime.
     *
     * The `DatabaseManager` implementation is responsible for:
     * - Establishing connections to one or more databases.
     * - Performing the necessary cleanup to release resources when no longer needed.
     * - Supporting reloading operations to refresh database states.
     *
     * It adheres to the `Loadable` and `Unloadable` interfaces, ensuring proper management
     * of its lifecycle within the Gradeway ecosystem.
     */
    val databases: DatabaseManager

    /**
     * Manages language-related functionalities within the Gradeway system.
     *
     * This property provides access to an instance of [LanguageManager], which is responsible for
     * handling localization, translation, and any other language-specific features required
     * by the Gradeway ecosystem. It acts as a centralized component for managing and
     * interacting with language resources, enabling the system to support multiple locales
     * and dynamic language adjustments.
     *
     * Typical responsibilities include:
     * - Loading language files and resources.
     * - Managing translations for customizable text.
     * - Switching between supported languages dynamically.
     * - Ensuring consistent localization throughout different parts of the system.
     *
     * As an implementation of the [Loadable] and [Unloadable] interfaces,
     * [LanguageManager] supports lifecycle operations to initialize and release resources
     * associated with its language management processes.
     */
    val languages: LanguageManager

    /**
     * Manages messaging-related functionality within the Gradeway system.
     *
     * This property provides access to an instance of `MessagingManager`, which is responsible for handling
     * messaging operations such as sending, receiving, and managing messages. It serves as a core
     * component for the communication and messaging infrastructure within the system.
     *
     * Implementations of `MessagingManager` typically integrate with external messaging systems or
     * facilitate internal communication between components in the Gradeway ecosystem. It also ensures
     * that messaging processes comply with the system's lifecycle by supporting loading, unloading,
     * and reloading operations.
     *
     * Typical responsibilities of `MessagingManager` include:
     * - Sending messages to recipients or channels.
     * - Managing message queues and delivery statuses.
     * - Integrating with external messaging protocols, if applicable.
     */
    val messaging: MessagingManager

    /**
     * Provides centralized management and operations for driver-related functionality within the Gradeway system.
     *
     * The `drivers` property exposes an instance of the [DriverManager] interface, which acts as the primary access
     * point for interacting with and managing drivers used by the system. It integrates with the lifecycle
     * management of the Gradeway system, supporting operations such as loading, unloading, and reloading drivers.
     *
     * Key responsibilities include:
     * - Facilitating the discovery of specific drivers based on their unique IDs and types.
     * - Serving as a bridge for driver-related functionality, ensuring cohesive integration with other Gradeway components.
     * - Simplifying the management of external or internal drivers, such as database or messaging drivers.
     */
    val drivers: DriverManager

    /**
     * Manages configuration-related operations within the `GradewayLifecycle` system.
     *
     * The `configs` property provides access to an implementation of the `ConfigManager` interface,
     * which is responsible for handling the configuration settings used by the Gradeway system.
     * This includes managing dynamic or static configurations needed for the proper functioning
     * of the application.
     *
     * Typical responsibilities of the `configs` property include:
     * - Loading configuration data from external sources.
     * - Providing runtime access to configuration parameters.
     * - Ensuring the consistency and integrity of configuration settings during the application's lifecycle.
     *
     * The `configs` property plays a vital role in enabling seamless startup, operation, and reload processes
     * within the `GradewayLifecycle` system, particularly when configuration data requires frequent updates
     * or validation.
     */
    val configs: ConfigManager

    /**
     * Manages backup operations within the Gradeway system, providing functionality
     * for exporting and importing data.
     *
     * This property exposes an instance of [BackupManager], which facilitates handling
     * critical backup-related tasks, such as exporting the current state of the system
     * to an external format and importing data from a provided file. It serves as a
     * central component for ensuring the persistence and recovery of system data.
     *
     * Typical operations include:
     * - Exporting the current system state.
     * - Importing system data from an external file.
     *
     * Errors during export or import are encapsulated in sealed `ExportError` or
     * `ImportError` types, allowing structured handling of unexpected issues.
     */
    val backups: BackupManager

    /**
     * Represents the current operational state of the Gradeway lifecycle.
     *
     * The `state` variable reflects the lifecycle's active phase, which determines
     * what operations are permissible based on the underlying state of the system.
     * It can assume one of the following values from the [GradewayState] enumeration:
     *
     * - `LOADED`: Indicates that the Gradeway system is fully initialized and operational.
     *   Loading additional resources is not allowed, but unloading is permitted.
     *
     * - `UNLOADED`: Indicates that the Gradeway system is not currently active.
     *   Loading is allowed, but unloading is not applicable since the system is inactive.
     *
     * - `PROCESSING`: Indicates that the Gradeway system is in the midst of ongoing operations
     *   such as a transition or critical task. Neither loading nor unloading is permissible
     *   during this state to ensure stability.
     *
     * This variable plays a key role in governing the lifecycle control of the implementing class,
     * impacting permissible operations like a load, unload, and reload.
     */
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
