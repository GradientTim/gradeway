/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import com.github.benmanes.caffeine.cache.Caffeine
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.CacheConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import java.util.concurrent.TimeUnit

class CommonCaches(val gradeway: CommonGradeway) : Caches {
    override val suggestions = CommonSuggestionIndex(gradeway)

    override val roles = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, RoleEntity> { id ->
            transaction(gradeway.database) {
                DatabaseRoleEntity.findById(id) as RoleEntity
            }
        }

    override val groups = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, GroupEntity> { id ->
            transaction(gradeway.database) {
                DatabaseGroupEntity.findById(id) as GroupEntity
            }
        }

    override val players = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, PlayerEntity> { id ->
            transaction(gradeway.database) {
                DatabasePlayerEntity.findById(id) as PlayerEntity
            }
        }

    override val permissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, PermissionEntity> { id ->
            transaction(gradeway.database) {
                DatabasePermissionEntity.findById(id) as PermissionEntity
            }
        }

    override val rolePermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<RolePermissionEntity>> { id ->
            gradeway.permissions.getRolePermissions(id)
        }

    override val groupPermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<GroupPermissionEntity>> { id ->
            gradeway.permissions.getGroupPermissions(id)
        }

    override val playerPermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<PlayerPermissionEntity>> { id ->
            gradeway.permissions.getPlayerPermissions(id)
        }

    override val roleEffectiveWeights = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_EFFECTIVE_WEIGHT_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_EFFECTIVE_WEIGHT_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Int> { id ->
            transaction(gradeway.database) {
                resolveEffectiveRoleWeight(roles.get(id))
            }
        }

    override val playerEffectiveWeights = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_EFFECTIVE_WEIGHT_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_EFFECTIVE_WEIGHT_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Int> { id ->
            transaction(gradeway.database) {
                resolveEffectivePlayerWeight(players.get(id))
            }
        }

    override val roleEffectivePermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<PermissionEntity>> { id ->
            transaction(gradeway.database) {
                resolveEffectiveRolePermissions(roles.get(id))
            }
        }

    override val groupEffectivePermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<PermissionEntity>> { id ->
            transaction(gradeway.database) {
                resolveEffectiveGroupPermissions(groups.get(id))
            }
        }

    override val playerEffectivePermissions = Caffeine.newBuilder()
        .maximumSize(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_MAX_SIZE)
        .expireAfterWrite(CacheConstants.ENTITY_EFFECTIVE_PERMISSIONS_WRITE_DURATION, TimeUnit.MINUTES)
        .build<UUID, Set<PermissionEntity>> { id ->
            transaction(gradeway.database) {
                resolveEffectivePlayerPermissions(players.get(id))
            }
        }

    private fun resolveEffectiveRoleWeight(role: RoleEntity): Int {
        if (role.weight != UNSET_WEIGHT) {
            return role.weight
        }

        val groupWeight = role.groups
            .mapNotNull { roleGroupEntity ->
                roleGroupEntity.group.defaultWeight.takeIf { it != UNSET_WEIGHT }
            }
            .maxOrNull()

        return groupWeight ?: DEFAULT_WEIGHT
    }

    private fun resolveEffectivePlayerWeight(player: PlayerEntity): Int {
        if (player.weight != UNSET_WEIGHT) {
            return player.weight
        }

        val activeRoleWeight = player.roles
            .filter { it.pausedAt == null && (it.untilAt == null || it.untilAt!! > gradeway.now()) }
            .maxOfOrNull { playerRoleEntity -> resolveEffectiveRoleWeight(playerRoleEntity.role) }

        return activeRoleWeight ?: DEFAULT_WEIGHT
    }

    private fun resolveEffectiveGroupPermissions(group: GroupEntity): Set<PermissionEntity> {
        val ownPermissions = group.permissions.filter { it.isEnabled }.map { it.permission }
        val templatePermissions = group.permissionTemplates.flatMap { groupPermissionTemplate ->
            groupPermissionTemplate.permissionTemplate.permissions.map { templatePermission ->
                templatePermission.permission
            }
        }
        return (ownPermissions + templatePermissions).toSet()
    }

    private fun resolveEffectiveRolePermissions(
        role: RoleEntity,
        visitedRoleIds: MutableSet<UUID> = mutableSetOf()
    ): Set<PermissionEntity> {
        if (!visitedRoleIds.add(role.id.value)) {
            return emptySet()
        }

        val ownPermissions = role.permissions.filter { it.isEnabled }.map { it.permission }
        val templatePermissions = role.permissionTemplates.flatMap { rolePermissionTemplate ->
            rolePermissionTemplate.permissionTemplate.permissions.map { templatePermission ->
                templatePermission.permission
            }
        }
        val groupPermissions = role.groups.flatMap { roleGroupEntity ->
            resolveEffectiveGroupPermissions(roleGroupEntity.group)
        }
        val parentPermissions = role.parents.flatMap { roleParentEntity ->
            resolveEffectiveRolePermissions(roleParentEntity.parent, visitedRoleIds)
        }

        return (ownPermissions + templatePermissions + groupPermissions + parentPermissions).toSet()
    }

    private fun resolveEffectivePlayerPermissions(player: PlayerEntity): Set<PermissionEntity> {
        val ownPermissions = player.permissions.filter { it.isEnabled }.map { it.permission }
        val templatePermissions = player.permissionTemplates.flatMap { playerPermissionTemplate ->
            playerPermissionTemplate.permissionTemplate.permissions.map { templatePermission ->
                templatePermission.permission
            }
        }

        val hasExpiredRoles = player.roles.any {
            it.pausedAt == null && it.untilAt != null && it.untilAt!! <= gradeway.now()
        }
        if (hasExpiredRoles) {
            scheduleExpiredRoleCleanup(player)
        }

        val activeRolePermissions = player.roles
            .filter { it.pausedAt == null && (it.untilAt == null || it.untilAt!! > gradeway.now()) }
            .flatMap { playerRoleEntity -> resolveEffectiveRolePermissions(playerRoleEntity.role) }

        return (ownPermissions + templatePermissions + activeRolePermissions).toSet()
    }

    /**
     * Fires off the actual removal of a player's expired roles on a background dispatcher instead of
     * inline, since this runs from inside permission resolution — one of the hottest, most
     * latency-sensitive paths in the plugin. (Called on nearly every player action.) Deleting a
     * database row synchronously there risks stalling the calling thread (e.g., the server main thread
     * on Bukkit); permission resolution itself already excludes expired roles live, so the physical
     * delete here is pure housekeeping and can safely happen a moment later.
     */
    private fun scheduleExpiredRoleCleanup(player: PlayerEntity) {
        gradeway.backgroundScope.launch {
            gradeway.players.removeExpiredRoles(player)
                .onLeft { error ->
                    gradeway.logger.error("Failed to remove expired roles for ${player.id.value}: $error")
                }
        }
    }

    companion object {
        const val UNSET_WEIGHT = -1
        const val DEFAULT_WEIGHT = 0
    }
}
