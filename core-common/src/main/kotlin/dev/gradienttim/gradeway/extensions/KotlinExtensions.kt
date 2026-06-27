/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import java.util.*

@Suppress("ForbiddenComment")
// TODO: replace it with a regexp pattern.
fun String.isUuid(): Boolean = runCatching {
    UUID.fromString(this)
}.isSuccess
