/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import arrow.core.Either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.attribute.AttributeType
import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import dev.gradienttim.gradeway.services.AttributeService.*
import dev.gradienttim.gradeway.services.PermissionService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.BooleanParser.booleanParser
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun <C : Any, TResult> MutableCommandBuilder<C>.registerGlobalListCommand(
    gradeway: CommonGradeway,
    permission: String,
    audienceProvider: AudienceProvider<C>,
    query: (page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    registerCopy("list") {
        permission(permission)

        optional("page", integerParser<C>(1))
        optional("limit", integerParser<C>(1))

        handler { context ->
            val audience = audienceProvider.apply(context.sender())
            val page = context.getOrDefault("page", 1)
            val limit = context.getOrDefault("limit", 10)

            val result = transaction(gradeway.database) { query(page, limit) }
            render(audience, page, limit, result)
        }
    }
}

internal fun <C : Any, TResult> MutableCommandBuilder<C>.registerScopedListCommand(
    gradeway: CommonGradeway,
    permission: String,
    scopeKey: String,
    audienceProvider: AudienceProvider<C>,
    query: (scope: String, page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    registerCopy("list") {
        permission(permission)

        optional("page", integerParser<C>(1))
        optional("limit", integerParser<C>(1))

        handler { context ->
            val audience = audienceProvider.apply(context.sender())
            val scope = context.get<String>(scopeKey)
            val page = context.getOrDefault("page", 1)
            val limit = context.getOrDefault("limit", 10)

            val result = transaction(gradeway.database) { query(scope, page, limit) }
            render(audience, page, limit, result)
        }
    }
}

internal fun <C : Any, TListResult> MutableCommandBuilder<C>.registerEntityPermissionCommands(
    gradeway: CommonGradeway,
    entityType: String,
    audienceProvider: AudienceProvider<C>,
    handleSetPermission: (idOrName: String, permission: String, status: Boolean) -> Either<SetPermissionError, Unit>,
    handleUnsetPermission: (idOrName: String, permission: String) -> Either<UnsetPermissionError, Unit>,
    handleClearPermissions: (idOrName: String) -> Either<ClearPermissionsError, Unit>,
    handleLinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<LinkTemplateError, Unit>,
    handleUnlinkTemplate: (idOrName: String, templateIdOrName: String) -> Either<UnlinkTemplateError, Unit>,
    handleApplyTemplate: (idOrName: String, templateIdOrName: String) -> Either<ApplyTemplateError, Boolean>,
    handleRevokeTemplate: (idOrName: String, templateIdOrName: String) -> Either<RevokeTemplateError, Boolean>,
    handleListQuery: (scope: String, page: Int, limit: Int) -> TListResult,
    handleListRender: (audience: Audience, page: Int, limit: Int, result: TListResult) -> Unit
) {
    registerCopy("permissions") {
        registerCopy("set") {
            permission("gradeway.$entityType.permissions.set")

            required("permission", stringParser())
            optional("status", booleanParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val permission = context.get<String>("permission")
                val status = context.getOrDefault("status", true)

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
                            return@handler
                        }
                        if (error is SetPermissionError.PermissionAlreadyEnabled) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.setPermission.alreadyEnabled",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                            return@handler
                        }
                        if (error is SetPermissionError.PermissionAlreadyDisabled) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.setPermission.alreadyDisabled",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("unset") {
            permission("gradeway.$entityType.permissions.unset")

            required("permission", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val permission = context.get<String>("permission")

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
                            return@handler
                        }
                        if (error is UnsetPermissionError.PermissionNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.setPermission.permissionNotFound",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("clear") {
            permission("gradeway.$entityType.permissions.clear")

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                handleClearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionsError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearPermission.entityNotFound",
                                    Component.text(idOrName),
                                ),
                            )
                            return@handler
                        }
                        if (error is ClearPermissionsError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearPermissions.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(error.throwable.message ?: "Unknown")
                                ),
                            )
                            return@handler
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

        registerScopedListCommand(
            gradeway = gradeway,
            permission = "gradeway.$entityType.permissions.list",
            scopeKey = "idOrName",
            audienceProvider = audienceProvider,
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { audience, page, limit, result -> handleListRender(audience, page, limit, result) }
        )

        registerCopy("template") {
            registerCopy("link") {
                permission("gradeway.$entityType.permissionTemplate.link")

                required("templateIdOrName", stringParser()) {
                    suggestsDebounced(gradeway) { remaining -> suggestPermissionTemplates(gradeway, remaining) }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val templateIdOrName = context.get<String>("templateIdOrName")

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
                                return@handler
                            }
                            if (error is LinkTemplateError.TemplateNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.linkTemplate.templateNotFound",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is LinkTemplateError.AlreadyLinked) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.linkTemplate.alreadyLinked",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is LinkTemplateError.WrongAssignedTo) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.linkTemplate.wrongAssignedTo",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerCopy("unlink") {
                permission("gradeway.$entityType.permissionTemplate.unlink")

                required("templateIdOrName", stringParser()) {
                    suggestsDebounced(gradeway) { remaining -> suggestPermissionTemplates(gradeway, remaining) }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val templateIdOrName = context.get<String>("templateIdOrName")

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
                                return@handler
                            }
                            if (error is UnlinkTemplateError.TemplateNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.unlinkTemplate.templateNotFound",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is UnlinkTemplateError.NotLinked) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.unlinkTemplate.notLinked",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerCopy("apply") {
                permission("gradeway.$entityType.permissionTemplate.apply")

                required("templateIdOrName", stringParser()) {
                    suggestsDebounced(gradeway) { remaining -> suggestPermissionTemplates(gradeway, remaining) }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val templateIdOrName = context.get<String>("templateIdOrName")

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
                                return@handler
                            }
                            if (error is ApplyTemplateError.TemplateNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.applyTemplate.templateNotFound",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
                            }
                            if (error is ApplyTemplateError.WrongAssignedTo) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.applyTemplate.wrongAssignedTo",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
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
                                return@handler
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

            registerCopy("revoke") {
                permission("gradeway.$entityType.permissionTemplate.revoke")

                required("templateIdOrName", stringParser()) {
                    suggestsDebounced(gradeway) { remaining -> suggestPermissionTemplates(gradeway, remaining) }
                }

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val templateIdOrName = context.get<String>("templateIdOrName")

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
                                return@handler
                            }
                            if (error is RevokeTemplateError.TemplateNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.$entityType.revokeTemplate.templateNotFound",
                                        Component.text(idOrName),
                                        Component.text(templateIdOrName)
                                    )
                                )
                                return@handler
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
                                return@handler
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

internal fun <C : Any, TListResult> MutableCommandBuilder<C>.registerEntityAttributeCommands(
    gradeway: CommonGradeway,
    entityType: String,
    audienceProvider: AudienceProvider<C>,
    handleAddAttribute: (idOrName: String, attribute: Attribute<Any>) -> Either<AddAttributeError, SharedAttributeEntity>,
    handleUpdateAttribute: (idOrName: String, key: Key, value: Any) -> Either<UpdateAttributeError, SharedAttributeEntity>,
    handleRemoveAttribute: (idOrName: String, key: Key) -> Either<RemoveAttributeError, Unit>,
    handleClearAttributes: (idOrName: String) -> Either<ClearAttributesError, Unit>,
    handleListQuery: (scope: String, page: Int, limit: Int) -> TListResult,
    handleListRender: (audience: Audience, page: Int, limit: Int, result: TListResult) -> Unit
) {
    registerCopy("attributes") {
        registerCopy("add") {
            permission("gradeway.$entityType.attributes.add")

            required("key", stringParser())
            required("type", stringParser()) {
                suggestsDebounced(gradeway) { remaining ->
                    suggestAttributeTypes(remaining.lowercase())
                }
            }
            required("value", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val key = Key.key(context.get<String>("key"))
                val type = context.get<String>("type")
                val value = context.get<String>("value")

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
                    return@handler
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
                            return@handler
                        }
                        if (error is AddAttributeError.AttributeAlreadyExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.addAttribute.attributeAlreadyExists",
                                    Component.text(idOrName),
                                    Component.text(attribute.key.asString())
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("update") {
            permission("gradeway.$entityType.attributes.update")

            required("key", stringParser())
            required("value", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val key = Key.key(context.get<String>("key"))
                val value = context.get<String>("value")

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
                            return@handler
                        }
                        if (error is UpdateAttributeError.AttributeNotExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.updateAttribute.attributeNotExists",
                                    Component.text(idOrName),
                                    Component.text(key.asString())
                                )
                            )
                            return@handler
                        }
                        if (error is UpdateAttributeError.AttributeTypeNotRegistered) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.updateAttribute.attributeTypeNotRegistered",
                                    Component.text(idOrName),
                                    Component.text(error.type)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("remove") {
            permission("gradeway.$entityType.attributes.remove")

            required("key", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val key = Key.key(context.get<String>("key"))

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
                            return@handler
                        }
                        if (error is RemoveAttributeError.AttributeNotExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.removeAttribute.attributeNotExists",
                                    Component.text(idOrName),
                                    Component.text(key.asString())
                                ),
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("clear") {
            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                handleClearAttributes(idOrName)
                    .onLeft { error ->
                        if (error is ClearAttributesError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.entityNotFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@handler
                        }
                        if (error is ClearAttributesError.NoAttributesFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.noAttributesFound",
                                    Component.text(idOrName)
                                )
                            )
                            return@handler
                        }
                        if (error is ClearAttributesError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.$entityType.clearAttributes.unexpectedError",
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
                                "gradeway.command.$entityType.clearAttributes.success",
                                Component.text(idOrName)
                            )
                        )
                    }
            }
        }

        registerScopedListCommand(
            gradeway = gradeway,
            permission = "gradeway.$entityType.attributes.list",
            scopeKey = "idOrName",
            audienceProvider = audienceProvider,
            query = { scope, page, limit -> handleListQuery(scope, page, limit) },
            render = { audience, page, limit, result -> handleListRender(audience, page, limit, result) }
        )
    }
}
