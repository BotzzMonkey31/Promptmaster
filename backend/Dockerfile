FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "backend.jar"]