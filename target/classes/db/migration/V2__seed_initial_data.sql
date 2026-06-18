INSERT INTO companies (id, name, is_active, created_at, updated_at)
VALUES (1, 'Dada Salon Kabarett', TRUE, NOW(), NOW());

INSERT INTO users (company_id, full_name, email, password_hash, role, is_active, created_at, updated_at)
VALUES (
    1,
    'Admin User',
    'admin@dada.com',
    '$2b$12$9kvL.pKYCEAYOBKhMzjWw.9uaqPwwTFM8u71HeeBoDC3W2RcqsqSi',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO cost_categories (company_id, name, description, is_default, is_active, created_at, updated_at) VALUES
(1, 'Sanatçı', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Garson', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Servis', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Teknik Ekip', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Yemek', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'İçki', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Personel Yemeği', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Müşteri Yemeği', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Güvenlik', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Temizlik', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Ulaşım', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Komisyon', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Vergi', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Diğer', NULL, TRUE, TRUE, NOW(), NOW());
