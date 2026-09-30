# --- Etapa de build ---
FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /app

# Cachear dependencias primero
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Etapa de runtime ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# curl solo para el healthcheck de docker-compose (/actuator/health)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# UID/GID fijos y numericos: permiten verificar "no root" desde fuera (compose, CI,
# runAsNonRoot de Kubernetes), que con un nombre de usuario no pueden comprobar.
RUN groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --create-home appuser
USER 10001:10001

# CA de AWS RDS para sslmode=verify-full (pgjdbc la busca por defecto en ~/.postgresql/root.crt)
RUN mkdir -p /home/appuser/.postgresql \
    && curl -fsSL https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem \
       -o /home/appuser/.postgresql/root.crt

COPY --from=build /app/target/delivery-pin-middleware.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
