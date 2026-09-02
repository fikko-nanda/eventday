# AGENTS.md - Eventday Application

## Project Overview

**Eventday** adalah aplikasi Spring Boot untuk penjualan tiket event secara online. Aplikasi ini mengelola event, tiket, pemesanan, pembayaran, dan check-in tiket.

- **Framework**: Spring Boot 4.0.8
- **Java Version**: 21
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Port**: 8081

> **Status Saat Ini (2026-09-02):** Tahap 1 Database Migration 10 tabel SELESAI (FULL SCHEMA). Tahap 2 Service & API baru **Authentication & User Management (Login/Register + JWT)** yang diimplementasi. Modul lain (Event, Booking, Order, Ticket, Payment, etc.) **schema-only** — tabel + entity sudah siap, service/controller belum diaktifkan.

## Tech Stack & Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa` | ORM & database access |
| `spring-boot-starter-webmvc` | REST API endpoints |
| `spring-boot-starter-validation` | Bean validation (DTO constraints) |
| `spring-boot-starter-security` | Security (BCrypt, Filter Chain, JWT) |
| `postgresql` | Database driver |
| `jjwt-api:0.12.5` + `jjwt-impl` + `jjwt-jackson` | JWT generate/parse (HS256) |
| `lombok` | Boilerplate reduction |
| `spring-boot-starter-data-jpa-test` | Test (test scope) |
| `spring-boot-starter-webmvc-test` | MVC test (test scope) |
| `spring-security-test` | Security test (test scope) |

## Project Structure

```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # Entry point, @EnableScheduling (scheduler tidak aktif, OrderScheduler dihapus)
├── config/
│   └── SecurityConfig.java          # BCrypt PasswordEncoder, STATELESS, JWT Filter, permitAll /api/v1/auth/**
├── security/                        # JWT layer (BARU - Tahap 2)
│   ├── JwtUtil.java                 # generateToken(userId,email,role), parse/validate, expiration 86400000ms
│   └── JwtAuthenticationFilter.java # OncePerRequestFilter, cek Authorization: Bearer <token>, validasi vs auth table
├── controller/                      # REST API layer — HANYA AUTH AKTIF
│   └── AuthController.java          # POST /api/v1/auth/register, POST /api/v1/auth/login
├── dto/                             # Data Transfer Objects — HANYA AUTH
│   ├── RegisterRequest.java         # name, email, password, phone, nik, role
│   ├── LoginRequest.java            # email, password
│   └── AuthResponse.java            # message, userId, name, email, role, token, expiresIn
├── entity/                          # JPA entities — 12 entity FULL SCHEMA (DB ready)
│   ├── User.java                    # Table: users
│   ├── Auth.java                    # Table: auth (password hash, akses_token, expired_token, status)
│   ├── Organizer.java               # Table: organizers — SCHEMA ONLY
│   ├── Event.java                   # Table: events — SCHEMA ONLY
│   ├── TicketTier.java              # Table: ticket_tiers — SCHEMA ONLY
│   ├── Booking.java                 # Table: bookings — SCHEMA ONLY
│   ├── Order.java                   # Table: orders — SCHEMA ONLY
│   ├── TicketItem.java              # Table: ticket_items — SCHEMA ONLY
│   ├── RefundRequest.java           # Table: refund_requests — SCHEMA ONLY
│   ├── RescheduleRequest.java       # Table: reschedule_requests — SCHEMA ONLY
│   ├── Settings.java                # Table: settings — SCHEMA ONLY
│   └── AuditLog.java                # Table: audit_logs
├── repository/                      # Spring Data JPA — HANYA AUTH
│   ├── UserRepository.java          # findByEmail, existsByEmail, existsByNik
│   ├── AuthRepository.java          # findByUserUserId, findByAksesToken
│   └── AuditLogRepository.java      # save log (getAll dihapus, service hanya log())
└── service/                         # Business logic — HANYA AUTH
    ├── AuthService.java             # register(), login(), logout() — pakai User+Auth terpisah, JWT
    └── AuditLogService.java         # log(actorId, actorName, action, detail) saja
```

**File yang DIHAPUS pada Tahap 2 (bersih Auth Only):**
- Service: `EventService`, `OrderService`, `TicketService`, `SettingsService`, `RefundService`, `RescheduleService`, `OrganizerService`, `OrderScheduler`
- Controller: `EventController`, `OrderController`, `PaymentController`, `SettingsController`, `TicketController`, `RefundController`, `RescheduleController`, `AuditController`, `OrganizerController`
- DTO: `CreateEventRequest`, `EventResponse`, `CreateOrderRequest`, `OrderResponse`, `PaymentResponse`, `TicketResponse`, `SettingsResponse`, `RefundRequestDto`, `RescheduleRequestDto`, `AuditLogResponse`
- Repository: `EventRepository`, `TicketTierRepository`, `TicketItemRepository`, `OrderRepository`, `BookingRepository`, `OrganizerRepository`, `SettingsRepository`, `RefundRequestRepository`, `RescheduleRequestRepository`

