/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.throwables.driver

import kotlin.reflect.KClass

class DriverUnsupportedAdapterThrowable(
    val id: String,
    val adapter: KClass<*>,
    message: String = "Driver '$id' does not support ${adapter.qualifiedName}."
) : Throwable(message)
