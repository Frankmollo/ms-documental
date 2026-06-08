# Etapa 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

# Copiar el wrapper de maven y el pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos de ejecución al wrapper
RUN chmod +x mvnw

# Descargar las dependencias
RUN ./mvnw dependency:go-offline

# Copiar el código fuente y compilar
COPY src src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Run
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
WORKDIR /app

# Copiar el jar compilado desde la etapa de build
COPY --from=build /workspace/app/target/*.jar app.jar

EXPOSE 8080

# Punto de entrada para iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
