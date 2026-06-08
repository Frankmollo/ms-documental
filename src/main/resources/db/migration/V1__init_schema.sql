CREATE TABLE documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(512) NOT NULL UNIQUE,
    meter_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_documents_meter_id ON documents(meter_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(50),
    CONSTRAINT fk_audit_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE INDEX idx_audit_logs_document_id ON audit_logs(document_id);
