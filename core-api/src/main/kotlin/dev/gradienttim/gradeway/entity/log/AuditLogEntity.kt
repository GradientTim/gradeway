/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.log

import java.time.Instant
import java.util.UUID

/**
 * Represents a log entry that captures audit information about actions performed in the system.
 *
 * This interface provides the structure for recording details about an actor performing an action
 * on a specific target, typically to maintain a history of changes or events for monitoring,
 * debugging, or compliance purposes.
 */
interface AuditLogEntity {
    /**
     * Describes the type of actor responsible for performing the action recorded in the audit log.
     *
     * The actor type indicates the category or classification of the entity initiating the action
     * (e.g., "player", "group", "role"). This helps identify the origin of the action for
     * auditing and tracing purposes.
     */
    val actorType: String

    /**
     * Represents the unique identifier of the actor responsible for the logged action.
     *
     * This property is used to specify the exact entity performing the action, such as a user ID,
     * system process ID, or service name, depending on the type of actor recorded. The identifier
     * aids in tracing and auditing the entity initiating the action.
     *
     * The value may be null if the actor does not have a specific identifier, or it is unavailable.
     */
    val actorIdentifier: String?

    /**
     * Describes the type of target associated with the action recorded in the audit log.
     *
     * The target type identifies the category or classification of the entity on which the action is
     * performed (e.g., "item", "resource", "document"). This property helps specify the nature of the
     * entity being affected or influenced by the recorded action for auditing and tracking purposes.
     */
    val targetType: String

    /**
     * Represents the unique identifier of the target entity associated with an action in the audit log.
     *
     * This property is used to specify the exact entity on which the action was performed, such as a resource,
     * document, or item. It provides a direct reference to the affected entity, aiding in tracking and
     * auditing interactions within the system.
     */
    val targetId: UUID

    /**
     * Represents the secondary identifier associated with the target entity in the audit log.
     * This identifier is optional and can be used to provide additional specificity or context
     * about the target being logged. Typically paired with `targetId` to differentiate entities
     * or subcomponents within the same target type.
     */
    val targetSecondaryId: UUID?

    /**
     * Represents the point in time when the audited action occurred.
     * It is stored as an instance of `Instant` to ensure precision and enable
     * easy manipulation and comparison of timestamps.
     */
    val timestamp: Instant

    /**
     * Represents the action performed in the context of an audit log.
     * This describes the specific activity captured in the log entry,
     * providing details about what operation or event occurred.
     */
    val action: String

    /**
     * Represents the state of the target entity before the action was performed.
     * This can be null if no prior state information is available or applicable.
     */
    val before: String?

    /**
     * Represents the state of the entity after the operation occurred.
     * This may contain a serialized snapshot or description of the entity's
     * state, and it can be null if the operation does not result in an
     * applicable state change or information is not available.
     */
    val after: String?
}
