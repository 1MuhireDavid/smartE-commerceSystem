-- ===========================================
--  Smart E-Commerce System — Full Database Schema  (PostgreSQL)
--  Run once against an empty database: smart_ecommerce
-- ==========================================================

-- ─── Drop existing objects (reverse FK order) ─────────────────────────────────
DROP TABLE IF EXISTS activity_logs   CASCADE;
DROP TABLE IF EXISTS wishlist        CASCADE;
DROP TABLE IF EXISTS reviews         CASCADE;
DROP TABLE IF EXISTS cart_items      CASCADE;
DROP TABLE IF EXISTS carts           CASCADE;
DROP TABLE IF EXISTS payments        CASCADE;
DROP TABLE IF EXISTS order_items     CASCADE;
DROP TABLE IF EXISTS orders          CASCADE;
DROP TABLE IF EXISTS inventory       CASCADE;
DROP TABLE IF EXISTS products        CASCADE;
DROP TABLE IF EXISTS categories      CASCADE;
DROP TABLE IF EXISTS users           CASCADE;
DROP FUNCTION IF EXISTS update_updated_at CASCADE;
DROP FUNCTION IF EXISTS update_last_updated CASCADE;


CREATE TABLE users (
    user_id       BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    address       VARCHAR(300),
    role          VARCHAR(10)  NOT NULL DEFAULT 'customer'
                               CHECK (role IN ('customer','seller','admin')),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_role     ON users (role);

-- ─── categories ───────────────────────────────────────────────────────────────
CREATE TABLE categories (
    category_id   SERIAL       PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INT          NOT NULL DEFAULT 0
);

-- ─── products ─────────────────────────────────────────────────────────────────
CREATE TABLE products (
    product_id     BIGSERIAL     PRIMARY KEY,
    seller_id      BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT,
    category_id    INT           REFERENCES categories (category_id) ON DELETE SET NULL,
    name           VARCHAR(200)  NOT NULL,
    slug           VARCHAR(220)  NOT NULL UNIQUE,
    description    TEXT,
    base_price     NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
    discount_price NUMERIC(10,2)          CHECK (discount_price IS NULL OR discount_price >= 0),
    status         VARCHAR(10)   NOT NULL DEFAULT 'draft'
                                 CHECK (status IN ('active','inactive','draft')),
    avg_rating     NUMERIC(3,2)  NOT NULL DEFAULT 0.00,
    review_count   INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_products_name_btree  ON products (lower(name));
CREATE INDEX idx_products_name_fts    ON products USING gin (to_tsvector('english', name));
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_seller_id   ON products (seller_id);
CREATE INDEX idx_products_status      ON products (status);

-- ─── inventory ────────────────────────────────────────────────────────────────
CREATE TABLE inventory (
    inventory_id  BIGSERIAL PRIMARY KEY,
    product_id    BIGINT    NOT NULL UNIQUE REFERENCES products (product_id) ON DELETE CASCADE,
    qty_in_stock  INT       NOT NULL DEFAULT 0  CHECK (qty_in_stock  >= 0),
    reserved_qty  INT       NOT NULL DEFAULT 0  CHECK (reserved_qty  >= 0),
    reorder_level INT       NOT NULL DEFAULT 10,
    last_updated  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reserved_lte_stock CHECK (reserved_qty <= qty_in_stock)
);

-- ─── orders ───────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    order_id         BIGSERIAL     PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES users     (user_id)     ON DELETE RESTRICT,
    order_number     VARCHAR(30)   NOT NULL UNIQUE,
    status           VARCHAR(15)   NOT NULL DEFAULT 'pending'
                                   CHECK (status IN ('pending','processing','completed','cancelled')),
    subtotal         NUMERIC(12,2) NOT NULL CHECK (subtotal       >= 0),
    discount_amount  NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    total_amount     NUMERIC(12,2) NOT NULL CHECK (total_amount   >= 0),
    payment_status   VARCHAR(10)   NOT NULL DEFAULT 'unpaid'
                                   CHECK (payment_status IN ('unpaid','paid','refunded')),
    ordered_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_total CHECK (total_amount = subtotal - discount_amount)
);
CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_ordered_at ON orders (ordered_at);

-- ─── order_items ──────────────────────────────────────────────────────────────
CREATE TABLE order_items (
    order_item_id BIGSERIAL     PRIMARY KEY,
    order_id      BIGINT        NOT NULL REFERENCES orders   (order_id)   ON DELETE CASCADE,
    product_id    BIGINT        NOT NULL REFERENCES products (product_id) ON DELETE RESTRICT,
    quantity      INT           NOT NULL CHECK (quantity    > 0),
    unit_price    NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    total_price   NUMERIC(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    item_status   VARCHAR(10)   NOT NULL DEFAULT 'pending'
                                CHECK (item_status IN ('pending','completed','returned'))
);
CREATE INDEX idx_order_items_order_id   ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- ─── payments ─────────────────────────────────────────────────────────────────
CREATE TABLE payments (
    payment_id     BIGSERIAL     PRIMARY KEY,
    order_id       BIGINT        NOT NULL REFERENCES orders (order_id) ON DELETE RESTRICT,
    payment_method VARCHAR(15)   NOT NULL
                                 CHECK (payment_method IN ('card','paypal','mobile_money','cash')),
    transaction_id VARCHAR(100)  UNIQUE,
    amount         NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status         VARCHAR(10)   NOT NULL DEFAULT 'pending'
                                 CHECK (status IN ('pending','completed','failed','refunded')),
    paid_at        TIMESTAMP
);
CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status   ON payments (status);

-- ─── carts ────────────────────────────────────────────────────────────────────
CREATE TABLE carts (
    cart_id    BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_carts_user_active ON carts (user_id, is_active);

-- ─── cart_items ───────────────────────────────────────────────────────────────
CREATE TABLE cart_items (
    cart_item_id BIGSERIAL     PRIMARY KEY,
    cart_id      BIGINT        NOT NULL REFERENCES carts    (cart_id)    ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products (product_id) ON DELETE CASCADE,
    quantity     INT           NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price   NUMERIC(10,2) NOT NULL,
    added_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id)
);

-- ─── reviews ──────────────────────────────────────────────────────────────────
CREATE TABLE reviews (
    review_id   BIGSERIAL  PRIMARY KEY,
    product_id  BIGINT     NOT NULL REFERENCES products (product_id) ON DELETE CASCADE,
    user_id     BIGINT     NOT NULL REFERENCES users    (user_id)    ON DELETE CASCADE,
    order_id    BIGINT              REFERENCES orders   (order_id)   ON DELETE SET NULL,
    rating      SMALLINT   NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(150),
    body        TEXT,
    is_approved BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_product_order UNIQUE (user_id, product_id, order_id)
);
CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_user_id    ON reviews (user_id);

-- ─── activity_logs  (NoSQL / JSONB) ───────────────────────────────────────────
-- Stores unstructured user events.  Each event_type carries its own payload
-- shape, making JSONB the right choice over fixed columns.
-- Examples: page_view, add_to_cart, search, checkout_started, review_posted
CREATE TABLE activity_logs (
    log_id     BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      REFERENCES users (user_id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB       NOT NULL,
    logged_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_activity_logs_user_id  ON activity_logs (user_id);
CREATE INDEX idx_activity_logs_type     ON activity_logs (event_type);
CREATE INDEX idx_activity_logs_gin      ON activity_logs USING gin (event_data);
CREATE INDEX idx_activity_logs_logged   ON activity_logs (logged_at);

-- ─── Trigger: auto-update updated_at ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS '
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_last_updated()
    RETURNS TRIGGER AS '
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users     FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products  FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts     FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_inventory_updated
    BEFORE UPDATE ON inventory FOR EACH ROW EXECUTE FUNCTION update_last_updated();
