-- 1. Tabel pengembalian dana pembeli (Customer Refund)
CREATE TABLE customer_refunds (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES users(user_id),
    order_id UUID NOT NULL REFERENCES orders(order_id),
    amount DECIMAL(12,2) NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. Tabel pencairan dana penyelenggara (Organizer Payout)
CREATE TABLE organizer_payouts (
    id BIGSERIAL PRIMARY KEY,
    organizer_id UUID NOT NULL REFERENCES organizers(organizer_id),
    event_id UUID REFERENCES events(event_id),
    amount DECIMAL(12,2) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    bank_account_number VARCHAR(35) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    processed_by_admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 3. Tabel akumulasi saldo real-time EO
CREATE TABLE eo_payout_balances (
    id BIGSERIAL PRIMARY KEY,
    organizer_id UUID NOT NULL REFERENCES organizers(organizer_id),
    event_id UUID REFERENCES events(event_id),
    gross_sales DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_admin_fees DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_refunds DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_balance DECIMAL(12,2) GENERATED ALWAYS AS (gross_sales - total_admin_fees - total_refunds) STORED,
    last_updated TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_organizer_event UNIQUE (organizer_id, event_id)
);