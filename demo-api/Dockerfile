# Runtime image only — build the JAR first:
#   cd demo-api && mvn -DskipTests package
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/minio-demo-api-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
