/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatePermissionsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.kotlin.extension.suggestionProvider
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.SuggestionProvider
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

internal fun <C : Any> MutableCommandBuilder<C>.registerPermissionCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
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

    registerCopy("permission") {
        registerCopy("add") {
            permission("gradeway.permission.add")

            required("value", stringParser())
            optional("type", stringParser()) {
                suggestionProvider = SuggestionProvider.blockingStrings { _, _ ->
                    PermissionEntity.Type.entries.map { it.name }
                }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val value = context.get<String>("value")
                val rawType = context.optional<String>("type").map { it.lowercase() }

                if (rawType.isEmpty) {
                    handleAddPermission(audience, value, PermissionEntity.Type.EQUALS)
                    return@handler
                }

                val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType.get() }
                if (type == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permission.add.invalidType",
                            Component.text(rawType.get())
                        )
                    )
                    return@handler
                }

                handleAddPermission(audience, value, type)
            }
        }

        registerCopy("remove") {
            permission("gradeway.permission.remove")

            required("idOrValue", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrValue = context.get<String>("idOrValue")

                gradeway.permissions.deletePermission(idOrValue)
                    .onLeft { error ->
                        if (error is DeletePermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permission.remove.entityNotFound",
                                    Component.text(idOrValue)
                                )
                            )
                            return@handler
                        }
                        if (error is DeletePermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permission.remove.unexpectedError",
                                    Component.text(idOrValue),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
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

        registerCopy("modify") {
            required("idOrValue", stringParser()) {
                suggests { remaining -> suggestPermissions(gradeway, remaining.lowercase()) }
            }

            registerCopy("setValue") {
                permission("gradeway.permission.setValue")

                required("value", stringParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrValue = context.get<String>("idOrValue")
                    val value = context.get<String>("value")

                    gradeway.permissions.updatePermissionValue(idOrValue, value)
                        .onLeft { error ->
                            if (error is UpdatePermissionValueError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.setValue.entityNotFound",
                                        Component.text(idOrValue)
                                    )
                                )
                                return@handler
                            }
                            if (error is UpdatePermissionValueError.ValueAlreadySet) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.setValue.valueAlreadySet",
                                        Component.text(idOrValue),
                                        Component.text(value)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerCopy("setType") {
                permission("gradeway.permission.setType")

                required("type", stringParser()) {
                    suggestionProvider = SuggestionProvider.blockingStrings { _, _ ->
                        PermissionEntity.Type.entries.map { it.name }
                    }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrValue = context.get<String>("idOrValue")
                    val rawType = context.get<String>("type").lowercase()

                    val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType }
                    if (type == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.permission.setType.typeNotFound",
                                Component.text(idOrValue),
                                Component.text(rawType)
                            )
                        )
                        return@handler
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
                                return@handler
                            }
                            if (error is UpdatePermissionTypeError.TypeAlreadySet) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permission.setType.typeAlreadySet",
                                        Component.text(idOrValue),
                                        Component.text(type.name)
                                    )
                                )
                                return@handler
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
                                return@handler
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

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.permission.list",
            audienceProvider = audienceProvider,
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
                    return@registerGlobalListCommand
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

        registerPermissionTemplateCommand(gradeway, audienceProvider)
    }
}

