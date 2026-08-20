ALTER TABLE project_reviewer_assignments
    DROP CONSTRAINT chk_project_reviewer_assignments_status;

ALTER TABLE project_reviewer_assignments
    ADD CONSTRAINT chk_project_reviewer_assignments_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'REMOVED', 'CANCELLED'));
