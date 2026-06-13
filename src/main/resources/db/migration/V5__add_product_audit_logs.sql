CREATE TABLE product_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT      NOT NULL REFERENCES products(id),
    operated_by     BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    operated_at     TIMESTAMP   NOT NULL,
    operation_type  VARCHAR(20) NOT NULL,
    before_snapshot TEXT,
    after_snapshot  TEXT
);

CREATE INDEX idx_audit_logs_product_id  ON product_audit_logs(product_id);
CREATE INDEX idx_audit_logs_operated_at ON product_audit_logs(operated_at);
