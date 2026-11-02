-- Deduplication key for event-driven notifications: a redelivered domain event must not create
-- a second row for the same user.
ALTER TABLE sys_user_notification ADD COLUMN IF NOT EXISTS business_key VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_notification_business
    ON sys_user_notification (tenant_id, user_id, business_key)
    WHERE business_key IS NOT NULL AND NOT deleted;
