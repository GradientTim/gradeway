/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.platform.stores

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import dev.gradienttim.gradeway.utilities.Store
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.util.*

class BukkitPersistentNotificationStore : Store<UUID, Boolean> {
    override fun set(key: UUID, value: Boolean): Either<Store.SetError, Boolean> = either {
        val player = Bukkit.getPlayer(key) ?: return@either false
        catch({
            player.persistentDataContainer.set(KEY, PersistentDataType.BOOLEAN, value)
        }) { throwable -> raise(Store.SetError.Unexpected(throwable)) }
        true
    }

    override fun unset(key: UUID): Either<Store.UnsetError, Boolean> = either {
        val player = Bukkit.getPlayer(key) ?: return@either false
        catch({
            player.persistentDataContainer.remove(KEY)
        }) { throwable -> raise(Store.UnsetError.Unexpected(throwable)) }
        true
    }

    override fun get(key: UUID): Boolean? {
        val player = Bukkit.getPlayer(key) ?: return false
        return runCatching {
            player.persistentDataContainer.get(KEY, PersistentDataType.BOOLEAN)
        }.getOrNull()
    }

    override fun clear(): Either<Store.ClearError, Unit> = either {
        raise(Store.ClearError.Unexpected(Throwable("Unable to clear data on persistent bukkit store.")))
    }

    companion object {
        private val KEY = NamespacedKey.fromString("gradeway:notification_store")!!
    }
}
