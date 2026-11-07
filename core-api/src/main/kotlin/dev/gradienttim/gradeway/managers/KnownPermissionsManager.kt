/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.utilities.lifecycle.Unloadable

interface KnownPermissionsManager : Unloadable {
    val registry: Set<String>

    fun seed(permissions: Collection<String>)
    fun record(permission: String)
}
