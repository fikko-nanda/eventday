-- V2: Performance indexes + is_featured default untuk Admin features
-- Index tambahan untuk query admin (filter, search, pagination)

-- Events
CREATE INDEX IF NOT EXISTS idx_events_status_organizer ON events(status, organizer_id);
CREATE INDEX IF NOT EXISTS idx_events_start_date ON events(start_date);
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category);

-- Ticket Items
CREATE INDEX IF NOT EXISTS idx_ticket_items_event_status ON ticket_items(event_id, check_in_status);
CREATE INDEX IF NOT EXISTS idx_ticket_items_created ON ticket_items(created_at DESC);

-- Orders
CREATE INDEX IF NOT EXISTS idx_orders_event_status ON orders(event_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(status, created_at DESC);

-- Default is_featured = false jika belum ada
UPDATE events SET is_featured = false WHERE is_featured IS NULL;
