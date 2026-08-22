/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class ArtifactMetadata {
    private static final String DEPENDENCIES_PATH = "/_GRADEWAY/dependencies.txt";
    private static final String REPOSITORIES_PATH = "/_GRADEWAY/repositories.txt";

    private final Set<ArtifactDependency> dependencies;
    private final Set<ArtifactRepository> repositories;

    private ArtifactMetadata(Set<ArtifactDependency> dependencies, Set<ArtifactRepository> repositories) {
        this.dependencies = dependencies;
        this.repositories = repositories;
    }

    public Set<ArtifactDependency> getDependencies() {
        return Set.copyOf(dependencies);
    }

    public Set<ArtifactRepository> getRepositories() {
        return Set.copyOf(repositories);
    }

    @Nullable
    public static ArtifactMetadata load() {
        var loadedDependencies = loadDependencies();
        var loadedRepositories = loadRepositories();

        var dependencies = new ArrayList<ArtifactDependency>(loadedDependencies.size());
        var repositories = new ArrayList<ArtifactRepository>(loadedRepositories.size());

        for (var loadedDependency : loadedDependencies) {
            var coordinate = loadedDependency.split(":", 3);
            if (coordinate.length != 3) {
                continue;
            }

            var dependency = new ArtifactDependency(coordinate[0], coordinate[1], coordinate[2]);
            dependencies.add(dependency);
        }

        for (var entry : loadedRepositories.entrySet()) {
            var repository = new ArtifactRepository(entry.getKey(), entry.getValue());
            repositories.add(repository);
        }

        if (dependencies.isEmpty() && repositories.isEmpty()) {
            return null;
        }

        return new ArtifactMetadata(Set.copyOf(dependencies), Set.copyOf(repositories));
    }

    private static List<String> loadDependencies() {
        return loadLines(DEPENDENCIES_PATH);
    }

    private static Map<String, String> loadRepositories() {
        var repos = new HashMap<String, String>();
        var lines = loadLines(REPOSITORIES_PATH);

        for (var line : lines) {
            int equalsIndex = line.indexOf('=');
            if (equalsIndex != -1) {
                String id = line.substring(0, equalsIndex).trim();
                String url = line.substring(equalsIndex + 1).trim();
                if (!id.isEmpty() && !url.isEmpty()) {
                    repos.put(id, url);
                }
            }
        }

        return repos;
    }

    private static List<String> loadLines(String resourcePath) {
        var lines = new ArrayList<String>();
        try (var resource = ArtifactMetadata.class.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                return lines;
            }

            var reader = new BufferedReader(new InputStreamReader(resource));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException exception) {
            return lines;
        }
        return lines;
    }
}
