/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.role.RoleParentEntity
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object RoleParentsTable : CompositeIdTable(name = TableConstants.ROLE_PARENTS_TABLE_NAME) {
    val parentId = reference("parent_id", RolesTable.id, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val childId = reference("child_id", RolesTable.id, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    init {
        addIdColumn(parentId)
        addIdColumn(childId)
        uniqueIndex(parentId, childId)
    }
}

class DatabaseRoleParentEntity(id: EntityID<CompositeID>) : CompositeEntity(id), RoleParentEntity {
    companion object : CompositeEntityClass<DatabaseRoleParentEntity>(RoleParentsTable)

    override var parentId by RoleParentsTable.parentId
    override var childId by RoleParentsTable.childId

    override val parent by DatabaseRoleEntity referencedOn RoleParentsTable.parentId
    override val child by DatabaseRoleEntity referencedOn RoleParentsTable.childId
}
