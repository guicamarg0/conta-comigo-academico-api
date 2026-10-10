FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /app/target/gestao-servicos-api-0.0.1-SNAPSHOT.jar /app/api.jar
USER app
ENV SPRING_PROFILES_ACTIVE=academico
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
