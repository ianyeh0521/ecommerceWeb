CREATE TABLE products (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    stock        INT           NOT NULL,
    is_published BOOLEAN       NOT NULL DEFAULT false,
    is_deleted   BOOLEAN       NOT NULL DEFAULT false,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);