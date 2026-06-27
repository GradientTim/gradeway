/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PermissionService.CreateTemplateError
import dev.gradienttim.gradeway.services.PermissionService.DeleteTemplateError
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

interface TemplatePermissionService {
    fun createTemplate(name: String): Either<CreateTemplateError, PermissionTemplateEntity>

    fun deleteTemplate(id: UUID): Either<DeleteTemplateError, Unit>

    fun setTemplateName(
        id: UUID,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean>

    fun setTemplateName(
        entity: PermissionTemplateEntity,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean>

    fun setTemplateAssignedTo(
        id: UUID,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    fun setTemplateAssignedTo(
        entity: PermissionTemplateEntity,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    fun findTemplateById(id: UUID): PermissionTemplateEntity?

    fun findTemplateByName(name: String): PermissionTemplateEntity?

    fun findTemplateByIdOrName(value: String): PermissionTemplateEntity?

    fun listTemplates(
        where: (() -> Op<Boolean>)? = null,
        orderBy: Set<Pair<Expression<*>, SortOrder>> = emptySet(),
        limit: Int = 20
    ): SizedIterable<PermissionTemplateEntity>

    fun linkTemplateToRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    fun linkTemplateToRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    fun unlinkTemplateFromRole(templateId: UUID, roleId: UUID): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun applyTemplateToRole(templateId: UUID, roleId: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToRole(templateId: UUID, role: RoleEntity): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    fun revokeTemplateFromRole(templateId: UUID, roleId: UUID): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun linkTemplateToPlayer(templateId: UUID, playerId: UUID): Either<PermissionService.LinkTemplateError, Unit>

    fun linkTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit>

    fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    fun unlinkTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    fun applyTemplateToPlayer(templateId: UUID, playerId: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    fun revokeTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>
}
