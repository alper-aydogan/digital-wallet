# Digital Wallet

Spring Boot tabanli basit bir dijital cuzdan API'si.

## Ozellikler
- Cuzdan olusturma
- Para yatirma
- DTO + validation
- Global exception handling (`@RestControllerAdvice`)
- H2 (dev: file-based, test: in-memory)

## Teknolojiler
- Java 21+
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Security
- H2 Database
- Maven

## Projeyi Calistirma
```bash
cd /Users/alper/Desktop/java_project/digital-wallet
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Test
```bash
cd /Users/alper/Desktop/java_project/digital-wallet
./mvnw test
```

## API Ornekleri
### Cuzdan Olustur
`POST /api/v1/wallets`

Body:
```json
{
  "userId": 1,
  "currency": "TRY"
}
```

### Para Yatir
`POST /api/v1/wallets/deposit`

Body:
```json
{
  "userId": 1,
  "amount": 100.50
}
```

## Notlar
- Varsayilan profile `dev`.
- H2 Console: `http://localhost:8080/h2-console`
- Dev DB URL: `jdbc:h2:file:./data/walletdb`

