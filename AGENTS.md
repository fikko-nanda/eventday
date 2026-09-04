# AGENTS.md - Eventday Ticketing Backend

## Project Overview
Spring Boot 3.2.4 (Java 21, target Java 25) REST API, PostgreSQL, Maven, port 8081. Eventday ticketing: user/organizer/event/ticket/booking/order/payment/refund management.

> **Status 2026-09-02:** Tahap 1 (JPA Entities 11 tabel) & Tahap 2 (Auth Only + Google Login) SELESAI.
> **Status 2026-09-04:** `.github/modernize/java-upgrade/20260904031947/` — active plan to upgrade Java 21 → 25 (branch `refund-dll`). Do NOT execute unless explicitly asked.

AI harus: jangan buat endpoint Event/Order/etc kecuali diminta; jangan buat ulang tabel reschedule; ikuti struktur di bawah.

## Source of Truth
- **Dependencies**: `pom.xml` (authoritative — AGENTS.md tech table may lag)
- **Runtime config**: `src/main/resources/application.properties`
- **API docs**: `API.md` (human-friendly reference)
- **Schema**: `src/main/resources/db/migration/V1__init_schema.sql`
- **Tests**: `src/test/java/com/example/eventday/EventdayApplicationTests.java` (only contextLoads)

## Tech Stack
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` (3.2.4) | REST |
| `spring-boot-starter-data-jpa` | ORM |
| `spring-boot-starter-validation` | `@NotBlank @Email @Size` di DTO |
| `spring-boot-starter-security` | `BCryptPasswordEncoder`, `SecurityFilterChain` |
| `spring-boot-starter-mail` | OTP & reset password email (Mailtrap sandbox) |
| `postgresql` | Driver |
| `jjwt-api:0.12.5` + `jjwt-impl` + `jjwt-jackson` | JWT HS256 |
| `lombok` | `@Data @Builder` |
| `spring-boot-starter-test` + `spring-security-test` | Test |
| `@EnableScheduling` on `EventdayApplication` | Cron job / scheduler enabled (no jobs currently) |

`application.properties` key values:
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/db_eventday
spring.datasource.username=postgres
spring.datasource.password=fikko04
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000
google.client-id=875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com  # ISI, bukan kosong!
app.order.admin-fee=5000
app.order.expiry-minutes=15
# Mailtrap sandbox SMTP (OTP & reset password)
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=c7de84de353b59
spring.mail.password=e922f277d22595
app.mail.from=noreply@eventday.local
app.mail.from-name=Eventday
app.otp.expiry-minutes=5
app.otp.length=6
app.reset-password.code-length=6
app.reset-password.expiry-minutes=15
app.reset-password.frontend-url=http://localhost:3000
```

