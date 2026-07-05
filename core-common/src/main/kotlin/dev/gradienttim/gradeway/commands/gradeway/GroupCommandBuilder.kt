/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.createGlobalListHandler
import dev.gradienttim.gradeway.commands.extensions.suggestGroups
import dev.gradienttim.gradeway.commands.extensions.suggestRoles
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.services.GroupService
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
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
    literal("permissions") {
        requires { hasPermission(it, "gradeway.group.permissions") }

        literal("set") {
            requires { hasPermission(it, "gradeway.group.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        gradeway.groups.setPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setPermission.alreadyEnabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setPermission.alreadyDisabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.group.setPermission.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(permission),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        ),
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setPermission.success",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                            }
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.groups.setPermission(idOrName, permission, true)
                        .onLeft { error ->
                            if (error is SetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setPermission.alreadyEnabled",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    ),
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.setPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("unset") {
            requires { hasPermission(it, "gradeway.group.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.groups.unsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.unsetPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    ),
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.unsetPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { hasPermission(it, "gradeway.group.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.groups.clearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.clearPermission.entityNotFound",
                                    Component.text(idOrName),
                                ),
                            )
                            return@execute
                        }
                        if (error is ClearPermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.clearPermissions.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(error.throwable.message ?: "Unknown")
                                ),
                            )
                            return@execute
                        }
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.group.clearPermissions.success",
                                Component.text(idOrName),
                            ),
                        )
                    }
            }
        }

//        literal("list") {
//            requires { hasPermission(it, "gradeway.group.permissions.list") }
//
//            execute {
//                val audience = sourceToAudience(source)
//
//                val idOrName = stringParam("idOrName")
//
//                val entity = gradeway.groups.findByIdOrName(idOrName)
//                if (entity == null) {
//                    audience.sendMessage(
//                        Component.translatable(
//                            "gradeway.command.group.notFound",
//                            Component.text(idOrName)
//                        )
//                    )
//                    return@execute
//                }
//            }
//        }
    }
}
