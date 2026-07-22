/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import com.github.benmanes.caffeine.cache.LoadingCache
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import java.util.*

/**
 * Represents a collection of caches for various entity types and their associated permissions.
 * The `Caches` interface serves as a structure for caching entities and related permissions to improve
 * performance by reducing repeated database queries or expensive computations.
 */
interface Caches {
    val roles: LoadingCache<UUID, RoleEntity>
    val groups: LoadingCache<UUID, GroupEntity>
    val players: LoadingCache<UUID, PlayerEntity>
    val permissions: LoadingCache<UUID, PermissionEntity>

    val rolePermissions: LoadingCache<UUID, Set<RolePermissionEntity>>
    val groupPermissions: LoadingCache<UUID, Set<GroupPermissionEntity>>
    val playerPermissions: LoadingCache<UUID, Set<PlayerPermissionEntity>>

    val roleEffectiveWeights: LoadingCache<UUID, Int>
    val playerEffectiveWeights: LoadingCache<UUID, Int>

    val roleEffectivePermissions: LoadingCache<UUID, Set<PermissionEntity>>
    val groupEffectivePermissions: LoadingCache<UUID, Set<PermissionEntity>>
    val playerEffectivePermissions: LoadingCache<UUID, Set<PermissionEntity>>

    val suggestions: SuggestionIndex

    fun invalidateEntities() {
        roles.invalidateAll()
        groups.invalidateAll()
        players.invalidateAll()
        permissions.invalidateAll()
    }

    fun invalidateEntityPermissions() {
        rolePermissions.invalidateAll()
        groupPermissions.invalidateAll()
        playerPermissions.invalidateAll()
    }

    fun invalidateEntityEffectiveWeights() {
        roleEffectiveWeights.invalidateAll()
        playerEffectiveWeights.invalidateAll()
    }

    fun invalidateEntityEffectivePermissions() {
        roleEffectivePermissions.invalidateAll()
        groupEffectivePermissions.invalidateAll()
        playerEffectivePermissions.invalidateAll()
    }

    fun invalidateAll() {
        invalidateEntities()
        invalidateEntityPermissions()
        invalidateEntityEffectiveWeights()
        invalidateEntityEffectivePermissions()
        suggestions.clear()
    }

    /**
     * Invalidates all cache entries scoped to a single player, identified by their [UUID].
     * Intended to be called when a player disconnects from a server, so their cached entity,
     * permissions, effective weight, and effective permissions are evicted instead of lingering
     * until they expire on their own.
     *
     * @param playerId the unique identifier of the player whose cache entries should be evicted.
     */
    fun invalidatePlayer(playerId: UUID) {
        players.invalidate(playerId)
        playerPermissions.invalidate(playerId)
        playerEffectiveWeights.invalidate(playerId)
        playerEffectivePermissions.invalidate(playerId)
    }
}

/**
 * An eagerly populated id-to-name index per suggestible entity type, used to answer command
 * tab-completion instantly from memory instead of querying the database on every keystroke.
 *
 * Unlike [Caches]'s other members, this index is not a lazy, pull-on-miss cache: nothing here is
 * loaded on demand. It is fully populated by [initialize] and kept correct afterward purely by
 * push updates reacting to messaging payloads (both locally-originated and synced from other
 * servers), so every entity that exists is always present - the property suggestions actually
 * need, unlike a lazily populated cache that only contains whatever has incidentally been looked
 * up by id.
 */
interface SuggestionIndex {
    val players: Map<UUID, String>
    val roles: Map<UUID, String>
    val groups: Map<UUID, String>
    val permissions: Map<UUID, String>
    val permissionTemplates: Map<UUID, String>

    /**
     * Fully (re)populates every map from the database. Must only be called once the database
     * connection is established, and is also used to rebuild the index in response to a
     * [dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload], since bulk operations like
     * a LuckPerms migration or backup import don't describe which specific entities changed.
     */
    fun initialize()

    /**
     * Empties every map, e.g., as part of [Caches.invalidateAll] on plugin unload.
     */
    fun clear()
}