## Project Structure
```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # @SpringBootApplication @EnableScheduling
├── controller/
│   ├── AuthController.java           # POST /api/v1/auth/register, login, google, forgot-password, reset-password
│   └── HomeController.java           # GET "/" → "Eventday API Server is Running!"
├── config/
│   ├── CorsConfig.java               # GLOBAL CORS * (ngrok), allowCredentials true, maxAge 3600
│   └── SecurityConfig.java           # BCrypt, STATELESS, / & /error permitAll, /api/v1/auth/** permitAll, /api/auth/** permitAll (legacy), JwtAuthenticationFilter
├── security/
│   ├── JwtTokenProvider.java         # @Value jwt.secret + jwt.expiration-ms, HS256
│   ├── JwtUtil.java                  # COMPAT — jangan hapus
│   └── JwtAuthenticationFilter.java  # OncePerRequestFilter, Bearer → validate → ROLE_*
├── controller/
├── dto/
│   ├── RegisterRequest.java          # name @NotBlank, email @Email, username @NotBlank @Size3-20 @Pattern ^[a-zA-Z0-9_]+$, phone @Size15, password @NotBlank @Size6, nik @Pattern \d{16}, role String
│   ├── LoginRequest.java             # email/username/identifier + password (support email OR username)
│   ├── GoogleLoginRequest.java       # idToken @NotBlank
│   └── ResetPasswordRequest.java     # email @NotBlank @Email, code, newPassword @NotBlank @Size6
│   └── VerifyOtpRequest.java         # email, otpCode
│   └── ResendOtpRequest.java         # email
│   └── AuthResponse.java             # message, userId, name, email, username, role, token, expiresIn (detik)
├── entity/                           # 13 files = 13 tabel (Settings & AuditLog jadi 1 poin)
│   ├── User.java                     # users — UUID PK, email unique, username unique 20, nik unique 16, role ENUM CUSTOMER/ORGANIZER/ADMIN
│   ├── Auth.java                     # auth — @ManyToOne User (user_id UNIQUE FK CASCADE), password BCrypt, authGoogle VARCHAR(20), aksesToken TEXT, expiredToken, status INACTIVE/ACTIVE
│   ├── Organizer.java                # organizers — @ManyToOne User, verificationStatus UNVERIFIED
│   ├── Event.java                    # events — @ManyToOne Organizer, status DRAFT
│   ├── TicketTier.java               # ticket_tiers — @ManyToOne Event, price NUMERIC(12,2)
│   ├── Booking.java                  # bookings — @ManyToOne User + TicketTier, status PENDING
│   ├── Order.java                    # orders — @ManyToOne Booking(UNIQUE)+Customer+Event+TicketTier
│   ├── TicketItem.java               # ticket_items — @ManyToOne Order+TicketTier, checkInStatus UNREDEEMED
│   ├── RefundRequest.java            # refund_requests — @ManyToOne Customer+Order, bank fields NOT NULL
│   ├── Settings.java                 # settings — BIGSERIAL PK, settings_key UNIQUE
│   ├── PasswordResetToken.java       # password_reset_tokens — @ManyToOne User, token VARCHAR(255), expiredAt
│   └── Otp.java                        # otp — @ManyToOne User, otpCode VARCHAR(10), expiredAt
│   └── AuditLog.java                 # audit_logs — actorId/actorName/action/detail/createdAt
├── repository/
│   ├── UserRepository.java           # findByEmail, findByUsername, existsByEmail, existsByUsername, existsByNik
│   ├── AuthRepository.java           # findByUserUserId, findByAksesToken
│   ├── PasswordResetTokenRepository.java # findByToken, findByUserUserIdAndToken, deleteByUserUserId
│   └── OtpRepository.java              # findByUserUserIdAndOtpCode, deleteByUserUserId
│   └── AuditLogRepository.java       # log only
└── service/
    ├── AuthService.java              # register + login + loginWithGoogle + logout + verifyOtp + resendOtp + sendResetCode + resetPassword
    ├── AuditLogService.java          # log(actorId, actorName, action, detail)
    └── EmailService.java             # sendOtpEmail + sendResetPasswordEmail (Mailtrap sandbox, @Value app.mail.*)
```

**DIHAPUS & JANGAN DIBUAT ULANG:** `RescheduleRequest` + semua service/controller DTO untuk Event/Order/Payment/Settings/Ticket/Refund/Reschedule/Organizer. Semua sengaja dihapus untuk Auth Only.

## Database Schema
Migrasi: `src/main/resources/db/migration/V1__init_schema.sql` (`uuid-ossp`, 11 tabel, FK, index, default `ADMIN_FEE=5000`, `ORDER_EXPIRY_MINUTES=15`, `BOOKING_EXPIRY_MINUTES=10`).

