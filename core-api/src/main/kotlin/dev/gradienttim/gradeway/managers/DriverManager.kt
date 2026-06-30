/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.utilities.Loadable
import dev.gradienttim.gradeway.utilities.Unloadable

interface DriverManager : Loadable, Unloadable {
    /**
     * Finds and returns a driver matching the provided ID and type, if available.
     *
     * @param id The unique identifier of the driver to find.
     * @param type The type of the driver to find, such as DATABASE or MESSAGING.
     * @return The matching driver if found, or null if no matching driver exists.
     */
    fun findDriver(id: String, type: DriverType): Driver?
}
