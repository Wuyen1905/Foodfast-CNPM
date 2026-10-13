ALTER TABLE products ADD COLUMN daily_limit INTEGER;
ALTER TABLE products ADD CONSTRAINT ck_products_daily_limit
    CHECK (daily_limit IS NULL OR daily_limit > 0);
