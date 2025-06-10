# Use the official Maven image to build the project
FROM maven:3.9.6-openjdk-17 AS build
WORKDIR /workspace

# Copy the Maven project files to the container
COPY pom.xml .
COPY src ./src

# Package the application to generate the JAR file
RUN mvn clean package -DskipTests

# Use the official OpenJDK image to run the application
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /workspace/target/be-0.0.1-SNAPSHOT.war be.war

# Expose the port the app runs on
EXPOSE 8080

# Set the entrypoint to run the JAR
ENTRYPOINT ["java", "-jar", "be.war"]
