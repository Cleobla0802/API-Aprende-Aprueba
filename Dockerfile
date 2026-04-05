# 1. Usamos una imagen de Java 21 (o la que uses) para compilar
FROM maven:3.8.5-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. Usamos una imagen más ligera para ejecutar
FROM openjdk:21-jdk-slim
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]