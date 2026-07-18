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
import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import dev.gradienttim.gradeway.services.AttributeService.*
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.KClass

internal fun <TSource, TResult> ArgumentBuilder<TSource, *>.createGlobalListHandler(
    gradeway: CommonGradeway,
    permission: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    query: (page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    fun handleList(audience: Audience, page: Int, limit: Int = 10) {
        val result = transaction(gradeway.database) {
            query(page, limit)
        }
        render(audience, page, limit, result)
    }

    literal("list") {
        requires { hasPermission(it, permission) }

        integer("page", min = 1) {
            integer("limit", min = 1) {
                execute {
                    val audience = sourceToAudience(source)
                    val page = intParam("page")
                    val limit = intParam("limit")
                    handleList(audience, page, limit)
                }
            }

            execute {
                val audience = sourceToAudience(source)
                val page = intParam("page")
                handleList(audience, page)
            }
        }

        execute {
            val audience = sourceToAudience(source)
            handleList(audience, 1)
        }
    }
}

internal fun <TSource, TResult> ArgumentBuilder<TSource, *>.createScopedListHandler(
    gradeway: CommonGradeway,
    permission: String,
    scopeKey: String,
    scopeType: KClass<*> = String::class,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    query: (scope: Any, page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    fun handleList(audience: Audience, scope: Any, page: Int, limit: Int = 10) {
        val result = transaction(gradeway.database) {
            query(scope, page, limit)
        }
        render(audience, page, limit, result)
    }

    literal("list") {
        requires { hasPermission(it, permission) }

        integer("page", min = 1) {
            integer("limit", min = 1) {
                execute {
                    val audience = sourceToAudience(source)
                    val scope = param(scopeKey, scopeType)
                    val page = intParam("page")
                    val limit = intParam("limit")
                    handleList(audience, scope, page, limit)
                }
            }

            execute {
                val audience = sourceToAudience(source)
                val scope = param(scopeKey, scopeType)
                val page = intParam("page")
                handleList(audience, scope, page)
            }
        }

        execute {
            val audience = sourceToAudience(source)
            val scope = param(scopeKey, scopeType)
            handleList(audience, scope, 1)
        }
    }
}

internal fun <TSource, TListResult> ArgumentBuilder<TSource, *>.createEntityPermissionHandler(
    gradeway: CommonGradeway,
    entityType: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    handleSetPermission: (idOrName: String, permission: String, status: Boolean) -> Either<SetPermissionError, Unit>,
    handleUnsetPermission: (idOrName: String, permission: String) -> Either<UnsetPermissionError, Unit>,
    handleClearPermissions: (idOrName: String) -> Either<ClearPermissionsError, Unit>,
    handleLinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<LinkTemplateError, Unit>,
    handleUnlinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<UnlinkTemplateError, Unit>,
    handleApplyTemplate: (idOrName: String, templateIdOrName: String) -> Either<ApplyTemplateError, Boolean>,
    handleRevokeTemplate: (idOrName: String, templateIdOrName: String) -> Either<RevokeTemplateError, Boolean>,
    handleListQuery: (scope: Any, page: Int, limit: Int) -> TListResult,
    handleListRender: (audience: Audience, page: Int, limit: Int, result: TListResult) -> Unit
) {
    literal("permissions") {
        requires { hasPermission(it, "gradeway.$entityType.permissions") }

        literal("set") {
            requires { hasPermission(it, "gradeway.$entityType.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        handleSetPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.setPermission.alreadyEnabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.setPermission.alreadyDisabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.setPermission.unexpectedError",
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
                                        "gradeway.command.$entityType.setPermission.success",
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

                    handleSetPermission(idOrName, permission, true)
                        .onLeft { error ->
                            if (error is SetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.alreadyEnabled",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.unexpectedError",
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
                                    "gradeway.command.$entityType.setPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("unset") {
            requires { hasPermission(it, "gradeway.$entityType.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    handleUnsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    )
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    )
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.unsetPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.unsetPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { hasPermission(it, "gradeway.$entityType.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                handleClearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionsError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearPermission.entityNotFound",
                                    Component.text(idOrName),
                                ),
                            )
                            return@execute
                        }
                        if (error is ClearPermissionsError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearPermissions.unexpectedError",
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
                                "gradeway.command.$entityType.clearPermissions.success",
                                Component.text(idOrName),
                            ),
                        )
                    }
            }
        }

        createScopedListHandler(
            gradeway = gradeway,
            permission = "gradeway.$entityType.permissions.list",
            scopeKey = "idOrName",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { audience, page, limit, result -> handleListRender(audience, page, limit, result) }
        )

        literal("template") {
            requires { hasPermission(it, "gradeway.$entityType.permissionTemplate") }

            literal("link") {
                requires { hasPermission(it, "gradeway.$entityType.permissionTemplate.link") }

                string("templateIdOrName") {
                    suggestsDebounced(gradeway) { builder ->
                        val remaining = builder.remaining
                        if (remaining.isNotEmpty()) {
                            builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                        }
                    }

                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleLinkTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is LinkTemplateError.TargetNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.TemplateNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.templateNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.AlreadyLinked) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.alreadyLinked",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.WrongAssignedTo) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.wrongAssignedTo",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is LinkTemplateError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.linkTemplate.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.linkTemplate.success",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("unlink") {
                requires { hasPermission(it, "gradeway.$entityType.permissionTemplate.unlink") }

                string("templateIdOrName") {
                    suggestsDebounced(gradeway) { builder ->
                        val remaining = builder.remaining
                        if (remaining.isNotEmpty()) {
                            builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                        }
                    }

                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleUnlinkTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is UnlinkTemplateError.TargetNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.TemplateNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.templateNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.NotLinked) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.notLinked",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UnlinkTemplateError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.unlinkTemplate.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.unlinkTemplate.success",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("apply") {
                requires { hasPermission(it, "gradeway.$entityType.permissionTemplate.apply") }

                string("templateIdOrName") {
                    suggestsDebounced(gradeway) { builder ->
                        val remaining = builder.remaining
                        if (remaining.isNotEmpty()) {
                            builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                        }
                    }

                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleApplyTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is ApplyTemplateError.TargetNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.TemplateNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.templateNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.WrongAssignedTo) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.wrongAssignedTo",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is ApplyTemplateError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.applyTemplate.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.applyTemplate.success",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }

            literal("revoke") {
                requires { hasPermission(it, "gradeway.$entityType.permissionTemplate.revoke") }

                string("templateIdOrName") {
                    suggestsDebounced(gradeway) { builder ->
                        val remaining = builder.remaining
                        if (remaining.isNotEmpty()) {
                            builder.suggestPermissionTemplates(gradeway, remaining.lowercase())
                        }
                    }

                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val templateIdOrName = stringParam("templateIdOrName")

                        handleRevokeTemplate(idOrName, templateIdOrName)
                            .onLeft { error ->
                                if (error is RevokeTemplateError.TargetNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is RevokeTemplateError.TemplateNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.templateNotFound",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is RevokeTemplateError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.revokeTemplate.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(templateIdOrName),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.revokeTemplate.success",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}

internal fun <TSource, TListResult> ArgumentBuilder<TSource, *>.createEntityAttributeHandler(
    gradeway: CommonGradeway,
    entityType: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    handleAddAttribute: (idOrName: String, attribute: Attribute<Any>) -> Either<AddAttributeError, SharedAttributeEntity>,
    handleUpdateAttribute: (idOrName: String, key: Key, value: Any) -> Either<UpdateAttributeError, SharedAttributeEntity>,
    handleRemoveAttribute: (idOrName: String, key: Key) -> Either<RemoveAttributeError, Unit>,
    handleClearAttributes: (idOrName: String) -> Either<ClearAttributesError, Unit>,
    handleListQuery: (scope: Any, page: Int, limit: Int) -> TListResult,
    handleListRender: (audience: Audience, page: Int, limit: Int, result: TListResult) -> Unit
) {
    literal("attributes") {
        requires { hasPermission(it, "gradeway.$entityType.attributes") }

        literal("add") {
            requires { hasPermission(it, "gradeway.$entityType.attributes.add") }

            string("key") {
                string("type") {
                    suggests { _, builder ->
                        builder.suggestAttributeTypes(builder.remainingLowerCase)
                        builder.buildFuture()
                    }

                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = Key.key(stringParam("key"))
                            val type = stringParam("type")
                            val value = stringParam("value")

                            @Suppress("UNCHECKED_CAST")
                            val attributeType = AttributeTypeRegistry.find(type) as? AttributeType<Any>
                            if (attributeType == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.addAttribute.attributeTypeNotRegistered",
                                        Component.text(idOrName),
                                        Component.text(type)
                                    )
                                )
                                return@execute
                            }

                            val attributeValue = attributeType.deserialize(value) ?: attributeType.fallback(key)
                            val attribute = Attribute(attributeType, key, attributeValue)

                            handleAddAttribute(idOrName, attribute)
                                .onLeft { error ->
                                    if (error is AddAttributeError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.entityNotFound",
                                                Component.text(idOrName),
                                                Component.text(attribute.key.asString())
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is AddAttributeError.AttributeAlreadyExists) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.attributeAlreadyExists",
                                                Component.text(idOrName),
                                                Component.text(attribute.key.asString())
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is AddAttributeError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.$entityType.addAttribute.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(attribute.key.asString()),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.addAttribute.success",
                                            Component.text(idOrName),
                                            Component.text(attribute.key.asString()),
                                            Component.text(attribute.value.toString())
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        literal("update") {
            requires { hasPermission(it, "gradeway.$entityType.attributes.update") }

            string("key") {
                string("value") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val key = Key.key(stringParam("key"))
                        val value = stringParam("value")

                        handleUpdateAttribute(idOrName, key, value)
                            .onLeft { error ->
                                if (error is UpdateAttributeError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(key.asString())
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.AttributeNotExists) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.attributeNotExists",
                                            Component.text(idOrName),
                                            Component.text(key.asString())
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.AttributeTypeNotRegistered) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.attributeTypeNotRegistered",
                                            Component.text(idOrName),
                                            Component.text(error.type)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is UpdateAttributeError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.$entityType.updateAttribute.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(key.asString()),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight { attributeEntity ->
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.updateAttribute.success",
                                        Component.text(idOrName),
                                        Component.text(key.asString()),
                                        Component.text(attributeEntity.attribute.value.toString())
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.$entityType.attributes.remove") }

            string("key") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val key = Key.key(stringParam("key"))

                    handleRemoveAttribute(idOrName, key)
                        .onLeft { error ->
                            if (error is RemoveAttributeError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(key.asString())
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.AttributeNotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.attributeNotExists",
                                        Component.text(idOrName),
                                        Component.text(key.asString())
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.removeAttribute.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(key.asString()),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.removeAttribute.success",
                                    Component.text(idOrName),
                                    Component.text(key.asString())
                                )
                            )
                        }
                }
            }
        }

        literal("clear") {
            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                handleClearAttributes(idOrName)
                    .onLeft { error ->
                        if (error is ClearAttributesError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.entityNotFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@execute
                        }
                        if (error is ClearAttributesError.NoAttributesFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.noAttributesFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@execute
                        }
                        if (error is ClearAttributesError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.unexpectedError",
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
                                "gradeway.command.$entityType.clearAttributes.success",
                                Component.text(idOrName)
                            )
                        )
                    }
            }
        }

        createScopedListHandler(
            gradeway = gradeway,
            permission = "gradeway.$entityType.attributes.list",
            scopeKey = "idOrName",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { audience, page, limit, result -> handleListRender(audience, page, limit, result) }
        )
    }
}
