/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.throwables

class GradewayAlreadyUnloadedThrowable(
    message: String = "Gradeway cannot be unloaded as it is already unloaded."
) : Throwable(message)
