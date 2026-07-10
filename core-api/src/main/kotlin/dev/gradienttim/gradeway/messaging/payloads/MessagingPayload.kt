/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.Serializable

/**
 * Represents a single real-time synchronization event exchanged between servers over the
 * configured [dev.gradienttim.gradeway.messaging.MessagingBroker].
 *
 * A `MessagingPayload` intentionally carries only the identifiers of the entities involved
 * and the kind of change that occurred, rather than the entity's full state. Receiving servers
 * are expected to treat a payload as an invalidation signal and re-read the current state of
 * the referenced entity from the database on next access, rather than applying the payload's
 * fields directly. This keeps messages small, keeps them independent of the database schema
 * and the backup DTO shapes, and avoids servers ever drifting out of sync by applying a stale
 * partial update.
 *
 * All direct implementations of this sealed interface are serialized and deserialized together
 * as a single polymorphic hierarchy, so new payload kinds can be introduced without changing how
 * they are published, subscribed to, or dispatched. Field numbers for Protocol Buffers encoding
 * fall back to declaration order since this module does not depend on the protobuf serialization
 * artifact; this is an accepted tradeoff given all servers in a deployment run the same Gradeway
 * version at a steady state.
 */
@Serializable
sealed interface MessagingPayload

/**
 * Describes the kind of mutation that occurred to the entity or relationship referenced by a
 * [MessagingPayload].
 *
 * Not every payload uses every action; for example, a payload describing a link between two
 * entities (such as a role being added to a group) typically only uses [CREATED] and [DELETED],
 * while a payload describing a mutable property (such as a permission's value) typically only
 * uses [UPDATED].
 */
@Serializable
enum class MessagingAction {
    /**
     * The referenced entity or relationship was newly created.
     */
    CREATED,

    /**
     * The referenced entity or relationship had one or more of its properties changed.
     */
    UPDATED,

    /**
     * The referenced entity or relationship was removed.
     */
    DELETED
}
