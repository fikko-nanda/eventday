# AGENTS.md - Eventday Ticketing Backend

## Project Overview
Spring Boot 3.2.4 (Java 21, target Java 25) REST API, PostgreSQL, Maven, port 8082. Eventday ticketing: user/organizer/event/ticket/booking/order/payment/refund management.

> **Status 2026-09-14 (rev.6):** PR #10 (merged → HEAD `2e1cdef`) **rampungkan Checkout** — `attendees` beneran tersimpan ke `order_attendees` (`model/Attendee` + `AttendeeRepository`, bukan stub) + tambah `POST /api/v1/checkout/calculation` (tax 10%) + `POST /api/v1/checkout/process` (→ `WAITING_PAYMENT`) + `GET /api/v1/orders/status` (polling) → `CheckoutController` kini 8 endpoint. `SecurityConfig` publik untuk `/api/v1/events/**` + `/api/v1/terms-conditions` + `/api/v1/privacy-policy` kini **SUDAH COMMITTED** (2 terakhir belum ada controller → 404). Sisa gap: kuota tak decrement, tiket tak diterbitkan otomatis. DB lokal `localhost:5432/eventday` (`postgres`).

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
spring.datasource.url=jdbc:postgresql://localhost:5432/eventday
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
spring.mail.username=3b9dd392077cb5
spring.mail.password=a7335e17e24c70
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
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %logger{0} : %m%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=logs/eventday.log
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG
```

## Project Structure
```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # @SpringBootApplication @EnableScheduling
├── controller/
│   ├── AuthController.java           # POST /api/v1/auth/* → ApiResponse.created(201)/ok(200)/badRequest(400), Set-Cookie access_token HttpOnly
│   ├── EventController.java          # GET /api/v1/events, /events/featured, /events/{id} → ApiResponse.ok(200)/notFound(404)
│   ├── HomeSearchController.java     # MODUL 01: GET /api/v1/home/hero-banner|event-card|locations + /api/v1/search/results|locations|categories (publik) → ApiResponse.ok(200), null-safe []
│   ├── CheckoutController.java       # POST /api/v1/checkout/initiate|attendees|calculation|process + GET /api/v1/checkout/summary|orders/status|orders/{id}/total-amount|expired-time (auth) → ApiResponse.success
│   ├── PaymentController.java        # GET /api/v1/payments/methods|methods/virtual-account + POST /api/v1/payments/charge (auth) → mock VA
│   ├── TicketController.java         # GET /api/tickets/user/{email} + POST /api/tickets/scan (auth, base TANPA /v1) → check-in QR
│   └── HomeController.java           # GET "/" → ApiResponse.ok("Eventday API Server is Running!","OK")
├── config/
│   ├── CorsConfig.java               # @Bean CorsFilter: allowedOriginPatterns "*", allowCredentials true, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, exposedHeaders Authorization/Content-Type, maxAge 3600 (ngrok + localhost:5173)
│   ├── ApiLoggingFilter.java         # OncePerRequestFilter format ASCII: [IN] [reqId] METHOD URI | IP | UA → [OUT] ... -> status (duration) user/auth/ip + [SLOW!] jika >1s (tanpa warna/box-drawing)
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice → ApiResponse.badRequest/unauthorized/forbidden/internalError (400/401/403/500)
│   └── SecurityConfig.java           # BCrypt, STATELESS, permitAll: /,/error + /api/v1/auth/** + /api/v1/home/** + /api/v1/search/** + /api/v1/events/** + /api/v1/terms-conditions + /api/v1/privacy-policy (COMMITTED); 401/403 via ObjectMapper
├── security/
│   ├── JwtTokenProvider.java         # @Value jwt.secret + jwt.expiration-ms, HS256 generate/validate
│   ├── JwtUtil.java                  # COMPAT — jangan hapus
│   └── JwtAuthenticationFilter.java  # OncePerRequestFilter, Bearer → validate → ROLE_*
├── dto/
│   ├── ApiResponse.java              # Wrapper {msg, status, data} @JsonInclude ALWAYS — helper created(201)/ok(200)/badRequest(400)/unauthorized(401)/forbidden(403)/notFound(404)/internalError(500)
│   ├── RegisterRequest.java          # name @NotBlank @Size100, email @Email unique, username @NotBlank @Size3-20 @Pattern ^[a-zA-Z0-9_]+$ unique, phone @Size15, password @NotBlank @Size6, nik @Pattern \d{16} unique, role String
│   ├── LoginRequest.java             # email, username, identifier, password + getIdentifier() (contains "@" → email else username, fallback)
│   ├── GoogleLoginRequest.java       # idToken @NotBlank
│   ├── VerifyOtpRequest.java         # email @NotBlank @Email, otpCode @NotBlank
│   ├── ResendOtpRequest.java         # email @NotBlank @Email
│   ├── ResetPasswordRequest.java     # email @NotBlank @Email, code @JsonAlias token/otp, token, newPassword @JsonAlias password + getEffectiveCode()/getEffectiveNewPassword() trim
│   ├── AuthResponse.java             # message, userId, name, email, username, role, token @JsonIgnore (hidden dari JSON, kirim via Set-Cookie access_token HttpOnly), expiresIn — dibungkus ApiResponse.data
│   ├── HeroBannerResponse.java       # MODUL 01: id, title, bannerUrl, eventDate, targetUrl (/events/{id})
│   ├── EventCardResponse.java        # MODUL 01: id, title, posterUrl, location, category, categoryLabel, startDate, dateDisplay, lowestPrice, priceDisplay
│   ├── EventCatalogResponse.java     # content (List<EventItem>), page, size, totalElements, totalPages — inner class: id, title, category, categoryLabel, date, dateDisplay, time, location, price, priceDisplay, image, status, isFeatured
│   ├── EventDetailResponse.java      # id, title, category, categoryLabel, date, dateDisplay, location, description, image, status, statusLabel, facilities (List<String>), lineup (List<LineupItem>), tickets (List<TicketItem>)
│   ├── CheckoutSummaryResponse.java  # orderId, orderNumber, eventTitle, ticketTierName, quantity, pricePerTicket, subtotal, adminFee, discountAmount, totalAmount, expiredAt
│   ├── AttendeeRequest.java          # orderId + List<AttendeeItem(fullName,email,phoneNumber,identityNumber)> — dipakai saveAttendees (PR #10; AttendeesRequest.java lama TIDAK dipakai)
│   ├── CalculationRequest.java       # tierId, quantity, discountAmount (PR #10)
│   ├── CalculationResponse.java      # subtotal, adminFee, tax (subtotal×10%), discount, totalAmount (PR #10)
│   ├── ProcessCheckoutRequest.java   # orderId (PR #10)
│   ├── InitiateCheckoutRequest.java  # tierId, quantity (PR #9)
│   ├── PaymentChargeRequest.java     # orderId, paymentMethod (VIRTUAL_ACCOUNT), bankCode (BCA/MANDIRI/BRI)
│   ├── PaymentChargeResponse.java    # orderId, orderNumber, totalAmount, paymentMethod, bankCode, virtualAccountNumber, expiredAt
│   └── TicketItemResponse.java       # ticketId, ticketCode, eventTitle, categoryName, eventDate, location, status — BELUM DIPAKAI controller manapun
├── entity/                           # 12 files = 12 tabel (password_reset_tokens dihapus, digabung ke auth) — tabel ke-13 `order_attendees` ada di `model/Attendee` (PR #10)
│   ├── User.java                     # users — UUID PK, email unique, username unique 20, nik unique 16, role ENUM CUSTOMER/ORGANIZER/ADMIN
│   ├── Auth.java                     # auth — @ManyToOne User (user_id UNIQUE CASCADE), password BCrypt, authGoogle VARCHAR(20), aksesToken TEXT, expiredToken, status INACTIVE/ACTIVE, resetToken VARCHAR(255), resetExpiredAt
│   ├── Otp.java                      # otp — @ManyToOne User, otpCode VARCHAR(10), expiredAt
│   ├── Organizer.java                # organizers — @ManyToOne User, verificationStatus UNVERIFIED
│   ├── Event.java                    # events — @ManyToOne Organizer, status DRAFT, isFeatured BOOLEAN
│   ├── TicketTier.java               # ticket_tiers — @ManyToOne Event, price NUMERIC(12,2), totalQuota, availableQuota
│   ├── Booking.java                  # bookings — @ManyToOne User + TicketTier, status PENDING
│   ├── Order.java                    # orders — @ManyToOne Booking(UNIQUE)+Customer+Event+TicketTier
│   ├── TicketItem.java               # ticket_items — @ManyToOne Order+TicketTier, checkInStatus UNREDEEMED
│   ├── RefundRequest.java            # refund_requests — @ManyToOne Customer+Order, bank fields NOT NULL
│   ├── Settings.java                 # settings — BIGSERIAL PK, settings_key UNIQUE
│   └── AuditLog.java                 # audit_logs — actorId/actorName/action/detail/createdAt
├── model/
│   └── Attendee.java                 # order_attendees — PK Long IDENTITY, orderId (String, BUKAN FK/UUID), fullName, email, phoneNumber, identityNumber (PR #10)
├── repository/
│   ├── UserRepository.java           # findByEmail, findByUsername, existsByEmail/Username/Nik
│   ├── AuthRepository.java           # findByUserUserId, findByAksesToken
│   ├── OtpRepository.java            # findByUserUserIdAndOtpCode, findByUserUserId, deleteByUserUserId
│   ├── EventRepository.java          # findPublishedEvents(category,search,location,pageable), findFeaturedEvents(pageable), findPublishedEventById(id)
│   │                               # MODUL 01: findDistinctLocations(), findDistinctCategories(), searchPublishedEvents(keyword,category,location,date,pageable)
│   ├── TicketTierRepository.java     # findByEvent(event)
│   ├── OrderRepository.java          # findByCustomerUserId (PR #9)
│   ├── AttendeeRepository.java       # findByOrderId (PR #10)
│   ├── TicketItemRepository.java     # findByOrderCustomerUserId/UserEmail/OrderId (PR #9)
│   └── AuditLogRepository.java       # log only
└── service/
    ├── AuthService.java              # register + login + loginWithGoogle + verifyOtp + resendOtp + resetPassword (langsung di auth.resetToken) + logout
    ├── EventService.java             # getEvents(filter+pagination), getFeaturedEvents(3 hero), getEventDetail(UUID) + format helpers (categoryLabel, priceDisplay, dateDisplay, facilities parsing)
    ├── HomeSearchService.java        # MODUL 01: getHeroBanners(max 5) + getHomeEventCards(pageable) + getLocations/getCategories(null-safe []) + searchEvents(keyword,category,location,date,pageable) + parseSort
    ├── AuditLogService.java          # log(actorId, actorName, action, detail)
    ├── EmailService.java             # sendOtpEmail + sendResetPasswordEmail (Mailtrap, @Value app.mail.*, gagal → log warn tidak throw)
    ├── OrderService.java             # createOrder: cek kuota (TIDAK decrement) → subtotal+adminFee → save PENDING+expiredAt; saveAttendees→order_attendees; calculateCheckout(adminFee+tax10%−discount); processCheckout→WAITING_PAYMENT; getOrderStatus (PR #10)
    ├── PaymentService.java           # getCheckoutSummary(orderId) + processPaymentCharge: mock VA "88325"+timestamp → status WAITING_PAYMENT
    └── TicketService.java            # generateTicket (TIDAK DIPANGGIL siapapun!) + getTicketsByEmail + validateAndUseTicket (QR=UUID, CHECKED_IN)
