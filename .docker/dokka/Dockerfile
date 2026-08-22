# Builds the aggregated Dokka HTML site (root `./gradlew dokkaGenerate`, see build.gradle.kts,
# output: build/dokka/html) and serves it as static files.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew dokkaGenerate --no-daemon --no-configuration-cache

FROM nginx:alpine AS serve
COPY --from=build /app/build/dokka/html /usr/share/nginx/html
EXPOSE 80
