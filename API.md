# API.md - Eventday REST API Documentation

Base URL (lokal): `http://localhost:8082`
Base URL (ngrok lintas-laptop): `https://<id-baru>.ngrok-free.app` → `ngrok http 8082`

> **Status 2026-09-14 (rev.8):** PR #12 (HEAD `0405d44`) **lengkapi Ticket** — `GET /api/tickets/user/{email}` alias `GET /api/tickets/my-tickets?userEmail=` (dua-duanya list `TicketItem`), **baru** `GET /api/tickets/issued-detail?ticketCode=<UUID>` (payload E-Ticket → `TicketDetailResponse`), `POST /api/tickets/scan` (sama). Modul aktif: Auth + Event Catalog + Home & Search (publik) + Checkout (8) + Payment + Ticket (4) + User/Profile.
> **Last Updated:** 2026-09-14 — sinkron PR #12 (my-tickets alias + issued-detail), lanjutan PR #11 (User/Profile 6 endpoint + logout), DB lokal `localhost:5432/eventday`.
> **ngrok:** URL `9538-2400-...ngrok-free.app` di screenshot sudah expired. Jalankan `ngrok http 8082` di laptop backend, copy URL baru, ganti `BASE` di frontend. `localhost:8082` hanya untuk 1 laptop.
> **Response Standard:** Semua API pakai `ApiResponse.java` `{msg, status, data}` `@JsonInclude ALWAYS` — helper `created(201)/ok(200)/badRequest(400)/unauthorized(401)/forbidden(403)/notFound(404)/internalError(500)`. `SecurityConfig.java` + `GlobalExceptionHandler.java` juga pakai `ApiResponse`.

```json
// sukses auth (token via Set-Cookie HttpOnly, tidak di JSON)
{"msg":"Login berhasil!","status":200,"data":{"userId":"...","name":"...","role":"CUSTOMER","expiresIn":86400}}
// error
{"msg":"Email atau password salah!","status":400,"data":null}
// 401/403 dari Security
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
{"msg":"Forbidden: akses ditolak","status":403,"data":""}
```

---

## Daftar Endpoint Auth (Public)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 1 | POST | `/api/v1/auth/register` | Registrasi + kirim OTP | `201` sukses, `400` duplikat |
| 2 | POST | `/api/v1/auth/verify-otp` | Aktivasi akun | `200` |
| 3 | POST | `/api/v1/auth/resend-otp` | Kirim ulang OTP | `200` |
| 4 | POST | `/api/v1/auth/login` | Login email/username | `200` |
| 5 | POST | `/api/v1/auth/google` | Login Google GIS | `200` / `201` baru |
| 6 | POST | `/api/v1/auth/reset-password` | Lupa password 2 tahap | `200` |

Alias legacy `POST /api/auth/**` juga permit.

> **⚠️ Troubleshooting 401 vs CORS (kasus screenshot `/register` → 401):**
> Network `register` → `401 Unauthorized` + `Access-Control-Allow-Origin: http://localhost:5173` + `Access-Control-Allow-Credentials: true` = **CORS sudah benar** (`CorsConfig.java:14` `CorsFilter` bean + `SecurityConfig.java:38` `.cors(cors->{})`). 401 terjadi karena frontend nembak `https://xxx.ngrok-free.app/register` (tanpa prefix), sedangkan `SecurityConfig.java:55` hanya `permitAll` untuk `/api/v1/auth/**` dan `/api/auth/**`. Fix: `BASE` harus `https://xxx.ngrok-free.app/api/v1/auth` sehingga request jadi `POST /api/v1/auth/register` → `201`. Jangan pakai `BASE` tanpa suffix `/api/v1/auth`.

## Daftar Endpoint Customer (Publik — lihat catatan SecurityConfig)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 7 | GET | `/api/v1/events` | List event + filter category/search/location + pagination | `200` |
| 8 | GET | `/api/v1/events/featured` | Event unggulan untuk hero slider (max 3) | `200` |
| 9 | GET | `/api/v1/events/{id}` | Detail event + lineup + tiket | `200` / `404` |

> \*SUDAH COMMITTED di HEAD: `SecurityConfig.java:67` `/api/v1/events/**` permitAll (juga `:70` `/api/v1/terms-conditions` + `/api/v1/privacy-policy`). Akses publik tanpa Bearer — bisa dibuka via browser/curl.

## Daftar Endpoint Home & Search (Public — tanpa JWT)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 10 | GET | `/api/v1/home/hero-banner` | Banner promo/sorotan utama (max 5 featured) | `200` |
| 11 | GET | `/api/v1/home/event-card` | Daftar card event aktif + pagination `?page&size` | `200` |
| 12 | GET | `/api/v1/home/locations` | Daftar kota/lokasi unik (filter) | `200` |
| 13 | GET | `/api/v1/search/results` | Pencarian multi-filter `?keyword&category&location&date&page&size&sort` | `200` |
| 14 | GET | `/api/v1/search/locations` | Daftar lokasi unik untuk filter search | `200` |
| 15 | GET | `/api/v1/search/categories` | Daftar kategori unik untuk filter search | `200` |

> `SecurityConfig.java` `permitAll` untuk `/api/v1/home/**` + `/api/v1/search/**` — bisa dibuka via browser/curl tanpa token. Data kosong → `data:[]` / `content:[]`, bukan error.

