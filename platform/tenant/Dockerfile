FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/*.jar app.jar
EXPOSE 8212
ENTRYPOINT ["java", "-jar", "app.jar"]
