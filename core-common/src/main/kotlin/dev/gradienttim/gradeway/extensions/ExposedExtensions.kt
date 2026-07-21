/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import org.jetbrains.exposed.v1.core.*

infix fun Expression<*>.likeAsStr(pattern: String): Op<Boolean> =
    castTo(VarCharColumnType()) like pattern

infix fun Expression<*>.eqAsStr(value: String): Op<Boolean> =
    castTo(VarCharColumnType()) eq value
