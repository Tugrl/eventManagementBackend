ALTER TABLE employees
    ADD COLUMN contact_id BIGINT;

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_contact
        FOREIGN KEY (contact_id) REFERENCES contacts(id);

CREATE INDEX idx_employees_company_contact
    ON employees(company_id, contact_id);
