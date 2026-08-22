/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

/**
 * Represents a repository for storing and retrieving artifacts.
 * This record encapsulates the repository's unique identifier and URL
 * necessary for accessing its resources.
 *
 * @param id  the unique identifier for the repository
 * @param url the base URL for the repository
 */
public record ArtifactRepository(String id, String url) {
    public String buildUrl(ArtifactDependency dependency) {
        return String.format("%s/%s", url, dependency.formatUrl());
    }
}
