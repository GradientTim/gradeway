/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.spec

import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity

/**
 * Describes a permission template that is about to be created.
 *
 * Configured by the caller of [dev.gradienttim.gradeway.services.PermissionService.createTemplate]
 * and can be changed by listeners of [dev.gradienttim.gradeway.events.template.TemplateCreateEvent]
 * before the insert.
 */
class TemplateCreateSpec(var name: String) {
    var assignedTo: PermissionTemplateEntity.AssignedTo = PermissionTemplateEntity.AssignedTo.ALL
}
