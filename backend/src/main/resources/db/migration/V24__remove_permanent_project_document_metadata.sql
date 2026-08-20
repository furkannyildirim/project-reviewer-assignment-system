ALTER TABLE projects
    DROP CONSTRAINT IF EXISTS chk_projects_file_size_non_negative;

ALTER TABLE projects
    DROP COLUMN source_file_id,
    DROP COLUMN original_file_name,
    DROP COLUMN storage_key,
    DROP COLUMN content_type,
    DROP COLUMN file_size;
