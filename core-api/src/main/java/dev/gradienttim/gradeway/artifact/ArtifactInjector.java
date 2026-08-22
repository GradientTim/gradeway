/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

/**
 * An interface designed for injecting artifacts into a specific runtime or environment.
 * Implementations of this interface define the process for adding an artifact,
 * represented by a file path, to the runtime classpath or other relevant locations.
 */
public interface ArtifactInjector {
    /**
     * Injects the specified file path into the runtime or environment.
     * The exact behavior of this method is defined by the implementation,
     * which may involve adding the file to the runtime classpath or performing
     * other integration steps.
     *
     * @param path the file path to be injected; must not be null
     * @return true if the injection was successful; false otherwise
     */
    boolean inject(@NonNull Path path);
}
