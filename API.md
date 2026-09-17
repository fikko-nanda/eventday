# API.md - Eventday REST API Documentation

Base URL (lokal): `http://localhost:8082`
Base URL (ngrok lintas-laptop): `https://<id-baru>.ngrok-free.app` → `ngrok http 8082`

> **Status 2026-09-14 (rev.8):** PR #12 (HEAD `0405d44`) **lengkapi Ticket** — `GET /api/tickets/user/{email}` alias `GET /api/tickets/my-tickets?userEmail=` (dua-duanya list `TicketItem`), **baru** `GET /api/tickets/issued-detail?ticketCode=<UUID>` (payload E-Ticket → `TicketDetailResponse`), `POST /api/tickets/scan` (sama). Modul aktif: Auth + Event Catalog + Home & Search (publik) + Checkout (8) + Payment + Ticket (4) + User/Profile.
> **Status 2026-09-16 (rev.13):** Tim merge PR #23 (refund), #24 (organizer-db-integration), #25 (legal) → HEAD `6e7bcfd`. Perubahan besar: ① Legal BUG **FIXED** — dual alias + permitAll 4 path, verified live `200` tanpa token (server di-restart, build lama yang bikin "masih bug"); ② **BREAKING**: `RefundController` `/api`→`/api/v1`, `OrganizerController` `/organizer`→`/api/organizer` (path lama → `500`); ③ Endpoint baru: `POST /api/events/create` (MOCK, **publik**), 9 stub organizer (`events`×6 + `dashboard`×3); ④ `order-summary` kini REAL (hitung dari order), `submitRefund` isi `organizerId` selalu (fallback = customerId), banks 8 bank + logo; ⑤ avatar user kini REAL (validasi image/5MB, tersimpan, diserve `WebConfig /uploads/**`); ⑥ `midtrans-notification` kini **publik**; ⑦ `LegalService` konten kaya (slug/version/sections). Total **98 path unik: 79 REAL, 17 MOCK, 2 PARTIAL, 0 BUG**.
> **Last Updated:** 2026-09-16 — sinkron PR #23-25 + restart server + verifikasi live, DB `localhost:5432/eventday`.
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
| 1 | POST | `/api/auth/register` | Registrasi + kirim OTP | `201` sukses, `400` duplikat |
| 2 | POST | `/api/auth/verify-otp` | Aktivasi akun | `200` |
| 3 | POST | `/api/auth/resend-otp` | Kirim ulang OTP | `200` |
| 4 | POST | `/api/auth/login` | Login email/username | `200` |
| 5 | POST | `/api/auth/google` | Login Google GIS | `200` / `201` baru |
| 6 | POST | `/api/auth/reset-password` | Lupa password 2 tahap | `200` |

Alias legacy `POST /api/auth/**` juga permit.

> **⚠️ Troubleshooting 401 vs CORS (kasus screenshot `/register` → 401):**
> Network `register` → `401 Unauthorized` + `Access-Control-Allow-Origin: http://localhost:5173` + `Access-Control-Allow-Credentials: true` = **CORS sudah benar** (`CorsConfig.java:14` `CorsFilter` bean + `SecurityConfig.java:38` `.cors(cors->{})`). 401 terjadi karena frontend nembak `https://xxx.ngrok-free.app/register` (tanpa prefix), sedangkan `SecurityConfig.java:55` hanya `permitAll` untuk `/api/auth/**` dan `/api/auth/**`. Fix: `BASE` harus `https://xxx.ngrok-free.app/api/auth` sehingga request jadi `POST /api/auth/register` → `201`. Jangan pakai `BASE` tanpa suffix `/api/auth`.

## Daftar Endpoint Customer (Publik — lihat catatan SecurityConfig)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 7 | GET | `/api/events` | List event + filter category/search/location + pagination | `200` |
| 8 | GET | `/api/events/featured` | Event unggulan untuk hero slider (max 3) | `200` |
| 9 | GET | `/api/events/{id}` | Detail event + lineup + tiket | `200` / `404` |

> \*SUDAH COMMITTED di HEAD: `SecurityConfig.java:67` `/api/events/**` permitAll — akses publik tanpa Bearer, bisa dibuka via browser/curl. Legal juga publik 4 path (FIXED rev.13, verified live `200`).

## Daftar Endpoint Home & Search (Public — tanpa JWT)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 10 | GET | `/api/home/hero-banner` | Banner promo/sorotan utama (max 5 featured) | `200` |
| 11 | GET | `/api/home/event-card` | Daftar card event aktif + pagination `?page&size` | `200` |
| 12 | GET | `/api/home/locations` | Daftar kota/lokasi unik (filter) | `200` |
| 13 | GET | `/api/search/results` | Pencarian multi-filter `?keyword&category&location&date&page&size&sort` | `200` |
| 14 | GET | `/api/search/locations` | Daftar lokasi unik untuk filter search | `200` |
| 15 | GET | `/api/search/categories` | Daftar kategori unik untuk filter search | `200` |

> `SecurityConfig.java` `permitAll` untuk `/api/home/**` + `/api/search/**` — bisa dibuka via browser/curl tanpa token. Data kosong → `data:[]` / `content:[]`, bukan error.

