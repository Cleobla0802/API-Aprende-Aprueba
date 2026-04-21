# Etapa 1: Compilar el proyecto (Usando Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación
FROM eclipse-temurin:21-jdk-jammy
# Copiamos el JAR generado
COPY --from=build /target/*.jar app.jar

# Exponemos el puerto
EXPOSE 8080

# Comando para arrancar
# Usamos -Dserver.port para que Spring Boot use obligatoriamente el puerto de Render
ENTRYPOINT ["java", "-Xmx512m", "-Dserver.port=${PORT}", "-jar", "/app.jar"]