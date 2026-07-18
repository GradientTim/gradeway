/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.role.RoleAttributesTable
import dev.gradienttim.gradeway.database.models.role.RolePermissionsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.RoleService.*
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

internal fun <C : Any> MutableCommandBuilder<C>.registerRoleCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("role") {
        registerCopy("create") {
            permission("gradeway.role.create")

            required("name", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val name = context.get<String>("name")

                gradeway.roles.create(name)
                    .onLeft { error ->
                        if (error is CreateRoleError.EntityAlreadyExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.create.entityAlreadyExists",
                                    Component.text(name)
                                )
                            )
                            return@handler
                        }
                        if (error is CreateRoleError.InvalidName) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.create.invalidName",
                                    Component.text(name)
                                )
                            )
                            return@handler
                        }
                        if (error is CreateRoleError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.create.unexpectedError",
                                    Component.text(name),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
                        }
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.create.success",
                                Component.text(name)
                            )
                        )
                    }
            }
        }

        registerCopy("delete") {
            permission("gradeway.role.delete")

            required("id", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val id = context.get<String>("id")
                val uuid = runCatching { UUID.fromString(id) }.getOrNull()

                if (uuid == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.delete.invalidUuid",
                            Component.text(id)
                        )
                    )
                    return@handler
                }

                gradeway.roles.delete(uuid)
                    .onLeft { error ->
                        if (error is DeleteRoleError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.delete.entityNotFound",
                                    Component.text(id)
                                )
                            )
                            return@handler
                        }
                        if (error is DeleteRoleError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.delete.unexpectedError",
                                    Component.text(id),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
                        }
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.delete.success",
                                Component.text(id)
                            )
                        )
                    }
            }
        }

        registerCopy("modify") {
            required("idOrName", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            registerEntityAttributeCommands(
                gradeway = gradeway,
                entityType = "role",
                audienceProvider = audienceProvider,
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
                handleListRender = { audience, page, limit, result ->
                    if (result.isEmpty()) {
                        audience.sendMessage(
                            Component.translatable("gradeway.command.role.listAttributes.empty")
                        )
                        return@registerEntityAttributeCommands
                    }

                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listAttributes.header",
                            Component.text(page),
                            Component.text(limit)
                        )
                    )

                    result.forEach { attributeEntity ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.listAttributes.entry",
                                Component.text(attributeEntity.type),
                                Component.text(attributeEntity.key.asString()),
                                Component.text(attributeEntity.value)
                            )
                        )
                    }
                }
            )

            registerEntityPermissionCommands(
                gradeway = gradeway,
                entityType = "role",
                audienceProvider = audienceProvider,
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
                handleListRender = { audience, page, limit, result ->
                    if (result.isEmpty()) {
                        audience.sendMessage(
                            Component.translatable("gradeway.command.role.listPermissions.empty")
                        )
                        return@registerEntityPermissionCommands
                    }

                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listPermissions.header",
                            Component.text(page),
                            Component.text(limit)
                        )
                    )

                    result.forEach { permissionEntity ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.listPermissions.entry",
                                Component.text(permissionEntity.value),
                                Component.text(permissionEntity.type.name),
                                Component.text(permissionEntity.isEnabled)
                            )
                        )
                    }
                }
            )

            registerRoleParentsCommand(gradeway, audienceProvider)

            registerCopy("setWeight") {
                permission("gradeway.role.setWeight")

                required("value", integerParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val weight = context.get<Int>("value")

                    gradeway.roles.setWeight(idOrName, weight)
                        .onLeft { error ->
                            if (error is SetWeightError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setWeight.entityNotFound",
                                        Component.text(idOrName),
                                    )
                                )
                                return@handler
                            }
                            if (error is SetWeightError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setWeight.unexpectedError",
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
                                    "gradeway.command.role.setWeight.success",
                                    Component.text(idOrName),
                                    Component.text(weight)
                                )
                            )
                        }
                }
            }
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.role.list",
            audienceProvider = audienceProvider,
            query = { page, limit ->
                RolesTable
                    .select(RolesTable.id, RolesTable.name)
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[RolesTable.id].value
                            val name = row[RolesTable.name]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(Component.translatable("gradeway.command.role.list.empty"))
                    return@registerGlobalListCommand
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.role.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { role ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.list.entry",
                            Component.text(role.id.toString()),
                            Component.text(role.name)
                        )
                    )
                }
            }
        )
    }
}

internal fun <C : Any> MutableCommandBuilder<C>.registerRoleParentsCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("parents") {
        registerCopy("add") {
            permission("gradeway.role.parents.add")

            required("parentId", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val parentId = context.get<String>("parentId")

                val parentUniqueId = runCatching { UUID.fromString(parentId) }.getOrNull()

                if (parentUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.addParent.invalidUuid",
                            Component.text(idOrName),
                            Component.text(parentId)
                        )
                    )
                    return@handler
                }

                gradeway.roles.addParent(idOrName, parentUniqueId)
                    .onLeft { error ->
                        if (error is AddParentError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.entityNotFound",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is AddParentError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is AddParentError.SelfReference) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.selfReference",
                                    Component.text(idOrName)
                                )
                            )
                            return@handler
                        }
                        if (error is AddParentError.AlreadyParent) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.alreadyParent",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is AddParentError.CyclicRelation) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.cyclicRelation",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is AddParentError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.addParent.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(parentId),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
                        }
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.addParent.success",
                                Component.text(idOrName),
                                Component.text(parentId)
                            )
                        )
                    }
            }
        }

        registerCopy("remove") {
            permission("gradeway.role.parents.remove")

            required("parentId", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val parentId = context.get<String>("parentId")

                val parentUniqueId = runCatching { UUID.fromString(parentId) }.getOrNull()

                if (parentUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.removeParent.invalidUuid",
                            Component.text(idOrName),
                            Component.text(parentId)
                        )
                    )
                    return@handler
                }

                gradeway.roles.removeParent(idOrName, parentUniqueId)
                    .onLeft { error ->
                        if (error is RemoveParentError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.removeParent.entityNotFound",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is RemoveParentError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.removeParent.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is RemoveParentError.NotParent) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.removeParent.notParent",
                                    Component.text(idOrName),
                                    Component.text(parentId)
                                )
                            )
                            return@handler
                        }
                        if (error is RemoveParentError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.removeParent.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(parentId),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
                        }
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.removeParent.success",
                                Component.text(idOrName),
                                Component.text(parentId)
                            )
                        )
                    }
            }
        }

        registerCopy("list") {
            permission("gradeway.role.parents.list")

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                val entity = gradeway.roles.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listParents.entityNotFound",
                            Component.text(idOrName)
                        )
                    )
                    return@handler
                }

                entity.parents.forEach { roleParentEntity ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listParents.entry",
                            Component.text(roleParentEntity.parent.name)
                        )
                    )
                }
            }
        }
    }
}
