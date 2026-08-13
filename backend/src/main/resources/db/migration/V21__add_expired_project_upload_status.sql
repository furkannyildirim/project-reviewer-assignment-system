ALTER TABLE project_uploads
    DROP CONSTRAINT chk_project_uploads_status;

ALTER TABLE project_uploads
    ADD CONSTRAINT chk_project_uploads_status
        CHECK (status IN ('PENDING', 'EXPIRED', 'CONSUMED'));
