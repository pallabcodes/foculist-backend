-- Add provider_sub column to map local users to AWS Cognito Sub identifiers
ALTER TABLE users ADD COLUMN provider_sub VARCHAR(100) UNIQUE;
