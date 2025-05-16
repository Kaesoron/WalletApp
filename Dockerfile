# Этап 1: Сборка приложения с Maven + JDK 21
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем pom и исходники
COPY pom.xml .
COPY src ./src

# Собираем проект (без тестов для ускорения)
RUN mvn clean package -DskipTests

# Этап 2: Минимальный runtime образ
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Скачиваем dockerize
ENV DOCKERIZE_VERSION v0.6.1
RUN curl -L https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-linux-amd64-${DOCKERIZE_VERSION}.tar.gz \
    | tar -C /usr/local/bin -xzv

# Копируем jar из builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Используем dockerize, чтобы дождаться готовности PostgreSQL перед стартом приложения
ENTRYPOINT ["dockerize", "-wait", "tcp://db:5432", "-timeout", "60s", "--", "java", "-jar", "app.jar"]