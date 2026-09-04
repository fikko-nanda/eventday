-- Eventday Database Schema Migration V1
-- PostgreSQL DDL untuk 10 tabel sesuai ERD

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. USERS TABLE
-- ============================================================
CREATE TABLE users (
    user_id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    nik             VARCHAR(16) UNIQUE,
    username        VARCHAR(20) NOT NULL DEFAULT '',
    role            VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       UUID,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      UUID
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_nik ON users(nik);

-- ============================================================
-- 2. AUTH TABLE (pisah dari users untuk keamanan)
-- ============================================================
CREATE TABLE auth (
    auth_id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    auth_google     VARCHAR(20),
    akses_token     TEXT,
    expired_token   TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       UUID,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      UUID,
    CONSTRAINT fk_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_user_id ON auth(user_id);
CREATE INDEX idx_auth_akses_token ON auth(akses_token) WHERE akses_token IS NOT NULL;

-- ============================================================
-- 3. ORGANIZERS TABLE
-- ============================================================
CREATE TABLE organizers (
    organizer_id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL UNIQUE,
    name_organizer      VARCHAR(100) NOT NULL,
    npwp_number         VARCHAR(25),
    akta_perusahaan     VARCHAR(255),
    bank_name           VARCHAR(50),
    bank_account_number VARCHAR(35),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           UUID,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          UUID,
    CONSTRAINT fk_organizer_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_organizer_user_id ON organizers(user_id);
CREATE INDEX idx_organizer_verification ON organizers(verification_status);

-- ============================================================
-- 4. EVENTS TABLE
-- ============================================================
CREATE TABLE events (
    event_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organizer_id   UUID NOT NULL,
    title          VARCHAR(150) NOT NULL,
    description    TEXT,
    category       VARCHAR(50),
    venue_name     VARCHAR(150),
    banner_url     VARCHAR(255),
    facility       TEXT,
    start_date     TIMESTAMP NOT NULL,
    end_date       TIMESTAMP NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      UUID,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     UUID,
    CONSTRAINT fk_event_organizer FOREIGN KEY (organizer_id) REFERENCES organizers(organizer_id) ON DELETE RESTRICT
);

CREATE INDEX idx_event_organizer ON events(organizer_id);
CREATE INDEX idx_event_status ON events(status);
CREATE INDEX idx_event_date ON events(start_date, end_date);

-- ============================================================
-- 5. TICKET_TIERS TABLE
-- ============================================================
CREATE TABLE ticket_tiers (
    tier_id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id         UUID NOT NULL,
    tier_name        VARCHAR(50) NOT NULL,
    price            NUMERIC(12,2) NOT NULL,
    total_quota      INTEGER NOT NULL,
    available_quota  INTEGER NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by        UUID,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       UUID,
    CONSTRAINT fk_tier_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE
);

CREATE INDEX idx_tier_event ON ticket_tiers(event_id);

-- ============================================================
-- 6. BOOKINGS TABLE (reservasi sementara sebelum order)
-- ============================================================
CREATE TABLE bookings (
    booking_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL,
    tier_id        UUID NOT NULL,
    quantity       INTEGER NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at     TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by      UUID,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     UUID,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_tier FOREIGN KEY (tier_id) REFERENCES ticket_tiers(tier_id) ON DELETE RESTRICT
);

CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_booking_tier ON bookings(tier_id);
CREATE INDEX idx_booking_status_expires ON bookings(status, expires_at);

-- ============================================================
-- 7. ORDERS TABLE (transaksi final setelah pembayaran)
-- ============================================================
CREATE TABLE orders (
    order_id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id             UUID UNIQUE,
    customer_id            UUID NOT NULL,
    event_id               UUID NOT NULL,
    tier_id                UUID NOT NULL,
    quantity               INTEGER NOT NULL,
    total_amount           NUMERIC(12,2) NOT NULL,
    admin_fee              NUMERIC(12,2) NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method         VARCHAR(50),
    transaction_id_gateway VARCHAR(100),
    paid_at                TIMESTAMP,
    expired_at             TIMESTAMP NOT NULL,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by              UUID,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             UUID,
    CONSTRAINT fk_order_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_event FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_tier FOREIGN KEY (tier_id) REFERENCES ticket_tiers(tier_id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_customer ON orders(customer_id);
CREATE INDEX idx_order_event ON orders(event_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_booking ON orders(booking_id);

-- ============================================================
-- 8. TICKET_ITEMS TABLE (tikett individual untuk check-in)
-- ============================================================
CREATE TABLE ticket_items (
    ticket_item_id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id         UUID NOT NULL,
    tier_id          UUID NOT NULL,
    attendee_email   VARCHAR(50),
    attendee_name    VARCHAR(100) NOT NULL,
    attendee_nik     VARCHAR(16) NOT NULL,
    check_in_status  VARCHAR(20) NOT NULL DEFAULT 'UNREDEEMED',
    check_in_at      TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by        UUID,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       UUID,
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_tier FOREIGN KEY (tier_id) REFERENCES ticket_tiers(tier_id) ON DELETE RESTRICT
);

CREATE INDEX idx_ticket_order ON ticket_items(order_id);
CREATE INDEX idx_ticket_tier ON ticket_items(tier_id);
CREATE INDEX idx_ticket_attendee_nik_tier ON ticket_items(attendee_nik, tier_id);
CREATE INDEX idx_ticket_checkin ON ticket_items(check_in_status);

-- ============================================================
-- 9. REFUND_REQUESTS TABLE
-- ============================================================
CREATE TABLE refund_requests (
    refund_id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id          UUID NOT NULL,
    order_id             UUID NOT NULL,
    refund_amount        NUMERIC(12,2) NOT NULL,
    bank_name            VARCHAR(50) NOT NULL,
    bank_account_number  VARCHAR(35) NOT NULL,
    bank_account_name    VARCHAR(100) NOT NULL,
    reason               TEXT NOT NULL,
    admin_note           TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at         TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by            UUID,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           UUID,
    CONSTRAINT fk_refund_customer FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE RESTRICT
);

CREATE INDEX idx_refund_customer ON refund_requests(customer_id);
CREATE INDEX idx_refund_order ON refund_requests(order_id);
CREATE INDEX idx_refund_status ON refund_requests(status);

-- ============================================================
-- 10. SETTINGS TABLE (konfigurasi sistem)
-- ============================================================
CREATE TABLE settings (
    settings_id   BIGSERIAL PRIMARY KEY,
    settings_key  VARCHAR(50) NOT NULL UNIQUE,
    settings_value VARCHAR(50) NOT NULL,
    description   TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     UUID,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    UUID
);

CREATE INDEX idx_settings_key ON settings(settings_key);

-- ============================================================
-- 11. AUDIT_LOGS TABLE (log aktivitas)
-- ============================================================
CREATE TABLE audit_logs (
    audit_id     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id     UUID,
    actor_name   VARCHAR(100),
    action       VARCHAR(100) NOT NULL,
    detail       TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

-- ============================================================
-- DEFAULT SETTINGS DATA
-- ============================================================
INSERT INTO settings (settings_key, settings_value, description) VALUES
('ADMIN_FEE', '5000', 'Admin fee per order (Rp)'),
('ORDER_EXPIRY_MINUTES', '15', 'Order expiry time in minutes'),
('BOOKING_EXPIRY_MINUTES', '10', 'Booking expiry time in minutes')
ON CONFLICT (settings_key) DO NOTHING;

-- ============================================================
-- TRIGGER UNTUK AUTO UPDATE updated_at (opsional, via application layer)
-- ============================================================
-- Catatan: updated_at di-handle di application layer (JPA @PreUpdate)
-- Jika ingin di DB level, gunakan trigger:
-- CREATE OR REPLACE FUNCTION update_updated_at_column()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = CURRENT_TIMESTAMP;
--     RETURN NEW;
-- END;
-- $$ language 'plpgsql';
-- 
-- CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
-- (ulangi untuk tabel lain yang perlu)