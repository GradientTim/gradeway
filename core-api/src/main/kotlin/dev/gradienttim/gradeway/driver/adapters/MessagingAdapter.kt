/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.adapters

import dev.gradienttim.gradeway.platform.Environment

interface MessagingAdapter {
    fun open(environment: Environment)
    fun close()
}
