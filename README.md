# Digital Wallet Application

Production-ready Spring Boot dijital cuzdan API'si. Clean Architecture + JWT + Flyway Migrations.

## Ozellikler
- ✅ Cuzdan olusturma / Para yatirma / Para cekme / Transfer
- ✅ JWT Authentication
- ✅ Rate Limiting (10 req/min per user)
- ✅ Structured JSON Logging + Correlation IDs
- ✅ Flyway Database Migrations
- ✅ Docker + Docker Compose (PostgreSQL)
- ✅ CI/CD Pipeline (GitHub Actions)
- ✅ Comprehensive Test Suite (16 unit/integration tests)
- ✅ Global Exception Handling
- ✅ Swagger/OpenAPI Documentation
- ✅ Actuator Monitoring

## Teknolojiler
- Java 17+
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security (JWT)
- PostgreSQL (prod) / H2 (dev/test)
- Flyway Database Migration
- Docker
- Maven

## Quick Start

### Local H2 ile:
```bash
cd /Users/alper/Desktop/java_project/digital-wallet
cp .env.example .env
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- App: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Docker altyapisi (PostgreSQL + Redis):
```bash
docker-compose up
```
- PostgreSQL: localhost:5432 (username: postgres, password: postgres)
- Redis: localhost:6379

## Authentication (JWT)

### 1. Dev Demo Token Al
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

### 2. Token ile Istek Yap
```bash
curl -X GET http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI..."
```

## Demo Frontend

Tarayicidan mini demo ekranini ac:
- `http://localhost:8080/demo/index.html`

Demo sayfasinda:
1. `Demo Token Uret` ile token al
2. Cuzdan olustur, deposit/withdraw yap
3. Transfer ve transaction listesi dene
4. Tüm yanitlari tek ekranda gor

## API Endpoints

### Wallets
- `POST /api/v1/wallets` - Cuzdan olustur
- `GET /api/v1/wallets` - Token kullanicisinin cuzdani
- `POST /api/v1/wallets/deposit` - Para yatir
- `POST /api/v1/wallets/withdraw` - Para cek
- `POST /api/v1/wallets/transfer` - Para transfer

### Transactions
- `GET /api/v1/wallets/{walletId}/transactions` - Islem gecsemisi (paginated)

### Auth
- `POST /api/v1/auth/demo-token` - Dev ortaminda demo JWT token uret

## Testing
```bash
./mvnw test
# 16 tests passed
```

## Monitoring
- Health Check: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Logs: JSON format (ELK/Kibana uyumlu)

## Database

### Flyway Migrations
Migrations otomatik olarak app startup'inda calisir:
```
src/main/resources/db/migration/V1__Initial_Schema.sql
```

### Schema
- `wallets` - User cuzdan bilgileri
- `transactions` - Islem gecmisi  
- `users` - Kullanici bilgileri

## Architecture

Clean Architecture (Layered):
```
domain/
  ├─ model/         (Entity'ler)
  ├─ repository/    (Port'lar)
  └─ exception/     (Custom exceptions)
  
application/
  ├─ usecase/       (Business logic)
  └─ dto/           (Request/Response models)
  
infrastructure/
  ├─ persistence/   (Repository implementations)
  ├─ rest/          (Controllers, exception handlers)
  ├─ config/        (Spring configurations)
  └─ security/      (JWT, Security config)
```

## Rate Limiting
- **Global:** 10 requests/minute per user/ip
- **Login/Auth:** 5 requests/minute
- **Transfer:** 3 requests/minute

Bucket4j + Redis ile dagitik ortamda calisir.

Response header `Retry-After` ile bilgi verilir.

## Error Handling
Standardized error responses:
```json
{
  "status": 400,
  "code": "WALLET_NOT_FOUND",
  "message": "Cuzdan bulunamadi!",
  "timestamp": "2026-04-20T14:30:45.123Z"
}
```

## CI/CD

GitHub Actions ile otomatik test ve Docker build:
- Her push'da: Unit tests calisir
- Main branch'e push: Docker image build edilir

## Environment Variables

### Development (.env.dev)
```
SPRING_PROFILES_ACTIVE=dev
FLYWAY_ENABLED=true
JWT_SECRET=your-secret-key-dev-at-least-32-bytes
APP_TRUST_PROXY=false
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Production (.env.prod)
```
SPRING_PROFILES_ACTIVE=prod
FLYWAY_ENABLED=true
JWT_SECRET=your-secret-key-prod-at-least-32-bytes
DB_URL=jdbc:postgresql://prod-db:5432/wallet
DB_USER=prod_user
DB_PASSWORD=prod_password
APP_TRUST_PROXY=true
REDIS_HOST=prod-redis
REDIS_PORT=6379
```

## Contributing
1. Fork the repo
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License
MIT
