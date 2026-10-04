/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import arrow.core.Either
import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.attribute.AttributeType
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import dev.gradienttim.gradeway.entity.role.RoleParentEntity
import dev.gradienttim.gradeway.managers.ConfirmationManager
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import dev.gradienttim.gradeway.services.AttributeService.*
import dev.gradienttim.gradeway.services.PermissionService.*
import dev.gradienttim.gradeway.services.RoleService.AddParentError
import dev.gradienttim.gradeway.services.RoleService.RemoveParentError
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

internal fun <TCommandSource> requestConfirmation(
    source: TCommandSource,
    context: CommandContext<TCommandSource>,
    gradeway: CommonGradeway<*>,
    rootLiteral: String,
    handler: () -> Unit,
) {
    gradeway.confirmations.request(
        sender = context.sourceToUUID(source),
        handler = handler,
        onTimeout = { jobId ->
            context.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.confirmation.timeout",
                    Argument.string("job", jobId)
                )
            )
        }
    ).onLeft { error ->
        if (error is ConfirmationManager.RequestJobError.FailedToRegister) {
            context.sendTranslatedMessage(
                source,
                Component.translatable("gradeway.confirmation.request.failedToRegister")
            )
            return
        }
        if (error is ConfirmationManager.RequestJobError.Unexpected) {
            context.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.confirmation.request.unexpectedError",
                    Argument.string("error", error.throwable.message ?: "Unknown")
                )
            )
            return
        }
    }.onRight { jobId ->
        context.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.confirmation.request.success",
                Argument.string("command", rootLiteral),
                Argument.string("job", jobId)
            )
        )
    }
}

internal fun <TCommandSource, TResult> ArgumentBuilder<TCommandSource, *>.registerGlobalListCommand(
    context: CommandContext<TCommandSource>,
    gradeway: CommonGradeway<*>,
    permission: String,
    query: (page: Int, limit: Int) -> TResult,
    render: (source: TCommandSource, page: Int, limit: Int, result: TResult) -> Unit,
) {
    literal("list") {
        requires { context.hasPermission(it, permission) }

        execute {
            val page = 1
            val limit = 10

            val result = transaction(gradeway.database) { query(page, limit) }
            render(source, page, limit, result)
        }

        integer("page") {
            execute {
                val page = intParam("page")
                val limit = 10

                val result = transaction(gradeway.database) { query(page, limit) }
                render(source, page, limit, result)
            }

            integer("limit") {
                execute {
                    val page = intParam("page")
                    val limit = intParam("limit")

                    val result = transaction(gradeway.database) { query(page, limit) }
                    render(source, page, limit, result)
                }
            }
        }
    }
}

internal fun <TCommandSource, TResult> ArgumentBuilder<TCommandSource, *>.registerWeightedListCommand(
    context: CommandContext<TCommandSource>,
    gradeway: CommonGradeway<*>,
    permission: String,
    query: (page: Int, limit: Int, order: SortOrder) -> TResult,
    render: (source: TCommandSource, page: Int, limit: Int, result: TResult) -> Unit,
) {
    literal("list") {
        requires { context.hasPermission(it, permission) }

        execute {
            val result = transaction(gradeway.database) { query(1, DEFAULT_LIST_LIMIT, SortOrder.DESC) }
            render(source, 1, DEFAULT_LIST_LIMIT, result)
        }

        integer("page") {
            execute {
                val page = intParam("page")

                val result = transaction(gradeway.database) { query(page, DEFAULT_LIST_LIMIT, SortOrder.DESC) }
                render(source, page, DEFAULT_LIST_LIMIT, result)
            }

            integer("limit") {
                execute {
                    val page = intParam("page")
                    val limit = intParam("limit")

                    val result = transaction(gradeway.database) { query(page, limit, SortOrder.DESC) }
                    render(source, page, limit, result)
                }

                string("order") {
                    suggestStrings { SORT_ORDERS.keys }

                    execute {
                        val page = intParam("page")
                        val limit = intParam("limit")
                        val orderName = stringParam("order")

                        val order = SORT_ORDERS[orderName.lowercase()]
                        if (order == null) {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.list.invalidOrder",
                                    Argument.string("order", orderName)
                                )
                            )
                            return@execute
                        }

                        val result = transaction(gradeway.database) { query(page, limit, order) }
                        render(source, page, limit, result)
                    }
                }
            }
        }
    }
}

private const val DEFAULT_LIST_LIMIT = 10
private val SORT_ORDERS = mapOf("asc" to SortOrder.ASC, "desc" to SortOrder.DESC)

