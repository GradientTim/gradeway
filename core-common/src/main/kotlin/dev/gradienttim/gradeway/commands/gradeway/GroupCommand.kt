/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.group.GroupPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.group.GroupPermissionsTable
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.role.RoleGroupsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.eqId
import dev.gradienttim.gradeway.extensions.formatUTC
import dev.gradienttim.gradeway.extensions.eqUuidStr
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.GroupService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun <TCommandContext> ArgumentBuilder<TCommandContext, *>.groupCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandContext>,
) {
    fun handleCreateGroup(source: TCommandContext, name: String, defaultWeight: Int = -1) {
        val actor = commandContext.sourceToActor(source)
        gradeway.groups.create(actor, name) {
            this.defaultWeight = defaultWeight
        }.onLeft { error ->
            if (commandContext.sendIfCancelled(source, error)) return@onLeft
            if (error is GroupService.CreateGroupError.InvalidName) {
                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.group.create.invalidName",
                        Argument.string("group", name)
                    )
                )
                return
            }
            if (error is GroupService.CreateGroupError.Unexpected) {
                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.group.create.unexpectedError",
                        Argument.string("group", name),
                        Argument.string("error", error.throwable.message ?: "Unknown")
                    )
                )
                return
            }
        }.onRight {
            commandContext.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.command.group.create.success",
                    Argument.string("group", name),
                    Argument.numeric("weight", defaultWeight)
                )
            )
        }
    }

    literal("group") {
        requires { commandContext.hasPermission(it, "gradeway.group") }

        literal("create") {
            requires { commandContext.hasPermission(it, "gradeway.group.create") }

            string("name") {
                execute {
                    val name = stringParam("name")

                    handleCreateGroup(source, name)
                }

                integer("defaultWeight") {
                    execute {
                        val name = stringParam("name")
                        val defaultWeight = intParam("defaultWeight")

                        handleCreateGroup(source, name, defaultWeight)
                    }
                }
            }
        }

        literal("delete") {
            requires { commandContext.hasPermission(it, "gradeway.group.delete") }

            string("idOrName") {
                suggestGroups(gradeway)

                execute {
                    val actor = commandContext.sourceToActor(source)
                    val idOrName = stringParam("idOrName")

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.groups.delete(actor, idOrName)
                            .onLeft { error ->
                                if (commandContext.sendIfCancelled(source, error)) return@requestConfirmation
                                if (error is GroupService.DeleteGroupError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.group.delete.entityNotFound",
                                            Argument.string("group", idOrName)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is GroupService.DeleteGroupError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.group.delete.unexpectedError",
                                            Argument.string("group", idOrName),
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@requestConfirmation
                                }
                            }
                            .onRight {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.delete.success",
                                        Argument.string("group", idOrName)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("info") {
            requires { commandContext.hasPermission(it, "gradeway.group.info") }

            string("idOrName") {
                suggestGroups(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")

                    val group = gradeway.groups.findByIdOrName(idOrName)
                    if (group == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.group.info.entityNotFound",
                                Argument.string("group", idOrName)
                            )
                        )
                        return@execute
                    }

                    val groupId = group.id.value

                    val details = transaction(gradeway.database) {
                        object {
                            val roles = RoleGroupsTable
                                .innerJoin(RolesTable, { roleId }, { id })
                                .select(RolesTable.name)
                                .where { RoleGroupsTable.groupId eqId groupId }
                                .orderBy(RolesTable.weight to SortOrder.DESC)
                                .map { Component.text(it[RolesTable.name]) }
                            val permissions = GroupPermissionsTable
                                .selectAll()
                                .where { GroupPermissionsTable.groupId eqId groupId }
                                .count()
                            val templates = GroupPermissionTemplatesTable
                                .selectAll()
                                .where { GroupPermissionTemplatesTable.groupId eqId groupId }
                                .count()
                        }
                    }

                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.group.info",
                            groupId.toIdArgument(),
                            Argument.string("name", group.name),
                            Argument.numeric("weight", group.defaultWeight),
                            *infoListArguments("roles", details.roles),
                            Argument.numeric("permissions", details.permissions),
                            Argument.numeric("templates", details.templates),
                            Argument.string("created", group.createdAt.formatUTC()),
                            Argument.string("updated", group.updatedAt.formatUTC())
                        )
                    )
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestGroups(gradeway)

                literal("setName") {
                    requires { commandContext.hasPermission(it, "gradeway.group.setName") }

                    string("name") {
                        execute {
                            val actor = commandContext.sourceToActor(source)
                            val idOrName = stringParam("idOrName")
                            val name = stringParam("name")

                            gradeway.groups.setName(actor, idOrName, name)
                                .onLeft { error ->
                                    if (commandContext.sendIfCancelled(source, error)) return@execute
                                    if (error is GroupService.SetNameError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setName.entityNotFound",
                                                Argument.string("group", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.InvalidName) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setName.invalidName",
                                                Argument.string("group", idOrName),
                                                Argument.string("name", name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.NameAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setName.nameAlreadySet",
                                                Argument.string("group", idOrName),
                                                Argument.string("name", name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetNameError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setName.unexpectedError",
                                                Argument.string("group", idOrName),
                                                Argument.string("error", error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.group.setName.success",
                                            Argument.string("group", idOrName),
                                            Argument.string("name", name)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setDefaultWeight") {
                    requires { commandContext.hasPermission(it, "gradeway.group.setDefaltWeight") }

                    integer("defaultWeight") {
                        execute {
                            val actor = commandContext.sourceToActor(source)
                            val idOrName = stringParam("idOrName")
                            val defaultWeight = intParam("defaultWeight")

                            gradeway.groups.setDefaultWeight(actor, idOrName, defaultWeight)
                                .onLeft { error ->
                                    if (commandContext.sendIfCancelled(source, error)) return@execute
                                    if (error is GroupService.SetDefaultWeightError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.entityNotFound",
                                                Argument.string("group", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetDefaultWeightError.WeightAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.weightAlreadySet",
                                                Argument.string("group", idOrName),
                                                Argument.numeric("weight", defaultWeight)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is GroupService.SetDefaultWeightError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.group.setDefaultWeight.unexpectedError",
                                                Argument.string("group", idOrName),
                                                Argument.string("error", error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.group.setDefaultWeight.success",
                                            Argument.string("group", idOrName),
                                            Argument.numeric("weight", defaultWeight)
                                        )
                                    )
                                }
                        }
                    }
                }

                registerGroupRolesCommand(gradeway, commandContext)

                registerEntityPermissionCommands(
                    rootLiteral = rootLiteral,
                    gradeway = gradeway,
                    entityType = "group",
                    context = commandContext,
                    handleSetPermission = { actor, idOrName, permission, status ->
                        gradeway.groups.setPermission(actor, idOrName, permission, status)
                    },
                    handleUnsetPermission = { actor, idOrName, permission ->
                        gradeway.groups.unsetPermission(
                            actor,
                            idOrName,
                            permission
                        )
                    },
                    handleClearPermissions = { actor, idOrName -> gradeway.groups.clearPermissions(actor, idOrName) },
                    handleLinkTemplate = { actor, idOrName, templateIdOrName ->
                        gradeway.permissions.linkTemplateToGroup(actor, templateIdOrName, idOrName)
                    },
                    handleUnlinkTemplate = { actor, idOrName, templateIdOrName ->
                        gradeway.permissions.unlinkTemplateFromGroup(actor, templateIdOrName, idOrName)
                    },
                    handleApplyTemplate = { actor, idOrName, templateIdOrName ->
                        gradeway.permissions.applyTemplateToGroup(actor, templateIdOrName, idOrName)
                    },
                    handleRevokeTemplate = { actor, idOrName, templateIdOrName ->
                        gradeway.permissions.revokeTemplateFromGroup(actor, templateIdOrName, idOrName)
                    },
                    handleListQuery = { scope, page, limit ->
                        GroupPermissionsTable
                            .innerJoin(GroupsTable, { groupId }, { id })
                            .innerJoin(PermissionsTable, { GroupPermissionsTable.permissionId }, { id })
                            .select(PermissionsTable.value, PermissionsTable.type, GroupPermissionsTable.isEnabled)
                            .where {
                                (GroupsTable.id eqUuidStr scope) or (GroupsTable.name.lowerCase() eq scope.lowercase())
                            }
                            .limit(limit)
                            .offset((page - 1).toLong() * limit)
                            .map { row ->
                                object {
                                    val value = row[PermissionsTable.value]
                                    val type = row[PermissionsTable.type]
                                    val isEnabled = row[GroupPermissionsTable.isEnabled]
                                }
                            }
                    },
                    handleListRender = { source, page, limit, result ->
                        if (result.isEmpty()) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.group.listPermissions.empty")
                            )
                            return@registerEntityPermissionCommands
                        }

                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.group.listPermissions.header",
                                Argument.numeric("page", page),
                                Argument.numeric("limit", limit)
                            )
                        )

                        result.forEach { permissionEntity ->
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.group.listPermissions.entry",
                                    Argument.string("permission", permissionEntity.value),
                                    Argument.string("type", permissionEntity.type.name),
                                    Argument.bool("enabled", permissionEntity.isEnabled)
                                )
                            )
                        }
                    }
                )
            }
        }

        registerWeightedListCommand(
            context = commandContext,
            gradeway = gradeway,
            permission = "gradeway.group.list",
            query = { page, limit, order ->
                GroupsTable
                    .select(GroupsTable.id, GroupsTable.name, GroupsTable.defaultWeight)
                    .orderBy(GroupsTable.defaultWeight to order, GroupsTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(((page - 1) * limit).toLong())
                    .map { row ->
                        object {
                            val id = row[GroupsTable.id].value
                            val name = row[GroupsTable.name]
                            val weight = row[GroupsTable.defaultWeight]
                        }
                    }
            },
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.group.list.empty")
                    )
                    return@registerWeightedListCommand
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.group.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { group ->
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.group.list.entry",
                            group.id.toIdArgument(),
                            Argument.string("name", group.name),
                            Argument.numeric("weight", group.weight)
                        )
                    )
                }
            }
        )
    }
}

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.registerGroupRolesCommand(
    gradeway: CommonGradeway<*>,
    context: CommandContext<TCommandSource>,
) {
    literal("roles") {
        literal("add") {
            requires { context.hasPermission(it, "gradeway.group.roles.add") }

            string("role") {
                suggestRoles(gradeway)

                execute {
                    val actor = context.sourceToActor(source)
                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("role")

                    val roleUniqueId = gradeway.roles.findByIdOrName(roleId)?.id?.value

                    if (roleUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.group.addRole.targetNotFound",
                                Argument.string("group", idOrName),
                                Argument.string("role", roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.groups.addRoleToGroup(actor, idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (context.sendIfCancelled(source, error)) return@execute
                            if (error is GroupService.AddTargetError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.addRole.entityNotFound",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.addRole.targetNotFound",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.AlreadyInGroup) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.addRole.alreadyInGroup",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.AddTargetError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.addRole.unexpectedError",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.group.addRole.success",
                                    Argument.string("group", idOrName),
                                    Argument.string("role", roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("remove") {
            requires { context.hasPermission(it, "gradeway.group.roles.remove") }

            string("role") {
                suggestRoles(gradeway)

                execute {
                    val actor = context.sourceToActor(source)
                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("role")

                    val roleUniqueId = gradeway.roles.findByIdOrName(roleId)?.id?.value

                    if (roleUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.group.removeRole.targetNotFound",
                                Argument.string("group", idOrName),
                                Argument.string("role", roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.groups.removeRoleFromGroup(actor, idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (context.sendIfCancelled(source, error)) return@execute
                            if (error is GroupService.RemoveTargetError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.entityNotFound",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.targetNotFound",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.NotInGroup) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.notInGroup",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is GroupService.RemoveTargetError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.group.removeRole.unexpectedError",
                                        Argument.string("group", idOrName),
                                        Argument.string("role", roleId),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.group.removeRole.success",
                                    Argument.string("group", idOrName),
                                    Argument.string("role", roleId)
                                )
                            )
                        }
                }
            }
        }

        registerScopedListCommand(
            gradeway = gradeway,
            permission = "gradeway.group.roles.list",
            scopeKey = "idOrName",
            context = context,
            query = { idOrName, page, limit ->
                val group = gradeway.groups.findByIdOrName(idOrName)
                object {
                    val requested = idOrName
                    val groupName = group?.name
                    val roles = group?.let {
                        RoleGroupsTable
                            .innerJoin(RolesTable, { roleId }, { id })
                            .select(RolesTable.id, RolesTable.name, RolesTable.weight)
                            .where { RoleGroupsTable.groupId eqId it.id.value }
                            .orderBy(RolesTable.weight to SortOrder.DESC, RolesTable.name to SortOrder.ASC)
                            .limit(limit)
                            .offset(((page - 1) * limit).toLong())
                            .map { row ->
                                object {
                                    val id = row[RolesTable.id].value
                                    val name = row[RolesTable.name]
                                    val weight = row[RolesTable.weight]
                                }
                            }
                    }.orEmpty()
                }
            },
            render = { source, page, limit, result ->
                val groupName = result.groupName
                if (groupName == null) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.group.listRoles.entityNotFound",
                            Argument.string("group", result.requested)
                        )
                    )
                    return@registerScopedListCommand
                }

                if (result.roles.isEmpty()) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.group.listRoles.empty",
                            Argument.string("group", groupName)
                        )
                    )
                    return@registerScopedListCommand
                }

                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.group.listRoles.header",
                        Argument.string("group", groupName),
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.roles.forEach { role ->
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.group.listRoles.entry",
                            role.id.toIdArgument(),
                            Argument.string("name", role.name),
                            Argument.numeric("weight", role.weight)
                        )
                    )
                }
            }
        )
    }
}
