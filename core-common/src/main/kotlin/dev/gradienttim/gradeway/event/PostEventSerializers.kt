/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

import dev.gradienttim.gradeway.events.attribute.AttributeAddedEvent
import dev.gradienttim.gradeway.events.attribute.AttributeRemovedEvent
import dev.gradienttim.gradeway.events.attribute.AttributeUpdatedEvent
import dev.gradienttim.gradeway.events.attribute.AttributesClearedEvent
import dev.gradienttim.gradeway.events.group.*
import dev.gradienttim.gradeway.events.permission.*
import dev.gradienttim.gradeway.events.player.*
import dev.gradienttim.gradeway.events.role.*
import dev.gradienttim.gradeway.events.template.*
import dev.gradienttim.gradeway.events.track.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal val postEventSerializersModule = SerializersModule {
    polymorphic(PostEvent::class) {
        subclass(RoleCreatedEvent::class)
        subclass(RoleDeletedEvent::class)
        subclass(RoleRenamedEvent::class)
        subclass(RoleWeightChangedEvent::class)
        subclass(RoleDefaultChangedEvent::class)
        subclass(RoleParentAddedEvent::class)
        subclass(RoleParentRemovedEvent::class)
        subclass(GroupCreatedEvent::class)
        subclass(GroupDeletedEvent::class)
        subclass(GroupRenamedEvent::class)
        subclass(GroupDefaultWeightChangedEvent::class)
        subclass(GroupRoleAddedEvent::class)
        subclass(GroupRoleRemovedEvent::class)
        subclass(PlayerCreatedEvent::class)
        subclass(PlayerDeletedEvent::class)
        subclass(PlayerRenamedEvent::class)
        subclass(PlayerWeightChangedEvent::class)
        subclass(PlayerRoleAddedEvent::class)
        subclass(PlayerRoleRemovedEvent::class)
        subclass(PlayerRoleExpiredEvent::class)
        subclass(PlayerRoleExpiryChangedEvent::class)
        subclass(PlayerRolePausedEvent::class)
        subclass(PlayerRoleResumedEvent::class)
        subclass(PlayerPrimaryRoleChangedEvent::class)
        subclass(PermissionNodeCreatedEvent::class)
        subclass(PermissionNodeDeletedEvent::class)
        subclass(PermissionNodeValueChangedEvent::class)
        subclass(PermissionNodeTypeChangedEvent::class)
        subclass(PermissionsAssignedEvent::class)
        subclass(PermissionsUnassignedEvent::class)
        subclass(PermissionsClearedEvent::class)
        subclass(TemplateCreatedEvent::class)
        subclass(TemplateDeletedEvent::class)
        subclass(TemplateRenamedEvent::class)
        subclass(TemplateAssignedToChangedEvent::class)
        subclass(TemplatePermissionAddedEvent::class)
        subclass(TemplatePermissionRemovedEvent::class)
        subclass(TemplatePermissionsClearedEvent::class)
        subclass(TemplateLinkedEvent::class)
        subclass(TemplateUnlinkedEvent::class)
        subclass(TemplateAppliedEvent::class)
        subclass(TemplateRevokedEvent::class)
        subclass(TrackCreatedEvent::class)
        subclass(TrackDeletedEvent::class)
        subclass(TrackSlugChangedEvent::class)
        subclass(TrackStageAddedEvent::class)
        subclass(TrackStageRemovedEvent::class)
        subclass(TrackStageMovedEvent::class)
        subclass(PlayerPromotedEvent::class)
        subclass(PlayerDemotedEvent::class)
        subclass(AttributeAddedEvent::class)
        subclass(AttributeUpdatedEvent::class)
        subclass(AttributeRemovedEvent::class)
        subclass(AttributesClearedEvent::class)
    }
}
