-- ====================================================================
-- SKRIP MIGRASI DATABASE CINEMAX - FITUR AKTIVASI AKUN, OAUTH2 & OTP
-- ====================================================================

-- 1. Tambah kolom baru pada tabel users dengan default enabled = TRUE untuk akun lama
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500) NULL;
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

-- Pastikan semua akun lama yang sudah ada di database di-set aktif (enabled = TRUE)
UPDATE users SET enabled = TRUE WHERE enabled IS NULL;
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;

-- 2. Buat tabel account_activation_tokens jika belum ada
CREATE TABLE IF NOT EXISTS account_activation_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME NOT NULL,
    CONSTRAINT fk_activation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. Tambah kolom OTP pada tabel transaksis
ALTER TABLE transaksis ADD COLUMN IF NOT EXISTS otp_hash VARCHAR(255) NULL;
ALTER TABLE transaksis ADD COLUMN IF NOT EXISTS otp_expiry DATETIME NULL;
ALTER TABLE transaksis ADD COLUMN IF NOT EXISTS otp_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE transaksis ADD COLUMN IF NOT EXISTS otp_last_sent_at DATETIME NULL;
ALTER TABLE transaksis ADD COLUMN IF NOT EXISTS otp_verified BOOLEAN NOT NULL DEFAULT FALSE;

