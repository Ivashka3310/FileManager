# Сборка
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY gradle ./gradle

RUN sed -i '/:app/d' settings.gradle.kts

COPY shared/build.gradle.kts ./shared/
COPY server/build.gradle.kts ./server/

RUN ./gradlew :server:dependencies --no-daemon

COPY shared ./shared
COPY server ./server
RUN ./gradlew :server:installDist --no-daemon

# Создание минимального финального образа
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/server/build/install/server /app/

USER appuser

EXPOSE 8081

CMD ["./bin/server"]