```

**DIHAPUS & JANGAN DIBUAT ULANG:** `RescheduleRequest` + `PasswordResetToken`/`password_reset_tokens` (sudah digabung ke `auth` sesuai mentor). PR #9 mengisi 7 placeholder PR #8; PR #10 (HEAD) lanjut isi `attendees` real + `calculation`/`process`/`orders/status` — sisa yang belum ada (jangan buat kecuali diminta): service/controller/DTO untuk Organizer, Settings, Refund, Reschedule.

## Database Schema
Migrasi: `src/main/resources/db/migration/V1__init_schema.sql` (`uuid-ossp`, 12 tabel, FK, index, default `ADMIN_FEE=5000`, `ORDER_EXPIRY_MINUTES=15`, `BOOKING_EXPIRY_MINUTES=10`). Tabel ke-13 `order_attendees` (PR #10) dibuat Hibernate `ddl-auto=update` dari `model/Attendee` — TIDAK ada di V1.

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

**Tabel ke-13 — `order_attendees`** (bukan di V1): PK Long IDENTITY (`model/Attendee`), kolom `order_id` String (BUKAN FK, BUKAN UUID) + `full_name/email/phone_number/identity_number`.

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

### ✅ AKTIF — Customer Event Catalog (Public — lihat catatan SecurityConfig)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/v1/events` | Publik* | `findPublishedEvents(category,search,location,pageable)` → map ke `EventCatalogResponse` (categoryLabel, priceDisplay, dateDisplay) → pagination `{content,page,size,totalElements,totalPages}` |
| GET | `/api/v1/events/featured` | Publik* | `findFeaturedEvents()` → max 3 event `isFeatured=true` → same shape `EventCatalogResponse` |
| GET | `/api/v1/events/{id}` | Publik* | `findPublishedEventById(id)` → map `EventDetailResponse` + `TicketTier` list + parse facilities TEXT → `List<String>` → lineup placeholder |

