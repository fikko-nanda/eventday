# API.md - Eventday REST API Documentation

Base URL: `http://localhost:8081`

> **Status:** Hanya modul **Auth** yang aktif. Modul lain (Events, Orders, Payments, Tickets, Settings, Refunds, Reschedules, Audit) masih **SCHEMA ONLY** — tabel & entity sudah siap, endpoint belum di-expose. Dokumen di bawah hanya menjelaskan yang sudah bisa dipakai.

---

## 1. Auth - Register

**POST** `/api/v1/auth/register`

> Alias lama `/api/auth/register` masih di-permitAll untuk backward compat, tapi gunakan `/api/v1/auth/register`.

### Request Body
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "08123456789",
  "password": "secret123",
  "role": "CUSTOMER",
  "nik": "3201234567890123"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| name | String | Yes | Max 100 chars, @NotBlank |
| email | String | Yes | Unique, @Email |
| phone | String | No | Max 15 chars |
| password | String | Yes | Min 6 chars, di-hash BCrypt ke tabel `auth` |
| role | String | No | CUSTOMER (default), ORGANIZER, ADMIN — case-insensitive, fallback CUSTOMER |
| nik | String | No | 16 digit angka (`\\d{16}`), Unique jika diisi |

Flow (`AuthService.java:29`): validasi DTO → `existsByEmail`/`existsByNik` → `users` insert → `auth` insert (`status=INACTIVE`, `password` hash) → audit `REGISTER`.

### Response (200 OK)
```json
{
  "message": "Registrasi berhasil!",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "token": null,
  "expiresIn": null
}
```

### Response (400 Bad Request)
String biasa (bukan JSON object) dari `catch RuntimeException`:
```json
"Email sudah terdaftar!"
```
```json
"NIK sudah terdaftar!"
```
Validasi bean (`@Valid`) juga return 400 dengan detail field error (Spring default).

### Contoh cURL
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com","password":"secret123","role":"CUSTOMER"}'
```

---

## 2. Auth - Login

**POST** `/api/v1/auth/login`

Flow (`AuthService.java:74`): cari `users` by email → ambil `auth` by `user_id` → `passwordEncoder.matches` → generate JWT (`JwtUtil.java:18` claim userId/email/role, HS256, 86400000ms) → update `auth.akses_token`, `expired_token`, `status=ACTIVE` → audit `LOGIN`.

### Request Body
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| email | String | Yes | @Email |
| password | String | Yes | @NotBlank |

### Response (200 OK)
```json
{
  "message": "Login berhasil!",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBl...",
  "expiresIn": 86400
}
```
`expiresIn` dalam **detik** (86400 = 24 jam). `token` simpan di client (header `Authorization: Bearer <token>` untuk request berikutnya).

### Response (400 Bad Request)
```json
"Email atau password salah!"
```
Juga tercatat audit `LOGIN_FAILED` jika password salah.

### Contoh cURL
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}'
```

---

## 3. Middleware JWT Authentication

Semua endpoint selain `/api/v1/auth/**` (dan alias `/api/auth/**`) sekarang **butuh JWT** (`SecurityConfig.java:20` → `anyRequest().authenticated()`, `STATELESS`).

**Implementasi** (`JwtAuthenticationFilter.java:15`):
- Cek header `Authorization: Bearer <token>`
- `JwtUtil.validateToken()` — cek signature HS256 & expiry
- `JwtUtil.getUserId/email/role` — parse claims
- DB check: `authRepository.findByUserUserId(userId)` → harus `status=ACTIVE` & `akses_token == token` (mendukung invalidasi via logout)
- Set `SecurityContextHolder` dengan `ROLE_<role>`

**Cara pakai di client:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}' | jq -r .token)

# contoh request protected (akan 200 jika token valid, 403 jika tidak)
curl http://localhost:8081/api/v1/protected-example \
  -H "Authorization: Bearer $TOKEN"
