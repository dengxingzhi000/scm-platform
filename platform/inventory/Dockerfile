FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/platform-inventory-service-*-exec.jar app.jar
EXPOSE 8202
ENTRYPOINT ["java", "-jar", "app.jar"]
