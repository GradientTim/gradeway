/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.command.string
import dev.gradienttim.gradeway.command.stringParam
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatePermissionsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.util.*

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.permissionCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    fun handleAddPermission(source: TCommandSource, value: String, type: PermissionEntity.Type) {
        gradeway.permissions.createPermission(value, type)
            .onLeft { error ->
                if (error is CreatePermissionError.AlreadyExists) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.permission.add.alreadyExists",
                            Argument.string("permission", value)
                        )
                    )
                    return
                }
                if (error is CreatePermissionError.Unexpected) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.permission.add.unexpectedError",
                            Argument.string("permission", value),
                            Argument.string("error", error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.permission.add.success",
                        Argument.string("permission", value),
                        Argument.string("type", type.name)
                    )
                )
            }
    }

    literal("permission") {
        requires { commandContext.hasPermission(it, "gradeway.permission") }

        literal("add") {
            requires { commandContext.hasPermission(it, "gradeway.permission.add") }

            string("value") {
                execute {
                    val value = stringParam("value")

                    handleAddPermission(source, value, PermissionEntity.Type.EQUALS)
                }

                string("type") {
                    suggestStrings { PermissionEntity.Type.entries.map { it.name } }

                    execute {
                        val value = stringParam("value")
                        val rawType = stringParam("type").lowercase()

                        val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType }
                        if (type == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.permission.add.invalidType",
                                    Argument.string("type", rawType)
                                )
                            )
                            return@execute
                        }

                        handleAddPermission(source, value, type)
                    }
                }
            }
        }

        literal("remove") {
            requires { commandContext.hasPermission(it, "gradeway.permission.remove") }

            string("idOrValue") {
                execute {
                    val idOrValue = stringParam("idOrValue")

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.permissions.deletePermission(idOrValue)
                            .onLeft { error ->
                                if (error is DeletePermissionError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.permission.remove.entityNotFound",
                                            Argument.string("permission", idOrValue)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is DeletePermissionError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.permission.remove.unexpectedError",
                                            Argument.string("permission", idOrValue),
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
                                        "gradeway.command.permission.remove.success",
                                        Argument.string("permission", idOrValue)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("modify") {
            string("idOrValue") {
                suggestPermissions(gradeway)

                literal("setValue") {
                    requires { commandContext.hasPermission(it, "gradeway.permission.setValue") }

                    string("value") {
                        execute {
                            val idOrValue = stringParam("idOrValue")
                            val value = stringParam("value")

                            gradeway.permissions.updatePermissionValue(idOrValue, value)
                                .onLeft { error ->
                                    if (error is UpdatePermissionValueError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.entityNotFound",
                                                Argument.string("permission", idOrValue)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionValueError.ValueAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.valueAlreadySet",
                                                Argument.string("permission", idOrValue),
                                                Argument.string("value", value)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionValueError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setValue.unexpectedError",
                                                Argument.string("permission", idOrValue),
                                                Argument.string("value", value),
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
                                            "gradeway.command.permission.setValue.success",
                                            Argument.string("permission", idOrValue),
                                            Argument.string("value", value)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setType") {
                    requires { commandContext.hasPermission(it, "gradeway.permission.setType") }

                    string("type") {
                        suggestStrings { PermissionEntity.Type.entries.map { it.name } }

                        execute {
                            val idOrValue = stringParam("idOrValue")
                            val rawType = stringParam("type").lowercase()

                            val type = PermissionEntity.Type.entries.find { it.name.lowercase() == rawType }
                            if (type == null) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permission.setType.typeNotFound",
                                        Argument.string("permission", idOrValue),
                                        Argument.string("type", rawType)
                                    )
                                )
                                return@execute
                            }

                            gradeway.permissions.updatePermissionType(idOrValue, type)
                                .onLeft { error ->
                                    if (error is UpdatePermissionTypeError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setType.entityNotFound",
                                                Argument.string("permission", idOrValue)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionTypeError.TypeAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setType.typeAlreadySet",
                                                Argument.string("permission", idOrValue),
                                                Argument.string("type", type.name)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is UpdatePermissionTypeError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permission.setType.unexpectedError",
                                                Argument.string("permission", idOrValue),
                                                Argument.string("type", type.name),
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
                                            "gradeway.command.permission.setType.success",
                                            Argument.string("permission", idOrValue),
                                            Argument.string("type", type.name)
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.permission.list",
            context = commandContext,
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
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.permission.list.empty")
                    )
                    return@registerGlobalListCommand
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.permission.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { permission ->
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.permission.list.entry",
                            permission.id.toIdArgument(),
                            Argument.string("permission", permission.value),
                            Argument.string("type", permission.type.name)
                        )
                    )
                }
            }
        )

        registerPermissionTemplateCommand(rootLiteral, gradeway, commandContext)
    }
}

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.registerPermissionTemplateCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    context: CommandContext<TCommandSource>,
) {
    literal("template") {
        requires { context.hasPermission(it, "gradeway.permissionTemplate") }

        literal("create") {
            requires { context.hasPermission(it, "gradeway.permissionTemplate.create") }

            string("name") {
                execute {
                    val name = stringParam("name")

                    gradeway.permissions.createTemplate(name)
                        .onLeft { error ->
                            if (error is CreateTemplateError.InvalidName) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.create.invalidName",
                                        Argument.string("template", name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateTemplateError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.create.unexpectedError",
                                        Argument.string("template", name),
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
                                    "gradeway.command.permissionTemplate.create.success",
                                    Argument.string("template", name)
                                )
                            )
                        }
                }
            }
        }

        literal("delete") {
            requires { context.hasPermission(it, "gradeway.permissionTemplate.delete") }

            string("idOrName") {
                suggestPermissionTemplates(gradeway)

                execute {
                    val id = stringParam("idOrName")

                    val template = gradeway.permissions.findTemplateByIdOrName(id)
                    if (template == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.permissionTemplate.delete.entityNotFound",
                                Argument.string("template", id)
                            )
                        )
                        return@execute
                    }

                    requestConfirmation(source, context, gradeway, rootLiteral) {
                        gradeway.permissions.deleteTemplate(template.id.value)
                            .onLeft { error ->
                                if (error is DeleteTemplateError.EntityNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.permissionTemplate.delete.entityNotFound",
                                            Argument.string("template", id)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is DeleteTemplateError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.permissionTemplate.delete.unexpectedError",
                                            Argument.string("template", id),
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@requestConfirmation
                                }
                            }
                            .onRight {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.delete.success",
                                        Argument.string("template", id),
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestPermissionTemplates(gradeway)

                literal("setName") {
                    requires { context.hasPermission(it, "gradeway.permissionTemplate.setName") }

                    string("value") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val value = stringParam("value")

                            gradeway.permissions.setTemplateName(idOrName, value)
                                .onLeft { error ->
                                    if (error is SetNameTemplateError.EntityNotFound) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.entityNotFound",
                                                Argument.string("template", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.InvalidName) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.invalidName",
                                                Argument.string("template", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.NameAlreadySet) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.nameAlreadySet",
                                                Argument.string("template", idOrName),
                                                Argument.string("name", value)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetNameTemplateError.Unexpected) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setName.unexpectedError",
                                                Argument.string("template", idOrName),
                                                Argument.string("name", value),
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
                                            "gradeway.command.permissionTemplate.setName.success",
                                            Argument.string("template", idOrName),
                                            Argument.string("name", value)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("setAssignedTo") {
                    requires { context.hasPermission(it, "gradeway.permissionTemplate.setAssignedTo") }

                    string("value") {
                        suggestStrings { PermissionTemplateEntity.AssignedTo.entries.map { it.name } }

                        execute {
                            val idOrName = stringParam("idOrName")
                            val rawAssignedTo = stringParam("value").lowercase()

                            val assignedTo = PermissionTemplateEntity.AssignedTo.entries.find {
                                it.name.lowercase() == rawAssignedTo
                            }

                            if (assignedTo == null) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.setAssignedTo.invalidAssignedTo",
                                        Argument.string("template", idOrName),
                                        Argument.string("assigned_to", rawAssignedTo)
                                    )
                                )
                                return@execute
                            }

                            gradeway.permissions.setTemplateAssignedTo(idOrName, assignedTo)
                                .onLeft { error ->
                                    if (error is SetAssignedToTemplateError.EntityNotFound) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.entityNotFound",
                                                Argument.string("template", idOrName)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetAssignedToTemplateError.AlreadyAssignedTo) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.alreadyAssignedTo",
                                                Argument.string("template", idOrName),
                                                Argument.string("assigned_to", rawAssignedTo)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetAssignedToTemplateError.Unexpected) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.permissionTemplate.setAssignedTo.unexpectedError",
                                                Argument.string("template", idOrName),
                                                Argument.string("assigned_to", rawAssignedTo),
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
                                            "gradeway.command.permissionTemplate.setAssignedTo.success",
                                            Argument.string("template", idOrName),
                                            Argument.string("assigned_to", assignedTo.name)
                                        )
                                    )
                                }
                        }
                    }
                }

                registerPermissionTemplatePermissionsCommand(rootLiteral, gradeway, context)
            }
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.list",
            context = context,
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
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.permissionTemplate.list.empty")
                    )
                    return@registerGlobalListCommand
                }

                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.permissionTemplate.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { permissionTemplate ->
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.permissionTemplate.list.entry",
                            permissionTemplate.id.toIdArgument(),
                            Argument.string("name", permissionTemplate.name)
                        )
                    )
                }
            }
        )
    }
}

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.registerPermissionTemplatePermissionsCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    context: CommandContext<TCommandSource>,
) {
    literal("permissions") {
        literal("add") {
            requires { context.hasPermission(it, "gradeway.permissionTemplate.permissions.add") }

            string("permissionIdOrValue") {
                suggestPermissions(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val permissionIdOrValue = stringParam("permissionIdOrValue")

                    gradeway.permissions.addPermissionToTemplate(idOrName, permissionIdOrValue)
                        .onLeft { error ->
                            if (error is AddPermissionToTemplateError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.entityNotFound",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.targetNotFound",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.PermissionAlreadyExists) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.alreadyExists",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddPermissionToTemplateError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.addPermission.unexpectedError",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue),
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
                                    "gradeway.command.permissionTemplate.addPermission.success",
                                    Argument.string("template", idOrName),
                                    Argument.string("permission", permissionIdOrValue)
                                )
                            )
                        }
                }
            }
        }

        literal("remove") {
            requires { context.hasPermission(it, "gradeway.permissionTemplate.permissions.remove") }

            string("permissionIdOrValue") {
                suggestPermissions(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val permissionIdOrValue = stringParam("permissionIdOrValue")

                    gradeway.permissions.removePermissionFromTemplate(idOrName, permissionIdOrValue)
                        .onLeft { error ->
                            if (error is RemovePermissionFromTemplateError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.entityNotFound",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.targetNotFound",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.PermissionNotExists) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.notExists",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemovePermissionFromTemplateError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.removePermission.unexpectedError",
                                        Argument.string("template", idOrName),
                                        Argument.string("permission", permissionIdOrValue),
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
                                    "gradeway.command.permissionTemplate.removePermission.success",
                                    Argument.string("template", idOrName),
                                    Argument.string("permission", permissionIdOrValue)
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { context.hasPermission(it, "gradeway.permissionTemplate.permissions.clear") }

            execute {
                val idOrName = stringParam("idOrName")

                requestConfirmation(source, context, gradeway, rootLiteral) {
                    gradeway.permissions.clearPermissionsFromTemplate(idOrName)
                        .onLeft { error ->
                            if (error is ClearPermissionsFromTemplateError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.clearPermissions.entityNotFound",
                                        Argument.string("template", idOrName)
                                    )
                                )
                                return@requestConfirmation
                            }
                            if (error is ClearPermissionsFromTemplateError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.permissionTemplate.clearPermissions.unexpectedError",
                                        Argument.string("template", idOrName),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@requestConfirmation
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.permissionTemplate.clearPermissions.success",
                                    Argument.string("template", idOrName)
                                )
                            )
                        }
                }
            }
        }

        registerScopedListCommand(
            gradeway = gradeway,
            permission = "gradeway.permissionTemplate.permissions.list",
            scopeKey = "idOrName",
            context = context,
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
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.permissionTemplate.permissions.list.empty")
                    )
                    return@registerScopedListCommand
                }

                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.permissionTemplate.permissions.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { permission ->
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.permissionTemplate.permissions.list.entry",
                            permission.id.toIdArgument(),
                            Argument.string("permission", permission.value),
                            Argument.string("type", permission.type.name)
                        )
                    )
                }
            }
        )
    }
}
