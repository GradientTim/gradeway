/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.group.GroupPermissionsTable
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.GroupService
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

internal fun <C : Any> MutableCommandBuilder<C>.registerGroupCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
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

    registerCopy("group") {
        registerCopy("create") {
            permission("gradeway.group.create")

            required("name", stringParser())
            optional("defaultWeight", integerParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val name = context.get<String>("name")
                val defaultWeight = context.getOrDefault("defaultWeight", -1)

                handleCreateGroup(audience, name, defaultWeight)
            }
        }

        registerCopy("delete") {
            permission("gradeway.group.delete")

            required("idOrName", stringParser()) {
                suggests { remaining -> suggestGroups(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                gradeway.groups.delete(idOrName)
                    .onLeft { error ->
                        if (error is GroupService.DeleteGroupError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.delete.entityNotFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@handler
                        }
                        if (error is GroupService.DeleteGroupError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.delete.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
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

        registerCopy("modify") {
            required("idOrName", stringParser()) {
                suggests { remaining -> suggestGroups(gradeway, remaining.lowercase()) }
            }

            registerCopy("setName") {
                permission("gradeway.group.setName")

                required("name", stringParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val name = context.get<String>("name")

                    gradeway.groups.setName(idOrName, name)
                        .onLeft { error ->
                            if (error is GroupService.SetNameError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setName.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is GroupService.SetNameError.InvalidName) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setName.invalidName",
                                        Component.text(idOrName),
                                        Component.text(name)
                                    )
                                )
                                return@handler
                            }
                            if (error is GroupService.SetNameError.NameAlreadySet) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setName.nameAlreadySet",
                                        Component.text(idOrName),
                                        Component.text(name)
                                    )
                                )
                                return@handler
                            }
                            if (error is GroupService.SetNameError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setName.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@handler
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

            registerCopy("setDefaultWeight") {
                permission("gradeway.group.setDefaltWeight")

                required("defaultWeight", integerParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val defaultWeight = context.get<Int>("defaultWeight")

                    gradeway.groups.setDefaultWeight(idOrName, defaultWeight)
                        .onLeft { error ->
                            if (error is GroupService.SetDefaultWeightError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setDefaultWeight.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is GroupService.SetDefaultWeightError.WeightAlreadySet) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setDefaultWeight.weightAlreadySet",
                                        Component.text(idOrName),
                                        Component.text(defaultWeight)
                                    )
                                )
                                return@handler
                            }
                            if (error is GroupService.SetDefaultWeightError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.group.setDefaultWeight.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@handler
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

            registerGroupRolesCommand(gradeway, audienceProvider)

            registerEntityPermissionCommands(
                gradeway = gradeway,
                entityType = "group",
                audienceProvider = audienceProvider,
                handleSetPermission = { idOrName, permission, status ->
                    gradeway.groups.setPermission(idOrName, permission, status)
                },
                handleUnsetPermission = { idOrName, permission ->
                    gradeway.groups.unsetPermission(
                        idOrName,
                        permission
                    )
                },
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
                                    (GroupsTable.name.lowerCase() like "${scope.lowercase()}%")
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
                        return@registerEntityPermissionCommands
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

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.group.list",
            audienceProvider = audienceProvider,
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
                    return@registerGlobalListCommand
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

internal fun <C : Any> MutableCommandBuilder<C>.registerGroupRolesCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("roles") {
        registerCopy("add") {
            permission("gradeway.group.roles.add")

            required("roleId", stringParser()) {
                suggests { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val roleId = context.get<String>("roleId")

                val roleUniqueId = runCatching { UUID.fromString(roleId) }.getOrNull()

                if (roleUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.group.addRole.invalidUuid",
                            Component.text(idOrName),
                            Component.text(roleId)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is GroupService.AddTargetError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.addRole.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
                        }
                        if (error is GroupService.AddTargetError.AlreadyInGroup) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.addRole.alreadyInGroup",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("remove") {
            permission("gradeway.group.roles.remove")

            required("roleId", stringParser()) {
                suggests { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val roleId = context.get<String>("roleId")

                val roleUniqueId = runCatching { UUID.fromString(roleId) }.getOrNull()

                if (roleUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.group.removeRole.invalidUuid",
                            Component.text(idOrName),
                            Component.text(roleId)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is GroupService.RemoveTargetError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.removeRole.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
                        }
                        if (error is GroupService.RemoveTargetError.NotInGroup) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.group.removeRole.notInGroup",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
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
                            return@handler
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
