FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY uploads ./uploads
ENTRYPOINT ["java", "-jar", "app.jar"]