internal fun <TCommandSource, TResult> ArgumentBuilder<TCommandSource, *>.registerScopedListCommand(
    context: CommandContext<TCommandSource>,
    gradeway: CommonGradeway<*>,
    permission: String,
    scopeKey: String,
    query: (scope: String, page: Int, limit: Int) -> TResult,
    render: (source: TCommandSource, page: Int, limit: Int, result: TResult) -> Unit,
) {
    literal("list") {
        requires { context.hasPermission(it, permission) }

        execute {
            val scope = stringParam(scopeKey)
            val page = 1
            val limit = 10

            val result = transaction(gradeway.database) { query(scope, page, limit) }
            render(source, page, limit, result)
        }

        integer("page") {
            execute {
                val scope = stringParam(scopeKey)
                val page = intParam("page")
                val limit = 10

                val result = transaction(gradeway.database) { query(scope, page, limit) }
                render(source, page, limit, result)
            }

            integer("limit") {
                execute {
                    val scope = stringParam(scopeKey)
                    val page = intParam("page")
                    val limit = intParam("limit")

                    val result = transaction(gradeway.database) { query(scope, page, limit) }
                    render(source, page, limit, result)
                }
            }
        }
    }
}

internal fun <TCommandSource, TListResult> ArgumentBuilder<TCommandSource, *>.registerRoleRelationCommands(
    context: CommandContext<TCommandSource>,
    gradeway: CommonGradeway<*>,
    literalName: String,
    relationKey: String,
    targetKey: String,
    handleAdd: (idOrName: String, targetId: UUID) -> Either<AddParentError, RoleParentEntity>,
    handleRemove: (idOrName: String, targetId: UUID) -> Either<RemoveParentError, Unit>,
    handleListQuery: (scope: String, page: Int, limit: Int) -> TListResult,
    handleListRender: (source: TCommandSource, page: Int, limit: Int, result: TListResult) -> Unit
) {
    literal(literalName) {
        literal("add") {
            requires { context.hasPermission(it, "gradeway.role.$literalName.add") }

            string(targetKey) {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val targetId = stringParam(targetKey)

                    val targetUniqueId = gradeway.roles.findByIdOrName(targetId)?.id?.value

                    if (targetUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.add$relationKey.targetNotFound",
                                Argument.string("role", idOrName),
                                Argument.string("target", targetId)
                            )
                        )
                        return@execute
                    }

                    handleAdd(idOrName, targetUniqueId)
                        .onLeft { error ->
                            if (error is AddParentError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.entityNotFound",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddParentError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.targetNotFound",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddParentError.SelfReference) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.selfReference",
                                        Argument.string("role", idOrName)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddParentError.AlreadyParent) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.alreadyParent",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddParentError.CyclicRelation) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.cyclicRelation",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is AddParentError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.add$relationKey.unexpectedError",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId),
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
                                    "gradeway.command.role.add$relationKey.success",
                                    Argument.string("role", idOrName),
                                    Argument.string("target", targetId)
                                )
                            )
                        }
                }
            }
        }

        literal("remove") {
            requires { context.hasPermission(it, "gradeway.role.$literalName.remove") }

            string(targetKey) {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val targetId = stringParam(targetKey)

                    val targetUniqueId = gradeway.roles.findByIdOrName(targetId)?.id?.value

                    if (targetUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.role.remove$relationKey.targetNotFound",
                                Argument.string("role", idOrName),
                                Argument.string("target", targetId)
                            )
                        )
                        return@execute
                    }

                    handleRemove(idOrName, targetUniqueId)
                        .onLeft { error ->
                            if (error is RemoveParentError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.remove$relationKey.entityNotFound",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemoveParentError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.remove$relationKey.targetNotFound",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemoveParentError.NotParent) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.remove$relationKey.notParent",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId)
                                    )
                                )
                                return@execute
                            }
                            if (error is RemoveParentError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.role.remove$relationKey.unexpectedError",
                                        Argument.string("role", idOrName),
                                        Argument.string("target", targetId),
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
                                    "gradeway.command.role.remove$relationKey.success",
                                    Argument.string("role", idOrName),
                                    Argument.string("target", targetId)
                                )
                            )
                        }
                }
            }
        }

        registerScopedListCommand(
            context = context,
            gradeway = gradeway,
            permission = "gradeway.role.$literalName.list",
            scopeKey = "idOrName",
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { source, page, limit, result -> handleListRender(source, page, limit, result) }
        )
    }
}

