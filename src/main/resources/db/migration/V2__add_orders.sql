CREATE TABLE orders (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE order_items (
    id         BIGSERIAL    PRIMARY KEY,
    order_id   UUID         NOT NULL REFERENCES orders(id),
    product_id BIGINT       NOT NULL,
    quantity   INT          NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_orders_user_id        ON orders(user_id);
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_order_items_order_id  ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
