# Etapa 1: Compilar el proyecto
FROM maven:3.8.4-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación
FROM openjdk:21-jdk-slim
# Copiamos el JAR generado (ajusta el nombre si tu .jar se llama distinto)
COPY --from=build /target/*.jar app.jar
# Exponemos el puerto que Render nos dará
EXPOSE 8080
# Comando para arrancar con optimización de memoria para el plan gratuito
ENTRYPOINT ["java", "-Xmx512m", "-Dserver.port=${PORT}", "-jar", "/app.jar"]