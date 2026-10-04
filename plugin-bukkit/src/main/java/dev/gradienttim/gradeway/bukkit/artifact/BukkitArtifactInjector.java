/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.artifact;

import dev.gradienttim.gradeway.artifact.ArtifactInjector;
import dev.gradienttim.gradeway.bukkit.GradewayPlugin;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class BukkitArtifactInjector implements ArtifactInjector {
    protected GradewayPlugin plugin;

    public BukkitArtifactInjector(GradewayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean inject(@NonNull Path path) {
        try {
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            if (!(classLoader instanceof URLClassLoader urlClassLoader)) {
                return false;
            }

            URL jarUrl = path.toUri().toURL();
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            addUrlMethod.setAccessible(true);
            addUrlMethod.invoke(urlClassLoader, jarUrl);

            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to inject artifact: " + exception.getMessage());
            return false;
        }
    }
}
