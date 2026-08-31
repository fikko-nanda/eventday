# API.md - Eventday REST API Documentation

Base URL: `http://localhost:8081`

---

## 1. Auth - Register

**POST** `/api/auth/register`

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
| name | String | Yes | Max 100 chars |
| email | String | Yes | Unique |
| phone | String | Yes | Max 20 chars |
| password | String | Yes | Min 6 chars |
| role | Enum | No | CUSTOMER (default), ORGANIZER, ADMIN |
| nik | String | No | 16 digit, Unique |

### Response (200 OK)
```json
{
  "message": "Registrasi berhasil!",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

### Response (400 Bad Request)
```json
"Email sudah terdaftar!"
```
atau
```json
"NIK sudah terdaftar!"
```

---

## 2. Auth - Login

**POST** `/api/auth/login`

### Request Body
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

### Response (200 OK)
```json
{
  "message": "Login berhasil!",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

### Response (400 Bad Request)
```json
"Email atau password salah!"
```

---

## 3. Events - Create Event

**POST** `/api/events`

### Request Body
```json
{
  "organizerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Konser BTS",
  "description": "Konser BTS di Jakarta",
  "category": "Musik",
  "venueName": "GBK Senayan",
  "bannerUrl": "https://example.com/banner.jpg",
  "startDate": "2025-12-25T19:00:00",
  "endDate": "2025-12-25T23:00:00"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| organizerId | UUID | Yes | User ID organizer |
| title | String | Yes | Max 150 chars |
| description | String | No | Text |
| category | String | No | Max 50 chars |
| venueName | String | No | Max 150 chars |
| bannerUrl | String | No | URL gambar |
| startDate | DateTime | Yes | Format: yyyy-MM-dd'T'HH:mm:ss |
| endDate | DateTime | Yes | Harus setelah startDate |

### Response (200 OK)
```json
{
  "eventId": "660e8400-e29b-41d4-a716-446655440000",
  "organizerId": "550e8400-e29b-41d4-a716-446655440000",
  "organizerName": "John Doe",
  "title": "Konser BTS",
  "description": "Konser BTS di Jakarta",
  "category": "Musik",
  "venueName": "GBK Senayan",
  "bannerUrl": "https://example.com/banner.jpg",
  "startDate": "2025-12-25T19:00:00",
  "endDate": "2025-12-25T23:00:00",
  "status": "PUBLISHED"
}
```

---

## 4. Events - Get All Published Events

**GET** `/api/events`

### Response (200 OK)
```json
[
  {
    "eventId": "660e8400-e29b-41d4-a716-446655440000",
    "organizerId": "550e8400-e29b-41d4-a716-446655440000",
    "organizerName": "John Doe",
    "title": "Konser BTS",
    "description": "Konser BTS di Jakarta",
    "category": "Musik",
    "venueName": "GBK Senayan",
    "bannerUrl": "https://example.com/banner.jpg",
    "startDate": "2025-12-25T19:00:00",
    "endDate": "2025-12-25T23:00:00",
    "status": "PUBLISHED"
  },
  {
    "eventId": "770e8400-e29b-41d4-a716-446655440000",
    "organizerId": "550e8400-e29b-41d4-a716-446655440000",
    "organizerName": "John Doe",
    "title": "Festival Musik",
    "description": "Festival musik terbesar",
    "category": "Festival",
    "venueName": "Jakarta International Expo",
    "bannerUrl": "https://example.com/banner2.jpg",
    "startDate": "2025-12-31T18:00:00",
    "endDate": "2025-12-31T23:59:00",
    "status": "PUBLISHED"
  }
]
```

---

## 5. Events - Get Event By ID

**GET** `/api/events/{eventId}`

### Response (200 OK)
```json
{
  "eventId": "660e8400-e29b-41d4-a716-446655440000",
  "organizerId": "550e8400-e29b-41d4-a716-446655440000",
  "organizerName": "John Doe",
  "title": "Konser BTS",
  "description": "Konser BTS di Jakarta",
  "category": "Musik",
  "venueName": "GBK Senayan",
  "bannerUrl": "https://example.com/banner.jpg",
  "startDate": "2025-12-25T19:00:00",
  "endDate": "2025-12-25T23:00:00",
  "status": "PUBLISHED"
}
```

### Response (400 Bad Request)
```json
"Event tidak ditemukan!"
```

---

## 6. Orders - Create Order

**POST** `/api/orders`

### Request Body
```json
{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "660e8400-e29b-41d4-a716-446655440000",
  "tierId": "880e8400-e29b-41d4-a716-446655440000",
  "attendees": [
    {
      "name": "John Doe",
      "nik": "3201234567890123"
    },
    {
      "name": "Jane Doe",
      "nik": "3201234567890124"
    }
  ]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| customerId | UUID | Yes | User ID customer |
| eventId | UUID | Yes | Event ID |
| tierId | UUID | Yes | Ticket Tier ID |
| attendees | Array | Yes | List attendees (min 1) |
| attendees[].name | String | Yes | Nama attendee |
| attendees[].nik | String | Yes | NIK attendee |

### Response (200 OK)
```json
{
  "orderId": "990e8400-e29b-41d4-a716-446655440000",
  "orderNumber": "ORD-1703123456789",
  "customer": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "event": {
    "eventId": "660e8400-e29b-41d4-a716-446655440000",
    "title": "Konser BTS"
  },
  "totalAmount": 255000,
  "adminFee": 5000,
  "status": "PENDING",
  "createdAt": "2025-12-20T10:00:00",
  "expiredAt": "2025-12-20T10:15:00"
}
```

### Response (400 Bad Request)
```json
"Customer tidak ditemukan!"
```
```json
"Event tidak ditemukan!"
```
```json
"Tier tidak ditemukan!"
```
```json
"Melebihi batas maksimal pembelian tiket per user!"
```
```json
"Kuota tiket tidak mencukupi!"
```

### Catatan Penting
- Order akan otomatis **EXPIRED** setelah menit yang diatur di settings (default 15 menit)
- Admin fee otomatis ditambahkan dari settings (default Rp 5.000)
- Kedua nilai bisa diubah via API Settings

---

## 7. Payments - Pay Order

**POST** `/api/payments/pay/{orderId}?paymentMethod=BCA`

### Query Parameters
| Parameter | Type | Required | Notes |
|---|---|---|---|
| paymentMethod | String | Yes | BCA, BRI, MANDIRI, GOPAY, OVO, dll |

### Response (200 OK)
```json
{
  "paymentId": "aa0e8400-e29b-41d4-a716-446655440000",
  "order": {
    "orderId": "990e8400-e29b-41d4-a716-446655440000",
    "orderNumber": "ORD-1703123456789",
    "status": "SUCCESS"
  },
  "paymentMethod": "BCA",
  "paymentStatus": "SUCCESS",
  "transactionIdGateway": "TRX-GW-a1b2c3d4",
  "paidAt": "2025-12-20T10:05:00"
}
```

### Response (400 Bad Request)
```json
"Order tidak ditemukan!"
```
```json
"Order sudah tidak valid atau kedaluwarsa!"
```

---

## 8. Settings - Get All Settings

**GET** `/api/settings`

### Response (200 OK)
```json
[
  {
    "settingKey": "ADMIN_FEE",
    "settingValue": "5000",
    "description": "Admin fee per order (Rp)"
  },
  {
    "settingKey": "ORDER_EXPIRY_MINUTES",
    "settingValue": "15",
    "description": "Order expiry time in minutes"
  }
]
```

---

## 9. Settings - Get Setting By Key

**GET** `/api/settings/{key}`

### Path Parameters
| Parameter | Type | Required | Notes |
|---|---|---|---|
| key | String | Yes | ADMIN_FEE or ORDER_EXPIRY_MINUTES |

### Response (200 OK)
```json
{
  "settingKey": "ADMIN_FEE",
  "settingValue": "5000",
  "description": "Admin fee per order (Rp)"
}
```

### Response (400 Bad Request)
```json
"Setting tidak ditemukan: INVALID_KEY"
```

---

## 10. Settings - Update Setting

**PUT** `/api/settings/{key}`

### Path Parameters
| Parameter | Type | Required | Notes |
|---|---|---|---|
| key | String | Yes | ADMIN_FEE or ORDER_EXPIRY_MINUTES |

### Request Body
```json
{
  "value": "10000",
  "description": "Admin fee dinaikkan"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| value | String | Yes | Nilai baru |
| description | String | No | Deskripsi (opsional) |

### Response (200 OK)
```json
{
  "settingKey": "ADMIN_FEE",
  "settingValue": "10000",
  "description": "Admin fee dinaikkan"
}
```

### Response (400 Bad Request)
```json
"Setting tidak ditemukan: INVALID_KEY"
```

---

## 11. Tickets - Check-in / Scan Ticket

**POST** `/api/tickets/scan/{ticketItemId}`

### Response (200 OK)
```json
{
  "ticketItemId": "bb0e8400-e29b-41d4-a716-446655440000",
  "order": {
    "orderId": "990e8400-e29b-41d4-a716-446655440000",
    "orderNumber": "ORD-1703123456789"
  },
  "tier": {
    "tierId": "880e8400-e29b-41d4-a716-446655440000",
    "tierName": "VIP"
  },
  "ticketCode": "TKT-a1b2c3d4",
  "attendeeName": "John Doe",
  "attendeeNik": "3201234567890123",
  "checkInStatus": "REDEEMED",
  "checkInAt": "2025-12-25T18:55:00"
}
```

### Response (400 Bad Request)
```json
"Tiket tidak valid / Tidak ditemukan!"
```
```json
"Tiket belum lunas / Pembayaran gagal!"
```
```json
"Gagal: Tiket sudah pernah di-scan pada 2025-12-25T18:55:00"
```

---

## Enum Values

### User.Role
| Value | Description |
|---|---|
| ADMIN | Administrator |
| ORGANIZER | Event Organizer |
| CUSTOMER | Customer (default) |

### User.KycStatus
| Value | Description |
|---|---|
| UNVERIFIED | Belum verifikasi (default) |
| PENDING | Sedang diproses |
| VERIFIED | Sudah diverifikasi |
| REJECTED | Ditolak |

### Event.EventStatus
| Value | Description |
|---|---|
| DRAFT | Belum dipublikasi |
| PUBLISHED | Sudah dipublikasi |
| CANCELLED | Dibatalkan |
| CLOSED | Selesai |

### Order.OrderStatus
| Value | Description |
|---|---|
| PENDING | Menunggu pembayaran |
| SUCCESS | Pembayaran berhasil |
| EXPIRED | Lewat waktu expiry (configurable), otomatis expired |
| CANCELLED | Dibatalkan |

### Payment.PaymentStatus
| Value | Description |
|---|---|
| PENDING | Menunggu pembayaran |
| SUCCESS | Pembayaran berhasil |
| FAILED | Pembayaran gagal |

### TicketItem.CheckInStatus
| Value | Description |
|---|---|
| UNREDEEMED | Belum di-scan |
| REDEEMED | Sudah di-scan |

### RefundRequest.RefundStatus
| Value | Description |
|---|---|
| PENDING | Menunggu approval |
| APPROVED | Disetujui |
| REJECTED | Ditolak |
| REFUNDED | Sudah direfund |

### RescheduleRequest.RescheduleStatus
| Value | Description |
|---|---|
| PENDING | Menunggu approval |
| APPROVED | Disetujui |
| REJECTED | Ditolak |

---

## Flow Diagram

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Register   │    │    Login     │    │  Get Events  │
│  /api/auth   │───▶│  /api/auth   │───▶│  /api/events │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                              ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Check-in    │    │    Pay      │    │Create Order  │
│  /api/tickets│◀───│ /api/payments│◀───│  /api/orders │
└─────────────┘    └─────────────┘    └─────────────┘
       │                  │                   │
       │                  │                   │
       ▼                  ▼                   ▼
  REDEEMED           SUCCESS             PENDING → EXPIRED
                                            (configurable)
```

---

## Notes for Frontend

1. **Tidak ada Authentication** - Semua endpoint bisa diakses tanpa token
2. **Format DateTime** - Gunakan format ISO: `yyyy-MM-dd'T'HH:mm:ss`
3. **UUID** - Semua ID menggunakan UUID v4
4. **Admin Fee** - Bisa diubah via `PUT /api/settings/ADMIN_FEE` (default Rp 5.000)
5. **Order Expiry** - Bisa diubah via `PUT /api/settings/ORDER_EXPIRY_MINUTES` (default 15 menit)
6. **Cron Job** - Server otomatis cancel order expired setiap 60 detik
7. **Settings API** - Untuk admin panel, gunakan `/api/settings` untuk kelola konfigurasi
