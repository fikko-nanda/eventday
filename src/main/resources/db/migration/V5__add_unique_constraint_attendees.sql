-- Migration: Add unique constraint to prevent duplicate attendees per order
-- Prevents double ticket generation when same attendee is submitted multiple times

ALTER TABLE attendees 
ADD CONSTRAINT uk_attendee_order_email UNIQUE (order_id, email);

-- Optional: Also add unique constraint on ticket_items for extra safety
-- ALTER TABLE ticket_items ADD CONSTRAINT uk_ticket_order_attendee UNIQUE (order_id, attendee_email);