/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.role.RoleAttributesTable
import dev.gradienttim.gradeway.database.models.role.RoleParentsTable
import dev.gradienttim.gradeway.database.models.role.RolePermissionsTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.RoleService.*
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*
import javax.management.relation.RoleStatus

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
                suggests { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
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
                suggests { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
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

            registerRoleRelationsCommand(gradeway, audienceProvider)

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

internal fun <C : Any> MutableCommandBuilder<C>.registerRoleRelationsCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerRoleRelationCommands(
        gradeway = gradeway,
        literal = "parents",
        relationKey = "Parent",
        targetKey = "parentId",
        audienceProvider = audienceProvider,
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
        handleListRender = { audience, page, limit, result ->
            if (result.isEmpty()) {
                audience.sendMessage(Component.translatable("gradeway.command.role.listParents.empty"))
                return@registerRoleRelationCommands
            }

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.role.listParents.header",
                    Component.text(page),
                    Component.text(limit)
                )
            )

            result.forEach { parent ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.role.listParents.entry",
                        Component.text(parent.name)
                    )
                )
            }
        }
    )

    registerRoleRelationCommands(
        gradeway = gradeway,
        literal = "children",
        relationKey = "Child",
        targetKey = "childId",
        audienceProvider = audienceProvider,
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
        handleListRender = { audience, page, limit, result ->
            if (result.isEmpty()) {
                audience.sendMessage(Component.translatable("gradeway.command.role.listChildren.empty"))
                return@registerRoleRelationCommands
            }

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.role.listChildren.header",
                    Component.text(page),
                    Component.text(limit)
                )
            )

            result.forEach { child ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.role.listChildren.entry",
                        Component.text(child.name)
                    )
                )
            }
        }
    )
}
