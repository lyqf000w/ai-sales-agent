-- Default auto-reply quiet window for personal sales assistant.
-- New inbound messages must reset pending_reply_due_time to message_time + quiet_seconds.

ALTER TABLE agent_wechat_account
  ALTER COLUMN quiet_seconds SET DEFAULT 90;

ALTER TABLE agent_wechat_contact
  ALTER COLUMN quiet_seconds SET DEFAULT 90;

UPDATE agent_wechat_account
SET quiet_seconds = 90,
    update_time = NOW()
WHERE deleted = 0
  AND (quiet_seconds IS NULL OR quiet_seconds IN (60, 180));

UPDATE agent_wechat_contact
SET quiet_seconds = 90,
    update_time = NOW()
WHERE deleted = 0
  AND (quiet_seconds IS NULL OR quiet_seconds IN (60, 180));
