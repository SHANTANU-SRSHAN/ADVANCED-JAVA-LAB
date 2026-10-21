-- ============================================================
-- ShopSphere — MySQL database schema (v2)
--
-- HOW TO RUN (important — avoids the '$' mangling bug):
--     mysql -u root -p shopsphere < database/schema.sql
-- The '<' redirect is safe. Do NOT paste the contents into a
-- PowerShell double-quoted string ("...") — PowerShell expands
-- $2a$12$... as variables and silently corrupts the password hash.
-- ============================================================

CREATE DATABASE IF NOT EXISTS shopsphere
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE shopsphere;

-- ------------------------------------------------------------
-- Idempotent column helper.
--
-- MySQL has no "ADD COLUMN IF NOT EXISTS" (that is MariaDB), so this
-- procedure checks information_schema first and only adds the column
-- when it is really missing. It makes this script safe to run on a
-- fresh database AND on an existing v1 install.
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_column_if_not_exists(
    IN p_table      VARCHAR(64),
    IN p_column     VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table,
                          '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- Users (customers + admins)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id              INT AUTO_INCREMENT PRIMARY KEY,
    full_name            VARCHAR(100) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,      -- BCrypt
    phone                VARCHAR(15)  NULL,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    email_verified       TINYINT(1)   NOT NULL DEFAULT 0,
    failed_login_attempts INT         NOT NULL DEFAULT 0,
    locked_until         DATETIME     NULL,
    last_login_at        DATETIME     NULL,
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Upgrade an older install that lacks the new account columns.
CALL add_column_if_not_exists('users', 'phone',                 'VARCHAR(15) NULL');
CALL add_column_if_not_exists('users', 'email_verified',        'TINYINT(1) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('users', 'failed_login_attempts', 'INT NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('users', 'locked_until',          'DATETIME NULL');
CALL add_column_if_not_exists('users', 'last_login_at',         'DATETIME NULL');

-- ------------------------------------------------------------
-- Address book (a customer can save several addresses)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_addresses (
    address_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    label         VARCHAR(40)  NOT NULL DEFAULT 'Home',
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(15)  NOT NULL,
    address_line1 VARCHAR(300) NOT NULL,
    address_line2 VARCHAR(300) NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NULL,
    pincode       VARCHAR(10)  NOT NULL,
    is_default    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    KEY idx_addr_user (user_id),
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- "Remember me" — long-lived login tokens
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS remember_tokens (
    token_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,   -- SHA-256 of the cookie value
    expires_at DATETIME    NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    KEY idx_remember_user (user_id),
    CONSTRAINT fk_remember_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Password reset tokens (forgot-password flow)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at DATETIME    NOT NULL,
    used       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Email verification tokens (sent on registration)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at DATETIME    NOT NULL,
    used       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verify_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Products
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    product_id  INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    category    VARCHAR(100),
    price       DECIMAL(10,2) NOT NULL,
    stock       INT NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CALL add_column_if_not_exists('products', 'is_active', 'TINYINT(1) NOT NULL DEFAULT 1');

-- ------------------------------------------------------------
-- Categories
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Shipping methods (chosen at checkout)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shipping_methods (
    method_id        INT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)  NOT NULL UNIQUE,
    name             VARCHAR(60)  NOT NULL,
    description      VARCHAR(200) NULL,
    price            DECIMAL(10,2) NOT NULL DEFAULT 0,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_days         INT NOT NULL DEFAULT 5,
    is_active        TINYINT(1)   NOT NULL DEFAULT 1,
    sort_order       INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Shopping cart (one row per user + product)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    product_id   INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    added_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart (user_id, product_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Coupons
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id        INT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    description      VARCHAR(200) NULL,
    discount_type    VARCHAR(10)  NOT NULL DEFAULT 'PERCENT',  -- PERCENT | FIXED
    discount_value   DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_discount     DECIMAL(10,2) NULL,
    usage_limit      INT NULL,          -- total uses across the shop
    used_count       INT NOT NULL DEFAULT 0,
    per_user_limit   INT NOT NULL DEFAULT 1,
    valid_from       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to         DATETIME NULL,
    is_active        TINYINT(1)  NOT NULL DEFAULT 1,
    created_at       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Which customers already used which coupon (per-user limit).
CREATE TABLE IF NOT EXISTS coupon_redemptions (
    redemption_id INT AUTO_INCREMENT PRIMARY KEY,
    coupon_id     INT NOT NULL,
    user_id       INT NOT NULL,
    order_id      INT NULL,
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_redemption_user (user_id),
    CONSTRAINT fk_redemption_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,
    CONSTRAINT fk_redemption_user   FOREIGN KEY (user_id)   REFERENCES users(user_id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Orders
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id            INT AUTO_INCREMENT PRIMARY KEY,
    order_number        VARCHAR(20) NOT NULL UNIQUE,
    user_id             INT NOT NULL,
    subtotal_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    shipping_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(10,2) NOT NULL,
    order_status        VARCHAR(30) NOT NULL DEFAULT 'PLACED',
    -- PLACED | CONFIRMED | SHIPPED | DELIVERED | CANCELLED
    coupon_code         VARCHAR(50) NULL,
    shipping_method_code VARCHAR(30) NULL,
    shipping_method_name VARCHAR(60) NULL,
    -- Address snapshot (kept even if the address book changes)
    full_name           VARCHAR(100) NOT NULL DEFAULT '',
    phone               VARCHAR(20)  NULL,
    address_line1       VARCHAR(300) NOT NULL DEFAULT '',
    address_line2       VARCHAR(300) NULL,
    city                VARCHAR(100) NOT NULL DEFAULT '',
    state               VARCHAR(100) NULL,
    pincode             VARCHAR(10)  NOT NULL DEFAULT '',
    shipping_address    VARCHAR(500) NOT NULL DEFAULT '',  -- one-line version
    notes               VARCHAR(500) NULL,
    tracking_number     VARCHAR(60)  NULL,
    estimated_delivery  DATE NULL,
    payment_method      VARCHAR(20) NOT NULL DEFAULT 'COD',   -- COD|CARD|UPI|NETBANKING|WALLET
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING|PAID|FAILED|REFUNDED
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (order_status),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Upgrade an older install that only had total_amount.
CALL add_column_if_not_exists('orders', 'order_number',         'VARCHAR(20) NULL');
CALL add_column_if_not_exists('orders', 'subtotal_amount',      'DECIMAL(10,2) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('orders', 'discount_amount',      'DECIMAL(10,2) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('orders', 'shipping_amount',      'DECIMAL(10,2) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('orders', 'tax_amount',           'DECIMAL(10,2) NOT NULL DEFAULT 0');
CALL add_column_if_not_exists('orders', 'coupon_code',          'VARCHAR(50) NULL');
CALL add_column_if_not_exists('orders', 'shipping_method_code', 'VARCHAR(30) NULL');
CALL add_column_if_not_exists('orders', 'shipping_method_name', 'VARCHAR(60) NULL');
CALL add_column_if_not_exists('orders', 'full_name',            'VARCHAR(100) NOT NULL DEFAULT ''''');
CALL add_column_if_not_exists('orders', 'address_line1',        'VARCHAR(300) NOT NULL DEFAULT ''''');
CALL add_column_if_not_exists('orders', 'address_line2',        'VARCHAR(300) NULL');
CALL add_column_if_not_exists('orders', 'city',                 'VARCHAR(100) NOT NULL DEFAULT ''''');
CALL add_column_if_not_exists('orders', 'state',                'VARCHAR(100) NULL');
CALL add_column_if_not_exists('orders', 'pincode',              'VARCHAR(10) NOT NULL DEFAULT ''''');
CALL add_column_if_not_exists('orders', 'notes',                'VARCHAR(500) NULL');
CALL add_column_if_not_exists('orders', 'tracking_number',      'VARCHAR(60) NULL');
CALL add_column_if_not_exists('orders', 'estimated_delivery',   'DATE NULL');
CALL add_column_if_not_exists('orders', 'updated_at',           'TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- Backfill order_number for rows created before this column existed.
UPDATE orders
SET order_number = CONCAT('SS-', LPAD(order_id, 6, '0'))
WHERE order_number IS NULL OR order_number = '';

-- ------------------------------------------------------------
-- Order line items (unit price + product snapshot captured
-- at purchase time so history survives catalog changes)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT NOT NULL,
    product_id    INT NOT NULL,
    product_name  VARCHAR(150) NOT NULL DEFAULT '',
    image_url     VARCHAR(500) NULL,
    quantity      INT NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    line_total    DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id)   ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB;

CALL add_column_if_not_exists('order_items', 'product_name', 'VARCHAR(150) NOT NULL DEFAULT ''''');
CALL add_column_if_not_exists('order_items', 'image_url',    'VARCHAR(500) NULL');
CALL add_column_if_not_exists('order_items', 'line_total',   'DECIMAL(10,2) NOT NULL DEFAULT 0');

-- ------------------------------------------------------------
-- Order status history (the tracking timeline)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id   INT NOT NULL,
    status     VARCHAR(30) NOT NULL,
    note       VARCHAR(300) NULL,
    changed_by VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_history_order (order_id),
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Payments — one row per payment attempt
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT NULL,
    user_id         INT NOT NULL,
    gateway         VARCHAR(30) NOT NULL DEFAULT 'SIMULATED',
    method          VARCHAR(20) NOT NULL,   -- COD|CARD|UPI|NETBANKING|WALLET
    transaction_id  VARCHAR(60) NULL UNIQUE,
    reference_id    VARCHAR(60) NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(5) NOT NULL DEFAULT 'INR',
    status          VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    -- INITIATED | SUCCESS | FAILED | REFUNDED
    failure_reason  VARCHAR(200) NULL,
    card_brand      VARCHAR(30) NULL,
    card_last4      VARCHAR(4)  NULL,
    upi_id          VARCHAR(100) NULL,
    bank_name       VARCHAR(60) NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_payment_order (order_id),
    KEY idx_payment_user (user_id),
    CONSTRAINT fk_payment_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Refunds (admin issues a refund against a paid payment)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refunds (
    refund_id     INT AUTO_INCREMENT PRIMARY KEY,
    payment_id    INT NOT NULL,
    order_id      INT NOT NULL,
    user_id       INT NOT NULL,
    amount        DECIMAL(10,2) NOT NULL,
    reason        VARCHAR(300) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    processed_by  VARCHAR(100) NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_refund_order (order_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id)   ON DELETE CASCADE,
    CONSTRAINT fk_refund_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Reviews & ratings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    review_id         INT AUTO_INCREMENT PRIMARY KEY,
    product_id        INT NOT NULL,
    user_id           INT NOT NULL,
    rating            INT NOT NULL,          -- 1..5
    title             VARCHAR(120) NULL,
    comment           TEXT NULL,
    is_verified_buy   TINYINT(1) NOT NULL DEFAULT 0,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_review_user_product (user_id, product_id),
    KEY idx_review_product (product_id),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Wishlist
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlist (
    wishlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wishlist (user_id, product_id),
    CONSTRAINT fk_wish_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_wish_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Seed data
-- ============================================================

-- Shipping methods
INSERT INTO shipping_methods
    (code, name, description, price, min_order_amount, max_days, sort_order)
VALUES
    ('STANDARD', 'Standard Delivery',
     'Delivered in 4-6 business days', 49.00, 0, 6, 1),
    ('EXPRESS',   'Express Delivery',
     'Delivered in 2-3 business days', 129.00, 999.00, 3, 2),
    ('FREE',      'Free Delivery',
     'Delivered in 5-7 business days', 0.00, 1999.00, 7, 3)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    max_days = VALUES(max_days),
    sort_order = VALUES(sort_order);

-- Coupons
INSERT INTO coupons
    (code, description, discount_type, discount_value,
     min_order_amount, max_discount, usage_limit, per_user_limit, valid_to)
VALUES
    ('WELCOME10', '10% off your first order (max ₹500 off)',
     'PERCENT', 10.00, 499.00, 500.00, NULL, 1, '2030-12-31 23:59:59'),
    ('SAVE200',   'Flat ₹200 off on orders above ₹1499',
     'FIXED', 200.00, 1499.00, NULL, 500, 2, '2030-12-31 23:59:59'),
    ('FLAT15',    'Flat ₹150 off on orders above ₹999',
     'FIXED', 150.00, 999.00, NULL, NULL, 3, '2030-12-31 23:59:59'),
    ('FREESHIP',  'Free shipping on all orders',
     'FIXED', 0.00, 0.00, NULL, NULL, 1, '2030-12-31 23:59:59')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    discount_value = VALUES(discount_value),
    min_order_amount = VALUES(min_order_amount),
    is_active = 1;

-- Default admin account  (email: admin@shopsphere.com / password: Admin@123)
--
-- The hash below is a valid BCrypt hash of "Admin@123" generated with
-- jbcrypt 0.4 at cost 12. Never regenerate it by pasting the value
-- through a PowerShell double-quoted string — '$' gets expanded.
INSERT INTO users (full_name, email, password_hash, role, email_verified)
VALUES ('Store Administrator', 'admin@shopsphere.com',
        '$2a$12$FHiz6aiIm/qu76GB6pbBO.zxBPWuR/GoJpkO5svybuVNmID6dwrIq',
        'ADMIN', 1)
ON DUPLICATE KEY UPDATE role = 'ADMIN', email_verified = 1;

-- Repair a corrupted admin hash (e.g. one that lost its '$2a$12$' prefix
-- because it was pasted through a shell that expanded the variables).
-- Only rows whose hash is NOT a valid 60-character BCrypt string are
-- touched, so a working admin password is never reset.
UPDATE users
SET password_hash = '$2a$12$FHiz6aiIm/qu76GB6pbBO.zxBPWuR/GoJpkO5svybuVNmID6dwrIq'
WHERE email = 'admin@shopsphere.com'
  AND CHAR_LENGTH(password_hash) <> 60;

-- Clean up the helper procedure so it does not linger in the schema.
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
