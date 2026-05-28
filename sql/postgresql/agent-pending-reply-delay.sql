ALTER TABLE agent_conversation
  ADD COLUMN IF NOT EXISTS pending_reply_message_id int8 NULL,
  ADD COLUMN IF NOT EXISTS pending_reply_due_time timestamp NULL;

COMMENT ON COLUMN agent_conversation.pending_reply_message_id IS '静默期结束后待处理的最后一条客户消息';
COMMENT ON COLUMN agent_conversation.pending_reply_due_time IS '静默期结束后的计划回复时间';

CREATE INDEX IF NOT EXISTS idx_agent_conversation_pending_reply_due
  ON agent_conversation(tenant_id, pending_reply_due_time)
  WHERE deleted = 0 AND pending_reply_due_time IS NOT NULL;

ALTER TABLE agent_wechat_account
  ALTER COLUMN quiet_seconds SET DEFAULT 60,
  ALTER COLUMN business_hours SET DEFAULT '{"start":"08:00","end":"22:00"}'::jsonb;

UPDATE agent_wechat_account
SET quiet_seconds = 60
WHERE quiet_seconds IS NULL OR quiet_seconds <= 0;

UPDATE agent_wechat_account
SET business_hours = '{"start":"08:00","end":"22:00"}'::jsonb
WHERE business_hours IS NULL
   OR NOT (business_hours ? 'start')
   OR NOT (business_hours ? 'end');
