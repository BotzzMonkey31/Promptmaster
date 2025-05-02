FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-Dspring.profiles.active=prod", "-jar", "backend.jar"]