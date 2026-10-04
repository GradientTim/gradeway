/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.track

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.*

/**
 * Represents an organized progression of roles within the system, defined as a sequence of stages.
 *
 * A `TrackEntity` serves as the root of a directed acyclic graph (DAG) where each node corresponds
 * to a stage, represented by a `TrackStageEntity`. Tracks establish a hierarchical or sequential
 * structure for roles, allowing for the modeling of promotion or demotion paths.
 *
 * Tracks are identified by unique slugs and include metadata such as creation and update timestamps.
 * They also provide access to the collection of associated track stages, enabling navigation through
 * the stages within the track.
 */
interface TrackEntity {
    /**
     * A unique identifier for a `TrackEntity`.
     *
     * This property represents an immutable and unique `EntityID` backed by a `UUID`.
     * It serves as the primary key for identifying a specific track within the system.
     * The `id` is essential for establishing relationships between entities and provides
     * a globally unique reference to a `TrackEntity`.
     */
    val id: EntityID<UUID>

    /**
     * Represents a unique, human-readable identifier for a `TrackEntity`.
     *
     * The `slug` serves as a concise, URL-friendly string that uniquely identifies a track
     * within the system. It is primarily used for organizing and referencing tracks in
     * contexts such as APIs, user interfaces, or external systems. The slug can be changed
     * through `TrackService.setSlug`, so long-lived references should use the track's `id`
     * instead. It is intended to provide a clear and descriptive representation of the
     * track's purpose or content.
     */
    val slug: String

    /**
     * Represents the timestamp when this `TrackEntity` was created.
     *
     * This property stores the exact point in time, expressed as an `Instant`,
     * when the track entity was initially persisted or introduced into the system.
     * It is immutable and serves as a historical reference for the track's creation,
     * useful for auditing, sorting, or querying track entities based on their creation time.
     */
    val createdAt: Instant

    /**
     * The timestamp indicating the last time this `TrackEntity` was updated.
     *
     * This property is automatically managed and updated whenever changes are made
     * to the corresponding entity. It serves as a temporal marker for tracking
     * modifications and is particularly useful for auditing or synchronizing updates.
     */
    val updatedAt: Instant

    /**
     * Represents a collection of [TrackStageEntity] instances associated with a [TrackEntity].
     *
     * This property encapsulates all the stages that are part of a specific track, providing
     * an iterable structure to traverse or manipulate the stages within the track.
     *
     * Each `TrackStageEntity` within this iterable corresponds to a defined position in
     * the track's sequence and is linked to a specific [dev.gradienttim.gradeway.entity.role.RoleEntity] and its associated
     * hierarchy on the track. The stages are typically ordered by their `position` property
     * to enable progression or navigation through the track.
     */
    val stages: SizedIterable<TrackStageEntity>
}
