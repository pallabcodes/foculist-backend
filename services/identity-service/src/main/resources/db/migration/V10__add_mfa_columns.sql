-- V10: Add short-lived MFA tracking columns to users table for native 2FA

ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS mfa_session_token VARCHAR(255);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS mfa_code VARCHAR(10);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS mfa_expires_at TIMESTAMP WITH TIME ZONE;
