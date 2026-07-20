FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace/app

# Copy gradle files first to cache dependencies
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (this layer will be cached unless dependencies change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application, skipping tests to speed up docker build
RUN ./gradlew bootJar -x test --no-daemon

# Create a lightweight runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Add a non-root user for security (Ubuntu/Debian syntax)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
