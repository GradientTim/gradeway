/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.artifact;

import dev.gradienttim.gradeway.artifact.ArtifactInjector;
import dev.gradienttim.gradeway.velocity.GradewayPlugin;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public class VelocityArtifactInjector implements ArtifactInjector {
    protected GradewayPlugin plugin;

    public VelocityArtifactInjector(GradewayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean inject(@NonNull Path path) {
        try {
            plugin.getServer().getPluginManager().addToClasspath(plugin, path);
            return true;
        } catch (UnsupportedOperationException exception) {
            return false;
        }
    }
}
