# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --shell /usr/sbin/nologin kanban
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# logback-spring.xml grava em logs/kanban-backend.log (caminho relativo ao working dir) — sem o
# diretório pré-criado e pertencendo ao usuário não-root, o boot falha (RollingFileAppender não
# consegue criar o diretório pai).
RUN mkdir logs && chown kanban:kanban /app/logs app.jar
USER kanban
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
