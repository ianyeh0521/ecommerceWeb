-- order_items.product_id could not reference products(id) in V2 because products table
-- did not exist until V4. This migration adds the FK after both tables are present.
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);