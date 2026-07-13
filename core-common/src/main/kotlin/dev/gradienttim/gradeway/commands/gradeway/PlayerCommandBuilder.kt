/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.suggestPlayers
import dev.gradienttim.gradeway.commands.extensions.suggestRoles
import dev.gradienttim.gradeway.services.AttributeService.*
import dev.gradienttim.gradeway.services.PermissionService.*
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.utilities.TimeParser
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.time.Duration
import java.time.Instant
import java.util.*

internal fun <TSource> ArgumentBuilder<TSource, *>.playerBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("player") {
        requires { hasPermission(it, "gradeway.player") }

        literal("modify") {
            string("idOrName") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPlayers(gradeway, remaining.lowercase())
                    }
                }

                playerRolesBuilder(gradeway, hasPermission, sourceToAudience)
                playerAttributesBuilder(gradeway, hasPermission, sourceToAudience)
                playerPermissionsBuilder(gradeway, hasPermission, sourceToAudience)

                literal("setWeight") {
                    requires { hasPermission(it, "gradeway.player.setWeight") }

                    integer("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val weight = intParam("value")

                            gradeway.players.setWeight(idOrName, weight)
                                .onLeft { error ->
                                    if (error is PlayerService.SetWeightError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.entityNotFound",
                                                Component.text(idOrName),
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is PlayerService.SetWeightError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.unexpectedError",
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
                                            "gradeway.command.player.setWeight.success",
                                            Component.text(idOrName),
                                            Component.text(weight)
                                        )
                                    )
                                }
                        }
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")

                    val entity = gradeway.players.findByIdOrName(idOrName)
                    if (entity == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.notFound",
                                Component.text(idOrName)
                            )
                        )
                        return@execute
                    }
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerRolesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    @Suppress("ReturnCount")
    fun handleAddRole(audience: Audience, playerIdOrName: String, roleId: String, until: Instant? = null) {
        val roleUniqueId = runCatching {
            UUID.fromString(roleId)
        }.getOrNull()

        if (roleUniqueId == null) {
            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.player.addRole.invalidUuid",
                    Component.text(playerIdOrName),
                    Component.text(roleId)
                )
            )
            return
        }

        gradeway.players.addRole(playerIdOrName, roleUniqueId, until)
            .onLeft { error ->
                if (error is PlayerService.AddRoleError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.entityNotFound",
                            Component.text(playerIdOrName)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.TargetNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.targetNotFound",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.AlreadyExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.alreadyExists",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.UntilInPast) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.untilInPast",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.unexpectedError",
                            Component.text(playerIdOrName),
                            Component.text(roleId),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.player.addRole.success",
                        Component.text(playerIdOrName),
                        Component.text(roleId)
                    )
                )
            }
    }

    literal("roles") {
        requires { hasPermission(it, "gradeway.player.roles") }

        literal("add") {
            requires { hasPermission(it, "gradeway.player.roles.add") }

            string("roleId") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                string("until") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val roleId = stringParam("roleId")
                        val until = stringParam("until")

                        val untilInstant = TimeParser.parseToInstant(until, gradeway.now())
                        if (untilInstant == null) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.addRole.invalidTimeFormat",
                                    Component.text(idOrName),
                                    Component.text(until)
                                )
                            )
                            return@execute
                        }

                        handleAddRole(audience, idOrName, roleId, untilInstant)
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    handleAddRole(audience, idOrName, roleId)
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.player.roles.remove") }

            string("roleId") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.removeRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.removeRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.RemoveRoleError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.NotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.notExists",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.removeRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("setPrimary") {
            string("roleId") {
                suggestsDebounced { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.setPrimaryRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.setPrimaryRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.SetPrimaryRoleError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.AlreadyPrimary) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.alreadyPrimary",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.setPrimaryRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    fun <TValue : Any> handleAddAttribute(audience: Audience, idOrName: String, attribute: Attribute<TValue>) {
        gradeway.players.addAttribute(idOrName, attribute)
            .onLeft { error ->
                if (error is AddAttributeError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addAttribute.entityNotFound",
                            Component.text(idOrName),
                            Component.text(attribute.key.asString())
                        )
                    )
                    return
                }
                if (error is AddAttributeError.AttributeAlreadyExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addAttribute.attributeAlreadyExists",
                            Component.text(idOrName),
                            Component.text(attribute.key.asString())
                        )
                    )
                    return
                }
                if (error is AddAttributeError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addAttribute.unexpectedError",
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
                        "gradeway.command.player.addAttribute.success",
                        Component.text(idOrName),
                        Component.text(attribute.key.asString()),
                        Component.text(attribute.value.toString())
                    )
                )
            }
    }

    fun <TValue : Any> handleUpdateAttribute(audience: Audience, idOrName: String, key: String, value: TValue) {
        val actualKey = Key.key(key)
        gradeway.players.updateAttribute(idOrName, Key.key(key), value)
            .onLeft { error ->
                if (error is UpdateAttributeError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.updateAttribute.entityNotFound",
                            Component.text(idOrName),
                            Component.text(actualKey.asString())
                        )
                    )
                    return
                }
                if (error is UpdateAttributeError.AttributeNotExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.updateAttribute.attributeNotExists",
                            Component.text(idOrName),
                            Component.text(actualKey.asString())
                        )
                    )
                    return
                }
                if (error is UpdateAttributeError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.updateAttribute.unexpectedError",
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
                        "gradeway.command.player.updateAttribute.success",
                        Component.text(idOrName),
                        Component.text(actualKey.asString()),
                        Component.text(value.toString())
                    )
                )
            }
    }

    literal("attributes") {
        requires { hasPermission(it, "gradeway.player.attributes") }

        literal("add") {
            requires { hasPermission(it, "gradeway.player.attributes.set") }

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
                                        "gradeway.command.player.addAttribute.invalidUuid",
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
            requires { hasPermission(it, "gradeway.player.attributes.update") }

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
                                        "gradeway.command.player.updateAttribute.invalidUuid",
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
            requires { hasPermission(it, "gradeway.player.attributes.remove") }

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
                                        "gradeway.command.player.removeAttribute.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(key)
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.AttributeNotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeAttribute.attributeNotExists",
                                        Component.text(idOrName),
                                        Component.text(key)
                                    ),
                                )
                                return@execute
                            }
                            if (error is RemoveAttributeError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeAttribute.unexpectedError",
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
                                    "gradeway.command.player.removeAttribute.success",
                                    Component.text(idOrName),
                                    Component.text(key)
                                ),
                            )
                        }
                }
            }
        }

        literal("list") {
            requires { hasPermission(it, "gradeway.player.attributes.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.players.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.listAttributes.entityNotFound",
                            Component.text(idOrName)
                        )
                    )
                    return@execute
                }

                entity.attributes.forEach { attribute ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.listAttributes.entry",
                            Component.text(attribute.key.asString()),
                            Component.text(attribute.value)
                        )
                    )
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerPermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "gradeway.player.permissions") }

        literal("set") {
            requires { hasPermission(it, "gradeway.player.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        gradeway.players.setPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.setPermission.alreadyEnabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.setPermission.alreadyDisabled",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.setPermission.unexpectedError",
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
                                        "gradeway.command.player.setPermission.success",
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
                                        "gradeway.command.player.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPermission.alreadyEnabled",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPermission.unexpectedError",
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
                                    "gradeway.command.player.setPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("unset") {
            requires { hasPermission(it, "gradeway.player.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.players.unsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.unsetPermission.unexpectedError",
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
                                    "gradeway.command.player.unsetPermission.success",
                                    Component.text(idOrName),
                                    Component.text(permission)
                                ),
                            )
                        }
                }
            }
        }

        literal("clear") {
            requires { hasPermission(it, "gradeway.player.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.players.clearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is ClearPermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.clearPermission.entityNotFound",
                                    Component.text(idOrName),
                                ),
                            )
                            return@execute
                        }
                        if (error is ClearPermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.clearPermissions.unexpectedError",
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
                                "gradeway.command.player.clearPermissions.success",
                                Component.text(idOrName),
                            ),
                        )
                    }
            }
        }

        literal("list") {
            requires { hasPermission(it, "gradeway.player.permissions.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.players.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.notFound",
                            Component.text(idOrName)
                        )
                    )
                    return@execute
                }
            }
        }
    }
}
