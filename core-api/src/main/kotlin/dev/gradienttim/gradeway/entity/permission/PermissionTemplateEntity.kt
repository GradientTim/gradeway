/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import arrow.core.Either
import dev.gradienttim.gradeway.services.PermissionService
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.time.Instant
import java.util.UUID

interface PermissionTemplateEntity {
    val id: EntityID<UUID>
    val name: String
    val assignedTo: AssignedTo

    val createdAt: Instant
    val updatedAt: Instant

    val permissions: SizedIterable<PermissionTemplatePermissionEntity>

    fun setName(name: String): Either<PermissionService.SetNameTemplateError, Boolean>
    fun setAssignedTo(assignedTo: AssignedTo): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    enum class AssignedTo {
        ALL,
        ROLE,
        PLAYER,
        ;

        val allowForRole: Boolean
            get() = this == ALL || this == ROLE

        val allowForPlayer: Boolean
            get() = this == ALL || this == PLAYER
    }
}
