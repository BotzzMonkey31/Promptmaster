FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_DATASOURCE_URL=jdbc:sqlserver://promptmaster-sql-server.database.windows.net:1433;database=mydatabase;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;
ENV SPRING_DATASOURCE_USERNAME=enriqueRoot@promptmaster-sql-server
ENV SPRING_DATASOURCE_PASSWORD=TaMaRa1726
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-Dspring.profiles.active=prod", "-jar", "backend.jar"]