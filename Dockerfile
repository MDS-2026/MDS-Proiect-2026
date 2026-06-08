# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Make wrapper executable
RUN chmod +x ./mvnw
# Batch mode + no transfer progress keeps logs clean and avoids stalling on progress output
RUN ./mvnw -B --no-transfer-progress dependency:go-offline
COPY src ./src
# Skip incremental compilation to avoid "hashes is null" error
RUN ./mvnw -B --no-transfer-progress clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
