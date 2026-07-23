/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.Gradeway
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URLClassLoader

abstract class Driver : KoinComponent {
    val gradeway: Gradeway<*> by inject()

    lateinit var config: DriverConfig
    var classLoader: URLClassLoader? = null

    open fun load() = Unit
    open fun unload() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Driver) return false
        return config == other.config
    }

    override fun hashCode(): Int = config.hashCode()
}
