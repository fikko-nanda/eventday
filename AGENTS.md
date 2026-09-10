# AGENTS.md - Eventday Ticketing Backend

## Project Overview
Spring Boot 3.2.4 (Java 21, target Java 25) REST API, PostgreSQL, Maven, port 8082. Eventday ticketing: user/organizer/event/ticket/booking/order/payment/refund management.

> **Status 2026-09-09:** Tahap 1 (JPA Entities 12 tabel - reset password digabung ke auth) & Tahap 2 (Auth Only + Google Login + OTP + Reset Password) **SELESAI**. Test `AuthFlowIntegrationTest` 21 cases PASS — `API.md` & `AGENTS.md` sinkron dengan `application.properties` (port 8082).

> **Status 2026-09-04:** `.github/modernize/java-upgrade/20260904031947/` — active plan to upgrade Java 21 → 25. Do NOT execute unless explicitly asked.

AI harus: jangan buat endpoint Event/Order/etc kecuali diminta; jangan buat ulang tabel reschedule; ikuti struktur di bawah. Frontend baca `API.md`.

## Source of Truth
- **Dependencies**: `pom.xml` (authoritative — AGENTS.md tech table may lag)
- **Runtime config**: `src/main/resources/application.properties`
- **API docs**: `API.md` (human-friendly, lengkap dengan curl & response untuk frontend)
- **Schema**: `src/main/resources/db/migration/V1__init_schema.sql`
- **Tests**: `src/test/java/com/example/eventday/AuthFlowIntegrationTest.java` (21) + `EventdayApplicationTests.java` (contextLoads)

