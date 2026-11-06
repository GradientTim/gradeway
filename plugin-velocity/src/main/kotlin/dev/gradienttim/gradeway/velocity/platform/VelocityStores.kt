/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.platform

import dev.gradienttim.gradeway.platform.Stores
import dev.gradienttim.gradeway.platform.stores.MemoryUuidBooleanStore
import dev.gradienttim.gradeway.utilities.Store
import java.util.*

class VelocityStores : Stores {
    override val notification: Store<UUID, Boolean> = MemoryUuidBooleanStore()
}
