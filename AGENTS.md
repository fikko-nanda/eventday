# AGENTS.md - Eventday Ticketing Backend

## Project Overview
Eventday adalah backend ticketing Spring Boot 3.x (Java 21, PostgreSQL, Maven, port 8081) untuk penjualan tiket event. Fokus: manajemen user, organizer, event, ticket, booking/order, payment, refund.

> **Status 2026-09-02 (kirim ini ke AI):**
> - **Tahap 1 — JPA Entities LENGKAP 10 tabel (11 physical): SELESAI.** `V1__init_schema.sql` + entities di `entity/` 100% siap. **RescheduleRequest DIHAPUS** (tidak ada di prompt terbaru).
> - **Tahap 2 — Auth Only SELESAI (+ Google Login di backend).** `POST /api/v1/auth/register` & `POST /api/v1/auth/login` & `POST /api/v1/auth/google` + JWT + CORS yang aktif. Modul lain (Event/Booking/Order/Ticket/Refund/Settings) **schema-only**: tabel+entity ada, service/controller/repo belum dibuat.

AI harus: jangan buat endpoint Event/Order dll kecuali diminta; jangan buat ulang tabel reschedule; ikuti struktur di bawah.

## Tech Stack
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` (3.2.4) | REST + RestTemplate (verifikasi Google) |
| `spring-boot-starter-data-jpa` | ORM |
| `spring-boot-starter-validation` | `@NotBlank @Email @Size` di DTO |
| `spring-boot-starter-security` | `BCryptPasswordEncoder`, `SecurityFilterChain` |
| `postgresql` | Driver |
| `jjwt-api:0.12.5` + `jjwt-impl` + `jjwt-jackson` | JWT HS256 |
| `lombok` | `@Data @Builder` |
| `spring-boot-starter-test` + `spring-security-test` | Test |

Config `application.properties`:
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/db_eventday
spring.datasource.username=postgres
spring.datasource.password=fikko04
spring.jpa.hibernate.ddl-auto=update
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000
google.client-id=   # kosong = tidak validasi aud, isi xxxx.apps.googleusercontent.com untuk validasi ketat
```

## Project Structure (yang benar — kirim ini)
```
src/main/java/com/example/eventday/
├── EventdayApplication.java
├── config/
│   ├── CorsConfig.java          # GLOBAL CORS * (ngrok) — allowedOriginPatterns *, allowCredentials true
│   └── SecurityConfig.java      # BCrypt, STATELESS, .cors(), csrf.disable(), permitAll /api/v1/auth/**, addFilterBefore JwtAuthenticationFilter
├── security/
│   ├── JwtTokenProvider.java    # UTAMA — @Value("${jwt.secret}") + @Value("${jwt.expiration-ms}") jjwt 0.12.5, generate/parse/validate, HS256
│   ├── JwtUtil.java             # COMPAT — sama logic dengan default fallback, dipakai legacy (jangan hapus)
│   └── JwtAuthenticationFilter.java # OncePerRequestFilter, cek Bearer, validate, cek auth.status==ACTIVE && aksesToken==token, set ROLE_*
├── controller/
│   └── AuthController.java      # POST /api/v1/auth/register, POST /api/v1/auth/login, POST /api/v1/auth/google (juga permit /api/auth/** legacy)
├── dto/
│   ├── RegisterRequest.java     # name @NotBlank, email @Email, phone @Size15, password @NotBlank @Size6, nik @Pattern \d{16}, role String
│   ├── LoginRequest.java        # email, password
│   ├── GoogleLoginRequest.java  # idToken @NotBlank (ID Token dari frontend Google GIS)
│   └── AuthResponse.java        # message, userId, name, email, role, token, expiresIn (detik)
├── entity/                      # 11 files = 10 tabel prompt (Settings & AuditLog jadi 1 poin)
│   ├── User.java                # users — @GeneratedValue UUID, email unique, nik unique 16, role ENUM CUSTOMER/ORGANIZER/ADMIN @Enumerated STRING
│   ├── Auth.java                # auth — @ManyToOne User (user_id UNIQUE FK CASCADE), password BCrypt, authGoogle VARCHAR(20) (potong 20 char), aksesToken TEXT, expiredToken, status INACTIVE/ACTIVE
│   ├── Organizer.java           # organizers — @ManyToOne User, nameOrganizer, npwpNumber, aktaPerusahaan, bankName/bankAccountNumber, verificationStatus UNVERIFIED
│   ├── Event.java               # events — @ManyToOne Organizer, title/description/category/venueName/bannerUrl/facility/startDate/endDate/status DRAFT
│   ├── TicketTier.java          # ticket_tiers — @ManyToOne Event, tierName/price(12,2)/totalQuota/availableQuota
│   ├── Booking.java             # bookings — @ManyToOne User + TicketTier, quantity/status PENDING/expiresAt
│   ├── Order.java               # orders — @ManyToOne Booking(UNIQUE)+Customer(User)+Event+TicketTier, quantity/totalAmount/adminFee/status/paymentMethod/transactionIdGateway/paidAt/expiredAt
│   ├── TicketItem.java          # ticket_items — @ManyToOne Order+TicketTier, attendeeEmail/Name/Nik/checkInStatus UNREDEEMED/checkInAt
│   ├── RefundRequest.java       # refund_requests — @ManyToOne Customer+Order, refundAmount/bank* /reason/adminNote/status PENDING/requestedAt/processedAt
│   ├── Settings.java            # settings — BIGSERIAL PK, settings_key UNIQUE
│   └── AuditLog.java            # audit_logs — actorId/actorName/action/detail/createdAt
├── repository/
│   ├── UserRepository.java      # findByEmail, existsByEmail, existsByNik
│   ├── AuthRepository.java      # findByUserUserId, findByAksesToken
│   └── AuditLogRepository.java  # log only
└── service/
    ├── AuthService.java         # register + login + loginWithGoogle(@Value google.client-id, RestTemplate tokeninfo, verify aud/exp/email_verified) + logout
    └── AuditLogService.java     # log(actorId, actorName, action, detail)
```
**DIHAPUS & JANGAN DIBUAT ULANG:** `RescheduleRequest.java` + `RescheduleRequestRepository` + service/controller `EventService/OrderService/TicketService/SettingsService/RefundService/OrganizerService/OrderScheduler` + controller `Event/Order/Payment/Settings/Ticket/Refund/Reschedule/Audit/Organizer` + DTO `CreateEvent/EventResponse/CreateOrder/OrderResponse` etc. — semua sengaja dihapus untuk Auth Only.

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
| `audit_logs` | `audit_id` UUID | — |

