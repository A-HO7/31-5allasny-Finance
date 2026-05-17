FROM eclipse-temurin:25.0.2_10-jdk
COPY target/api-gateway-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