## Tech Stack
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` (3.2.4) | REST + `RestTemplate` (verifikasi Google) |
| `spring-boot-starter-data-jpa` | ORM |
| `spring-boot-starter-validation` | `@NotBlank @Email @Size @Pattern` di DTO |
| `spring-boot-starter-security` | `BCryptPasswordEncoder`, `SecurityFilterChain` |
| `spring-boot-starter-mail` | OTP & reset password email (Mailtrap sandbox) |
| `postgresql` | Driver |
| `jjwt-api:0.12.5` + `jjwt-impl` + `jjwt-jackson` | JWT HS256 |
| `lombok` | `@Data @Builder` |
| `spring-boot-starter-test` + `spring-security-test` | Test (MockMvc) |
| `@EnableScheduling` on `EventdayApplication` | Scheduler enabled (no jobs yet) |

`application.properties` key values:
```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/db_eventday
spring.datasource.username=postgres
spring.datasource.password=fikko04
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000
google.client-id=875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com  # ISI, bukan kosong!
app.order.admin-fee=5000
app.order.expiry-minutes=15
# Mailtrap sandbox SMTP
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=b3eb7c16eae2ee
spring.mail.password=a5cd8a29ea8ba7
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=noreply@eventday.local
app.mail.from-name=Eventday
app.otp.expiry-minutes=5
app.otp.length=6
app.reset-password.code-length=6
app.reset-password.expiry-minutes=15
app.reset-password.frontend-url=http://localhost:3000
# Logging
logging.level.root=INFO
logging.level.com.example.eventday=DEBUG
logging.level.com.example.eventday.config.ApiLoggingFilter=INFO
logging.level.com.example.eventday.service.EmailService=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.springframework.mail=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=logs/eventday.log
```

## Project Structure
```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # @SpringBootApplication @EnableScheduling
├── controller/
│   ├── AuthController.java           # POST /api/v1/auth/register, login, google, verify-otp, resend-otp, reset-password
│   └── HomeController.java           # GET "/" → "Eventday API Server is Running!"
├── config/
│   ├── CorsConfig.java               # GLOBAL CORS * (ngrok), allowCredentials true, maxAge 3600
│   ├── ApiLoggingFilter.java         # OncePerRequestFilter log [API HIT]/[API DONE] method URI status duration
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice handle Validation/401/403 -> ApiResponse
│   └── SecurityConfig.java           # BCrypt, STATELESS, / & /error permitAll, /api/v1/auth/** permitAll, /api/auth/** permitAll (legacy), JwtAuthenticationFilter + 401/403 Json ApiResponse
├── security/
│   ├── JwtTokenProvider.java         # @Value jwt.secret + jwt.expiration-ms, HS256 generate/validate
│   ├── JwtUtil.java                  # COMPAT — jangan hapus
│   └── JwtAuthenticationFilter.java  # OncePerRequestFilter, Bearer → validate → ROLE_*
├── dto/
│   ├── ApiResponse.java              # Wrapper {msg, status, data} — semua controller pakai ini (200/201/400/401/403)
│   ├── RegisterRequest.java          # name @NotBlank @Size100, email @Email unique, username @NotBlank @Size3-20 @Pattern ^[a-zA-Z0-9_]+$ unique, phone @Size15, password @NotBlank @Size6, nik @Pattern \d{16} unique, role String
│   ├── LoginRequest.java             # email, username, identifier, password + getIdentifier() (contains "@" → email else username, fallback)
│   ├── GoogleLoginRequest.java       # idToken @NotBlank
│   ├── VerifyOtpRequest.java         # email @NotBlank @Email, otpCode @NotBlank
│   ├── ResendOtpRequest.java         # email @NotBlank @Email
│   ├── ResetPasswordRequest.java     # email @NotBlank @Email, code @JsonAlias token/otp, token, newPassword @JsonAlias password + getEffectiveCode()/getEffectiveNewPassword() trim
│   └── AuthResponse.java             # message, userId, name, email, username, role, token @JsonIgnore (hidden dari JSON, kirim via Set-Cookie access_token HttpOnly), expiresIn — dibungkus ApiResponse.data
├── entity/                           # 12 files = 12 tabel (password_reset_tokens dihapus, digabung ke auth)
│   ├── User.java                     # users — UUID PK, email unique, username unique 20, nik unique 16, role ENUM CUSTOMER/ORGANIZER/ADMIN
│   ├── Auth.java                     # auth — @ManyToOne User (user_id UNIQUE CASCADE), password BCrypt, authGoogle VARCHAR(20), aksesToken TEXT, expiredToken, status INACTIVE/ACTIVE, resetToken VARCHAR(255), resetExpiredAt
│   ├── Otp.java                      # otp — @ManyToOne User, otpCode VARCHAR(10), expiredAt
│   ├── Organizer.java                # organizers — @ManyToOne User, verificationStatus UNVERIFIED
│   ├── Event.java                    # events — @ManyToOne Organizer, status DRAFT
│   ├── TicketTier.java               # ticket_tiers — @ManyToOne Event, price NUMERIC(12,2)
│   ├── Booking.java                  # bookings — @ManyToOne User + TicketTier, status PENDING
│   ├── Order.java                    # orders — @ManyToOne Booking(UNIQUE)+Customer+Event+TicketTier
│   ├── TicketItem.java               # ticket_items — @ManyToOne Order+TicketTier, checkInStatus UNREDEEMED
│   ├── RefundRequest.java            # refund_requests — @ManyToOne Customer+Order, bank fields NOT NULL
│   ├── Settings.java                 # settings — BIGSERIAL PK, settings_key UNIQUE
│   └── AuditLog.java                 # audit_logs — actorId/actorName/action/detail/createdAt
├── repository/
│   ├── UserRepository.java           # findByEmail, findByUsername, existsByEmail/Username/Nik
│   ├── AuthRepository.java           # findByUserUserId, findByAksesToken
│   ├── OtpRepository.java            # findByUserUserIdAndOtpCode, findByUserUserId, deleteByUserUserId
│   └── AuditLogRepository.java       # log only
└── service/
    ├── AuthService.java              # register + login + loginWithGoogle + verifyOtp + resendOtp + resetPassword (langsung di auth.resetToken) + logout
    ├── AuditLogService.java          # log(actorId, actorName, action, detail)
    └── EmailService.java             # sendOtpEmail + sendResetPasswordEmail (Mailtrap, @Value app.mail.*, gagal → log warn tidak throw)
