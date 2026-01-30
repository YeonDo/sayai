FROM amazoncorretto:21-al2023-jdk
# The application's jar file
ARG JAR_FILE=build/libs/record-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} app.jar
# Make port 8080 available to the world outside this container]
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java","-jar","/app.jar"]
