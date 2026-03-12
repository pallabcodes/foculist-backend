-- Add tenant_id for strict data layer isolation
ALTER TABLE organizations ADD COLUMN tenant_id VARCHAR(50);
-- Update existing rows to a default public/system tenant if any exist
UPDATE organizations SET tenant_id = slug WHERE tenant_id IS NULL;
ALTER TABLE organizations ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_organizations_tenant ON organizations(tenant_id);

ALTER TABLE users ADD COLUMN tenant_id VARCHAR(50);
UPDATE users SET tenant_id = 'public' WHERE tenant_id IS NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_users_tenant ON users(tenant_id);

ALTER TABLE memberships ADD COLUMN tenant_id VARCHAR(50);
UPDATE memberships SET tenant_id = 'public' WHERE tenant_id IS NULL;
ALTER TABLE memberships ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);

-- Depending on architecture, memberships and users might have unique constraints that should include tenant_id
-- We drop the old unique constraints and add ones that include tenant_id
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users ADD CONSTRAINT users_tenant_email_key UNIQUE (tenant_id, email);

ALTER TABLE memberships DROP CONSTRAINT IF EXISTS memberships_organization_id_user_id_key;
ALTER TABLE memberships ADD CONSTRAINT memberships_tenant_org_user_key UNIQUE (tenant_id, organization_id, user_id);
