# API.md - Eventday REST API Documentation

Base URL: `http://localhost:8082`

> **Status 2026-09-09:** Hanya modul **Auth** yang aktif (+ Google Login + OTP). Modul lain masih **SCHEMA ONLY**. Backend `port 8082` sinkron `application.properties`, `jwt 24 jam`, `CORS *` untuk ngrok.
> **Last Updated:** 2026-09-09 — sinkron dengan `AGENTS.md` & `application.properties` (port 8082).
> **Response Standard:** Semua API sekarang pakai format ** `{msg, status, data}` ** — `status` = HTTP code, `msg` = pesan, `data` = payload / `null`.

```json
// sukses
{"msg":"Login berhasil!","status":200,"data":{"userId":"...","name":"...","token":"eyJ..."}}
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
`Set-Cookie: access_token=eyJhbG...; Path=/; HttpOnly; Max-Age=86400; SameSite=Lax` — token **tidak ada di `data.token` (hidden via `@JsonIgnore`)**, browser simpan otomatis. `data.expiresIn` detik (`86400` = 24 jam). Request selanjutnya kirim otomatis via `Cookie: access_token` atau manual `Authorization: Bearer <token>` (filter support keduanya `JwtAuthenticationFilter.java:32`).

### Error `400`
```json
{"msg":"Email atau username harus diisi!","status":400,"data":null}
{"msg":"Email atau password salah!","status":400,"data":null}
{"msg":"Akun belum aktif! Silakan verifikasi OTP terlebih dahulu.","status":400,"data":null}
```

```bash
curl -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
# ambil token: data.token
TOKEN=$(curl -s -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}' | jq -r .data.token)
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

**Frontend GIS:** `https://accounts.google.com/gsi/client` + `data-client_id=google.client-id` → `res.credential` → `POST /google` → `data.token`.

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

Semua selain `/api/v1/auth/**` butuh JWT. `SecurityConfig.java:31` `STATELESS`.

`JwtAuthenticationFilter.java:23`: `Authorization: Bearer <token>` → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `SecurityContext ROLE_*` → `anyRequest.authenticated()`.

`SecurityConfig.java` kini return standard `msg/status/data` untuk `401/403`:

```json
// tanpa token
{"msg":"Unauthorized: token tidak ada atau tidak valid","status":401,"data":""}
// token salah/expired/inactive
{"msg":"Forbidden: akses ditolak","status":403,"data":""}
```

```bash
TOKEN=$(curl -s -X POST localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}' | jq -r .data.token)
curl localhost:8082/api/events -H "Authorization: Bearer $TOKEN"  # 200 jika events aktif, 403 jika schema-only
curl localhost:8082/api/events  # -> 401 {msg, status:401}
curl localhost:8082/ # -> 200 {msg, status:200, data:"OK"}
```

Config `application.properties`:
```properties
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000 # 24 jam -> data.expiresIn 86400
google.client-id=875040780549-1jq8bicaq1ne1ltjt7bfjcfjo82e5dj0.apps.googleusercontent.com
```

---

## ⏳ Modul Lain — SCHEMA ONLY (belum aktif)

| Modul | Rencana Endpoint | Status | HTTP |
|---|---|---|---|
| Events | `POST /api/events`, `GET /api/events` | DB ready | `401/403` dengan `{msg,status,data}` |
| Orders | `POST /api/orders` | DB ready | `401/403` |
| Payments | `POST /api/payments/pay/{orderId}` | DB ready | `401/403` |
| Tickets | `POST /api/tickets/scan/{ticketItemId}` | DB ready | `401/403` |
| Settings | `GET/PUT /api/settings/**` | DB ready | `401/403` |
| Refunds | `POST /api/refunds` | DB ready | `401/403` |
| Audit | `GET /api/audit/**` | hanya log internal | `401/403` |

`RescheduleRequest` **dihapus** — jangan panggil.

---

## Error Format Global (sudah rapi)

`dto/ApiResponse.java:10` `GlobalExceptionHandler.java:10` + `SecurityConfig.java:33` — semua return `{msg,status,data}`:

*   Validasi `@Valid` → `400` `{"msg":"username: Username 3-20 karakter","status":400,"data":null}`
*   `RuntimeException` → `400`
*   `AuthenticationException` → `401`
*   `AccessDeniedException` → `403`
*   `Exception` → `500`
*   Sukses → `200` / `201` (register/google baru)

Network tab Chrome/Fetch: cek `Response` → `msg` untuk toast, `status` untuk branching, `data` untuk payload. `HomeController.java:10` juga sudah `{msg,status:200,data:"OK"}`.

---

## Enum

*   `User.role`: `CUSTOMER` (default) / `ORGANIZER` / `ADMIN`
*   `Auth.status`: `INACTIVE` / `ACTIVE`
*   Lain schema-only: `organizers.verification_status UNVERIFIED`, `events.status DRAFT`, `bookings/orders PENDING`, `ticket_items UNREDEEMED`, `refund_requests PENDING`

---

## Flow Diagram Frontend (pakai `.data`)

```
register {name,email,username,password} --201 {msg,status:201,data} OTP--> verify-otp {email,otpCode} --200 {msg,status:200}--> login {identifier,password} --200 {msg,status:200,data.token}--> simpan data.token
                                                                                                           |
google GIS idToken ------------------------POST /google --200/201 {msg,status,data.token}----------------+
                                                                                                           |
lupa password: POST /reset-password {email} --200 {msg}--email code--> POST /reset-password {email,code,newPassword} --200 {msg}--> login baru
```

DB: `users --1:1-- auth (hash+googleId+token+resetToken) --1:N-- otp` (`password_reset_tokens` dihapus, digabung ke `auth`)

---

## Notes Frontend (copy-paste ready dengan `msg/status/data` + HttpOnly Cookie)

```js
// helper fetch standar — token TIDAK di localStorage, pakai HttpOnly Cookie
async function api(path, body){
  const res = await fetch(`http://localhost:8082${path}`, {
    method:'POST',
    headers:{
      'Content-Type':'application/json',
      'ngrok-skip-browser-warning': 'true' // wajib jika pakai ngrok-free
    },
    credentials: 'include', // WAJIB agar Cookie access_token terkirim otomatis
    body: JSON.stringify(body)
  });
  const json = await res.json(); // {msg, status, data} — data.token TIDAK ADA
  if(!res.ok) throw new Error(json.msg);
  return json;
}

// contoh — tidak perlu simpan token manual
const reg = await api('/api/v1/auth/register', {name,email,username,password}); // 201
const v = await api('/api/v1/auth/verify-otp', {email, otpCode});
const login = await api('/api/v1/auth/login', {identifier: email, password}); // login.data.expiresIn saja
// Cookie access_token sudah tersimpan HttpOnly otomatis, request selanjutnya auto terkirim
// Jika butuh Bearer manual (postman), ambil dari Set-Cookie header di Network → Headers
```

1. Base `http://localhost:8082`, `Content-Type: application/json` selalu.
2. Cek `json.status` (bukan `res.status` saja) & `json.msg` untuk notifikasi.
3. Register `201` → langsung ke form OTP.
4. Login `200` → `data.token` + `data.expiresIn`.
5. Tanpa token → `401/403` dengan `msg` yang sama — redirect ke login.
6. `CORS *` sudah allow, `maxAge 3600`, `allowCredentials true` — aman untuk ngrok.
7. OTP `5 menit`, Reset code `15 menit`, JWT `24 jam`.
8. `ApiLoggingFilter.java:10` log tiap hit: `[API HIT] POST /api/v1/auth/login -> 200 (45ms)`.
```