Semua `created_at/updated_at/create_by/updated_by` ada; `role/status` VARCHAR default.

## API Endpoints

### ✅ AKTIF — Auth
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | validasi → cek duplikat email/nik → `User.Role.valueOf` fallback CUSTOMER → save `User` → `BCrypt` → save `Auth(INACTIVE)` → audit REGISTER → return tanpa token |
| POST | `/api/v1/auth/login` | Public | find `User` → find `Auth` → `matches` → `jwtTokenProvider.generateToken(userId,email,role.name)` (86400000ms) → update `aksesToken/expiredToken/status ACTIVE` → audit LOGIN → return `token + expiresIn` |
| POST | `/api/v1/auth/google` | Public | terima `idToken` frontend GIS → `GET https://oauth2.googleapis.com/tokeninfo?id_token=` → cek `aud==google.client-id` (jika diisi), `exp`, `email_verified` → find/create `User` by email → find/create `Auth` (password dummy BCrypt, `authGoogle` potong 20 char) → generate JWT sama → audit REGISTER_GOOGLE/LOGIN_GOOGLE |

`POST /api/auth/**` juga permit (legacy).

**JWT Middleware** `JwtAuthenticationFilter.java:23`: header `Authorization: Bearer <token>` → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_<role>` → `anyRequest.authenticated()`.

**Google Flow (backend):** ID Token didapat di **frontend** via `https://accounts.google.com/gsi/client` (`data-client_id=google.client-id`), lalu `POST /api/v1/auth/google {idToken}`. Backend hanya verifikasi, tidak OAuth redirect.

### ⏳ SCHEMA-ONLY (jangan implement kecuali diminta)
`POST /api/events`, `GET /api/events`, `POST /api/orders`, `POST /api/payments/pay/{orderId}`, `POST /api/tickets/scan/{ticketItemId}`, `GET/PUT /api/settings`, `POST /api/refunds` — entity+table ready, 403 jika dipanggil.

## Key Business Logic (Auth Only)
- `CorsConfig.java:10` global `*` untuk ngrok (maxAge 3600, allowCredentials).
- Password tidak pernah di `users`, hanya di `auth.password`. Untuk Google, password dummy UUID BCrypt (kolom NOT NULL).
- Token disimpan di DB untuk invalidasi logout (`AuthService.logout` null-kan token, belum expose endpoint).
- `authGoogle` `VARCHAR(20)` → potong `sub.substring(0,20)` (Google sub ~21 char).

## Known Issues / TODO untuk AI
1. `jwt.secret` hardcoded di properties — prod pindah ke env/Secret Manager.
2. No refresh token, hanya access 24 jam.
3. `@EnableScheduling` masih ada tapi tidak ada job (aman).
4. `Settings.java` pakai `IDENTITY` BIGSERIAL bukan UUID (sesuai DDL).
5. Jangan buat ulang `reschedule_requests`.
6. Google `aud` tidak divalidasi jika `google.client-id` kosong — isi untuk produksi.

## Code Conventions
- Package `com.example.eventday`, entity singular, table plural snake_case
- PK `GenerationType.UUID` kecuali `settings_id`
- Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- DTO validasi `jakarta.validation`, `@Service @Transactional`, `@RestController`
- `createdAt = LocalDateTime.now()` via `@Builder.Default`

## Build & Run
```bash
./mvnw clean compile -DskipTests # BUILD SUCCESS 28 files (2026-09-02, +Google)
./mvnw spring-boot:run
curl -X POST localhost:8081/api/v1/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@mail.com","password":"123456","role":"CUSTOMER"}'
curl -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -X POST localhost:8081/api/v1/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
curl -H "Authorization: Bearer <token>" localhost:8081/any-protected # 403 tanpa token
```
