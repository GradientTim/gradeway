/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.suggestRoles
import dev.gradienttim.gradeway.services.AttributeService.*
import dev.gradienttim.gradeway.services.PermissionService.*
import dev.gradienttim.gradeway.services.RoleService.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.time.Duration
import java.time.Instant
import java.util.*

internal fun <TSource> ArgumentBuilder<TSource, *>.roleBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("role") {
        requires { hasPermission(it, "gradeway.role") }

        literal("create") {
            requires { hasPermission(it, "gradeway.role.create") }

            string("name") {
                execute {
                    val audience = sourceToAudience(source)

                    val name = stringParam("name")

                    gradeway.roles.create(name)
                        .onLeft { error ->
                            if (error is CreateRoleError.EntityAlreadyExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.create.entityAlreadyExists",
                                        Component.text(name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateRoleError.InvalidName) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.create.invalidName",
                                        Component.text(name)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.create.unexpectedError",
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
                                    "gradeway.command.role.create.success",
                                    Component.text(name)
                                )
                            )
                        }
                }
            }
        }

        literal("delete") {
            requires { hasPermission(it, "gradeway.role.delete") }

            string("id") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val id = stringParam("id")
                    val uuid = runCatching {
                        UUID.fromString(id)
                    }.getOrNull()

                    if (uuid == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.role.delete.invalidUuid",
                                Component.text(id)
                            )
                        )
                        return@execute
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
                                return@execute
                            }
                            if (error is DeleteRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.delete.unexpectedError",
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
                                    "gradeway.command.role.delete.success",
                                    Component.text(id)
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
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                roleAttributesBuilder(gradeway, hasPermission, sourceToAudience)
                rolePermissionsBuilder(gradeway, hasPermission, sourceToAudience)

                literal("setWeight") {
                    requires { hasPermission(it, "gradeway.role.setWeight") }

                    integer("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val weight = intParam("value")

                            gradeway.roles.setWeight(idOrName, weight)
                                .onLeft { error ->
                                    if (error is SetWeightError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.role.setWeight.entityNotFound",
                                                Component.text(idOrName),
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetWeightError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.role.setWeight.unexpectedError",
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
                                            "gradeway.command.role.setWeight.success",
                                            Component.text(idOrName),
                                            Component.text(weight)
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.roleAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    fun <TValue : Any> handleAddAttribute(audience: Audience, idOrName: String, attribute: Attribute<TValue>) {
        gradeway.roles.addAttribute(idOrName, attribute)
            .onLeft { error ->
                if (error is AddAttributeError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.addAttribute.entityNotFound",
                            Component.text(idOrName),
                            Component.text(attribute.key.asString())
                        )
                    )
                    return
                }
                if (error is AddAttributeError.AttributeAlreadyExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.addAttribute.attributeAlreadyExists",
                            Component.text(idOrName),
                            Component.text(attribute.key.asString())
                        )
                    )
                    return
                }
                if (error is AddAttributeError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.addAttribute.unexpectedError",
                            Component.text(idOrName),
                            Component.text(attribute.key.asString()),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.role.addAttribute.success",
                        Component.text(idOrName),
                        Component.text(attribute.key.asString()),
                        Component.text(attribute.value.toString())
                    )
                )
            }
    }

    fun <TValue : Any> handleUpdateAttribute(audience: Audience, idOrName: String, key: String, value: TValue) {
        val actualKey = Key.key(key)
        gradeway.roles.updateAttribute(idOrName, Key.key(key), value)
            .onLeft { error ->
                if (error is UpdateAttributeError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.updateAttribute.entityNotFound",
                            Component.text(idOrName),
                            Component.text(actualKey.asString())
                        )
                    )
                    return
                }
                if (error is UpdateAttributeError.AttributeNotExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.updateAttribute.attributeNotExists",
                            Component.text(idOrName),
                            Component.text(actualKey.asString())
                        )
                    )
                    return
                }
                if (error is UpdateAttributeError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.updateAttribute.unexpectedError",
                            Component.text(idOrName),
                            Component.text(actualKey.asString()),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.role.updateAttribute.success",
                        Component.text(idOrName),
                        Component.text(actualKey.asString()),
                        Component.text(value.toString())
                    )
                )
            }
    }

    literal("attributes") {
        requires { hasPermission(it, "gradeway.role.attributes") }

        literal("add") {
            requires { hasPermission(it, "gradeway.role.attributes.set") }

            string("key") {
                literal("string") {
                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = stringParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.string(Key.key(key), value))
                        }
                    }
                }

                literal("boolean") {
                    boolean("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = booleanParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.boolean(Key.key(key), value))
                        }
                    }
                }

                literal("integer") {
                    integer("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = intParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.integer(Key.key(key), value))
                        }
                    }
                }

                literal("long") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.long(Key.key(key), value))
                        }
                    }
                }

                literal("double") {
                    double("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = doubleParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.double(Key.key(key), value))
                        }
                    }
                }

                literal("float") {
                    float("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = floatParam("value")

                            handleAddAttribute(audience, idOrName, Attribute.float(Key.key(key), value))
                        }
                    }
                }

                literal("uuid") {
                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = stringParam("value")

                            val uuid = runCatching {
                                UUID.fromString(value)
                            }.getOrNull()

                            if (uuid == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.addAttribute.invalidUuid",
                                        Component.text(idOrName),
                                        Component.text(key),
                                        Component.text(value)
                                    )
                                )
                                return@execute
                            }

                            handleAddAttribute(audience, idOrName, Attribute.uuid(Key.key(key), uuid))
                        }
                    }
                }

                literal("instant") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleAddAttribute(
                                audience,
                                idOrName,
                                Attribute.instant(Key.key(key), Instant.ofEpochMilli(value))
                            )
                        }
                    }
                }

                literal("duration") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleAddAttribute(
                                audience,
                                idOrName,
                                Attribute.duration(Key.key(key), Duration.ofMillis(value))
                            )
                        }
                    }
                }
            }
        }

        literal("update") {
            requires { hasPermission(it, "gradeway.role.attributes.update") }

            string("key") {
                literal("string") {
                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = stringParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("boolean") {
                    boolean("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = booleanParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("integer") {
                    integer("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = intParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("long") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("double") {
                    double("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = doubleParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("float") {
                    float("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = floatParam("value")

                            handleUpdateAttribute(audience, idOrName, key, value)
                        }
                    }
                }

                literal("uuid") {
                    string("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = stringParam("value")

                            val uuid = runCatching {
                                UUID.fromString(value)
                            }.getOrNull()

                            if (uuid == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.updateAttribute.invalidUuid",
                                        Component.text(idOrName),
                                        Component.text(key),
                                        Component.text(value)
                                    )
                                )
                                return@execute
                            }

                            handleUpdateAttribute(audience, idOrName, key, uuid)
                        }
                    }
                }

                literal("instant") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleUpdateAttribute(audience, idOrName, key, Instant.ofEpochMilli(value))
                        }
                    }
                }

                literal("duration") {
                    long("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val key = stringParam("key")
                            val value = longParam("value")

                            handleUpdateAttribute(audience, idOrName, key, Duration.ofMillis(value))
                        }
                    }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.role.attributes.remove") }

            string("key") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val key = stringParam("key")

                    gradeway.roles.removeAttribute(idOrName, Key.key(key))
                        .onLeft { error ->
                            if (error is RemoveAttributeError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.removeAttribute.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(key)
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.AttributeNotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.removeAttribute.attributeNotExists",
                                        Component.text(idOrName),
                                        Component.text(key)
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.removeAttribute.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(key),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    ),
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.removeAttribute.success",
                                    Component.text(idOrName),
                                    Component.text(key)
                                ),
                            )
                        }
                }
            }
        }

        literal("list") {
            requires { hasPermission(it, "gradeway.role.attributes.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.roles.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listAttributes.entityNotFound",
                            Component.text(idOrName)
                        )
                    )
                    return@execute
                }

                entity.attributes.forEach { attribute ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.listAttributes.entry",
                            Component.text(attribute.key.asString()),
                            Component.text(attribute.value)
                        )
                    )
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.rolePermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "gradeway.role.permissions") }

        literal("set") {
            requires { hasPermission(it, "gradeway.role.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        gradeway.roles.setPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.role.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.role.setPermission.alreadyEnabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.role.setPermission.alreadyDisabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.role.setPermission.unexpectedError",
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
                                        "gradeway.command.role.setPermission.success",
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

                    gradeway.players.setPermission(idOrName, permission, true)
                        .onLeft { error ->
                            if (error is SetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setPermission.alreadyEnabled",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setPermission.unexpectedError",
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
                                    "gradeway.command.role.setPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("unset") {
            requires { hasPermission(it, "gradeway.role.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.roles.unsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.role.unsetPermission.unexpectedError",
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
                                    "gradeway.command.role.unsetPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { hasPermission(it, "gradeway.role.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.roles.clearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.clearPermission.entityNotFound",
                                    Component.text(idOrName)
                                ),
                            )
                            return@execute
                        }
                        if (error is ClearPermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.role.clearPermissions.unexpectedError",
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
                                "gradeway.command.role.clearPermissions.success",
                                Component.text(idOrName),
                            ),
                        )
                    }
            }
        }

        literal("list") {
            requires { hasPermission(it, "gradeway.role.permissions.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.roles.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.role.notFound",
                            Component.text(idOrName)
                        )
                    )
                    return@execute
                }
            }
        }
    }
}
