/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.attribute.Attribute
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.time.Instant
import java.util.*

interface SharedAttributeEntity {
    val id: EntityID<UUID>

    val key: Key
    val type: Key
    var value: String

    val createdAt: Instant
    val updatedAt: Instant

    val attribute: Attribute<*>

    fun flush(): Boolean
    fun delete()
}
