
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src

RUN ./mvnw -B clean package -DskipTests


# Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Hunspell: biblioteca nativa necesaria para ARM64
RUN apt-get update \
    && apt-get install -y --no-install-recommends libhunspell-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Usuario sin privilegios
RUN groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --no-create-home appuser

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
