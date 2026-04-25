# Digital Wallet Application

## Overview

A backend system that simulates a real-world digital wallet platform where users can securely manage balances, perform transactions, and transfer money using a scalable and secure REST API.

Built to demonstrate production-level backend engineering practices including authentication, rate limiting, caching, CI/CD, and clean architecture principles.

Production-ready Spring Boot digital wallet API built with Clean Architecture, JWT, and Flyway.

## Features
- Wallet creation / deposit / withdraw / transfer
- JWT authentication
- Rate limiting (10 req/min per user)
- Structured JSON logging + correlation IDs
- Flyway database migrations
- Docker + Docker Compose (PostgreSQL)
- CI/CD pipeline (GitHub Actions)
- Comprehensive test suite (unit/integration)
- Global exception handling
- Swagger/OpenAPI documentation
- Actuator monitoring

## Tech Stack
- Java 17+
- Spring Boot 4.x
- Spring Web
- Spring Data JPA
- Spring Security (JWT)
- PostgreSQL (prod) / H2 (dev/test)
- Flyway
- Docker
- Maven

## Quick Start

### Local (H2)
**Profile must be set explicitly for safety (no default dev profile):**

```bash
cd /Users/alper/Desktop/java_project/digital-wallet
cp .env.example .env

# Option 1: Environment variable
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Option 2: Maven property
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Option 3: JVM argument
./mvnw spring-boot:run -Dspring.profiles.active=dev
```
- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

### Docker infrastructure (PostgreSQL + Redis)
```bash
docker-compose up
```
- PostgreSQL: localhost:5432 (username: `postgres`, password: `postgres`)
- Redis: localhost:6379

## Authentication (JWT)

### 1) Get a dev demo token
```bash
curl -X POST http://localhost:8080/api/v1/auth/demo-token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
}
```

### 2) Call a protected endpoint
```bash
curl -X GET http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
```

## Demo Frontend

Open in your browser:
- `http://localhost:8080/demo/index.html`
- `http://localhost:8080/` (landing page)

On the demo page:
1. Generate a demo token
2. Create a wallet, then deposit/withdraw
3. Transfer money and list transactions
4. Inspect all responses on a single screen

## API Endpoints

### Wallets
- `POST /api/v1/wallets` - Create wallet
- `GET /api/v1/wallets` - Get authenticated user's wallet
- `POST /api/v1/wallets/deposit` - Deposit money
- `POST /api/v1/wallets/withdraw` - Withdraw money
- `POST /api/v1/wallets/transfer` - Transfer money

### Transactions
- `GET /api/v1/wallets/{walletId}/transactions` - Transaction history (paginated)

### Auth
- `POST /api/v1/auth/demo-token` - Generate demo JWT token (dev only)

## Testing
```bash
./mvnw test
```

## Monitoring
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Logs: JSON format (ELK/Kibana friendly)

## Database

### Flyway Migrations
Migrations run automatically on startup:
```
src/main/resources/db/migration/V1__Initial_Schema.sql
```

### Schema
- `wallets` - user wallet data
- `transactions` - transaction history
- `users` - user data

## Architecture

Layered Clean Architecture:
```
domain/
  ├─ model/         (Entities)
  ├─ repository/    (Ports)
  └─ exception/     (Custom exceptions)

application/
  ├─ usecase/       (Business logic)
  └─ dto/           (Request/Response models)

infrastructure/
  ├─ persistence/   (Repository implementations)
  ├─ rest/          (Controllers, exception handlers)
  ├─ config/        (Spring configurations)
  └─ security/      (JWT, security config)
```

## Rate Limiting
- **Global:** 10 requests/minute per user/ip
- **Login/Auth:** 5 requests/minute
- **Transfer:** 3 requests/minute

Implemented with Bucket4j + Redis for distributed environments.

`Retry-After` response header is returned when throttled.

## Error Handling
Standardized error response:
```json
{
  "status": 400,
  "code": "WALLET_NOT_FOUND",
  "message": "Wallet not found",
  "timestamp": "2026-04-20T14:30:45.123Z"
}
```

## CI/CD

GitHub Actions pipeline:
- Run unit tests on every push
- Build Docker image on `main` branch pushes

## Environment Variables

### Development
```
SPRING_PROFILES_ACTIVE=dev
FLYWAY_ENABLED=true
JWT_SECRET=your-secret-key-dev-at-least-32-bytes
APP_TRUST_PROXY=false
APP_RATE_LIMITING_ENABLED=false
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Production
```
SPRING_PROFILES_ACTIVE=prod
FLYWAY_ENABLED=true
JWT_SECRET=your-secret-key-prod-at-least-32-bytes
DB_URL=jdbc:postgresql://prod-db:5432/wallet
DB_USER=prod_user
DB_PASSWORD=prod_password
APP_TRUST_PROXY=true
APP_RATE_LIMITING_ENABLED=true
REDIS_HOST=prod-redis
REDIS_PORT=6379
```

## Swagger / OpenAPI

Swagger UI:
- `http://localhost:8080/swagger-ui.html`

Raw OpenAPI document:
- `http://localhost:8080/v3/api-docs`

To test protected endpoints in Swagger UI:
1. Call `POST /api/v1/auth/demo-token` to get a token
2. Click **Authorize**
3. Enter token as `Bearer <token>`
4. Call protected wallet endpoints

## Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to your branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License
MIT
