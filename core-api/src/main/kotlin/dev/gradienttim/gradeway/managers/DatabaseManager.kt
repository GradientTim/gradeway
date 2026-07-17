/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.utilities.lifecycle.Disableable
import dev.gradienttim.gradeway.utilities.lifecycle.Enableable

/**
 * Interface for managing database interactions and operations.
 *
 * The `DatabaseManager` interface extends both `Enableable` and `Disableable`, indicating that
 * implementations of this interface can be enabled or disabled with a lifecycle management mechanism.
 * This interface is intended to encapsulate the logic and functionalities required for managing
 * interactions with a database system.
 *
 * Common responsibilities of a `DatabaseManager` implementation may include establishing and managing
 * database connections, executing queries, handling transactions, performing migrations, and ensuring
 * the stability and reliability of database operations.
 *
 * By adopting the `Enableable` and `Disableable` traits, the `DatabaseManager` provides a controlled
 * lifecycle for database operations, allowing implementations to transition between active and inactive
 * states as required.
 */
interface DatabaseManager : Enableable, Disableable
