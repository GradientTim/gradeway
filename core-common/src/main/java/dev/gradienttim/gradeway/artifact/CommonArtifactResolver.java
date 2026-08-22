/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.artifact;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CommonArtifactResolver {
    private static final int HTTP_OK = 200;
    private static final int BUFFER_SIZE = 8192;
    private static final int THREAD_POOL_SIZE = 8;

    private final Path directory;
    private final ArtifactInjector injector;
    private final Consumer<String> logInfo;
    private final Consumer<String> logWarn;
    private final Consumer<String> logError;
    private final Set<ArtifactRepository> repositories;
    private final Set<ArtifactDependency> dependencies;

    private final HttpClient httpClient;
    private final MessageDigest sha1Digest;

    protected CommonArtifactResolver(@NonNull Builder builder) throws IOException, NoSuchAlgorithmException {
        this.directory = builder.directory;
        this.logInfo = builder.logInfo;
        this.logWarn = builder.logWarn;
        this.logError = builder.logError;
        this.injector = builder.injector;
        this.repositories = builder.repositories;
        this.dependencies = builder.dependencies;
        this.httpClient = HttpClient.newHttpClient();

        this.sha1Digest = MessageDigest.getInstance("SHA-1");

        if (!Files.exists(directory)) {
            Files.createDirectory(directory);
        }
    }

    public void resolve(
            @NonNull Consumer<Void> onSuccess,
            @NonNull Consumer<Throwable> onFailure
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        executor.submit(() -> {
            try {
                Files.createDirectories(directory);

                var futures = dependencies.stream()
                        .map(dependency -> java.util.concurrent.CompletableFuture.supplyAsync(
                                () -> resolveDependency(dependency),
                                executor
                        ))
                        .toList();

                var resolvedPaths = new HashSet<Path>();
                for (var future : futures) {
                    try {
                        var result = future.get();
                        if (result != null) {
                            resolvedPaths.add(result);
                        }
                    } catch (Exception exception) {
                        logWarn.accept("Failed to resolve dependency: " + exception.getMessage());
                    }
                }

                for (var path : resolvedPaths) {
                    if (!injector.inject(path)) {
                        logWarn.accept("Failed to inject dependency '" + path + "'");
                    }
                }

                onSuccess.accept(null);
            } catch (Throwable throwable) {
                onFailure.accept(throwable);
            } finally {
                try {
                    executor.shutdown();
                    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException exception) {
                    executor.shutdownNow();
                }
                httpClient.close();
            }
        });
    }

    private Path resolveDependency(ArtifactDependency dependency) {
        var dependencyFile = directory.resolve(dependency.formatFileName());
        if (Files.exists(dependencyFile)) {
            return dependencyFile;
        }

        for (var repository : repositories) {
            var dependencyUrl = repository.buildUrl(dependency);
            var expectedChecksum = fetchChecksum(dependencyUrl);

            if (expectedChecksum != null) {
                var result = downloadDependency(dependency, dependencyUrl, dependencyFile, expectedChecksum);
                if (result != null) {
                    return result;
                }
            }
        }

        logWarn.accept("Dependency '" + dependency.formatCoordinate() +
                "' not found in any repository");
        return null;
    }


    private Path downloadDependency(
            ArtifactDependency dependency,
            String dependencyUrl,
            Path dependencyFile,
            String expectedChecksum
    ) {
        if (dependencyUrl == null) {
            return null;
        }

        try {
            logInfo.accept("Downloading dependency '" + dependency.formatCoordinate() + "'...");

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(dependencyUrl))
                    .GET()
                    .build();

            var response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() == HTTP_OK) {
                return handleSuccessfulDownload(dependency, dependencyFile, response, expectedChecksum);
            } else {
                logWarn.accept("Failed to resolve dependency '" + dependency.formatCoordinate() +
                        "' from '" + dependencyUrl + "': HTTP status code " + response.statusCode());
                return null;
            }
        } catch (Exception exception) {
            logWarn.accept("Failed to resolve dependency '" + dependency.formatCoordinate() +
                    "': " + exception.getMessage());
            return null;
        }
    }

    private Path handleSuccessfulDownload(
            ArtifactDependency dependency,
            Path dependencyFile,
            HttpResponse<InputStream> response,
            String expectedChecksum
    ) {
        try {
            Files.copy(response.body(), dependencyFile);
            logInfo.accept("Downloaded dependency '" + dependency.formatCoordinate() +
                    "' into '" + dependencyFile + "'");

            if (expectedChecksum != null) {
                var downloadedChecksum = sha1Hex(dependencyFile);
                if (downloadedChecksum == null ||
                        !downloadedChecksum.equalsIgnoreCase(expectedChecksum)) {
                    logError.accept("Downloaded dependency '" + dependency.formatCoordinate() +
                            "' has hash mismatch, skipping injection");
                    return null;
                }
            }

            return dependencyFile;
        } catch (IOException exception) {
            logError.accept("Failed to write dependency '" + dependency.formatCoordinate() +
                    "' to '" + dependencyFile + "': " + exception.getMessage());
            return null;
        }
    }

    private String fetchChecksum(String artifactUrl) {
        try {
            var checksumUrl = artifactUrl + ".sha1";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(checksumUrl))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                return response.body().trim();
            }
            return null;
        } catch (Exception exception) {
            logWarn.accept("Failed to fetch checksum from '" + artifactUrl + ".sha1': " +
                    exception.getMessage());
            return null;
        }
    }

    private String sha1Hex(Path path) {
        try {
            var buffer = new byte[BUFFER_SIZE];

            try (var input = Files.newInputStream(path)) {
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    sha1Digest.update(buffer, 0, bytesRead);
                }
            }

            return bytesToHex(sha1Digest.digest());
        } catch (IOException exception) {
            logError.accept("Failed to compute SHA-1 for '" + path + "': " + exception.getMessage());
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        var stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public static class Builder {
        protected Path directory;
        protected ArtifactInjector injector;

        protected Consumer<String> logInfo = message -> {
        };
        protected Consumer<String> logWarn = message -> {
        };
        protected Consumer<String> logError = message -> {
        };

        protected Set<ArtifactRepository> repositories = new HashSet<>();
        protected Set<ArtifactDependency> dependencies = new HashSet<>();

        public Builder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public Builder injector(ArtifactInjector injector) {
            this.injector = injector;
            return this;
        }

        public Builder logInfo(@NonNull Consumer<String> logInfo) {
            this.logInfo = logInfo;
            return this;
        }

        public Builder logWarn(@NonNull Consumer<String> logWarn) {
            this.logWarn = logWarn;
            return this;
        }

        public Builder logError(@NonNull Consumer<String> logError) {
            this.logError = logError;
            return this;
        }

        public Builder repositories(Set<ArtifactRepository> repositories) {
            this.repositories = new HashSet<>(repositories);
            return this;
        }

        public Builder dependencies(Set<ArtifactDependency> dependencies) {
            this.dependencies = new HashSet<>(dependencies);
            return this;
        }

        public Builder addRepository(ArtifactRepository repository) {
            this.repositories.add(repository);
            return this;
        }

        public Builder addDependency(ArtifactDependency dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public @NonNull CommonArtifactResolver build() throws IOException, NoSuchAlgorithmException {
            if (directory == null || injector == null) {
                throw new IllegalStateException("Directory and injector must be set");
            }
            return new CommonArtifactResolver(this);
        }
    }
}