> \*SUDAH COMMITTED di HEAD: `SecurityConfig.java:67` `/api/v1/events/**` permitAll (juga `:70` `/api/v1/terms-conditions` + `/api/v1/privacy-policy`) — akses publik tanpa Bearer, bisa dites via browser/curl.

### ✅ AKTIF — Modul 01 Home & Search (Public, tanpa JWT)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/v1/home/hero-banner` | Public | `findFeaturedEvents(page 0 size 5)` → map `HeroBannerResponse` (targetUrl `/events/{id}`), kosong → `[]` |
| GET | `/api/v1/home/event-card?page&size` | Public | `findPublishedEvents(null,null,null,pageable)` → map `EventCardResponse` (lowestPrice = min tier, fallback 0) → pagination `{content,page,size,totalElements,totalPages}` |
| GET | `/api/v1/home/locations` | Public | `findDistinctLocations()` (DISTINCT `venueName` PUBLISHED), null-safe `[]` |
| GET | `/api/v1/search/results` | Public | `searchPublishedEvents(keyword,category,location,date,pageable)` — keyword di title/description/venue, kategori dinormalisasi `Semua/ALL→null`, `date` `YYYY-MM-DD` |
| GET | `/api/v1/search/locations` | Public | sama dengan home/locations (opsi filter search) |
| GET | `/api/v1/search/categories` | Public | `findDistinctCategories()` (DISTINCT `category` PUBLISHED), null-safe `[]` |

