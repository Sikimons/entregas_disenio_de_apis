# --- Etapa de build ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cachear dependencias primero
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Etapa de runtime ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/target/delivery-pin-middleware.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
