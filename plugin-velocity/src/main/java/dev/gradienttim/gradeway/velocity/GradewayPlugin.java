package dev.gradienttim.gradeway.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.gradienttim.gradeway.BuildInfo;
import dev.gradienttim.gradeway.artifact.ArtifactMetadata;
import dev.gradienttim.gradeway.artifact.CommonArtifactResolver;
import dev.gradienttim.gradeway.velocity.artifact.VelocityArtifactInjector;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "gradeway",
        name = "Gradeway",
        authors = {"GradientTim"},
        version = BuildInfo.VERSION
)
public class GradewayPlugin {
    final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private GradewayVelocityInstance gradewayInstance = null;

    @Inject
    public GradewayPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        var metadata = ArtifactMetadata.load();
        if (metadata == null) {
            logger.warn("Failed to load metadata");
            return;
        }

        try {
            var resolver = CommonArtifactResolver.builder()
                    .logInfo(logger::info)
                    .logWarn(logger::warn)
                    .logError(logger::error)
                    .directory(dataDirectory.resolve("dependencies"))
                    .injector(new VelocityArtifactInjector(this))
                    .repositories(metadata.getRepositories())
                    .dependencies(metadata.getDependencies())
                    .build();

            resolver.resolve(success -> {
                gradewayInstance = new GradewayVelocityInstance(this, logger, dataDirectory);
                gradewayInstance.initialize();
            }, Throwable::printStackTrace);
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (gradewayInstance != null) {
            gradewayInstance.terminate();
        }
    }

    public ProxyServer getServer() {
        return server;
    }
}
