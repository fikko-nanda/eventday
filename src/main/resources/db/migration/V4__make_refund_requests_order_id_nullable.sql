-- Migration untuk memperbaiki refund_requests.order_id menjadi nullable
-- Dibutuhkan karena tabel refund_requests dipakai bersama oleh:
-- 1. Refunds (customer) -> order_id = required (ada relasi ke orders)
-- 2. Payouts (organizer) -> order_id = null (tidak ada relasi, berdasar event_payout_balance)
-- Perubahan ini memungkinkan EO mengajukan payout tanpa harus punya order terkait langsung

-- ALTER TABLE refund_requests
ALTER TABLE refund_requests 
    ALTER COLUMN order_id DROP NOT NULL;

-- Verifikasi constraint sudah hilang
-- SELECT column_name, is_nullable FROM information_schema.columns 
-- WHERE table_name = 'refund_requests' AND column_name = 'order_id';