| Table | PK | FK |
|---|---|---|
| `users` | `user_id` UUID | — |
| `auth` | `auth_id` UUID | `user_id` UNIQUE → users CASCADE |
| `organizers` | `organizer_id` UUID | `user_id` UNIQUE → users |
| `events` | `event_id` UUID | `organizer_id` → organizers RESTRICT |
| `ticket_tiers` | `tier_id` UUID | `event_id` → events CASCADE |
| `bookings` | `booking_id` UUID | `user_id`→users, `tier_id`→ticket_tiers |
| `orders` | `order_id` UUID | `booking_id` UNIQUE, `customer_id`→users, `event_id`→events, `tier_id`→tiers |
| `ticket_items` | `ticket_item_id` UUID | `order_id`→orders CASCADE, `tier_id`→tiers |
| `refund_requests` | `refund_id` UUID | `customer_id`→users, `order_id`→orders |
| `settings` | `settings_id` BIGSERIAL | — |
| `password_reset_tokens` | `reset_id` UUID | `user_id` → users |
| `audit_logs` | `audit_id` UUID | — |

Semua `created_at/updated_at/create_by/updated_by` ada; `role/status` VARCHAR default.

## API Endpoints

### ✅ AKTIF — Auth
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | validasi → cek duplikat email/username/nik → `User.Role.valueOf` fallback CUSTOMER → save `User` → `BCrypt` → save `Auth(INACTIVE)` → audit REGISTER → return tanpa token |
| POST | `/api/v1/auth/login` | Public | `identifier` (email/username) → find `User` by email OR username → find `Auth` → `matches` → cek `status ACTIVE` → `jwtTokenProvider.generateToken(userId,email,role.name)` (86400000ms) → update `aksesToken/expiredToken/status ACTIVE` → audit LOGIN → return `token + expiresIn + username` |
| POST | `/api/v1/auth/google` | Public | terima `idToken` frontend GIS → `GET https://oauth2.googleapis.com/tokeninfo?id_token=` → cek `aud==google.client-id` (diisi!), `exp`, `email_verified` → find/create `User` by email → find/create `Auth` (password dummy BCrypt, `authGoogle` potong 20 char) → generate JWT sama → audit REGISTER_GOOGLE/LOGIN_GOOGLE |

| POST | `/api/v1/auth/reset-password` | Public | **3 mode:** tanpa `code` → generate 6-digit OTP → `password_reset_tokens` (+15min) → `sendResetOtpEmail` OTP (bukan link) → audit PASSWORD_RESET_TOKEN_SENT. `code` tanpa `newPassword` → `verifyResetCode` cek valid/expired → return valid (untuk **klik Lanjutkan → pindah halaman** frontend). `code+newPassword` → validasi → update BCrypt → delete token → audit PASSWORD_RESET_SUCCESS |
| POST | `/api/v1/auth/verify-otp` | Public | find `User` →  cek `Otp` by userId+otpCode →  cek expired →  set `Auth.status=ACTIVE` →  delete OTP |
| POST | `/api/v1/auth/resend-otp` | Public | find `User` →  delete OTP lama →  generate OTP baru →  simpan di `otp` →  kirim email |
`GET /` dan `GET /error` juga permit (lihat `SecurityConfig.java:37`). `POST /api/auth/**` permit (legacy).

**JWT Middleware** `JwtAuthenticationFilter.java:23`: header `Authorization: Bearer <token>` → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_<role>` → `anyRequest.authenticated()`.

**Google Flow (backend):** ID Token didapat di **frontend** via `https://accounts.google.com/gsi/client` (`data-client_id=google.client-id`), lalu `POST /api/v1/auth/google {idToken}`. Backend hanya verifikasi, tidak OAuth redirect.

### ⏳ SCHEMA-ONLY (jangan implement kecuali diminta)
`POST /api/events`, `GET /api/events`, `POST /api/orders`, `POST /api/payments/pay/{orderId}`, `POST /api/tickets/scan/{ticketItemId}`, `GET/PUT /api/settings`, `POST /api/refunds` — entity+table ready, 403 jika dipanggil.

