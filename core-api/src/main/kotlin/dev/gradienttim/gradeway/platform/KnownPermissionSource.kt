/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

/**
 * Provides the permission nodes a platform knows about statically - without anyone having
 * checked them yet - such as permissions declared in plugin descriptors or set on registered
 * commands.
 *
 * Gradeway seeds [dev.gradienttim.gradeway.managers.KnownPermissionsManager] from this source when
 * it is enabled. Nodes that are only ever checked in code are picked up separately at runtime,
 * when the platform's permission hook records them.
 */
fun interface KnownPermissionSource {
    /**
     * Collects every permission node currently known to the platform.
     */
    fun collect(): Collection<String>

    companion object {
        /**
         * A source for platforms that have no static permission metadata (e.g. Velocity).
         */
        val NONE = KnownPermissionSource { emptyList() }
    }
}
