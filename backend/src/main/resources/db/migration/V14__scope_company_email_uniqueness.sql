DROP INDEX uq_company_contact_methods_email_value;

CREATE UNIQUE INDEX uq_company_contact_methods_company_email_value
    ON company_contact_methods (company_id, LOWER(value))
    WHERE type = 'EMAIL';
