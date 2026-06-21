/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class GradewayProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = GradewayProcessor(environment)
}
