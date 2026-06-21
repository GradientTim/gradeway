/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import dev.gradienttim.gradeway.ksp.resolvers.DriverResolver

class GradewayProcessor(
    environment: SymbolProcessorEnvironment,
): SymbolProcessor {
    private val driverResolver = DriverResolver(environment)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        driverResolver.process(resolver)
        return emptyList()
    }
}
