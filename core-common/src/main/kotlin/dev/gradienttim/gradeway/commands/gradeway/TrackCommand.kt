/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.command.execute
import dev.gradienttim.gradeway.command.intParam
import dev.gradienttim.gradeway.command.integer
import dev.gradienttim.gradeway.command.literal
import dev.gradienttim.gradeway.command.string
import dev.gradienttim.gradeway.command.stringParam
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.database.models.track.TrackStagesTable
import dev.gradienttim.gradeway.database.models.track.TracksTable
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.entity.track.TrackStageEntity
import dev.gradienttim.gradeway.extensions.eqId
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.TrackService.*
import dev.gradienttim.gradeway.services.track.PlayerTrackService.DemotePlayerError
import dev.gradienttim.gradeway.services.track.PlayerTrackService.PromotePlayerError
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.trackCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("track") {
        requires { commandContext.hasPermission(it, "gradeway.track") }

        literal("create") {
            requires { commandContext.hasPermission(it, "gradeway.track.create") }

            string("slug") {
                execute {
                    val slug = stringParam("slug")

                    gradeway.tracks.createTrack(slug)
                        .onLeft { error ->
                            if (error is CreateTrackError.InvalidSlug) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.create.invalidSlug",
                                        Argument.string("track", slug)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateTrackError.EntityAlreadyExists) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.create.entityAlreadyExists",
                                        Argument.string("track", slug)
                                    )
                                )
                                return@execute
                            }
                            if (error is CreateTrackError.Unexpected) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.create.unexpectedError",
                                        Argument.string("track", slug),
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
                                    "gradeway.command.track.create.success",
                                    Argument.string("track", slug)
                                )
                            )
                        }
                }
            }
        }

        literal("delete") {
            requires { commandContext.hasPermission(it, "gradeway.track.delete") }

            string("idOrSlug") {
                suggestTracks(gradeway)

                execute {
                    val idOrSlug = stringParam("idOrSlug")

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.tracks.deleteTrack(idOrSlug)
                            .onLeft { error ->
                                if (error is DeleteTrackError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.delete.entityNotFound",
                                            Argument.string("track", idOrSlug)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is DeleteTrackError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.delete.unexpectedError",
                                            Argument.string("track", idOrSlug),
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
                                        "gradeway.command.track.delete.success",
                                        Argument.string("track", idOrSlug)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("info") {
            requires { commandContext.hasPermission(it, "gradeway.track.info") }

            string("idOrSlug") {
                suggestTracks(gradeway)

                execute {
                    val idOrSlug = stringParam("idOrSlug")

                    val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                    if (track == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.track.info.entityNotFound",
                                Argument.string("track", idOrSlug)
                            )
                        )
                        return@execute
                    }

                    val stages = transaction(gradeway.database) {
                        TrackStagesTable
                            .innerJoin(RolesTable, { roleId }, { id })
                            .select(TrackStagesTable.id, RolesTable.name)
                            .where { TrackStagesTable.trackId eqId track.id.value }
                            .orderBy(TrackStagesTable.position to SortOrder.ASC)
                            .map { row ->
                                object {
                                    val id = row[TrackStagesTable.id].value
                                    val roleName = row[RolesTable.name]
                                }
                            }
                    }

                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.track.info.header",
                            Argument.string("track", track.slug),
                            track.id.value.toIdArgument()
                        )
                    )

                    if (stages.isEmpty()) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable("gradeway.command.track.info.noStages")
                        )
                        return@execute
                    }

                    stages.forEachIndexed { index, stage ->
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.track.info.stageEntry",
                                Argument.numeric("position", index + 1),
                                Argument.string("role", stage.roleName),
                                stage.id.toIdArgument()
                            )
                        )
                    }
                }
            }
        }

        literal("modify") {
            string("idOrSlug") {
                suggestTracks(gradeway)

                literal("setSlug") {
                    requires { commandContext.hasPermission(it, "gradeway.track.setSlug") }

                    string("slug") {
                        execute {
                            val idOrSlug = stringParam("idOrSlug")
                            val slug = stringParam("slug")

                            gradeway.tracks.setSlug(idOrSlug, slug)
                                .onLeft { error ->
                                    if (error is SetSlugError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.setSlug.entityNotFound",
                                                Argument.string("track", idOrSlug)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetSlugError.InvalidSlug) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.setSlug.invalidSlug",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("slug", slug)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetSlugError.SlugAlreadySet) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.setSlug.slugAlreadySet",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("slug", slug)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetSlugError.SlugAlreadyExists) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.setSlug.slugAlreadyExists",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("slug", slug)
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is SetSlugError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.setSlug.unexpectedError",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("slug", slug),
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
                                            "gradeway.command.track.setSlug.success",
                                            Argument.string("track", idOrSlug),
                                            Argument.string("slug", slug)
                                        )
                                    )
                                }
                        }
                    }
                }

                literal("stages") {
                    literal("add") {
                        requires { commandContext.hasPermission(it, "gradeway.track.stages.add") }

                        string("role") {
                            suggestRoles(gradeway)

                            execute {
                                val idOrSlug = stringParam("idOrSlug")
                                val roleId = stringParam("role")
                                val roleUuid = gradeway.roles.findByIdOrName(roleId)?.id?.value

                                if (roleUuid == null) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.addStage.targetNotFound",
                                            Argument.string("role", roleId)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.tracks.addStage(idOrSlug, roleUuid)
                                    .onLeft { error ->
                                        if (error is AddStageError.EntityNotFound) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.addStage.entityNotFound",
                                                    Argument.string("track", idOrSlug)
                                                )
                                            )
                                            return@execute
                                        }
                                        if (error is AddStageError.TargetNotFound) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.addStage.targetNotFound",
                                                    Argument.string("role", roleId)
                                                )
                                            )
                                            return@execute
                                        }
                                        if (error is AddStageError.AlreadyExists) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.addStage.alreadyExists",
                                                    Argument.string("track", idOrSlug),
                                                    Argument.string("role", roleId)
                                                )
                                            )
                                            return@execute
                                        }
                                        if (error is AddStageError.Unexpected) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.addStage.unexpectedError",
                                                    Argument.string("track", idOrSlug),
                                                    Argument.string("role", roleId),
                                                    Argument.string("error", error.throwable.message ?: "Unknown")
                                                )
                                            )
                                            return@execute
                                        }
                                    }
                                    .onRight { stage ->
                                        val position = transaction(gradeway.database) {
                                            TrackStagesTable
                                                .selectAll()
                                                .where { TrackStagesTable.trackId eqId stage.trackId.value }
                                                .count()
                                        }
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.addStage.success",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("role", roleId),
                                                Argument.numeric("position", position)
                                            )
                                        )
                                    }
                            }
                        }
                    }

                    literal("remove") {
                        requires { commandContext.hasPermission(it, "gradeway.track.stages.remove") }

                        string("stage") {
                            suggestTrackStages(gradeway)

                            execute {
                                val idOrSlug = stringParam("idOrSlug")
                                val stageId = stringParam("stage")
                                val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                                if (track == null) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.removeStage.trackNotFound",
                                            Argument.string("track", idOrSlug)
                                        )
                                    )
                                    return@execute
                                }

                                val stage = gradeway.findStageOnTrack(track, stageId)
                                if (stage == null) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.removeStage.entityNotFound",
                                            Argument.string("track", idOrSlug),
                                            Argument.string("stage", stageId)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.tracks.removeStage(stage)
                                    .onLeft { error ->
                                        if (error is RemoveStageError.EntityNotFound) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.removeStage.entityNotFound",
                                                    Argument.string("track", idOrSlug),
                                                    Argument.string("stage", stageId)
                                                )
                                            )
                                            return@execute
                                        }
                                        if (error is RemoveStageError.Unexpected) {
                                            commandContext.sendTranslatedMessage(
                                                source,
                                                Component.translatable(
                                                    "gradeway.command.track.removeStage.unexpectedError",
                                                    Argument.string("track", idOrSlug),
                                                    Argument.string("stage", stageId),
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
                                                "gradeway.command.track.removeStage.success",
                                                Argument.string("track", idOrSlug),
                                                Argument.string("stage", stageId)
                                            )
                                        )
                                    }
                            }
                        }
                    }

                    literal("move") {
                        requires { commandContext.hasPermission(it, "gradeway.track.stages.move") }

                        string("stage") {
                            suggestTrackStages(gradeway)

                            integer("position", min = 1) {
                                execute {
                                    val idOrSlug = stringParam("idOrSlug")
                                    val stageId = stringParam("stage")
                                    val position = intParam("position")
                                    val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                                    if (track == null) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.moveStage.trackNotFound",
                                                Argument.string("track", idOrSlug)
                                            )
                                        )
                                        return@execute
                                    }

                                    val stage = gradeway.findStageOnTrack(track, stageId)
                                    if (stage == null) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.track.moveStage.entityNotFound",
                                                Argument.string("track", track.slug),
                                                Argument.string("stage", stageId)
                                            )
                                        )
                                        return@execute
                                    }

                                    val roleName = transaction(gradeway.database) { stage.role.name }

                                    gradeway.tracks.moveStage(stage, position - 1)
                                        .onLeft { error ->
                                            if (error is MoveStageError.EntityNotFound) {
                                                commandContext.sendTranslatedMessage(
                                                    source,
                                                    Component.translatable(
                                                        "gradeway.command.track.moveStage.entityNotFound",
                                                        Argument.string("track", track.slug),
                                                        Argument.string("stage", stageId)
                                                    )
                                                )
                                                return@execute
                                            }
                                            if (error is MoveStageError.InvalidPosition) {
                                                commandContext.sendTranslatedMessage(
                                                    source,
                                                    Component.translatable(
                                                        "gradeway.command.track.moveStage.invalidPosition",
                                                        Argument.string("track", track.slug),
                                                        Argument.numeric("position", position),
                                                        Argument.numeric("count", error.stageCount)
                                                    )
                                                )
                                                return@execute
                                            }
                                            if (error is MoveStageError.AlreadyAtPosition) {
                                                commandContext.sendTranslatedMessage(
                                                    source,
                                                    Component.translatable(
                                                        "gradeway.command.track.moveStage.alreadyAtPosition",
                                                        Argument.string("track", track.slug),
                                                        Argument.string("role", roleName),
                                                        Argument.numeric("position", position)
                                                    )
                                                )
                                                return@execute
                                            }
                                            if (error is MoveStageError.Unexpected) {
                                                commandContext.sendTranslatedMessage(
                                                    source,
                                                    Component.translatable(
                                                        "gradeway.command.track.moveStage.unexpectedError",
                                                        Argument.string("track", track.slug),
                                                        Argument.string("role", roleName),
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
                                                    "gradeway.command.track.moveStage.success",
                                                    Argument.string("track", track.slug),
                                                    Argument.string("role", roleName),
                                                    Argument.numeric("position", position)
                                                )
                                            )
                                        }
                                }
                            }
                        }
                    }

                    registerScopedListCommand(
                        gradeway = gradeway,
                        permission = "gradeway.track.stages.list",
                        scopeKey = "idOrSlug",
                        context = commandContext,
                        query = { idOrSlug, page, limit ->
                            val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                            object {
                                val requested = idOrSlug
                                val slug = track?.slug
                                val stages = track?.let {
                                    TrackStagesTable
                                        .innerJoin(RolesTable, { roleId }, { id })
                                        .select(TrackStagesTable.id, RolesTable.name)
                                        .where { TrackStagesTable.trackId eqId it.id.value }
                                        .orderBy(TrackStagesTable.position to SortOrder.ASC)
                                        .limit(limit)
                                        .offset(((page - 1) * limit).toLong())
                                        .map { row ->
                                            object {
                                                val id = row[TrackStagesTable.id].value
                                                val roleName = row[RolesTable.name]
                                            }
                                        }
                                }.orEmpty()
                            }
                        },
                        render = { source, page, limit, result ->
                            val slug = result.slug
                            if (slug == null) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.listStages.entityNotFound",
                                        Argument.string("track", result.requested)
                                    )
                                )
                                return@registerScopedListCommand
                            }

                            val stages = result.stages
                            if (stages.isEmpty()) {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.listStages.empty",
                                        Argument.string("track", slug)
                                    )
                                )
                                return@registerScopedListCommand
                            }

                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.track.listStages.header",
                                    Argument.string("track", slug),
                                    Argument.numeric("page", page),
                                    Argument.numeric("limit", limit)
                                )
                            )

                            stages.forEachIndexed { index, stage ->
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.track.listStages.entry",
                                        Argument.numeric("position", (page - 1) * limit + index + 1),
                                        Argument.string("role", stage.roleName),
                                        stage.id.toIdArgument()
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        literal("promote") {
            requires { commandContext.hasPermission(it, "gradeway.track.promote") }

            string("player") {
                suggestPlayers(gradeway)

                string("idOrSlug") {
                    suggestTracks(gradeway)

                    execute {
                        val playerIdOrName = stringParam("player")
                        val idOrSlug = stringParam("idOrSlug")

                        val player = gradeway.players.findByIdOrName(playerIdOrName)
                        if (player == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.track.promote.entityNotFound",
                                    Argument.string("player", playerIdOrName)
                                )
                            )
                            return@execute
                        }

                        val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                        if (track == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.track.promote.targetNotFound",
                                    Argument.string("track", idOrSlug)
                                )
                            )
                            return@execute
                        }

                        gradeway.tracks.promotePlayer(player, track)
                            .onLeft { error ->
                                if (error is PromotePlayerError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.promote.entityNotFound",
                                            Argument.string("player", playerIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PromotePlayerError.TargetNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.promote.targetNotFound",
                                            Argument.string("track", idOrSlug)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PromotePlayerError.MaximumStageReached) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.promote.maximumStageReached",
                                            Argument.string("player", playerIdOrName),
                                            Argument.string("track", track.slug)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PromotePlayerError.NoStages) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.promote.noStages",
                                            Argument.string("track", track.slug)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PromotePlayerError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.promote.unexpectedError",
                                            Argument.string("player", playerIdOrName),
                                            Argument.string("track", track.slug),
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
                                        "gradeway.command.track.promote.success",
                                        Argument.string("player", playerIdOrName),
                                        Argument.string("track", track.slug)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("demote") {
            requires { commandContext.hasPermission(it, "gradeway.track.demote") }

            string("player") {
                suggestPlayers(gradeway)

                string("idOrSlug") {
                    suggestTracks(gradeway)

                    execute {
                        val playerIdOrName = stringParam("player")
                        val idOrSlug = stringParam("idOrSlug")

                        val player = gradeway.players.findByIdOrName(playerIdOrName)
                        if (player == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.track.demote.entityNotFound",
                                    Argument.string("player", playerIdOrName)
                                )
                            )
                            return@execute
                        }

                        val track = gradeway.tracks.findTrackByIdOrSlug(idOrSlug)
                        if (track == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.track.demote.targetNotFound",
                                    Argument.string("track", idOrSlug)
                                )
                            )
                            return@execute
                        }

                        gradeway.tracks.demotePlayer(player, track)
                            .onLeft { error ->
                                if (error is DemotePlayerError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.demote.entityNotFound",
                                            Argument.string("player", playerIdOrName)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is DemotePlayerError.TargetNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.demote.targetNotFound",
                                            Argument.string("track", idOrSlug)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is DemotePlayerError.MinimumStageReached) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.demote.minimumStageReached",
                                            Argument.string("player", playerIdOrName),
                                            Argument.string("track", track.slug)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is DemotePlayerError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.track.demote.unexpectedError",
                                            Argument.string("player", playerIdOrName),
                                            Argument.string("track", track.slug),
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
                                        "gradeway.command.track.demote.success",
                                        Argument.string("player", playerIdOrName),
                                        Argument.string("track", track.slug)
                                    )
                                )
                            }
                    }
                }
            }
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.track.list",
            context = commandContext,
            query = { page, limit ->
                val tracks = TracksTable
                    .selectAll()
                    .orderBy(TracksTable.slug to SortOrder.ASC)
                    .limit(limit)
                    .offset(((page - 1) * limit).toLong())
                    .map { row -> row[TracksTable.id].value to row[TracksTable.slug] }

                val stageNamesByTrack = TrackStagesTable
                    .innerJoin(RolesTable, { roleId }, { id })
                    .select(TrackStagesTable.trackId, RolesTable.name)
                    .where { TrackStagesTable.trackId inList tracks.map { (id, _) -> id } }
                    .orderBy(TrackStagesTable.position to SortOrder.ASC)
                    .groupBy({ it[TrackStagesTable.trackId].value }, { it[RolesTable.name] })

                tracks.map { (trackId, trackSlug) ->
                    object {
                        val id = trackId
                        val slug = trackSlug
                        val stages = stageNamesByTrack[trackId].orEmpty()
                    }
                }
            },
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.track.list.empty")
                    )
                    return@registerGlobalListCommand
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.track.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { track ->
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.track.list.entry",
                            track.id.toIdArgument(),
                            Argument.string("track", track.slug),
                            Argument.component("stages", trackStagePreview(track.stages))
                        )
                    )
                }
            }
        )
    }
}

private const val MAX_STAGE_PREVIEW = 10

internal fun trackStagePreview(roleNames: List<String>): Component {
    val entries = roleNames.take(MAX_STAGE_PREVIEW).mapIndexed { index, roleName ->
        Component.translatable(
            "gradeway.command.track.list.stage",
            Argument.numeric("position", index + 1),
            Argument.string("role", roleName)
        )
    }
    val remaining = roleNames.size - MAX_STAGE_PREVIEW
    if (remaining <= 0) {
        return infoList(entries)
    }
    return infoList(
        entries + Component.translatable(
            "gradeway.command.track.list.moreStages",
            Argument.numeric("count", remaining)
        )
    )
}

private fun CommonGradeway<*>.findStageOnTrack(track: TrackEntity, value: String): TrackStageEntity? {
    val stage = runCatching { UUID.fromString(value) }.getOrNull()?.let { tracks.findStageById(it) }
        ?: roles.findByIdOrName(value)?.let { role -> tracks.findStageByRole(track.id.value, role.id.value) }
    return stage?.takeIf { it.trackId.value == track.id.value }
}
