FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/scm-audit-service-*-exec.jar app.jar
EXPOSE 8210
ENTRYPOINT ["java", "-jar", "app.jar"]
