# Wallet Application

Это Spring Boot-приложение, имитирующее операции с кошельками. В проекте реализовано:

- REST API для пополнения и снятия средств
- Интеграция с PostgreSQL через Spring Data
- Использование Testcontainers для запуска базы данных в тестах
- Нагрузочное тестирование через JUnit + `HttpClient`

## ⚙️ Технологии

- Java 17+
- Spring Boot
- PostgreSQL
- Testcontainers
- JUnit 5
- Liquibase
- Maven

## 🚀 Запуск
1. Запустить Docker
2. docker-compose up -d --build
