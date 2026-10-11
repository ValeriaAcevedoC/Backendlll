# Construir desde la raiz: docker build -f clientes-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -pl clientes-service -am package -DskipTests
RUN cp clientes-service/target/*.jar /app/servicio.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/servicio.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
