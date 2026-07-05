/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.group

import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents a group-specific permission entity within the system.
 *
 * `GroupPermissionEntity` provides an interface for defining permissions tied to a specific group.
 * It extends the `SharedPermissionEntity` interface, inheriting common permission fields, and adds
 * group-specific associations and properties. This interface serves as a foundational contract
 * for managing permissions in a group-centric manner.
 */
interface GroupPermissionEntity: SharedPermissionEntity {
    /**
     * The unique identifier for the group entity.
     *
     * This property represents a UUID-backed entity ID that uniquely identifies a group
     * within the system. It is used to establish relationships with other entities,
     * such as permissions or attributes, and guarantees uniqueness across all instances.
     */
    val groupId: EntityID<UUID>

    /**
     * Represents the associated `GroupEntity` for the current entity.
     *
     * This property provides a link to a `GroupEntity` instance and is used to
     * establish a relationship between the implementing entity and a specific group.
     * The `GroupEntity` serves as a central concept encompassing attributes, permissions,
     * and other group-related metadata within the system.
     */
    val group: GroupEntity
}
