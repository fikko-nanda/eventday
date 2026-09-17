# Frontend Mock → Usable Guide (rev.11+)

> Semua endpoint MOCK sudah **hybrid DB+fallback** — frontend bisa langsung pakai tanpa menunggu backend selesai 100%. Jika data DB belum ada, response akan berisi `mock:true` atau fallback values, tapi tetap 200 (tidak 404/500).

## 1. Legal — FIXED (sekarang publik)

**Sebelum:** `GET /terms-conditions` butuh login (BUG path mismatch).
**Sesudah:** Dual alias → `GET /terms-conditions` **dan** `GET /api/terms-conditions` → **200 tanpa token**.

```js
fetch(`${BASE}/terms-conditions`) // BASE = http://localhost:8082  atau /api
fetch(`${BASE}/api/terms-conditions`) // kedua-duanya work
// Response: {msg, status, data: {title, slug, content: "<h1>...", sections: [...], updated_at}}
```

Same untuk `/privacy-policy`.

Frontend: render `data.content` (HTML) atau `data.sections.map(s => <h2>{s.heading}</h2><p>{s.body}</p>)`.

---

## 2. Refund Customer — HYBRID REAL

### Banks dropdown (8 bank enriched)

```js
const {data: banks} = await fetch(`${BASE}/api/refund/banks`, {credentials:'include'}).then(r=>r.json())
// banks = [
//   {bankCode:"BCA", bankName:"Bank Central Asia", logoUrl:"/assets/banks/bca.png", active:true},
//   {bankCode:"MANDIRI", ...}, {bankCode:"BSI", ...}, {bankCode:"DANAMON", ...} // total 8
// ]
// Render: <select>{banks.map(b=> <option value={b.bankCode}>{b.bankName}</option>)}</select>
```

**Usable:** Tidak perlu hardcode di frontend lagi. `logoUrl` optional untuk icon.

### Order summary (REAL dari Order DB)

```js
const {data} = await fetch(`${BASE}/api/refund/order-summary?orderId=${orderId}`, {credentials:'include'}).then(r=>r.json())
// data = {orderId, orderNumber:"ORD-AB12CD34", eventTitle, ticketTierName, ticketQuantity, grossAmount, adminFee, refundableAmount, status, expiredAt}
// refundableAmount = gross - adminFee (hitung real, bukan hardcoded 290k)
```

Jika `orderId` tidak ada di DB, fallback tetap 290k agar dev tidak block.

### Submit refund (amount auto dari Order)

```js
await fetch(`${BASE}/api/tickets/refund/request`, {
  method:'POST', credentials:'include',
  headers:{'Content-Type':'application/json'},
  body: JSON.stringify({orderId, reason:"Event batal", bankCode:"BCA", accountNumber:"123456", accountHolderName:"Budi"})
})
// amount otomatis hitung dari Order; organizerId otomatis dari Order.event.organizer
```

---

## 3. Organizer — 20 endpoint HYBRID DB+fallback

Semua `/organizer/*` sekarang **usable**:

| Flow | Endpoint | Catatan usable |
|------|----------|---------------|
| Register | `POST /organizer/register` | Body `{name, npwp_number, bank_name, bank_account_number}` → create DB PENDING, duplicate → return existing |
| Status | `GET /organizer/status` | Real verification_status dari DB, fallback PENDING |
| Dashboard | `GET /organizer/dashboard` | Real hitung myEvents + revenue/tickets dari Event+Order, fallback statis |
| Profile | `GET /organizer/profile` | Join Organizer+User real, fallback mock jika belum register |
| Update | `PUT /organizer/profile` | Update DB Organizer+User, return payload |
| Uploads | `POST /organizer/profile/avatar`, `/upload-portfolio`, `/upload-deed`, `/documents/upload` | Save file ke `uploads/...` → return `{avatar_url/document_url}` |
| Refunds | `GET /organizer/refunds`, `GET /organizer/refunds/detail?id=`, `PATCH /organizer/refunds/{id}/status` | Query RefundRequestEntity by organizerId, fallback global PENDING |
| Payout | `GET /organizer/payouts`, `POST /organizer/payouts`, `GET /organizer/payouts/detail?id=`, `GET /organizer/bank-accounts`, `GET /organizer/events/{id}/payout-balance` | Payout via RefundRequestEntity PENDING, balance aggregate real |

**Cara pakai untuk dev tanpa seed:**

```js
// 1. Login sebagai CUSTOMER dulu (organizer adalah role extension dari user)
await fetch(`${BASE}/api/auth/login`, {method:'POST', credentials:'include', body: JSON.stringify({identifier:"eo@mail.com", password:"123456"})})

// 2. Register organizer (sekali)
await fetch(`${BASE}/organizer/register`, {method:'POST', credentials:'include', body: JSON.stringify({name:"PT Event Saya"})})

// 3. Sekarang profile/dashboard/refs sudah real
const profile = await fetch(`${BASE}/organizer/profile`, {credentials:'include'}).then(r=>r.json())
// profile.data = {organizer_id, name, pic_name, email, phone, npwp, verification_status: "PENDING", ...}
```

Jika `verification_status` masih `PENDING`, Admin bisa approve via `PATCH /admin/eo-applications/{id}/status {status:"VERIFIED"}`.

**Upload file:**
```js
const fd = new FormData(); fd.append('file', fileInput.files[0]);
await fetch(`${BASE}/organizer/profile/avatar`, {method:'POST', credentials:'include', body: fd})
// → {avatar_url: "/uploads/avatars/<uuid>_file.png"} — bisa langsung <img src="/uploads/avatars/...">
```

File disimpan di `uploads/` dan diserve via `WebConfig` (`/uploads/**` → `file:uploads/`).

---

## 4. User Avatar — REAL

```js
const fd = new FormData(); fd.append('file', avatarFile);
const {data: url} = await fetch(`${BASE}/api/user/avatar`, {method:'POST', credentials:'include', body: fd}).then(r=>r.json())
// url = "/uploads/avatars/<uuid>_avatar.png"
<img src={`http://localhost:8082${url}`} />
```

Validasi: `image/*`, ≤5MB.

---

## 5. Checklist Frontend Integration

- [ ] Semua `fetch` pakai `credentials: 'include'` (wajib untuk cookie HttpOnly Partitioned)
- [ ] BASE = `http://localhost:8082/api` (lokal) atau `https://<ngrok>.ngrok-free.app/api` (lintas laptop) — jangan tanpa `/api`
- [ ] Event `facilities` masih `STRING` → `facilities.split(', ')` di frontend
- [ ] Checkout `initiate` → `attendees` → `process` → `charge` (Midtrans redirectUrl) → polling `orders/status`
- [ ] Refund banks pakai API (8 bank) jangan hardcode
- [ ] Organizer flow: register dulu sebelum akses dashboard/profile

---

## 6. PDF Laporan Lengkap

Lihat `docs/Laporan_Audit_Eventday_2026-09-16.pdf` — 9 halaman A4, berisi:
- Ringkasan 40 REAL / 3 PARTIAL / 24 MOCK→HYBRID / 2 FIXED / 4 RISK
- Tabel per modul + 80 endpoint rinci
- Permintaan UI/UX sudah vs belum
- Catatan sinkronisasi + curl siap pakai
