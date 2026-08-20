package dev.gradienttim.gradeway.paper;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GradewayPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries pluginLibraries = load();
        pluginLibraries.asDependencies().forEach(resolver::addDependency);
        pluginLibraries.asRepositories().forEach(resolver::addRepository);

        if (pluginLibraries.repositories == null || pluginLibraries.repositories.isEmpty()) {
            resolver.addRepository(
                    new RemoteRepository.Builder(
                            "central",
                            "default",
                            MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
                    ).build()
            );
        }

        classpathBuilder.addLibrary(resolver);
    }

    public PluginLibraries load() {
        try (var inputStream = getClass().getResourceAsStream("/paper-libraries.json")) {
            assert inputStream != null;
            return new Gson().fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), PluginLibraries.class);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private record PluginLibraries(Map<String, String> repositories, List<String> dependencies) {
        public Stream<Dependency> asDependencies() {
            if (dependencies == null) {
                return Stream.empty();
            }
            return dependencies.stream()
                    .map(dependency -> new Dependency(new DefaultArtifact(dependency), null));
        }

        public Stream<RemoteRepository> asRepositories() {
            if (repositories == null) {
                return Stream.empty();
            }
            return repositories.entrySet().stream()
                    .filter(repository -> !repository.getKey().equals("central"))
                    .map(repository -> new RemoteRepository.Builder(repository.getKey(), "default", repository.getValue()).build());
        }
    }
}
