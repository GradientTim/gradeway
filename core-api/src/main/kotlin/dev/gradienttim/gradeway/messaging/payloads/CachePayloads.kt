/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging.payloads

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals that every effective-permission and effective-weight cache should be dropped in full,
 * without identifying which entities changed.
 *
 * Bulk operations such as a LuckPerms migration or a backup import wipe and repopulate entire
 * tables directly through the DAO/DSL layer rather than through the individual role/player/
 * permission services, both for performance and because the imported ids don't correspond to
 * anything a receiving cache could already hold. That bypasses the narrowly targeted payloads
 * (like [RoleChangedPayload] or [RolePermissionChangedPayload]) those services normally publish
 * per mutation, and raw batch inserts into relationship/junction tables don't go through the DAO
 * layer at all, so the entity-hook-based whole-entity sync that backs those payloads never
 * observes them either. Publishing a single `CacheFlushPayload` once the bulk write commits is
 * the only way for every server (including this one) to learn that its caches are stale.
 */
@Serializable
@SerialName("cache_flush")
object CacheFlushPayload : MessagingPayload
