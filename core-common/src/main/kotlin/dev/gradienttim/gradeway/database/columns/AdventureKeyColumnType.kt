/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.columns

import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class AdventureKeyColumnType : ColumnType<Key>() {
    override fun sqlType(): String = "TEXT"

    override fun valueToDB(value: Key?): Any? {
        if (value == null) return null
        return value.asString()
    }

    override fun valueFromDB(value: Any): Key? {
        if (value !is String) return null
        return Key.key(value)
    }
}

fun Table.adventureKey(name: String): Column<Key> = registerColumn(name, AdventureKeyColumnType())
