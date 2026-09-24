# AGENTS.md - Eventday Ticketing Backend

## Project Overview
Spring Boot 3.2.4 (Java 21, target Java 25) REST API, PostgreSQL, Maven, port 8082. Eventday ticketing: user/organizer/event/ticket/booking/order/payment/refund management.

> **Status 2026-09-14 (rev.9):** E2E bugfix (4 bug, verified via curl): ① `checkout/initiate` lazy-proxy 500 → `@JsonIgnoreProperties` di `Order`/`TicketItem`; ② `process→charge` konflik → `charge` terima `PENDING`/`WAITING_PAYMENT` + `process` guard anti-downgrade; ③ `logout` bikin akun mati (`INACTIVE`) → hanya null-kan token; ④ `/admin/**` terbuka untuk CUSTOMER → `hasRole("ADMIN")` + register paksa `CUSTOMER` (tutup eskalasi `role=ADMIN`).

> **Status 2026-09-15 (rev.10+):** Merge PR admin EO & settings (`4836263`) → `AdminSettingsController`, `AdminEoController`, `AdminSettingsService`, `AdminEoService` SEMUA ADA di main. Akar masalah 401 checkout ketemu di **frontend** — `authService.js` 6× `fetch` tanpa `credentials: 'include'` → cookie `access_token` tidak pernah terkirim. Fix = tambah `credentials: "include"` di 6 fetch tsb. `pom.xml` `java.version` 25→21 (JDK terinstal 21, BUILD FAILURE); seed 3 event coba (total 7 PUBLISHED).

> **Status 2026-09-16 (rev.11):** Audit 80 path (86 alias). Temuan: ① `LegalController` BUG — map `/terms-conditions` & `/privacy-policy` tanpa `/api/v1` prefix vs `SecurityConfig:70` permit `/api/*` → tak cocok → **butuh login**; ② Admin Payouts 4 endpoint BARU (`AdminPayoutController` + `AdminPayoutService` + 3 DTO) sharing tabel `refund_requests` tanpa discriminator; ③ Admin Settings +3 (`audit-logs/export`, `export/csv`, `settings/upload-logo`); ④ `RefundRequestEntity` +6 kolom payout; `Settings.settingsValue` 50→255; `RefundRepository` +3 query, `AuditLogRepository` +`findAllForExport`; ⑤ `PaymentService.processPaymentCharge()` dead code; ⑥ Organizer 20 endpoint (bukan 21) SEMUA MOCK; Refund banks MOCK, history REAL.