internal fun <TCommandSource, TListResult> ArgumentBuilder<TCommandSource, *>.registerEntityPermissionCommands(
    context: CommandContext<TCommandSource>,
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    entityType: String,
    handleSetPermission: (idOrName: String, permission: String, status: Boolean) -> Either<SetPermissionError, Unit>,
    handleUnsetPermission: (idOrName: String, permission: String) -> Either<UnsetPermissionError, Unit>,
    handleClearPermissions: (idOrName: String) -> Either<ClearPermissionsError, Unit>,
    handleLinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<LinkTemplateError, Unit>,
    handleUnlinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<UnlinkTemplateError, Unit>,
    handleApplyTemplate: (idOrName: String, templateIdOrName: String) -> Either<ApplyTemplateError, Boolean>,
    handleRevokeTemplate: (idOrName: String, templateIdOrName: String) -> Either<RevokeTemplateError, Boolean>,
    handleListQuery: (scope: String, page: Int, limit: Int) -> TListResult,
    handleListRender: (source: TCommandSource, page: Int, limit: Int, result: TListResult) -> Unit
) {
    literal("permissions") {
        literal("set") {
            requires { context.hasPermission(it, "gradeway.$entityType.permissions.set") }

            string("permission") {
                execute {
                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    handleSetPermission(idOrName, permission, true)
                        .onLeft { error ->
                            renderSetPermissionError(
                                source,
                                context,
                                entityType,
                                idOrName,
                                permission,
                                error
                            )
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.$entityType.setPermission.success",
                                    Argument.string("entity", idOrName),
                                    Argument.string("permission", permission)
                                ),
                            )
                        }
                }

                boolean("status") {
                    execute {
                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = param("status", Boolean::class)

                        handleSetPermission(idOrName, permission, status)
                            .onLeft { error ->
                                renderSetPermissionError(source, context, entityType, idOrName, permission, error)
                            }
                            .onRight {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("permission", permission)
                                    ),
                                )
                            }
                    }
                }
            }
        }

        literal("unset") {
            requires { context.hasPermission(it, "gradeway.$entityType.permissions.unset") }

            string("permission") {
                execute {
                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    handleUnsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.unsetPermission.entityNotFound",
                                        Argument.string("entity", idOrName),
                                        Argument.string("permission", permission)
                                    )
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.permissionNotFound",
                                        Argument.string("entity", idOrName),
                                        Argument.string("permission", permission)
                                    )
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.unsetPermission.unexpectedError",
                                        Argument.string("entity", idOrName),
                                        Argument.string("permission", permission),
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
                                    "gradeway.command.$entityType.unsetPermission.success",
                                    Argument.string("entity", idOrName),
                                    Argument.string("permission", permission)
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { context.hasPermission(it, "gradeway.$entityType.permissions.clear") }

            execute {
                val idOrName = stringParam("idOrName")

                requestConfirmation(source, context, gradeway, rootLiteral) {
                    handleClearPermissions(idOrName)
                        .onLeft { error ->
                            if (error is ClearPermissionsError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.clearPermission.entityNotFound",
                                        Argument.string("entity", idOrName),
                                    ),
                                )
                                return@requestConfirmation
                            }
                            if (error is ClearPermissionsError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.clearPermissions.unexpectedError",
                                        Argument.string("entity", idOrName),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    ),
                                )
                                return@requestConfirmation
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.$entityType.clearPermissions.success",
                                    Argument.string("entity", idOrName),
                                ),
                            )
                        }
                }
            }
        }

        registerScopedListCommand(
            context = context,
            gradeway = gradeway,
            permission = "gradeway.$entityType.permissions.list",
            scopeKey = "idOrName",
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { source, page, limit, result -> handleListRender(source, page, limit, result) }
        )

        literal("template") {
            literal("link") {
                requires { context.hasPermission(it, "gradeway.$entityType.permissionTemplate.link") }

                string("templateIdOrName") {
                    suggestPermissionTemplates(gradeway)

                    execute {
                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleLinkTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is LinkTemplateError.TargetNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.entityNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.TemplateNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.templateNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.AlreadyLinked) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.alreadyLinked",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.WrongAssignedTo) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.wrongAssignedTo",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.unexpectedError",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName),
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
                                        "gradeway.command.$entityType.linkTemplate.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("template", templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("unlink") {
                requires { context.hasPermission(it, "gradeway.$entityType.permissionTemplate.unlink") }

                string("templateIdOrName") {
                    suggestPermissionTemplates(gradeway)

                    execute {
                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleUnlinkTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is UnlinkTemplateError.TargetNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.entityNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.TemplateNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.templateNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.NotLinked) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.notLinked",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.unexpectedError",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName),
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
                                        "gradeway.command.$entityType.unlinkTemplate.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("template", templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("apply") {
                requires { context.hasPermission(it, "gradeway.$entityType.permissionTemplate.apply") }

                string("templateIdOrName") {
                    suggestPermissionTemplates(gradeway)

                    execute {
                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleApplyTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is ApplyTemplateError.TargetNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.entityNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.TemplateNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.templateNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.WrongAssignedTo) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.wrongAssignedTo",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.unexpectedError",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName),
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
                                        "gradeway.command.$entityType.applyTemplate.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("template", templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("revoke") {
                requires { context.hasPermission(it, "gradeway.$entityType.permissionTemplate.revoke") }

                string("templateIdOrName") {
                    suggestPermissionTemplates(gradeway)

                    execute {
                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleRevokeTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is RevokeTemplateError.TargetNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.entityNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is RevokeTemplateError.TemplateNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.templateNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is RevokeTemplateError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.unexpectedError",
                                            Argument.string("entity", idOrName),
                                            Argument.string("template", templateIdOrName),
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
                                        "gradeway.command.$entityType.revokeTemplate.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("template", templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}

private fun <TCommandSource> renderSetPermissionError(
    source: TCommandSource,
    context: CommandContext<TCommandSource>,
    entityType: String,
    idOrName: String,
    permission: String,
    error: SetPermissionError,
) {
    if (error is SetPermissionError.EntityNotFound) {
        context.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.$entityType.setPermission.entityNotFound",
                Argument.string("entity", idOrName),
                Argument.string("permission", permission)
            ),
        )
        return
    }
    if (error is SetPermissionError.PermissionAlreadyEnabled) {
        context.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.$entityType.setPermission.alreadyEnabled",
                Argument.string("entity", idOrName),
                Argument.string("permission", permission)
            ),
        )
        return
    }
    if (error is SetPermissionError.PermissionAlreadyDisabled) {
        context.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.$entityType.setPermission.alreadyDisabled",
                Argument.string("entity", idOrName),
                Argument.string("permission", permission)
            ),
        )
        return
    }
    if (error is SetPermissionError.Unexpected) {
        context.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.$entityType.setPermission.unexpectedError",
                Argument.string("entity", idOrName),
                Argument.string("permission", permission),
                Argument.string("error", error.throwable.message ?: "Unknown")
            ),
        )
        return
    }
}