**Tetap ada (schema-only, tidak dipakai service):** semua entity di atas tetap ada agar DDL lengkap, tapi repository/service/controller-nya belum dibuat — akan diaktifkan di tahap berikutnya.

## Database Schema

Migrasi: `src/main/resources/db/migration/V1__init_schema.sql` — PostgreSQL, `uuid-ossp`, 11 tabel (10 utama + audit_logs) dengan FK constraints & index.

| Table | PK | FK | Note |
|---|---|---|---|
| `users` | `user_id` UUID | — | `email` unique, `nik` unique (16 digit), `role` DEFAULT CUSTOMER, `created_at/updated_at` |
| `auth` | `auth_id` UUID | `user_id` UNIQUE → `users` ON DELETE CASCADE | Pisah dari users untuk keamanan: `password` VARCHAR(255) BCrypt, `auth_google`, `akses_token` TEXT, `expired_token` TIMESTAMP, `status` INACTIVE/ACTIVE |
| `organizers` | `organizer_id` UUID | `user_id` UNIQUE → `users` | `verification_status` UNVERIFIED default, `npwp_number`, `akta_perusahaan`, bank — SCHEMA ONLY |
| `events` | `event_id` UUID | `organizer_id` → `organizers` RESTRICT | `title`, `category`, `venue_name`, `banner_url`, `facility`, `start_date/end_date`, `status` DRAFT — SCHEMA ONLY |
| `ticket_tiers` | `tier_id` UUID | `event_id` → `events` CASCADE | `tier_name`, `price` NUMERIC(12,2), `total_quota/available_quota` — SCHEMA ONLY |
| `bookings` | `booking_id` UUID | `user_id` → users, `tier_id` → ticket_tiers | `quantity`, `status` PENDING, `expires_at` — SCHEMA ONLY |
| `orders` | `order_id` UUID | `booking_id` UNIQUE, `customer_id` → users, `event_id` → events, `tier_id` → tiers | `total_amount/admin_fee`, `status` PENDING, `payment_method`, `transaction_id_gateway`, `paid_at/expired_at` — SCHEMA ONLY |
| `ticket_items` | `ticket_item_id` UUID | `order_id` → orders CASCADE, `tier_id` → tiers | `attendee_name/nik/email`, `check_in_status` UNREDEEMED, `check_in_at` — SCHEMA ONLY |
| `refund_requests` | `refund_id` UUID | `customer_id` → users, `order_id` → orders | `refund_amount`, bank fields, `reason/admin_note`, `status` PENDING, `requested_at/processed_at` — SCHEMA ONLY |
| `settings` | `settings_id` BIGSERIAL | — | `settings_key` UNIQUE (ADMIN_FEE, ORDER_EXPIRY_MINUTES, BOOKING_EXPIRY_MINUTES), `settings_value` — SCHEMA ONLY |
| `audit_logs` | `audit_id` UUID | `actor_id` nullable | `actor_name`, `action`, `detail`, `created_at` — dipakai `AuditLogService.log()` untuk REGISTER/LOGIN |

Default data: `ADMIN_FEE=5000`, `ORDER_EXPIRY_MINUTES=15`, `BOOKING_EXPIRY_MINUTES=10` (insert ON CONFLICT DO NOTHING).

## API Endpoints

### ✅ Auth — AKTIF (`/api/v1/auth`) — Implementasi Tahap 2
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Register user baru (validasi, cek duplikat email/NIK, simpan users+auth hash) | Public |
| POST | `/api/v1/auth/login` | Login (cek password BCrypt, generate JWT, update auth.akses_token/expired_token/status ACTIVE) | Public |

> Alias lama `/api/auth/**` masih di-permitAll di `SecurityConfig` untuk backward compat, tapi dokumentasi resmi pakai `/api/v1/auth`.

**Middleware JWT** (`security/JwtAuthenticationFilter.java:15`):
- Cek header `Authorization: Bearer <token>`
- Validasi signature & expiry via `JwtUtil`
- Cek `auth.status=ACTIVE` & token == `akses_token` di DB (support logout/invalidasi)
- Set `SecurityContext` dengan `ROLE_<role>` → semua route selain `/api/v1/auth/**` butuh autentikasi (`anyRequest().authenticated()`, `SessionCreationPolicy.STATELESS`)

