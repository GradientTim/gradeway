/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

enum class GradewayState(
    val allowLoad: Boolean,
    val allowUnload: Boolean,
) {
    LOADED(false, true),
    UNLOADED(true, false),
    PROCESSING(false, false),
}
