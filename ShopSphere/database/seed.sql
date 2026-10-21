-- ============================================================
-- ShopSphere — demo catalogue seed
--
-- Fills the store with a clean, image-backed catalogue. Every
-- image is a bundled SVG in src/main/webapp/images/products/,
-- so the storefront works without an internet connection.
--
-- Safe to run repeatedly:
--     mysql -u root -p shopsphere < database/seed.sql
-- ============================================================

USE shopsphere;

INSERT INTO products
    (product_id, name, description, category, price, stock, image_url, is_active)
VALUES
    (1, 'Aurora Wireless Headphones',
     'Immersive over-ear sound with active noise cancellation, plush memory-foam cushions and up to 30 hours of battery life.',
     'Electronics', 2499.00, 21, 'images/products/product-1.jpg', 1),

    (2, 'Everyday Cotton T-Shirt',
     'A soft, breathable everyday tee made from 100% combed cotton with a relaxed unisex fit.',
     'Fashion', 799.00, 38, 'images/products/product-2.jpg', 1),

    (3, 'Nordic Desk Lamp',
     'A warm, dimmable LED desk lamp with a matte finish and an adjustable arm for late-night reading.',
     'Home & Living', 1299.00, 13, 'images/products/product-3.jpg', 1),

    (4, 'Pulse Smart Watch',
     'Track workouts, heart rate and sleep with a bright AMOLED display and a seven-day battery life.',
     'Electronics', 3499.00, 18, 'images/products/product-4.jpg', 1),

    (5, 'Clean Code Handbook',
     'Practical patterns and habits for writing readable, maintainable software — a desk reference for every developer.',
     'Books', 499.00, 30, 'images/products/product-5.jpg', 1),

    (6, 'Studio Over-Ear Headphones',
     'Balanced studio sound with a detachable cable and a fold-flat design that travels well.',
     'Electronics', 1999.00, 22, 'images/products/product-6.jpg', 1),

    (7, 'Linen Casual Shirt',
     'Lightweight linen-blend shirt with a natural texture and a relaxed button-down collar.',
     'Fashion', 999.00, 25, 'images/products/product-7.jpg', 1),

    (8, 'Minimal Ceramic Lamp',
     'Hand-finished ceramic base paired with a linen shade that casts a soft, ambient glow.',
     'Home & Living', 899.00, 15, 'images/products/product-8.jpg', 1),

    (9, 'Atom Wireless Earbuds',
     'Pocket-sized true wireless earbuds with touch controls, USB-C charging and 24 hours of total playtime.',
     'Electronics', 1599.00, 18, 'images/products/product-9.jpg', 1),

    (10, 'Chrono Fitness Watch',
     'A rugged fitness companion with built-in GPS, twenty sport modes and an always-on colour display.',
     'Electronics', 2499.00, 4, 'images/products/product-10.jpg', 1),

    (11, 'Graphic Print Tee',
     'Heavyweight cotton tee with a screen-printed graphic and a slightly boxy cut.',
     'Fashion', 599.00, 40, 'images/products/product-11.jpg', 1),

    (12, 'The Productive Mind',
     'A calm, evidence-based guide to focus, habits and doing meaningful work without burnout.',
     'Books', 349.00, 35, 'images/products/product-12.jpg', 1),

    (13, 'Woven Storage Basket',
     'Handwoven seagrass basket that keeps blankets, toys or plants tidy in any room.',
     'Home & Living', 749.00, 16, 'images/products/product-13.jpg', 1)

ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    description = VALUES(description),
    category    = VALUES(category),
    price       = VALUES(price),
    stock       = VALUES(stock),
    image_url   = VALUES(image_url),
    is_active   = 1;
