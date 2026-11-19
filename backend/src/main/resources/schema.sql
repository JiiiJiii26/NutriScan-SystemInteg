-- ===========================================================
-- NutriScan Database Schema - Increment 1
-- Single Table: users
-- ===========================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index on email for fast lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
