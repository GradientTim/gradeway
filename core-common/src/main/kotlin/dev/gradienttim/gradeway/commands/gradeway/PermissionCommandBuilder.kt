/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.createGlobalListHandler
import dev.gradienttim.gradeway.commands.extensions.createScopedListHandler
import dev.gradienttim.gradeway.commands.extensions.suggestPermissionTemplates
import dev.gradienttim.gradeway.commands.extensions.suggestPermissions
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatePermissionsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

internal fun <TSource> ArgumentBuilder<TSource, *>.permissionBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    fun handleAddPermission(audience: Audience, value: String, type: PermissionEntity.Type) {
        gradeway.permissions.createPermission(value, type)
            .onLeft { error ->
                if (error is CreatePermissionError.AlreadyExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permission.add.alreadyExists",
                            Component.text(value)
                        )
                    )
                    return
                }
                if (error is CreatePermissionError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permission.add.unexpectedError",
                            Component.text(value),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.permission.add.success",
                        Component.text(value),
                        Component.text(type.name)
                    )
                )
            }
    }

    literal("permission") {
        requires { hasPermission(it, "gradeway.permission") }

        literal("add") {
            requires { hasPermission(it, "gradeway.permission.add") }

            string("value") {
                permissionType("type") {
                    execute {
                        val audience = sourceToAudience(source)

                        val value = stringParam("value")
                        val rawType = stringParam("type").lowercase()

                        val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType }
                        if (type == null) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permission.add.invalidType",
                                    Component.text(rawType)
                                )
                            )
                            return@execute
                        }

                        handleAddPermission(audience, value, type)
                    }
                }

                execute {
                    val audience = sourceToAudience(source)
                    val value = stringParam("value")

                    handleAddPermission(audience, value, PermissionEntity.Type.EQUALS)
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.permission.remove") }

            string("idOrValue") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrValue = stringParam("idOrValue")

                    gradeway.permissions.deletePermission(idOrValue)
                        .onLeft { error ->
                            if (error is DeletePermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.remove.entityNotFound",
                                        Component.text(idOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is DeletePermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.remove.unexpectedError",
                                        Component.text(idOrValue),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permission.remove.success",
                                    Component.text(idOrValue)
                                )
                            )
                        }
                }
            }
        }

        literal("modify") {
            string("idOrValue") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPermissions(gradeway, remaining.lowercase())
                    }
                }

                literal("setValue") {
                    requires { hasPermission(it, "gradeway.permission.setValue") }

                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrValue = stringParam("idOrValue")
                            val value = stringParam("value")

                            gradeway.permissions.updatePermissionValue(idOrValue, value)
                                .onLeft { error ->
                                    if (error is UpdatePermissionValueError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.entityNotFound",
                                                Component.text(idOrValue)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionValueError.ValueAlreadySet) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.valueAlreadySet",
                                                Component.text(idOrValue),
                                                Component.text(value)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionValueError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.unexpectedError",
                                                Component.text(idOrValue),
                                                Component.text(value),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.permission.setValue.success",
                                            Component.text(idOrValue),
                                            Component.text(value)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setType") {
                    requires { hasPermission(it, "gradeway.permission.setType") }

                    permissionType("type") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrValue = stringParam("idOrValue")
                            val rawType = stringParam("type").lowercase()

                            val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType }
                            if (type == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.setType.typeNotFound",
                                        Component.text(idOrValue),
                                        Component.text(rawType)
                                    )
                                )
                                return@execute
                            }

                            gradeway.permissions.updatePermissionType(idOrValue, type)
                                .onLeft { error ->
                                    if (error is UpdatePermissionTypeError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setType.entityNotFound",
                                                Component.text(idOrValue)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionTypeError.TypeAlreadySet) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setType.typeAlreadySet",
                                                Component.text(idOrValue),
                                                Component.text(type.name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionTypeError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permission.setType.unexpectedError",
                                                Component.text(idOrValue),
                                                Component.text(type.name),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.permission.setType.success",
                                            Component.text(idOrValue),
                                            Component.text(type.name)
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        createGlobalListHandler(
            gradeway = gradeway,
            permission = "gradeway.permission.list",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { page, limit ->
                PermissionsTable
                    .select(PermissionsTable.id, PermissionsTable.value, PermissionsTable.type)
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[PermissionsTable.id].value
                            val value = row[PermissionsTable.value]
                            val type = row[PermissionsTable.type]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(Component.translatable("gradeway.command.permission.list.empty"))
                    return@createGlobalListHandler
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.permission.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { permission ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permission.list.entry",
                            Component.text(permission.id.toString()),
                            Component.text(permission.value),
                            Component.text(permission.type.name)
                        )
                    )
                }
            }
        )

        permissionTemplateBuilder(gradeway, hasPermission, sourceToAudience)
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.permissionTemplateBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("template") {
        requires { hasPermission(it, "gradeway.permissionTemplate") }

        literal("create") {
            requires { hasPermission(it, "gradeway.permissionTemplate.create") }

            string("name") {
                execute {
                    val audience = sourceToAudience(source)

                    val name = stringParam("name")

                    gradeway.permissions.createTemplate(name)
                        .onLeft { error ->
                            if (error is CreateTemplateError.InvalidName) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.create.invalidName",
                                        Component.text(name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateTemplateError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.create.unexpectedError",
                                        Component.text(name),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.create.success",
                                    Component.text(name)
                                )
                            )
                        }
                }
            }
        }

        literal("delete") {
            requires { hasPermission(it, "gradeway.permissionTemplate.delete") }

            string("id") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val id = stringParam("id")

                    val uniqueId = runCatching {
                        UUID.fromString(id)
                    }.getOrNull()

                    if (uniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.permissionTemplate.delete.invalidUuid",
                                Component.text(id)
                            )
                        )
                        return@execute
                    }

                    gradeway.permissions.deleteTemplate(uniqueId)
                        .onLeft { error ->
                            if (error is DeleteTemplateError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.delete.entityNotFound",
                                        Component.text(id)
                                    )
                                )
                                return@execute
                            }
                            if (error is DeleteTemplateError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.delete.unexpectedError",
                                        Component.text(id),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.delete.success",
                                    Component.text(id),
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
                        builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                    }
                }

                literal("setName") {
                    requires { hasPermission(it, "gradeway.permissionTemplate.setName") }

                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val value = stringParam("value")

                            gradeway.permissions.setTemplateName(idOrName, value)
                                .onLeft { error ->
                                    if (error is SetNameTemplateError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.entityNotFound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.InvalidName) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.invalidName",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.NameAlreadySet) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.nameAlreadySet",
                                                Component.text(idOrName),
                                                Component.text(value)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(value),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.permissionTemplate.setName.success",
                                            Component.text(idOrName),
                                            Component.text(value)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setAssignedTo") {
                    requires { hasPermission(it, "gradeway.permissionTemplate.setAssignedTo") }

                    permissionTemplateAssignedTo("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val rawAssignedTo = stringParam("value").lowercase()

                            val assignedTo = PermissionTemplateEntity.AssignedTo.entries.find {
                                it.name.lowercase() == rawAssignedTo
                            }

                            if (assignedTo == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setAssignedTo.invalidAssignedTo",
                                        Component.text(idOrName),
                                        Component.text(rawAssignedTo)
                                    )
                                )
                                return@execute
                            }

                            gradeway.permissions.setTemplateAssignedTo(idOrName, assignedTo)
                                .onLeft { error ->
                                    if (error is SetAssignedToTemplateError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.entityNotFound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetAssignedToTemplateError.AlreadyAssignedTo) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.alreadyAssignedTo",
                                                Component.text(idOrName),
                                                Component.text(rawAssignedTo)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetAssignedToTemplateError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(rawAssignedTo),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.permissionTemplate.setAssignedTo.success",
                                            Component.text(idOrName),
                                            Component.text(assignedTo.name)
                                        )
                                    )
                                }
                        }
                    }
                }

                permissionTemplatePermissionsBuilder(gradeway, hasPermission, sourceToAudience)
            }
        }

        createGlobalListHandler(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.list",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { page, limit ->
                PermissionTemplatesTable
                    .select(PermissionTemplatesTable.id, PermissionTemplatesTable.name)
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[PermissionTemplatesTable.id].value
                            val name = row[PermissionTemplatesTable.name]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(Component.translatable("gradeway.command.permissionTemplate.list.empty"))
                    return@createGlobalListHandler
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.permissionTemplate.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { permissionTemplate ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permissionTemplate.list.entry",
                            Component.text(permissionTemplate.id.toString()),
                            Component.text(permissionTemplate.name)
                        )
                    )
                }
            }
        )
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.permissionTemplatePermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "gradeway.permissionTemplate.permissions") }

        literal("add") {
            requires { hasPermission(it, "gradeway.permissionTemplate.permissions.add") }

            string("permissionIdOrValue") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPermissions(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permissionIdOrValue = stringParam("permissionIdOrValue")

                    gradeway.permissions.addPermissionToTemplate(idOrName, permissionIdOrValue)
                        .onLeft { error ->
                            if (error is AddPermissionToTemplateError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.PermissionAlreadyExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.alreadyExists",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.addPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                        }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.permissionTemplate.permissions.remove") }

            string("permissionIdOrValue") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPermissions(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permissionIdOrValue = stringParam("permissionIdOrValue")

                    gradeway.permissions.removePermissionFromTemplate(idOrName, permissionIdOrValue)
                        .onLeft { error ->
                            if (error is RemovePermissionFromTemplateError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.PermissionNotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.notExists",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permissionIdOrValue),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.removePermission.success",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { hasPermission(it, "gradeway.permissionTemplate.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.permissions.clearPermissionsFromTemplate(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionsFromTemplateError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.clearPermissions.entityNotFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@execute
                        }
                        if (error is ClearPermissionsFromTemplateError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.clearPermissions.unexpectedError",
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
                                "gradeway.command.permissionTemplate.clearPermissions.success",
                                Component.text(idOrName)
                            )
                        )
                    }
            }
        }

        createScopedListHandler(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.permissions.list",
            scopeKey = "idOrName",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { scope, page, limit ->
                PermissionTemplatePermissionsTable
                    .innerJoin(PermissionTemplatesTable, { templateId }, { id })
                    .innerJoin(PermissionsTable, { PermissionTemplatePermissionsTable.permissionId }, { id })
                    .select(PermissionsTable.id, PermissionsTable.value, PermissionsTable.type)
                    .where {
                        (PermissionTemplatesTable.id likeAsStr "$scope%") or
                                (PermissionTemplatesTable.name.lowerCase() like
                                        "${scope.toString().lowercase()}%")
                    }
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[PermissionsTable.id].value
                            val value = row[PermissionsTable.value]
                            val type = row[PermissionsTable.type]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(
                        Component.translatable("gradeway.command.permissionTemplate.permissions.list.empty")
                    )
                    return@createScopedListHandler
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.permissionTemplate.permissions.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { permission ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permissionTemplate.permissions.list.entry",
                            Component.text(permission.id.toString()),
                            Component.text(permission.value),
                            Component.text(permission.type.name)
                        )
                    )
                }
            }
        )
    }
}
