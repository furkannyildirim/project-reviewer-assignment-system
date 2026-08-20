DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM projects
        WHERE status IN ('DRAFT', 'CANCELLED')
    ) THEN
        RAISE EXCEPTION
            'Cannot remove legacy project statuses while DRAFT or CANCELLED projects exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM project_reviewer_assignments
        WHERE status = 'CANCELLED'
    ) THEN
        RAISE EXCEPTION
            'Cannot remove legacy assignment status while CANCELLED assignments exist';
    END IF;
END
$$;

ALTER TABLE projects
    ALTER COLUMN status DROP DEFAULT,
    DROP CONSTRAINT chk_projects_status,
    ADD CONSTRAINT chk_projects_status
        CHECK (status IN ('AWAITING_REVIEWER', 'ACTIVE', 'COMPLETED'));

ALTER TABLE project_reviewer_assignments
    DROP CONSTRAINT chk_project_reviewer_assignments_status,
    ADD CONSTRAINT chk_project_reviewer_assignments_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'REMOVED'));
