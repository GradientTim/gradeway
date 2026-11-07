/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import java.util.concurrent.ConcurrentHashMap

class CommonKnownPermissionManager : KnownPermissionsManager {
    override val registry: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun unload(): Either<Throwable, Unit> = either {
        registry.clear()
    }

    override fun seed(permissions: Collection<String>) {
        permissions.forEach(::record)
    }

    override fun record(permission: String) {
        registry.add(permission)
    }
}
