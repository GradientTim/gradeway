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
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayerRolesTable
import dev.gradienttim.gradeway.database.models.role.RoleAttributesTable
import dev.gradienttim.gradeway.database.models.role.RoleGroupsTable
import dev.gradienttim.gradeway.database.models.role.RoleParentsTable
import dev.gradienttim.gradeway.database.models.role.RolePermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.role.RolePermissionsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.eqId
import dev.gradienttim.gradeway.extensions.formatUTC
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.RoleService.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.roleCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("role") {
        requires { commandContext.hasPermission(it, "gradeway.role") }

        literal("create") {
            requires { commandContext.hasPermission(it, "gradeway.role.create") }

            string("name") {
                execute {
                    val name = stringParam("name")

                    gradeway.roles.create(name)
                        .onLeft { error ->
                            if (error is CreateRoleError.EntityAlreadyExists) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.create.entityAlreadyExists",
                                        Argument.string("role", name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateRoleError.InvalidName) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.create.invalidName",
                                        Argument.string("role", name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateRoleError.Unexpected) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.create.unexpectedError",
                                        Argument.string("role", name),
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
                                    "gradeway.command.role.create.success",
                                    Argument.string("role", name)
                                )
                            )
                        }
                }
            }
        }

        literal("delete") {
            requires { commandContext.hasPermission(it, "gradeway.role.delete") }

            string("idOrName") {
                suggestRoles(gradeway)

                execute {
                    val id = stringParam("idOrName")

                    val role = gradeway.roles.findByIdOrName(id)
                    if (role == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.delete.entityNotFound",
                                Argument.string("role", id)
                            )
                        )
                        return@execute
                    }

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.roles.delete(role.id.value)
                            .onLeft { error ->
                                if (error is DeleteRoleError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.role.delete.entityNotFound",
                                            Argument.string("role", role.name)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is DeleteRoleError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.role.delete.unexpectedError",
                                            Argument.string("role", role.name),
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
                                        "gradeway.command.role.delete.success",
                                        Argument.string("role", role.name)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("info") {
            requires { commandContext.hasPermission(it, "gradeway.role.info") }

            string("idOrName") {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")

                    val role = gradeway.roles.findByIdOrName(idOrName)
                    if (role == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.info.entityNotFound",
                                Argument.string("role", idOrName)
                            )
                        )
                        return@execute
                    }

                    val roleId = role.id.value

                    val details = transaction(gradeway.database) {
                        object {
                            val parents = RoleParentsTable
                                .innerJoin(RolesTable, { parentId }, { id })
                                .select(RolesTable.name)
                                .where { RoleParentsTable.childId eqId roleId }
                                .orderBy(RolesTable.name to SortOrder.ASC)
                                .map { Component.text(it[RolesTable.name]) }
                            val children = RoleParentsTable
                                .innerJoin(RolesTable, { childId }, { id })
                                .select(RolesTable.name)
                                .where { RoleParentsTable.parentId eqId roleId }
                                .orderBy(RolesTable.name to SortOrder.ASC)
                                .map { Component.text(it[RolesTable.name]) }
                            val groups = RoleGroupsTable
                                .innerJoin(GroupsTable, { groupId }, { id })
                                .select(GroupsTable.name)
                                .where { RoleGroupsTable.roleId eqId roleId }
                                .orderBy(GroupsTable.name to SortOrder.ASC)
                                .map { Component.text(it[GroupsTable.name]) }
                            val players = PlayerRolesTable
                                .selectAll()
                                .where { PlayerRolesTable.roleId eqId roleId }
                                .count()
                            val permissions = RolePermissionsTable
                                .selectAll()
                                .where { RolePermissionsTable.roleId eqId roleId }
                                .count()
                            val templates = RolePermissionTemplatesTable
                                .selectAll()
                                .where { RolePermissionTemplatesTable.roleId eqId roleId }
                                .count()
                            val attributes = RoleAttributesTable
                                .selectAll()
                                .where { RoleAttributesTable.roleId eqId roleId }
                                .count()
                        }
                    }

                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.role.info",
                            roleId.toIdArgument(),
                            Argument.string("name", role.name),
                            Argument.component(
                                "default",
                                if (role.isDefault) {
                                    Component.translatable("gradeway.command.role.info.defaultMarker")
                                } else {
                                    Component.empty()
                                }
                            ),
                            Argument.numeric("weight", role.weight),
                            Argument.numeric("players", details.players),
                            *infoListArguments("parents", details.parents),
                            *infoListArguments("children", details.children),
                            *infoListArguments("groups", details.groups),
                            Argument.numeric("permissions", details.permissions),
                            Argument.numeric("templates", details.templates),
                            Argument.numeric("attributes", details.attributes),
                            Argument.string("created", role.createdAt.formatUTC()),
                            Argument.string("updated", role.updatedAt.formatUTC())
                        )
                    )
                }
            }
        }

        literal("default") {
            requires { commandContext.hasPermission(it, "gradeway.role.default") }

            execute {
                val role = gradeway.roles.getDefaultRole()
                if (role == null) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.role.default.info.none")
                    )
                    return@execute
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.role.default.info",
                        Argument.string("role", role.name)
                    )
                )
                warnAboutMultipleDefaultRoles(gradeway, commandContext, source)
            }

            literal("set") {
                requires { commandContext.hasPermission(it, "gradeway.role.default.set") }

                string("idOrName") {
                    suggestRoles(gradeway)

                    execute {
                        val idOrName = stringParam("idOrName")

                        gradeway.roles.setDefault(idOrName)
                            .onLeft { error ->
                                if (error is SetDefaultError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.role.default.set.entityNotFound",
                                            Argument.string("role", idOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is SetDefaultError.AlreadyDefault) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.role.default.set.alreadyDefault",
                                            Argument.string("role", idOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is SetDefaultError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.role.default.set.unexpectedError",
                                            Argument.string("role", idOrName),
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
                                        "gradeway.command.role.default.set.success",
                                        Argument.string("role", idOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("clear") {
                requires { commandContext.hasPermission(it, "gradeway.role.default.clear") }

                execute {
                    gradeway.roles.clearDefault()
                        .onLeft { error ->
                            if (error is ClearDefaultError.NoDefaultRole) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable("gradeway.command.role.default.clear.noDefaultRole")
                                )
                                return@execute
                            }
                            if (error is ClearDefaultError.Unexpected) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.default.clear.unexpectedError",
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.role.default.clear.success")
                            )
                        }
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestRoles(gradeway)

                registerEntityAttributeCommands(
                    rootLiteral = rootLiteral,
                    gradeway = gradeway,
                    entityType = "role",
                    context = commandContext,
                    handleAddAttribute = { idOrName, attribute -> gradeway.roles.addAttribute(idOrName, attribute) },
                    handleUpdateAttribute = { idOrName, key, value ->
                        gradeway.roles.updateAttribute(
                            idOrName,
                            key,
                            value
                        )
                    },
                    handleRemoveAttribute = { idOrName, key -> gradeway.roles.removeAttribute(idOrName, key) },
                    handleClearAttributes = { idOrName -> gradeway.roles.clearAttributes(idOrName) },
                    handleListQuery = { scope, page, limit ->
                        RoleAttributesTable
                            .innerJoin(RolesTable, { roleId }, { id })
                            .select(
                                RoleAttributesTable.type,
                                RoleAttributesTable.key,
                                RoleAttributesTable.value
                            )
                            .where {
                                (RolesTable.id likeAsStr "$scope%") or
                                        (RolesTable.name.lowerCase() like "${scope.lowercase()}%")
                            }
                            .limit(limit)
                            .offset((page - 1).toLong())
                            .map { row ->
                                object {
                                    val type = row[RoleAttributesTable.type]
                                    val key = row[RoleAttributesTable.key]
                                    val value = row[RoleAttributesTable.value]
                                }
                            }
                    },
                    handleListRender = { source, page, limit, result ->
                        if (result.isEmpty()) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.role.listAttributes.empty")
                            )
                            return@registerEntityAttributeCommands
                        }

                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.listAttributes.header",
                                Argument.numeric("page", page),
                                Argument.numeric("limit", limit)
                            )
                        )

                        result.forEach { attributeEntity ->
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.role.listAttributes.entry",
                                    Argument.string("type", attributeEntity.type),
                                    Argument.string("attribute", attributeEntity.key.asString()),
                                    Argument.string("value", attributeEntity.value)
                                )
                            )
                        }
                    }
                )

                registerEntityPermissionCommands(
                    rootLiteral = rootLiteral,
                    gradeway = gradeway,
                    entityType = "role",
                    context = commandContext,
                    handleSetPermission = { idOrName, permission, status ->
                        gradeway.roles.setPermission(idOrName, permission, status)
                    },
                    handleUnsetPermission = { idOrName, permission ->
                        gradeway.roles.unsetPermission(
                            idOrName,
                            permission
                        )
                    },
                    handleClearPermissions = { idOrName -> gradeway.roles.clearPermissions(idOrName) },
                    handleLinkTemplate = { idOrName, templateIdOrName ->
                        gradeway.permissions.linkTemplateToRole(templateIdOrName, idOrName).map { }
                    },
                    handleUnlinkTemplate = { idOrName, templateIdOrName ->
                        gradeway.permissions.unlinkTemplateFromRole(templateIdOrName, idOrName)
                    },
                    handleApplyTemplate = { idOrName, templateIdOrName ->
                        gradeway.permissions.applyTemplateToRole(templateIdOrName, idOrName)
                    },
                    handleRevokeTemplate = { idOrName, templateIdOrName ->
                        gradeway.permissions.revokeTemplateFromRole(templateIdOrName, idOrName)
                    },
                    handleListQuery = { scope, page, limit ->
                        RolePermissionsTable
                            .innerJoin(RolesTable, { roleId }, { id })
                            .innerJoin(PermissionsTable, { RolePermissionsTable.permissionId }, { id })
                            .select(PermissionsTable.value, PermissionsTable.type, RolePermissionsTable.isEnabled)
                            .where {
                                (RolesTable.id likeAsStr "$scope%") or
                                        (RolesTable.name.lowerCase() like "${scope.lowercase()}%")
                            }
                            .limit(limit)
                            .offset((page - 1).toLong())
                            .map { row ->
                                object {
                                    val value = row[PermissionsTable.value]
                                    val type = row[PermissionsTable.type]
                                    val isEnabled = row[RolePermissionsTable.isEnabled]
                                }
                            }
                    },
                    handleListRender = { source, page, limit, result ->
                        if (result.isEmpty()) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.role.listPermissions.empty")
                            )
                            return@registerEntityPermissionCommands
                        }

                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.listPermissions.header",
                                Argument.numeric("page", page),
                                Argument.numeric("limit", limit)
                            )
                        )

                        result.forEach { permissionEntity ->
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.role.listPermissions.entry",
                                    Argument.string("permission", permissionEntity.value),
                                    Argument.string("type", permissionEntity.type.name),
                                    Argument.bool("enabled", permissionEntity.isEnabled)
                                )
                            )
                        }
                    }
                )

                registerRoleRelationsCommand(gradeway, commandContext)

                literal("setName") {
                    requires { commandContext.hasPermission(it, "gradeway.role.setName") }

                    string("name") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val name = stringParam("name")

                            gradeway.roles.setName(idOrName, name)
                                .onLeft { error ->
                                    if (error is SetNameError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setName.entityNotFound",
                                                Argument.string("role", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameError.InvalidName) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setName.invalidName",
                                                Argument.string("role", idOrName),
                                                Argument.string("name", name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameError.NameAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setName.nameAlreadySet",
                                                Argument.string("role", idOrName),
                                                Argument.string("name", name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameError.NameAlreadyExists) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setName.nameAlreadyExists",
                                                Argument.string("role", idOrName),
                                                Argument.string("name", name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setName.unexpectedError",
                                                Argument.string("role", idOrName),
                                                Argument.string("name", name),
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
                                            "gradeway.command.role.setName.success",
                                            Argument.string("role", idOrName),
                                            Argument.string("name", name)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setDefault") {
                    requires { commandContext.hasPermission(it, "gradeway.role.setDefault") }

                    boolean("isDefault") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val isDefault = booleanParam("isDefault")

                            gradeway.roles.setDefaultFlag(idOrName, isDefault)
                                .onLeft { error ->
                                    if (error is SetDefaultFlagError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setDefault.entityNotFound",
                                                Argument.string("role", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetDefaultFlagError.FlagAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            if (isDefault) {
                                                Component.translatable(
                                                    "gradeway.command.role.setDefault.alreadyDefault",
                                                    Argument.string("role", idOrName)
                                                )
                                            } else {
                                                Component.translatable(
                                                    "gradeway.command.role.setDefault.notDefault",
                                                    Argument.string("role", idOrName)
                                                )
                                            }
                                        )
                                        return@execute
                                    }
                                    if (error is SetDefaultFlagError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setDefault.unexpectedError",
                                                Argument.string("role", idOrName),
                                                Argument.string("error", error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        if (isDefault) {
                                            Component.translatable(
                                                "gradeway.command.role.setDefault.marked",
                                                Argument.string("role", idOrName)
                                            )
                                        } else {
                                            Component.translatable(
                                                "gradeway.command.role.setDefault.unmarked",
                                                Argument.string("role", idOrName)
                                            )
                                        }
                                    )
                                    warnAboutMultipleDefaultRoles(gradeway, commandContext, source)
                                }
                        }
                    }
                }

                literal("setWeight") {
                    requires { commandContext.hasPermission(it, "gradeway.role.setWeight") }

                    integer("value") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val weight = intParam("value")

                            gradeway.roles.setWeight(idOrName, weight)
                                .onLeft { error ->
                                    if (error is SetWeightError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setWeight.entityNotFound",
                                                Argument.string("role", idOrName),
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetWeightError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.role.setWeight.unexpectedError",
                                                Argument.string("role", idOrName),
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
                                            "gradeway.command.role.setWeight.success",
                                            Argument.string("role", idOrName),
                                            Argument.numeric("weight", weight)
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        registerWeightedListCommand(
            gradeway = gradeway,
            permission = "gradeway.role.list",
            context = commandContext,
            query = { page, limit, order ->
                RolesTable
                    .select(RolesTable.id, RolesTable.name, RolesTable.weight, RolesTable.isDefault)
                    .orderBy(RolesTable.weight to order, RolesTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(((page - 1) * limit).toLong())
                    .map { row ->
                        object {
                            val id = row[RolesTable.id].value
                            val name = row[RolesTable.name]
                            val weight = row[RolesTable.weight]
                            val isDefault = row[RolesTable.isDefault]
                        }
                    }
            },
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.role.list.empty")
                    )
                    return@registerWeightedListCommand
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.role.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { role ->
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.role.list.entry",
                            role.id.toIdArgument(),
                            Argument.string("name", role.name),
                            Argument.numeric("weight", role.weight),
                            Argument.component("default", infoBoolean(role.isDefault))
                        )
                    )
                }
            }
        )
    }
}

private fun <TCommandSource> warnAboutMultipleDefaultRoles(
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
    source: TCommandSource,
) {
    val defaultRoles = gradeway.roles.getDefaultRoles()
    if (defaultRoles.size < 2) {
        return
    }

    commandContext.sendTranslatedMessage(
        source,
        Component.translatable(
            "gradeway.command.role.default.multipleWarning",
            *infoListArguments("roles", defaultRoles.map { Component.text(it.name) })
        )
    )
}

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.registerRoleRelationsCommand(
    gradeway: CommonGradeway<*>,
    context: CommandContext<TCommandSource>,
) {
    registerRoleRelationCommands(
        gradeway = gradeway,
        literalName = "parents",
        relationKey = "Parent",
        targetKey = "parent",
        context = context,
        handleAdd = { idOrName, parentId -> gradeway.roles.addParent(idOrName, parentId) },
        handleRemove = { idOrName, parentId -> gradeway.roles.removeParent(idOrName, parentId) },
        handleListQuery = { scope, page, limit ->
            val childRole = RolesTable.alias("child_role")
            val parentRole = RolesTable.alias("parent_role")

            RoleParentsTable
                .innerJoin(childRole, { childId }, { childRole[RolesTable.id] })
                .innerJoin(parentRole, { RoleParentsTable.parentId }, { parentRole[RolesTable.id] })
                .select(parentRole[RolesTable.name])
                .where {
                    (childRole[RolesTable.id] likeAsStr "$scope%") or
                            (childRole[RolesTable.name].lowerCase() like "${scope.lowercase()}%")
                }
                .limit(limit)
                .offset((page - 1).toLong())
                .map { row ->
                    object {
                        val name = row[parentRole[RolesTable.name]]
                    }
                }
        },
        handleListRender = { source, page, limit, result ->
            if (result.isEmpty()) {
                context.sendTranslatedMessage(source, Component.translatable("gradeway.command.role.listParents.empty"))
                return@registerRoleRelationCommands
            }

            context.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.command.role.listParents.header",
                    Argument.numeric("page", page),
                    Argument.numeric("limit", limit)
                )
            )

            result.forEach { parent ->
                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.role.listParents.entry",
                        Argument.string("parent", parent.name)
                    )
                )
            }
        }
    )

    registerRoleRelationCommands(
        gradeway = gradeway,
        literalName = "children",
        relationKey = "Child",
        targetKey = "child",
        context = context,
        handleAdd = { idOrName, childId -> gradeway.roles.addChild(idOrName, childId) },
        handleRemove = { idOrName, childId -> gradeway.roles.removeChild(idOrName, childId) },
        handleListQuery = { scope, page, limit ->
            val parentRole = RolesTable.alias("parent_role")
            val childRole = RolesTable.alias("child_role")

            RoleParentsTable
                .innerJoin(parentRole, { parentId }, { parentRole[RolesTable.id] })
                .innerJoin(childRole, { RoleParentsTable.childId }, { childRole[RolesTable.id] })
                .select(childRole[RolesTable.name])
                .where {
                    (parentRole[RolesTable.id] likeAsStr "$scope%") or
                            (parentRole[RolesTable.name].lowerCase() like "${scope.lowercase()}%")
                }
                .limit(limit)
                .offset((page - 1).toLong())
                .map { row ->
                    object {
                        val name = row[childRole[RolesTable.name]]
                    }
                }
        },
        handleListRender = { source, page, limit, result ->
            if (result.isEmpty()) {
                context.sendTranslatedMessage(
                    source,
                    Component.translatable("gradeway.command.role.listChildren.empty")
                )
                return@registerRoleRelationCommands
            }

            context.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.command.role.listChildren.header",
                    Argument.numeric("page", page),
                    Argument.numeric("limit", limit)
                )
            )

            result.forEach { child ->
                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.role.listChildren.entry",
                        Argument.string("child", child.name)
                    )
                )
            }
        }
    )
}
