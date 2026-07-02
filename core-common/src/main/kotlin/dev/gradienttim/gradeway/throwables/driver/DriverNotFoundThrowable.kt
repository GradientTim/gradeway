/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.throwables.driver

class DriverNotFoundThrowable(
    val id: String,
    message: String = "No database driver found with id '$id'."
) : Throwable(message)
