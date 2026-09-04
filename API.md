# API.md - Eventday REST API Documentation

Base URL: `http://localhost:8081`

> **Status:** Hanya modul **Auth** yang aktif (+ Google Login). Modul lain masih **SCHEMA ONLY**.

---

## 1. Auth - Register

**POST** `/api/v1/auth/register` (alias `/api/auth/register` juga permit)

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
| Field | Required | Notes |
|---|---|---|
| name | Yes | Max 100 |
| email | Yes | Unique, @Email |
| username | Yes | Unique, 3-20 chars, huruf/angka/underscore (`^[a-zA-Z0-9_]+$`) |
| phone | No | Max 15 |
| password | Yes | Min 6, BCrypt ke `auth` |
| role | No | CUSTOMER/ORGANIZER/ADMIN fallback CUSTOMER |
| nik | No | 16 digit, Unique |

Flow `AuthService.java:60`: validasi → duplikat email/username/nik → `User` save → `Auth` save `INACTIVE` → generate OTP → save `otp` → `sendOtpEmail` → audit REGISTER.

### Response 200
```json
{"message":"Registrasi berhasil! OTP telah dikirim ke email Anda.","userId":"...","name":"John Doe","email":"john@example.com","username":"johndoe_99","role":"CUSTOMER","token":null,"expiresIn":null}
```
400: `"Email sudah terdaftar!"` / `"Username sudah terdaftar!"` / `"NIK sudah terdaftar!"`

```bash
curl -X POST localhost:8081/api/v1/auth/register -H "Content-Type: application/json" -d '{"name":"John","email":"john@mail.com","username":"john123","password":"123456"}'
```

---

## 2. Auth - Login

**POST** `/api/v1/auth/login`

Login support **email ATAU username** + password. Backend deteksi `identifier` mengandung `@` → cari by email, else by username (fallback cross-check).

Flow `AuthService.java:126`: `getIdentifier()` → `findByEmail`/`findByUsername` → `findByUserUserId` → `matches` → cek `status ACTIVE` → `jwtTokenProvider.generateToken(userId,email,role)` (86400000ms) → update `aksesToken/expiredToken/ACTIVE` → return.

### Request (email)
```json
{"email":"john@example.com","password":"123456"}
```
### Request (username)
```json
{"username":"johndoe_99","password":"123456"}
```
### Request (generic identifier)
```json
{"identifier":"johndoe_99","password":"123456"}
```

### Response 200
```json
{"message":"Login berhasil!","userId":"...","name":"John Doe","email":"john@example.com","username":"johndoe_99","role":"CUSTOMER","token":"eyJ...","expiresIn":86400}
```
400: `"Email atau username harus diisi!"` / `"Email atau password salah!"` / `"Akun belum aktif! Silakan verifikasi OTP terlebih dahulu."`

```bash
curl -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}'
curl -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"johndoe_99","password":"123456"}'
```

---

## 3. Auth - Google Login (Baru)

**POST** `/api/v1/auth/google`

ID Token diambil **di frontend** via Google Identity Services, backend hanya verifikasi.

Flow `AuthService.java:115` `loginWithGoogle()`:
1. `GET https://oauth2.googleapis.com/tokeninfo?id_token=<idToken>` (RestTemplate)
2. cek `aud==google.client-id` (jika `google.client-id` diisi di `application.properties:19`), cek `exp`, `email_verified`
3. `findByEmail` → jika null buat `User(name,email,role=CUSTOMER)` (isNewUser)
4. `findByUserUserId` → jika null buat `Auth(password dummy BCrypt, authGoogle= sub.substring(0,20))` else update `authGoogle`
5. `jwtTokenProvider.generateToken()` → `aksesToken/expiredToken/ACTIVE` → audit `REGISTER_GOOGLE/LOGIN_GOOGLE` → return JWT sama seperti login.

### Request
```json
{"idToken":"eyJhbGciOiJSUzI1NiIs...Google ID Token..."}
```
| Field | Required | Notes |
|---|---|---|
| idToken | Yes | @NotBlank, ID Token JWT dari `google.accounts.id` GIS |

### Response 200
```json
{"message":"Login via Google berhasil!","userId":"...","name":"John Doe","email":"john@example.com","role":"CUSTOMER","token":"eyJ...","expiresIn":86400}
```
atau jika baru: `"Registrasi via Google berhasil!"`. 400 jika token invalid/expired/aud mismatch/email_verified false.

```bash
curl -X POST localhost:8081/api/v1/auth/google -H "Content-Type: application/json" -d '{"idToken":"eyJ...GoogleIDToken"}'
```

**Frontend:** `https://accounts.google.com/gsi/client` + `data-client_id=google.client-id` → `res.credential` → POST ke backend. Lihat `handle()` contoh di chat.

> `authGoogle` `VARCHAR(20)` → disubstring 20 char. `password` dummy UUID BCrypt karena kolom NOT NULL. Jika `google.client-id` kosong, validasi aud diskip (dev).

---

---

## 4. Auth - Verify OTP

**POST** `/api/v1/auth/verify-otp`

Mengaktifkan akun setelah registrasi.

### Request
```json
{"email":"john@example.com","otpCode":"123456"}
```
| Field | Required | Notes |
|---|---|---|
| email | Yes | @NotBlank, @Email |
| otpCode | Yes | @NotBlank |

