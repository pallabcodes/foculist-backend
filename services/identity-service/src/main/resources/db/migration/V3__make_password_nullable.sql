-- Make password_hash nullable for social login support
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