## Key Business Logic (Auth Only)
- `CorsConfig.java:10` global `*` untuk ngrok (maxAge 3600, allowCredentials).
- Password tidak pernah di `users`, hanya di `auth.password`. Untuk Google, password dummy UUID BCrypt (kolom NOT NULL).
- Token disimpan di DB untuk invalidasi logout (`AuthService.logout` null-kan token, belum expose endpoint).
- `authGoogle` `VARCHAR(20)` → potong `sub.substring(0,20)` (Google sub ~21 char).
- `EmailService.java` menggunakan Mailtrap sandbox SMTP — saat mail server connection gagal, log warning dan kembalikan OTP/reset link di log (tidak throw).
- Forgot Password single-endpoint: `POST /api/v1/auth/reset-password` **3 mode** tanpa `code` → OTP (+15min) via `sendResetOtpEmail` (HTML kode, bukan link). `code` tanpa `newPassword` → `verifyResetCode` cek valid/expired → return valid (untuk klik Lanjutkan → pindah halaman frontend). `code+newPassword` → validasi → update BCrypt → delete token. DTO tunggal: `ResetPasswordRequest {email, code, newPassword}`.
- Register flow: `register()` generates 6-digit OTP → simpan di `otp` table (expired 5min) → `sendOtpEmail`. `verifyOtp(email, otpCode)` validates OTP → set `Auth.status=ACTIVE`. `resendOtp(email)` invalidates old OTP → generates new → sends email.
- `@EnableScheduling` aktif di `EventdayApplication.java` tapi tidak ada job schedule.

## Known Issues / TODO
1. `jwt.secret` hardcoded di properties — prod pindah ke env/Secret Manager.
2. No refresh token, hanya access 24 jam.
3. Java upgrade plan 21→25 ada di `.github/modernize/java-upgrade/20260904031947/` — **JANGAN JALANKAN** kecuali diminta.
4. `Settings.java` pakai `IDENTITY` BIGSERIAL bukan UUID (sesuai DDL).
5. Jangan buat ulang `reschedule_requests`.
6. `google.client-id` sudah **diisi** di `application.properties` — validasi aud aktif.
7. `AuthService.logout(UUID)` ada tapi belum ada endpoint REST untuk memanggilnya.
8. Forgot password menggunakan satu endpoint `/reset-password`. Body tanpa `code` = kirim kode; body dengan `code` = reset password. DTO tunggal `ResetPasswordRequest`.
9. egister()\ sekarang menyimpan OTP ke table \otp\ dan mengirim via email. Sebelumnya OTP tidak pernah di DB — sekarang sudah.

## Code Conventions
- Package `com.example.eventday`, entity singular, table plural snake_case
- PK `GenerationType.UUID` kecuali `settings_id` (IDENTITY BIGSERIAL)
- Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- DTO validasi `jakarta.validation`, `@Service @Transactional`, `@RestController`
- `createdAt = LocalDateTime.now()` via `@Builder.Default`
- `@Value` untuk config injection di service
- `RestTemplate` untuk Google token verification

## Build & Run
```bash
./mvnw clean compile -DskipTests   # BUILD SUCCESS (verify first)
./mvnw spring-boot:run
./mvnw test                        # hanya contextLoads test
curl -X POST localhost:8081/api/v1/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456","role":"CUSTOMER"}'
curl -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"john123","password":"123456"}'
curl -X POST localhost:8081/api/v1/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
curl -X POST localhost:8081/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"user@mail.com"}'  # kirim kode
curl -X POST localhost:8081/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"user@mail.com","code":"123456","newPassword":"newPassword123"}'  # reset password
curl -H "Authorization: Bearer <token>" localhost:8081/any-protected   # 403 tanpa token
```

## Reference Files
- `API.md` — full API documentation with curl examples and response schemas
- `HELP.md` — Spring Boot default help
- `.vscode/settings.json` — IDE config (interactive build config, null analysis automatic)