internal fun <C : Any> MutableCommandBuilder<C>.registerPermissionTemplateCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("template") {
        registerCopy("create") {
            permission("gradeway.permissionTemplate.create")

            required("name", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val name = context.get<String>("name")

                gradeway.permissions.createTemplate(name)
                    .onLeft { error ->
                        if (error is CreateTemplateError.InvalidName) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.create.invalidName",
                                    Component.text(name)
                                )
                            )
                            return@handler
                        }
                        if (error is CreateTemplateError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.create.unexpectedError",
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
                                "gradeway.command.permissionTemplate.create.success",
                                Component.text(name)
                            )
                        )
                    }
            }
        }

        registerCopy("delete") {
            permission("gradeway.permissionTemplate.delete")

            required("id", stringParser()) {
                suggests { remaining -> suggestPermissionTemplates(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val id = context.get<String>("id")

                val uniqueId = runCatching { UUID.fromString(id) }.getOrNull()

                if (uniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.permissionTemplate.delete.invalidUuid",
                            Component.text(id)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is DeleteTemplateError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.delete.unexpectedError",
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
                                "gradeway.command.permissionTemplate.delete.success",
                                Component.text(id),
                            )
                        )
                    }
            }
        }

        registerCopy("modify") {
            required("idOrName", stringParser()) {
                suggests { remaining -> suggestPermissionTemplates(gradeway, remaining.lowercase()) }
            }

            registerCopy("setName") {
                permission("gradeway.permissionTemplate.setName")

                required("value", stringParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val value = context.get<String>("value")

                    gradeway.permissions.setTemplateName(idOrName, value)
                        .onLeft { error ->
                            if (error is SetNameTemplateError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setName.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is SetNameTemplateError.InvalidName) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setName.invalidName",
                                        Component.text(idOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is SetNameTemplateError.NameAlreadySet) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setName.nameAlreadySet",
                                        Component.text(idOrName),
                                        Component.text(value)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerCopy("setAssignedTo") {
                permission("gradeway.permissionTemplate.setAssignedTo")

                required("value", stringParser()) {
                    suggestionProvider = SuggestionProvider.blockingStrings { _, _ ->
                        PermissionTemplateEntity.AssignedTo.entries.map { it.name }
                    }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val rawAssignedTo = context.get<String>("value").lowercase()

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
                        return@handler
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
                                return@handler
                            }
                            if (error is SetAssignedToTemplateError.AlreadyAssignedTo) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setAssignedTo.alreadyAssignedTo",
                                        Component.text(idOrName),
                                        Component.text(rawAssignedTo)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerPermissionTemplatePermissionsCommand(gradeway, audienceProvider)
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.list",
            audienceProvider = audienceProvider,
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
                    return@registerGlobalListCommand
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

internal fun <C : Any> MutableCommandBuilder<C>.registerPermissionTemplatePermissionsCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("permissions") {
        registerCopy("add") {
            permission("gradeway.permissionTemplate.permissions.add")

            required("permissionIdOrValue", stringParser()) {
                suggests { remaining -> suggestPermissions(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val permissionIdOrValue = context.get<String>("permissionIdOrValue")

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
                            return@handler
                        }
                        if (error is AddPermissionToTemplateError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.addPermission.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                            return@handler
                        }
                        if (error is AddPermissionToTemplateError.PermissionAlreadyExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.addPermission.alreadyExists",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("remove") {
            permission("gradeway.permissionTemplate.permissions.remove")

            required("permissionIdOrValue", stringParser()) {
                suggests { remaining -> suggestPermissions(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val permissionIdOrValue = context.get<String>("permissionIdOrValue")

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
                            return@handler
                        }
                        if (error is RemovePermissionFromTemplateError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.removePermission.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                            return@handler
                        }
                        if (error is RemovePermissionFromTemplateError.PermissionNotExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.removePermission.notExists",
                                    Component.text(idOrName),
                                    Component.text(permissionIdOrValue)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("clear") {
            permission("gradeway.permissionTemplate.permissions.clear")

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                gradeway.permissions.clearPermissionsFromTemplate(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionsFromTemplateError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.clearPermissions.entityNotFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@handler
                        }
                        if (error is ClearPermissionsFromTemplateError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.clearPermissions.unexpectedError",
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
                                "gradeway.command.permissionTemplate.clearPermissions.success",
                                Component.text(idOrName)
                            )
                        )
                    }
            }
        }

        registerScopedListCommand(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.permissions.list",
            scopeKey = "idOrName",
            audienceProvider = audienceProvider,
            query = { scope, page, limit ->
                PermissionTemplatePermissionsTable
                    .innerJoin(PermissionTemplatesTable, { templateId }, { id })
                    .innerJoin(PermissionsTable, { PermissionTemplatePermissionsTable.permissionId }, { id })
                    .select(PermissionsTable.id, PermissionsTable.value, PermissionsTable.type)
                    .where {
                        (PermissionTemplatesTable.id likeAsStr "$scope%") or
                                (PermissionTemplatesTable.name.lowerCase() like
                                        "${scope.lowercase()}%")
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
                    return@registerScopedListCommand
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
