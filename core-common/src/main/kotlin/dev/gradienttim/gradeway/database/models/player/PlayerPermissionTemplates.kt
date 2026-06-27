/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.entity.player.PlayerPermissionTemplateEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object PlayerPermissionTemplatesTable : CompositeIdTable(name = TableConstants.PLAYER_PERMISSION_TEMPLATES_TABLE_NAME) {
    val playerId = reference(
        name = "player_id",
        refColumn = PlayersTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val permissionTemplateId = reference(
        name = "permission_template_id",
        refColumn = PermissionTemplatesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
}

class DatabasePlayerPermissionTemplateEntity(id: EntityID<CompositeID>) : CompositeEntity(id),
    PlayerPermissionTemplateEntity {
    companion object : CompositeEntityClass<DatabasePlayerPermissionTemplateEntity>(PlayerPermissionTemplatesTable)

    override var playerId by PlayerPermissionTemplatesTable.playerId
    override var permissionTemplateId by PlayerPermissionTemplatesTable.permissionTemplateId

    override val player by DatabasePlayerEntity referencedOn PlayerPermissionTemplatesTable.playerId
    override val permissionTemplate by DatabasePermissionTemplateEntity referencedOn
            PlayerPermissionTemplatesTable.permissionTemplateId
}
