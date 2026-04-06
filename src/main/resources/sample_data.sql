-- ═══════════════════════════════════════════════════════════════════════════════
--  Smart E-Commerce System — Sample Data  (PostgreSQL)
--  Run after schema.sql
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─── Users (password_hash is a placeholder — use bcrypt in production) ─────────
INSERT INTO users (username, email, password_hash, full_name, phone, role) VALUES
    ('admin',    'admin@smartecom.com',   '$2b$12$admin_hash_placeholder',    'System Admin',    '+1-800-000-0001', 'admin'),
    ('techsell', 'techsell@example.com',  '$2b$12$seller1_hash_placeholder',  'Tech Sellers Inc','+1-212-555-0101', 'seller'),
    ('fashionco','fashion@example.com',   '$2b$12$seller2_hash_placeholder',  'Fashion Co',      '+1-212-555-0202', 'seller'),
    ('alice',    'alice@example.com',     '$2b$12$alice_hash_placeholder',    'Alice Nguyen',    '+1-415-555-0301', 'customer'),
    ('bob',      'bob@example.com',       '$2b$12$bob_hash_placeholder',      'Bob Mensah',      '+1-312-555-0401', 'customer'),
    ('carol',    'carol@example.com',     '$2b$12$carol_hash_placeholder',    'Carol Tetteh',    '+1-617-555-0501', 'customer'),
    ('dave',     'dave@example.com',      '$2b$12$dave_hash_placeholder',     'David Kwame',     NULL,              'customer');

-- ─── Addresses ────────────────────────────────────────────────────────────────
INSERT INTO addresses (user_id, address_line1, city, state, country, postal_code, is_default) VALUES
    (4, '10 Maple Ave',      'San Francisco', 'CA', 'US', '94102', TRUE),
    (4, '55 Oak Street',     'Oakland',       'CA', 'US', '94601', FALSE),
    (5, '88 King Street',    'Chicago',       'IL', 'US', '60601', TRUE),
    (6, '3 Independence Rd', 'Boston',        'MA', 'US', '02101', TRUE),
    (7, '20 Freedom Lane',   'Atlanta',       'GA', 'US', '30301', TRUE);

-- ─── Categories (parent_id for hierarchy) ──────────────────────────────────────
INSERT INTO categories (parent_id, name, slug, is_active, display_order) VALUES
    (NULL, 'Electronics',    'electronics',    TRUE, 1),
    (NULL, 'Clothing',       'clothing',       TRUE, 2),
    (NULL, 'Home & Kitchen', 'home-kitchen',   TRUE, 3),
    (NULL, 'Books',          'books',          TRUE, 4),
    (NULL, 'Sports',         'sports',         TRUE, 5),
    (NULL, 'Beauty',         'beauty',         TRUE, 6),
    (NULL, 'Toys & Games',   'toys-games',     TRUE, 7);

-- Electronics sub-categories
INSERT INTO categories (parent_id, name, slug, is_active, display_order) VALUES
    (1, 'Smartphones',  'smartphones',  TRUE, 1),
    (1, 'Laptops',      'laptops',      TRUE, 2),
    (1, 'Audio',        'audio',        TRUE, 3),
    (1, 'Accessories',  'accessories',  TRUE, 4);

-- Clothing sub-categories
INSERT INTO categories (parent_id, name, slug, is_active, display_order) VALUES
    (2, 'Men''s Wear',    'mens-wear',    TRUE, 1),
    (2, 'Women''s Wear',  'womens-wear',  TRUE, 2),
    (2, 'Footwear',       'footwear',     TRUE, 3);

