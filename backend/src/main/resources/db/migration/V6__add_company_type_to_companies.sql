ALTER TABLE companies
    ADD COLUMN company_type VARCHAR(20) DEFAULT 'TGB';

UPDATE companies
SET company_type = 'TGB'
WHERE company_type IS NULL;

ALTER TABLE companies
    ALTER COLUMN company_type SET NOT NULL;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_company_type
        CHECK (company_type IN ('TGB', 'EXTERNAL'));

ALTER TABLE companies
    ALTER COLUMN company_type DROP DEFAULT;
