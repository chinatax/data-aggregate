FROM amazoncorretto:17-alpine
WORKDIR /app
COPY dataagg-start/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
