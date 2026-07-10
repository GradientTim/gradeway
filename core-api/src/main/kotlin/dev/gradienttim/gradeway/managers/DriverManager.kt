/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.utilities.lifecycle.Loadable
import dev.gradienttim.gradeway.utilities.lifecycle.Unloadable

interface DriverManager : Loadable, Unloadable {
    /**
     * Finds and returns a driver matching the provided ID and type, if available.
     *
     * @param id The unique identifier of the driver to find.
     * @param type The type of the driver to find, such as DATABASE or MESSAGING.
     * @return The matching driver if found, or null if no matching driver exists.
     */
    fun findDriver(id: String, type: DriverType): Driver?

    /**
     * Registers a driver instance that was constructed in-code rather than loaded from an
     * external jar, making it discoverable through [findDriver] like any other driver.
     *
     * Intended for drivers that need direct access to a platform-specific object (such as a
     * Bukkit `JavaPlugin` or a Velocity `ProxyServer`) that generic, environment-variable-based
     * driver jars have no way to obtain.
     *
     * @param id The unique identifier to register the driver under.
     * @param type The type of the driver, such as DATABASE or MESSAGING.
     * @param driver The driver instance to register.
     * @return `true` if the driver was registered, or `false` if a driver with the same ID and
     *         type is already registered.
     */
    fun registerDriver(id: String, type: DriverType, driver: Driver): Boolean
}
