/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.paper

import dev.gradienttim.gradeway.artifact.ArtifactMetadata
import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class GradewayPluginLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        val metadata = ArtifactMetadata.load() ?: return
        val resolver = MavenLibraryResolver()

        metadata.repositories.forEach { repository ->
            resolver.addRepository(RemoteRepository.Builder(repository.id, "default", repository.url).build())
        }

        metadata.dependencies.forEach { dependency ->
            resolver.addDependency(Dependency(DefaultArtifact(dependency.formatCoordinate()), null))
        }

        classpathBuilder.addLibrary(resolver)
    }
}
