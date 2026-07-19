-- Add environment column to audit_logs
ALTER TABLE audit_logs ADD COLUMN environment VARCHAR(20);

-- Try to backfill environment from new_state if it exists
UPDATE audit_logs SET environment = new_state->>'environment' WHERE new_state->>'environment' IS NOT NULL;

CREATE INDEX idx_audit_logs_environment ON audit_logs(environment);
