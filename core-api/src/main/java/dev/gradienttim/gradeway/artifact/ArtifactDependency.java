/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

/**
 * Represents an artifact dependency with attributes for a group, name, and version.
 * This record is used to define a uniquely identifiable artifact and provides methods
 * for formatting the artifact's file name, coordinates, and URL.
 */
public record ArtifactDependency(String group, String name, String version) {
    public String formatFileName() {
        return String.format("%s-%s-%s.jar", group, name, version);
    }

    public String formatCoordinate() {
        return String.format("%s:%s:%s", group, name, version);
    }

    public String formatUrl() {
        return String.format("%s/%s/%s/%s-%s.jar",
                group.replace(".", "/"), name, version, name, version);
    }
}
