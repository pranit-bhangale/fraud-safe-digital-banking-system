# Fraud-Safe Digital Banking System

A production-grade, distributed digital banking platform built with Spring Boot microservices, Spring Cloud, Kafka, and Redis, designed with high resilience and real-time fraud detection.

---

## 🏗️ Architecture & Services

The platform consists of 9 core Java microservices:

| Service | Port | Description | Health Endpoint | Swagger UI Docs |
|---|---|---|---|---|
| **`config-server`** | `8888` | Centralized external configuration server | `http://localhost:8888/actuator/health` | - |
| **`service-registry`** | `8761` | Netflix Eureka service discovery server | `http://localhost:8761/actuator/health` | - |
| **`user-service`** | `8081` | Authentication, JWT token issuance & user profiles | `http://localhost:8081/actuator/health` | `http://localhost:8081/swagger-ui.html` |
| **`account-service`** | `8082` | Account balance ledgers & reservation management | `http://localhost:8082/actuator/health` | `http://localhost:8082/swagger-ui.html` |
| **`transaction-service`** | `8083` | Distributed Saga transaction orchestration & gRPC | `http://localhost:8083/actuator/health` | `http://localhost:8083/swagger-ui.html` |
| **`fraud-detection`** | `8084` | Real-time transaction fraud scoring via Kafka | `http://localhost:8084/actuator/health` | - (Kafka Streaming) |
| **`payment-service`** | `8085` | Payment processing & merchant orders | `http://localhost:8085/actuator/health` | `http://localhost:8085/swagger-ui.html` |
| **`notification-service`** | `8086` | Real-time event notifications & alerts | `http://localhost:8086/actuator/health` | - (AMQP / RabbitMQ) |
| **`api-gateway`** | `8080` | Edge routing, rate limiting & public ingress | `http://localhost:8080/actuator/health` | `http://localhost:8080/swagger-ui.html` |

---

## 🚀 Local Development Quick Start (Zero Docker)

To run the entire system locally without Docker, use the automated sequential bootstrap in `bootstrap/project-bootstrap`:

### 1. Navigate to the Bootstrap Directory
```bash
cd bootstrap/project-bootstrap
```

### 2. Start All Services Sequentially
```bash
make setup
```
*(Starts services strictly in dependency order `1 -> 2 -> ... -> 9` and verifies each health endpoint before starting the next).*

### 3. Check Service Status
```bash
make status
```

### 4. Stop All Services (Reverse Teardown)
```bash
make down
```
*(Shuts down services safely in reverse order `9 -> ... -> 1` to halt traffic before core services stop).*

---

## 📚 Documentation

For complete step-by-step guides, polyglot configurations, and customization options, see the [Project Bootstrap Documentation](file:///p:/fraud-safe-digital-banking-system/bootstrap/project-bootstrap/README.md).
