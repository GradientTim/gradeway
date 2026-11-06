/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditRecorder
import dev.gradienttim.gradeway.audit.audited
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleParentEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RoleParentEntity
import dev.gradienttim.gradeway.event.fireCancellable
import dev.gradienttim.gradeway.events.role.*
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.spec.RoleCreateSpec
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*

@Suppress("LargeClass", "TooManyFunctions")
class CommonRoleService(
    val gradeway: CommonGradeway<*>
) : RoleService {
    init {
        gradeway.messaging.subscribe { payload -> invalidateWeightFor(payload) }
    }

    override fun create(
        actor: Actor,
        name: String,
        builder: RoleCreateSpec.() -> Unit,
    ): Either<RoleService.CreateRoleError, DatabaseRoleEntity> = gradeway.audited(actor) { audit ->
        val spec = RoleCreateSpec(name).apply(builder)
        fireCancellable(gradeway, RoleCreateEvent(actor, spec), RoleService.CreateRoleError.Cancelled)
        if (!spec.name.isNameValid(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            raise(RoleService.CreateRoleError.InvalidName)
        }
        try {
            transaction(gradeway.database) {
                DatabaseRoleEntity.new {
                    this.name = spec.name
                    this.weight = spec.weight
                    this.isDefault = spec.isDefault
                }.also {
                    audit.created(it)
                    audit.emit(RoleCreatedEvent(actor, it.id.value, it.name, it.weight, it.isDefault))
                }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(RoleService.CreateRoleError.EntityAlreadyExists)
            }
            raise(RoleService.CreateRoleError.Unexpected(throwable))
        }
    }

    override fun delete(
        actor: Actor,
        id: UUID,
    ): Either<RoleService.DeleteRoleError, Unit> = gradeway.audited(actor) { audit ->
        val entity = findById(id) ?: raise(RoleService.DeleteRoleError.EntityNotFound)
        fireCancellable(gradeway, RoleDeleteEvent(actor, entity), RoleService.DeleteRoleError.Cancelled)
        try {
            transaction(gradeway.database) {
                audit.deleted(entity)
                audit.emit(RoleDeletedEvent(actor, entity.id.value, entity.name))
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(RoleService.DeleteRoleError.Unexpected(throwable))
        }
    }

    override fun setName(
        actor: Actor,
        id: UUID,
        name: String,
    ): Either<RoleService.SetNameError, Boolean> = either {
        val entity = findById(id) ?: raise(RoleService.SetNameError.EntityNotFound)
        return setName(actor, entity, name)
    }

    override fun setName(
        actor: Actor,
        entity: RoleEntity,
        name: String,
    ): Either<RoleService.SetNameError, Boolean> = gradeway.audited(actor) { audit ->
        if (!name.isNameValid(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            raise(RoleService.SetNameError.InvalidName)
        }
        if (entity.name == name) {
            raise(RoleService.SetNameError.NameAlreadySet)
        }
        if (entity !is DatabaseRoleEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseRoleEntity")
            raise(RoleService.SetNameError.Unexpected(throwable))
        }
        fireCancellable(gradeway, RoleRenameEvent(actor, entity, name), RoleService.SetNameError.Cancelled)
        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(RoleRenamedEvent(actor, entity.id.value, entity.name, name))
                entity.name = name
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(RoleService.SetNameError.NameAlreadyExists)
            }
            raise(RoleService.SetNameError.Unexpected(throwable))
        }
    }

    override fun setName(
        actor: Actor,
        idOrName: String,
        name: String,
    ): Either<RoleService.SetNameError, Boolean> = either {
        val entity = findByIdOrName(idOrName) ?: raise(RoleService.SetNameError.EntityNotFound)
        return setName(actor, entity, name)
    }

    override fun setWeight(
        actor: Actor,
        id: UUID,
        weight: Int,
    ): Either<RoleService.SetWeightError, Boolean> = either {
        val entity = findById(id) ?: raise(RoleService.SetWeightError.EntityNotFound)
        return setWeight(actor, entity, weight)
    }

    override fun setWeight(
        actor: Actor,
        entity: RoleEntity,
        weight: Int,
    ): Either<RoleService.SetWeightError, Boolean> = gradeway.audited(actor) { audit ->
        if (entity !is DatabaseRoleEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseRoleEntity")
            raise(RoleService.SetWeightError.Unexpected(throwable))
        }
        fireCancellable(gradeway, RoleWeightChangeEvent(actor, entity, weight), RoleService.SetWeightError.Cancelled)
        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(RoleWeightChangedEvent(actor, entity.id.value, entity.weight, weight))
                entity.weight = weight
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(RoleService.SetWeightError.Unexpected(throwable))
        }
    }

    override fun setWeight(
        actor: Actor,
        idOrName: String,
        weight: Int,
    ): Either<RoleService.SetWeightError, Boolean> = either {
        val entity = findByIdOrName(idOrName) ?: raise(RoleService.SetWeightError.EntityNotFound)
        return setWeight(actor, entity, weight)
    }

    override fun getEffectiveWeight(id: UUID): Int {
        return gradeway.caches.roleEffectiveWeights.get(id)
    }

    override fun getEffectiveWeight(entity: RoleEntity): Int {
        return getEffectiveWeight(entity.id.value)
    }

    override fun getEffectiveWeight(idOrName: String): Int {
        val entity = findByIdOrName(idOrName) ?: return -1
        return getEffectiveWeight(entity)
    }

    private fun invalidateWeightFor(payload: MessagingPayload) {
        when (payload) {
            is RoleChangedPayload -> invalidateRoleWeight(payload.roleId)
            is GroupRoleChangedPayload -> invalidateRoleWeight(payload.roleId)
            is GroupChangedPayload, is CacheFlushPayload -> {
                gradeway.caches.roles.invalidateAll()
                gradeway.caches.roleEffectiveWeights.invalidateAll()
            }

            else -> {}
        }
    }

    /**
     * Invalidates both the effective-weight cache and the underlying raw role-entity cache that
     * feeds it: [dev.gradienttim.gradeway.platform.CommonCaches.resolveEffectiveRoleWeight] reads
     * the role's own `weight` field off the cached [RoleEntity], so leaving that entity cached
     * would let a weight change keep resolving to its stale pre-change value until the entity
     * cache's own TTL separately expires.
     */
    private fun invalidateRoleWeight(rawRoleId: String) {
        val roleId = runCatching { UUID.fromString(rawRoleId) }.getOrNull() ?: return
        gradeway.caches.roles.invalidate(roleId)
        gradeway.caches.roleEffectiveWeights.invalidate(roleId)
    }

    override fun findById(id: UUID): DatabaseRoleEntity? {
        return transaction {
            DatabaseRoleEntity.findById(id)
        }
    }

    override fun findByName(name: String): DatabaseRoleEntity? {
        if (!name.isNameValid(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find { RolesTable.name eq name }.limit(1).firstOrNull()
        }
    }

    override fun findByIdOrName(value: String): DatabaseRoleEntity? {
        if (
            value.length <= TableConstants.ROLES_TABLE_MAX_NAME_LENGTH &&
            !value.isNameValid(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)
        ) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find {
                (RolesTable.id eqAsStr value) or (RolesTable.name eq value)
            }.limit(1).firstOrNull()
        }
    }

    override fun existsById(id: UUID): Boolean =
        findById(id) != null

    override fun existsByName(name: String): Boolean =
        findByName(name) != null

    override fun existsByIdOrName(value: String): Boolean =
        findByIdOrName(value) != null

    override fun getDefaultRole(): DatabaseRoleEntity? {
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find { RolesTable.isDefault eq true }
                .orderBy(RolesTable.weight to SortOrder.DESC, RolesTable.name to SortOrder.ASC)
                .limit(1)
                .firstOrNull()
        }
    }

    override fun getDefaultRoles(): List<DatabaseRoleEntity> {
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find { RolesTable.isDefault eq true }
                .orderBy(RolesTable.weight to SortOrder.DESC, RolesTable.name to SortOrder.ASC)
                .toList()
        }
    }

    override fun setDefaultFlag(
        actor: Actor,
        role: RoleEntity,
        isDefault: Boolean,
    ): Either<RoleService.SetDefaultFlagError, Unit> = gradeway.audited(actor) { audit ->
        catch({
            transaction(gradeway.database) {
                val current = RolesTable
                    .select(RolesTable.isDefault)
                    .where { RolesTable.id eq role.id.value }
                    .firstOrNull()
                    ?.get(RolesTable.isDefault)
                    ?: raise(RoleService.SetDefaultFlagError.EntityNotFound)
                if (current == isDefault) {
                    raise(RoleService.SetDefaultFlagError.FlagAlreadySet)
                }
                fireCancellable(
                    gradeway,
                    RoleDefaultChangeEvent(actor, role, isDefault, exclusive = false),
                    RoleService.SetDefaultFlagError.Cancelled,
                )

                auditDefaultChanges(actor, audit, RolesTable.id eq role.id.value) {
                    RolesTable.update({ RolesTable.id eq role.id.value }) { it[this.isDefault] = isDefault }
                }
            }
        }) { throwable ->
            raise(RoleService.SetDefaultFlagError.Unexpected(throwable))
        }
    }

    override fun setDefaultFlag(
        actor: Actor,
        idOrName: String,
        isDefault: Boolean,
    ): Either<RoleService.SetDefaultFlagError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(RoleService.SetDefaultFlagError.EntityNotFound)
        return setDefaultFlag(actor, entity, isDefault)
    }

    override fun setDefault(
        actor: Actor,
        role: RoleEntity,
    ): Either<RoleService.SetDefaultError, Unit> = gradeway.audited(actor) { audit ->
        catch({
            transaction(gradeway.database) {
                val defaultRoleIds = RolesTable
                    .select(RolesTable.id)
                    .where { RolesTable.isDefault eq true }
                    .map { it[RolesTable.id].value }
                if (defaultRoleIds == listOf(role.id.value)) {
                    raise(RoleService.SetDefaultError.AlreadyDefault)
                }
                fireCancellable(
                    gradeway,
                    RoleDefaultChangeEvent(actor, role, isDefault = true, exclusive = true),
                    RoleService.SetDefaultError.Cancelled,
                )

                val affected = (RolesTable.isDefault eq true) or (RolesTable.id eq role.id.value)
                auditDefaultChanges(actor, audit, affected) {
                    RolesTable.update({ RolesTable.isDefault eq true }) { it[isDefault] = false }
                    val updated = RolesTable.update({ RolesTable.id eq role.id.value }) { it[isDefault] = true }
                    if (updated == 0) {
                        raise(RoleService.SetDefaultError.EntityNotFound)
                    }
                }
            }
        }) { throwable ->
            raise(RoleService.SetDefaultError.Unexpected(throwable))
        }
    }

    override fun setDefault(
        actor: Actor,
        idOrName: String,
    ): Either<RoleService.SetDefaultError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(RoleService.SetDefaultError.EntityNotFound)
        return setDefault(actor, entity)
    }

    override fun clearDefault(
        actor: Actor,
    ): Either<RoleService.ClearDefaultError, Unit> = gradeway.audited(actor) { audit ->
        catch({
            transaction(gradeway.database) {
                if (RolesTable.select(RolesTable.id).where { RolesTable.isDefault eq true }.empty()) {
                    raise(RoleService.ClearDefaultError.NoDefaultRole)
                }
                fireCancellable(
                    gradeway,
                    RoleDefaultChangeEvent(actor, null, isDefault = false, exclusive = true),
                    RoleService.ClearDefaultError.Cancelled,
                )

                auditDefaultChanges(actor, audit, RolesTable.isDefault eq true) {
                    val cleared = RolesTable.update({ RolesTable.isDefault eq true }) { it[isDefault] = false }
                    if (cleared == 0) {
                        raise(RoleService.ClearDefaultError.NoDefaultRole)
                    }
                }
            }
        }) { throwable ->
            raise(RoleService.ClearDefaultError.Unexpected(throwable))
        }
    }

    /**
     * Returns whether [candidateAncestor] is reachable by walking [role]'s parent chain transitively,
     * i.e., whether [candidateAncestor] is (directly or indirectly) a parent of [role].
     */
    private fun isAncestorOf(
        candidateAncestor: RoleEntity,
        role: RoleEntity,
        visitedRoleIds: MutableSet<UUID> = mutableSetOf()
    ): Boolean {
        if (!visitedRoleIds.add(role.id.value)) {
            return false
        }
        return role.parents.any { roleParentEntity ->
            roleParentEntity.parentId == candidateAncestor.id ||
                    isAncestorOf(candidateAncestor, roleParentEntity.parent, visitedRoleIds)
        }
    }

    override fun addParent(
        actor: Actor,
        role: RoleEntity,
        parentId: UUID,
    ): Either<RoleService.AddParentError, RoleParentEntity> = either {
        val parent = findById(parentId) ?: raise(RoleService.AddParentError.TargetNotFound)
        return addParent(actor, role, parent)
    }

    override fun addParent(
        actor: Actor,
        role: RoleEntity,
        parent: RoleEntity,
    ): Either<RoleService.AddParentError, RoleParentEntity> = gradeway.audited(actor) { audit ->
        if (role.id == parent.id) {
            raise(RoleService.AddParentError.SelfReference)
        }

        catch({
            transaction(gradeway.database) {
                if (isAncestorOf(role, parent)) {
                    raise(RoleService.AddParentError.CyclicRelation)
                }
                fireCancellable(gradeway, RoleParentAddEvent(actor, role, parent), RoleService.AddParentError.Cancelled)

                DatabaseRoleParentEntity.new {
                    this.parentId = parent.id
                    this.childId = role.id
                }.also {
                    audit.created(it)
                    audit.emit(RoleParentAddedEvent(actor, role.id.value, parent.id.value))
                }
            }
        }) { throwable ->
            if (throwable.isIntegrityConstraintViolation()) {
                raise(RoleService.AddParentError.AlreadyParent)
            }
            raise(RoleService.AddParentError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(
            RoleParentChangedPayload(role.id.value.toString(), parent.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun addParent(
        actor: Actor,
        roleIdOrName: String,
        parentId: UUID,
    ): Either<RoleService.AddParentError, RoleParentEntity> = either {
        val role = findByIdOrName(roleIdOrName) ?: raise(RoleService.AddParentError.EntityNotFound)
        val parent = findById(parentId) ?: raise(RoleService.AddParentError.TargetNotFound)
        return addParent(actor, role, parent)
    }

    override fun addParent(
        actor: Actor,
        roleIdOrName: String,
        parent: RoleEntity,
    ): Either<RoleService.AddParentError, RoleParentEntity> = either {
        val role = findByIdOrName(roleIdOrName) ?: raise(RoleService.AddParentError.EntityNotFound)
        return addParent(actor, role, parent)
    }

    override fun addChild(
        actor: Actor,
        parentIdOrName: String,
        childId: UUID,
    ): Either<RoleService.AddParentError, RoleParentEntity> = either {
        val parent = findByIdOrName(parentIdOrName) ?: raise(RoleService.AddParentError.EntityNotFound)
        val child = findById(childId) ?: raise(RoleService.AddParentError.TargetNotFound)
        return addParent(actor, child, parent)
    }

    override fun removeParent(
        actor: Actor,
        role: RoleEntity,
        parentId: UUID,
    ): Either<RoleService.RemoveParentError, Unit> = either {
        val parent = findById(parentId) ?: raise(RoleService.RemoveParentError.TargetNotFound)
        return removeParent(actor, role, parent)
    }

    override fun removeParent(
        actor: Actor,
        role: RoleEntity,
        parent: RoleEntity,
    ): Either<RoleService.RemoveParentError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            val roleParentEntity = role.parents.find { it.parentId == parent.id }
            if (roleParentEntity == null) {
                raise(RoleService.RemoveParentError.NotParent)
            }
            if (roleParentEntity !is DatabaseRoleParentEntity) {
                val throwable = Throwable("Entity is not a type of DatabaseRoleParentEntity")
                raise(RoleService.RemoveParentError.Unexpected(throwable))
            }
            fireCancellable(
                gradeway,
                RoleParentRemoveEvent(actor, role, parent),
                RoleService.RemoveParentError.Cancelled,
            )
            try {
                audit.deleted(roleParentEntity)
                audit.emit(RoleParentRemovedEvent(actor, role.id.value, parent.id.value))
                roleParentEntity.delete()
            } catch (throwable: Throwable) {
                raise(RoleService.RemoveParentError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            RoleParentChangedPayload(role.id.value.toString(), parent.id.value.toString(), MessagingAction.DELETED)
        )
    }

    override fun removeParent(
        actor: Actor,
        roleIdOrName: String,
        parentId: UUID,
    ): Either<RoleService.RemoveParentError, Unit> = either {
        val role = findByIdOrName(roleIdOrName) ?: raise(RoleService.RemoveParentError.EntityNotFound)
        val parent = findById(parentId) ?: raise(RoleService.RemoveParentError.TargetNotFound)
        return removeParent(actor, role, parent)
    }

    override fun removeParent(
        actor: Actor,
        roleIdOrName: String,
        parent: RoleEntity,
    ): Either<RoleService.RemoveParentError, Unit> = either {
        val role = findByIdOrName(roleIdOrName) ?: raise(RoleService.RemoveParentError.EntityNotFound)
        return removeParent(actor, role, parent)
    }

    override fun removeChild(
        actor: Actor,
        parentIdOrName: String,
        childId: UUID,
    ): Either<RoleService.RemoveParentError, Unit> = either {
        val parent = findByIdOrName(parentIdOrName) ?: raise(RoleService.RemoveParentError.EntityNotFound)
        val child = findById(childId) ?: raise(RoleService.RemoveParentError.TargetNotFound)
        return removeParent(actor, child, parent)
    }

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        id: UUID,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addRoleAttribute(actor, id, attribute)

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        entity: RoleEntity,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addRoleAttribute(actor, entity, attribute)

    override fun <TValue : Any> addAttribute(
        actor: Actor,
        idOrName: String,
        attribute: Attribute<TValue>,
    ) = gradeway.attributes.addRoleAttribute(actor, idOrName, attribute)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updateRoleAttribute(actor, id, key, value)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        entity: RoleEntity,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updateRoleAttribute(actor, entity, key, value)

    override fun <TValue : Any> updateAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
        value: TValue,
    ) = gradeway.attributes.updateRoleAttribute(actor, idOrName, key, value)

    override fun removeAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
    ) = gradeway.attributes.removeRoleAttribute(actor, id, key)

    override fun removeAttribute(
        actor: Actor,
        entity: RoleEntity,
        key: Key,
    ) = gradeway.attributes.removeRoleAttribute(actor, entity, key)

    override fun removeAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
    ) = gradeway.attributes.removeRoleAttribute(actor, idOrName, key)

    override fun clearAttributes(
        actor: Actor,
        id: UUID,
    ) = gradeway.attributes.clearRoleAttributes(actor, id)

    override fun clearAttributes(
        actor: Actor,
        entity: RoleEntity,
    ) = gradeway.attributes.clearRoleAttributes(actor, entity)

    override fun clearAttributes(
        actor: Actor,
        idOrName: String,
    ) = gradeway.attributes.clearRoleAttributes(actor, idOrName)

    override fun hasAttribute(id: UUID, key: Key) =
        gradeway.attributes.hasRoleAttribute(id, key)

    override fun hasAttribute(entity: RoleEntity, key: Key) =
        gradeway.attributes.hasRoleAttribute(entity, key)

    override fun hasAttribute(idOrName: String, key: Key) =
        gradeway.attributes.hasRoleAttribute(idOrName, key)

    override fun getAttribute(id: UUID, key: Key) =
        gradeway.attributes.getRoleAttribute(id, key)

    override fun getAttribute(entity: RoleEntity, key: Key) =
        gradeway.attributes.getRoleAttribute(entity, key)

    override fun getAttribute(idOrName: String, key: Key) =
        gradeway.attributes.getRoleAttribute(idOrName, key)

    override fun setPermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setRolePermission(actor, id, permission, enabled)

    override fun setPermission(
        actor: Actor,
        entity: RoleEntity,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setRolePermission(actor, entity, permission, enabled)

    override fun setPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setRolePermission(actor, idOrName, permission, enabled)

    override fun setPermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setRolePermissions(actor, id, permissions)

    override fun setPermissions(
        actor: Actor,
        entity: RoleEntity,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setRolePermissions(actor, entity, permissions)

    override fun setPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setRolePermissions(actor, idOrName, permissions)

    override fun unsetPermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ) = gradeway.permissions.unsetRolePermission(actor, id, permission)

    override fun unsetPermission(
        actor: Actor,
        entity: RoleEntity,
        permission: String,
    ) = gradeway.permissions.unsetRolePermission(actor, entity, permission)

    override fun unsetPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ) = gradeway.permissions.unsetRolePermission(actor, idOrName, permission)

    override fun unsetPermissions(
        actor: Actor,
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.unsetRolePermissions(actor, id, permissions)

    override fun unsetPermissions(
        actor: Actor,
        entity: RoleEntity,
        permissions: List<String>,
    ) = gradeway.permissions.unsetRolePermissions(actor, entity, permissions)

    override fun unsetPermissions(
        actor: Actor,
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.unsetRolePermissions(actor, idOrName, permissions)

    override fun clearPermissions(
        actor: Actor,
        id: UUID,
    ) = gradeway.permissions.clearRolePermissions(actor, id)

    override fun clearPermissions(
        actor: Actor,
        entity: RoleEntity,
    ) = gradeway.permissions.clearRolePermissions(actor, entity)

    override fun clearPermissions(
        actor: Actor,
        idOrName: String,
    ) = gradeway.permissions.clearRolePermissions(actor, idOrName)

    override fun hasPermission(id: UUID, permission: String) =
        gradeway.permissions.hasRolePermission(id, permission)

    override fun hasPermission(entity: RoleEntity, permission: String) =
        gradeway.permissions.hasRolePermission(entity, permission)

    override fun hasPermission(idOrName: String, permission: String) =
        gradeway.permissions.hasRolePermission(idOrName, permission)

    override fun hasAnyPermissions(id: UUID, permissions: List<String>) =
        gradeway.permissions.hasRoleAnyPermissions(id, permissions)

    override fun hasAnyPermissions(entity: RoleEntity, permissions: List<String>) =
        gradeway.permissions.hasRoleAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(idOrName: String, permissions: List<String>) =
        gradeway.permissions.hasRoleAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(id: UUID, permissions: List<String>) =
        gradeway.permissions.hasRoleAllPermissions(id, permissions)

    override fun hasAllPermissions(entity: RoleEntity, permissions: List<String>) =
        gradeway.permissions.hasRoleAllPermissions(entity, permissions)

    override fun hasAllPermissions(idOrName: String, permissions: List<String>) =
        gradeway.permissions.hasRoleAllPermissions(idOrName, permissions)

    override fun getPermissions(id: UUID) =
        gradeway.permissions.getRolePermissions(id)

    override fun getPermissions(entity: RoleEntity) =
        gradeway.permissions.getRolePermissions(entity)

    override fun getPermissions(idOrName: String) =
        gradeway.permissions.getRolePermissions(idOrName)
}

/**
 * Records an update for every role matching [where] whose state [block] changed, and emits a
 * [RoleDefaultChangedEvent] for every role whose default flag flipped.
 *
 * The default flag is changed through DSL updates that bypass the DAO cache, so the matching
 * roles are loaded and snapshotted first, then refreshed from the database after [block] ran.
 */
private inline fun <T> auditDefaultChanges(
    actor: Actor,
    audit: AuditRecorder,
    where: Op<Boolean>,
    block: () -> T,
): T {
    val roles = DatabaseRoleEntity.find(where).toList()
    val before = roles.associateWith { it.snapshot() }
    val wasDefault = roles.associateWith { it.isDefault }
    val result = block()
    roles.forEach { role ->
        role.refresh()
        audit.updated(role, before.getValue(role))
        if (wasDefault.getValue(role) != role.isDefault) {
            audit.emit(RoleDefaultChangedEvent(actor, role.id.value, role.isDefault))
        }
    }
    return result
}