```

**DIHAPUS & JANGAN DIBUAT ULANG:** `RescheduleRequest` + `PasswordResetToken`/`password_reset_tokens` (sudah digabung ke `auth` sesuai mentor) + semua service/controller DTO untuk Event/Order/Payment/Settings/Ticket/Refund/Reschedule/Organizer. Semua sengaja dihapus untuk Auth Only.

## Database Schema
Migrasi: `src/main/resources/db/migration/V1__init_schema.sql` (`uuid-ossp`, 12 tabel, FK, index, default `ADMIN_FEE=5000`, `ORDER_EXPIRY_MINUTES=15`, `BOOKING_EXPIRY_MINUTES=10`).

| Table | PK | FK | Catatan |
|---|---|---|---|
| `users` | `user_id` UUID | — | `username UNIQUE`, `email UNIQUE`, `nik UNIQUE` |
| `auth` | `auth_id` UUID | `user_id` UNIQUE → users CASCADE | `password` hash, `auth_google` 20, `akses_token` TEXT, `reset_token` 255, `reset_expired_at` 15 menit |
| `otp` | `otp_id` UUID | `user_id` → users CASCADE | `otp_code` 10, `expired_at` 5 menit |
| `organizers` | `organizer_id` UUID | `user_id` UNIQUE → users | `verification_status UNVERIFIED` |
| `events` | `event_id` UUID | `organizer_id` → organizers RESTRICT | `status DRAFT` |
| `ticket_tiers` | `tier_id` UUID | `event_id` → events CASCADE | `price NUMERIC(12,2)` |
| `bookings` | `booking_id` UUID | `user_id`→users, `tier_id`→tiers | `status PENDING` |
| `orders` | `order_id` UUID | `booking_id` UNIQUE, `customer_id`→users, `event_id`→events, `tier_id`→tiers | |
| `ticket_items` | `ticket_item_id` UUID | `order_id`→orders CASCADE, `tier_id`→tiers | `UNREDEEMED` |
| `refund_requests` | `refund_id` UUID | `customer_id`→users, `order_id`→orders | |
| `settings` | `settings_id` BIGSERIAL | — | `settings_key UNIQUE` |
| `audit_logs` | `audit_id` UUID | — | `actorId/action/detail` |

Semua `created_at/updated_at/create_by/updated_by` ada; `role/status` VARCHAR default. `password_reset_tokens` **dihapus** (digabung ke `auth`).

## API Endpoints

### ✅ AKTIF — Auth
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | cek duplikat email/username/nik → `User.Role.valueOf` fallback CUSTOMER → save `User` → BCrypt → save `Auth(INACTIVE)` → generate OTP 6-digit → save `otp` → `sendOtpEmail` → audit REGISTER → return tanpa token |
| POST | `/api/v1/auth/verify-otp` | Public | find `User` by email → cek `Otp` by userId+otpCode → cek expired → set `Auth.status=ACTIVE` → delete OTP |
| POST | `/api/v1/auth/resend-otp` | Public | find `User` → delete OTP lama → generate baru → save → kirim email |
| POST | `/api/v1/auth/login` | Public | `getIdentifier()` (contains "@" → email else username, fallback) → find `User` → `findByUserUserId` → `matches` → cek `ACTIVE` else `Akun belum aktif!` → `generateToken(userId,email,role)` 86400000ms → update `aksesToken/expiredToken/ACTIVE` → `Set-Cookie access_token HttpOnly` + return `data` **tanpa token** (`@JsonIgnore`) + `expiresIn` |
| POST | `/api/v1/auth/google` | Public | `GET tokeninfo?id_token=` → cek `aud==google.client-id`, `exp`, `email_verified` → find/create `User` (username auto) → find/create `Auth` dummy BCrypt `authGoogle[0:20]` → generate JWT → `Set-Cookie access_token HttpOnly` |
| POST | `/api/v1/auth/reset-password` | Public | **Single endpoint 2 tahap (langsung di auth)**: tanpa `code`/`token` → generate 6-digit → `auth.reset_token/reset_expired_at` 15min → email. Dengan `code`/`token`+`newPassword` → `getEffectiveCode()` trim + cek `auth.resetToken==code && not expired` → update BCrypt → clear `resetToken` |

`GET /` dan `GET /error` permit, `POST /api/auth/**` legacy permit. Lihat `SecurityConfig.java:53`.

**JWT Middleware** `JwtAuthenticationFilter.java:23`: `Authorization: Bearer <token>` **atau** `Cookie: access_token` (`resolveToken()`) → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`. `AuthResponse.token` `@JsonIgnore` — token hanya via `Set-Cookie` HttpOnly, tidak di Network → Response.

**Google Flow:** ID Token dari `https://accounts.google.com/gsi/client` (`data-client_id=google.client-id`) → `POST /google {idToken}`. Backend hanya verifikasi, tidak redirect.

### ⏳ SCHEMA-ONLY (jangan implement kecuali diminta)
`POST /api/events`, `GET /api/events`, `POST /api/orders`, `POST /api/payments/pay/{orderId}`, `POST /api/tickets/scan/{ticketItemId}`, `GET/PUT /api/settings`, `POST /api/refunds` — entity+table ready, 403 jika dipanggil.

## Key Business Logic (Auth Only)
- `CorsConfig.java:10` global `*` untuk ngrok (maxAge 3600, allowCredentials).
- Password hanya di `auth.password`, tidak di `users`. Untuk Google, dummy UUID BCrypt (kolom NOT NULL).
- Token JWT disimpan di DB `auth.aksesToken` untuk invalidasi logout (`AuthService.logout` null-kan token, belum expose endpoint).
- `authGoogle` `VARCHAR(20)` → `sub.substring(0,20)`.
- `EmailService.java` Mailtrap sandbox — gagal → `log.warn` tidak throw, OTP/token tetap bisa dilihat di log.
- Reset Password **digabung ke `auth`**: `auth.reset_token` + `auth.reset_expired_at` (mentor request, tidak lagi tabel terpisah). `ResetPasswordRequest` trim code & alias `token`/`otp`.
- Register flow: `register()` generates 6-digit OTP → `otp` exp 5min → email. `verifyOtp()` → `ACTIVE`. `resendOtp()` invalidates old → new.
- `@EnableScheduling` aktif di `EventdayApplication.java` tapi belum ada job.
- Logging Spring Boot aktif `logging.level.com.example.eventday=DEBUG` → `logs/eventday.log`.

## Known Issues / TODO
1. `jwt.secret` hardcoded — prod pindah env/Secret Manager.
2. No refresh token, hanya access 24 jam.
3. Java upgrade plan 21→25 di `.github/modernize/...` — **JANGAN JALANKAN** kecuali diminta.
4. `Settings.java` pakai `IDENTITY` BIGSERIAL bukan UUID (sesuai DDL).
5. Jangan buat ulang `reschedule_requests` & `password_reset_tokens` (sudah digabung).
6. `google.client-id` sudah **diisi** — validasi aud aktif.
7. `AuthService.logout(UUID)` ada tapi belum expose endpoint REST.
8. `register()` OTP di `otp` table 5 menit, reset code di `auth` 15 menit.

## Code Conventions
- Package `com.example.eventday`, entity singular, table plural snake_case
- PK `GenerationType.UUID` kecuali `settings_id` (IDENTITY BIGSERIAL)
- Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- DTO validasi `jakarta.validation` + `Jackson @JsonAlias` untuk compat
- `@Service @Transactional`, `@RestController`
- `createdAt = LocalDateTime.now()` via `@Builder.Default`
- `@Value` untuk config injection
- `RestTemplate` untuk Google verification
- `SecureRandom` untuk OTP/code 6-digit

## Build & Run
```bash
./mvnw clean compile -DskipTests   # BUILD SUCCESS
./mvnw spring-boot:run             # port 8082
./mvnw test                        # 22 tests (AuthFlowIntegrationTest 21 + contextLoads) PASS

curl -X POST localhost:8082/api/v1/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456","role":"CUSTOMER"}'
curl -X POST localhost:8082/api/v1/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com","otpCode":"123456"}'
curl -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"john123","password":"123456"}'
curl -X POST localhost:8082/api/v1/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
curl -X POST localhost:8082/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'  # minta kode (simpan di auth.reset_token)
curl -X POST localhost:8082/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com","code":"123456","newPassword":"newPass123"}'
curl -H "Authorization: Bearer <token>" localhost:8082/any-protected   # 401 tanpa token, 403 jika endpoint schema-only
```

## Reference Files
- `API.md` — full API documentation (frontend handoff, lengkap curl & response)
- `HELP.md` — Spring Boot help
- `.vscode/settings.json` — IDE config

