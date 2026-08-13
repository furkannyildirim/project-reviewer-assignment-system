CREATE TABLE project_uploads
(
    id                 UUID         PRIMARY KEY,
    uploaded_by_id     BIGINT       NOT NULL,
    storage_key        VARCHAR(500) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type       VARCHAR(255) NOT NULL,
    file_size          BIGINT       NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at        TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_project_uploads_uploaded_by
        FOREIGN KEY (uploaded_by_id)
            REFERENCES users (id),
    CONSTRAINT chk_project_uploads_file_size_positive
        CHECK (file_size > 0),
    CONSTRAINT chk_project_uploads_status
        CHECK (status IN ('PENDING', 'CONSUMED')),
    CONSTRAINT chk_project_uploads_expires_after_created
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_project_uploads_uploaded_by_status
    ON project_uploads (uploaded_by_id, status);

CREATE INDEX idx_project_uploads_expires_at
    ON project_uploads (expires_at);