## Daftar Endpoint Checkout/Order (perlu login — PR #9 + PR #10)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 16 | POST | `/api/checkout/initiate` | Buat order `{tierId,quantity}` → `PENDING` + expired 15 mnt | `200` |
| 17 | POST | `/api/checkout/attendees` | Simpan peserta ke `order_attendees` (PR #10, bukan stub lagi) | `200` |
| 18 | POST | `/api/checkout/calculation` | Hitung rincian — subtotal + adminFee + tax 10% − discount (PR #10) | `200` |
| 19 | POST | `/api/checkout/process` | Ubah status order → `WAITING_PAYMENT` (PR #10) | `200` |
| 20 | GET | `/api/orders/status?orderId=` | Polling status order (PR #10) | `200` |
| 21 | GET | `/api/checkout/summary?orderId=` | Ringkasan tagihan | `200` |
| 22 | GET | `/api/orders/{orderId}/total-amount` | Total nominal | `200` |
| 23 | GET | `/api/orders/{orderId}/expired-time` | Batas waktu bayar | `200` |

## Daftar Endpoint Payment (perlu login — Midtrans Snap REAL, §13)

| # | Method | Endpoint | Deskripsi | HTTP |
|---|--------|----------|-----------|------|
| 24 | POST | `/api/payments/charge` | `{orderId, grossAmount, customerName, customerEmail}` → Midtrans Snap → `{snapToken, redirectUrl}` | `200` |
| 25 | POST | `/api/payments/midtrans-notification` | Webhook Midtrans (log only, belum update order) | `200` |

> ⚠️ `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** dari kode.

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
| 31 | GET | `/api/user/profile` | Profil akun sendiri | `200` |
| 32 | PUT | `/api/user/profile/save` | Update nama/phone/nik | `200` |
| 33 | PUT | `/api/account/change-password` | Ganti password (wajib old + new min 6) | `200` |
| 34 | POST | `/api/user/avatar` | Upload avatar (multipart → mock URL) | `200` |
| 35 | POST | `/api/user/logout` | Logout — hapus cookie + invalidasi token DB | `200` |
| 36 | GET | `/api/transactions/history` | Riwayat transaksi/order milik user | `200` |

---

## 1. Auth - Register

**POST** `/api/auth/register` → `201`

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
curl -X POST localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456"}'
# Network tab: Status 201, Response {msg, status:201, data:{...}}
```

> Setelah register, user **INACTIVE** — harus `verify-otp` dulu baru bisa login.

---

## 2. Auth - Verify OTP

**POST** `/api/auth/verify-otp` → `200`

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
curl -X POST localhost:8082/api/auth/verify-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com","otpCode":"123456"}'
```

---

## 3. Auth - Resend OTP

**POST** `/api/auth/resend-otp` → `200`

### Request
```json
{"email":"john@example.com"}
```

### Response `200`
```json
{"msg":"OTP baru berhasil dikirim ke email Anda!","status":200,"data":null}
```

```bash
curl -X POST localhost:8082/api/auth/resend-otp -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'
```

---

## 4. Auth - Login

**POST** `/api/auth/login` → `200`

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
curl -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}' -c cookies.txt
# token tersimpan di Set-Cookie: access_token (HttpOnly). Untuk curl manual ambil dari DB atau pakai -b cookies.txt
# curl -b cookies.txt localhost:8082/api/events
```

---

## 5. Auth - Google Login

**POST** `/api/auth/google` → `200` (login) / `201` (registrasi baru)

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
curl -X POST localhost:8082/api/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
```

**Frontend GIS** (client ID wajib SAMA dengan backend, copy-paste):
```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
<div id="g_id_onload"
     data-client_id="875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com"
     data-callback="onGoogleCredential"></div>
<script>
  function onGoogleCredential(res) {
    fetch(`${BASE_AUTH}/google`, { // BASE_AUTH = .../api/auth
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include", // WAJIB — tanpanya cookie login Google tidak tersimpan → 401
      body: JSON.stringify({ idToken: res.credential })
    }).then(r => r.json());
  }
</script>
```
`res.credential` → `POST /google {idToken}` → `Set-Cookie access_token`. Backend (`AuthService.java:362-364`) tolak token bila `aud != google.client-id` di atas (`Token Google aud tidak sesuai`), jadi GIS wajib pakai client ID yang sama persis.

---

## 6. Auth - Reset Password (Lupa Password)

**POST** `/api/auth/reset-password` — **single endpoint 2 tahap**.

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
curl -X POST localhost:8082/api/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'
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
curl -X POST localhost:8082/api/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com","code":"123456","newPassword":"newPass123"}'
```

---

## 7. Middleware JWT & Protected Routes

Publik (tanpa JWT): `/api/auth/**`, `/api/home/**`, `/api/search/**`, `/api/events/**`, `/`, `/error` — **SUDAH di-commit** di `SecurityConfig.java:55-73`. Sisanya (Checkout, Payment, Ticket, dst) butuh JWT. `SecurityConfig.java:40` `STATELESS`.

> ✅ **Legal FIXED (rev.13, PR #25):** `LegalController` dual alias (`/terms-conditions` + `/api/terms-conditions`, sama untuk privacy-policy) + `SecurityConfig:70` permit keempat path → **publik tanpa token** (verified live `200`). Isu BUG rev.11/12 sudah tutup.

`JwtAuthenticationFilter.java:36`: `Authorization: Bearer <token>` **atau** `Cookie: access_token` (`resolveToken()`) → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`.

`SecurityConfig.java` kini return standard `msg/status/data` untuk `401/403`:

```json
// tanpa token
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
// token salah/expired/inactive
{"msg":"Forbidden: akses ditolak","status":403,"data":""}
```

```bash
curl -c cookies.txt -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -b cookies.txt localhost:8082/api/events  # 200 jika ada data PUBLISHED
curl localhost:8082/api/events  # -> 200 juga (PUBLIK — SecurityConfig events/** COMMITTED)
curl localhost:8082/ # -> 200 {msg, status:200, data:"OK"}
curl localhost:8082/api/checkout/summary?orderId=xxx  # -> 401 {msg, status:401} (perlu login)
# Alternatif manual: curl -H "Authorization: Bearer <token-dari-DB>" localhost:8082/api/events
```

Config `application.properties`:
```properties
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000 # 24 jam -> data.expiresIn 86400
google.client-id=875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com
```

---

## 8. Customer - Events Catalog (Dashboard)

### GET `/api/events` — List event untuk `CustomerDashboard.jsx`

**Auth:** **publik** — `SecurityConfig.java:67` sudah di-commit (`/api/events/**` permitAll), boleh akses tanpa token (browser/curl). Role `CUSTOMER` / `ORGANIZER` / `ADMIN` tetap boleh akses via Bearer/Cookie.

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

**Alternatif Hero:** `GET /api/events/featured` → khusus 3 event hero, response shape sama.

```bash
# pakai cookie (recommended, token HttpOnly):
curl -b cookies.txt "localhost:8082/api/events?category=MUSIC%20FESTIVAL&search=Neon&page=0&size=12"
curl -b cookies.txt "localhost:8082/api/events/featured"
# atau pakai Bearer jika token diambil manual dari DB/auth:
curl -H "Authorization: Bearer $TOKEN" "localhost:8082/api/events?category=MUSIC%20FESTIVAL&search=Neon&page=0&size=12"
```

**Error:**
```json
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
```

---

## 9. Customer - Event Detail

### GET `/api/events/{id}` — Detail `DetailEventCustomer.jsx`

**Auth:** **publik** — sudah di-commit di `SecurityConfig.java:67` (`/api/events/**` permitAll), bisa diakses tanpa token.

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
curl -b cookies.txt localhost:8082/api/events/{uuid}
# atau: curl -H "Authorization: Bearer $TOKEN" localhost:8082/api/events/{uuid}
```

---

## 10. Home & Search — Hero, Event Card, Locations

### GET `/api/home/hero-banner` — Public, tanpa token
Ambil max 5 event `isFeatured=true` + `status=PUBLISHED` (`HomeSearchService.getHeroBanners()`).
```json
{"msg":"Berhasil mengambil hero banner","status":200,"data":[{"id":"uuid","title":"Neon Nights","bannerUrl":"https://...","eventDate":"2026-12-15T19:00:00","targetUrl":"/events/uuid"}]}
```
DB kosong → `{"msg":"...","status":200,"data":[]}`.

### GET `/api/home/event-card?page=0&size=12` — Public
Card event aktif paginated. `size` = **jumlah data per halaman** (bukan ukuran CSS).
```json
{"msg":"Berhasil mengambil event card","status":200,"data":{"content":[{"id":"uuid","title":"Neon Nights","posterUrl":"https://...","location":"GBK","category":"MUSIC_FESTIVAL","categoryLabel":"Musik","startDate":"2026-12-15T19:00:00","dateDisplay":"15 Dec 2026","lowestPrice":200000,"priceDisplay":"Rp 200.000"}],"page":0,"size":12,"totalElements":42,"totalPages":4}}
```
`lowestPrice` = harga min dari `ticket_tiers`, fallback `0` jika belum ada tier.

### GET `/api/home/locations` — Public
```json
{"msg":"Berhasil mengambil daftar lokasi","status":200,"data":["Jakarta","Bandung"]}
```
Query: `SELECT DISTINCT e.venueName ... WHERE status='PUBLISHED'` (`EventRepository.findDistinctLocations()`). Kosong → `[]`.

```bash
curl "localhost:8082/api/home/hero-banner"
curl "localhost:8082/api/home/event-card?page=0&size=12"
curl "localhost:8082/api/home/locations"
# atau buka langsung di browser (GET publik, tanpa token)
```

---

## 11. Search — Results, Locations, Categories

### GET `/api/search/results` — Public
| Param | Deskripsi | Contoh |
|---|---|---|
| `keyword` | cari di title/description/venue (case-insensitive) | `?keyword=neon` |
| `category` | `MUSIC_FESTIVAL`/`CONFERENCE`/`EXHIBITION`/`CULINARY`, `Semua`/`ALL` = tanpa filter | `?category=MUSIC_FESTIVAL` |
| `location` | partial match venue | `?location=Jakarta` |
| `date` | format `YYYY-MM-DD`, cocok `CAST(startDate AS date)` | `?date=2026-12-15` |
| `page`/`size`/`sort` | pagination, `sort=latest` (default), `date_asc`, `price_asc/desc` | `?page=0&size=12&sort=latest` |

Response sama shape dengan event-card (`{content,page,size,totalElements,totalPages}`).

### GET `/api/search/locations` — Public → `[string]` lokasi unik
### GET `/api/search/categories` — Public → `[string]` kategori unik (`SELECT DISTINCT e.category ... PUBLISHED`)

```bash
curl "localhost:8082/api/search/results?keyword=neon&category=MUSIC_FESTIVAL&location=Jakarta&page=0&size=12&sort=latest"
curl "localhost:8082/api/search/results?date=2026-12-15"
curl "localhost:8082/api/search/locations"
curl "localhost:8082/api/search/categories"
```

---

## 12. Checkout & Order (perlu login)

### POST `/api/checkout/initiate` — body `{tierId, quantity}`
`OrderService.createOrder`: cek `availableQuota` (tidak dikurangi!) → `subtotal = price×qty`, `total = subtotal + adminFee(5000)` → save `Order(PENDING, expiredAt +15mnt)`. Return entity `Order` langsung.
```bash
curl -b cookies.txt -X POST localhost:8082/api/checkout/initiate -H "Content-Type: application/json" -d '{"tierId":"<tier-uuid>","quantity":2}'
```

### POST `/api/checkout/attendees` — simpan peserta (PR #10, bukan stub lagi)
Body:
```json
{"orderId":"<order-uuid>","attendees":[{"fullName":"Budi","email":"budi@mail.com","phoneNumber":"08123456","identityNumber":"3201234567890123"}]}
```
Simpan ke tabel `order_attendees` (`model/Attendee`, PK Long IDENTITY) — `identityNumber` = NIK/No. KTP, `orderId` plain String (bukan FK/UUID). Respons `data` = `[{id, orderId, fullName, email, phoneNumber, identityNumber}]`.

### POST `/api/checkout/calculation` — hitung rincian (PR #10, TIDAK menyimpan order)
Body `{tierId, quantity, discountAmount?}` → `data`:
```json
{"msg":"Kalkulasi checkout berhasil","status":200,"data":{"subtotal":400000,"adminFee":5000,"tax":40000,"discount":0,"totalAmount":445000}}
```
`tax` = `subtotal × 10%`, `totalAmount = subtotal + adminFee + tax − discount`.

### POST `/api/checkout/process` — kunci order (PR #10)
Body `{orderId}` → status order jadi `WAITING_PAYMENT` (TIDAK cek `PENDING` dulu — beda dengan `/payments/charge` yang wajib PENDING). Return entity `Order`.

### GET `/api/orders/status?orderId=<uuid>` — polling (PR #10)
`data` = `{"orderId":"<uuid>","status":"WAITING_PAYMENT"}`.

### GET `/api/checkout/summary?orderId=<uuid>` → `CheckoutSummaryResponse`
`{orderId, orderNumber("ORD-XXXXXXXX"), eventTitle, ticketTierName, quantity, pricePerTicket, subtotal, adminFee, discountAmount(0), totalAmount, expiredAt}`.
### GET `/api/orders/{id}/total-amount` → `{totalAmount}` · GET `/api/orders/{id}/expired-time` → `{expiredAt}`

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

### GET `/api/user/profile`
```json
{"msg":"Berhasil mengambil data profil","status":200,"data":{"userId":"uuid","name":"John Doe","email":"john@mail.com","username":"john123","phone":"08123456789","nik":"3201234567890123","role":"CUSTOMER","avatarUrl":null}}
```

### PUT `/api/user/profile/save`
Body `{"name":"John Doe","phone":"08123456789","nik":"3201234567890123"}` — `name` @NotBlank @Size100, `phone` @Size15, `nik` `^\d{16}$` unik.
```json
{"msg":"Profil berhasil diperbarui","status":200,"data":{...UserProfileResponse terbaru...}}
```
Error `400` NIK duplikat: `{"msg":"NIK sudah digunakan akun lain!","status":400,"data":null}`.

### PUT `/api/account/change-password`
Body `{"oldPassword":"123456","newPassword":"newPass123"}` — `newPassword` min 6, tidak boleh sama dengan `oldPassword`. Verifikasi `matches(old)` → simpan BCrypt baru → audit `CHANGE_PASSWORD` + email notifikasi `sendPasswordChangedNotification`.
```json
{"msg":"Password berhasil diubah","status":200,"data":null}
```
Error `400`: `{"msg":"Password lama salah!","status":400,"data":null}` / `{"msg":"Password baru tidak boleh sama dengan password lama!","status":400,"data":null}`.

### POST `/api/user/avatar` (multipart)
`-F "file=@avatar.jpg"` → return mock URL (file belum disimpan):
```json
{"msg":"Avatar berhasil diperbarui","status":200,"data":"/uploads/avatars/<uuid>_avatar.jpg"}
```

### POST `/api/user/logout`
Panggil `AuthService.logout` (null-kan `aksesToken/expiredToken` + status `INACTIVE`) + `Set-Cookie access_token` maxAge 0 (hapus cookie).
```json
{"msg":"Logout berhasil","status":200,"data":null}
```
Setelah logout, token lama tidak valid (`403`).

### GET `/api/transactions/history`
`OrderRepository.findByCustomerUserId` → `data[]`:
```json
{"msg":"Berhasil mengambil riwayat transaksi","status":200,"data":[{"orderId":"uuid","orderNumber":"ORD-XXXXXXXX","eventTitle":"Neon Nights 2024","ticketTierName":"Early Bird","quantity":2,"totalAmount":405000,"status":"PENDING","createdAt":"2026-09-14T10:00:00","expiredAt":"2026-09-14T10:15:00"}]}
```

```bash
curl -b cookies.txt localhost:8082/api/user/profile
curl -b cookies.txt -X PUT localhost:8082/api/user/profile/save -H "Content-Type: application/json" -d '{"name":"John Doe","phone":"08123456789","nik":"3201234567890123"}'
curl -b cookies.txt -X PUT localhost:8082/api/account/change-password -H "Content-Type: application/json" -d '{"oldPassword":"123456","newPassword":"newPass123"}'
curl -b cookies.txt -X POST localhost:8082/api/user/avatar -F "file=@avatar.jpg"
curl -b cookies.txt -X POST localhost:8082/api/user/logout
curl -b cookies.txt localhost:8082/api/transactions/history
```

---

## 16. Admin Module (perlu login **ADMIN** — `hasRole("ADMIN")`, 20 endpoint: 13 merge 4836263 + 7 baru)

> **Auth:** Semua endpoint di bawah butuh cookie/Bearer dengan role `ADMIN` (`SecurityConfig.java:76` `/api/admin/**` + alias `/admin/**` → `hasRole("ADMIN")` dual alias rev.14). CUSTOMER → `403 Forbidden`. Ambil `adminId` dari `authentication.getName()`. Kanonikal `/api/admin/**` (alias lama `/admin/**` tetap didukung).

### Dashboard — `AdminDashboardController`

**GET `/api/admin/dashboard/metrics`** (alias `/admin/dashboard/metrics`) → `AdminDashboardMetricsResponse`:
```json
{"msg":"Berhasil mengambil metrik admin","status":200,"data":{"totalPlatformRevenue":300000.0,"totalEvents":7,"activeEvents":7,"totalUsers":39,"totalTicketsSold":0}}
```

**GET `/api/admin/dashboard/recent-events`** → `List<Map>` (event terbaru platform).

**GET `/api/admin/dashboard/recent-transactions`** → `List<TransactionHistoryResponse>` (sama shape dengan `/transactions/history`).

### Users — `AdminUserController`

**GET `/api/admin/users?role=CUSTOMER`** → `List<AdminUserListItemResponse>` (`role` opsional: CUSTOMER/ORGANIZER/ADMIN):
```json
{"userId":"uuid","name":"Budi Santoso","email":"budi@example.com","username":"budi_b78d","phone":"081234567890","nik":"3171012345670001","role":"CUSTOMER","authStatus":"INACTIVE","createdAt":"2026-09-02T22:41:40"}
```

**GET `/api/admin/users/{id}`** → `AdminUserListItemResponse` (satu user).

**PATCH `/api/admin/users/{id}/status`** — body `{"status":"ACTIVE"}` (ACTIVE/INACTIVE/SUSPENDED, `@NotBlank`) → `{"msg":"Status pengguna berhasil diperbarui","status":200,"data":null}`.

**PATCH `/api/admin/users/{id}/suspend`** — no body → langsung SUSPENDED + audit.

### Settings — `AdminSettingsController`

**GET `/api/admin/settings/general`** → `Map<String,String>` (nilai dari tabel `settings`; kosong `{}` jika belum di-seed).

**PUT `/api/admin/settings/general`** — body `AdminSettingsRequest`:
```json
{"appName":"Eventday","contactEmail":"admin@eventday.local","adminFee":5000,"orderExpiryMinutes":15}
```
→ `{"msg":"Pengaturan sistem berhasil diperbarui","status":200,"data":null}` + audit.

**GET `/api/admin/audit-logs?page=0&size=20`** → `Page<AuditLog>` (audit trail: REGISTER/LOGIN/UPDATE_PROFILE/CHANGE_PASSWORD/SUSPEND/dll).

**GET `/api/admin/audit-logs/export`** → `List<AuditLogExportResponse>` (BARU, JSON untuk unduh di frontend).

**GET `/api/admin/audit-logs/export/csv`** → string CSV `auditId,actorId,actorName,action,detail,createdAt` dibungkus `ApiResponse.data` (BARU; bukan file download `text/csv`).

**POST `/api/admin/settings/upload-logo`** (multipart `file`) → validasi PNG/JPG/JPEG ≤5MB → simpan `uploads/logos/platform-logo-<uuid>.<ext>` → update `PLATFORM_LOGO` di tabel `settings` → `{logoUrl:"/uploads/logos/..."}` (BARU; butuh `upload.logo.dir=uploads/logos` + `spring.servlet.multipart.max-file-size=5MB` di `application.properties`).

### EO Applications — `AdminEoController`

**GET `/api/admin/eo-applications?status=UNVERIFIED`** → `List<AdminEoApplicationResponse>` (`status` opsional: UNVERIFIED/VERIFIED/REJECTED):
```json
{"organizerId":"uuid","userId":"uuid","nameOrganizer":"EO Konser Nusantara","userEmail":"...","userPhone":"...","npwpNumber":"...","bankName":"...","bankAccountNumber":"...","aktaPerusahaan":"...","verificationStatus":"VERIFIED","createdAt":"..."}
```

**GET `/api/admin/eo-applications/{id}`** → `AdminEoApplicationResponse` (satu aplikasi).

**PATCH `/api/admin/eo-applications/{id}/status`** — body `AdminEoStatusRequest`:
```json
{"status":"VERIFIED","rejectionReason":null}
```
`status` `VERIFIED`/`REJECTED` (`@NotBlank`, di-uppercase; nilai lain diterima apa adanya); `rejectionReason` **opsional** (hanya ditempel ke audit log bila ada — tidak divalidasi wajib) → `{"msg":"Status verifikasi EO berhasil diperbarui","status":200,"data":null}`. `VERIFIED` otomatis set `users.role=ORGANIZER`.

**GET `/api/admin/eo-applications/{id}/documents/company-deed`** → `{"documentUrl":"..."}` (link akta perusahaan).

### Payouts — `AdminPayoutController` (committed PR #24, sharing tabel `refund_requests`)

**GET `/api/admin/payouts?status=`** → `List<PayoutResponse>` (`status` opsional; tanpa filter kembalikan semua — termasuk refund customer!).

**GET `/api/admin/payouts/{id}`** → `PayoutDetailResponse` (payoutId/organizerId/nameOrganizer/amount/bankName/accountNumber/accountHolder/status/rejectionReason/adminNote/reconciliationDocumentUrl/createdAt/updatedAt).

**PATCH `/api/admin/payouts/{id}/status`** — body `{"status":"APPROVED","adminNote":"..."}` → set status + `adminNote`, APPROVED otomatis `processedAt=now` + audit `UPDATE_PAYOUT_STATUS`.

**GET `/api/admin/payouts/{id}/documents/reconciliation`** → detail + `reconciliationDocumentUrl` (null sampai diisi manual di DB).

```bash
# Semua admin endpoint butuh cookie admin (login role ADMIN):
curl -b admin-cookies.txt localhost:8082/api/admin/dashboard/metrics
curl -b admin-cookies.txt "localhost:8082/api/admin/users?role=CUSTOMER"
curl -b admin-cookies.txt -X PATCH localhost:8082/api/admin/users/<uuid>/status -H "Content-Type: application/json" -d '{"status":"ACTIVE"}'
curl -b admin-cookies.txt -X PATCH localhost:8082/api/admin/users/<uuid>/suspend
curl -b admin-cookies.txt localhost:8082/api/admin/settings/general
curl -b admin-cookies.txt -X PUT localhost:8082/api/admin/settings/general -H "Content-Type: application/json" -d '{"appName":"Eventday","contactEmail":"a@b.c","adminFee":5000,"orderExpiryMinutes":15}'
curl -b admin-cookies.txt "localhost:8082/api/admin/audit-logs?page=0&size=20"
curl -b admin-cookies.txt localhost:8082/api/admin/audit-logs/export
curl -b admin-cookies.txt localhost:8082/api/admin/audit-logs/export/csv
curl -b admin-cookies.txt -X POST localhost:8082/api/admin/settings/upload-logo -F "file=@logo.png"
curl -b admin-cookies.txt "localhost:8082/api/admin/eo-applications?status=UNVERIFIED"
curl -b admin-cookies.txt -X PATCH localhost:8082/api/admin/eo-applications/<uuid>/status -H "Content-Type: application/json" -d '{"status":"VERIFIED"}'
curl -b admin-cookies.txt localhost:8082/api/admin/eo-applications/<uuid>/documents/company-deed
curl -b admin-cookies.txt "localhost:8082/api/admin/payouts?status=PENDING"
curl -b admin-cookies.txt localhost:8082/api/admin/payouts/<uuid>
curl -b admin-cookies.txt -X PATCH localhost:8082/api/admin/payouts/<uuid>/status -H "Content-Type: application/json" -d '{"status":"APPROVED","adminNote":"Cairkan"}'
curl -b admin-cookies.txt localhost:8082/api/admin/payouts/<uuid>/documents/reconciliation
```

---

## 17. Endpoint Reference — Spesifikasi Lengkap per Endpoint (rev.12, verified dari kode)

Konvensi: semua response dibungkus `ApiResponse {msg: string, status: int, data: any|null}` (`ApiResponse.java`, `data` selalu ada via `@JsonInclude ALWAYS`).
Auth: `Public` = tanpa token; `Bearer` = `Authorization: Bearer <jwt>` **atau** `Cookie: access_token` (keduanya didukung `JwtAuthenticationFilter.java:36`); `ADMIN` = Bearer + `role=ADMIN` (`SecurityConfig.java:73` `/admin/**` → `hasRole("ADMIN")`).
Dari browser wajib `credentials: 'include'` (axios: `withCredentials: true`) agar cookie terkirim.
Error mapping (`GlobalExceptionHandler.java`): validasi `@Valid` → `400 {field: pesan}`; `RuntimeException` (termasuk `IllegalArgumentException`/`IllegalStateException`) → `400` berisi pesan service; `Exception` lain (mis. path tak dikenal → `NoResourceFoundException`) → `500 "Internal server error: ..."`; tanpa token → `401`; role salah → `403`.
Legenda tipe: `string` | `int` | `number` (decimal) | `bool` | `uuid` | `datetime` (ISO `YYYY-MM-DDTHH:mm:ss`) | `date` (`YYYY-MM-DD`).

### 17.1 Auth — `AuthController` (`/api/auth`, semua Public)

Header: `Content-Type: application/json`. Login/Google sukses kirim `Set-Cookie: access_token=<jwt>; Path=/; Max-Age=86400; Secure; HttpOnly; SameSite=None; Partitioned` — `data.token` selalu `null` (`@JsonIgnore`).

**POST `/api/auth/register`** → `201`
Body (`RegisterRequest.java`, `@Valid`):

| Field | Tipe | Req | Validasi |
|---|---|---|---|
| `name` | string | Ya | `@NotBlank`, `@Size max 100` |
| `email` | string | Ya | `@NotBlank`, `@Email`, unique |
| `username` | string | Ya | `@NotBlank`, `@Size 3-20`, `@Pattern ^[a-zA-Z0-9_]+$`, unique |
| `phone` | string | Tidak | `@Size max 15` |
| `password` | string | Ya | `@NotBlank`, `@Size min 6` (di-BCrypt) |
| `nik` | string | Tidak | `@Pattern ^\d{16}$` — kirim `null`/omit bila tak ada (string kosong gagal validasi) |
| `role` | string | Tidak | **DIABAIKAN — selalu `CUSTOMER`** (anti-eskalasi rev.9) |

Sukses `201`: `{"msg":"Registrasi berhasil! OTP telah dikirim ke email Anda.","status":201,"data":{"message":"...","userId":"<uuid>","name":"...","email":"...","username":"...","role":"CUSTOMER","token":null,"expiresIn":null}}`
Error `400`: `{"msg":"Email sudah terdaftar!","status":400,"data":null}` (sama untuk `Username/NIK sudah terdaftar!`) atau `{"msg":"username: Username 3-20 karakter, ...","status":400,"data":null}`.

**POST `/api/auth/verify-otp`** → `200`
Body (`VerifyOtpRequest.java`, `@Valid`): `email` (Ya, `@NotBlank @Email`), `otpCode` (Ya, `@NotBlank`).
Sukses `200`: `{"msg":"OTP terverifikasi! Akun aktif, silakan login.","status":200,"data":null}`
Error `400`: `{"msg":"Kode OTP tidak valid!","status":400,"data":null}` / `{"msg":"Kode OTP sudah expired!","status":400,"data":null}` (OTP 6 digit, exp 5 mnt).

**POST `/api/auth/resend-otp`** → `200`
Body (`ResendOtpRequest.java`, `@Valid`): `email` (Ya, `@NotBlank @Email`).
Sukses `200`: `{"msg":"OTP baru berhasil dikirim ke email Anda!","status":200,"data":null}` (OTP lama dihapus dulu).

**POST `/api/auth/login`** → `200` (tanpa `@Valid`)
Body (`LoginRequest.java` — semua opsional di DTO, tapi minimal satu identifier + password):

| Field | Tipe | Req | Catatan |
|---|---|---|---|
| `email` | string | Salah satu | `getIdentifier()`: `identifier` → `email` → `username`; deteksi `contains("@")` |
| `username` | string | Salah satu | fallback cross-check email↔username |
| `identifier` | string | Salah satu | prioritas tertinggi |
| `password` | string | Ya | `passwordEncoder.matches` vs `auth.password` |

Sukses `200` + `Set-Cookie`: `{"msg":"Login berhasil!","status":200,"data":{"message":"Login berhasil!","userId":"<uuid>","name":"...","email":"...","username":"...","role":"CUSTOMER","expiresIn":86400}}`
Error `400`: `Email atau username harus diisi!` / `Email atau password salah!` / `Akun belum aktif! Silakan verifikasi OTP terlebih dahulu.`

**POST `/api/auth/google`** → `200` (login) / `201` (user baru)
Body (`GoogleLoginRequest.java`, `@Valid`): `idToken` (Ya, `@NotBlank` — Google ID Token dari GIS).
Sukses + `Set-Cookie`: `{"msg":"Login via Google berhasil!","status":200,"data":{...,"role":"CUSTOMER","expiresIn":86400}}` (registrasi: `201 "Registrasi via Google berhasil!"`).
Error `400`: `Token Google tidak valid: ...` / `Email Google belum terverifikasi` / `Token Google aud tidak sesuai dengan google.client-id` / `Token Google sudah expired`. GIS wajib pakai client ID persis `875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com`.

**POST `/api/auth/reset-password`** → `200` (2 tahap, satu endpoint)
Body (`ResetPasswordRequest.java`, `@Valid`): `email` (Ya, `@NotBlank @Email`); `code` (Tidak, alias JSON `token`/`otp` — efektif via `getEffectiveCode()` trim); `token` (Tidak, alias); `newPassword` (Tidak, alias JSON `password`/`new_password`).
Tahap 1 (tanpa `code`): `{"msg":"Kode reset password berhasil dikirim ke email Anda!","status":200,"data":null}` (simpan `auth.reset_token` + `reset_expired_at` +15 mnt).
Tahap 2 (dengan `code`+`newPassword`): `{"msg":"Password berhasil direset! Silakan login dengan password baru.","status":200,"data":null}`
Error `400`: `Kode reset password tidak valid/expired` / `Password baru minimal 6 karakter`.

### 17.2 Events — `EventController` (`/api/events`, semua Publik)

**GET `/api/events`** → `200`
Query (semua opsional): `category` (string, `Semua`/`ALL` = tanpa filter), `search` (string, judul/venue), `location` (string), `page` (int, default `0`), `size` (int, default `12`), `sort` (string: `latest` default, `price_asc`, `price_desc`, `date_asc`).
Sukses `200`: `{"msg":"Berhasil mengambil daftar event","status":200,"data":{"content":[{"id":"<uuid>","title":"...","category":"MUSIC_FESTIVAL","categoryLabel":"Musik","date":"2026-12-15T19:00:00","dateDisplay":"15 Dec 2026","time":"19:00","location":"...","price":200000,"priceDisplay":"Rp 200.000","image":"https://...","status":"AVAILABLE","isFeatured":true}],"page":0,"size":12,"totalElements":42,"totalPages":4}}` (`price` = tier termurah; `AVAILABLE` = alias `PUBLISHED`).

**GET `/api/events/featured`** → `200` (tanpa param, max 3, shape sama).

**POST `/api/events/create`** → `201` (⚠️ MOCK STUB, PR #25 — **Publik!** `SecurityConfig` permit `/api/events/**` untuk semua method)
Body: Map bebas (diabaikan server). Sukses: HTTP `201`, body `{"msg":"Event berhasil dibuat","status":200,"data":{"eventId":"<random-uuid>","status":"DRAFT"}}` — **tak ada yang disimpan ke DB**. ⚠️ Catatan keamanan: endpoint publik tanpa auth; aman sementara karena no-op, tapi pertimbangkan `authenticated()` + role EO sebelum diisi logic real. DTO `CreateEventRequest.java` (title/description/category/location/venueName/eventDate/bannerUrl/facilities[]/ticketTiers[]) sudah dibuat tapi **belum dipakai** controller (masih terima Map).

**GET `/api/events/{id}`** → `200` / `404`
Path: `id` (uuid event). Publik.
Sukses `200` (`EventDetailResponse`): `{id,title,category,categoryLabel,date,dateDisplay,location,description,image,status,statusLabel,facilities,lineup:[{name,image}],tickets:[{id,name,label,price,priceDisplay,quota,remaining,saleStart,saleEnd}]}` — ⚠️ `facilities` masih **string mentah** (`"Parkir Luas, Food Court"`), frontend `.split(', ')`.
Error `404`: `{"msg":"Event tidak ditemukan","status":404,"data":null}`.

### 17.3 Home & Search — `HomeSearchController` (semua Publik, kosong → `[]`)

| Method + Path | Query | Sukses `200` |
|---|---|---|
| GET `/api/home/hero-banner` | — | `data: [{id,title,bannerUrl,eventDate,targetUrl}]` (max 5 featured) |
| GET `/api/home/event-card` | `page` (default 0), `size` (default 12) | `data: {content:[{id,title,posterUrl,location,category,categoryLabel,startDate,dateDisplay,lowestPrice,priceDisplay}],page,size,totalElements,totalPages}` |
| GET `/api/home/locations` | — | `data: ["Jakarta",...]` (DISTINCT venueName PUBLISHED) |
| GET `/api/search/results` | `keyword`?, `category`? (`Semua`/`ALL`=null), `location`?, `date`? (`YYYY-MM-DD`), `page`?, `size`?, `sort`? (`latest` default, `date_asc`, `price_asc/desc`) | shape sama dengan event-card |
| GET `/api/search/locations` | — | `data: [string]` |
| GET `/api/search/categories` | — | `data: [string]` (DISTINCT category PUBLISHED) |

### 17.4 Checkout/Order — `CheckoutController` (`/api`, semua Bearer)

**POST `/api/checkout/initiate`** → `200`
Body (`InitiateCheckoutRequest.java`, tanpa `@Valid`): `tierId` (uuid, Ya), `quantity` (int, Ya).
Sukses `200`: `data` = entity `Order` (`{orderId,quantity,adminFee:5000,totalAmount,status:"PENDING",expiredAt:+15mnt,...}`, relasi lazy di-ignore). Kuota dicek tapi **tidak dikurangi**.
Error `400`: `Ticket tier tidak ditemukan` / `Kuota tiket tidak mencukupi` / `User tidak ditemukan`. `401` tanpa token.

**POST `/api/checkout/attendees`** → `200`
Body (`AttendeeRequest.java`, tanpa `@Valid`): `orderId` (string UUID, Ya), `attendees` (array, Ya — kosong → `[]`, **tanpa cek orderId ada di DB**):

| Field item | Tipe | Req |
|---|---|---|
| `fullName` | string | Ya (implisit — kolom NOT NULL di DB) |
| `email` | string | Ya |
| `phoneNumber` | string | Ya |
| `identityNumber` | string | Ya (NIK/No. KTP) |

Sukses `200`: `data: [{id,orderId,fullName,email,phoneNumber,identityNumber}]` (tersimpan `order_attendees`).

**POST `/api/checkout/calculation`** → `200` (hitung saja, TIDAK simpan order)
Body (`CalculationRequest.java`): `tierId` (uuid, Ya), `quantity` (int, Ya), `discountAmount` (number, Tidak, default 0).
Sukses `200`: `data: {subtotal,adminFee,tax (subtotal×10%),discount,totalAmount (=subtotal+adminFee+tax−discount)}`.
Error `400`: `Ticket tier tidak ditemukan`.

**POST `/api/checkout/process`** → `200`
Body (`ProcessCheckoutRequest.java`): `orderId` (uuid, Ya).
Sukses `200`: `data` = entity `Order` dengan `status: "WAITING_PAYMENT"`.
Error `400`: `Order tidak ditemukan` / `Order tidak dapat diproses pada status <STATUS>` (tolak `PAID`/`EXPIRED`/`CANCELLED`, terima `PENDING`/`WAITING_PAYMENT`).

**GET `/api/orders/status?orderId=<uuid>`** → `200`: `data: {orderId:"<uuid>",status:"..."}`. Error `400`: `Order tidak ditemukan`.
**GET `/api/checkout/summary?orderId=<uuid>`** → `200` (`CheckoutSummaryResponse`): `{orderId,orderNumber:"ORD-XXXXXXXX",eventTitle,ticketTierName,quantity,pricePerTicket,subtotal,adminFee,discountAmount:0,totalAmount,expiredAt}`. Error `400`: `Order tidak ditemukan`.
**GET `/api/orders/{orderId}/total-amount`** → `200`: `data: {totalAmount}` (dari summary).
**GET `/api/orders/{orderId}/expired-time`** → `200`: `data: {expiredAt}` (dari summary).

### 17.5 Payment — `PaymentController` (`/api/payments`; charge = Bearer, notification = **Public** sejak PR #24)

**POST `/api/payments/charge`** → `200` (REAL Midtrans Snap sandbox)
Body (langsung `Map`, **tanpa DTO/validasi** — semua wajib, hilang satu → `400` NPE):

| Field | Tipe | Req |
|---|---|---|
| `orderId` | string (UUID) | Ya |
| `grossAmount` | number | Ya |
| `customerName` | string | Ya |
| `customerEmail` | string | Ya |

Sukses `200`: `{"msg":"Snap Token berhasil dibuat","status":200,"data":{"snapToken":"...","redirectUrl":"https://app.sandbox.midtrans.com/snap/v2/vtweb/..."}}` (via `MidtransService.createSnapTransaction`, Basic Auth server-key).
Error `400`: `Gagal menghubungkan transaksi ke Midtrans Snap API`.
> 🗑️ DEPRECATED/DIHAPUS: `GET /payments/methods`, `GET /payments/methods/virtual-account` (tak ada di kode). DTO `PaymentChargeRequest`/`PaymentChargeResponse` stale (mock VA lama) — charge pakai Map. `PaymentService.processPaymentCharge()` dead code.

**POST `/api/payments/midtrans-notification`** → `200` (Public sejak PR #24 — `SecurityConfig` permitAll karena server Midtrans tak punya JWT; ⚠️ log-only)
Body (Map bebas): `order_id` (string), `transaction_status` (string: `settlement`/`capture`/`cancel`/`expire`/…).
Sukses `200` **tanpa token**: `{"msg":"Notifikasi Midtrans berhasil diproses","status":200,"data":"OK"}` — **status order TIDAK diupdate** (cabang `settlement`/`cancel` masih komentar). Verified live.

### 17.6 Ticket — `TicketController` (`/api/tickets` TANPA `/v1`, semua Bearer)

**GET `/api/tickets/user/{email}`** dan alias **GET `/api/tickets/my-tickets?userEmail=`** → `200`
Path/query: `email` (path, opsional bila query ada) / `userEmail` (query, fallback). Sukses `200`: `data: [TicketItem...]` via `findByOrderCustomerEmailOrderByCreatedAtDesc` — kosong `[]` sampai `generateTicket()` dipanggil (saat ini tak ada alur yang memanggil).
**GET `/api/tickets/issued-detail?ticketCode=<uuid>`** → `200` / `400`
Query: `ticketCode` (Ya — UUID `ticketItemId`). Sukses `data` (`TicketDetailResponse`): `{ticketId,ticketCode,orderId,eventTitle,eventDate,venueName,categoryName,attendeeName,attendeeEmail,attendeeIdentityNumber,status,issuedAt}`. Error `400`: `Format kode tiket tidak valid!` / `Tiket tidak ditemukan!` (controller catch → `badRequest`).
**POST `/api/tickets/scan`** → `200` / `400`
Body (Map): `ticketCode` (string UUID, Ya). Sukses `200`: `data: {status:"TIKET_VALID"|"TIKET_SUDAH_DIPAKAI",message:"Proses scan selesai"}` (`TIKET_VALID` → set `CHECKED_IN`+`checkInAt`). Error `400` format tak valid.
> DTO `TicketScanRequest/Response`, `TicketSummaryResponse`, `TicketItemResponse` BELUM DIPAKAI controller manapun.

### 17.7 User/Profile — `UserController` (`/api`, semua Bearer, userId dari `authentication.getName()`)

**GET `/api/user/profile`** → `200`: `data: {userId,name,email,username,phone,nik,role,avatarUrl}`.
**PUT `/api/user/profile/save`** → `200` (`UpdateProfileRequest.java`, `@Valid`): `name` (Ya, `@NotBlank @Size max 100`), `phone` (Tidak, `@Size max 15`), `nik` (Tidak, `@Pattern ^\d{16}$`). Email/username/role tak bisa diganti. Sukses: profil terbaru. Error `400`: validasi / `NIK sudah digunakan akun lain!`.
**PUT `/api/account/change-password`** → `200` (`ChangePasswordRequest.java`, `@Valid`): `oldPassword` (Ya, `@NotBlank`), `newPassword` (Ya, `@NotBlank @Size min 6`, wajib ≠ old). Sukses `data: null` + audit + email notif. Error `400`: `Password lama salah!` / `Password baru tidak boleh sama dengan password lama!`.
**POST `/api/user/avatar`** → `200` (✅ REAL sejak PR #24 — file tersimpan & diserve `WebConfig`): multipart `file` (form-data). Validasi: kosong → `400 File avatar tidak boleh kosong!`; `Content-Type` bukan `image/*` → `400 File harus berupa gambar (PNG/JPG/JPEG)`; >5MB → `400 Ukuran file maksimal 5MB`. Sukses: `data: "/uploads/avatars/<uuid>_<nama-aman>"` — bisa langsung dipakai `<img src>`. Gagal tulis → `400 Gagal menyimpan avatar: ...`.
**POST `/api/user/logout`** → `200`: null-kan `aksesToken/expiredToken` + `Set-Cookie access_token` maxAge 0. Sukses `data: null`. Status user TIDAK diubah (bisa login lagi).
**GET `/api/transactions/history`** → `200`: `data: [{orderId,orderNumber:"ORD-XXXXXXXX",eventTitle,ticketTierName,quantity,totalAmount,status,createdAt,expiredAt}]`.

### 17.8 Refund Customer — `RefundController` (**base `/api`**), semua Bearer

**POST `/api/tickets/refund/request`** dan alias **POST `/api/refund/submit`** → `200` (REAL)
Body (`RefundRequest.java` DTO, tanpa `@Valid` — semua string/uuid polos):

| Field | Tipe | Req | Dipetakan ke |
|---|---|---|---|
| `orderId` | uuid | Ya | `RefundRequestEntity.orderId` |
| `reason` | string | Ya | `reason` |
| `bankCode` | string | Ya | `bankName` |
| `accountNumber` | string | Ya | `bankAccountNumber` |
| `accountHolderName` | string | Ya | `accountHolder` |

Server isi otomatis (PR #24): `customerId` (dari token), `amount` = `order.totalAmount − adminFee` bila order ada (fallback `290000.00`), `status: "PENDING"`, `organizerId` = organizer milik event bila ada, **fallback = `customerId`** (selalu terisi, tak pernah null lagi).
Sukses `200`: `data` (`RefundDetailResponse`): `{refundId,orderId,amount,reason,bankName,accountNumber,accountHolderName,status,proofUrl:null,createdAt}`.
**GET `/api/refund/banks`** → `200` (⚠️ MOCK, 8 bank): `data: [{bankCode,bankName,logoUrl,active}]` = BCA/MANDIRI/BNI/BRI/CIMB/PERMATA/BSI/DANAMON hardcoded (+`logoUrl:/assets/banks/*.png` fiktif — file tak ada di repo).
**GET `/api/refund/order-summary?orderId=<uuid>`** → `200` (REAL bila order ada, fallback mock bila tidak): `data: {orderId,orderNumber:"ORD-XXXXXXXX",eventTitle,ticketTierName,ticketQuantity,grossAmount,adminFee,refundableAmount (=gross−adminFee, floor 0),status,expiredAt,createdAt}`. Fallback: `{orderId,ticketQuantity:2,grossAmount:300000.00,adminFee:5000.00,refundableAmount:295000.00}`.
**GET `/api/refund/refund-detail/info?refundId=<uuid>`** → `200` (REAL via `findById`): `data: RefundDetailResponse`. Error `400`: `Refund tidak ditemukan!`.
**GET `/api/refund/refund-detail/download-proof?refundId=<uuid>`** → `200` (endpoint REAL, data PARTIAL): `data: {proofUrl:""}` — selalu string kosong karena entity tak punya kolom proof.
**GET `/api/tickets/refund/refund-history`** → `200` (REAL via `findByCustomerId`, customerId dari token, tanpa param): `data: [RefundDetailResponse]`.

### 17.9 Legal — `LegalController` (✅ FIXED PR #25 — Publik 4 path, konten masih hardcoded)

| Method + Path | Auth | Sukses `200` (verified live tanpa token) |
|---|---|---|
| GET `/terms-conditions` + alias `/api/terms-conditions` | Public | `data: {title,slug:"terms-conditions",version:"1.0",content (HTML `<h1>/<h2>` 5 pasal),sections:[{heading,body}×5],updated_at:"2026-09-16"}` |
| GET `/privacy-policy` + alias `/api/privacy-policy` | Public | `data: {title,slug:"privacy-policy",version:"1.0",content (HTML 5 pasal UU PDP),sections:[{heading,body}×5],updated_at:"2026-09-16"}` |

Fix: `@GetMapping({"/terms-conditions","/api/terms-conditions"})` + `SecurityConfig:70` permit keempat path. `LegalService` tetap hardcoded (MOCK content, bukan DB) — render `content` sebagai HTML atau map `sections` untuk tampilan native.

### 17.10 Organizer — `OrganizerController` (**base `/api/organizer`** sejak PR #25 — ⚠️ BREAKING, path lama `/api/organizer/*` mati; 29 endpoint, Bearer) — HYBRID DB + mock fallback (PR #21) + 9 stub baru

`OrganizerService.java` (638 baris) DB-backed: tiap method baca `organizerRepository.findByUserUserId(currentUserId)` dulu; bila ada konteks organizer → data REAL; bila tidak → fallback statis dengan flag **`"mock": true`** (kasus real ditandai `"real": true`). Frontend bisa branching dari flag ini. Semua body Map tanpa DTO/validasi (key `snake_case`!). Upload file benar-benar tersimpan (`uploads/avatars|organizer-docs/...`, butuh `upload.dir`), kecuali deed URL belum ditulis ke `organizer.akta_perusahaan`.

**Registrasi & status**
**POST `/api/organizer/register`** → `200`. Body (Map): `name`|`organizer_name` (default nama user), `npwp_number`?, `bank_name`?, `bank_account_number`?. Baru → simpan `Organizer(VERIFICATION_STATUS=PENDING)` + `data: {organizer_id,organizer_name,verification_status:"PENDING",user_id}`. Sudah terdaftar → `{organizer_id,organizer_name,verification_status,message:"Organizer sudah terdaftar"}`. Tanpa login → mock `{organizer_name,verification_status:"PENDING",mock:true,note:"Login dulu..."}`.
**POST `/api/organizer/documents/upload`** → `200`. Multipart `file` + query/form `type` (default `"KTP"`). File disimpan `uploads/organizer-docs/` → `data: {document_type,file_name,file_size,document_url,uploaded_at}`.
**GET `/api/organizer/status`** → `200`. REAL: `{organizer_id,organizer_name,verification_status,created_at}`; fallback mock `{organizer_name:"PT Penyelenggara Event",verification_status:"PENDING",mock:true}`.
**GET `/api/organizer/dashboard`** → `200`. REAL (`real:true`): `{total_revenue (sum order PAID/WAITING_PAYMENT milik EO),active_events,total_events,tickets_sold,organizer_id}`; fallback mock `{total_revenue:42500000,active_events:2,tickets_sold:1248,mock:true}`.

**Profil & legalitas**
**GET `/api/organizer/profile`** → `200`. REAL: `{organizer_id,user_id,name,pic_name,email,phone,npwp,bank_name,bank_account_number,verification_status,avatar_url (dicebear seed organizerId),created_at}`; user non-organizer → `{...,verification_status:"UNREGISTERED",note:"Belum terdaftar...POST /organizer/register dulu"}`; tanpa login → mock Harmoni (`mock:true`).
**PUT `/api/organizer/profile`** → `200`. Body key: `name`|`organizer_name`, `npwp`|`npwp_number`, `bank_name`, `bank_account_number`, `pic_name` (→ `users.name`), `phone` (→ `users.phone`). Persist + echo payload + `{organizer_id,updated_at}`; tanpa konteks → echo saja.
**POST `/api/organizer/profile/avatar`** → `200`. Multipart `file` → simpan `uploads/avatars/` → `{avatar_url,file_name,file_size}`.
**POST `/api/organizer/profile/upload-portfolio`** → `200`. Multipart `file` → `uploads/organizer-docs/portfolio/` → `{portfolio_url,file_name,file_size}`.
**POST `/api/organizer/profile/upload-deed`** → `200`. Multipart `file` → `uploads/organizer-docs/deeds/` → `{company_deed_url,...}` (⚠️ PARTIAL: file tersimpan, tapi URL **belum** ditulis ke `organizer.akta_perusahaan`).
**GET `/api/organizer/profile/document`** → `200`. REAL: `{organizer_id,akta_perusahaan,has_akta,portfolio_name,deed_name,ktp_name}`; fallback mock (`mock:true`).

**Auth EO**
**POST `/api/organizer/auth/change-password`** → `200`. Body: `oldPassword` (Ya), `newPassword`|`new_password` (Ya). Login → verifikasi BCrypt + update `auth.password`; salah → `400 Password lama salah!`; tanpa login → no-op tetap `200` (`data: null`).
**POST `/api/organizer/auth/logout`** → `200` (⚠️ STUB no-op, hanya log). Frontend harus panggil `POST /api/user/logout` untuk logout sungguhan.

**Refund milik EO** (sumber `refund_requests` by `organizerId`; kosong → fallback PENDING global max 20; tanpa login → mock `REF-001`)
**GET `/api/organizer/refunds`** → `200`: `[{refund_id,order_id,customer_id,amount,reason,bank_name,account_number,account_holder,status,created_at}]`.
**GET `/api/organizer/refunds/detail?id=<uuid>`** → `200`: `{refund_id,order_id,amount,reason,bank_name,account_number,account_holder,status,rejection_reason,admin_note,created_at}` (+mock bila id bukan UUID/tak ada).
**PATCH `/api/organizer/refunds/{id}/status`** → `200`. Body key: `status`|`newStatus` (default APPROVED), `adminNote`|`admin_note`?, `rejection_reason`?. APPROVED/REJECTED → `processedAt=now`. Sukses: `{refund_id,status,admin_note,processed_at}`; fallback mock `{refund_id,status,notes,mock:true}`.

**Payout & saldo EO**
**GET `/api/organizer/bank-accounts`** → `200`. REAL dari entity: `[{id (organizerId),bank_name,account_number,account_holder,is_primary:true}]`; fallback mock BCA (`mock:true`).
**GET `/api/organizer/events/{id}/payout-balance`** → `200`. Path `id` (Long eventId). REAL: `{event_id,organizer_id,total_sales,withdrawable_balance (=90%),pending_payout (=10%),currency:"IDR"}`; fallback mock 50jt/45jt/5jt (`mock:true`).
**GET `/api/organizer/payouts`** → `200`. REAL (`findByOrganizerId`): `[{id,payout_id,amount,status,bank_name,requested_at,processed_at}]`; kosong/tanpa login → mock `{id:101,amount:25000000,status:"SUCCESS",requested_at:"2026-09-10",mock:true}`.
**POST `/api/organizer/payouts`** → `200`. Body: `amount` (Ya, wajib ada untuk REAL), `description`?, `bank_name`? (default BCA), `account_number`?, `account_holder`?. Login + amount → simpan `RefundRequestEntity{organizerId,customerId,PENDING}` → `{payout_id,amount,status:"PENDING_APPROVAL",organizer_id}`; selain itu mock `{payout_id:102,amount,status:"PENDING_APPROVAL",mock:true}`.
**GET `/api/organizer/payouts/detail?id=`** → `200` (⚠️ praktis selalu mock: param `Long`, UUID tak bisa di-bind → fallback `{id,amount:25000000,status:"SUCCESS",bank_name,account_number,transfer_proof_url,mock:true}`).

**Event milik EO — 6 stub BARU (PR #25, semua ⚠️ MOCK, Bearer)**
**GET `/api/organizer/events`** → `200 data: []`. **POST `/api/organizer/events`** → `200`: body Map bebas (diabaikan) → `{eventId:"<random-uuid>",status:"DRAFT"}`. **PUT `/api/organizer/events/update`** → `200`: echo body. **POST `/api/organizer/events/publish?id=<long>`** → `200`: `{eventId:<id>,status:"PUBLISHED"}` (tak update DB!). **GET `/api/organizer/events/draft`** → `200 data: []`. **GET `/api/organizer/events/{id}/sales-summary`** → `200`: `{eventId,ticketsSold:0,totalRevenue:0}`.

**Dashboard EO — 3 stub BARU (PR #25, semua ⚠️ MOCK, Bearer; bedakan dengan `GET /api/organizer/dashboard` hybrid di atas)**
**GET `/api/organizer/dashboard/metrics`** → `200`: `{totalEvents:0,totalRevenue:0,totalTicketsSold:0}`. **GET `/api/organizer/dashboard/recent-events`** → `200 data: []`. **GET `/api/organizer/dashboard/recent-transactions`** → `200 data: []`.

### 17.11 Admin — `/admin/**` (21 endpoint, semua ADMIN, `adminId` dari `authentication.getName()`)

CUSTOMER/ORGANIZER → `403 {"msg":"Forbidden: akses ditolak","status":403}`. Error umum: UUID path tak valid → `400`; id tak ada → `400 Pengajuan/Aplikasi ... tidak ditemukan!`.

**Dashboard** (`AdminDashboardController` `/admin/dashboard`): **GET `/metrics`** → `200 {totalPlatformRevenue,totalEvents,activeEvents,totalUsers,totalTicketsSold}` (revenue = sum semua order, tickets = count `ticket_items`). **GET `/recent-events`** → `200 [Map]` (5 terbaru). **GET `/recent-transactions`** → `200 [TransactionHistoryResponse]` (10 terbaru).

**Users** (`AdminUserController` `/api/admin/users`): **GET `?role=`** → `200 [AdminUserListItemResponse]` (`role` opsional: CUSTOMER/ORGANIZER/ADMIN, tanpa filter = semua). Item: `{userId,name,email,username,phone,nik,role,authStatus,createdAt}`. **GET `/{id}`** → `200` satu item (`400` bila tak ada). **PATCH `/{id}/status`** → `200 data:null` + audit. Body (`AdminUserStatusRequest`, `@Valid`): `status` (Ya, `@NotBlank` — `ACTIVE`/`INACTIVE`/`SUSPENDED`, uppercased). **PATCH `/{id}/suspend`** → `200 data:null` (tanpa body, langsung SUSPENDED + audit).

**Settings & audit** (`AdminSettingsController` `/admin`): **GET `/settings/general`** → `200 Map<String,String>` (isi tabel `settings`, kosong → `{}`). **PUT `/settings/general`** → `200 data:null` + audit. Body (`AdminSettingsRequest`, `@Valid`, semua opsional): `appName`? (string), `contactEmail`? (string), `adminFee`? (int), `orderExpiryMinutes`? (int) — null dilewati. **GET `/audit-logs?page=0&size=20`** → `200 Page<AuditLog>` (`{content,page,size,totalElements,totalPages,...}`). **GET `/audit-logs/export`** → `200 [AuditLogExportResponse]` (`{auditId,actorId,actorName,action,detail,createdAt}`). **GET `/audit-logs/export/csv`** → `200` string CSV satu header `auditId,actorName,...` dalam `data` (bukan `text/csv`). **POST `/settings/upload-logo`** → `200 {logoUrl:"/uploads/logos/platform-logo-<uuid>.<ext>"}` + update `PLATFORM_LOGO` + audit. Multipart `file` (Ya). Validasi: kosong → `400 File logo tidak boleh kosong!`; bukan PNG/JPG/JPEG → `400 Format file tidak valid...`; >5MB → `400 Ukuran file melebihi batas maksimum 5MB!` (butuh `upload.logo.dir` + `spring.servlet.multipart.*` di properties; `settingsValue` VARCHAR(255)).

**EO Applications** (`AdminEoController` `/api/admin/eo-applications`, via `Organizer` entity — TAK ADA tabel terpisah): **GET `?status=`** → `200 [AdminEoApplicationResponse]` (`status` opsional: `UNVERIFIED`/`VERIFIED`/`REJECTED`/`ALL`/kosong = semua). Item: `{organizerId,userId,nameOrganizer,userEmail,userPhone,npwpNumber,bankName,bankAccountNumber,aktaPerusahaan,verificationStatus,createdAt}`. **GET `/{id}`** → `200` satu aplikasi (`400 Aplikasi EO tidak ditemukan!`). **PATCH `/{id}/status`** → `200 data:null` + audit `VERIFY_EO`. Body (`AdminEoStatusRequest`, `@Valid`): `status` (Ya, `@NotBlank` — `VERIFIED`/`REJECTED`, uppercased; nilai lain diterima apa adanya), `rejectionReason` (Tidak — **opsional**, hanya ditempel ke audit log bila ada). `VERIFIED` otomatis set `users.role=ORGANIZER`. **GET `/{id}/documents/company-deed`** → `200 {documentUrl:"..."}` (`""` bila null).

**Payouts** (`AdminPayoutController` `/api/admin/payouts`, sharing tabel `refund_requests` — TAK ADA tabel terpisah): **GET `?status=`** → `200 [PayoutResponse]` (`{payoutId,organizerId,nameOrganizer ("-" bila null),amount,bankName,accountNumber,accountHolder,status,rejectionReason,adminNote,createdAt,updatedAt}`) — ⚠️ tanpa discriminator, refund customer ikut muncul. **GET `/{id}`** → `200 PayoutDetailResponse` (+`userEmail:null,userPhone:null,reconciliationDocumentUrl`). **PATCH `/{id}/status`** → `200 PayoutDetailResponse` terbaru. Body (`UpdatePayoutStatusRequest`, `@Valid`): `status` (Ya, `@NotBlank`), `adminNote` (Tidak); APPROVED → `processedAt=now` + audit `UPDATE_PAYOUT_STATUS`. **GET `/{id}/documents/reconciliation`** → `200` detail (termasuk `reconciliationDocumentUrl`, null sampai diisi manual).

### 17.12 Perubahan BREAKING + file statis (PR #23–25, wajib dibaca frontend)

**Migrasi path (path lama MATI → `500 "Internal server error: No static resource..."`, bukan 404!):**

| Lama (mati) | Baru (aktif) | Modul |
|---|---|---|
| `/organizer/*` (20 endpoint) | `/api/organizer/*` | Organizer |
| `/api/refund/*`, `/api/tickets/refund/*` (7 path) | `/api/refund/*`, `/api/tickets/refund/*` | Refund |
| — (baru) | `/api/terms-conditions`, `/api/privacy-policy` (alias, publik) | Legal |

Aturan ingat: backend **tanpa versioning fallback** — path salah satu karakter → `500` via `GlobalExceptionHandler` (`NoResourceFoundException` bukan `RuntimeException`). Selalu copy-paste path dari §17.

**File statis** (`WebConfig.java`, PR #24): `GET /uploads/**` diserve dari folder lokal `uploads/` (`upload.dir`). URL yang dikembalikan endpoint (`/uploads/avatars/...`, `/uploads/logos/...`, `/uploads/organizer-docs/...`) langsung bisa dipakai `<img src="http://localhost:8082/uploads/avatars/x.png">`. Catatan: URL avatar lama (era mock, file tak pernah disimpan) → `404` saat dibuka.

## ⏳ Modul Lain — SCHEMA ONLY (belum aktif)

| Modul | Rencana Endpoint | Status | HTTP |
|---|---|---|---|
| Organizer events | `POST /api/events/create` (stub), `/api/organizer/events/*` (6 stub) | ⚠️ Stub MOCK — belum persist | `201`/`200` mock |
| Organizer dashboard DB | `/api/organizer/dashboard/metrics|recent-events|recent-transactions` (3 stub) | ⚠️ Stub zeros (beda dari `/dashboard` hybrid) | `200` mock |

> **Sudah AKTIF (bukan schema-only lagi):** User/Profile via PR #11 (+avatar REAL PR #24); **Admin module 20 endpoint** (lihat §16); Refund customer 7 path aktif (§05, base `/api`); Legal 4 path publik (FIXED); Organizer 29 endpoint (18 hybrid + 11 mock, base `/api/organizer`); `POST /api/events/create` stub; `midtrans-notification` publik.
> Catatan: **register EO sisi customer** (`POST /organizer/register`) ada tapi MOCK — admin hanya bisa memverifikasi organizer yang sudah ada di DB.

`RescheduleRequest` **dihapus** — jangan panggil. `EoApplication`/`Payout`/`AdminSettings` entity **TIDAK ADA dan jangan buat** (pakai `Organizer`/`RefundRequestEntity`/`Settings`).

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
*   `orders.status`: `PENDING` / `WAITING_PAYMENT` (Midtrans Snap PR #9) / `PAID` / `EXPIRED` / `CANCELLED`
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
  POST /payments/charge {orderId,grossAmount,customerName,customerEmail} --> Midtrans Snap {snapToken,redirectUrl}
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

**Base URL (wajib lengkap dengan prefix `/api/auth`):**
- Lokal (1 laptop): `http://localhost:8082/api/auth`
- Lintas laptop (ngrok): `https://<id-baru>.ngrok-free.app/api/auth` dari `ngrok http 8082` — ganti tiap restart ngrok. Jangan pakai `9538-...` (expired) atau `127.0.0.1:8000` (Django).
- **Contoh salah → 401:** `BASE = 'https://xxx.ngrok-free.app'` lalu `fetch(BASE + '/register')` → request ke `/register` (tidak ada di `SecurityConfig.java:55`) → `401`. **Benar:** `BASE = 'https://xxx.ngrok-free.app/api/auth'` lalu `fetch(BASE + '/register')` → `POST /api/auth/register` → `201`.

> **🔧 FIX WAJIB frontend — penyebab 401 checkout (verified 2026-09-15):** `authService.js` 6× `fetch` **tanpa** `credentials: 'include'` → `Set-Cookie access_token` tidak pernah tersimpan/terkirim, semua request protected `401` (`user=- auth=none`, log filter: `JWT: TIDAK ADA TOKEN`). `api.js` (`apiFetch`) + `checkoutService.js` sudah benar. Tambahkan `credentials: "include",` di tiap fetch berikut (nomor baris = copy `Downloads/authService.js`):
> - `verify-otp` (`:179`), `resend-otp` (`:219`), `login` (`:284`), `google` (`:316`), `reset-password` ×2 (`:355` forgotPassword, `:412` resetPassword) — contoh: `fetch(\`${API_URL}/login\`, { method: "POST", headers: getHeaders(), credentials: "include", body: ... })`.
> - Setelah edit: login ulang di **Incognito** (`Ctrl+Shift+N`) → cek `Application → Cookies → localhost:8082` ada `access_token` → checkout harus `200`.
> - Kalau event baru tidak muncul padahal backend kirim 7 (`totalElements: 7`): `api.js`/`authService.js` default ke ngrok lama `https://174a-140-213-45-232.ngrok-free.app` (kemungkinan expired) — set `.env` `VITE_API_URL=http://localhost:8082/api` + **restart `npm run dev`**, lalu cek Network → Request URL + `totalElements`.

```js
// helper standar — token via HttpOnly Cookie, JANGAN pakai getApiBase
const BASE_AUTH = 'https://<id-baru>.ngrok-free.app/api/auth'; // ngrok untuk teman beda laptop
// const BASE_AUTH = 'http://localhost:8082/api/auth'; // untuk lokal
// const BASE = 'http://192.168.x.x:8082/api/auth'; // alternatif satu WiFi tanpa ngrok

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
  // untuk events, BASE tanpa /auth: ganti /api/auth → /api
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
const reg = await apiAuth('/register', {name,email,username,password}); // POST /api/auth/register → 201
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

// contoh checkout & payment — perlu login (credentials:include), base /api
export const initiateCheckout = (tierId,quantity) => fetch(`${BASE_API}/checkout/initiate`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({tierId,quantity})}).then(r=>r.json());
export const saveAttendees = (orderId,attendees) => fetch(`${BASE_API}/checkout/attendees`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId,attendees})}).then(r=>r.json()); // attendees:[{fullName,email,phoneNumber,identityNumber}] → order_attendees
export const calcCheckout = (tierId,quantity,discountAmount=0) => fetch(`${BASE_API}/checkout/calculation`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({tierId,quantity,discountAmount})}).then(r=>r.json()); // data:{subtotal,adminFee,tax,discount,totalAmount}
export const processCheckout = (orderId) => fetch(`${BASE_API}/checkout/process`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId})}).then(r=>r.json()); // → WAITING_PAYMENT
export const getOrderStatus = (orderId) => apiGet(`/orders/status?orderId=${orderId}`); // polling → data.status
export const getSummary = (orderId) => apiGet(`/checkout/summary?orderId=${orderId}`);
export const chargeMidtrans = (orderId,grossAmount,customerName,customerEmail) => fetch(`${BASE_API}/payments/charge`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({orderId,grossAmount,customerName,customerEmail})}).then(r=>r.json()); // → {snapToken, redirectUrl}
// ticket (base TANPA /v1! perlu login): GET /api/tickets/user/{email} + alias /my-tickets?userEmail= + GET /issued-detail?ticketCode= + POST /scan {ticketCode}
export const getMyTickets = (email) => apiGet(`/tickets/my-tickets?userEmail=${encodeURIComponent(email)}`); // data[] → TicketItem (PR #12)
export const getTicketDetail = (ticketCode) => apiGet(`/tickets/issued-detail?ticketCode=${ticketCode}`); // data → TicketDetailResponse (PR #12)
export const scanTicket = (ticketCode) => fetch(`${BASE_API.replace('/v1','')}/api/tickets/scan`,{method:'POST',credentials:'include',headers:{'Content-Type':'application/json'},body:JSON.stringify({ticketCode})}).then(r=>r.json()); // → data.status TIKET_VALID/TIKET_SUDAH_DIPAKAI

// contoh user/profile & logout — base /api (PR #11)
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

# GAP ANALYSIS — Status Aktual vs Spec v1.3.0

> **Terakhir diperbarui:** 15 September 2026, branch `main` (HEAD `60fe852`).
> **PDF audit** (`laporan_gap_analysis_audit_rev10.pdf`) OUTDATED — Refund, Organizer, Legal, Payment sudah berubah.

## LEGENDA

| Simbol | Arti |
|---|---|
| ✅ REAL | Aktif, persisten ke DB |
| ⚠️ MOCK | Aktif, data statis/hardcoded |
| ❌ BELUM | Belum ada controller/service |
| 🗑️ HAPUS | Pernah ada, sudah dihapus dari kode |

---

## A. SPEC V1.3.0 — Status per Modul

### 01. Home & Search — ✅ 6/6

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/home/hero-banner` | ✅ | `HomeSearchController` → `HomeSearchService` |
| GET | `/api/home/event-card` | ✅ | `HomeSearchController` → `HomeSearchService` |
| GET | `/api/home/locations` | ✅ | `HomeSearchController` → `HomeSearchService` |
| GET | `/api/search/results` | ✅ | `HomeSearchController` → `HomeSearchService` |
| GET | `/api/search/locations` | ✅ | `HomeSearchController` → `HomeSearchService` |
| GET | `/api/search/categories` | ✅ | `HomeSearchController` → `HomeSearchService` |

### 02. Events & Detail — ✅ 8 REAL + 2 stub/partial (`facilities` string, `create` stub)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/events` | ✅ | `EventController` → `EventService` |
| GET | `/api/events/featured` | ✅ | `EventController` → `EventService` |
| GET | `/api/events/{id}` | ✅ | `EventController` → `EventService` |
| GET | `/events/detail/banner` | ✅ | Field `image` di `EventDetailResponse` |
| GET | `/events/detail/info` | ✅ | Field title/date/location di detail |
| GET | `/events/detail/description` | ✅ | Field description di detail |
| GET | `/events/detail/facilities` | ⚠️ | Raw string, frontend split(", ") |
| GET | `/events/detail/lineup` | ✅ | JSON populated |
| GET | `/events/detail/tickets` | ✅ | Field tickets array |
| **POST** | **`/api/events/create`** | ⚠️ | **ADA sebagai STUB** (publik, random UUID + DRAFT, body diabaikan — perlu persist + `CreateEventRequest`) |

### 03. Checkout & Payment — ✅ 11/11

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| POST | `/api/checkout/initiate` | ✅ | `CheckoutController` → `OrderService` |
| POST | `/api/checkout/attendees` | ✅ | `CheckoutController` → `OrderService` |
| POST | `/api/checkout/calculation` | ✅ | `CheckoutController` → `OrderService` |
| GET | `/api/checkout/summary` | ✅ | `CheckoutController` → `PaymentService` |
| POST | `/api/checkout/process` | ✅ | `CheckoutController` → `OrderService` |
| GET | `/api/orders/status` | ✅ | `CheckoutController` → `OrderService` |
| GET | `/api/orders/{id}/total-amount` | ✅ | `CheckoutController` → `PaymentService` |
| GET | `/api/orders/{id}/expired-time` | ✅ | `CheckoutController` → `PaymentService` |
| POST | `/api/payments/charge` | ✅ | `PaymentController` → `MidtransService` (REAL Snap) |
| POST | `/api/payments/midtrans-notification` | ⚠️ | `PaymentController` (log only, belum update order) |
| GET | `/payments/methods` | 🗑️ | **DIHAPUS dari kode** |
| GET | `/payments/methods/virtual-account` | 🗑️ | **DIHAPUS dari kode** |

### 04. My Tickets & E-Ticket — ✅ 4/4

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/tickets/user/{email}` | ✅ | `TicketController` → `TicketService` |
| GET | `/api/tickets/my-tickets?userEmail=` | ✅ | Alias PR #12 |
| GET | `/api/tickets/issued-detail?ticketCode=` | ✅ | `TicketController` → `TicketService` |
| POST | `/api/tickets/scan` | ✅ | `TicketController` → `TicketService` |

### 05. Refund — 5 REAL + 1 MOCK + 1 PARTIAL (7 path, base `/api`)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| POST | `/api/tickets/refund/request` | ✅ | `submitRefund` REAL — amount = order−fee bila order ada; `organizerId` selalu terisi (fallback = customerId) |
| POST | `/api/refund/submit` | ✅ | Alias path yang sama |
| GET | `/api/refund/banks` | ⚠️ | 8 bank + logoUrl/active, masih hardcoded (bukan DB) |
| GET | `/api/refund/order-summary` | ✅ | REAL bila order ada (orderNumber/eventTitle/tier/qty/status); fallback mock 295000 bila tidak |
| GET | `/api/refund/refund-detail/info` | ✅ | REAL dari DB via `findById` |
| GET | `/api/refund/refund-detail/download-proof` | ⚠️ | Endpoint REAL tapi `proofUrl` selalu `""` — entity tak punya kolom proof |
| GET | `/api/tickets/refund/refund-history` | ✅ | REAL via `findByCustomerId` `@Query` |

### 06. User Profile & Transaksi — ✅ 6/6

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/user/profile` | ✅ | `UserController` → `UserService` |
| PUT | `/api/user/profile/save` | ✅ | `UserController` → `UserService` |
| PUT | `/api/account/change-password` | ✅ | `UserController` → `UserService` |
| POST | `/api/user/avatar` | ✅ | REAL (validasi image/5MB, saveFile, diserve WebConfig) |
| POST | `/api/user/logout` | ✅ | `UserController` → `AuthService` |
| GET | `/api/transactions/history` | ✅ | `UserController` → `UserService` |

### 07. Organizer (29 endpoint, base `/api/organizer` sejak PR #25 — path lama `/organizer/*` MATI) — ✅ 18 HYBRID + 11 MOCK; Legal (4 path) — ⚠️ MOCK content, ✅ PUBLIC (BUG fixed PR #25)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| POST | `/api/organizer/register` | ✅ hybrid | Simpan `Organizer(PENDING)` bila login; mock berflag bila tidak |
| POST | `/api/organizer/documents/upload` | ✅ | Simpan file `uploads/organizer-docs/` + `document_url` |
| GET | `/api/organizer/status` | ✅ hybrid | DB `findByUserUserId`; fallback mock PENDING |
| GET | `/api/organizer/dashboard` | ✅ hybrid | Hitung real dari events/orders (`real:true`); fallback mock 42500000 |
| GET | `/api/organizer/profile` | ✅ hybrid | DB (+`UNREGISTERED` bila user belum EO); fallback mock Harmoni |
| PUT | `/api/organizer/profile` | ✅ hybrid | Update organizer+user (key snake_case); echo bila tanpa konteks |
| POST | `/api/organizer/profile/avatar` | ✅ | Simpan `uploads/avatars/` (URL tak ditulis ke entity) |
| POST | `/api/organizer/profile/upload-portfolio` | ✅ | Simpan `uploads/organizer-docs/portfolio/` |
| POST | `/api/organizer/profile/upload-deed` | ✅ partial | File tersimpan, tapi URL **belum** ditulis ke `organizer.akta_perusahaan` |
| GET | `/api/organizer/profile/document` | ✅ hybrid | Baca `aktaPerusahaan` DB (`has_akta`); fallback mock |
| POST | `/api/organizer/auth/change-password` | ✅ hybrid | BCrypt verify+update (`oldPassword`+`newPassword`/`new_password`); `400 Password lama salah!`; no-op bila tanpa login |
| POST | `/api/organizer/auth/logout` | ⚠️ | STUB no-op — pakai `POST /api/user/logout` untuk logout sungguhan |
| GET | `/api/organizer/refunds` | ✅ hybrid | `findByOrganizerId` → fallback PENDING global → mock REF-001 |
| GET | `/api/organizer/refunds/detail?id=` | ✅ hybrid | `findById` (id UUID string); fallback mock |
| PATCH | `/api/organizer/refunds/{id}/status` | ✅ hybrid | Update status/adminNote/rejection_reason/processedAt; fallback mock |
| GET | `/api/organizer/bank-accounts` | ✅ hybrid | Dari entity organizer; fallback mock BCA |
| GET | `/api/organizer/events/{id}/payout-balance` | ✅ hybrid | Hitung real (90%/10%); fallback mock 50jt |
| GET | `/api/organizer/payouts` | ✅ hybrid | `findByOrganizerId`; fallback mock id 101 |
| POST | `/api/organizer/payouts` | ✅ hybrid | Simpan payout (`organizerId` terisi!) bila login+amount; fallback mock 102 |
| GET | `/api/organizer/payouts/detail?id=` | ⚠️ | Praktis selalu mock — param `Long`, UUID tak bisa di-bind |
| GET | `/api/organizer/events` | ⚠️ | BARU PR #25 — stub `[]` |
| POST | `/api/organizer/events` | ⚠️ | BARU PR #25 — stub random UUID DRAFT, body diabaikan |
| PUT | `/api/organizer/events/update` | ⚠️ | BARU PR #25 — stub echo body |
| POST | `/api/organizer/events/publish?id=` | ⚠️ | BARU PR #25 — stub, tak update DB |
| GET | `/api/organizer/events/draft` | ⚠️ | BARU PR #25 — stub `[]` |
| GET | `/api/organizer/events/{id}/sales-summary` | ⚠️ | BARU PR #25 — stub zeros |
| GET | `/api/organizer/dashboard/metrics` | ⚠️ | BARU PR #25 — stub zeros (beda dari `/dashboard` hybrid) |
| GET | `/api/organizer/dashboard/recent-events` | ⚠️ | BARU PR #25 — stub `[]` |
| GET | `/api/organizer/dashboard/recent-transactions` | ⚠️ | BARU PR #25 — stub `[]` |
| GET | `/terms-conditions` + alias `/api/terms-conditions` | ⚠️ content, ✅ public | `LegalService` hardcoded (kini kaya: slug/version/sections) — FIXED PR #25, verified live 200 |
| GET | `/privacy-policy` + alias `/api/privacy-policy` | ⚠️ content, ✅ public | Sama — FIXED PR #25, verified live 200 |

### Admin Dashboard & Users — ✅ 7/7

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/admin/dashboard/metrics` | ✅ | `AdminDashboardController` → `AdminDashboardService` (count + sum revenue) |
| GET | `/api/admin/dashboard/recent-events` | ✅ | `AdminDashboardController` → `AdminDashboardService` |
| GET | `/api/admin/dashboard/recent-transactions` | ✅ | `AdminDashboardController` → `AdminDashboardService` |
| GET | `/api/admin/users` | ✅ | `AdminUserController` → `AdminUserService` |
| GET | `/api/admin/users/{id}` | ✅ | `AdminUserController` → `AdminUserService` |
| PATCH | `/api/admin/users/{id}/status` | ✅ | `AdminUserController` → `AdminUserService` + audit |
| PATCH | `/api/admin/users/{id}/suspend` | ✅ | `AdminUserController` → `AdminUserService` + audit |

### Admin EO Applications — ✅ 4/4 (merge `4836263`, SUDAH DI MAIN)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/admin/eo-applications[?status=]` | ✅ | `AdminEoController:27` → `AdminEoService` via `Organizer` entity (`findByVerificationStatus`), **bukan `EoApplication` terpisah** |
| GET | `/api/admin/eo-applications/{id}` | ✅ | Detail via `organizerRepository.findById` |
| PATCH | `/api/admin/eo-applications/{id}/status` | ✅ | VERIFIED/REJECTED + `rejectionReason` + audit; VERIFIED juga update `users.role` → ORGANIZER |
| GET | `/api/admin/eo-applications/{id}/documents/company-deed` | ✅ | Return `{documentUrl}` dari kolom akta organizer |

### Admin Settings — ✅ 6/6 (3 dari merge `4836263` + 3 extension baru)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/admin/settings/general` | ✅ | `AdminSettingsController:55` → `settingsRepository.findAll` → `Map<String,String>` |
| PUT | `/api/admin/settings/general` | ✅ | Save APP_NAME/CONTACT_EMAIL/ADMIN_FEE/ORDER_EXPIRY_MINUTES + audit |
| GET | `/api/admin/audit-logs?page&size` | ✅ | `findAllByOrderByCreatedAtDesc` pageable |
| GET | `/api/admin/audit-logs/export` | ✅ | **BARU** — `findAllForExport()` → `List<AuditLogExportResponse>` (JSON) |
| GET | `/api/admin/audit-logs/export/csv` | ✅ | **BARU** — bangun string CSV `auditId,actorId,actorName,action,detail,createdAt` (masih dibungkus `ApiResponse`, bukan file download) |
| POST | `/api/admin/settings/upload-logo` | ✅ | **BARU** — validasi PNG/JPG/JPEG ≤5MB → simpan `uploads/logos/platform-logo-<uuid>.<ext>` → `PLATFORM_LOGO` di tabel `settings` (butuh `settingsValue` VARCHAR(255)) + audit |

### Admin Payouts — ✅ 4/4 TAPI ⚠️ table-sharing risk (committed PR #24)

| Method | Endpoint | Status | Controller/Service |
|---|---|---|---|
| GET | `/api/admin/payouts[?status=]` | ✅ ⚠️ | `AdminPayoutController:27` → `AdminPayoutService.getAllPayouts` via `RefundRepository.findByStatus` / `findAllByOrderByCreatedAtDesc` — **tanpa discriminator, refund customer ikut muncul** |
| GET | `/api/admin/payouts/{id}` | ✅ | `findById` → `PayoutDetailResponse` |
| PATCH | `/api/admin/payouts/{id}/status` | ✅ | Set status + `adminNote`, APPROVED → `processedAt=now` + audit |
| GET | `/api/admin/payouts/{id}/documents/reconciliation` | ✅ | Return detail + `reconciliationDocumentUrl` (null sampai diisi manual) |

> **Catatan payout:** tidak ada entity `Payout` terpisah — pakai `RefundRequestEntity` (tabel `refund_requests`). Sejak PR #24 `submitRefund()` selalu isi `organizerId` (real dari event, fallback = customerId) → filter `organizerId IS NOT NULL` **tak lagi memisahkan** refund vs payout. Perlu discriminator baru (mis. kolom `type`, atau payout = baris dengan `orderId IS NULL` karena `createPayout` EO tak set orderId).

---

## B. ENDPOINT YANG BELUM ADA (perlu dibuat)

### B1. Events — ✅ ADA sebagai STUB (PR #25)

`POST /api/events/create` (publik, MOCK: random UUID + DRAFT, body diabaikan). Upgrade ke REAL perlu persist `events` + `ticket_tiers` (DTO `CreateEventRequest` sudah ada tapi belum dipakai).

### B2. Organizer Events — ✅ ADA sebagai STUB (PR #25, 6 endpoint MOCK)

| Method | Endpoint | Kebutuhan |
|---|---|---|
| GET | `/api/organizer/events` | List event milik EO |
| POST | `/api/organizer/events` | Buat event baru |
| PUT | `/api/organizer/events/update` | Update event |
| POST | `/api/organizer/events/publish` | Publish event |
| GET | `/api/organizer/events/draft` | Draft events |
| GET | `/api/organizer/events/{id}/sales-summary` | Ringkasan penjualan |

**Service baru:** `OrganizerEventService` — entity `OrganizerEvent`, repo `OrganizerEventRepository`

### B3. Organizer Dashboard (3 endpoint — ✅ ADA sebagai STUB PR #25, MOCK zeros)

| Method | Endpoint | Kebutuhan |
|---|---|---|
| GET | `/api/organizer/dashboard/metrics` | Metrics dari DB |
| GET | `/api/organizer/dashboard/recent-events` | Event terbaru |
| GET | `/api/organizer/dashboard/recent-transactions` | Transaksi terbaru |

**Service baru:** `OrganizerDashboardService` — query dari `events`, `orders`, `ticket_items`

### B4. Admin EO Applications — ✅ SUDAH ADA (merge `4836263`)

`GET /api/admin/eo-applications[?status=]`, `GET /api/admin/eo-applications/{id}`, `PATCH /api/admin/eo-applications/{id}/status`, `GET /api/admin/eo-applications/{id}/documents/company-deed` — semua aktif via `AdminEoService` + `Organizer` entity. **Tidak ada entity `EoApplication` terpisah — jangan buat.**

### B5. Admin Payouts — ✅ SUDAH ADA (committed PR #24)

`GET /api/admin/payouts[?status=]`, `GET /api/admin/payouts/{id}`, `PATCH /api/admin/payouts/{id}/status`, `GET /api/admin/payouts/{id}/documents/reconciliation` — aktif via `AdminPayoutService` + `RefundRepository` (tabel `refund_requests` bersama refund). Sisa: tambah discriminator (filter `IS NOT NULL` tak mempan — `organizerId` kini selalu terisi).

### B6. Admin Audit & Settings — ✅ SUDAH ADA (3 lama + 3 baru)

`GET /api/admin/audit-logs`, `GET /api/admin/settings/general`, `PUT /api/admin/settings/general` (merge `4836263`) + `GET /api/admin/audit-logs/export`, `GET /api/admin/audit-logs/export/csv`, `POST /api/admin/settings/upload-logo` (baru). Semua via `AdminSettingsService` + tabel `settings` yang sudah ada. Tidak perlu entity baru.

---

## C. ENDPOINT YANG PERLU UPGRADE (MOCK → REAL)

| # | Endpoint | Service | Yang perlu diubah |
|---|---|---|---|
| 1 | `POST /api/organizer/profile/upload-deed` | `OrganizerService.uploadDeed` | Tulis URL file ke `organizer.akta_perusahaan` (saat ini file tersimpan tapi kolom tak diupdate) |
| 2 | `GET /organizer/payouts/detail?id=` | `OrganizerController.getPayoutDetail` | Ubah param `Long` → `String`/`UUID` agar UUID bisa di-bind (saat ini selalu jatuh ke mock) |
| 3 | `POST /organizer/auth/logout` | `OrganizerService.logout` | Delegasikan ke `AuthService.logout` + hapus cookie (saat ini no-op) |
| 4 | `POST /organizer/auth/change-password` | `OrganizerService.changePassword` | Kembalikan `400` eksplisit bila tanpa login/auth tak ada (saat ini silent no-op `200`) |
| 5 | Fallback mock organizer | `OrganizerService` | Pertahankan flag `"mock":true` agar frontend bisa branching real-vs-mock |
| 16 | `GET /api/refund/banks` | `RefundService` | Pindah ke tabel/konstanta DB bila perlu |
| 17 | `GET /api/refund/order-summary` | `RefundService` | Query `orders` by `orderId` ganti hardcoded `290000.00` |
| 18 | `GET /api/refund/refund-detail/download-proof` | `RefundService` | Tambah kolom `proof_url` di `RefundRequestEntity` atau return dari storage |
| 19 | `POST /api/payments/midtrans-notification` | `PaymentController` | Update `orders.status` → PAID/EXPIRED/CANCELLED (saat ini log only) |
| 20 | `GET /terms-conditions`, `GET /privacy-policy` | `LegalController`/`SecurityConfig` | Dual alias: root + `/api` prefix — FIXED |
| 21 | `GET /api/admin/payouts` (+ discriminator) | `AdminPayoutService` | Tambah kolom/filter pemisah refund vs payout (`organizerId IS NOT NULL` tak mempan) |
| 22 | `POST /api/user/avatar` | `UserController` | Simpan file ke `uploads/avatars/` ganti mock URL |

> `GET /api/tickets/refund/refund-history` **TIDAK perlu upgrade** — sudah REAL (`findByCustomerId`). `PaymentService.processPaymentCharge()` **dead code** — tak dipanggil `PaymentController` (langsung `MidtransService`), boleh hapus atau biarkan.

---

## D. FILE PATHS YANG RELEVAN

```
src/main/java/com/example/eventday/
├── controller/
│   ├── AuthController.java           # /api/auth/*
│   ├── CheckoutController.java       # /api/checkout/*, /api/orders/*
│   ├── EventController.java          # /api/events/* + POST /create (MOCK publik, body diabaikan; CreateEventRequest belum dipakai)
│   ├── HomeSearchController.java     # /api/home/*, /api/search/*
│   ├── LegalController.java          # /terms-conditions|/privacy-policy + alias /api/* — FIXED rev.13 (publik, konten hardcoded kaya)
│   ├── OrganizerController.java      # base /api/organizer (BREAKING rev.13): 29 endpoint = 18 hybrid + 11 mock
│   ├── PaymentController.java        # /api/payments/* (charge Bearer REAL Snap + notification PUBLIK log-only rev.13)
│   ├── RefundController.java           # base /api (BREAKING rev.13): 7 path — submit/detail/history/order-summary REAL, banks MOCK, download-proof partial
│   ├── TicketController.java         # /api/tickets/* (3 path, 4 dengan alias my-tickets)
│   ├── UserController.java           # /api/user/*, /api/account/*, /api/transactions/* (avatar REAL rev.13)
│   ├── WebConfig.java                # NEW rev.13: serve /uploads/** + /uploads/logos/** dari disk
│   ├── HomeController.java           # /
│   └── admin/
│       ├── AdminDashboardController.java  # /admin/dashboard/* (3)
│       ├── AdminUserController.java       # /api/admin/users/* (4)
│       ├── AdminSettingsController.java   # /api/admin/audit-logs*, /api/admin/settings/* (6: 3 lama + export/export-csv/upload-logo baru)
│       ├── AdminEoController.java         # /api/admin/eo-applications/* (4, merge 4836263)
│       └── AdminPayoutController.java     # /api/admin/payouts/* (4, committed PR #24)
├── service/
│   ├── AuthService.java
│   ├── EventService.java
│   ├── HomeSearchService.java
│   ├── OrderService.java
│   ├── PaymentService.java           # getCheckoutSummary REAL; processPaymentCharge DEAD CODE (tak dipanggil)
│   ├── MidtransService.java          # Midtrans Snap gateway REAL (dipanggil PaymentController langsung)
│   ├── TicketService.java            # generateTicket() tak dipanggil siapapun
│   ├── UserService.java
│   ├── EmailService.java
│   ├── AuditLogService.java
│   ├── LegalService.java             # hardcoded kaya slug/version/sections/HTML (MOCK content, rev.13)
│   ├── OrganizerService.java         # 638 baris HYBRID DB + mock fallback (PR #21); 9 stub baru tak pakai service (controller langsung)
│   ├── RefundService.java            # submit/detail/history/order-summary REAL (2 terakhir baru PR #24); banks MOCK; download-proof partial
│   └── admin/
│       ├── AdminDashboardService.java
│       ├── AdminUserService.java
│       ├── AdminSettingsService.java     # + exportAuditLogs/exportCsv/uploadLogo (PR #24)
│       ├── AdminEoService.java           # pakai Organizer entity (merge 4836263)
│       └── AdminPayoutService.java       # committed PR #24 — via RefundRepository + OrganizerRepository
├── entity/
│   ├── User.java, Auth.java, Otp.java, Organizer.java, Event.java
│   ├── TicketTier.java, Booking.java, Order.java, TicketItem.java
│   ├── RefundRequestEntity.java      # tabel refund_requests + kolom payout (organizer_id/account_holder/rejection_reason/admin_note/reconciliation_document_url/processed_at); TAK ADA entity Payout terpisah
│   ├── RefundRequest.java            # entity lama @ManyToOne (satu tabel, potensi konflik mapping — waspada)
│   ├── Settings.java, AuditLog.java  # settingsValue VARCHAR(255) setelah update
├── dto/
│   ├── BankResponse.java, RefundDetailResponse.java, RefundRequest.java  # Refund DTOs
│   ├── admin/PayoutResponse.java, admin/PayoutDetailResponse.java, admin/UpdatePayoutStatusRequest.java  # BARU payout
│   ├── admin/AuditLogExportResponse.java  # BARU export audit
│   ├── AdminDashboardMetricsResponse.java, AdminUserListItemResponse.java, AdminUserStatusRequest.java  # Admin DTOs
│   └── (semua DTO lain yang sudah ada)
└── repository/
    ├── UserRepository.java, AuthRepository.java, OtpRepository.java
    ├── EventRepository.java, TicketTierRepository.java, OrderRepository.java
    ├── AttendeeRepository.java, TicketItemRepository.java
    ├── RefundRepository.java         # + findByOrganizerId/findByStatus/findAllByOrderByCreatedAtDesc (baru)
    ├── AuditLogRepository.java       # + findAllForExport() (baru)
    └── OrganizerRepository.java      # + findByVerificationStatus (PR #21)
```

---

## E. SERVICES — SUDAH ADA vs PERLU BARU

**SUDAH ADA (jangan buat ulang):** `AdminEoService`, `AdminPayoutService`, `AdminSettingsService` (+ `AdminDashboardService`, `AdminUserService`).

| Service | Path | Status |
|---|---|---|
| `OrganizerEventService` | `service/OrganizerEventService.java` | ❌ BELUM — CRUD event milik EO |
| `OrganizerDashboardService` | `service/OrganizerDashboardService.java` | ❌ BELUM — metrics EO dari DB |

## F. ENTITIES/DTOs/REPOS — SUDAH ADA vs PERLU BARU

**SUDAH ADA:** `PayoutResponse`, `PayoutDetailResponse`, `UpdatePayoutStatusRequest`, `AuditLogExportResponse` (DTO payout/export baru); `AdminEoApplicationResponse`, `AdminEoStatusRequest`, `AdminSettingsRequest`, `AdminDashboardMetricsResponse`, `AdminUserListItemResponse`, `AdminUserStatusRequest`. **TIDAK ADA dan JANGAN BUAT:** `EoApplication` entity, `Payout` entity, `AdminSettings` entity (pakai `Organizer` / `RefundRequestEntity` / `Settings` yang sudah ada).

| Item | Path | Status |
|---|---|---|
| `OrganizerEvent` | `entity/OrganizerEvent.java` | ❌ BELUM |
| `OrganizerEventRepository` | `repository/OrganizerEventRepository.java` | ❌ BELUM |

---

# INSTRUKSI UNTUK AI AGENT — BUAT PDF

Anda akan membuat PDF dokumentasi lengkap berdasarkan isi file ini. Ikuti aturan berikut:

## Struktur PDF yang Harus Dibuat
1. **Halaman Sampul** — Judul "Laporan Audit Status API & Gap Analysis Eventday", tanggal audit 15 September 2026, workspace D:\eventday
2. **Ringkasan Eksekutif** — Total endpoint aktif, persentase per modul
3. **REKAPITULASI PER MODUL** — Tabel status per modul (sesuai §A di atas)
4. **DAFTAR RINCI STATUS 98 ENDPOINT** — Setiap endpoint dengan status SUDAH ADA / BELUM ADA / DIHAPUS / MOCK (sumber utama: **§17**, bukan §A)
5. **PERMINTAAN BARU TIM UI/UX** — Yang masih BELUM (REAL): persist `events/create`, persist 6 `organizer/events`, persist 3 `dashboard` EO; yang SUDAH ADA (stub/MOCK): ketiga itu + Admin Payouts (4) + Settings extension (3) + EO (4) + Organizer hybrid (18)
6. **Catatan Sinkronisasi Kode** — Fakta-fakta teknis yang berlaku

## Aturan Penting (JANGAN LUPA — VERIFIKASI 2026-09-16, HEAD `d75de2b` + uncommitted payout/settings)
- **AdminSettings & AdminEo SUDAH ADA di main** (merge commit `4836263`). Jangan bilang "belum ada" atau "di branch lain"
- **Admin Payouts (4) + Settings extension (3) SUDAH ADA** (uncommitted, siap commit): `AdminPayoutController`, `export/export-csv/upload-logo`
- **`AdminEoService` pakai `Organizer` entity** (bukan `EoApplication` terpisah). `OrganizerRepository.findByVerificationStatus()` digunakan (PR #21)
- **Refund history** (`GET /tickets/refund/refund-history`) **REAL** — queries `refundRepository.findByCustomerId()` via `@Query`. Jangan bilang MOCK
- **Refund banks** (`GET /api/refund/banks`) **MOCK** — hardcoded BCA/MANDIRI/BNI/BRI. Jangan bilang REAL
- **Refund download-proof** endpoint **REAL** — tapi `proofUrl` selalu `""` karena `RefundRequestEntity` tak punya field `proofUrl` (hanya `RefundDetailResponse` punya)
- **Refund order-summary** kini **REAL** (hitung dari order; fallback mock 295rb bila order tak ada). Jangan bilang MOCK-hardcoded-290rb
- **Midtrans Snap REAL** — `POST /api/payments/charge` return `{snapToken, redirectUrl}` via `MidtransService`. Bukan mock VA. `midtrans-notification` log-only. `PaymentService.processPaymentCharge()` dead code
- **LegalController FIXED (rev.13)** — dual alias + permitAll 4 path → **publik, verified live 200**. `LegalService` tetap hardcoded (slug/version/sections)
- **OrganizerService HYBRID + stub** (rev.13) — 18 DB-backed + mock fallback berflag + 11 mock (9 stub baru PR #25 + `auth/logout` + `payouts/detail`). Base `/api/organizer` (BREAKING)
- Payment `GET /payments/methods` dan `GET /payments/methods/virtual-account` sudah **DIHAPUS** (🗑️); DTO `PaymentChargeRequest/Response` stale; `PaymentService.processPaymentCharge()` dead code
- `EoApplication.java` / `Payout.java` / `AdminSettings.java` **TIDAK ADA** sebagai entity terpisah — jangan buat
- `rejectionReason` di EO status **opsional** (bukan wajib saat REJECTED)
- Total: **98 path unik — 79 REAL** (18 hybrid organizer), **17 MOCK, 2 PARTIAL**, 0 BUG (legal fixed), 4 table-sharing risk (subset Payout — catatan: `organizerId` kini selalu terisi/fallback customerId, jadi filter `IS NOT NULL` tak lagi memisahkan; perlu discriminator lebih cerdas)

## Format PDF
- Gunakan library PDF Java atau generate via Markdown → PDF
- Sertakan tabel, status, dan catatan teknis
- Format: A4, font readable, struktur profesional
- Sertakan "LEGENDA" di awal: ✅ REAL / ⚠️ MOCK / ❌ BELUM / 🗑️ HAPUS

---

# CATATAN KOREKSI PENTING
> File ini diverifikasi 15 September 2026 (HEAD `4836263`) lalu diaudit ulang 16 September 2026 (HEAD `d75de2b` + uncommitted payout/settings). Koreksi rev.11:
> 1. Refund history: MOCK → **REAL** (DB query `findByCustomerId`)
> 2. Refund download-proof: MOCK → **REAL endpoint** (proofUrl `""` karena entity mismatch)
> 3. Refund banks: REAL → **MOCK** (hardcoded list, bukan DB)
> 4. Admin EO/Settings: "di branch lain" → **SUDAH ADA DI MAIN** (+ 3 extension baru: export/export-csv/upload-logo)
> 5. Admin Payouts: "belum ada" → **SUDAH ADA** (4 endpoint, sharing tabel `refund_requests`, belum commit)
> 6. Legal: "publik permitAll" → **BUG PATH, butuh login** (`LegalController` tanpa `/api/v1` prefix vs `SecurityConfig:70`)
> 7. Organizer: "4 endpoint" → **20 endpoint, SEMUA MOCK**
> 8. EoApplication/Payout/AdminSettings entity: **TIDAK ADA** — pakai `Organizer`/`RefundRequestEntity`/`Settings`
> 9. `PaymentService.processPaymentCharge()`: **dead code** — `PaymentController` langsung panggil `MidtransService`
> 10. Organizer: SEMUA MOCK → **HYBRID** (`OrganizerService` 638 baris DB-backed PR #21, mock hanya fallback berflag)
> 11. `rejectionReason` EO: "wajib saat REJECTED" → **opsional** (kode tak validasi, hanya audit log)
> 12. Total: 80 path/40 REAL → **86 path/78 REAL**; referensi lengkap baru di **§17** (method/path/auth/body/query/response per endpoint)
>
> Tambahan rev.13 (PR #23 `324932a`, #24 `3b1c02d`, #25 `6e7bcfd` → HEAD `6e7bcfd`, server di-restart + verified live):
> 13. Legal BUG **FIXED** — dual alias + permitAll 4 path (`200` tanpa token). "Masih bug" kemarin = build lama yang jalan
> 14. **BREAKING**: `RefundController` `/api`→`/api/v1`, `OrganizerController` `/organizer`→`/api/organizer` (path lama → `500`)
> 15. Baru: `POST /api/events/create` (MOCK publik!), 9 stub organizer, `midtrans-notification` publik, avatar REAL, `order-summary` REAL, `submitRefund` selalu isi `organizerId`, `WebConfig /uploads/**`
> 16. Total: 86 → **98 path (79 REAL / 17 MOCK / 2 PARTIAL)**; B1/B2/B3 → ADA-sebagai-stub

