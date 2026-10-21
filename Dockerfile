
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/payanam-travel-1.0.0.jar app.jar
COPY src/main/resources/static ./src/main/resources/static
COPY data ./data

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
