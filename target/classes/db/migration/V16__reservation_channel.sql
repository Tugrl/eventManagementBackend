ALTER TABLE reservations
    ADD COLUMN reservation_channel VARCHAR(30) NOT NULL DEFAULT 'PHONE';
