-- Default auto-reply quiet window.
-- New messages reset pending_reply_due_time to message_time + quiet_seconds.

ALTER TABLE agent_agent
  ALTER COLUMN quiet_seconds SET DEFAULT 180;

ALTER TABLE agent_wechat_account
  ALTER COLUMN quiet_seconds SET DEFAULT 180;

UPDATE agent_agent
SET quiet_seconds = 180,
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND quiet_seconds = 60;

UPDATE agent_wechat_account
SET quiet_seconds = 180,
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND quiet_seconds = 60;