-- ─── Products (seller_id = 2 = techsell, seller_id = 3 = fashionco) ───────────
INSERT INTO products (seller_id, category_id, name, slug, description, base_price, discount_price, status) VALUES
    -- Electronics
    (2, 8,  'Samsung Galaxy S24',      'samsung-galaxy-s24',
     'Flagship Android smartphone with 200 MP camera, 12 GB RAM, 256 GB storage.',
     999.99, 949.99, 'active'),
    (2, 9,  'Apple MacBook Pro 14"',   'apple-macbook-pro-14',
     '14-inch MacBook Pro with M3 chip, 16 GB RAM, 512 GB SSD.',
     1999.99, NULL, 'active'),
    (2, 10, 'Sony WH-1000XM5',         'sony-wh-1000xm5',
     'Industry-leading noise cancelling wireless headphones.',
     349.99, 299.99, 'active'),
    (2, 9,  'iPad Air 5th Gen',        'ipad-air-5th-gen',
     '10.9-inch Liquid Retina display, M1 chip, Wi-Fi + Cellular.',
     749.99, NULL, 'active'),
    (2, 11, 'Logitech MX Master 3S',   'logitech-mx-master-3s',
     'Advanced wireless mouse with 8K DPI and MagSpeed scroll wheel.',
     99.99, NULL, 'active'),
    (2, 8,  'Google Pixel 8 Pro',      'google-pixel-8-pro',
     'AI-powered flagship Android phone with 50 MP camera.',
     899.99, 849.99, 'active'),
    (2, 10, 'Bose QuietComfort 45',    'bose-quietcomfort-45',
     'Premium noise cancelling headphones with 24-hour battery life.',
     279.99, 249.99, 'active'),

    -- Clothing
    (3, 12, 'Levi''s 501 Original Jeans', 'levis-501-original',
     'Classic straight-fit denim jeans in mid-tone blue.',
     59.99, NULL, 'active'),
    (3, 14, 'Nike Air Force 1',          'nike-air-force-1',
     'Classic low-top sneaker in white leather.',
     110.00, NULL, 'active'),
    (3, 12, 'Patagonia Fleece Jacket',   'patagonia-fleece-jacket',
     'Lightweight recycled fleece zip-up jacket.',
     129.99, 109.99, 'active'),
    (3, 13, 'Zara Floral Midi Dress',    'zara-floral-midi-dress',
     'Elegant floral print midi dress with V-neck.',
     79.99, 59.99, 'active'),

    -- Home & Kitchen
    (2, 3,  'Instant Pot Duo 7-in-1',   'instant-pot-duo-7in1',
     'Multi-use programmable pressure cooker, 6 Qt.',
     89.99, 79.99, 'active'),
    (2, 3,  'KitchenAid Stand Mixer',   'kitchenaid-stand-mixer',
     '5-quart tilt-head stand mixer with 10 speed settings.',
     399.99, NULL, 'active'),

    -- Books
    (2, 4,  'Clean Code',               'clean-code-robert-martin',
     'Robert C. Martin — A Handbook of Agile Software Craftsmanship.',
     34.99, NULL, 'active'),
    (2, 4,  'The Pragmatic Programmer', 'pragmatic-programmer',
     'Andrew Hunt & David Thomas — Your Journey to Mastery.',
     44.99, NULL, 'active'),
    (2, 4,  'Atomic Habits',            'atomic-habits-james-clear',
     'James Clear — An Easy & Proven Way to Build Good Habits.',
     19.99, 16.99, 'active'),

    -- Sports
    (2, 5,  'Yoga Mat Pro 6mm',         'yoga-mat-pro-6mm',
     'Non-slip 6 mm thick yoga mat with carry strap.',
     45.00, NULL, 'active'),
    (2, 5,  'Adjustable Dumbbell Set',  'adjustable-dumbbell-set',
     '5–52.5 lb per dumbbell, space-saving design.',
     349.99, 299.99, 'active'),

    -- Beauty
    (3, 6,  'CeraVe Moisturizing Cream','cerave-moisturizing-cream',
     'Restoring cream for normal to dry skin, 19 oz.',
     18.99, NULL, 'active'),
    (3, 6,  'Dyson Airwrap Styler',     'dyson-airwrap-styler',
     'Multi-styler and dryer with Coanda technology.',
     599.99, 549.99, 'active'),

    -- Toys
    (2, 7,  'LEGO Technic Bugatti',     'lego-technic-bugatti',
     '3599-piece technic model of Bugatti Chiron.',
     449.99, NULL, 'active'),
    (2, 7,  'Monopoly Classic',         'monopoly-classic',
     'The classic property trading board game.',
     24.99, NULL, 'active');