> **Status 2026-09-16 (rev.12):** Re-scan penuh. Koreksi: ① `OrganizerService` 638 baris (PR #21 `d75de2b`) ternyata **HYBRID DB-backed + mock fallback** (flag `"mock":true`/`"real":true`) — 19 REAL, 1 STUB (`auth/logout`); ② `rejectionReason` EO **opsional** (bukan wajib saat REJECTED); ③ Total **86 path unik: 78 REAL, 7 MOCK, 1 PARTIAL**; ④ Tambah **API.md §17 Endpoint Reference** (method/path/auth/body/query/response per endpoint, verified dari DTO); ⑤ `agent.md` **tidak ada** di repo (yang ada `AGENTS.md`); backend **tanpa AI agent/WebSocket/SSE/LLM/tool-call** (verified via grep) — interaksi murni REST.

> **Status 2026-09-21 (Phase 1 Admin Core CRUD + rev.14):** 19 file baru (11 DTO + 3 Service + 3 Controller + 1 Util + 1 Migration), 4 Repository modified. **18 endpoint admin baru aktif:** Event (8) + Ticket (7) + Transaction (4). BUILD SUCCESS (144 files compiled). Tests: 20 PASS, 2 FAIL (pre-existing JWT bypass + AuthResponse token assertion — bukan dari Phase 1). Refactoring: 3× esc() → CsvUtil, 3 N+1 query fixed, dead code removed. Migration: V2__admin_features.sql (indexes only). Security: sudah dilindungi `hasRole("ADMIN")` di SecurityConfig. ⚠️ File tambahan tercommit: AdminEventController, OrganizerEventController, FileStorageService.

> **Status 2026-09-22 (rev.15, HEAD `4680644` + committed):** PR #35-43 + rev.14 fixes + RBAC & null-safety fixes. Perubahan besar vs rev.14: ① **RBAC Matriks Otorisasi** — SecurityConfig eksplisit: `/api/organizer/**` → `hasAnyRole("ORGANIZER","ADMIN")`, `/api/admin/**` → `hasRole("ADMIN")`, `GET /api/events/**` publik hanya PUBLISHED; ② **Organizer Event Detail** — `GET /api/organizer/events/{id}` baru: akses event DRAFT/PUBLISHED milik sendiri, validasi kepemilikan 403 jika bukan pemilik; ③ **Null-Safety Facility/Lineup** — `facility`, `lineup` aman dari NPE (default `""`) di EventService, OrganizerEventService, AdminEventService; ④ **Sales Summary** — default 0 untuk `tickets_sold`, `total_revenue` saat belum ada transaksi; ⑤ **DTO Only Response** — semua endpoint pakai DTO/Map, tidak ada raw entity → hindari circular JSON; ⑥ **Error Standar** — 404 Not Found, 403 Forbidden struktur teratur. Test: 20 PASS / 2 FAIL pre-existing.

> **Status 2026-09-23 (rev.17):** Isolasi Admin↔EO final + fix sort/EO create/refund schema. ① **`CreateEventRequest` + `organizerId` (UUID opsional)** — `POST /api/admin/events`: `organizerId` dikirim → assign ke organizer tsb (UUID tak valid → `400`); **tidak dikirim → `organizer=null` (event platform, JANGAN default ke organizer pertama — `findDefaultOrganizer()` DIHAPUS)**; status tetap `PUBLISHED` → tampil di katalog via LEFT JOIN. `PUT /{id}`: `organizerId` non-null → re-assign, null → tanpa perubahan. ② **DDL live**: `ALTER TABLE events ALTER COLUMN organizer_id DROP NOT NULL` (verify `is_nullable=YES`, insert NULL OK) + entity `Event.organizer` → `@ManyToOne(fetch=LAZY)` `@JoinColumn(nullable=true)`. ③ **Sort default → `created_at DESC`** (`EventService.parseSort` + `HomeSearchService.parseSort`): event terbaru/Admin muncul di halaman 0 (dulu `startDate DESC` → event ber-tanggal lama tenggelam ke halaman 2); seed 7 row di-backfill `created_at='2026-01-01'` (NULLS FIRST lama dulu menduduki page 0); `date_asc/date_desc` sort startDate; ⚠️ `price_asc/price_desc` **masih bug sort startDate** (belum di-fix). ④ **EO create 500 FIX** — `OrganizerEventController`: multipart terima part `event`/`data` + fallback `@RequestParam event` JSON + **endpoint baru JSON murni** (`consumes APPLICATION_JSON`); `OrganizerEventService.parseDateTime` multi-format (ISO/T/spasi/date-only) anti `DateTimeParseException`; `GlobalExceptionHandler` + `MissingServletRequestPartException` → `400` (bukan `500`) + `ResponseStatusException` → status asli. ⑤ **EO event response + `organizer_id` & `organizerId`** (`mapEventToResponse`) untuk verifikasi frontend; list/draft sudah terisolasi `findByOrganizer_OrganizerId...` (event Admin `organizer=null` & EO lain TIDAK ikut). ⑥ **DDL live**: `ALTER TABLE refund_requests ALTER COLUMN order_id DROP NOT NULL` (payout EO = `order_id NULL`, `organizer_id` terisi; refund customer = kebalikannya) + entity `RefundRequest.order` → `nullable=true` + **migration `V4__make_refund_requests_order_id_nullable.sql`**. ⑦ Alur publish diverifikasi: EO create `DRAFT` → publish `PENDING_APPROVAL` → `PATCH /admin/events/{id}/approve` (wajib ≥1 tier) → `PUBLISHED`; katalog customer hanya `PUBLISHED`. ⑧ E2E verified live: admin create tanpa `organizerId` → `organizerId=null`, `status=PUBLISHED`, DB `organizer_id=NULL`, langsung page0 katalog. BUILD SUCCESS. ⚠️ Password admin kini `NewAdmin123` (di-reset via alur asli saat E2E; hash lama `$2a$10$F.SSEdynjEZNu0tF79vCJOdgdKw6Whz/VltqjKGsrK0h7t0LNg1ie`).
> **Handover (belum dikerjakan — untuk tim backend):** ① **Race condition kuota** — `OrderService.createOrder` potong `availableQuota` tanpa lock/`@Version` → overselling concurrent (butuh `SELECT FOR UPDATE`/optimistic lock); ② **Unique index `events.title`** belum ada (event duplikat bisa dibuat); ③ **Discriminator refund vs payout** — `refund_requests` masih shared: pemisah terbaik = `orderId IS NULL` (payout) vs NOT NULL (refund); perlu kolom `type` atau split tabel; ④ **Refresh Token flow** — saat ini access token 24h tanpa refresh (rencana dual-token: access 30m + refresh 7d + tabel `refresh_tokens`, lihat diskusi sebelumnya); ⑤ `price_asc/price_desc` sort bug; ⑥ Balance payout EO belum ada tabel tracking (`eo_payout_balances`) — Midtrans webhook belum update saldo; ⑦ Data lama ~30 event Admin masih `organizer_id` non-null (dibuat era `findDefaultOrganizer`) — belum di-null-kan.

> **Status 2026-09-24 (rev.18):** Fix alur Refund Tiket Customer→EO (audit penuh). ① **`RefundRepository` +4 query** — `findByOrganizerIdAndOrderIdNotNull` (refund EO only), `findAllPayoutsByOrderByCreatedAtDesc` + `findPayoutsWithStatus` (payout admin only, `orderId IS NULL`), `findPayoutsByOrganizerId` (payout EO only). ② **`OrganizerRefundService`** — list pakai query `orderId IS NOT NULL` (payout tak ikut); detail & update-status tolak `orderId NULL` (403 "bukan refund tiket"); **APPROVED → trigger refund payment**: order `PAID→REFUNDED` (guard: selain PAID → 400), restore `availableQuota`, semua `TicketItem` → `REFUNDED` (skip `CHECKED_IN`), semua dalam 1 `@Transactional`. ③ **`AdminPayoutService`** — `getAllPayouts` pakai query payout-only + guard `orderId!=null` → reject di `getDetail`/`updateStatus`/`getReconciliationDocument` (refund customer tak bocor ke list/detail payout). ④ **`OrganizerPayoutService`** — list pakai `findPayoutsByOrganizerId` + detail tolak `orderId!=null`. `submitRefund` sudah benar (strict `order→event→organizer`, status `PENDING`). BUILD SUCCESS; tests 20/22 (2 pre-existing).

> **Status 2026-09-24 (rev.19):** Refund fee + EO register overhaul + scan block + customer transaksi EO. ① **Biaya refund per tiket** — `app.refund.fee-per-ticket=2500` (`application.properties`, flat Rupiah **bukan %**; rentang 2000–5000, default 2500); `RefundService.submitRefund`: `amount = gross − adminFee − (fee×qty)`, floor 0; `getRefundOrderSummary` +`refundFeePerTicket` +`refundFeeTotal`; **double-refund guard** — 1 refund aktif per order (REJECTED boleh submit lagi); `findByCustomerId` filter `orderId IS NOT NULL`. ② **Scan blokir tiket refund/revoke** — `TicketService.validateAndUseTicket`: `REFUNDED` → `TIKET_SUDAH_DIREFUNDED`, `REVOKED` → `TIKET_DIREVOKE` (sebelumnya lolos `TIKET_VALID`). ③ **EO dashboard transaksi +customer** — `getRecentTransactions` +`customer_id`/`customer_name`/`customer_email` dari `order.getCustomer()` (fix: customer_id kosong `-` & nama salah isi judul event). ④ **EO Register 500/403 FIX (revisi besar)** — `OrganizerController.register`: part `data` = String→parse JSON (invalid → 400) + `@RequestParam Map<String,String> allParams` **alias** (`nama|namaEo`, `npwp|npwpNumber`, `bankName`, `account_number|bankAccountNumber`) + file 8 alias→3 slot (`cvFile|cv`, `portfolio|portfolioFile`, `aktaFile|akta|companyDeed|aktaPerusahaan`, semua `required=false`); `OrganizerService`: **uid null → `401 Daftar EO harus login terlebih dahulu`** (mock dihapus — wajib login sbg Customer dulu), `getStr()` String-safe anti ClassCastException, status **`UNVERIFIED`** (bukan PENDING — samakan dg admin filter), simpan URL dokumen ke entity (**`Organizer` +2 kolom `cv_url`/`portfolio_url`**, akta → `akta_perusahaan`); `OrganizerHelperService.saveFile` delegasi `FileStorageService.saveFile(subfolder, allowPdf)` → gagal validasi = **`400`** (bukan 500) + `Files.createDirectories`. ⑤ **SecurityConfig +3 permitAll upload** (sebelum filter `/api/organizer/**`): `POST /api/organizer/documents/upload`, `/profile/upload-portfolio`, `/profile/upload-deed` → CUSTOMER login tak kena 403. ⑥ **DB pindah** `localhost:5432` → **`192.168.28.16:5432/eventday`** (`application.properties` line 5). ⑦ Pengajuan EO verified masuk admin dashboard: `GET /api/admin/eo-applications` 200 + `PATCH /{id}/status` 200 (log live), 4 row organizers, approve → `users.role=ORGANIZER`. ⚠️ **Kode rev.18–19 BELUM di-restart** di server (manual restart oleh user).


> **Status 2026-09-14 (rev.7):**
 PR #11 (merged → HEAD `5269654`) tambah **User/Profile (customer dashboard)** — `UserController` (6 endpoint: `GET /user/profile`, `PUT /user/profile/save`, `PUT /account/change-password`, `POST /user/avatar` mock, `POST /user/logout`, `GET /transactions/history`) + `UserService` + 4 DTO baru. `AuthService.logout` kini **expose** via `POST /api/user/logout` (null-kan token + status INACTIVE + hapus cookie) — known issue #7 selesai. `EmailService` +2 metode (`sendPasswordChangedNotification`, `sendOrderConfirmationEmail`). Sisa schema-only: Organizer, Settings, Refund.

> **Status 2026-09-14 (rev.6):** PR #10 (merged → HEAD `2e1cdef`) **rampungkan Checkout** — `attendees` beneran tersimpan ke `order_attendees` (`model/Attendee` + `AttendeeRepository`, bukan stub) + tambah `POST /api/checkout/calculation` (tax 10%) + `POST /api/checkout/process` (→ `WAITING_PAYMENT`) + `GET /api/orders/status` (polling) → `CheckoutController` kini 8 endpoint. `SecurityConfig` publik untuk `/api/events/**` + `/api/terms-conditions` + `/api/privacy-policy` kini **SUDAH COMMITTED** (2 terakhir belum ada controller → 404). Sisa gap: kuota tak decrement, tiket tak diterbitkan otomatis. DB lokal `localhost:5432/eventday` (`postgres`).

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
# Koneksi ke PostgreSQL (rev.19: server tim — 192.168.28.16, dulu localhost:5432)
spring.datasource.url=jdbc:postgresql://192.168.28.16:5432/eventday
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
# Refund (rev.19): biaya proses refund per tiket, flat Rupiah (bukan persen), ditanggung customer
app.refund.fee-per-ticket=2500
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
# File Upload Configuration (rev.13, committed PR #24)
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
upload.dir=uploads
upload.logo.dir=uploads/logos
allowed.logo.types=PNG,JPG,JPEG
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
│   ├── AuthController.java           # POST /api/auth/* → ApiResponse.created(201)/ok(200)/badRequest(400), Set-Cookie access_token HttpOnly
│   ├── EventController.java          # GET /api/events|featured|{id} → ok/notFound + POST /create (MOCK, butuh login rev.14; CreateEventRequest dipakai @RequestPart admin/EO events)
│   ├── HomeSearchController.java     # MODUL 01: GET /api/home/hero-banner|event-card|locations + /api/search/results|locations|categories (publik) → ApiResponse.ok(200), null-safe []
│   ├── CheckoutController.java       # POST /api/checkout/initiate|attendees|calculation|process + GET /api/checkout/summary|orders/status|orders/{id}/total-amount|expired-time (auth) → ApiResponse.success
│   ├── PaymentController.java        # base /api/payments + /api/v1/payments: POST /charge (Bearer, Midtrans Snap REAL) + POST /midtrans-notification (PUBLIK rev.14, REAL — SHA-512, PAID→auto ticket+email, cancel/expire restore kuota)
│   ├── TicketController.java         # PR #38 rev.14: GET /my-tickets (auth-based, no /user/{email}), GET /issued-detail?ticketCode= (anti-IDOR), POST /scan (hasAnyRole ADMIN/ORGANIZER) → ApiResponse.success
│   ├── UserController.java           # PR #11 + PR #24 avatar REAL: GET /user/profile, PUT /user/profile/save, PUT /account/change-password, POST /user/avatar (saveFile)|logout, GET /transactions/history (auth)
│   ├── LegalController.java            # GET /terms-conditions|/privacy-policy + alias /api/* — FIXED rev.13 (publik, konten hardcoded kaya)
│   ├── organizer/                      # ✅ REV.14: 6 controller (30 endpoint, base /api/organizer)
│   │   ├── OrganizerController.java        # register MULTIPART rev.19 (part `data` String→JSON + `@RequestParam Map` alias nama/npwp/bank + 8 alias file→3 slot, wajib login 401, UNVERIFIED), documents/upload (permitAll rev.19), status, auth/change-password, auth/logout
│   │   ├── OrganizerProfileController.java # GET/PUT profile, avatar, upload-portfolio, upload-deed, document
│   │   ├── OrganizerDashboardController.java # GET /dashboard, /metrics, /recent-events, /recent-transactions (REAL, terisolasi per organizerId)
│   │   ├── OrganizerEventController.java  # GET/POST/PUT events (multipart REAL), publish→PENDING_APPROVAL, draft, sales-summary, /banner (BARU) — list/draft/detail terisolasi organizerId
│   │   ├── OrganizerRefundController.java # GET refunds/detail, PATCH {id}/status
│   │   └── OrganizerPayoutController.java # bank-accounts, events/{id}/payout-balance, payouts GET/POST, payouts/detail (String UUID)
│   ├── RefundController.java           # base /api (BREAKING rev.13): 7 path — submit/detail/history/order-summary REAL, banks MOCK (8 bank), download-proof partial; submit hitung amount real + organizerId selalu terisi
│   ├── admin/                                # 39 endpoint, ADMIN only (`hasRole("ADMIN")`): 20 existing + 19 Phase 1 baru
│   │   ├── AdminDashboardController.java     # GET /api/admin/dashboard/metrics|recent-events|recent-transactions (+ alias /admin/**)
│   │   ├── AdminUserController.java          # GET /api/admin/users[?role=] + /{id}, PATCH /{id}/status|/suspend (+ alias)
│   │   ├── AdminSettingsController.java      # GET /api/admin/audit-logs|/audit-logs/export|/audit-logs/export/csv + GET|PUT /api/admin/settings/general + POST /api/admin/settings/upload-logo (3 terakhir BARU) (+ alias)
│   │   ├── AdminEoController.java            # GET /api/admin/eo-applications[?status=] + /{id}, PATCH /{id}/status, GET /{id}/documents/company-deed + /download (BARU rev.14) (+ alias)
│   │   ├── AdminPayoutController.java        # GET /api/admin/payouts[?status=] + /{id}, PATCH /{id}/status, GET /{id}/documents/reconciliation (sharing refund_requests) (+ alias)
│   │   ├── AdminEventController.java         # Phase 1: /api/admin/events/* (10 rev.14: list/detail/create/update multipart + status/approve/reject/delete/sales/export)
│   │   ├── AdminTicketController.java        # Phase 1: /api/admin/tickets/* (7: list/detail/generate/revoke/checkin/inventory/export) — revoke→REVOKED, checkin→CHECKED_IN
│   │   └── AdminTransactionController.java   # Phase 1: /api/admin/transactions/* (4: list/detail/status/export) — transisi ketat
│   └── HomeController.java           # GET "/" → ApiResponse.ok("Eventday API Server is Running!","OK")
├── config/
│   ├── CorsConfig.java               # @Bean CorsFilter: allowedOriginPatterns "*", allowCredentials true, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, exposedHeaders Authorization/Content-Type, maxAge 3600 (ngrok + localhost:5173)
│   ├── ApiLoggingFilter.java         # OncePerRequestFilter format ASCII: [IN] [reqId] METHOD URI | IP | UA → [OUT] ... -> status (duration) user/auth/ip + [SLOW!] jika >1s (tanpa warna/box-drawing)
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice → ApiResponse.badRequest/unauthorized/forbidden/internalError (400/401/403/500)
│   ├── WebConfig.java                 # NEW rev.13: serve /uploads/** + /uploads/logos/** dari disk (upload.dir) — URL avatar/logo/deed langsung bisa <img>
│   └── SecurityConfig.java           # BCrypt, STATELESS, permitAll: /,/error + /api/auth/** + /api/home/** + /api/search/** + /api/events/** + 4 path legal (FIXED rev.13) + /api/payments/midtrans-notification (publik rev.13) + POST /api/organizer/register permitAll (rev.16); `/api/organizer/**` → hasAnyRole ORGANIZER/ADMIN (rev.16 explicit), `/api/admin/**` + alias `/admin/**` → `hasRole("ADMIN")` (dual alias rev.14); 401/403 via ObjectMapper
├── security/
│   ├── JwtTokenProvider.java         # @Value jwt.secret + jwt.expiration-ms, HS256 generate/validate
│   ├── JwtUtil.java                  # COMPAT — jangan hapus
│   └── JwtAuthenticationFilter.java  # OncePerRequestFilter, Bearer → validate → ROLE_*
├── dto/
│   ├── ApiResponse.java              # Wrapper {msg, status, data} @JsonInclude ALWAYS — helper created(201)/ok(200)/badRequest(400)/unauthorized(401)/forbidden(403)/notFound(404)/internalError(500)
│   ├── RegisterRequest.java          # name @NotBlank @Size100, email @Email unique, username @NotBlank @Size3-20 @Pattern ^[a-zA-Z0-9_]+$ unique, phone @Size15, password @NotBlank @Size6, nik @Pattern \d{16} unique, role String (DIABAIKAN rev.9 — selalu CUSTOMER)
│   ├── LoginRequest.java             # email, username, identifier, password + getIdentifier() (contains "@" → email else username, fallback)
│   ├── GoogleLoginRequest.java       # idToken @NotBlank
│   ├── VerifyOtpRequest.java         # email @NotBlank @Email, otpCode @NotBlank
│   ├── ResendOtpRequest.java         # email @NotBlank @Email
│   ├── ResetPasswordRequest.java     # email @NotBlank @Email, code @JsonAlias token/otp, token, newPassword @JsonAlias password + getEffectiveCode()/getEffectiveNewPassword() trim
│   ├── AuthResponse.java             # message, userId, name, email, username, role, token @JsonIgnore (hidden dari JSON, kirim via Set-Cookie access_token HttpOnly), expiresIn — dibungkus ApiResponse.data
│   ├── HeroBannerResponse.java       # MODUL 01: id, title, bannerUrl, eventDate, targetUrl (/events/{id})
│   ├── EventCardResponse.java        # MODUL 01: id, title, posterUrl, location, category, categoryLabel, startDate, dateDisplay, lowestPrice, priceDisplay
│   ├── EventCatalogResponse.java     # content (List<EventItem>), page, size, totalElements, totalPages — inner class: id, title, category, categoryLabel, date, dateDisplay, time, location, price, priceDisplay, image, status, isFeatured
│   ├── EventDetailResponse.java      # id, title, category, categoryLabel, date, dateDisplay, location, description, image, status, statusLabel, facilities STRING (bukan List<String>! raw TEXT), lineup (List<LineupItem>), tickets (List<TicketItem>)
│   ├── CheckoutSummaryResponse.java  # orderId, orderNumber, eventTitle, ticketTierName, quantity, pricePerTicket, subtotal, adminFee, discountAmount, totalAmount, expiredAt
│   ├── AttendeeRequest.java          # orderId + List<AttendeeItem(fullName,email,phoneNumber,identityNumber)> — dipakai saveAttendees (PR #10; AttendeesRequest.java lama TIDAK dipakai)
│   ├── CalculationRequest.java       # tierId, quantity, discountAmount (PR #10)
│   ├── CalculationResponse.java      # subtotal, adminFee, tax (subtotal×10%), discount, totalAmount (PR #10)
│   ├── ProcessCheckoutRequest.java   # orderId (PR #10)
│   ├── InitiateCheckoutRequest.java  # tierId, quantity (PR #9)
│   ├── CreateEventRequest.java       # NEW rev.13: title/description/category/location/venueName/eventDate/bannerUrl/facilities[]/ticketTiers[] — BELUM DIPAKAI (create pakai Map)
│   ├── PaymentChargeRequest.java     # orderId, grossAmount, customerName, customerEmail — Midtrans Snap (VERIFY: charge sekarang pakai Map langsung)
│   ├── PaymentChargeResponse.java    # snapToken, redirectUrl — Midtrans Snap response (VERIFY: lama punya virtualAccountNumber, sudah tidak akurat)
│   ├── UserProfileResponse.java      # PR #11: userId, name, email, username, phone, nik, role, avatarUrl (opsional)
│   ├── UpdateProfileRequest.java     # PR #11: name @NotBlank @Size100, phone @Size15, nik @Pattern \d{16}
│   ├── ChangePasswordRequest.java    # PR #11: oldPassword @NotBlank, newPassword @NotBlank @Size min6
│   ├── TransactionHistoryResponse.java # PR #11: orderId, orderNumber, eventTitle, ticketTierName, quantity, totalAmount, status, createdAt, expiredAt
│   ├── TicketDetailResponse.java     # PR #12: ticketId, ticketCode, orderId, eventTitle, eventDate, venueName, categoryName, attendeeName/Email/IdentityNumber, status, issuedAt — dipakai /issued-detail
│   ├── TicketScanRequest.java        # PR #12: ticketCode, eventId (Long) — BELUM DIPAKAI (scan pakai Map langsung)
│   ├── TicketScanResponse.java       # PR #12: valid, message, attendeeName, categoryName, scannedAt — BELUM DIPAKAI
│   ├── TicketSummaryResponse.java    # PR #12: orderId, ticketId, ticketCode, eventTitle, bannerUrl, eventDate, venueName, categoryName, status (ISSUED/USED/EXPIRED/REFUNDED) — BELUM DIPAKAI
│   └── TicketItemResponse.java       # ticketId, ticketCode, eventTitle, categoryName, eventDate, location, status — BELUM DIPAKAI controller manapun
│   └── admin/                              # 10 DTO admin: 6 merge 4836263 (DashboardMetrics, UserListItem, UserStatusRequest, SettingsRequest, EoApplicationResponse, EoStatusRequest) + 4 BARU (PayoutResponse, PayoutDetailResponse, UpdatePayoutStatusRequest, AuditLogExportResponse)
├── entity/                           # 12 files = 12 tabel (password_reset_tokens dihapus, digabung ke auth) — tabel ke-13 `order_attendees` ada di `model/Attendee` (PR #10)
│   ├── User.java                     # users — UUID PK, email unique, username unique 20, nik unique 16, role ENUM CUSTOMER/ORGANIZER/ADMIN
│   ├── Auth.java                     # auth — @ManyToOne User (user_id UNIQUE CASCADE), password BCrypt, authGoogle VARCHAR(20), aksesToken TEXT, expiredToken, status INACTIVE/ACTIVE, resetToken VARCHAR(255), resetExpiredAt
│   ├── Otp.java                      # otp — @ManyToOne User, otpCode VARCHAR(10), expiredAt
│   ├── Organizer.java                # organizers — @ManyToOne User, verificationStatus UNVERIFIED
│   ├── Event.java                    # events — @ManyToOne Organizer (rev.16: nullable=true untuk event Admin PUBLISHED via LEFT JOIN), status DRAFT, isFeatured BOOLEAN
│   ├── TicketTier.java               # ticket_tiers — @ManyToOne Event, price NUMERIC(12,2), totalQuota, availableQuota
│   ├── Booking.java                  # bookings — @ManyToOne User + TicketTier, status PENDING
│   ├── Order.java                    # orders — @ManyToOne Booking(UNIQUE)+Customer+Event+TicketTier
│   ├── TicketItem.java               # ticket_items — @ManyToOne Order+TicketTier, attendeeName/Email/Nik, checkInStatus UNREDEEMED, checkInAt
│   ├── RefundRequestEntity.java        # refund_requests — UUID PK AUTO; orderId/customerId/organizerId UUID biasa (bukan FK); amount/reason/bankName/bankAccountNumber/accountHolder; status PENDING; +6 kolom payout BARU (rejection_reason/admin_note/reconciliation_document_url/processed_at/organizer_id/account_holder); TIDAK ADA entity Payout terpisah
│   ├── RefundRequest.java            # entity lama @ManyToOne Customer+Order satu tabel (potensi konflik mapping — waspada)
│   ├── Settings.java                 # settings — BIGSERIAL PK, settings_key UNIQUE, settingsValue VARCHAR(255) setelah rev.11 (dulu 50)
│   └── AuditLog.java                 # audit_logs — actorId/actorName/action/detail/createdAt
├── model/
│   └── Attendee.java                 # order_attendees — PK Long IDENTITY, orderId (String, BUKAN FK/UUID), fullName, email, phoneNumber, identityNumber (PR #10)
├── repository/
│   ├── UserRepository.java           # findByEmail, findByUsername, existsByEmail/Username/Nik
│   ├── AuthRepository.java           # findByUserUserId, findByAksesToken
│   ├── OtpRepository.java            # findByUserUserIdAndOtpCode, findByUserUserId, deleteByUserUserId
│   ├── EventRepository.java          # findPublishedEvents(category,search,location,pageable), findFeaturedEvents(pageable), findPublishedEventById(id) — rev.16: LEFT JOIN e.organizer untuk admin events
│   │                               # MODUL 01: findDistinctLocations(), findDistinctCategories(), searchPublishedEvents(keyword,category,location,date,pageable) — rev.16 LEFT JOIN
│   ├── TicketTierRepository.java     # findByEvent(event)
│   ├── OrderRepository.java          # findByCustomerUserId (PR #9)
│   ├── AttendeeRepository.java       # findByOrderId (PR #10)
│   ├── TicketItemRepository.java     # findByOrderCustomerUserId/UserEmailOrderByCreatedAtDesc/OrderOrderId (PR #9)
│   ├── RefundRepository.java         # findByCustomerId/findByStatus/findAllByOrderByCreatedAtDesc + findByOrganizerId/findByOrganizerIdAndStatus (BARU payout)
│   ├── AuditLogRepository.java       # + findAllForExport() (BARU) + findAllByOrderByCreatedAtDesc(Pageable)
│   ├── OrganizerRepository.java      # findByVerificationStatus (PR #21) + findAllByOrderByCreatedAtDesc
│   └── SettingsRepository.java       # findBySettingsKey
└── service/
    ├── AuthService.java              # register + login + loginWithGoogle + verifyOtp + resendOtp + resetPassword (langsung di auth.resetToken) + logout
    ├── EventService.java             # getEvents(filter+pagination), getFeaturedEvents(3 hero), getEventDetail(UUID) + format helpers (categoryLabel, priceDisplay, dateDisplay) — facilities BELUM di-parse (return String raw)
    ├── HomeSearchService.java        # MODUL 01: getHeroBanners(max 5) + getHomeEventCards(pageable) + getLocations/getCategories(null-safe []) + searchEvents(keyword,category,location,date,pageable) + parseSort
    ├── AuditLogService.java          # log(actorId, actorName, action, detail)
    ├── EmailService.java             # sendOtpEmail + sendResetPasswordEmail + sendPasswordChangedNotification + sendOrderConfirmationEmail (Mailtrap, @Value app.mail.*, gagal → log warn tidak throw)
    ├── UserService.java              # PR #11: getProfile + updateProfile(name/phone/nik, cek NIK unik, audit UPDATE_PROFILE) + changePassword(old cocok & != new → BCrypt, audit + email notif) + getTransactionHistory (findByCustomerUserId)
    ├── OrderService.java             # createOrder: cek kuota → **DIKURANGI (rev.14)** + saveAttendees→order_attendees; calculateCheckout; processCheckout→WAITING_PAYMENT; getOrderStatus; handleExpiredOrCancelledOrder restore kuota
    ├── PaymentService.java           # getCheckoutSummary(orderId) + processPaymentCharge: DEAD CODE (tak dipanggil; PaymentController panggil MidtransService langsung)
    ├── MidtransService.java          # Midtrans Snap REAL gateway (createSnapTransaction, verifikasi signature SHA-512)
    ├── TicketService.java            # generateTicketsForOrder (REAL, idempotent — dipanggil webhook/admin/PaymentService) + getIssuedDetail + validateAndUseTicket (QR=UUID; CHECKED_IN→TIKET_SUDAH_DIPAKAI, REFUNDED→TIKET_SUDAH_DIREFUNDED, REVOKED→TIKET_DIREVOKE — rev.19)
    ├── FileStorageService.java       # ✅ REV.14 BARU: saveImage validasi PNG/JPG/JPEG/WEBP/GIF/PDF ≤5MB → uploads/<subfolder> → URL relative /uploads/... (rev.16: +PDF + validateFile allowPdf + application/pdf)
    ├── organizer/                    # ✅ REV.14 BARU (domain organizer)
    │   ├── OrganizerHelperService.java     # currentUserId, resolveOrganizer, saveFile
    │   ├── OrganizerProfileService.java    # profile/update/upload/(deed partial)
    │   ├── OrganizerDashboardService.java  # metrics/recent-events/recent-transactions REAL
    │   ├── OrganizerEventService.java      # events CRUD REAL + uploadBanner (PR #42)
    │   ├── OrganizerRefundService.java     # refunds milik EO
    │   └── OrganizerPayoutService.java     # bank-accounts/balance/payouts/create/detail (getPayoutDetailByString — fix rev.14)
    ├── RefundService.java            # submit/detail/history/order-summary REAL; banks MOCK; download-proof partial; rev.19: fee/tiket `app.refund.fee-per-ticket`, double-refund guard, history filter orderId NOT NULL
    └── admin/                        # 8 service: 5 existing (merge 4836263) + 3 Phase 1 baru — AdminDashboardService (metrics), AdminUserService (list/status/suspend+audit), AdminSettingsService (+export/exportCsv/uploadLogo), AdminEoService (pakai Organizer entity), AdminPayoutService (via RefundRepository+OrganizerRepository+audit), AdminEventService (CRUD+sales+export+Specification + approve/reject + transisi ketat rev.14), AdminTicketService (management+inventory+export, revoke→REVOKED/checkin→CHECKED_IN rev.14), AdminTransactionService (management+transisi ketat+export rev.14)
```

**DIHAPUS & JANGAN DIBUAT ULANG:** `RescheduleRequest` + `PasswordResetToken`/`password_reset_tokens` (sudah digabung ke `auth` sesuai mentor) + `EoApplication`/`Payout`/`AdminSettings`/`OrganizerEvent` entity terpisah (pakai `Organizer`/`RefundRequestEntity`/`Settings`/`Event`+`TicketTier`). PR #9 mengisi 7 placeholder PR #8; PR #10 lanjut isi `attendees` real + `calculation`/`process`/`orders/status`; PR #11 isi User/Profile + logout; PR #12 isi Ticket (issued-detail + my-tickets alias); rev.11 isi Admin Payouts (4) + Settings extension (3); **rev.14 isi Organizer DB (6 controller/service) + FileStorageService** — sisa yang belum ada (jangan buat kecuali diminta): Reschedule.

## Database Schema
Migrasi: `src/main/resources/db/migration/V1__init_schema.sql` (`uuid-ossp`, 12 tabel, FK, index, default `ADMIN_FEE=5000`, `ORDER_EXPIRY_MINUTES=15`, `BOOKING_EXPIRY_MINUTES=10`). Tabel ke-13 `order_attendees` (PR #10) dibuat Hibernate `ddl-auto=update` dari `model/Attendee` — TIDAK ada di V1.

| Table | PK | FK | Catatan |
|---|---|---|---|
| `users` | `user_id` UUID | — | `username UNIQUE`, `email UNIQUE`, `nik UNIQUE` |
| `auth` | `auth_id` UUID | `user_id` UNIQUE → users CASCADE | `password` hash, `auth_google` 20, `akses_token` TEXT, `reset_token` 255, `reset_expired_at` 15 menit |
| `otp` | `otp_id` UUID | `user_id` → users CASCADE | `otp_code` 10, `expired_at` 5 menit |
| `organizers` | `organizer_id` UUID | `user_id` UNIQUE → users | `verification_status UNVERIFIED` |
| `events` | `event_id` UUID | **`organizer_id` → organizers (rev.17: ALTER COLUMN DROP NOT NULL; entity `@ManyToOne(fetch=LAZY)` + `nullable=true`)** | `status DRAFT` |
| `ticket_tiers` | `tier_id` UUID | `event_id` → events CASCADE | `price NUMERIC(12,2)` |
| `bookings` | `booking_id` UUID | `user_id`→users, `tier_id`→tiers | `status PENDING` |
| `orders` | `order_id` UUID | `booking_id` UNIQUE, `customer_id`→users, `event_id`→events, `tier_id`→tiers | |
| `ticket_items` | `ticket_item_id` UUID | `order_id`→orders CASCADE, `tier_id`→tiers | `UNREDEEMED`, `attendee_name/nik NOT NULL` (ada di V1) |
| `refund_requests` | `refund_id` UUID | **`customer_id`→users**, **`order_id`→orders (rev.17: ALTER COLUMN DROP NOT NULL untuk support payout EO = `order_id=NULL`); payout EO = `organizer_id IS NOT NULL, customer_id IS NULL, order_id IS NULL, bank_* terisi`** | |
| `settings` | `settings_id` BIGSERIAL | — | `settings_key UNIQUE` |
| `audit_logs` | `audit_id` UUID | — | `actorId/action/detail` |

---

**Migration DB rev.17:**
- `src/main/resources/db/migration/V4__make_refund_requests_order_id_nullable.sql`: `ALTER TABLE refund_requests ALTER COLUMN order_id DROP NOT NULL;` (untuk allow payout EO tanpa `order_id`).

Semua `created_at/updated_at/create_by/updated_by` ada; `role/status` VARCHAR default. `password_reset_tokens` **dihapus** (digabung ke `auth`).

**Tabel ke-13 — `order_attendees`** (bukan di V1): PK Long IDENTITY (`model/Attendee`), kolom `order_id` String (BUKAN FK, BUKAN UUID) + `full_name/email/phone_number/identity_number`.

## API Endpoints

### ✅ AKTIF — Auth
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/auth/register` | Public | cek duplikat email/username/nik → role **paksa `CUSTOMER`** (cegah eskalasi `role=ADMIN`) → save `User` → BCrypt → save `Auth(INACTIVE)` → generate OTP 6-digit → save `otp` → `sendOtpEmail` → audit REGISTER → return tanpa token |
| POST | `/api/auth/verify-otp` | Public | find `User` by email → cek `Otp` by userId+otpCode → cek expired → set `Auth.status=ACTIVE` → delete OTP |
| POST | `/api/auth/resend-otp` | Public | find `User` → delete OTP lama → generate baru → save → kirim email |
| POST | `/api/auth/login` | Public | `getIdentifier()` (contains "@" → email else username, fallback) → find `User` → `findByUserUserId` → `matches` → cek `ACTIVE` else `Akun belum aktif!` → `generateToken(userId,email,role)` 86400000ms → update `aksesToken/expiredToken/ACTIVE` → `Set-Cookie access_token HttpOnly` + return `data` **tanpa token** (`@JsonIgnore`) + `expiresIn` |
| POST | `/api/auth/google` | Public | `GET tokeninfo?id_token=` → cek `aud==google.client-id`, `exp`, `email_verified` → find/create `User` (username auto) → find/create `Auth` dummy BCrypt `authGoogle[0:20]` → generate JWT → `Set-Cookie access_token HttpOnly` |
| POST | `/api/auth/reset-password` | Public | **Single endpoint 2 tahap (langsung di auth)**: tanpa `code`/`token` → generate 6-digit → `auth.reset_token/reset_expired_at` 15min → email. Dengan `code`/`token`+`newPassword` → `getEffectiveCode()` trim + cek `auth.resetToken==code && not expired` → update BCrypt → clear `resetToken` |

### ✅ AKTIF — Customer Event Catalog (Public — lihat catatan SecurityConfig)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/events` | Publik* | `findPublishedEvents(category,search,location,pageable)` → map ke `EventCatalogResponse` (categoryLabel, priceDisplay, dateDisplay) → pagination `{content,page,size,totalElements,totalPages}` |
| GET | `/api/events/featured` | Publik* | `findFeaturedEvents()` → max 3 event `isFeatured=true` → same shape `EventCatalogResponse` |
| GET | `/api/events/{id}` | Publik* | `findPublishedEventById(id)` → map `EventDetailResponse` + `TicketTier` list + facilities = raw String (BELUM di-parse jadi `List<String>`) → lineup placeholder |
| POST | `/api/events/create` | Publik* (⚠️) | NEW rev.13 MOCK — body diabaikan → `{eventId: random-UUID, status: DRAFT}` HTTP 201; `CreateEventRequest` DTO belum dipakai; pertimbangkan auth+EO role sebelum diisi logic |

> \*SUDAH COMMITTED di HEAD: `SecurityConfig.java:67` `/api/events/**` permitAll — akses publik tanpa Bearer, bisa dites via browser/curl. Legal juga publik 4 path (FIXED rev.13, verified live `200`).

### ✅ AKTIF — Modul 01 Home & Search (Public, tanpa JWT)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/home/hero-banner` | Public | `findFeaturedEvents(page 0 size 5)` → map `HeroBannerResponse` (targetUrl `/events/{id}`), kosong → `[]` |
| GET | `/api/home/event-card?page&size` | Public | `findPublishedEvents(null,null,null,pageable)` → map `EventCardResponse` (lowestPrice = min tier, fallback 0) → pagination `{content,page,size,totalElements,totalPages}` |
| GET | `/api/home/locations` | Public | `findDistinctLocations()` (DISTINCT `venueName` PUBLISHED), null-safe `[]` |
| GET | `/api/search/results` | Public | `searchPublishedEvents(keyword,category,location,date,pageable)` — keyword di title/description/venue, kategori dinormalisasi `Semua/ALL→null`, `date` `YYYY-MM-DD` |
| GET | `/api/search/locations` | Public | sama dengan home/locations (opsi filter search) |
| GET | `/api/search/categories` | Public | `findDistinctCategories()` (DISTINCT `category` PUBLISHED), null-safe `[]` |

`GET /` dan `GET /error` permit, `POST /api/auth/**` legacy permit. Lihat `SecurityConfig.java:55`.

### ✅ AKTIF — Checkout/Order (PR #9 + PR #10, perlu login)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/checkout/initiate` | Bearer | body `{tierId,quantity}` → `OrderService.createOrder`: cek kuota (TIDAK decrement!) → subtotal+adminFee(5000) → save `Order(PENDING)` + `expiredAt` +15min → return entity `Order` langsung |
| POST | `/api/checkout/attendees` | Bearer | (PR #10, bukan stub) body `{orderId,attendees[{fullName,email,phoneNumber,identityNumber}]}` → `OrderService.saveAttendees` simpan ke `order_attendees` → return `List<Attendee>` |
| POST | `/api/checkout/calculation` | Bearer | (PR #10) body `{tierId,quantity,discountAmount}` → `CalculationResponse`: subtotal+adminFee+tax(subtotal×10%)−discount → totalAmount (TIDAK simpan order) |
| POST | `/api/checkout/process` | Bearer | (PR #10, +guard rev.9) body `{orderId}` → tolak `PAID`/`EXPIRED`/`CANCELLED` (anti-downgrade), else status → `WAITING_PAYMENT` |
| GET | `/api/orders/status?orderId=` | Bearer | (PR #10) `OrderService.getOrderStatus` polling → `{orderId, status}` |
| GET | `/api/checkout/summary?orderId=` | Bearer | `PaymentService.getCheckoutSummary` → `CheckoutSummaryResponse` (orderNumber `ORD-XXXXXXXX`) |
| GET | `/api/orders/{orderId}/total-amount` | Bearer | `{totalAmount}` dari summary |
| GET | `/api/orders/{orderId}/expired-time` | Bearer | `{expiredAt}` dari summary |

### ✅ AKTIF — Payment — Midtrans Snap (perlu login)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/payments/charge` | Bearer | body `{orderId, grossAmount, customerName, customerEmail}` → `MidtransService.createSnapTransaction()` → return `{snapToken, redirectUrl}` (REAL Midtrans sandbox). |
| POST | `/api/payments/midtrans-notification` | **Public** (rev.13) | Webhook Midtrans (log only, belum update order status) — permitAll karena server Midtrans tak punya JWT. |

> ⚠️ `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** dari kode.

### ✅ AKTIF — Ticket (PR #9 + PR #12, perlu login, base TANPA /v1)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/tickets/user/{email}` | Bearer | `getTicketsByEmail` via `findByOrderCustomerEmailOrderByCreatedAtDesc` → `List<TicketItem>` — kosong sampai `generateTicket()` dipanggil (saat ini TIDAK DIPANGGIL siapapun!) |
| GET | `/api/tickets/my-tickets?userEmail=` | Bearer | (PR #12) alias dari `/user/{email}` — `email` path optional, fallback ke query `userEmail` |
| GET | `/api/tickets/issued-detail?ticketCode=` | Bearer | (PR #12) `TicketService.getIssuedDetail` → `TicketDetailResponse` (ticketId=UUID, ticketCode, orderId, eventTitle, eventDate, venueName, categoryName, attendeeName/Email/IdentityNumber, status, issuedAt) — 400 format tak valid / tiket tak ada |
| POST | `/api/tickets/scan` | Bearer | body `{ticketCode: "<UUID ticketItemId>"}` → `TIKET_VALID` / `TIKET_SUDAH_DIPAKAI` / **`TIKET_SUDAH_DIREFUNDED`** / **`TIKET_DIREVOKE`** (rev.19, blokir tiket REFUNDED/REVOKED) / 400 format tak valid |

### ✅ AKTIF — User/Profile & Logout (PR #11, perlu login)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/api/user/profile` | Bearer | userId dari `authentication.getName()` → `UserService.getProfile` → `UserProfileResponse{userId,name,email,username,phone,nik,role,avatarUrl}` |
| PUT | `/api/user/profile/save` | Bearer | `@Valid UpdateProfileRequest{name@NotBlank@Size100,phone@Size15,nik@Pattern\d{16}}` → update `User` (cek NIK unik) → audit UPDATE_PROFILE → return profil terbaru |
| PUT | `/api/account/change-password` | Bearer | `ChangePasswordRequest{oldPassword,newPassword}` → cek `passwordEncoder.matches(old)` + old≠new → BCrypt baru → audit + `sendPasswordChangedNotification` |
| POST | `/api/user/avatar` | Bearer | multipart `file` (image/*, ≤5MB) → simpan `uploads/avatars/` → URL langsung diserve `WebConfig` (REAL rev.13) |
| POST | `/api/user/logout` | Bearer | `AuthService.logout` (rev.9): null-kan `aksesToken/expiredToken` **saja** (status TIDAK diubah → bisa login lagi) + `Set-Cookie access_token` maxAge 0 (hapus cookie) |
| GET | `/api/transactions/history` | Bearer | `OrderRepository.findByCustomerUserId` → `List<TransactionHistoryResponse>` (orderNumber `ORD-XXXXXXXX`) |

### ✅ AKTIF — Refund Customer (perlu login, 6 path / 7 alias)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| POST | `/api/tickets/refund/request` + alias `/api/refund/submit` | Bearer | `submitRefund` REAL — **strict** `order→event→organizer` (organizer null → 400 "tidak valid", TANPA fallback customerId); `amount = gross − adminFee − (app.refund.fee-per-ticket × qty)` floor 0 (rev.19); **double-refund guard**: 1 refund aktif/order (REJECTED boleh ulang); status `PENDING` |
| GET | `/api/refund/banks` | Bearer | ⚠️ MOCK — 8 bank + logoUrl/active hardcoded |
| GET | `/api/refund/order-summary?orderId=` | Bearer | REAL: hitung dari order + **`refundFeePerTicket`/`refundFeeTotal`** (rev.19), `refundableAmount = gross − adminFee − refundFeeTotal` |
| GET | `/api/refund/refund-detail/info?refundId=` | Bearer | REAL — `getRefundDetail` via `findById` |
| GET | `/api/refund/refund-detail/download-proof?refundId=` | Bearer | REAL endpoint tapi `proofUrl` selalu `""` (entity tak punya field) |
| GET | `/api/tickets/refund/refund-history` | Bearer | REAL — `getRefundHistoryByCustomer` via `findByCustomerId` `@Query` |

### ✅ AKTIF PUBLIK — Legal (4 path / 2 route, `LegalService` hardcoded kaya)
| Method | Endpoint | Auth | Flow |
|---|---|---|---|
| GET | `/terms-conditions` + alias `/api/terms-conditions` | Public (FIXED rev.13) | `LegalService` hardcoded kaya (slug/version/sections/HTML) — verified live 200 |
| GET | `/privacy-policy` + alias `/api/privacy-policy` | Public (FIXED rev.13) | Sama — verified live 200 |

### ✅ AKTIF HYBRID — Organizer (30 endpoint, base `/api/organizer` BREAKING rev.13, Bearer)
20 lama (PR #21): `OrganizerService` 638 baris DB-backed (`findByUserUserId` → REAL, tanpa konteks → fallback `"mock":true`). 9 stub BARU rev.13 (PR #25) + rev.16 fix: `events` list/draft/detail terisolasi organizerId (`findByOrganizer_OrganizerId...`), `POST /api/organizer/register` MULTIPART `permitAll` + PDF (cvFile/portfolio/aktaFile), `dashboard/metrics|recent-events|recent-transactions` REAL terisolasi (hapus `findAll()`). Body Map `snake_case`, upload tersimpan (`uploads/`). Validasi kepemilikan 403/404 di detail/update/publish/sales. Detail lihat `API.md §17.10`.

**JWT Middleware** `JwtAuthenticationFilter.java:36`: `Authorization: Bearer <token>` **atau** `Cookie: access_token` (`resolveToken()`) → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`. `AuthResponse.token` `@JsonIgnore` — token hanya via `Set-Cookie` HttpOnly, tidak di Network → Response.

**Google Flow:** ID Token dari `https://accounts.google.com/gsi/client` (`data-client_id=google.client-id`) → `POST /google {idToken}`. Backend hanya verifikasi, tidak redirect. Frontend GIS wajib pakai **client ID yang sama persis** (`875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com`, `application.properties:20`) — `AuthService.java:362-364` tolak token bila `aud != google.client-id` (`Token Google aud tidak sesuai`). Lihat contoh snippet GIS di `API.md` §5.

### ✅ AKTIF — Admin Module (20 endpoint: 13 merge 4836263 + 7 baru — `/api/admin/**` + alias `/admin/**` `hasRole("ADMIN")` rev.14)
| Method | Endpoint | Flow |
|---|---|---|
| GET | `/admin/dashboard/metrics` | `AdminDashboardMetricsResponse` (totalPlatformRevenue/totalEvents/activeEvents/totalUsers/totalTicketsSold) |
| GET | `/admin/dashboard/recent-events` | `List<Map>` event terbaru |
| GET | `/admin/dashboard/recent-transactions` | `List<TransactionHistoryResponse>` global |
| GET | `/admin/users?role=` | `List<AdminUserListItemResponse>` (filter role opsional) |
| GET | `/admin/users/{id}` | `AdminUserListItemResponse` detail |
| PATCH | `/admin/users/{id}/status` | body `{status: ACTIVE/INACTIVE/SUSPENDED}` (`@NotBlank`) + audit |
| PATCH | `/admin/users/{id}/suspend` | no body → SUSPENDED + audit |
| GET | `/admin/settings/general` | `Map<String,String>` dari tabel `settings` |
| PUT | `/admin/settings/general` | `AdminSettingsRequest{appName,contactEmail,adminFee,orderExpiryMinutes}` → save + audit |
| GET | `/admin/audit-logs?page&size` | `Page<AuditLog>` (REGISTER/LOGIN/SUSPEND/dll) |
| GET | `/admin/audit-logs/export` | BARU → `List<AuditLogExportResponse>` (JSON) |
| GET | `/admin/audit-logs/export/csv` | BARU → string CSV (masih dibungkus `ApiResponse`, bukan file download) |
| POST | `/admin/settings/upload-logo` | BARU → multipart PNG/JPG/JPEG ≤5MB → `uploads/logos/` → `PLATFORM_LOGO` di `settings` + audit |
| GET | `/admin/eo-applications?status=` | `List<AdminEoApplicationResponse>` (filter status opsional, via `Organizer` entity) |
| GET | `/admin/eo-applications/{id}` | `AdminEoApplicationResponse` detail |
| PATCH | `/admin/eo-applications/{id}/status` | `AdminEoStatusRequest{status: VERIFIED/REJECTED, rejectionReason opsional}` + audit (VERIFIED → `users.role=ORGANIZER`) |
| GET | `/admin/eo-applications/{id}/documents/company-deed` | `{documentUrl}` link akta |
| GET | `/admin/payouts?status=` | BARU → `List<PayoutResponse>` via `refund_requests` (⚠️ tanpa discriminator — refund customer ikut) |
| GET | `/admin/payouts/{id}` | BARU → `PayoutDetailResponse` |
| PATCH | `/admin/payouts/{id}/status` | BARU → `{status, adminNote}` + `processedAt` saat APPROVED + audit |
| GET | `/admin/payouts/{id}/documents/reconciliation` | BARU → detail + `reconciliationDocumentUrl` |

> Semua endpoint admin ambil `adminId` dari `authentication.getName()`. CUSTOMER → `403`. Lihat detail + response shape di `API.md` §16.

### ⏳ SCHEMA-ONLY (jangan implement kecuali diminta)
- Organizer **29 endpoint** (`OrganizerController`, base `/api/organizer` BREAKING rev.13) — 18 hybrid + 11 mock (9 stub baru events/dashboard + logout + payouts/detail). Sisa gap: persist deed URL + persist 9 stub + logout delegasi (lihat `API.md §C`).
- Admin **Settings + audit export + upload-logo** — SUDAH ADA di main (3 merge 4836263 + 3 baru: `audit-logs/export`, `export/csv`, `settings/upload-logo`).
- Admin **EO Applications** — SUDAH ADA di main (merge 4836263). `AdminEoService` pakai `Organizer` entity (bukan `EoApplication` terpisah).
- Admin **Payouts** (4 endpoint) — SUDAH ADA (committed PR #24): `AdminPayoutController` + `AdminPayoutService` + 3 DTO, sharing tabel `refund_requests` tanpa discriminator.
- `POST /api/events/create` — ADA-sebagai-stub rev.13 (MOCK publik; REAL perlu persist + pakai `CreateEventRequest`).
- `/api/organizer/events/*` (6) + `/api/organizer/dashboard/*` (3) — ADA-sebagai-stub rev.13 (MOCK; REAL perlu persist/query DB).
- `/api/organizer/payouts/*` — aktif hybrid (riwayat + detail + create REAL bila konteks; detail-by-id praktis mock).
- `/api/organizer/profile/*` — aktif hybrid; `/organizer/auth/login` — login khusus EO BELUM ADA (pakai `/api/auth/login`).
- `/api/organizer/refunds/*` — aktif hybrid (REAL bila konteks).

## Key Business Logic (Auth + Event Catalog)
- `CorsConfig.java:14` `@Bean CorsFilter`: `allowedOriginPatterns "*"`, `allowCredentials true`, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, `allowedHeaders "*"`, `exposedHeaders Authorization/Content-Type`, `maxAge 3600` untuk ngrok + `localhost:5173` (catatan: `POST /register` 401 bukan CORS, tapi `BASE` tanpa `/api/auth`).
- Password hanya di `auth.password`, tidak di `users`. Untuk Google, dummy UUID BCrypt (kolom NOT NULL).
- Token JWT disimpan di DB `auth.aksesToken` untuk invalidasi logout — `AuthService.logout` null-kan token **saja** (rev.9: status TIDAK diubah, user bisa login lagi), expose via `POST /api/user/logout` (sekalian hapus cookie `access_token`).
- `authGoogle` `VARCHAR(20)` → `sub.substring(0,20)`.
- `EmailService.java` Mailtrap sandbox — 4 metode (OTP, reset password, password-changed, order-confirm), gagal → `log.warn` tidak throw, OTP/token tetap bisa dilihat di log.
- Reset Password **digabung ke `auth`**: `auth.reset_token` + `auth.reset_expired_at` (mentor request, tidak lagi tabel terpisah). `ResetPasswordRequest` trim code & alias `token`/`otp`.
- Register flow: `register()` generates 6-digit OTP → `otp` exp 5min → email. `verifyOtp()` → `ACTIVE`. `resendOtp()` invalidates old → new.
- Modul 01 publik: `HomeSearchController` tanpa JWT (`SecurityConfig` permitAll `/api/home/**` + `/api/search/**`). Entity pakai `venueName` (bukan `location`) + `status PUBLISHED` (bukan `ACTIVE`) — query distinct/search menyesuaikan. `size` di event-card = jumlah data/halaman pagination, bukan CSS.
- `@EnableScheduling` aktif di `EventdayApplication.java` tapi belum ada job.
- Checkout PR #9+#10 (+rev.9+**rev.14**): `initiate` return entity `Order` langsung — relasi lazy di-ignore via `@JsonIgnoreProperties` (`Order`: booking/customer/event/ticketTier; `TicketItem`: order/tier); **kuota DIKURANGI saat initiate (anti-overselling, rev.14)** + dikembalikan saat EXPIRED/CANCELLED via `handleExpiredOrCancelledOrder`; `attendees` real → `order_attendees`; `calculation` (tax 10%); `process` (→ `WAITING_PAYMENT`, tolak PAID/EXPIRED/CANCELLED); `orders/status` (polling).
- Payment: `POST /api/payments/charge` REAL Midtrans Snap via `MidtransService.createSnapTransaction()` (dipanggil langsung `PaymentController`, bukan via `PaymentService`). **Webhook `midtrans-notification` kini REAL (rev.14)** — verifikasi signature SHA-512, PAID → auto `generateTicketsForOrder` + email konfirmasi; cancel/deny/expire → CANCELLED/EXPIRED + restore kuota; idempotent. `PaymentService.processPaymentCharge()` **dead code** (mock VA lama, tak dipanggil siapapun). Status `WAITING_PAYMENT` di luar enum lama (`PENDING/PAID/EXPIRED/CANCELLED`).
- Legal rev.13 FIXED: dual alias + permitAll 4 path (verified live `200`); `LegalService` konten kaya (slug/version/sections/HTML) tapi tetap hardcoded (MOCK content).
- Refund rev.13: `submitRefund`/`getRefundDetail`/`getRefundHistoryByCustomer`/`getRefundOrderSummary` REAL (summary hitung dari order, fallback mock); `getSupportedBanks` MOCK (8 bank + logo); `download-proof` REAL tapi `proofUrl` selalu `""`; `submitRefund()` hitung amount real + `organizerId` selalu terisi (fallback = customerId).
- Admin Payout rev.13: sharing tabel `refund_requests` tanpa discriminator → `getAllPayouts` kembalikan refund customer juga. Catatan: sejak PR #24 `submitRefund` selalu isi `organizerId` (real dari event, fallback = customerId) → filter `organizerId IS NOT NULL` **tak lagi memisahkan**; perlu discriminator baru (mis. kolom `type` REFUND/PAYOUT, atau payout = baris dengan `orderId IS NULL`).
- **Ticket rev.14 (PR #38): base path `/api/tickets` (tanpa `/v1`); `GET /api/tickets/user/{email}` DIHAPUS; `GET /my-tickets` auth-based (`authentication.getName()`); `GET /issued-detail` anti-IDOR (owner/ADMIN/ORGANIZER); `POST /scan` wajib ADMIN/ORGANIZER (CUSTOMER → 403). `TicketService.generateTicketsForOrder` dipanggil otomatis di webhook/admin/PaymentService (idempotent). ⚠️ Bug potensial: `JwtAuthenticationFilter` principal = userId (UUID) → `getName()` = UUID ≠ email → query kosong.**
- Organizer rev.14 (PR #36-42): **events & dashboard REAL DB** (bukan stub) — 6 controller (`controller/organizer/`), 6 service (`service/organizer/`), `OrganizerService` ±128 baris; create/update event multipart (pakai `CreateEventRequest` @RequestPart), publish → `PENDING_APPROVAL`, `POST /api/organizer/events/banner` baru.
- Organizer rev.16: **isolasi 100%** — `OrganizerEventService` list/draft/detail/update/publish/sales-summary + `OrganizerDashboardService` metrics/recent-events/recent-transactions semua filter `organizerId` dari JWT (`resolveCurrentOrganizer/currentUserId`), `getOrganizerDashboard` hapus `findAll()` → `findByOrganizer_OrganizerId...`; validasi kepemilikan 403/404.
- Customer rev.16: `Event.organizer` `nullable=true`, `EventRepository` 4 query publik jadi `LEFT JOIN e.organizer` → event Admin (organizer NULL) muncul di `GET /api/events`; mapping `organizerName="EventDay Official"` anti-NPE; admin create event default `PUBLISHED`.
- EO Register rev.16: `SecurityConfig` `POST /api/organizer/register permitAll` + `OrganizerController` MULTIPART `@RequestPart("data")` + 4 `@RequestParam(required=false)` PDF + `OrganizerService.registerOrganizer(request, files)` → `organizer-docs/` + `FileStorageService` tambah PDF + `application/pdf`.
- Admin rev.14: events POST/PUT **multipart** (`@RequestPart("event")` + `file`); `PATCH /{id}/approve` + `PATCH /{id}/reject` event EO; `AdminTicketService` revoke → `REVOKED`, checkin → `CHECKED_IN` (bukan USED/CANCELLED); `AdminTransactionService` transisi ketat: `PENDING → {WAITING_PAYMENT,CANCELLED,EXPIRED}`, `WAITING_PAYMENT → {PAID,CANCELLED,EXPIRED}`, `PAID → REFUNDED`.
- SecurityConfig rev.14+16: hanya `GET /api/events/**` publik (POST/PUT/DELETE butuh login), `GET /uploads/**` permitAll, `POST /api/tickets/scan` → `hasAnyRole(ADMIN,ORGANIZER)`, alias `/api/v1/payments/**`; **rev.16**: `POST /api/organizer/register permitAll` sebelum `hasAnyRole ORGANIZER/ADMIN`; **rev.19**: +3 permitAll sebelum filter — `POST /api/organizer/documents/upload`, `/api/organizer/profile/upload-portfolio`, `/api/organizer/profile/upload-deed` (fix 403 upload dokumen pendaftaran EO oleh CUSTOMER).
- `FileStorageService` BARU (rev.14): validasi PNG/JPG/JPEG/WEBP/GIF ≤5MB → `uploads/<subfolder>` → URL relative `/uploads/...`; **rev.16**: tambah `PDF` + `validateFile(allowPdf)` + `application/pdf`.
- User/Profile PR #11 + PR #24: `UserController` ambil userId dari `authentication.getName()` (UUID); profil baca dari `users`; update hanya name/phone/nik (email/username/role TIDAK bisa diganti); avatar REAL (validasi image/*/5MB → `uploads/avatars/`, diserve `WebConfig`); change-password wajib `oldPassword` cocok & != `newPassword` (min 6); riwayat transaksi = semua `Order` milik user via `findByCustomerUserId`.
- Logging Spring Boot aktif `logging.level.com.example.eventday=DEBUG` → `logs/eventday.log`. Console format ASCII `%d %5p [%t] %logger : %m%n` + `ApiLoggingFilter` `[IN] [reqId] METHOD URI | IP | UA` → `[OUT] ... -> status (duration) user/auth/ip` + `[SLOW!]` jika >1s (tanpa warna/box-drawing).

## AI Agent / Realtime — TIDAK ADA di Backend (verified rev.12)

Grep seluruh `src/` untuk `WebSocket|@MessageMapping|STOMP|SSE|SseEmitter|OpenAI|ChatClient|prompt template|FunctionCall|@Tool|LangChain|MCP` → **nol hasil**. Konsekuensi:
- **Tanpa** AI agent, tool/function-call, schema payload agent, prompt template, event streaming, WebSocket/SSE, interaksi LLM di backend. File `agent.md` **tidak ada** di repo (yang ada file ini, `AGENTS.md`).
- **Interaksi frontend↔backend murni REST** (`ApiResponse {msg,status,data}`) + auth cookie `access_token`/Bearer. Tidak ada protokol khusus selain HTTP + `credentials: 'include'`.
- **"Trigger endpoint" untuk frontend** = endpoint REST biasa sesuai alur: `POST /api/auth/login` (sesi) → `GET /api/events*` (katalog) → `POST /api/checkout/*` (order) → `POST /api/payments/charge` (bayar) → `GET /api/tickets/*` (tiket) → `/admin/**` (backoffice). polling via `GET /api/orders/status`, bukan socket.
- Konteks endpoint lama tetap berlaku; yang baru: Admin Payouts (4), Settings extension (3), Organizer hybrid (19). Deprecated/🗑️: `GET /payments/methods` + `/payments/methods/virtual-account` (dihapus); stale: `PaymentChargeRequest/Response`, `TicketScanRequest/Response`, `TicketSummaryResponse`, `TicketItemResponse`, `AttendeesRequest.java` lama (tak dipakai controller); dead code: `PaymentService.processPaymentCharge()`, `TicketService.generateTicket()` (tak dipanggil).

## Known Issues / TODO
1. `jwt.secret` hardcoded — prod pindah env/Secret Manager.
2. No refresh token, hanya access 24 jam.
3. Java upgrade plan 21→25 di `.github/modernize/...` — **JANGAN JALANKAN** kecuali diminta.
4. `Settings.java` pakai `IDENTITY` BIGSERIAL bukan UUID (sesuai DDL).
5. Jangan buat ulang `reschedule_requests` & `password_reset_tokens` (sudah digabung).
6. `google.client-id` sudah **diisi** — validasi aud aktif. Frontend GIS wajib pakai client ID yang sama persis (lihat `API.md` §5 snippet).
7. `AuthService.logout(UUID)` expose via `POST /api/user/logout` — rev.9: hanya bersihkan `aksesToken/expiredToken` + hapus cookie (status TIDAK diubah; sebelumnya `INACTIVE` bikin akun tak bisa login lagi).
8. `register()` OTP di `otp` table 5 menit, reset code di `auth` 15 menit.
9. DB `192.168.28.16:5432/eventday` (`postgres`/`fikko04`, rev.19 pindah dari `localhost:5432`) — seed/tes ke DB ini. Jangan pakai kredensial shared `192.168.28.28` dari PR #8.
10. **Rev.14 selesai:** kuota **dikurangi** saat initiate + dikembalikan saat EXPIRED/CANCELLED; tiket **diterbitkan otomatis** saat PAID (webhook Midtrans/admin); avatar **REAL** (file tersimpan `uploads/avatars/`); Legal **FIXED** (public 4 path); Admin Settings/EO/Payouts **SUDAH ADA di main**; Organizer events/dashboard **REAL DB** (bukan stub); Midtrans webhook **REAL** (PAID→tiket+email, cancel/expire→restore kuota); Admin create/update event = multipart + approve/reject EO events.
11. `GET /api/events/{id}` → `facilities` masih **String** raw (`EventDetailResponse.java` tipe `String`, `EventService` set `event.getFacility()` tanpa parse) — frontend `.split(', ')` sampai diperbaiki.
12. `RefundRequest.java` (lama, `@ManyToOne`) dan `RefundRequestEntity.java` (baru, UUID biasa) map satu tabel `refund_requests` — potensi konflik mapping Hibernate, waspada saat ubah entity.
13. **BREAKING rev.13**: path lama `/organizer/*` (20) dan `/api/refund/*` + `/api/tickets/refund/*` (7) MATI → `500 No static resource`. Frontend wajib migrasi ke `/api/organizer/*` + `/api/refund/*` + `/api/tickets/refund/*` (lihat `API.md §17.12`).
14. **Bug `my-tickets`**: `JwtAuthenticationFilter` principal = userId (UUID) → `authentication.getName()` = UUID, tapi `TicketItemRepository.findByOrderCustomerEmailOrderByCreatedAtDesc` query pakai email → hasil kosong `[]`. Fix: JWT subject = email, atau query pakai `order.customer.id`.
15. ⚠️ Uncommitted (belum commit): `AdminEventController/Service`, `OrganizerEventController/Service`, `FileStorageService`, 3 file `uploads/event-banners/`.

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

curl -X POST localhost:8082/api/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456","role":"CUSTOMER"}'
curl -X POST localhost:8082/api/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com","otpCode":"123456"}'
curl -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" -d '{"username":"john123","password":"123456"}'
curl -X POST localhost:8082/api/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
curl -X POST localhost:8082/api/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'  # minta kode (simpan di auth.reset_token)
curl -X POST localhost:8082/api/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com","code":"123456","newPassword":"newPass123"}'
curl -H "Authorization: Bearer <token>" localhost:8082/any-protected   # 401 tanpa token, 403 jika endpoint schema-only
curl "localhost:8082/api/events"  # list events — PUBLIK (SecurityConfig events/** COMMITTED)
curl -H "Authorization: Bearer <token>" "localhost:8082/api/events/featured"  # hero slider
curl -H "Authorization: Bearer <token>" "localhost:8082/api/events/{id}"  # detail event
# Modul 01 publik (tanpa token):
curl "localhost:8082/api/home/hero-banner"
curl "localhost:8082/api/home/event-card?page=0&size=12"
curl "localhost:8082/api/home/locations"
curl "localhost:8082/api/search/results?keyword=neon&category=MUSIC_FESTIVAL&page=0&size=12"
curl "localhost:8082/api/search/locations"
curl "localhost:8082/api/search/categories"
# Checkout/Payment/Ticket (perlu login → cookie/Bearer):
curl -b cookies.txt -X POST localhost:8082/api/checkout/initiate -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2}'
curl -b cookies.txt -X POST localhost:8082/api/checkout/attendees -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>","attendees":[{"fullName":"Budi","email":"budi@mail.com","phoneNumber":"08123456","identityNumber":"3201234567890123"}]}'
curl -b cookies.txt -X POST localhost:8082/api/checkout/calculation -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2,"discountAmount":0}'
curl -b cookies.txt -X POST localhost:8082/api/checkout/process -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>"}'
curl -b cookies.txt "localhost:8082/api/orders/status?orderId=<order-uuid>"
curl -b cookies.txt "localhost:8082/api/checkout/summary?orderId=<order-uuid>"
curl -b cookies.txt -X POST localhost:8082/api/payments/charge -H "Content-Type: application/json" -d '{"orderId":"<order-uuid>","grossAmount":300000,"customerName":"Budi","customerEmail":"budi@mail.com"}'
curl -b cookies.txt -X POST localhost:8082/api/payments/midtrans-notification -H "Content-Type: application/json" -d '{"order_id":"...","transaction_status":"settlement","va_number":"..."}'
curl -b cookies.txt -X POST localhost:8082/api/tickets/scan -H "Content-Type: application/json" -d '{"ticketCode":"<ticketItem-uuid>"}'
curl -b cookies.txt "localhost:8082/api/tickets/user/<email>"          # list TicketItem (my tickets)
curl -b cookies.txt "localhost:8082/api/tickets/my-tickets?userEmail=<email>"  # alias PR #12
curl -b cookies.txt "localhost:8082/api/tickets/issued-detail?ticketCode=<ticketItem-uuid>"  # E-Ticket detail (PR #12)
# User/Profile & Logout (perlu login → cookie/Bearer):
curl -b cookies.txt localhost:8082/api/user/profile
curl -b cookies.txt -X PUT localhost:8082/api/user/profile/save -H "Content-Type: application/json" -d '{"name":"John Doe","phone":"08123456789","nik":"3201234567890123"}'
curl -b cookies.txt -X PUT localhost:8082/api/account/change-password -H "Content-Type: application/json" -d '{"oldPassword":"123456","newPassword":"newPass123"}'
curl -b cookies.txt -X POST localhost:8082/api/user/avatar -F "file=@avatar.jpg"
curl -b cookies.txt -X POST localhost:8082/api/user/logout
curl -b cookies.txt localhost:8082/api/transactions/history
```

## Reference Files
- `API.md` — full API documentation (frontend handoff, lengkap curl & response)
- `HELP.md` — Spring Boot help
- `.vscode/settings.json` — IDE config

---

# INSTRUKSI UNTUK AI AGENT — BUAT PDF

Anda akan membuat PDF dokumentasi lengkap berdasarkan isi file ini dan `API.md`. Ikuti aturan berikut:

## Struktur PDF yang Harus Dibuat
1. **Halaman Sampul** — Judul "Laporan Audit Status API & Gap Analysis UI/UX Eventday", tanggal audit 16 September 2026, workspace D:\eventday
2. **Ringkasan Eksekutif** — Total **98 path unik: 79 REAL** (18 hybrid organizer), **17 MOCK, 2 PARTIAL**; 0 BUG (legal fixed); 4 table-sharing risk (Payout); Admin 20 endpoint; ⚠️ BREAKING base-path refund & organizer
3. **REKAPITULASI PER MODUL** — Tabel status per modul dengan keterangan & keputusan teknis
4. **DAFTAR RINCI STATUS 86 ENDPOINT** — Setiap endpoint dengan status nyata (sumber utama: **API.md §17**)
5. **PERMINTAAN BARU TIM UI/UX** — Yang masih BELUM (REAL): persist `events/create` + 6 `organizer/events` + 3 dashboard EO (semua ADA-sebagai-stub rev.13); yang SUDAH ADA: Admin Payouts (4), Settings extension (3), EO (4), Organizer hybrid (18)
6. **Catatan Sinkronisasi Kode** — Fakta-fakta teknis

## ATURAN PENTING (JANGAN LUPA — VERIFIKASI DARI KODE AKTUAL rev.11)
- **AdminSettings & AdminEo SUDAH ADA di main** (merge commit `4836263`). Jangan bilang "belum ada" atau "branch lain"
- **Admin Payouts (4) + Settings extension (3) SUDAH ADA** (committed PR #24). Jangan bilang "belum ada"
- **`AdminEoService` pakai `Organizer` entity** (bukan `EoApplication` terpisah). `OrganizerRepository.findByVerificationStatus()` (PR #21)
- **Refund history** (`GET /tickets/refund/refund-history`) **REAL** — `refundRepository.findByCustomerId()` via `@Query`. Jangan bilang MOCK
- **Refund banks** (`GET /api/refund/banks`) **MOCK** — hardcoded. Jangan bilang REAL
- **Refund download-proof** endpoint **REAL** — tapi `proofUrl` selalu `""` karena `RefundRequestEntity` tak punya field `proofUrl` (hanya `RefundDetailResponse` punya)
- **Refund order-summary** kini **REAL** (hitung dari order; fallback mock 295rb bila order tak ada). Jangan bilang MOCK-hardcoded-290rb
- **Midtrans Snap REAL** — `POST /api/payments/charge` return `{snapToken, redirectUrl}` via `MidtransService`; `midtrans-notification` log-only; `PaymentService.processPaymentCharge()` dead code
- **LegalController FIXED (rev.13)** — dual alias + permitAll 4 path → **publik**. Jangan bilang butuh-login
- **OrganizerService HYBRID + stub** — 18 DB-backed + fallback berflag + 11 mock (9 baru + logout + payouts/detail); base `/api/organizer`. Jangan bilang SEMUA MOCK
- **rejectionReason EO opsional** — jangan bilang wajib saat REJECTED
- Payment `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** (🗑️)
- `EoApplication.java` / `Payout.java` / `AdminSettings.java` **TIDAK ADA** sebagai entity terpisah — jangan buat
- Frontend fix: `authService.js` 6× `fetch` perlu `credentials: 'include'`

## Format PDF
- Gunakan library PDF Java atau Markdown → PDF
- Sertakan tabel, status, dan catatan teknis
- Format: A4, font readable, struktur profesional
- Sertakan LEGENDA: ✅ REAL / ⚠️ MOCK / ❌ BELUM / 🗑️ HAPUS

---

# CATATAN KOREKSI PENTING
> File ini diverifikasi 15 September 2026 (HEAD `4836263`) lalu diaudit ulang 16 September 2026 (HEAD `d75de2b` + uncommitted payout/settings). Koreksi rev.11:
> 1. Refund history: MOCK → **REAL** (DB query via `findByCustomerId`)
> 2. Refund download-proof: MOCK → **REAL endpoint** (proofUrl `""` karena entity mismatch)
> 3. Refund banks: REAL → **MOCK** (hardcoded list)
> 4. Admin EO/Settings: "di branch lain" → **SUDAH ADA DI MAIN** (+ 3 extension baru)
> 5. Admin Payouts: "belum ada" → **SUDAH ADA** (sharing `refund_requests`, belum commit)
> 6. Legal: "publik permitAll" → **BUG PATH, butuh login**
> 7. Organizer: "21 endpoint" → **20 endpoint, SEMUA MOCK**
> 8. EoApplication/Payout/AdminSettings entity: **TIDAK ADA** — pakai `Organizer`/`RefundRequestEntity`/`Settings**
>
> Tambahan rev.12 (re-scan penuh, HEAD `d75de2b`):
> 9. Organizer SEMUA MOCK → **HYBRID** (19 DB-backed + fallback `mock:true`, 1 stub logout)
> 10. `rejectionReason` EO "wajib saat REJECTED" → **opsional**
> 11. Total 80 path/40 REAL → **86 path/78 REAL**; detail per endpoint di **API.md §17**
> 12. `agent.md` tidak ada; backend **tanpa AI agent/WebSocket/SSE/LLM** — murni REST
>
> Tambahan rev.13 (PR #23-25 → HEAD `6e7bcfd`, restart + verified live):
> 13. Legal BUG **FIXED** (dual alias + permitAll 4 path) — "masih bug" = build lama
> 14. **BREAKING**: Refund `/api`→`/api/v1`, Organizer `/organizer`→`/api/organizer` (lama → `500`)
> 15. Baru: `POST /api/events/create` (MOCK publik), 9 stub organizer, notif publik, avatar REAL, order-summary REAL
> 16. Total 86 → **98 path (79 REAL / 17 MOCK / 2 PARTIAL)**
>
> **Status 2026-09-16 (rev.13):** Merge PR #23 (refund), #24 (organizer-db-integration), #25 (legal) → HEAD `6e7bcfd`. Legal BUG **FIXED** (dual alias + permitAll, verified live `200` — "masih bug" = server jalan build lama; sudah restart). **BREAKING**: Refund `/api`→`/api/v1`, Organizer `/organizer`→`/api/organizer`. Baru: `POST /api/events/create` (MOCK publik), 9 stub organizer, `midtrans-notification` publik, avatar REAL + `WebConfig /uploads/**`, `order-summary` REAL, `submitRefund` selalu isi `organizerId`. Total **98 path: 79 REAL, 17 MOCK, 2 PARTIAL**.