### ⏳ Modul Lain — SCHEMA ONLY (Belum Diimplementasi, akan datang)
| Modul | Endpoint Rencana | Status |
|---|---|---|
| Events | `POST /api/events`, `GET /api/events`, `GET /api/events/{id}` | DB+Entity ready, controller/service dihapus |
| Orders/Bookings | `POST /api/orders` | DB ready |
| Payments | `POST /api/payments/pay/{orderId}` | DB ready |
| Tickets | `POST /api/tickets/scan/{ticketItemId}` | DB ready |
| Settings | `GET/PUT /api/settings/**` | DB ready |
| Refunds | `POST/GET/PUT /api/refunds/**` | DB ready |
| Reschedules | `POST/PUT/GET /api/reschedules/**` | DB ready |
| Audit | `GET /api/audit/**` | Hanya `log()` internal, controller dihapus |

## Database Configuration

```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/db_eventday
spring.datasource.username=postgres
spring.datasource.password=fikko04
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT (Tahap 2)
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000

# Order (fallback, source of truth ada di settings table)
app.order.admin-fee=5000
app.order.expiry-minutes=15
```

DDL: `ddl-auto=update` untuk dev; production disarankan Flyway dengan `V1__init_schema.sql`.

## Key Business Logic

### ✅ Implemented (Tahap 2)
1. **Register Flow** (`AuthService.java:29`): Validasi `@NotBlank/@Email/@Size/@Pattern` → cek `existsByEmail/existsByNik` → `role` normalisasi (CUSTOMER/ORGANIZER/ADMIN, fallback CUSTOMER) → save `User` → hash BCrypt → save `Auth(status=INACTIVE)` → audit `REGISTER` → return `AuthResponse` tanpa token.
2. **Login Flow** (`AuthService.java:74`): Cari `User` by email → ambil `Auth` by `userId` → `passwordEncoder.matches` → generate JWT (`JwtUtil:18` claim userId/email/role, expiry 24 jam) → update `auth.akses_token/expired_token/status=ACTIVE` → audit `LOGIN`/`LOGIN_FAILED` → return `AuthResponse` dengan `token` & `expiresIn` (detik).
3. **JWT Auth** (`JwtUtil.java`, `JwtAuthenticationFilter.java:15`, `SecurityConfig.java:20`): HS256, secret minimal 32 char, validasi signature + DB check, stateless, `Authorization: Bearer`.
4. **Audit Trail Minimal**: `AuditLogService.log()` dipanggil di register/login saja (tabel `audit_logs` siap untuk modul lain).

### ⏳ Planned (Schema Ready, belum ada logic)
- Order 15-menit expiry + auto-cancel 60s, Ticket check-in, Admin fee via settings, Refund via EO, Reschedule via super admin — semua tabel & entity sudah ada, service akan ditambah bertahap.

## Known Issues

1. **JWT Secret Hardcoded di properties** — untuk production harus pindah ke env variable / Secret Manager, minimal 32 char.
2. **No Refresh Token** — hanya access token 24 jam, belum ada refresh/rotation. Logout via `AuthService.logout()` meng-null-kan token di DB tapi belum ada endpoint `/logout` yang expose.
3. **Config Duplication (minor)** — `app.order.*` di properties vs `settings` table; saat ini `Auth` tidak pakai, tapi modul Order nanti harus konsisten pakai `settings` sebagai source of truth.
4. **Scheduler Masih @EnableScheduling** — annotation masih di `EventdayApplication` tapi `OrderScheduler` sudah dihapus, tidak ada job yang jalan (tidak berbahaya, tapi bisa dihapus atau biarkan untuk tahap Order).
5. **Audit GET Belum Ada** — `AuditLogService` hanya `log()`, tidak ada `getAllLogs`/`getLogsByActor` dan controller `/api/audit` sudah dihapus (sengaja untuk Auth Only).

## Code Conventions

- Package: `com.example.eventday`
- Entity naming: singular (User, Auth, Event), Table: plural snake_case (users, auth, events)
- UUID PK `GenerationType.UUID` (kecuali `settings_id` BIGSERIAL)
- Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- DTO terpisah dari entity, validasi di `dto/*` pakai `jakarta.validation`
- `@Service @Transactional` untuk business logic, `@RestController` untuk API
- Security: `BCryptPasswordEncoder`, `JwtUtil` di `security/`, filter di `SecurityConfig`
- `created_at/updated_at/create_by/updated_by` di semua tabel (handle via JPA atau manual set di service)

## Build & Run

```bash
# Build (verfied BUILD SUCCESS 2026-09-02, 25 files)
./mvnw clean compile -DskipTests
./mvnw clean install

# Run
./mvnw spring-boot:run

# Cek endpoint Auth
curl -X POST http://localhost:8081/api/v1/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@example.com","password":"secret123","role":"CUSTOMER"}'
curl -X POST http://localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@example.com","password":"secret123"}'
# pakai token:
curl http://localhost:8081/api/v1/auth/me -H "Authorization: Bearer <token>" # contoh protected route nanti

# Run with profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing

```bash
./mvnw test
# Catatan: butuh PostgreSQL db_eventday running. Untuk unit test AuthService, mock UserRepository/AuthRepository/JwtUtil.
```
