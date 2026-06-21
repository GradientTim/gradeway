/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

interface Environment {
    fun int(vararg names: String): Int?
    fun long(vararg names: String): Long?
    fun double(vararg names: String): Double?
    fun string(vararg names: String): String?
    fun boolean(vararg names: String): Boolean?

    fun intRequired(vararg names: String): Int
    fun longRequired(vararg names: String): Long
    fun doubleRequired(vararg names: String): Double
    fun stringRequired(vararg names: String): String
    fun booleanRequired(vararg names: String): Boolean

    fun intDefault(vararg names: String, default: Int): Int = int(*names) ?: default
    fun longDefault(vararg names: String, default: Long): Long = long(*names) ?: default
    fun doubleDefault(vararg names: String, default: Double): Double = double(*names) ?: default
    fun stringDefault(vararg names: String, default: String): String = string(*names) ?: default
    fun booleanDefault(vararg names: String, default: Boolean): Boolean = boolean(*names) ?: default
}
