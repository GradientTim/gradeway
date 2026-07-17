/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.constants

object CacheConstants {
    const val ENTITY_MAX_SIZE: Long = 10_000
    const val ENTITY_WRITE_DURATION: Long = 5
    const val ENTITY_PERMISSIONS_MAX_SIZE: Long = ENTITY_MAX_SIZE
    const val ENTITY_PERMISSIONS_WRITE_DURATION: Long = 10
    const val ENTITY_EFFECTIVE_WEIGHT_MAX_SIZE: Long = ENTITY_MAX_SIZE
    const val ENTITY_EFFECTIVE_WEIGHT_WRITE_DURATION: Long = 30
    const val ENTITY_EFFECTIVE_PERMISSIONS_MAX_SIZE: Long = ENTITY_MAX_SIZE
    const val ENTITY_EFFECTIVE_PERMISSIONS_WRITE_DURATION: Long = 30
}
