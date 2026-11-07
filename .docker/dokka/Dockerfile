# Builds the aggregated Dokka HTML site (root `./gradlew dokkaGenerate`, see build.gradle.kts,
# output: build/dokka/html) and serves it as static files under `/$DOKKA_PATH/`, so several
# projects can share one host (e.g. https://dokka.gradienttim.dev/gradeway/).
ARG DOKKA_PATH=gradeway

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew dokkaGenerate --no-daemon --no-configuration-cache

FROM nginx:alpine AS serve
ARG DOKKA_PATH
COPY .docker/dokka/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build/dokka/html /usr/share/nginx/html/${DOKKA_PATH}
EXPOSE 80
