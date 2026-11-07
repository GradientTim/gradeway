/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.audit

import dev.gradienttim.gradeway.constants.TargetConstants
import dev.gradienttim.gradeway.utilities.Typed
import java.util.*

data class Target(
    override val type: String,
    val id: UUID,
    val secondaryId: UUID? = null,
) : Typed {
    companion object {
        fun group(
            groupId: UUID,
        ) = Target(TargetConstants.GROUP, groupId)

        fun groupPermission(
            groupId: UUID,
            permissionId: UUID,
        ) = Target(TargetConstants.GROUP_PERMISSION, groupId, permissionId)

        fun groupPermissionTemplate(
            groupId: UUID,
            templateId: UUID,
        ) = Target(TargetConstants.GROUP_PERMISSION_TEMPLATE, groupId, templateId)

        fun permission(
            permissionId: UUID,
        ) = Target(TargetConstants.PERMISSION, permissionId)

        fun permissionTemplate(
            templateId: UUID,
        ) = Target(TargetConstants.PERMISSION_TEMPLATE, templateId)

        fun permissionTemplatePermission(
            templateId: UUID,
            permissionId: UUID,
        ) = Target(TargetConstants.PERMISSION_TEMPLATE_PERMISSION, templateId, permissionId)

        fun player(
            playerId: UUID,
        ) = Target(TargetConstants.PLAYER, playerId)

        fun playerRole(
            playerId: UUID,
            roleId: UUID,
        ) = Target(TargetConstants.PLAYER_ROLE, playerId, roleId)

        fun playerAttribute(
            attributeId: UUID,
        ) = Target(TargetConstants.PLAYER_ATTRIBUTE, attributeId)

        fun playerPermission(
            playerId: UUID,
            permissionId: UUID,
        ) = Target(TargetConstants.PLAYER_PERMISSION, playerId, permissionId)

        fun playerPermissionTemplate(
            playerId: UUID,
            templateId: UUID,
        ) = Target(TargetConstants.PLAYER_PERMISSION_TEMPLATE, playerId, templateId)

        fun role(
            roleId: UUID,
        ) = Target(TargetConstants.ROLE, roleId)

        fun roleGroup(
            roleId: UUID,
            groupId: UUID,
        ) = Target(TargetConstants.ROLE_GROUP, roleId, groupId)

        fun roleParent(
            childId: UUID,
            parentId: UUID,
        ) = Target(TargetConstants.ROLE_PARENT, childId, parentId)

        fun roleAttribute(
            attributeId: UUID,
        ) = Target(TargetConstants.ROLE_ATTRIBUTE, attributeId)

        fun rolePermission(
            roleId: UUID,
            permissionId: UUID,
        ) = Target(TargetConstants.ROLE_PERMISSION, roleId, permissionId)

        fun rolePermissionTemplate(
            roleId: UUID,
            templateId: UUID,
        ) = Target(TargetConstants.ROLE_PERMISSION_TEMPLATE, roleId, templateId)

        fun track(
            trackId: UUID,
        ) = Target(TargetConstants.TRACK, trackId)

        fun trackStage(
            stageId: UUID,
        ) = Target(TargetConstants.TRACK_STAGE, stageId)
    }
}