```

**Error tanpa/cacat token:**
- Tanpa header → `403 Forbidden` (atau `401` tergantung Spring Security)
- Token expired / signature salah / sudah logout (`status=INACTIVE`) → filter skip auth → `403`

**Logout (internal, belum ada endpoint):** `AuthService.logout(userId)` — null-kan `akses_token/expired_token`, set `INACTIVE`. Bisa di-expose jadi `POST /api/v1/auth/logout` di tahap berikutnya.

**Konfigurasi JWT** (`application.properties`):
```properties
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000
```

---

## ⏳ Modul Lain — SCHEMA ONLY (Belum Diimplementasi)

Semua tabel & entity sudah ada (migration `V1__init_schema.sql`), tapi **service/controller/DTO/repository sudah dihapus** dan akan diaktifkan bertahap. Jangan hit endpoint di bawah — akan 403/404.

| Modul | Endpoint Rencana | Status |
|---|---|---|
| Events | `POST /api/events`, `GET /api/events`, `GET /api/events/{id}` | DB ready |
| Orders/Bookings | `POST /api/orders` | DB ready |
| Payments | `POST /api/payments/pay/{orderId}` | DB ready |
| Tickets | `POST /api/tickets/scan/{ticketItemId}` | DB ready |
| Settings | `GET/PUT /api/settings/**` | DB ready |
| Refunds | `POST/GET/PUT /api/refunds/**` | DB ready |
| Reschedules | `POST/PUT/GET /api/reschedules/**` | DB ready |
| Audit | `GET /api/audit/**` | Hanya `AuditLogService.log()` internal |

Jika butuh aktivasi modul tertentu, buat service/controller/repository + DTO sesuai entity yang sudah ada.

---

## Enum Values

### User.role (kolom `users.role` VARCHAR, normalisasi di AuthService)
| Value | Description |
|---|---|
| CUSTOMER | Customer (default) |
| ORGANIZER | Event Organizer |
| ADMIN | Administrator |

### Auth.status (kolom `auth.status`)
| Value | Description |
|---|---|
| INACTIVE | Default setelah register, atau setelah logout |
| ACTIVE | Setelah login sukses, token valid |

### Lainnya — SCHEMA ONLY (nilai sesuai migration, belum ada logic)
- `organizers.verification_status`: UNVERIFIED (default)
- `events.status`: DRAFT (default)
- `ticket_tiers` / `bookings.status`: PENDING
- `orders.status`: PENDING
- `ticket_items.check_in_status`: UNREDEEMED
- `refund_requests.status`: PENDING
- `settings_key`: ADMIN_FEE, ORDER_EXPIRY_MINUTES, BOOKING_EXPIRY_MINUTES

---

## Flow Diagram

```
        ┌─────────────────┐
        │ POST /api/v1/auth/register  (public)
        │  name,email,pass,nik,role
        └────────┬────────┘
                 │ 200 + audit REGISTER
                 ▼
        ┌─────────────────┐
        │ POST /api/v1/auth/login (public)
        │  email,pass → JWT
        └────────┬────────┘
                 │ 200 token+expiresIn
                 │ update auth.akses_token
                 ▼
        ┌─────────────────┐
        │  Client simpan token
        │  Header: Authorization: Bearer <token>
        └────────┬────────┘
                 ▼
        ┌─────────────────┐
        │  JwtAuthenticationFilter
        │  validate + DB check
        │  → SecurityContext ROLE_*
        └────────┬────────┘
                 ▼
        ┌─────────────────┐
        │  Protected API  (anyRequest.authenticated)
        │  — modul lain akan di sini —
        └─────────────────┘

  DB: users (profil) ──1:1── auth (hash+token+status)
      organizers/events/ticket_tiers/bookings/orders/ticket_items/refund_requests/settings
      → sudah ada tabel & entity, belum ada flow
```

---

## Notes for Frontend

1. **Base URL**: `http://localhost:8081` (port di `application.properties:2`)
2. **Auth dulu**: register → login → simpan `token` dari response
3. **Kirim token**: setiap request protected → `Authorization: Bearer <token>` (tanpa token = 403)
4. **Format**: `Content-Type: application/json`, `expiresIn` detik, `userId` UUID
5. **Validasi**: name/email/password required, NIK 16 digit optional, role fallback CUSTOMER
6. **Duplikat**: email/NIK unique → 400 "Email/NIK sudah terdaftar!"
7. **Modul lain**: jangan panggil dulu — akan 403/404 sampai service diaktifkan (entity & tabel sudah ready)
8. **Logout**: belum ada endpoint, tapi token bisa diinvalidasi via `auth.status=INACTIVE` (hubungi backend jika perlu)
```
