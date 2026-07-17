/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.group

import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.*

/**
 * Represents a group entity within the system.
 *
 * The `GroupEntity` interface defines the structure and behavior for managing group-level
 * attributes, roles, permissions, and metadata. It serves as a foundational contract for
 * modeling groups in the system with support for permission references and permission
 * template references. The interface is designed to integrate seamlessly with permission
 * management and role-based access control mechanisms.
 *
 * This entity provides the following key properties:
 * - `id`: A unique identifier for the group.
 * - `name`: The name of the group.
 * - `defaultWeight`: The default weight or priority of the group.
 * - `createdAt` and `updatedAt`: Timestamps indicating when the group was created and last updated.
 * - `roles`: A collection of roles associated with the group.
 */
interface GroupEntity : PermissionReference<GroupPermissionEntity>,
    PermissionTemplateReference<GroupPermissionTemplateEntity> {
    /**
     * A unique identifier for a `GroupEntity`.
     *
     * This property represents an immutable and unique `EntityID` backed by a `UUID`,
     * which serves as the primary key for identifying a specific group in the system.
     * It is used to establish relationships with other entities, such as permissions,
     * roles, metadata, or templates, ensuring a consistent and reliable reference to
     * the group across the system.
     */
    val id: EntityID<UUID>

    /**
     * Represents the name of the group entity.
     *
     * This property holds the human-readable, descriptive name of the group.
     * It is intended to uniquely identify or categorize a group within a system
     * from a user perspective. The name serves as an important attribute when
     * dealing with permissions, roles, or templates associated with the group.
     *
     * Constraints or requirements for this property may vary depending on
     * system-specific rules, such as length, character restrictions, or uniqueness.
     */
    val name: String

    /**
     * Represents the default weight value associated with a `GroupEntity`.
     *
     * This variable is used to define a default numeric weight or priority for a group,
     * impacting its significance or ordering in the system. The `defaultWeight` can be
     * used in scenarios such as sorting, ranking, or assigning precedence for groups.
     *
     * The value is mutable, allowing adjustments to the weight as needed for system or
     * user-specific requirements.
     */
    var defaultWeight: Int

    /**
     * The timestamp indicating when the group entity was created.
     *
     * This property represents the creation time of the current instance of `GroupEntity`.
     * It is stored as an `Instant` value and serves as an immutable record of when the
     * entity was initially saved or instantiated in the system. The `createdAt` property
     * is intended to provide a reliable point of reference for auditing, versioning, or
     * chronological tracking of entity lifecycle events.
     */
    val createdAt: Instant

    /**
     * The timestamp indicating the last time the group entity was updated.
     *
     * This property represents the last modification date and time for the `GroupEntity`. It is
     * used to track and manage changes to the entity over time, facilitating auditing and
     * synchronization processes. The value is immutable and updated automatically whenever
     * the `GroupEntity` undergoes any modifications.
     */
    val updatedAt: Instant

    /**
     * Represents the collection of roles associated with a [GroupEntity].
     *
     * The `roles` property provides a navigable and queryable iterable
     * of [RoleEntity] instances linked to the group. These roles define
     * the permissions, attributes, and other role-based functionalities
     * relevant to the specific group.
     *
     * This property facilitates access to the roles assigned to the group,
     * supporting operations such as querying, auditing, and updating the
     * roles within the context of the group entity.
     */
    val roles: SizedIterable<RoleEntity>
}
