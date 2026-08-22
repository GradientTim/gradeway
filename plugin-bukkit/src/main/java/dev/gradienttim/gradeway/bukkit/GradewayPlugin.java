/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit;

import dev.gradienttim.gradeway.artifact.ArtifactMetadata;
import dev.gradienttim.gradeway.artifact.CommonArtifactResolver;
import dev.gradienttim.gradeway.bukkit.artifact.BukkitArtifactInjector;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

public class GradewayPlugin extends JavaPlugin {
    private GradewayBukkitInstance gradewayInstance = null;

    @Override
    public void onEnable() {
        if (!checkModulesOpen()) {
            getLogger().severe("Plugin cannot start without the '--add-opens java.base/java.net=ALL-UNNAMED' JVM flag");
            return;
        }

        var metadata = ArtifactMetadata.load();
        if (metadata == null) {
            getLogger().warning("Failed to load metadata");
            return;
        }

        try {
            var resolver = CommonArtifactResolver.builder()
                    .logInfo(message -> getLogger().info(message))
                    .logWarn(message -> getLogger().warning(message))
                    .logError(message -> getLogger().severe(message))
                    .directory(getDataFolder().toPath().resolve("dependencies"))
                    .injector(new BukkitArtifactInjector(this))
                    .repositories(metadata.getRepositories())
                    .dependencies(metadata.getDependencies())
                    .build();

            resolver.resolve(success -> {
                gradewayInstance = new GradewayBukkitInstance(this, getLogger(), getDataFolder());
                gradewayInstance.initialize();
            }, Throwable::printStackTrace);
        } catch (Exception exception) {
            getLogger().severe(exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (gradewayInstance != null) {
            gradewayInstance.terminate();
        }
    }

    private boolean checkModulesOpen() {
        try {
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", java.net.URL.class);
            addUrlMethod.setAccessible(true);
            return true;
        } catch (InaccessibleObjectException ignored) {
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }
}
