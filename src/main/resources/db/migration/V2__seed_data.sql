-- =====================================================================
-- V2__seed_data.sql
-- Sample data for local development / demo / Postman collection.
--
-- All seeded users share the same demo password: Password123!
-- (BCrypt hash below is a real hash of that password, strength 10 —
-- verifiable with Spring Security's BCryptPasswordEncoder out of the box.)

-- Users
INSERT INTO users (email, full_name, password_hash, role) VALUES
    ('admin@ticketbooking.com',    'System Admin',   '$2b$10$TcYu4ArrHNulN.Lmh73vJOJ4L2qiEmrInUxAVNwPzxRFkGveUfpd2', 'ADMIN'),
    ('operator@ticketbooking.com', 'Ops Operator',   '$2b$10$TcYu4ArrHNulN.Lmh73vJOJ4L2qiEmrInUxAVNwPzxRFkGveUfpd2', 'OPERATOR'),
    ('alice@example.com',          'Alice Nguyen',   '$2b$10$TcYu4ArrHNulN.Lmh73vJOJ4L2qiEmrInUxAVNwPzxRFkGveUfpd2', 'CUSTOMER'),
    ('bob@example.com',            'Bob Tran',       '$2b$10$TcYu4ArrHNulN.Lmh73vJOJ4L2qiEmrInUxAVNwPzxRFkGveUfpd2', 'CUSTOMER');


-- Concerts
-- ---------------------------------------------------------------------
-- Concert 1: the flash-sale headline event, ON_SALE, near-term date,
-- deliberately small quantities so it's easy to demo/test oversell
-- protection without needing to fire thousands of requests.
INSERT INTO concerts (title, description, venue, event_date, status, created_by) VALUES
    ('Neon Nights Live', 'Flash-sale headline concert for launch week.', 'Saigon Exhibition Center',
     CURRENT_TIMESTAMP + INTERVAL '14 days', 'ON_SALE',
     (SELECT id FROM users WHERE email = 'operator@ticketbooking.com'));

-- Concert 2: a second, less urgent concert to demonstrate multi-concert
-- browsing / filtering, still UPCOMING (not yet on sale).
INSERT INTO concerts (title, description, venue, event_date, status, created_by) VALUES
    ('Acoustic Sessions Vol. 3', 'Intimate acoustic evening.', 'The Vee Lounge',
     CURRENT_TIMESTAMP + INTERVAL '45 days', 'UPCOMING',
     (SELECT id FROM users WHERE email = 'operator@ticketbooking.com'));

-- Concert 3: already ended, useful for testing that booking is rejected
-- once a concert is no longer ON_SALE.
INSERT INTO concerts (title, description, venue, event_date, status, created_by) VALUES
    ('Retro Wave Festival', 'Past event, kept for booking-history demo.', 'District 7 Amphitheatre',
     CURRENT_TIMESTAMP - INTERVAL '10 days', 'ENDED',
     (SELECT id FROM users WHERE email = 'operator@ticketbooking.com'));


-- Ticket Categories

-- Neon Nights Live (small quantities on purpose, see note above)
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity) VALUES
    ((SELECT id FROM concerts WHERE title = 'Neon Nights Live'), 'VIP',       2500000, 50,  50),
    ((SELECT id FROM concerts WHERE title = 'Neon Nights Live'), 'Standard',  900000,  200, 200),
    ((SELECT id FROM concerts WHERE title = 'Neon Nights Live'), 'Economy',   450000,  300, 300);

-- Acoustic Sessions Vol. 3
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity) VALUES
    ((SELECT id FROM concerts WHERE title = 'Acoustic Sessions Vol. 3'), 'VIP',      1500000, 30, 30),
    ((SELECT id FROM concerts WHERE title = 'Acoustic Sessions Vol. 3'), 'Standard', 600000,  100, 100);

-- Retro Wave Festival (ended — kept with 0 available to reflect sold out /
-- closed sale, useful for negative-path testing)
INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity) VALUES
    ((SELECT id FROM concerts WHERE title = 'Retro Wave Festival'), 'Standard', 700000, 150, 0);

-- Vouchers
-- ---------------------------------------------------------------------
-- Generic, high-quota voucher — always usable in demo/Postman flows.
INSERT INTO vouchers (code, discount_amount, min_order_value, total_quantity, used_quantity, start_date, end_date) VALUES
    ('WELCOME10', 100000, 500000, 1000, 0,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '90 days');

-- Scarce voucher — deliberately low quota, useful for demoing/testing
-- voucher-quota exhaustion under concurrent requests.
INSERT INTO vouchers (code, discount_amount, min_order_value, total_quantity, used_quantity, start_date, end_date) VALUES
    ('FLASH5', 200000, 1000000, 5, 0,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Expired voucher — useful for testing that expired vouchers are rejected.
INSERT INTO vouchers (code, discount_amount, min_order_value, total_quantity, used_quantity, start_date, end_date) VALUES
    ('EXPIRED20', 150000, 0, 100, 0,
     CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day');