/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import arrow.core.Either

interface Loadable {
    fun load(): Either<Throwable, Unit>
}