### Response 200
```json
{"message":"OTP terverifikasi! Akun aktif, silakan login."}
```
400: `"Kode OTP tidak valid!"`, `"Kode OTP sudah expired!"`

---

## 5. Auth - Resend OTP

**POST** `/api/v1/auth/resend-otp`

Kirim ulang OTP baru jika yang lama expired. Menghapus OTP sebelumnya.

### Request
```json
{"email":"john@example.com"}
```

### Response 200
```json
{"message":"OTP baru berhasil dikirim ke email Anda!"}
```

---

## 6. Auth - Reset Password

**POST** `/api/v1/auth/reset-password` — single endpoint 3 mode untuk flow frontend OTP → Lanjutkan → New Password

Endpoint ini menangani 3 mode Lupa Password (agar frontend `klik Lanjutkan → pindah halaman`):
- **Mode 1 — Kirim OTP:** body `{"email"}` → generate 6-digit OTP → `password_reset_tokens` (+15min) → kirim email OTP
- **Mode 2 — Verifikasi OTP (klik Lanjutkan):** body `{"email","code"}` tanpa `newPassword` → cek `code` valid & belum expired → return `Kode OTP valid!` → frontend `navigate("/reset-password/new")`
- **Mode 3 — Reset Password:** body `{"email","code","newPassword"}` → verifikasi + update `Auth.password` BCrypt → delete token

### Request Mode 1 (Minta OTP)
```json
{"email":"john@example.com"}
```
```bash
curl -X POST localhost:8081/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com"}'
```
### Response Mode 1
```json
{"message":"Kode OTP berhasil dikirim ke email Anda!"}
```

### Request Mode 2 (Verifikasi OTP — klik Lanjutkan)
```json
{"email":"john@example.com","code":"123456"}
```
```bash
curl -X POST localhost:8081/api/v1/auth/reset-password -H "Content-Type: application/json" -d '{"email":"john@mail.com","code":"123456"}'
```
### Response Mode 2
```json
{"message":"Kode OTP valid! Silakan buat password baru."}
```
400: `"Kode OTP tidak valid!"` / `"Kode OTP sudah expired!"`

### Request Mode 3 (Reset Password)
```json
{"email":"john@example.com","code":"123456","newPassword":"newPassword123"}
```
| Field | Required | Notes |
|---|---|---|
| email | Yes | |
| code | Mode 2 & 3 | OTP 6 digit dari email |
| newPassword | Mode 3 only | Min 6 karakter |

### Response Mode 3
```json
{"message":"Password berhasil direset! Silakan login dengan password baru."}
```
400: `"Password baru minimal 6 karakter"`, `"Kode OTP tidak valid/expired"`


## 7. Middleware JWT

Semua selain `/api/v1/auth/**` butuh JWT `SecurityConfig.java:30` `STATELESS`.

`JwtAuthenticationFilter.java:23`: `Authorization: Bearer <token>` → `validate` → `getUserId/role` → cek `auth.status ACTIVE && aksesToken==token` → `ROLE_*` → `anyRequest.authenticated()`.

```bash
TOKEN=$(curl -s -X POST localhost:8081/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"john@mail.com","password":"123456"}' | jq -r .token)
curl localhost:8081/protected -H "Authorization: Bearer $TOKEN"
# 403 tanpa token / expired / logout
```

Config `application.properties:19`:
```properties
jwt.secret=eventday-super-secret-key-min-32-chars-change-in-production-123456
jwt.expiration-ms=86400000
google.client-id= # kosong = tidak cek aud
```

Logout internal `AuthService.logout(userId)` null-kan token, belum expose endpoint.

---

## ⏳ Modul Lain — SCHEMA ONLY

| Modul | Endpoint Rencana | Status |
|---|---|---|
| Events | `POST /api/events`, `GET /api/events` | DB ready |
| Orders | `POST /api/orders` | DB ready |
| Payments | `POST /api/payments/pay/{orderId}` | DB ready |
| Tickets | `POST /api/tickets/scan/{ticketItemId}` | DB ready |
| Settings | `GET/PUT /api/settings/**` | DB ready |
| Refunds | `POST /api/refunds` | DB ready |
| Audit | `GET /api/audit/**` | hanya log internal |

`RescheduleRequest` **dihapus** — jangan panggil.

---

## Enum

`User.role`: CUSTOMER (default) / ORGANIZER / ADMIN
`Auth.status`: INACTIVE / ACTIVE
LAIN schema-only: `organizers.verification_status UNVERIFIED`, `events.status DRAFT`, `bookings/orders PENDING`, `ticket_items UNREDEEMED`, `refund_requests PENDING`

---

## Flow Diagram
```
register (email/pass) ─→ login (email/pass) ─┐
                                             ├→ JWT Eventday → Bearer → Filter → Protected
google GIS (idToken) ──→ POST /google ───────┘       (RestTemplate tokeninfo, find/create User+Auth)
```
DB: `users --1:1-- auth (hash+googleId+token)`

---

## Notes Frontend
1. Base `http://localhost:8081`
2. Register/login simpan `token`, kirim `Authorization: Bearer <token>`
3. Google: `https://accounts.google.com/gsi/client` + `CLIENT_ID` → `res.credential` → POST ke `/google`
4. `google.client-id` harus sama di frontend & backend
5. Tanpa token 403, modul lain 403/404 sampai diaktifkan
