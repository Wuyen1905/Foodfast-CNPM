-- Product image URLs can contain long CDN query strings. Keep the cart snapshot
-- column aligned with products.image_url so adding a valid product never fails.
ALTER TABLE cart_items ALTER COLUMN product_image VARCHAR(1024);
