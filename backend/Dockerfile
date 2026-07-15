# syntax=docker/dockerfile:1
# =====================================================================
# Backend parking — imagen multi-stage.
#   Stage build  : compila el WAR con Maven + JDK 21 (tests omitidos aqui;
#                  el Quality Gate corre en CI, no en la imagen de despliegue).
#   Stage runtime: Tomcat 10.1 sobre JDK 21. El WAR se despliega como
#                  parking-api.war -> contexto /parking-api (coincide con
#                  server.servlet.context-path). El starter-tomcat es `provided`
#                  en el pom, asi que el runtime lo aporta este Tomcat.
# =====================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias: copiamos primero el pom y resolvemos offline.
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline

# Codigo fuente y empaquetado del WAR.
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---------- Stage 2: runtime ----------
FROM tomcat:10.1-jdk21-temurin AS runtime

# curl para el HEALTHCHECK; Tomcat sin las webapps de ejemplo (ROOT, docs...).
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf ${CATALINA_HOME}/webapps/*

# El nombre del WAR fija el context-path a /parking-api.
COPY --from=build /build/target/parking-api.war ${CATALINA_HOME}/webapps/parking-api.war

EXPOSE 8080

# Healthcheck del contenedor: el actuator responde en /parking-api/actuator/health.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD ["sh", "-c", "curl -fsS http://localhost:8080/parking-api/actuator/health | grep -q '\"status\":\"UP\"' || exit 1"]

CMD ["catalina.sh", "run"]