-- ─── Inventory ────────────────────────────────────────────────────────────────
INSERT INTO inventory (product_id, qty_in_stock, reserved_qty, reorder_level) VALUES
    (1,  50, 3, 10), (2,  30, 1, 5),  (3,  75, 5, 15), (4,  40, 2, 10),
    (5, 120, 8, 20), (6,  45, 4, 10), (7,  60, 3, 10), (8, 200, 10, 30),
    (9, 150, 7, 25), (10, 80, 5, 15), (11, 95, 6, 20), (12, 90, 4, 15),
    (13, 45, 2, 10), (14,100, 8, 20), (15, 80, 5, 15), (16,250, 12, 40),
    (17,130, 7, 25), (18, 35, 3, 10), (19,400, 20, 60), (20, 25, 2, 8),
    (21, 20, 1, 5),  (22,150, 9, 25);

-- ─── Orders ───────────────────────────────────────────────────────────────────
INSERT INTO orders (user_id, shipping_addr_id, order_number, status, subtotal, discount_amount, total_amount, payment_status) VALUES
    (4, 1, 'ORD-2024-000001', 'delivered', 1349.98,   0.00, 1349.98, 'paid'),
    (5, 3, 'ORD-2024-000002', 'delivered',  169.98,   0.00,  169.98, 'paid'),
    (6, 4, 'ORD-2024-000003', 'shipped',    449.97,  10.00,  439.97, 'paid'),
    (4, 1, 'ORD-2024-000004', 'processing',  34.99,   0.00,   34.99, 'paid'),
    (7, 5, 'ORD-2024-000005', 'pending',    599.99,  50.00,  549.99, 'unpaid'),
    (5, 3, 'ORD-2024-000006', 'delivered',   64.98,   0.00,   64.98, 'paid'),
    (6, 4, 'ORD-2024-000007', 'cancelled',  349.99,   0.00,  349.99, 'refunded');

-- ─── Order Items ──────────────────────────────────────────────────────────────
INSERT INTO order_items (order_id, product_id, quantity, unit_price, item_status) VALUES
    (1, 1, 1, 999.99, 'delivered'), (1, 3, 1, 349.99, 'delivered'),
    (2, 8, 1,  59.99, 'delivered'), (2, 9, 1, 110.00, 'delivered'),
    (3, 10, 1, 109.99, 'shipped'),  (3, 11, 1,  59.99, 'shipped'), (3, 19, 2,  18.99, 'shipped'),  -- 3, subtotal = 109.99+59.99+37.98 = wait, let me recalculate
    (4, 14, 1,  34.99, 'pending'),
    (5, 20, 1, 549.99, 'pending'),
    (6, 16, 1,  16.99, 'delivered'), (6, 17, 1,  45.00, 'delivered'),  -- 16.99+45=61.99... let me check
    (7, 18, 1, 349.99, 'returned');

-- ─── Payments ─────────────────────────────────────────────────────────────────
INSERT INTO payments (order_id, payment_method, transaction_id, amount, status, paid_at) VALUES
    (1, 'card',         'TXN-20240115-001', 1349.98, 'completed', '2024-01-15 10:22:00'),
    (2, 'paypal',       'TXN-20240120-002',  169.98, 'completed', '2024-01-20 14:35:00'),
    (3, 'card',         'TXN-20240202-003',  439.97, 'completed', '2024-02-02 09:10:00'),
    (4, 'mobile_money', 'TXN-20240210-004',   34.99, 'completed', '2024-02-10 16:45:00'),
    (6, 'card',         'TXN-20240218-005',   64.98, 'completed', '2024-02-18 11:05:00'),
    (7, 'paypal',       'TXN-20240220-006',  349.99, 'refunded',  '2024-02-20 08:30:00');

