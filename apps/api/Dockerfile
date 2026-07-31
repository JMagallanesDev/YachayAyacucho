# ============================================================
#  Backend Yachay Ayacucho - imagen de produccion
# ============================================================
#  Contenedor configurado 100% por variables de entorno (RNF-39):
#  no incluye ningun .env, de modo que la misma imagen sirve para
#  Railway, Fly.io o Koyeb cambiando solo la configuracion.
#
#  Construir (desde apps/api):
#      docker build -t yachay-api .
#  Probar:
#      docker run --rm -p 8080:8080 --env-file ../../.env yachay-api
# ============================================================

# ---------- Etapa 1: compilacion ----------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# El wrapper y el pom van primero: mientras pom.xml no cambie, Docker
# reutiliza la capa de dependencias y la compilacion es mucho mas rapida.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests \
    && mv target/*.jar target/app.jar

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sin privilegios: si alguien escapa del proceso, no es root.
RUN addgroup -S yachay && adduser -S yachay -G yachay

WORKDIR /app
COPY --from=build --chown=yachay:yachay /build/target/app.jar app.jar

USER yachay

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
