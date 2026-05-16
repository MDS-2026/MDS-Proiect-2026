-- V3__notifications_and_audit_fix.sql

-- 1. Create notifications table if not exists
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES fair_pay_groups(id) ON DELETE CASCADE,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Ensure audit_logs table exists and fix the constraint
-- If Hibernate already created it, we just drop the strict check constraint
-- so new enum values (like AI_DECISION) don't crash the app
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action VARCHAR(50) NOT NULL,
    performed_by_email VARCHAR(255) NOT NULL,
    group_id UUID NOT NULL,
    target_entity_id UUID,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Drop the check constraint that Hibernate might have created 
-- which limits 'action' to old enum values
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