## Daftar Endpoint Checkout/Order (perlu login — PR #9 + PR #10)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 16 | POST | `/api/v1/checkout/initiate` | Buat order `{tierId,quantity}` → `PENDING` + expired 15 mnt | `200` |
| 17 | POST | `/api/v1/checkout/attendees` | Simpan peserta ke `order_attendees` (PR #10, bukan stub lagi) | `200` |
| 18 | POST | `/api/v1/checkout/calculation` | Hitung rincian — subtotal + adminFee + tax 10% − discount (PR #10) | `200` |
| 19 | POST | `/api/v1/checkout/process` | Ubah status order → `WAITING_PAYMENT` (PR #10) | `200` |
| 20 | GET | `/api/v1/orders/status?orderId=` | Polling status order (PR #10) | `200` |
| 21 | GET | `/api/v1/checkout/summary?orderId=` | Ringkasan tagihan | `200` |
| 22 | GET | `/api/v1/orders/{orderId}/total-amount` | Total nominal | `200` |
| 23 | GET | `/api/v1/orders/{orderId}/expired-time` | Batas waktu bayar | `200` |

## Daftar Endpoint Payment (perlu login — Midtrans Snap REAL, §13)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 24 | POST | `/api/payments/charge` | `{orderId, grossAmount, customerName, customerEmail}` → Midtrans Snap → `{snapToken, redirectUrl}` | `200` |
| 25 | POST | `/api/payments/midtrans-notification` | Webhook Midtrans (log only, belum update order) | `200` |

> ⚠️ **Catatan:** `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** dari kode. Hanya ada 2 endpoint di `PaymentController`.

## Daftar Endpoint Ticket (perlu login — PR #9 + PR #12, base TANPA /v1)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 27 | GET | `/api/tickets/user/{email}` | List tiket milik user (kosong sampai tiket diterbitkan) | `200` |
| 28 | GET | `/api/tickets/my-tickets?userEmail=` | Alias #27 (PR #12) — email via query param | `200` |
| 29 | GET | `/api/tickets/issued-detail?ticketCode=` | Detail E-Ticket (PR #12) — `TicketDetailResponse` | `200`/`400` |
| 30 | POST | `/api/tickets/scan` | Scan QR `{ticketCode: "<UUID>"}` → `TIKET_VALID`/`TIKET_SUDAH_DIPAKAI` | `200`/`400` |

## Daftar Endpoint User/Profile & Logout (perlu login — PR #11)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 31 | GET | `/api/v1/user/profile` | Profil akun sendiri | `200` |
| 32 | PUT | `/api/v1/user/profile/save` | Update nama/phone/nik | `200` |
| 33 | PUT | `/api/v1/account/change-password` | Ganti password (wajib old + new min 6) | `200` |
| 34 | POST | `/api/v1/user/avatar` | Upload avatar (multipart → mock URL) | `200` |
| 35 | POST | `/api/v1/user/logout` | Logout — hapus cookie + invalidasi token DB | `200` |
| 36 | GET | `/api/v1/transactions/history` | Riwayat transaksi/order milik user | `200` |

---

## 1. Auth - Register

**POST** `/api/v1/auth/register` → `201`

### Request
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "username": "johndoe_99",
  "phone": "08123456789",
  "password": "secret123",
  "role": "CUSTOMER",
  "nik": "3201234567890123"
}
```
| Field | Required | Validasi |
|---|---|---|
| name | Yes | max 100 |
| email | Yes | `@Email`, unique |
| username | Yes | `3-20`, `^[a-zA-Z0-9_]+$`, unique |
| phone | No | max 15 |
| password | Yes | min 6, di-BCrypt ke `auth.password` |
| role | No | `CUSTOMER`/`ORGANIZER`/`ADMIN`, fallback `CUSTOMER` |
| nik | No | `^\d{16}$`, unique |

Flow `AuthService.java:61`: cek duplikat `email/username/nik` → `save User` → `save Auth(INACTIVE)` → generate OTP 6-digit `app.otp.length=6` exp `5 menit` → `save otp` → `sendOtpEmail` (Mailtrap, gagal → log warn + OTP tetap di log) → `audit REGISTER`.

### Response `201`
```json
{
  "msg":"Registrasi berhasil! OTP telah dikirim ke email Anda.",
  "status":201,
  "data":{
    "message":"Registrasi berhasil! OTP telah dikirim ke email Anda.",
    "userId":"uuid",
    "name":"John Doe",
    "email":"john@example.com",
    "username":"johndoe_99",
    "role":"CUSTOMER",
    "token":null,
    "expiresIn":null
  }
}
```
### Error `400`
```json
{"msg":"Email sudah terdaftar!","status":400,"data":null}
{"msg":"Username sudah terdaftar!","status":400,"data":null}
{"msg":"NIK sudah terdaftar!","status":400,"data":null}
{"msg":"name: Nama tidak boleh kosong, username: Username 3-20 karakter","status":400,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456"}'
# Network tab: Status 201, Response {msg, status:201, data:{...}}
```

> Setelah register, user **INACTIVE** — harus `verify-otp` dulu baru bisa login.

---

## 2. Auth - Verify OTP

**POST** `/api/v1/auth/verify-otp` → `200`

### Request
```json
{"email":"john@example.com","otpCode":"123456"}
```

### Response `200`
```json
{"msg":"OTP terverifikasi! Akun aktif, silakan login.","status":200,"data":null}
```
### Error `400`
```json
{"msg":"Kode OTP tidak valid!","status":400,"data":null}
{"msg":"Kode OTP sudah expired!","status":400,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com","otpCode":"123456"}'
```

---

## 3. Auth - Resend OTP

**POST** `/api/v1/auth/resend-otp` → `200`

### Request
```json
{"email":"john@example.com"}
```

### Response `200`
```json
{"msg":"OTP baru berhasil dikirim ke email Anda!","status":200,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/resend-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'
```

---

## 4. Auth - Login

**POST** `/api/v1/auth/login` → `200`

Support **email ATAU username ATAU identifier** + password. Backend `LoginRequest.java:6` `getIdentifier()` deteksi `contains("@")` → cari by email else username, fallback cross-check.

### Request varian
```json
{"email":"john@example.com","password":"123456"}
{"username":"johndoe_99","password":"123456"}
{"identifier":"johndoe_99","password":"123456"}
```

Flow `AuthService.java:124`: resolve identifier → `findByUserUserId` → `matches` → cek `status ACTIVE` else `Akun belum aktif!` → `jwtTokenProvider.generateToken(userId,email,role)` `86400000ms` → `update aksesToken/expiredToken/ACTIVE` → return.

### Response `200` — **Token TIDAK muncul di JSON (HttpOnly Cookie)**
```json
{
  "msg":"Login berhasil!",
  "status":200,
  "data":{
    "message":"Login berhasil!",
    "userId":"uuid",
    "name":"John Doe",
    "email":"john@example.com",
    "username":"johndoe_99",
    "role":"CUSTOMER",
    "expiresIn":86400
  }
}
```
`Set-Cookie: access_token=eyJhbG...; Path=/; Max-Age=86400; Expires=...; Secure; HttpOnly; SameSite=None; Partitioned` — token **tidak ada di `data.token` (hidden via `@JsonIgnore`)**, browser simpan otomatis. `data.expiresIn` detik (`86400` = 24 jam). Request selanjutnya kirim otomatis via `Cookie: access_token` atau manual `Authorization: Bearer <token>` (filter support keduanya `JwtAuthenticationFilter.java:32`).

> **Penting**: Kalau login dilakukan dari `localhost:5173` ke `localhost:8082`, browser kemungkinan besar **menyembunyikan/memblokir cookie** karna third‑party. Pastikan:
> 1. login ulang lewat **Incognito window** (`Ctrl+Shift+N`) — cookie pasti muncul di `Application → Cookies → localhost`.
> 2. di frontend fetch/axios pakai `credentials: 'include'` (axios: `withCredentials: true`).
> 3. setelah server restart (10:38:55), cookie lama tetap ada tapi **harus login ulang** biarkan session terbaru (old cookie sudah tidak bisa digunakan).

### Error `400`
```json
{"msg":"Email atau username harus diisi!","status":400,"data":null}
{"msg":"Email atau password salah!","status":400,"data":null}
{"msg":"Akun belum aktif! Silakan verifikasi OTP terlebih dahulu.","status":400,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}' -c cookies.txt
# token tersimpan di Set-Cookie: access_token (HttpOnly). Untuk curl manual ambil dari DB atau pakai -b cookies.txt
# curl -b cookies.txt localhost:8082/api/v1/events
```

---

## 5. Auth - Google Login

**POST** `/api/v1/auth/google` → `200` (login) / `201` (registrasi baru)

Flow `AuthService.java:190`:
1. `GET https://oauth2.googleapis.com/tokeninfo?id_token=<idToken>` (RestTemplate)
2. cek `aud==google.client-id` (`875040780549-...apps.googleusercontent.com`), cek `exp`, `email_verified==true`
3. `findByEmail` → jika null buat `User(name,email,username auto dari email, role=CUSTOMER)`
4. `findByUserUserId` → jika null buat `Auth(password dummy BCrypt UUID, authGoogle=sub[0:20])` else update `authGoogle`
5. `generateToken()` → `aksesToken/expiredToken/ACTIVE` → `audit REGISTER_GOOGLE/LOGIN_GOOGLE`

### Request
```json
{"idToken":"eyJhbGciOiJSUzI1NiIs...Google ID Token..."}
```

### Response `200` / `201` — **Token via HttpOnly Cookie**
```json
{"msg":"Login via Google berhasil!","status":200,"data":{"message":"Login via Google berhasil!","userId":"...","name":"...","email":"...","username":"...","role":"CUSTOMER","expiresIn":86400}}
{"msg":"Registrasi via Google berhasil!","status":201,"data":{...}}
```
`Set-Cookie: access_token=...; HttpOnly` — `data.token` **tidak ada di Network → Response** (hidden).
Error `400` `{"msg":"Token Google tidak valid: ...","status":400,"data":null}`

```bash
curl -X POST localhost:8082/api/v1/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
```

**Frontend GIS:** `https://accounts.google.com/gsi/client` + `data-client_id=google.client-id` → `res.credential` → `POST /google` → `Set-Cookie access_token`.

---

## 6. Auth - Reset Password (Lupa Password)

**POST** `/api/v1/auth/reset-password` — **single endpoint 2 tahap**.

*   **Tahap 1** `code` kosong → minta kode ke email
*   **Tahap 2** `code` terisi → verifikasi & ganti password

Backend `ResetPasswordRequest.java:8` support alias: `code` alias `token`/`otp`, `newPassword` alias `password`/`new_password`. `AuthService.java:302` pakai `getEffectiveCode()` trim.

### Tahap 1 - Minta Kode
```json
{"email":"john@example.com"}
```
Response `200`:
```json
{"msg":"Kode reset password berhasil dikirim ke email Anda!","status":200,"data":null}
```
Generate 6-digit `app.reset-password.code-length=6`, simpan `auth.reset_token` + `auth.reset_expired_at` exp `15 menit` (digabung ke `auth` sesuai mentor), kirim via `EmailService`.

```bash
curl -X POST localhost:8082/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'
```

### Tahap 2 - Reset Password
```json
{"email":"john@example.com","code":"123456","newPassword":"newPassword123"}
```
Juga valid: `{"email":"...","token":"123456","password":"..."}`
Response `200`:
```json
{"msg":"Password berhasil direset! Silakan login dengan password baru.","status":200,"data":null}
```
Error `400`:
```json
{"msg":"Password baru minimal 6 karakter","status":400,"data":null}
{"msg":"Kode reset password tidak valid/expired","status":400,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com","code":"123456","newPassword":"newPass123"}'
```

---

## 7. Middleware JWT & Protected Routes

Publik (tanpa JWT): `/api/v1/auth/**`, `/api/v1/home/**`, `/api/v1/search/**`, `/api/v1/events/**`, `/api/v1/terms-conditions`, `/api/v1/privacy-policy`, `/`, `/error` — **semua SUDAH di-commit** di `SecurityConfig.java:55-73`. Sisanya (Checkout, Payment, Ticket, dst) butuh JWT. `SecurityConfig.java:40` `STATELESS`.

`JwtAuthenticationFilter.java:36`: `Authorization: Bearer <token>` **atau** `Cookie: access_token` (`resolveToken()`) → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`.

`SecurityConfig.java` kini return standard `msg/status/data` untuk `401/403`:

```json
// tanpa token
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
// token salah/expired/inactive
{"msg":"Forbidden: akses ditolak","status":403,"data":""}
```

```bash
curl -c cookies.txt -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -b cookies.txt localhost:8082/api/v1/events  # 200 jika ada data PUBLISHED
curl localhost:8082/api/v1/events  # -> 200 juga (PUBLIK — SecurityConfig events/** COMMITTED)
curl localhost:8082/ # -> 200 {msg, status:200, data:"OK"}
curl localhost:8082/api/v1/checkout/summary?orderId=xxx  # -> 401 {msg, status:401} (perlu login)
# Alternatif manual: curl -H "Authorization: Bearer <token-dari-DB>" localhost:8082/api/v1/events
```

Config `application.properties`:
```properties
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000 # 24 jam -> data.expiresIn 86400
google.client-id=875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com
```

---

## 8. Customer - Events Catalog (Dashboard)

### GET `/api/v1/events` — List event untuk `CustomerDashboard.jsx`

**Auth:** **publik** — `SecurityConfig.java:67` sudah di-commit (`/api/v1/events/**` permitAll), boleh akses tanpa token (browser/curl). Role `CUSTOMER` / `ORGANIZER` / `ADMIN` tetap boleh akses via Bearer/Cookie.

**Query Params (semua opsional):**

| Param | Tipe | Deskripsi | Default | Contoh |
|---|---|---|---|---|
| `category` | string | Filter kategori. `Semua` = tanpa param | all | `?category=MUSIC%20FESTIVAL` |
| `search` | string | Pencarian judul / venue | — | `?search=Neon` |
| `location` | string | Filter lokasi/venue | — | `?location=Jakarta` |
| `page` | int | Pagination 0-based | `0` | `?page=0&size=12` |
| `size` | int | Page size | `12` | |
| `sort` | string | `latest` (default), `price_asc`, `price_desc`, `date_asc` | `latest` | `?sort=latest` |

**Response `200`:**
```json
{
  "msg": "Berhasil mengambil daftar event",
  "status": 200,
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "Neon Nights 2024",
        "category": "MUSIC_FESTIVAL",
        "categoryLabel": "Musik",
        "date": "2026-12-15T19:00:00",
        "dateDisplay": "15 Dec 2026",
        "time": "19:00",
        "location": "Stadion Utama GBK",
        "price": 200000,
        "priceDisplay": "Rp 200.000",
        "image": "https://images.unsplash.com/photo-...",
        "status": "AVAILABLE",
        "isFeatured": true
      }
    ],
    "page": 0,
    "size": 12,
    "totalElements": 42,
    "totalPages": 4
  }
}
```

> **Notes:**
> - `price` = harga terendah dari semua tier event.
> - `status` `PUBLISHED` ditampilkan sebagai `AVAILABLE`.
> - `category` di DB pakai underscore (`MUSIC_FESTIVAL`), API mengembalikan label display (`Musik`).
> - Pagination wajib — frontend render `empty-events` bila `content.length === 0`.

**Alternatif Hero:** `GET /api/v1/events/featured` → khusus 3 event hero, response shape sama.

```bash
# pakai cookie (recommended, token HttpOnly):
curl -b cookies.txt "localhost:8082/api/v1/events?category=MUSIC%20FESTIVAL&search=Neon&page=0&size=12"
curl -b cookies.txt "localhost:8082/api/v1/events/featured"
# atau pakai Bearer jika token diambil manual dari DB/auth:
curl -H "Authorization: Bearer $TOKEN" "localhost:8082/api/v1/events?category=MUSIC%20FESTIVAL&search=Neon&page=0&size=12"
```

**Error:**
```json
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
```

---

## 9. Customer - Event Detail

### GET `/api/v1/events/{id}` — Detail `DetailEventCustomer.jsx`

**Auth:** **publik** — sudah di-commit di `SecurityConfig.java:67` (`/api/v1/events/**` permitAll), bisa diakses tanpa token.

**Path:** `id` UUID event.

**Response `200`:**
```json
{
  "msg": "Berhasil mengambil detail event",
  "status": 200,
  "data": {
    "id": "uuid",
    "title": "Neon Nights 2024",
    "category": "MUSIC_FESTIVAL",
    "categoryLabel": "Musik",
    "date": "2026-12-15T19:00:00",
    "dateDisplay": "15 Desember 2026",
    "location": "Stadion Utama GBK",
    "description": "Festival musik elektronik terbesar...",
    "image": "https://images.unsplash.com/photo-...",
    "status": "PUBLISHED",
    "statusLabel": "Tersedia",
    "facilities": "Parkir Luas, Food Court, Musholla, Toilet Bersih, Wifi Gratis",  // ⚠️ STRING (belum di-parse jadi array!)
    "lineup": [
      {"name": "Bintang Tamu", "image": ""},
      {"name": "Bintang Tamu", "image": ""},
      {"name": "Bintang Tamu", "image": ""}
    ],
    "tickets": [
      {
        "id": "uuid-tier",
        "name": "Early Bird",
        "label": "Early Bird",
        "price": 200000,
        "priceDisplay": "Rp 200.000",
        "quota": 100,
        "remaining": 42,
        "saleStart": "2026-10-01T00:00:00",
        "saleEnd": null
      },
      {
        "id": "uuid-tier",
        "name": "Regular",
        "label": "Regular",
        "price": 300000,
        "priceDisplay": "Rp 300.000",
        "quota": 200,
        "remaining": 150
      }
    ]
  }
}
```

**Mapping ke frontend:**
- `event.tickets[0]` → Early Bird, `event.tickets[1]` → Regular
- `event.lineup` → daftar bintang tamu
- `event.facilities` → **STRING** (raw kolom `facility` TEXT, delimiter koma, contoh `"Parkir Luas, Food Court"`). ⚠️ **BUKAN array** — frontend harus `.split(', ')` dulu. Gap: backend belum parse → `List<String>` (lihat Known Issue di AGENTS.md).
- `quantity` state lokal frontend, saat `Beli Tiket` → `navigate(/checkout/:id)` bawa `quantity` + `selectedTicketId`

**Error `404`:**
```json
{"msg":"Event tidak ditemukan","status":404,"data":null}
```

```bash
curl -b cookies.txt localhost:8082/api/v1/events/{uuid}
# atau: curl -H "Authorization: Bearer $TOKEN" localhost:8082/api/v1/events/{uuid}
```

---

## 10. Home & Search — Hero, Event Card, Locations

### GET `/api/v1/home/hero-banner` — Public, tanpa token
Ambil max 5 event `isFeatured=true` + `status=PUBLISHED` (`HomeSearchService.getHeroBanners()`).
```json
{"msg":"Berhasil mengambil hero banner","status":200,"data":[{"id":"uuid","title":"Neon Nights","bannerUrl":"https://...","eventDate":"2026-12-15T19:00:00","targetUrl":"/events/uuid"}]}
```
DB kosong → `{"msg":"...","status":200,"data":[]}`.

### GET `/api/v1/home/event-card?page=0&size=12` — Public
Card event aktif paginated. `size` = **jumlah data per halaman** (bukan ukuran CSS).
```json
{"msg":"Berhasil mengambil event card","status":200,"data":{"content":[{"id":"uuid","title":"Neon Nights","posterUrl":"https://...","location":"GBK","category":"MUSIC_FESTIVAL","categoryLabel":"Musik","startDate":"2026-12-15T19:00:00","dateDisplay":"15 Dec 2026","lowestPrice":200000,"priceDisplay":"Rp 200.000"}],"page":0,"size":12,"totalElements":42,"totalPages":4}}
```
`lowestPrice` = harga min dari `ticket_tiers`, fallback `0` jika belum ada tier.

### GET `/api/v1/home/locations` — Public
```json
{"msg":"Berhasil mengambil daftar lokasi","status":200,"data":["Jakarta","Bandung"]}
```
Query: `SELECT DISTINCT e.venueName ... WHERE status='PUBLISHED'` (`EventRepository.findDistinctLocations()`). Kosong → `[]`.

```bash
curl "localhost:8082/api/v1/home/hero-banner"
curl "localhost:8082/api/v1/home/event-card?page=0&size=12"
curl "localhost:8082/api/v1/home/locations"
# atau buka langsung di browser (GET publik, tanpa token)
```

---

## 11. Search — Results, Locations, Categories

### GET `/api/v1/search/results` — Public
| Param | Deskripsi | Contoh |
|---|---|---|
| `keyword` | cari di title/description/venue (case-insensitive) | `?keyword=neon` |
| `category` | `MUSIC_FESTIVAL`/`CONFERENCE`/`EXHIBITION`/`CULINARY`, `Semua`/`ALL` = tanpa filter | `?category=MUSIC_FESTIVAL` |
| `location` | partial match venue | `?location=Jakarta` |
| `date` | format `YYYY-MM-DD`, cocok `CAST(startDate AS date)` | `?date=2026-12-15` |
| `page`/`size`/`sort` | pagination, `sort=latest` (default), `date_asc`, `price_asc/desc` | `?page=0&size=12&sort=latest` |

Response sama shape dengan event-card (`{content,page,size,totalElements,totalPages}`).

### GET `/api/v1/search/locations` — Public → `[string]` lokasi unik
### GET `/api/v1/search/categories` — Public → `[string]` kategori unik (`SELECT DISTINCT e.category ... PUBLISHED`)

```bash
curl "localhost:8082/api/v1/search/results?keyword=neon&category=MUSIC_FESTIVAL&location=Jakarta&page=0&size=12&sort=latest"
curl "localhost:8082/api/v1/search/results?date=2026-12-15"
curl "localhost:8082/api/v1/search/locations"
curl "localhost:8082/api/v1/search/categories"
```

---

## 12. Checkout & Order (perlu login)

### POST `/api/v1/checkout/initiate` — body `{tierId, quantity}`
`OrderService.createOrder`: cek `availableQuota` (tidak dikurangi!) → `subtotal = price×qty`, `total = subtotal + adminFee(5000)` → save `Order(PENDING, expiredAt +15mnt)`. Return entity `Order` langsung.
```bash
curl -b cookies.txt -X POST localhost:8082/api/v1/checkout/initiate -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2}'
```

### POST `/api/v1/checkout/attendees` — simpan peserta (PR #10, bukan stub lagi)
Body:
```json
{"orderId":"<order-uuid>","attendees":[{"fullName":"Budi","email":"budi@mail.com","phoneNumber":"08123456","identityNumber":"3201234567890123"}]}
```
Simpan ke tabel `order_attendees` (`model/Attendee`, PK Long IDENTITY) — `identityNumber` = NIK/No. KTP, `orderId` plain String (bukan FK/UUID). Respons `data` = `[{id, orderId, fullName, email, phoneNumber, identityNumber}]`.

### POST `/api/v1/checkout/calculation` — hitung rincian (PR #10, TIDAK menyimpan order)
Body `{tierId, quantity, discountAmount?}` → `data`:
```json
{"msg":"Kalkulasi checkout berhasil","status":200,"data":{"subtotal":400000,"adminFee":5000,"tax":40000,"discount":0,"totalAmount":445000}}
```
`tax` = `subtotal × 10%`, `totalAmount = subtotal + adminFee + tax − discount`.

### POST `/api/v1/checkout/process` — kunci order (PR #10)
Body `{orderId}` → status order jadi `WAITING_PAYMENT` (TIDAK cek `PENDING` dulu — beda dengan `/payments/charge` yang wajib PENDING). Return entity `Order`.

### GET `/api/v1/orders/status?orderId=<uuid>` — polling (PR #10)
`data` = `{"orderId":"<uuid>","status":"WAITING_PAYMENT"}`.

### GET `/api/v1/checkout/summary?orderId=<uuid>` → `CheckoutSummaryResponse`
`{orderId, orderNumber("ORD-XXXXXXXX"), eventTitle, ticketTierName, quantity, pricePerTicket, subtotal, adminFee, discountAmount(0), totalAmount, expiredAt}`.
### GET `/api/v1/orders/{id}/total-amount` → `{totalAmount}` · GET `/api/v1/orders/{id}/expired-time` → `{expiredAt}`

---

## 13. Payment — Midtrans Snap (perlu login, REAL gateway sandbox)

```bash
# Midtrans Snap — return snapToken:
curl -b cookies.txt -X POST localhost:8082/api/payments/charge \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<order-uuid>","grossAmount":300000,"customerName":"Budi","customerEmail":"budi@mail.com"}'
# → {"status":200,"data":{"snapToken":"Mid-trans...","redirectUrl":"https://app.sandbox.midtrans.com/snap/v2/vtweb/..."}}

# Webhook dari Midtrans (otomatis, bukan manual):
curl -X POST localhost:8082/api/payments/midtrans-notification \
  -H "Content-Type: application/json" \
  -d '{"order_id":"...","transaction_status":"settlement","va_number":"..."}'
```

- `charge` body: `{orderId, grossAmount, customerName, customerEmail}` → return `{snapToken, redirectUrl}`
- Config (`application.properties:63-66`): `midtrans.server-key=SB-Mid-server-...`, `midtrans.snap-url=https://app.sandbox.midtrans.com/snap/v1/transactions`.
- Frontend redirect ke `redirectUrl` untuk bayar, lalu Midtrans webhook `/api/payments/midtrans-notification` (log only, belum update order status).
- ⚠️ `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** dari kode.

---

## 14. Ticket — My Tickets, E-Ticket Detail & Scan (perlu login, base `/api/tickets` TANPA `/v1`)

### GET `/api/tickets/user/{email}` dan alias `GET /api/tickets/my-tickets?userEmail=<email>` (PR #12)
Kedua endpoint mengembalikan `List<TicketItem>` (entity) milik user via `findByOrderCustomerEmailOrderByCreatedAtDesc`. **Kosong `[]` sampai `generateTicket()` dipanggil** (saat ini tak ada alur yang memanggil!).

### GET `/api/tickets/issued-detail?ticketCode=<ticketItem-uuid>` — E-Ticket Detail (PR #12)
`ticketCode` = **UUID `ticketItemId`**. Respons `data` = `TicketDetailResponse`:
```json
{"msg":"Detail E-Ticket berhasil dimuat","status":200,"data":{"ticketId":"<uuid>","ticketCode":"<uuid>","orderId":"<uuid>","eventTitle":"Neon Nights Concert","eventDate":"2026-12-15T19:00:00","venueName":"Stadion Utama GBK","categoryName":"Early Bird","attendeeName":"Budi","attendeeEmail":"budi@mail.com","attendeeIdentityNumber":"3201234567890123","status":"UNREDEEMED","issuedAt":"2026-09-14T10:00:00"}}
```
Error `400`: `{"msg":"Format kode tiket tidak valid!","status":400,"data":null}` / `{"msg":"Tiket tidak ditemukan!","status":400,"data":null}`.

### POST `/api/tickets/scan`
Body `{"ticketCode":"<ticketItem-uuid>"}` → `TIKET_VALID` (set `CHECKED_IN`+`checkInAt`) / `TIKET_SUDAH_DIPAKAI` / 400 format tak valid. Respons:
```json
{"msg":"Proses scan selesai","status":200,"data":{"status":"TIKET_VALID","message":"Proses scan selesai"}}
```

```bash
curl -b cookies.txt localhost:8082/api/tickets/user/john@mail.com                        # [] sampai generateTicket() dipanggil
curl -b cookies.txt "localhost:8082/api/tickets/my-tickets?userEmail=john@mail.com"      # alias PR #12
curl -b cookies.txt "localhost:8082/api/tickets/issued-detail?ticketCode=<ticketItem-uuid>"  # E-Ticket detail (PR #12)
curl -b cookies.txt -X POST localhost:8082/api/tickets/scan -H "Content-Type: application/json" -d '{"ticketCode":"<ticketItem-uuid>"}'
# → TIKET_VALID / TIKET_SUDAH_DIPAKAI / 400 format tak valid
```

---

## 15. User — Profile, Ganti Password, Logout, Riwayat (PR #11, perlu login)

Semua endpoint ambil userId dari `authentication.getName()` (`UserController.getAuthenticatedUserId`). Email/username/role **tidak bisa diganti** — hanya name/phone/nik.

### GET `/api/v1/user/profile`
```json
{"msg":"Berhasil mengambil data profil","status":200,"data":{"userId":"uuid","name":"John Doe","email":"john@mail.com","username":"john123","phone":"08123456789","nik":"3201234567890123","role":"CUSTOMER","avatarUrl":null}}
```

### PUT `/api/v1/user/profile/save`
Body `{"name":"John Doe","phone":"08123456789","nik":"3201234567890123"}` — `name` @NotBlank @Size100, `phone` @Size15, `nik` `^\d{16}$` unik.
```json
{"msg":"Profil berhasil diperbarui","status":200,"data":{...UserProfileResponse terbaru...}}
```
Error `400` NIK duplikat: `{"msg":"NIK sudah digunakan akun lain!","status":400,"data":null}`.

### PUT `/api/v1/account/change-password`
Body `{"oldPassword":"123456","newPassword":"newPass123"}` — `newPassword` min 6, tidak boleh sama dengan `oldPassword`. Verifikasi `matches(old)` → simpan BCrypt baru → audit `CHANGE_PASSWORD` + email notifikasi `sendPasswordChangedNotification`.
```json
{"msg":"Password berhasil diubah","status":200,"data":null}
```
Error `400`: `{"msg":"Password lama salah!","status":400,"data":null}` / `{"msg":"Password baru tidak boleh sama dengan password lama!","status":400,"data":null}`.

### POST `/api/v1/user/avatar` (multipart)
`-F "file=@avatar.jpg"` → return mock URL (file belum disimpan):
```json
{"msg":"Avatar berhasil diperbarui","status":200,"data":"/uploads/avatars/<uuid>_avatar.jpg"}
```

### POST `/api/v1/user/logout`
Panggil `AuthService.logout` (null-kan `aksesToken/expiredToken` + status `INACTIVE`) + `Set-Cookie access_token` maxAge 0 (hapus cookie).
```json
{"msg":"Logout berhasil","status":200,"data":null}
```
Setelah logout, token lama tidak valid (`403`).

### GET `/api/v1/transactions/history`
`OrderRepository.findByCustomerUserId` → `data[]`:
```json
{"msg":"Berhasil mengambil riwayat transaksi","status":200,"data":[{"orderId":"uuid","orderNumber":"ORD-XXXXXXXX","eventTitle":"Neon Nights 2024","ticketTierName":"Early Bird","quantity":2,"totalAmount":405000,"status":"PENDING","createdAt":"2026-09-14T10:00:00","expiredAt":"2026-09-14T10:15:00"}]}
```

```bash
curl -b cookies.txt localhost:8082/api/v1/user/profile
curl -b cookies.txt -X PUT localhost:8082/api/v1/user/profile/save -H "Content-Type: application/json" -d '{"name":"John Doe","phone":"08123456789","nik":"3201234567890123"}'
curl -b cookies.txt -X PUT localhost:8082/api/v1/account/change-password -H "Content-Type: application/json" -d '{"oldPassword":"123456","newPassword":"newPass123"}'
curl -b cookies.txt -X POST localhost:8082/api/v1/user/avatar -F "file=@avatar.jpg"
curl -b cookies.txt -X POST localhost:8082/api/v1/user/logout
curl -b cookies.txt localhost:8082/api/v1/transactions/history
```

---

## 17. Legal — Syarat & Privasi (Public, `LegalController`)

| Method | Endpoint | Deskripsi | HTTP |
|---|---|---|---|
| GET | `/terms-conditions` | Syarat & ketentuan (statik) | `200` |
| GET | `/privacy-policy` | Kebijakan privasi (statik) | `200` |

> **Catatan:** Controller ini mapping di root (`/terms-conditions`), BUKAN `/api/v1/terms-conditions`. `SecurityConfig` permit `/api/v1/terms-conditions` + `/api/v1/privacy-policy` — **mismatch path**! Saat ini `/api/v1/terms-conditions` → 404 (permit tapi controller tidak map sana), `/terms-conditions` → 200.

```json
{"msg":"Terms and conditions retrieved successfully","status":200,"data":{"title":"Syarat dan Ketentuan EventDay","content":"...","updated_at":"2026-09-14"}}
```

---

## 18. Organizer / EO Module (perlu login — `OrganizerController`, `/organizer/**`)

> **Status: MOCK/STUB** — semua return data statis `Map`, **belum persisten ke DB**. Gunakan untuk integrasi UI saja; logika bisnis belum final.

| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/organizer/register` | Daftar EO → `{organizer_name, verification_status: "PENDING"}` |
| POST | `/organizer/documents/upload?type=KTP` | Upload dokumen (multipart) → `{document_type, file_name, file_size}` |
| GET | `/organizer/status` | Status verifikasi EO (mock `PENDING`) |
| GET | `/organizer/dashboard` | Metrics dashboard EO (mock: `total_revenue, active_events, tickets_sold`) |
| GET | `/organizer/profile` | Profil EO (mock statis) |
| PUT | `/organizer/profile` | Update profil (echo payload) |
| POST | `/organizer/profile/avatar` | Upload avatar (mock URL) |
| POST | `/organizer/profile/upload-portfolio` | Upload portofolio (mock URL) |
| POST | `/organizer/profile/upload-deed` | Upload akta (mock URL) |
| GET | `/organizer/profile/document` | Daftar dokumen legalitas (mock) |
| POST | `/organizer/auth/change-password` | Ganti password EO (no-op) |
| POST | `/organizer/auth/logout` | Logout EO (no-op) |
| GET | `/organizer/refunds` | List refund request ke EO (mock) |
| GET | `/organizer/refunds/detail?id=` | Detail refund (mock) |
| PATCH | `/organizer/refunds/{id}/status` | Update status refund (echo) |
| GET | `/organizer/bank-accounts` | Rekening EO (mock) |
| GET | `/organizer/events/{id}/payout-balance` | Saldo payout event (mock, `eventId` Long) |
| GET | `/organizer/payouts` | Riwayat payout (mock) |
| POST | `/organizer/payouts` | Ajukan payout (mock) |
| GET | `/organizer/payouts/detail?id=` | Detail payout (mock, `id` Long) |

---

## 19. Refund Customer (perlu login — `RefundController`, `/api/**`)

| Method | Endpoint | Deskripsi |
|---|---|---|
| POST | `/api/tickets/refund/request` (alias `/api/refund/submit`) | Ajukan refund — simpan ke `refund_requests` (REAL), body `RefundRequest{orderId, reason, bankCode, accountNumber, accountHolderName}` |
| GET | `/api/refund/banks` | Bank pendukung BCA/MANDIRI/BNI/BRI → `BankResponse{bankCode, bankName}` |
| GET | `/api/refund/order-summary?orderId=<uuid>` | Ringkasan refund (mock: `ticketQuantity, grossAmount, adminFee, refundableAmount`) |
| GET | `/api/refund/refund-detail/info?refundId=<uuid>` | Detail refund dari DB (REAL) → `RefundDetailResponse` |
| GET | `/api/refund/refund-detail/download-proof?refundId=<uuid>` | URL bukti transfer → `{proofUrl}` |
| GET | `/api/tickets/refund/refund-history?email=<email>` | Riwayat refund (mock 1 item) |

`submitRefund` ambil `customerId` dari JWT (`SecurityContextHolder`), `refundAmount` di-hardcode `290000.00` (belum dihitung dari order).

---

## 📋 Status Semua Modul (ringkas)

| Modul | Endpoint | Status |
|---|---|---|
| Auth | `/api/v1/auth/*` | ✅ AKTIF |
| Events + Home + Search | `/api/v1/events/*`, `/home/*`, `/search/*` | ✅ AKTIF (publik) |
| Checkout/Order | `/api/v1/checkout/*`, `/orders/*` | ✅ AKTIF |
| Payment | `/api/v1/payments/*` | ✅ AKTIF — **Midtrans Snap** (§13, bukan mock VA lagi) |
| Ticket | `/api/tickets/*` | ✅ AKTIF |
| User/Profile | `/user/*`, `/account/*`, `/transactions/*` | ✅ AKTIF |
| Admin | `/admin/*` | ✅ AKTIF (§16, ADMIN only; settings+eo ada di branch lain) |
| Legal | `/terms-conditions`, `/privacy-policy` | ⚠️ AKTIF tapi path mismatch (§17) |
| Organizer | `/organizer/*` | ⚠️ AKTIF tapi MOCK (§18) |
| Refund | `/api/refund/*`, `/api/tickets/refund/*` | ⚠️ AKTIF sebagian (§19) |
| Audit publik | — | internal saja (lihat `/admin/audit-logs` §16) |

`RescheduleRequest` **dihapus** — jangan panggil.

---

## Error Format Global (sudah rapi — ApiResponse konsisten)

`dto/ApiResponse.java:19` `@JsonInclude ALWAYS` + `GlobalExceptionHandler.java:14` + `SecurityConfig.java:42` — semua return `{msg,status,data}` via helper:

*   Validasi `@Valid` → `ApiResponse.badRequest` `400` `{"msg":"username: Username 3-20 karakter","status":400,"data":null}`
*   `RuntimeException` → `badRequest` `400`
*   `AuthenticationException` → `unauthorized` `401`
*   `AccessDeniedException` → `forbidden` `403`
*   `Exception` → `internalError` `500`
*   Sukses → `ok(200)` / `created(201)` (register/google baru) — `AuthController.java` `created`, `EventController.java` `ok`/`notFound(404)`, `HomeSearchController.java` `ok` (6 endpoint home/search), `UserController.java` `ok` (PR #11: profile/save/change-password/avatar/logout/transactions), `HomeController.java:10` `ok("Eventday API Server is Running!","OK")`

Network tab Chrome/Fetch: cek `Response` → `msg` untuk toast, `status` untuk branching, `data` untuk payload.

---

## Enum

*   `User.role`: `CUSTOMER` (default) / `ORGANIZER` / `ADMIN`
*   `Auth.status`: `INACTIVE` / `ACTIVE`
*   `events.status`: `DRAFT` / `PUBLISHED` / `CANCELLED` / `COMPLETED` (tampilkan `AVAILABLE` di API sebagai alias `PUBLISHED`)
*   `events.category`: `MUSIC_FESTIVAL` / `CONFERENCE` / `EXHIBITION` / `CULINARY` (API kirim display label)
*   `orders.status`: `PENDING` / `WAITING_PAYMENT` (mock VA PR #9) / `PAID` / `EXPIRED` / `CANCELLED`
*   `ticket_items.status`: `UNREDEEMED` / `CHECKED_IN` (hasil scan PR #9/#12) / `REDEEMED` / `EXPIRED`
*   `refund_requests.status`: `PENDING` / `APPROVED` / `REJECTED`
*   Lain schema-only: `organizers.verification_status UNVERIFIED`, `bookings PENDING`

---

## Flow Diagram Frontend (pakai `.data`)

```
register {name,email,username,password} --201 {msg,status:201,data} OTP--> verify-otp {email,otpCode} --200 {msg,status:200}--> login {identifier,password} --200 {msg,status:200,Set-Cookie access_token}--> Cookie HttpOnly
                                                                                                            |
google GIS idToken ------------------------POST /google --200/201 {msg,status,Set-Cookie}----------------+
                                                                                                            |
lupa password: POST /reset-password {email} --200 {msg}--email code--> POST /reset-password {email,code,newPassword} --200 {msg}--> login baru

CUSTOMER FLOW (setelah login, Bearer token):
  GET /events?category=&search=  --> CustomerDashboard grid + hero
  GET /events/featured            --> Hero slider (3 event)
  GET /events/{id}               --> DetailEventCustomer (pilih qty + ticketType)

CHECKOUT FLOW (setelah login):
  POST /checkout/initiate {tierId,quantity} --> Order PENDING + expiredAt
  POST /checkout/attendees {orderId,attendees[]} --> simpan order_attendees (PR #10)
  POST /checkout/calculation {tierId,quantity} --> subtotal+adminFee+tax10% (opsional, tidak simpan)
  POST /checkout/process {orderId}          --> status WAITING_PAYMENT (PR #10)
  POST /payments/charge {orderId,VIRTUAL_ACCOUNT,BCA} --> VA 88325... + WAITING_PAYMENT
  GET /orders/status?orderId=               --> polling status (PR #10)
  GET /checkout/summary?orderId=            --> ringkasan tagihan
  GET /tickets/user/{email}|/my-tickets?userEmail= --> [] sampai tiket diterbitkan
  GET /tickets/issued-detail?ticketCode=    --> detail E-Ticket (PR #12)
  POST /tickets/scan {ticketCode}           --> TIKET_VALID / TIKET_SUDAH_DIPAKAI

USER PROFILE FLOW (setelah login, PR #11):
  GET /user/profile                        --> data profil {name,email,username,phone,nik,role}
  PUT /user/profile/save {name,phone,nik}  --> update profil
  PUT /account/change-password {old,new}   --> ganti password + email notif
  POST /user/avatar (multipart file)       --> mock URL avatar
  POST /user/logout                        --> hapus cookie + token DB INACTIVE
  GET /transactions/history                --> riwayat order milik user

HOME & SEARCH FLOW (publik, tanpa token):
  GET /home/hero-banner           --> banner promo (max 5 featured)
  GET /home/event-card?page&size  --> grid homepage (size = jumlah data/halaman, bukan CSS)
  GET /home/locations             --> dropdown kota
  GET /search/results?keyword&category&location&date&page&size&sort --> hasil pencarian
  GET /search/locations|/search/categories --> opsi filter search
```

DB: `users --1:1-- auth (hash+googleId+token+resetToken) --1:N-- otp` (`password_reset_tokens` dihapus, digabung ke `auth`)

---

## Notes Frontend (copy-paste ready dengan `msg/status/data` + HttpOnly Cookie)

**Base URL (wajib lengkap dengan prefix `/api/v1/auth`):**
- Lokal (1 laptop): `http://localhost:8082/api/v1/auth`
- Lintas laptop (ngrok): `https://<id-baru>.ngrok-free.app/api/v1/auth` dari `ngrok http 8082` — ganti tiap restart ngrok. Jangan pakai `9538-...` (expired) atau `127.0.0.1:8000` (Django).
- **Contoh salah → 401:** `BASE = 'https://xxx.ngrok-free.app'` lalu `fetch(BASE + '/register')` → request ke `/register` (tidak ada di `SecurityConfig.java:55`) → `401`. **Benar:** `BASE = 'https://xxx.ngrok-free.app/api/v1/auth'` lalu `fetch(BASE + '/register')` → `POST /api/v1/auth/register` → `201`.

```js
// helper standar — token via HttpOnly Cookie, JANGAN pakai getApiBase
const BASE_AUTH = 'https://<id-baru>.ngrok-free.app/api/v1/auth'; // ngrok untuk teman beda laptop
// const BASE_AUTH = 'http://localhost:8082/api/v1/auth'; // untuk lokal
// const BASE = 'http://192.168.x.x:8082/api/v1/auth'; // alternatif satu WiFi tanpa ngrok

async function apiAuth(path, body){
  const res = await fetch(`${BASE_AUTH}${path}`, {
    method:'POST',
    headers:{ 'Content-Type':'application/json' },
    credentials: 'include', // WAJIB agar Set-Cookie access_token terkirim otomatis
    body: JSON.stringify(body)
  });
  const json = await res.json(); // {msg, status, data}
  if(!res.ok) throw new Error(json.msg);
  return json;
}
async function apiGet(path){
  // untuk events, BASE tanpa /auth: ganti /api/v1/auth → /api/v1
  const BASE_API = BASE_AUTH.replace('/auth','');
  const res = await fetch(`${BASE_API}${path}`, {
    credentials: 'include', // Cookie HttpOnly otomatis
    headers:{ 'Content-Type':'application/json' }
    // Alternatif jika mau Bearer manual: headers: { Authorization: `Bearer ${token}` }
  });
  const json = await res.json();
  if(!res.ok) throw new Error(json.msg);
  return json;
}

// contoh auth (path relatif terhadap BASE_AUTH)
const reg = await apiAuth('/register', {name,email,username,password}); // POST /api/v1/auth/register → 201
const v = await apiAuth('/verify-otp', {email, otpCode});
const login = await apiAuth('/login', {identifier: email, password}); // {msg,status,data:{expiresIn}}

// contoh customer events — src/services/eventService.js (JANGAN import getApiBase dari authService.js)
export const getEvents = (params={}) => apiGet(`/events?${new URLSearchParams(params)}`); // → data.content
export const getFeaturedEvents = () => apiGet('/events/featured');
export const getEventDetail = (id) => apiGet(`/events/${id}`);

// contoh home & search — publik, tanpa token (bisa tanpa credentials:include)
export const getHeroBanner = () => apiGet('/home/hero-banner'); // → data[]
export const getEventCards = (page=0,size=12) => apiGet(`/home/event-card?page=${page}&size=${size}`); // → data.content
export const getLocations = () => apiGet('/home/locations'); // → data[]
export const searchEvents = (p={}) => apiGet(`/search/results?${new URLSearchParams(p)}`); // keyword,category,location,date,page,size,sort
export const getSearchCategories = () => apiGet('/search/categories');

// contoh checkout & payment — perlu login (credentials:include), base /api/v1
export const initiateCheckout = (tierId,quantity) => fetch(`${BASE_API}/checkout/initiate`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({tierId,quantity})}).then(r=>r.json());
export const saveAttendees = (orderId,attendees) => fetch(`${BASE_API}/checkout/attendees`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId,attendees})}).then(r=>r.json()); // attendees:[{fullName,email,phoneNumber,identityNumber}] → order_attendees
export const calcCheckout = (tierId,quantity,discountAmount=0) => fetch(`${BASE_API}/checkout/calculation`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({tierId,quantity,discountAmount})}).then(r=>r.json()); // data:{subtotal,adminFee,tax,discount,totalAmount}
export const processCheckout = (orderId) => fetch(`${BASE_API}/checkout/process`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId})}).then(r=>r.json()); // → WAITING_PAYMENT
export const getOrderStatus = (orderId) => apiGet(`/orders/status?orderId=${orderId}`); // polling → data.status
export const getSummary = (orderId) => apiGet(`/checkout/summary?orderId=${orderId}`);
export const chargeVA = (orderId,bankCode='BCA') => fetch(`${BASE_API}/payments/charge`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId,paymentMethod:'VIRTUAL_ACCOUNT',bankCode})}).then(r=>r.json());
// ticket (base TANPA /v1! perlu login): GET /api/tickets/user/{email} + alias /my-tickets?userEmail= + GET /issued-detail?ticketCode= + POST /scan {ticketCode}
export const getMyTickets = (email) => apiGet(`/tickets/my-tickets?userEmail=${encodeURIComponent(email)}`); // data[] → TicketItem (PR #12)
export const getTicketDetail = (ticketCode) => apiGet(`/tickets/issued-detail?ticketCode=${ticketCode}`); // data → TicketDetailResponse (PR #12)
export const scanTicket = (ticketCode) => fetch(`${BASE_API.replace('/v1','')}/api/tickets/scan`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({ticketCode})}).then(r=>r.json()); // → data.status TIKET_VALID/TIKET_SUDAH_DIPAKAI

// contoh user/profile & logout — base /api/v1 (PR #11)
export const getProfile = () => apiGet('/user/profile'); // data:{userId,name,email,username,phone,nik,role,avatarUrl}
export const updateProfile = (payload) => fetch(`${BASE_API}/user/profile/save`,{method:'PUT',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}).then(r=>r.json()); // payload:{name,phone,nik}
export const changePassword = (oldPassword,newPassword) => fetch(`${BASE_API}/account/change-password`,{method:'PUT',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({oldPassword,newPassword})}).then(r=>r.json());
export const uploadAvatar = (file) => { const fd = new FormData(); fd.append('file',file); return fetch(`${BASE_API}/user/avatar`,{method:'POST',credentials:'include',body:fd}).then(r=>r.json()); }; // → data:"/uploads/avatars/<uuid>_<nama>"
export const logout = () => fetch(`${BASE_API}/user/logout`,{method:'POST',credentials:'include'}).then(r=>r.json());
export const getTransactions = () => apiGet('/transactions/history'); // → data[]
```

1. Base `http://localhost:8082`, `Content-Type: application/json`, `credentials:'include'` selalu.
2. Cek `json.status` & `json.msg` untuk toast, `json.data` untuk payload (`data` selalu ada via `ALWAYS`).
3. Register `201` `ApiResponse.created` → langsung ke form OTP.
4. Login `200` `ApiResponse.ok` → `data.expiresIn` + `Set-Cookie access_token` (tidak ada `data.token`).
5. Tanpa cookie → `401` `unauthorized` / `403` `forbidden` `{msg,status}` — redirect ke login.
6. `CorsConfig.java:14` `CorsFilter` bean `allowedOriginPatterns "*"` `allowCredentials true` `exposedHeaders Authorization,Content-Type` `maxAge 3600` — aman untuk ngrok.
7. OTP `5 menit`, Reset code `15 menit`, JWT `24 jam`.
8. `ApiLoggingFilter.java` format ASCII `[IN] [reqId] METHOD URI | IP | UA` → `[OUT] ... -> status (duration) user/auth/ip` + `[SLOW!]` jika >1s (tanpa warna/box-drawing) + console `%d %5p [%t] %logger : %m%n`.
9. Customer events paginated `?page&size` — default `page 0 size 12`. `size` = jumlah data/halaman, bukan ukuran CSS.
10. Harga number IDR, format `Intl.NumberFormat("id-ID",{style:"currency",currency:"IDR"})`.
11. Home & search publik tanpa token — bisa dites via browser/curl/`test-modul1.http`. `date` format `YYYY-MM-DD`.
12. User/Profile & logout (PR #11): endpoint `/user/**`, `/account/change-password`, `/transactions/history` — semua perlu login (cookie/Bearer); avatar masih mock (return URL, file belum disimpan).
```

---

# GAP ANALYSIS — Status Aktual vs Spec v1.3.0 + UI/UX Requests

> **Terakhir diperbarui:** 15 September 2026, berdasarkan kode di branch `main` (HEAD `60fe852`).
> **Catatan:** PDF audit (`laporan_gap_analysis_audit_rev10.pdf`) OUTDATED untuk modul Refund, Organizer, Legal, dan Payment.

## LEGENDA STATUS

| Simbol | Arti |
|---|---|
| ✅ REAL | Endpoint aktif, data persisten ke DB |
| ⚠️ MOCK | Endpoint aktif, data statis/hardcoded (belum ke DB) |
| ❌ BELUM | Belum ada controller/service |
| 🗑️ DIHAPUS | Pernah ada tapi sudah dihapus dari kode |
| 🔧 PERLU FIX | Ada tapi perlu perbaikan (body berubah, dsb) |

---

## A. SPEC V1.3.0 (48 Endpoints) — Status per Modul

### 01. Home & Search — ✅ 6/6 SELESAI

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| GET | `/api/v1/home/hero-banner` | ✅ REAL | `HomeSearchController` — max 5 featured |
| GET | `/api/v1/home/event-card?page&size` | ✅ REAL | Paginated, 7 event live |
| GET | `/api/v1/home/locations` | ✅ REAL | DISTINCT venueName |
| GET | `/api/v1/search/results` | ✅ REAL | Multi-filter keyword/category/date |
| GET | `/api/v1/search/locations` | ✅ REAL | Filter lokasi |
| GET | `/api/v1/search/categories` | ✅ REAL | DISTINCT category |

### 02. Events & Detail — ⚠️ 7/8 (missing 1)

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| GET | `/api/v1/events` | ✅ REAL | Paginated catalog |
| GET | `/api/v1/events/featured` | ✅ REAL | Hero slider max 3 |
| GET | `/api/v1/events/{id}` | ✅ REAL | Detail + lineup + tickets |
| GET | `/events/detail/banner` | ✅ REAL | Field `image` di detail |
| GET | `/events/detail/info` | ✅ REAL | Field title/date/location |
| GET | `/events/detail/description` | ✅ REAL | Field description |
| GET | `/events/detail/facilities` | ⚠️ REAL* | Raw string, frontend split(", ") |
| GET | `/events/detail/lineup` | ✅ REAL | JSON populated |
| GET | `/events/detail/tickets` | ✅ REAL | Field tickets array |
| **POST** | **`/events/create`** | **❌ BELUM** | **Event Organizer create event baru** |

### 03. Checkout & Payment — ✅ 11/11 SELESAI (Payment pakai Midtrans Snap)

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| POST | `/api/v1/checkout/initiate` | ✅ REAL | `@JsonIgnoreProperties` fix lazy-proxy |
| POST | `/api/v1/checkout/attendees` | ✅ REAL | Simpan ke `order_attendees` |
| POST | `/api/v1/checkout/calculation` | ✅ REAL | Tax 10%, diskon |
| GET | `/api/v1/checkout/summary` | ✅ REAL | Order summary |
| POST | `/api/v1/checkout/process` | ✅ REAL | Guard anti-downgrade |
| GET | `/api/v1/orders/status` | ✅ REAL | Polling status |
| GET | `/api/v1/orders/{id}/total-amount` | ✅ REAL | Total amount |
| GET | `/api/v1/orders/{id}/expired-time` | ✅ REAL | Expired time |
| POST | `/api/payments/charge` | ✅ REAL | **Midtrans Snap** (bukan mock VA) |
| POST | `/api/payments/midtrans-notification` | ⚠️ MOCK | Webhook log only, belum update order |
| GET | `/payments/methods` | 🗑️ DIHAPUS | Tidak ada di kode |
| GET | `/payments/methods/virtual-account` | 🗑️ DIHAPUS | Tidak ada di kode |

### 04. My Tickets & E-Ticket — ✅ 4/4 SELESAI

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| GET | `/api/tickets/user/{email}` | ✅ REAL | List tiket (kosong sampai `generateTicket` dipanggil) |
| GET | `/api/tickets/my-tickets?userEmail=` | ✅ REAL | Alias PR #12 |
| GET | `/api/tickets/issued-detail?ticketCode=` | ✅ REAL | E-Ticket detail |
| POST | `/api/tickets/scan` | ✅ REAL | QR scan |
| GET | `/tickets/view-ticket` | — | Frontend React only |
| GET | `/tickets/download-qr` | — | Frontend React only |

### 05. Refund — ⚠️ 6/7 (1 endpoint perlu perbaikan)

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| POST | `/api/tickets/refund/request` | ✅ REAL | Submit ke DB (`refund_requests`) |
| POST | `/api/refund/submit` | ✅ REAL | Alias dari `/tickets/refund/request` |
| GET | `/api/refund/banks` | ✅ REAL | BCA/MANDIRI/BNI/BRI |
| GET | `/api/refund/order-summary` | ⚠️ MOCK | Hardcoded `290000.00` |
| GET | `/api/refund/refund-detail/info` | ✅ REAL | Dari DB |
| GET | `/api/refund/refund-detail/download-proof` | ⚠️ MOCK | Return `proofUrl` kosong |
| GET | `/api/tickets/refund/refund-history` | ⚠️ MOCK | Hardcoded 1 item |

### 06. User Profile & Transaksi — ✅ 6/6 SELESAI

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| GET | `/api/v1/user/profile` | ✅ REAL | Biodata profil |
| PUT | `/api/v1/user/profile/save` | ✅ REAL | Update name/phone/NIK |
| PUT | `/api/v1/account/change-password` | ✅ REAL | + email notif |
| POST | `/api/v1/user/avatar` | ⚠️ MOCK | Return mock URL |
| POST | `/api/v1/user/logout` | ✅ REAL | Null token + hapus cookie |
| GET | `/api/v1/transactions/history` | ✅ REAL | Order history |

### 07. Organizer & Legal — ⚠️ 6/6 ADA (semua MOCK/stub)

| Method | Endpoint | Status | Keterangan |
|---|---|---|---|
| POST | `/organizer/register` | ⚠️ MOCK | Return statis |
| POST | `/organizer/documents/upload` | ⚠️ MOCK | Return filename |
| GET | `/organizer/status` | ⚠️ MOCK | Hardcoded PENDING |
| GET | `/organizer/dashboard` | ⚠️ MOCK | Hardcoded metrics |
| GET | `/terms-conditions` | ✅ REAL | Static content |
| GET | `/privacy-policy` | ✅ REAL | Static content |

---

## B. PERMINTAAN BARU UI/UX (31 Endpoints) — Status per Area

### B1. Organizer Portal — 16 Endpoints

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Profil** | GET | `/organizer/profile` | ⚠️ MOCK | Static profile |
| | PUT | `/organizer/profile` | ⚠️ MOCK | Echo payload |
| | POST | `/organizer/profile/avatar` | ⚠️ MOCK | Mock URL |
| | POST | `/organizer/profile/upload-portfolio` | ⚠️ MOCK | Mock URL |
| | POST | `/organizer/profile/upload-deed` | ⚠️ MOCK | Mock URL |
| | GET | `/organizer/profile/document` | ⚠️ MOCK | Static docs |
| **Auth** | POST | `/organizer/auth/change-password` | ⚠️ MOCK | No-op |
| | POST | `/organizer/auth/logout` | ⚠️ MOCK | No-op |
| **Refund** | GET | `/organizer/refunds` | ⚠️ MOCK | Static list |
| | GET | `/organizer/refunds/detail` | ⚠️ MOCK | Static detail |
| | PATCH | `/organizer/refunds/{id}/status` | ⚠️ MOCK | Echo status |
| **Payout** | GET | `/organizer/bank-accounts` | ⚠️ MOCK | Static bank |
| | GET | `/organizer/events/{id}/payout-balance` | ⚠️ MOCK | Static balance |
| | GET | `/organizer/payouts` | ⚠️ MOCK | Static list |
| | POST | `/organizer/payouts` | ⚠️ MOCK | Static response |
| | GET | `/organizer/payouts/detail` | ⚠️ MOCK | Static detail |

### B2. Organizer Events — 6 Endpoints (❌ BELUM ADA)

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Manajemen Event** | GET | `/organizer/events` | ❌ BELUM | List event milik EO |
| | POST | `/organizer/events` | ❌ BELUM | Buat event baru |
| | PUT | `/organizer/events/update` | ❌ BELUM | Update event |
| | POST | `/organizer/events/publish` | ❌ BELUM | Publish event |
| | GET | `/organizer/events/draft` | ❌ BELUM | Draft events |
| | GET | `/organizer/events/{id}/sales-summary` | ❌ BELUM | Ringkasan penjualan |

### B3. Organizer Dashboard — 3 Endpoints (❌ BELUM ADA)

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Dashboard** | GET | `/organizer/dashboard/metrics` | ❌ BELUM | Metrics detail |
| | GET | `/organizer/dashboard/recent-events` | ❌ BELUM | Event terbaru |
| | GET | `/organizer/dashboard/recent-transactions` | ❌ BELUM | Transaksi terbaru |

### B4. Admin EO Applications — 4 Endpoints (❌ BELUM ADA)

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Verifikasi EO** | GET | `/admin/eo-applications` | ❌ BELUM | List aplikasi EO |
| | GET | `/admin/eo-applications/detail` | ❌ BELUM | Detail aplikasi |
| | PATCH | `/admin/eo-applications/status` | ❌ BELUM | Approve/reject |
| | GET | `/admin/eo-applications/{id}/documents/company-deed` | ❌ BELUM | Dokumen akta |

### B5. Admin Payouts — 4 Endpoints (❌ BELUM ADA)

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Pencairan Dana** | GET | `/admin/payouts` | ❌ BELUM | List payout |
| | GET | `/admin/payouts/detail` | ❌ BELUM | Detail payout |
| | PATCH | `/admin/payouts/status` | ❌ BELUM | Approve/reject |
| | GET | `/admin/payouts/{id}/documents/reconciliation` | ❌ BELUM | Dokumen rekonsiliasi |

### B6. Admin Audit & Settings — 4 Endpoints (❌ BELUM ADA)

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Audit** | GET | `/admin/audit-logs` | ❌ BELUM | Log jejak audit |
| | PUT | `/admin/audit-logs/export` | ❌ BELUM | Export audit log |
| **Settings** | GET | `/admin/settings/general` | ❌ BELUM | Pengaturan sistem |
| | POST | `/admin/settings/upload-logo` | ❌ BELUM | Upload logo |

### B7. Admin Dashboard & Users — ✅ 7/7 SUDAH ADA

| Area | Method | Endpoint | Status | Keterangan |
|---|---|---|---|---|
| **Dashboard** | GET | `/admin/dashboard/metrics` | ✅ REAL | `AdminDashboardService` |
| | GET | `/admin/dashboard/recent-events` | ✅ REAL | 5 event terbaru |
| | GET | `/admin/dashboard/recent-transactions` | ✅ REAL | 10 transaksi terbaru |
| **Users** | GET | `/admin/users` | ✅ REAL | Filter by role |
| | GET | `/admin/users/{id}` | ✅ REAL | Detail user |
| | PATCH | `/admin/users/{id}/status` | ✅ REAL | ACTIVE/INACTIVE/SUSPENDED |
| | PATCH | `/admin/users/{id}/suspend` | ✅ REAL | Suspend + cabut JWT |

---

## C. RINGKASAN KEBUTUHAN IMPLEMENTASI

### Yang perlu dibuat BARU (❌ BELUM ADA) — 21 endpoint:

| # | Endpoint | Modul | Prioritas |
|---|---|---|---|
| 1 | `POST /events/create` | Events | Tinggi |
| 2 | `GET /organizer/events` | Organizer Events | Tinggi |
| 3 | `POST /organizer/events` | Organizer Events | Tinggi |
| 4 | `PUT /organizer/events/update` | Organizer Events | Tinggi |
| 5 | `POST /organizer/events/publish` | Organizer Events | Tinggi |
| 6 | `GET /organizer/events/draft` | Organizer Events | Sedang |
| 7 | `GET /organizer/events/{id}/sales-summary` | Organizer Events | Sedang |
| 8 | `GET /organizer/dashboard/metrics` | Organizer Dashboard | Sedang |
| 9 | `GET /organizer/dashboard/recent-events` | Organizer Dashboard | Sedang |
| 10 | `GET /organizer/dashboard/recent-transactions` | Organizer Dashboard | Sedang |
| 11 | `GET /admin/eo-applications` | Admin EO | Tinggi |
| 12 | `GET /admin/eo-applications/detail` | Admin EO | Tinggi |
| 13 | `PATCH /admin/eo-applications/status` | Admin EO | Tinggi |
| 14 | `GET /admin/eo-applications/{id}/documents/company-deed` | Admin EO | Sedang |
| 15 | `GET /admin/payouts` | Admin Payout | Tinggi |
| 16 | `GET /admin/payouts/detail` | Admin Payout | Tinggi |
| 17 | `PATCH /admin/payouts/status` | Admin Payout | Tinggi |
| 18 | `GET /admin/payouts/{id}/documents/reconciliation` | Admin Payout | Sedang |
| 19 | `GET /admin/audit-logs` | Admin Audit | Sedang |
| 20 | `PUT /admin/audit-logs/export` | Admin Audit | Rendah |
| 21 | `GET /admin/settings/general` | Admin Settings | Sedang |
| 22 | `POST /admin/settings/upload-logo` | Admin Settings | Rendah |

### Yang perlu di-upgrade dari MOCK ke REAL — 18 endpoint:

| # | Endpoint | Modul | Yang perlu diubah |
|---|---|---|---|
| 1 | `POST /organizer/register` | Organizer | Simpan ke DB `organizers` |
| 2 | `POST /organizer/documents/upload` | Organizer | Simpan file ke storage |
| 3 | `GET /organizer/status` | Organizer | Baca dari DB |
| 4 | `GET /organizer/dashboard` | Organizer | Query dari DB |
| 5 | `GET /organizer/profile` | Organizer | Baca dari DB |
| 6 | `PUT /organizer/profile` | Organizer | Update DB |
| 7 | `POST /organizer/profile/avatar` | Organizer | Simpan file |
| 8 | `POST /organizer/profile/upload-portfolio` | Organizer | Simpan file |
| 9 | `POST /organizer/profile/upload-deed` | Organizer | Simpan file |
| 10 | `GET /organizer/profile/document` | Organizer | Baca dari DB |
| 11 | `POST /organizer/auth/change-password` | Organizer | BCrypt + update DB |
| 12 | `POST /organizer/auth/logout` | Organizer | Null token |
| 13 | `GET /organizer/refunds` | Organizer | Query dari `refund_requests` |
| 14 | `GET /organizer/refunds/detail` | Organizer | Query dari DB |
| 15 | `GET /organizer/bank-accounts` | Organizer | Baca dari DB |
| 16 | `GET /organizer/payouts` | Organizer | Query dari DB |
| 17 | `POST /organizer/payouts` | Organizer | Simpan ke DB |
| 18 | `GET /organizer/payouts/detail` | Organizer | Query dari DB |

### Yang perlu diperbaiki — 4 endpoint:

| # | Endpoint | Issue |
|---|---|---|
| 1 | `GET /api/refund/order-summary` | Hardcoded `290000.00` — harus query dari order |
| 2 | `GET /api/refund/refund-detail/download-proof` | Return `proofUrl` kosong |
| 3 | `GET /api/tickets/refund/refund-history` | Hardcoded 1 item — harus query dari DB |
| 4 | `POST /api/payments/midtrans-notification` | Log only — belum update order status ke PAID |

### Services yang belum ada (perlu dibuat):

| Service | Modul | Fungsi |
|---|---|---|
| `EventCreationService` | Events | CRUD event baru (atau extend `EventService`) |
| `OrganizerEventService` | Organizer Events | Manajemen event milik EO |
| `OrganizerDashboardService` | Organizer Dashboard | Metrics & recent dari DB |
| `AdminEoService` | Admin EO | Verifikasi aplikasi EO |
| `AdminPayoutService` | Admin Payout | Validasi & approve payout |
| `AdminAuditService` | Admin Audit | Log jejak & export |
| `AdminSettingsService` | Admin Settings | Pengaturan sistem |
| `PayoutService` | Organizer Payout | Pengajuan & riwayat payout |

### Entity/DTO yang belum ada (perlu dibuat):

| Entity/DTO | Modul | Keterangan |
|---|---|---|
| `OrganizerEvent` | Events | Entity event milik EO |
| `Payout` | Payout | Entity payout |
| `PayoutDocument` | Payout | Entity dokumen rekonsiliasi |
| `EoApplication` | Admin EO | Entity aplikasi EO |
| `AdminAuditLog` | Admin Audit | Entity audit log (atau extend `AuditLog`) |
| `AdminSettings` | Admin Settings | Entity settings (atau extend `Settings`) |

---

## D. CATATAN PENTING UNTUK AI AGENT

1. **Branch `main`** (HEAD `60fe852`) adalah source of truth. `AdminSettingsController` + `AdminEoController` ADA DI BRANCH `feat/admin-eo-audit` tapi **TIDAK ADA DI main**.
2. **PaymentController** hanya punya 2 endpoint: `POST /charge` (Midtrans Snap) + `POST /midtrans-notification` (webhook). `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS**.
3. **OrganizerController** punya 21 endpoint tapi **SEMUA MOCK/stub** — semua return `Map<String, Object>` statis, belum persisten ke DB.
4. **RefundController** punya 6 endpoint — submit **REAL** ke DB, sisanya mock.
5. **LegalController** punya 2 endpoint — static content (bukan `/api/v1/...` tapi `/terms-conditions`).
6. **generateTicket()** di `TicketService` belum dipanggil siapapun → my-tickets kosong.
7. **SecurityConfig**: `/organizer/**`, `/api/refund/**`, `/api/tickets/refund/**` = authenticated. `/terms-conditions`, `/privacy-policy` = public (tapi path mismatch dengan `/api/v1/...`).
8. **DTO `PaymentChargeRequest` / `PaymentChargeResponse`** mungkin sudah tidak akurat — charge sekarang pakai `Map` langsung, return `{snapToken, redirectUrl}`.
9. **Entity `RefundRequestEntity`** (bukan `RefundRequest`) — `RefundRequest` di dto/ adalah DTO request, bukan entity.
10. **`order_attendees`** tabel ke-13, dibuat Hibernate `ddl-auto=update`, tidak ada di V1 migration.

