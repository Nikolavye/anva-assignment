# Use an official Maven image to build the application (Build Stage)
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
# Build the application without running or compiling tests in the image build.
RUN mvn -B clean package -Dmaven.test.skip=true

# Use an official lightweight Java runtime image (Run Stage)
FROM eclipse-temurin:25-jre
WORKDIR /app
# Copy the built jar from the build stage
COPY --from=build /app/target/wordfrequency-0.0.1-SNAPSHOT.jar app.jar
# Expose the application port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
