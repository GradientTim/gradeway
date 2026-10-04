/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
        List<String> loadedDependencies = loadDependencies();
        Map<String, String> loadedRepositories = loadRepositories();

        ArrayList<ArtifactDependency> dependencies = new ArrayList<>(loadedDependencies.size());
        ArrayList<ArtifactRepository> repositories = new ArrayList<>(loadedRepositories.size());

        for (String loadedDependency : loadedDependencies) {
            String[] coordinate = loadedDependency.split(":", 3);
            if (coordinate.length != 3) {
                continue;
            }

            ArtifactDependency dependency = new ArtifactDependency(coordinate[0], coordinate[1], coordinate[2]);
            dependencies.add(dependency);
        }

        for (Map.Entry<String, String> entry : loadedRepositories.entrySet()) {
            ArtifactRepository repository = new ArtifactRepository(entry.getKey(), entry.getValue());
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
        HashMap<String, String> repos = new HashMap<>();
        List<String> lines = loadLines(REPOSITORIES_PATH);

        for (String line : lines) {
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
        ArrayList<String> lines = new ArrayList<>();
        try (InputStream resource = ArtifactMetadata.class.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                return lines;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
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
