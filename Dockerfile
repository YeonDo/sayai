FROM openjdk:17-jdk-slim
# The application's jar file
ARG JAR_FILE=target/record-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java","-jar","/app.jar"]