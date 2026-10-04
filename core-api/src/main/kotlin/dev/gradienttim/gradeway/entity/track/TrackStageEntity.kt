/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.track

import dev.gradienttim.gradeway.entity.role.RoleEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents a single position on a [TrackEntity], wrapping an existing
 * [RoleEntity] rather than duplicating role data.
 */
interface TrackStageEntity {
    /**
     * A unique identifier for a `TrackStageEntity`.
     *
     * This property represents an immutable and unique `EntityID` backed by a `UUID`,
     * which serves as the primary key for identifying a specific track stage in the system.
     */
    val id: EntityID<UUID>

    /**
     * Represents the unique identifier of the track this stage belongs to.
     *
     * This UUID-based identifier establishes a relationship between the `TrackStageEntity`
     * and its owning `TrackEntity`. The identifier corresponds to the primary key of the
     * `TrackEntity` within the database.
     */
    val trackId: EntityID<UUID>

    /**
     * Represents the unique identifier of the role this stage wraps.
     *
     * This UUID-based identifier establishes a relationship between the `TrackStageEntity`
     * and the `RoleEntity` it represents a position for. The identifier corresponds to the
     * primary key of the `RoleEntity` within the database.
     */
    val roleId: EntityID<UUID>

    /**
     * Represents the position of a stage within a [TrackEntity], indicating its rank or
     * order along the track.
     *
     * This property defines the numerical assignment of a [TrackStageEntity]'s placement
     * in the sequence of stages. It is primarily used to determine the linear or hierarchical
     * flow of roles within the track and serves as the basis for navigation or progression
     * between stages.
     */
    val position: Int

    /**
     * Represents the associated [TrackEntity] instance for this stage.
     *
     * This property defines the specific track to which a [TrackStageEntity] belongs.
     * A `TrackEntity` is the root of a directed acyclic graph (DAG) that organizes roles into
     * branching promotion/demotion paths. Each stage in this graph, including this one, is
     * represented by a [TrackStageEntity].
     */
    val track: TrackEntity

    /**
     * Represents the associated `RoleEntity` for the `TrackStageEntity`.
     *
     * This property establishes the relationship between a track stage and its corresponding role,
     * allowing for the assignment of role-specific attributes, permissions, and behaviors within
     * the context of a track stage. The `role` property provides access to the full functionality
     * offered by `RoleEntity`, including its name, weight, attributes, permissions, and hierarchical
     * relationships.
     *
     * The associated role can be used to define role-based access control, organizational structures,
     * or other role-specific features linked to the track stage instance.
     */
    val role: RoleEntity
}
