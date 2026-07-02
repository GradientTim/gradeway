/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.throwables

class GradewayAlreadyLoadedThrowable(
    message: String = "Gradeway cannot be loaded as it was already loaded."
) : Throwable(message)