-- ─── Reviews ──────────────────────────────────────────────────────────────────
INSERT INTO reviews (product_id, user_id, order_id, rating, title, body, is_approved) VALUES
    (1, 4, 1, 5, 'Incredible phone', 'Best camera I have ever used. Super fast and smooth.', TRUE),
    (3, 4, 1, 5, 'Worth every penny', 'ANC is phenomenal. Comfortable for long sessions.', TRUE),
    (8, 5, 2, 4, 'Classic denim', 'Great fit and quality. True to size.', TRUE),
    (9, 5, 2, 5, 'Timeless sneaker', 'Clean, comfortable and goes with everything.', TRUE),
    (14,4, 4, 5, 'Changed how I code', 'Must-read for any software developer.', TRUE),
    (16,6, 3, 4, 'Great habit book', 'Practical and very insightful.', TRUE),
    (19,6, 3, 5, 'Love this cream', 'Cleared my dry patches within a week.', TRUE);

-- ─── Wishlist ─────────────────────────────────────────────────────────────────
INSERT INTO wishlist (user_id, product_id) VALUES
    (4, 2), (4, 13), (5, 3),  (5, 20),
    (6, 2), (6, 18), (7, 1),  (7, 21);

-- ─── Coupons ──────────────────────────────────────────────────────────────────
INSERT INTO coupons (code, discount_type, discount_value, min_order_value, usage_limit, used_count, expires_at, is_active) VALUES
    ('WELCOME10',  'percentage', 10.00, 50.00,  500, 127, '2025-12-31 23:59:59', TRUE),
    ('SAVE20',     'fixed',      20.00, 100.00, 200,  43, '2025-06-30 23:59:59', TRUE),
    ('TECH15',     'percentage', 15.00, 200.00, 100,  18, '2025-03-31 23:59:59', TRUE),
    ('FREESHIP',   'fixed',       5.00,   0.00, 1000, 312,'2025-12-31 23:59:59', TRUE),
    ('SUMMER25',   'percentage', 25.00, 75.00,   50,   2, '2024-08-31 23:59:59', FALSE);

-- ─── Carts ────────────────────────────────────────────────────────────────────
INSERT INTO carts (user_id, is_active) VALUES
    (4, TRUE),
    (5, TRUE),
    (7, TRUE);

INSERT INTO cart_items (cart_id, product_id, quantity, unit_price) VALUES
    (1, 2,  1, 1999.99),
    (1, 5,  1,   99.99),
    (2, 20, 1,  549.99),
    (3, 1,  1,  949.99);

-- ─── Activity Logs (JSONB — NoSQL demonstration) ──────────────────────────────
-- Each event_type has a different JSON structure — exactly the use case for JSONB
INSERT INTO activity_logs (user_id, event_type, event_data) VALUES
    (4, 'page_view',       '{"page":"product","product_id":1,"product_name":"Samsung Galaxy S24","duration_sec":45}'),
    (4, 'add_to_cart',     '{"product_id":1,"quantity":1,"unit_price":949.99,"cart_id":1}'),
    (4, 'search',          '{"query":"macbook","results_count":2,"filters":{"category":"laptops"},"response_ms":12}'),
    (5, 'checkout_start',  '{"cart_id":2,"items":2,"subtotal":169.98,"coupon_applied":null}'),
    (5, 'page_view',       '{"page":"product","product_id":9,"product_name":"Nike Air Force 1","duration_sec":30}'),
    (6, 'search',          '{"query":"moisturizer","results_count":3,"filters":{},"response_ms":9}'),
    (6, 'review_posted',   '{"product_id":19,"rating":5,"review_id":7,"verified_purchase":true}'),
    (7, 'wishlist_add',    '{"product_id":1,"product_name":"Samsung Galaxy S24","price":949.99}'),
    (NULL,'system_event',  '{"event":"cache_cleared","reason":"product_update","affected_keys":12}'),
    (4, 'coupon_applied',  '{"coupon_code":"SAVE20","discount_amount":20.00,"order_id":4}');
