/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.createEntityPermissionHandler
import dev.gradienttim.gradeway.commands.extensions.createGlobalListHandler
import dev.gradienttim.gradeway.commands.extensions.suggestGroups
import dev.gradienttim.gradeway.commands.extensions.suggestRoles
import dev.gradienttim.gradeway.database.models.group.GroupPermissionsTable
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.GroupService
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

internal fun <TSource> ArgumentBuilder<TSource, *>.groupBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    fun handleCreateGroup(audience: Audience, name: String, defaultWeight: Int = -1) {
        gradeway.groups.create(name) {
            this.defaultWeight = defaultWeight
        }.onLeft { error ->
            if (error is GroupService.CreateGroupError.InvalidName) {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.group.create.invalidName",
                        Component.text(name)
                    )
                )
                return
            }
            if (error is GroupService.CreateGroupError.Unexpected) {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.group.create.unexpectedError",
                        Component.text(name),
                        Component.text(error.throwable.message ?: "Unknown")
                    )
                )
                return
            }
        }.onRight {
            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.group.create.success",
                    Component.text(name),
                    Component.text(defaultWeight)
                )
            )
        }
    }

    literal("group") {
        requires { hasPermission(it, "gradeway.group") }

        literal("create") {
            requires { hasPermission(it, "gradeway.group.create") }

            string("name") {
                integer("defaultWeight") {
                    execute {
                        val audience = sourceToAudience(source)

                        val name = stringParam("name")
                        val defaultWeight = intParam("defaultWeight")

                        handleCreateGroup(audience, name, defaultWeight)
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val name = stringParam("name")

                    handleCreateGroup(audience, name)
                }
            }
        }

        literal("delete") {
            requires { hasPermission(it, "gradeway.group.delete") }

            string("idOrName") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestGroups(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")

                    gradeway.groups.delete(idOrName)
                        .onLeft { error ->
                            if (error is GroupService.DeleteGroupError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.delete.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.DeleteGroupError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.delete.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.delete.success",
                                    Component.text(idOrName)
                                )
                            )
                        }
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestGroups(gradeway, remaining.lowercase())
                    }
                }

                literal("setName") {
                    requires { hasPermission(it, "gradeway.group.setName") }

                    string("name") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val name = stringParam("name")

                            gradeway.groups.setName(idOrName, name)
                                .onLeft { error ->
                                    if (error is GroupService.SetNameError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setName.entityNotFound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.InvalidName) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setName.invalidName",
                                                Component.text(idOrName),
                                                Component.text(name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.NameAlreadySet) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setName.nameAlreadySet",
                                                Component.text(idOrName),
                                                Component.text(name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setName.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setName.success",
                                            Component.text(idOrName),
                                            Component.text(name)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setDefaultWeight") {
                    requires { hasPermission(it, "gradeway.group.setDefaltWeight") }

                    integer("defaultWeight") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val defaultWeight = intParam("defaultWeight")

                            gradeway.groups.setDefaultWeight(idOrName, defaultWeight)
                                .onLeft { error ->
                                    if (error is GroupService.SetDefaultWeightError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.entityNotFound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetDefaultWeightError.WeightAlreadySet) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.weightAlreadySet",
                                                Component.text(idOrName),
                                                Component.text(defaultWeight)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetDefaultWeightError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setDefaultWeight.success",
                                            Component.text(idOrName),
                                            Component.text(defaultWeight)
                                        )
                                    )
                                }
                        }
                    }
                }

                groupRolesBuilder(gradeway, hasPermission, sourceToAudience)
                groupPermissionsBuilder(gradeway, hasPermission, sourceToAudience)
            }
        }

        createGlobalListHandler(
            gradeway = gradeway,
            permission = "gradeway.group.list",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { page, limit ->
                GroupsTable
                    .select(GroupsTable.id, GroupsTable.name)
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[GroupsTable.id].value
                            val name = row[GroupsTable.name]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(Component.translatable("gradeway.command.group.list.empty"))
                    return@createGlobalListHandler
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.group.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { group ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.group.list.entry",
                            Component.text(group.id.toString()),
                            Component.text(group.name)
                        )
                    )
                }
            }
        )
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.groupRolesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("roles") {
        requires { hasPermission(it, "gradeway.group.roles") }

        literal("add") {
            requires { hasPermission(it, "gradeway.group.roles.add") }

            string("roleId") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.group.addRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.groups.addRoleToGroup(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is GroupService.AddTargetError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.addRole.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.addRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.AlreadyInGroup) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.addRole.alreadyInGroup",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.addRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.addRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.group.roles.remove") }

            string("roleId") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.group.removeRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.groups.removeRoleFromGroup(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is GroupService.RemoveTargetError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.NotInGroup) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.notInGroup",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.removeRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.groupPermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    createEntityPermissionHandler(
        gradeway = gradeway,
        entityType = "group",
        hasPermission = hasPermission,
        sourceToAudience = sourceToAudience,
        handleSetPermission = { idOrName, permission, status ->
            gradeway.groups.setPermission(
                idOrName,
                permission,
                status
            )
        },
        handleUnsetPermission = { idOrName, permission -> gradeway.groups.unsetPermission(idOrName, permission) },
        handleClearPermissions = { idOrName -> gradeway.groups.clearPermissions(idOrName) },
        handleLinkTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.linkTemplateToGroup(templateIdOrName, idOrName)
        },
        handleUnlinkTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.unlinkTemplateFromGroup(templateIdOrName, idOrName)
        },
        handleApplyTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.applyTemplateToGroup(templateIdOrName, idOrName)
        },
        handleRevokeTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.revokeTemplateFromGroup(templateIdOrName, idOrName)
        },
        handleListQuery = { scope, page, limit ->
            GroupPermissionsTable
                .innerJoin(GroupsTable, { groupId }, { id })
                .innerJoin(PermissionsTable, { GroupPermissionsTable.permissionId }, { id })
                .select(PermissionsTable.value, PermissionsTable.type, GroupPermissionsTable.isEnabled)
                .where {
                    (GroupsTable.id likeAsStr "$scope%") or
                            (GroupsTable.name.lowerCase() like "${scope.toString().lowercase()}%")
                }
                .limit(limit)
                .offset((page - 1).toLong())
                .map { row ->
                    object {
                        val value = row[PermissionsTable.value]
                        val type = row[PermissionsTable.type]
                        val isEnabled = row[GroupPermissionsTable.isEnabled]
                    }
                }
        },
        handleListRender = { audience, page, limit, result ->
            if (result.isEmpty()) {
                audience.sendMessage(
                    Component.translatable("gradeway.command.group.listPermissions.empty")
                )
                return@createEntityPermissionHandler
            }

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.group.listPermissions.header",
                    Component.text(page),
                    Component.text(limit)
                )
            )

            result.forEach { permissionEntity ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.group.listPermissions.entry",
                        Component.text(permissionEntity.value),
                        Component.text(permissionEntity.type.name),
                        Component.text(permissionEntity.isEnabled)
                    )
                )
            }
        }
    )
}
