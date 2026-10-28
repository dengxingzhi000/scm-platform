FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/scm-inventory-service-*-exec.jar app.jar
EXPOSE 8202
ENTRYPOINT ["java", "-jar", "app.jar"]
