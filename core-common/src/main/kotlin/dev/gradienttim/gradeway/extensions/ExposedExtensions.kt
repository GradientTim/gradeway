/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

private const val INTEGRITY_CONSTRAINT_VIOLATION_SQL_STATE_CLASS = "23"
private const val SQLITE_CONSTRAINT_ERROR_CODE = 19

fun Throwable.isIntegrityConstraintViolation(): Boolean =
    generateSequence(this) { it.cause }
        .filterIsInstance<SQLException>()
        .any { exception ->
            exception is SQLIntegrityConstraintViolationException ||
                    exception.sqlState?.startsWith(INTEGRITY_CONSTRAINT_VIOLATION_SQL_STATE_CLASS) == true ||
                    (exception.sqlState == null && exception.errorCode == SQLITE_CONSTRAINT_ERROR_CODE)
        }

infix fun Expression<*>.likeAsStr(pattern: String): Op<Boolean> =
    castTo(VarCharColumnType()) like pattern

infix fun Expression<*>.eqAsStr(value: String): Op<Boolean> =
    castTo(VarCharColumnType()) eq value

infix fun <T : Comparable<T>> Column<EntityID<T>>.eqId(value: T): Op<Boolean> {
    @Suppress("UNCHECKED_CAST")
    val idTable = table as IdTable<T>
    return eq(EntityID(value, idTable))
}