`GET /` dan `GET /error` permit, `POST /api/auth/**` legacy permit. Lihat `SecurityConfig.java:55`.

### ✅ AKTIF — Checkout/Order (PR #9 + PR #10, perlu login)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/v1/checkout/initiate` | Bearer | body `{tierId,quantity}` → `OrderService.createOrder`: cek kuota (TIDAK decrement!) → subtotal+adminFee(5000) → save `Order(PENDING)` + `expiredAt` +15min → return entity `Order` langsung |
| POST | `/api/v1/checkout/attendees` | Bearer | (PR #10, bukan stub) body `{orderId,attendees[{fullName,email,phoneNumber,identityNumber}]}` → `OrderService.saveAttendees` simpan ke `order_attendees` → return `List<Attendee>` |
| POST | `/api/v1/checkout/calculation` | Bearer | (PR #10) body `{tierId,quantity,discountAmount}` → `CalculationResponse`: subtotal+adminFee+tax(subtotal×10%)−discount → totalAmount (TIDAK simpan order) |
| POST | `/api/v1/checkout/process` | Bearer | (PR #10) body `{orderId}` → status → `WAITING_PAYMENT` (tanpa cek PENDING; beda dgn `/payments/charge`) |
| GET | `/api/v1/orders/status?orderId=` | Bearer | (PR #10) `OrderService.getOrderStatus` polling → `{orderId, status}` |
| GET | `/api/v1/checkout/summary?orderId=` | Bearer | `PaymentService.getCheckoutSummary` → `CheckoutSummaryResponse` (orderNumber `ORD-XXXXXXXX`) |
| GET | `/api/v1/orders/{orderId}/total-amount` | Bearer | `{totalAmount}` dari summary |
| GET | `/api/v1/orders/{orderId}/expired-time` | Bearer | `{expiredAt}` dari summary |

### ✅ AKTIF — Payment mock VA (PR #9, perlu login)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/v1/payments/methods` | Bearer | hardcoded `["VIRTUAL_ACCOUNT","E_WALLET","CREDIT_CARD"]` |
| GET | `/api/v1/payments/methods/virtual-account` | Bearer | hardcoded channel BCA/MANDIRI/BRI |
| POST | `/api/v1/payments/charge` | Bearer | body `{orderId,paymentMethod,bankCode}` → cek `PENDING` → mock VA `"88325"+timestamp` → status jadi `WAITING_PAYMENT` (bukan gateway asli!) |

### ✅ AKTIF — Ticket scan (PR #9, perlu login, base TANPA /v1)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/tickets/user/{email}` | Bearer | `getTicketsByEmail` — kosong sampai `generateTicket()` dipanggil (saat ini TIDAK DIPANGGIL siapapun!) |
| POST | `/api/tickets/scan` | Bearer | body `{ticketCode: "<UUID ticketItemId>"}` → `TIKET_VALID` / `TIKET_SUDAH_DIPAKAI` (`CHECKED_IN`+`checkInAt`) / 400 format tak valid

**JWT Middleware** `JwtAuthenticationFilter.java:36`: `Authorization: Bearer <token>` **atau** `Cookie: access_token` (`resolveToken()`) → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`. `AuthResponse.token` `@JsonIgnore` — token hanya via `Set-Cookie` HttpOnly, tidak di Network → Response.

**Google Flow:** ID Token dari `https://accounts.google.com/gsi/client` (`data-client_id=google.client-id`) → `POST /google {idToken}`. Backend hanya verifikasi, tidak redirect.

### ⏳ SCHEMA-ONLY (jangan implement kecuali diminta)
`GET/PUT /api/settings`, `POST /api/refunds`, Organizer register/dashboard, User profile/transactions — entity+table ready (atau belum ada service), 401/403/404 jika dipanggil. `/api/v1/terms-conditions` + `/api/v1/privacy-policy` permitAll tapi BELUM ADA controller → 404.

## Key Business Logic (Auth + Event Catalog)
- `CorsConfig.java:14` `@Bean CorsFilter`: `allowedOriginPatterns "*"`, `allowCredentials true`, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, `allowedHeaders "*"`, `exposedHeaders Authorization/Content-Type`, `maxAge 3600` untuk ngrok + `localhost:5173` (catatan: `POST /register` 401 bukan CORS, tapi `BASE` tanpa `/api/v1/auth`).
- Password hanya di `auth.password`, tidak di `users`. Untuk Google, dummy UUID BCrypt (kolom NOT NULL).
- Token JWT disimpan di DB `auth.aksesToken` untuk invalidasi logout (`AuthService.logout` null-kan token, belum expose endpoint).
- `authGoogle` `VARCHAR(20)` → `sub.substring(0,20)`.
- `EmailService.java` Mailtrap sandbox — gagal → `log.warn` tidak throw, OTP/token tetap bisa dilihat di log.
- Reset Password **digabung ke `auth`**: `auth.reset_token` + `auth.reset_expired_at` (mentor request, tidak lagi tabel terpisah). `ResetPasswordRequest` trim code & alias `token`/`otp`.
- Register flow: `register()` generates 6-digit OTP → `otp` exp 5min → email. `verifyOtp()` → `ACTIVE`. `resendOtp()` invalidates old → new.
- Modul 01 publik: `HomeSearchController` tanpa JWT (`SecurityConfig` permitAll `/api/v1/home/**` + `/api/v1/search/**`). Entity pakai `venueName` (bukan `location`) + `status PUBLISHED` (bukan `ACTIVE`) — query distinct/search menyesuaikan. `size` di event-card = jumlah data/halaman pagination, bukan CSS.
- `@EnableScheduling` aktif di `EventdayApplication.java` tapi belum ada job.
- Checkout PR #9+#10: `initiate` return entity `Order` langsung (relasi lazy — butuh OpenEntityManagerInView); kuota dicek tapi tidak dikurangi; `attendees` (PR #10) real: simpan ke `order_attendees` (PK Long IDENTITY, `order_id` String BUKAN FK/UUID); tambahan PR #10: `calculation` (tax 10%), `process` (→ `WAITING_PAYMENT`, TANPA cek PENDING — beda dengan `/payments/charge` yang wajib PENDING), `orders/status` (polling).
- Payment PR #9: mock VA saja, tidak ada gateway; status `WAITING_PAYMENT` di luar enum lama (`PENDING/PAID/EXPIRED/CANCELLED`).
- Ticket PR #9: base path inkonsisten `/api/tickets` (tanpa `/v1`); `generateTicket()` belum dipanggil alur manapun sehingga my-tickets kosong; `TicketItemResponse` belum dipakai.
- Logging Spring Boot aktif `logging.level.com.example.eventday=DEBUG` → `logs/eventday.log`. Console format ASCII `%d %5p [%t] %logger : %m%n` + `ApiLoggingFilter` `[IN] [reqId] METHOD URI | IP | UA` → `[OUT] ... -> status (duration) user/auth/ip` + `[SLOW!]` jika >1s (tanpa warna/box-drawing).

## Known Issues / TODO
1. `jwt.secret` hardcoded — prod pindah env/Secret Manager.
2. No refresh token, hanya access 24 jam.
3. Java upgrade plan 21→25 di `.github/modernize/...` — **JANGAN JALANKAN** kecuali diminta.
4. `Settings.java` pakai `IDENTITY` BIGSERIAL bukan UUID (sesuai DDL).
5. Jangan buat ulang `reschedule_requests` & `password_reset_tokens` (sudah digabung).
6. `google.client-id` sudah **diisi** — validasi aud aktif.
7. `AuthService.logout(UUID)` ada tapi belum expose endpoint REST.
8. `register()` OTP di `otp` table 5 menit, reset code di `auth` 15 menit.
9. DB lokal `localhost:5432/eventday` (`postgres`) — seed/tes ke DB ini. Jangan pakai kredensial shared `192.168.28.28` dari PR #8.
10. PR #9 mengisi 7 placeholder PR #8; PR #10 menambah `calculation`/`process`/`orders/status` + real `attendees` (stub dihilangkan). Gap tersisa: kuota tak decrement, tiket tak diterbitkan otomatis, `order_attendees.order_id` plain String (bukan FK/UUID), tabel `order_attendees` tidak ada di V1 (Hibernate buat sendiri).

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
curl "localhost:8082/api/v1/events"  # list events — PUBLIK (SecurityConfig events/** COMMITTED)
curl -H "Authorization: Bearer <token>" "localhost:8082/api/v1/events/featured"  # hero slider
curl -H "Authorization: Bearer <token>" "localhost:8082/api/v1/events/{id}"  # detail event
# Modul 01 publik (tanpa token):
curl "localhost:8082/api/v1/home/hero-banner"
curl "localhost:8082/api/v1/home/event-card?page=0&size=12"
curl "localhost:8082/api/v1/home/locations"
curl "localhost:8082/api/v1/search/results?keyword=neon&category=MUSIC_FESTIVAL&page=0&size=12"
curl "localhost:8082/api/v1/search/locations"
curl "localhost:8082/api/v1/search/categories"
# Checkout/Payment/Ticket (perlu login → cookie/Bearer):
curl -b cookies.txt -X POST localhost:8082/api/v1/checkout/initiate -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2}'
curl -b cookies.txt -X POST localhost:8082/api/v1/checkout/attendees -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>","attendees":[{"fullName":"Budi","email":"budi@mail.com","phoneNumber":"08123456","identityNumber":"3201234567890123"}]}'
curl -b cookies.txt -X POST localhost:8082/api/v1/checkout/calculation -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2,"discountAmount":0}'
curl -b cookies.txt -X POST localhost:8082/api/v1/checkout/process -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>"}'
curl -b cookies.txt "localhost:8082/api/v1/orders/status?orderId=<order-uuid>"
curl -b cookies.txt "localhost:8082/api/v1/checkout/summary?orderId=<order-uuid>"
curl -b cookies.txt -X POST localhost:8082/api/v1/payments/charge -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>","paymentMethod":"VIRTUAL_ACCOUNT","bankCode":"BCA"}'
curl -b cookies.txt localhost:8082/api/v1/payments/methods
curl -b cookies.txt -X POST localhost:8082/api/tickets/scan -H "Content-Type: application/json" -d '{"ticketCode":"<ticketItem-uuid>"}'
```

## Reference Files
- `API.md` — full API documentation (frontend handoff, lengkap curl & response)
- `HELP.md` — Spring Boot help
- `.vscode/settings.json` — IDE config

