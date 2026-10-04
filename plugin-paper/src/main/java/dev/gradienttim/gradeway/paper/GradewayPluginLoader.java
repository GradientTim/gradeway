package dev.gradienttim.gradeway.paper;

import dev.gradienttim.gradeway.artifact.ArtifactMetadata;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NonNull;

public class GradewayPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        ArtifactMetadata metadata = ArtifactMetadata.load();
        if (metadata == null) {
            System.out.println("No artifact metadata found");
            return;
        }
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        metadata.getRepositories().forEach(repository -> {
            resolver.addRepository(new RemoteRepository.Builder(repository.id(), "default", repository.url()).build());
        });

        metadata.getDependencies().forEach(dependency -> {
            resolver.addDependency(new Dependency(new DefaultArtifact(dependency.formatCoordinate()), null));
        });

        classpathBuilder.addLibrary(resolver);
    }
}
