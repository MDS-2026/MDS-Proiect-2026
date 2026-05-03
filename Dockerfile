# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Make wrapper executable
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline
COPY src ./src
# Skip incremental compilation to avoid "hashes is null" error
RUN ./mvnw clean package -DskipTests -Dorg.slf4j.simpleLogger.defaultLogLevel=warn

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
