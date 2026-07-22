/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.messaging.payloads.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Default [SuggestionIndex] implementation. Backed by plain [ConcurrentHashMap]s rather than
 * Caffeine caches: these maps are 1:1 push-updated mirrors of real table rows (see [initialize]
 * and [handle]), so they're already self-bounded by the actual number of entities that exist -
 * an eviction policy wouldn't protect against anything real here, and if one ever triggered it
 * would silently drop entities from suggestions.
 */
class CommonSuggestionIndex(private val gradeway: CommonGradeway) : SuggestionIndex {
    override val players = ConcurrentHashMap<UUID, String>()
    override val roles = ConcurrentHashMap<UUID, String>()
    override val groups = ConcurrentHashMap<UUID, String>()
    override val permissions = ConcurrentHashMap<UUID, String>()
    override val permissionTemplates = ConcurrentHashMap<UUID, String>()

    init {
        gradeway.messaging.subscribe { payload -> handle(payload) }
    }

    override fun initialize() {
        transaction(gradeway.database) {
            val playerRows = PlayersTable.select(PlayersTable.id, PlayersTable.name)
                .associate { row -> row[PlayersTable.id].value to row[PlayersTable.name] }
            val roleRows = RolesTable.select(RolesTable.id, RolesTable.name)
                .associate { row -> row[RolesTable.id].value to row[RolesTable.name] }
            val groupRows = GroupsTable.select(GroupsTable.id, GroupsTable.name)
                .associate { row -> row[GroupsTable.id].value to row[GroupsTable.name] }
            val permissionRows = PermissionsTable.select(PermissionsTable.id, PermissionsTable.value)
                .associate { row -> row[PermissionsTable.id].value to row[PermissionsTable.value] }
            val permissionTemplateRows = PermissionTemplatesTable
                .select(PermissionTemplatesTable.id, PermissionTemplatesTable.name)
                .associate { row -> row[PermissionTemplatesTable.id].value to row[PermissionTemplatesTable.name] }

            players.clear()
            players.putAll(playerRows)
            roles.clear()
            roles.putAll(roleRows)
            groups.clear()
            groups.putAll(groupRows)
            permissions.clear()
            permissions.putAll(permissionRows)
            permissionTemplates.clear()
            permissionTemplates.putAll(permissionTemplateRows)
        }
    }

    override fun clear() {
        players.clear()
        roles.clear()
        groups.clear()
        permissions.clear()
        permissionTemplates.clear()
    }

    private fun handle(payload: MessagingPayload) {
        when (payload) {
            is PlayerChangedPayload -> update(players, payload.playerId, payload.action) {
                DatabasePlayerEntity.findById(it)?.name
            }

            is RoleChangedPayload -> update(roles, payload.roleId, payload.action) {
                DatabaseRoleEntity.findById(it)?.name
            }

            is GroupChangedPayload -> update(groups, payload.groupId, payload.action) {
                DatabaseGroupEntity.findById(it)?.name
            }

            is PermissionChangedPayload -> update(permissions, payload.permissionId, payload.action) {
                DatabasePermissionEntity.findById(it)?.value
            }

            is PermissionTemplateChangedPayload -> update(
                permissionTemplates,
                payload.templateId,
                payload.action
            ) { DatabasePermissionTemplateEntity.findById(it)?.name }

            is CacheFlushPayload -> initialize()
            else -> Unit
        }
    }

    /**
     * Applies a single-entity change to [map]: removes [rawId] on [MessagingAction.DELETED]
     * without a query, otherwise re-fetches the current name/value via [lookup] and puts it -
     * treating a null lookup (the entity was already gone by the time this ran) as a removal too.
     */
    private fun update(
        map: ConcurrentHashMap<UUID, String>,
        rawId: String,
        action: MessagingAction,
        lookup: (UUID) -> String?
    ) {
        val id = runCatching { UUID.fromString(rawId) }.getOrNull() ?: return

        if (action == MessagingAction.DELETED) {
            map.remove(id)
            return
        }

        val value = transaction(gradeway.database) { lookup(id) }
        if (value == null) {
            map.remove(id)
        } else {
            map[id] = value
        }
    }
}
