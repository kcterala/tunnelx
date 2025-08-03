# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Download dependencies and build the JAR
RUN mvn clean package dependency:copy-dependencies -DoutputDirectory=target/dependency

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy app JAR and dependencies
COPY --from=build /app/target/tunnel-server-1.0-SNAPSHOT.jar ./app.jar
COPY --from=build /app/target/dependency ./lib


# Expose port
EXPOSE 8080

# Set classpath: main JAR + all dependencies
ENV CLASSPATH=app.jar:lib/*

# Run the application by main class (not using -jar because it ignores classpath)
ENTRYPOINT ["java", "dev.kcterala.tunnelx.TunnelX"]
