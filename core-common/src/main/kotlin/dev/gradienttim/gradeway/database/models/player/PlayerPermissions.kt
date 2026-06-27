/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object PlayerPermissionsTable : CompositeIdTable(name = TableConstants.PLAYER_PERMISSIONS_TABLE_NAME) {
    val playerId = reference(
        name = "player_id",
        refColumn = PlayersTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionId = reference(
        name = "permission_id",
        refColumn = PermissionsTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val isEnabled = bool("is_enabled").default(true)
}

class DatabasePlayerPermissionEntity(id: EntityID<CompositeID>) : CompositeEntity(id), PlayerPermissionEntity {
    companion object : CompositeEntityClass<DatabasePlayerPermissionEntity>(PlayerPermissionsTable)

    override var playerId by PlayerPermissionsTable.playerId
    override var permissionId by PlayerPermissionsTable.permissionId

    override var isEnabled by PlayerPermissionsTable.isEnabled

    override val player by DatabasePlayerEntity referencedOn PlayerPermissionsTable.playerId
    override val permission by DatabasePermissionEntity referencedOn PlayerPermissionsTable.permissionId

    override fun flush() = flush(null)
}
