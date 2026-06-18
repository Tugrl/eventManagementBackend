INSERT INTO cost_categories (company_id, name, description, is_default, is_active, created_at, updated_at)
SELECT 1, 'Tüketim Planı', 'Tüketim planı finalize edildiğinde sistem tarafından oluşturulan maliyet kategorisi', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cost_categories WHERE company_id = 1 AND name = 'Tüketim Planı'
);