internal fun <TCommandSource, TListResult> ArgumentBuilder<TCommandSource, *>.registerEntityAttributeCommands(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    entityType: String,
    context: CommandContext<TCommandSource>,
    handleAddAttribute: (idOrName: String, attribute: Attribute<Any>) -> Either<AddAttributeError, SharedAttributeEntity>,
    handleUpdateAttribute: (idOrName: String, key: Key, value: Any) -> Either<UpdateAttributeError, SharedAttributeEntity>,
    handleRemoveAttribute: (idOrName: String, key: Key) -> Either<RemoveAttributeError, Unit>,
    handleClearAttributes: (idOrName: String) -> Either<ClearAttributesError, Unit>,
    handleListQuery: (scope: String, page: Int, limit: Int) -> TListResult,
    handleListRender: (source: TCommandSource, page: Int, limit: Int, result: TListResult) -> Unit
) {
    literal("attributes") {
        literal("add") {
            requires { context.hasPermission(it, "gradeway.$entityType.attributes.add") }

            string("key") {
                string("type") {
                    suggestAttributeTypes()

                    string("value") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val key = Key.key(stringParam("key"))
                            val type = stringParam("type")
                            val value = stringParam("value")

                            @Suppress("UNCHECKED_CAST")
                            val attributeType = AttributeTypeRegistry.find(type) as? AttributeType<Any>
                            if (attributeType == null) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.addAttribute.attributeTypeNotRegistered",
                                        Argument.string("entity", idOrName),
                                        Argument.string("type", type)
                                    )
                                )
                                return@execute
                            }

                            val attributeValue = attributeType.deserialize(value) ?: attributeType.fallback(key)
                            val attribute = Attribute(attributeType, key, attributeValue)

                            handleAddAttribute(idOrName, attribute)
                                .onLeft { error ->
                                    if (error is AddAttributeError.EntityNotFound) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.entityNotFound",
                                                Argument.string("entity", idOrName),
                                                Argument.string("attribute", attribute.key.asString())
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is AddAttributeError.AttributeAlreadyExists) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.attributeAlreadyExists",
                                                Argument.string("entity", idOrName),
                                                Argument.string("attribute", attribute.key.asString())
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is AddAttributeError.Unexpected) {
                                        context.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.unexpectedError",
                                                Argument.string("entity", idOrName),
                                                Argument.string("attribute", attribute.key.asString()),
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
                                            "gradeway.command.$entityType.addAttribute.success",
                                            Argument.string("entity", idOrName),
                                            Argument.string("attribute", attribute.key.asString()),
                                            Argument.string("value", attribute.value.toString())
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        literal("update") {
            requires { context.hasPermission(it, "gradeway.$entityType.attributes.update") }

            string("key") {
                string("value") {
                    execute {
                        val idOrName = stringParam("idOrName")
                        val key = Key.key(stringParam("key"))
                        val value = stringParam("value")

                        handleUpdateAttribute(idOrName, key, value)
                            .onLeft { error ->
                                if (error is UpdateAttributeError.EntityNotFound) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.entityNotFound",
                                            Argument.string("entity", idOrName),
                                            Argument.string("attribute", key.asString())
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.AttributeNotExists) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.attributeNotExists",
                                            Argument.string("entity", idOrName),
                                            Argument.string("attribute", key.asString())
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.AttributeTypeNotRegistered) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.attributeTypeNotRegistered",
                                            Argument.string("entity", idOrName),
                                            Argument.string("type", error.type)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.Unexpected) {
                                    context.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.unexpectedError",
                                            Argument.string("entity", idOrName),
                                            Argument.string("attribute", key.asString()),
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight { attributeEntity ->
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.updateAttribute.success",
                                        Argument.string("entity", idOrName),
                                        Argument.string("attribute", key.asString()),
                                        Argument.string("value", attributeEntity.attribute.value.toString())
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("remove") {
            requires { context.hasPermission(it, "gradeway.$entityType.attributes.remove") }

            string("key") {
                execute {
                    val idOrName = stringParam("idOrName")
                    val key = Key.key(stringParam("key"))

                    handleRemoveAttribute(idOrName, key)
                        .onLeft { error ->
                            if (error is RemoveAttributeError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.entityNotFound",
                                        Argument.string("entity", idOrName),
                                        Argument.string("attribute", key.asString())
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.AttributeNotExists) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.attributeNotExists",
                                        Argument.string("entity", idOrName),
                                        Argument.string("attribute", key.asString())
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.unexpectedError",
                                        Argument.string("entity", idOrName),
                                        Argument.string("attribute", key.asString()),
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
                                    "gradeway.command.$entityType.removeAttribute.success",
                                    Argument.string("entity", idOrName),
                                    Argument.string("attribute", key.asString())
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { context.hasPermission(it, "gradeway.$entityType.attributes.clear") }

            execute {
                val idOrName = stringParam("idOrName")

                requestConfirmation(source, context, gradeway, rootLiteral) {
                    handleClearAttributes(idOrName)
                        .onLeft { error ->
                            if (error is ClearAttributesError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.clearAttributes.entityNotFound",
                                        Argument.string("entity", idOrName)
                                    )
                                )
                                return@requestConfirmation
                            }
                            if (error is ClearAttributesError.NoAttributesFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.clearAttributes.noAttributesFound",
                                        Argument.string("entity", idOrName)
                                    )
                                )
                                return@requestConfirmation
                            }
                            if (error is ClearAttributesError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.$entityType.clearAttributes.unexpectedError",
                                        Argument.string("entity", idOrName),
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
                                    "gradeway.command.$entityType.clearAttributes.success",
                                    Argument.string("entity", idOrName)
                                )
                            )
                        }
                }
            }
        }

        registerScopedListCommand(
            context = context,
            gradeway = gradeway,
            permission = "gradeway.$entityType.attributes.list",
            scopeKey = "idOrName",
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { audience, page, limit, result -> handleListRender(audience, page, limit, result) }
        )
    }
}

internal fun infoList(entries: List<Component>): Component =
    if (entries.isEmpty()) {
        Component.translatable("gradeway.command.info.noEntries")
    } else {
        Component.join(JoinConfiguration.newlines(), entries)
    }

internal fun infoListArguments(name: String, entries: List<Component>): Array<ComponentLike> = arrayOf(
    Argument.numeric("${name}_count", entries.size),
    Argument.component("${name}_list", infoList(entries))
)

internal fun infoBoolean(value: Boolean): Component =
    if (value) {
        Component.translatable("gradeway.command.info.yes")
    } else {
        Component.translatable("gradeway.command.info.no")
    }
