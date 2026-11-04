/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform.stores

import arrow.core.Either
import arrow.core.raise.context.either
import dev.gradienttim.gradeway.utilities.Store
import java.util.*

class MemoryUuidBooleanStore : Store<UUID, Boolean> {
    private val store = mutableMapOf<UUID, Boolean>()

    override fun set(
        key: UUID,
        value: Boolean,
    ): Either<Store.SetError, Boolean> = either {
        store[key] = value
        true
    }

    override fun unset(
        key: UUID,
    ): Either<Store.UnsetError, Boolean> = either {
        store.remove(key) != null
    }

    override fun get(
        key: UUID,
    ): Boolean? = store[key]

    override fun clear(): Either<Store.ClearError, Unit> = either {
        store.clear()
    }
}
