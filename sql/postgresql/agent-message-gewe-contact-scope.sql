-- Scope Gewe message de-duplication to the actual conversation.
-- Some Gewe payloads can reuse the same message id across different contacts or chatrooms.
DROP INDEX IF EXISTS uk_agent_message_account_gewe_msg;

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_message_account_gewe_msg
  ON agent_message(tenant_id, wechat_account_id, contact_id, gewe_message_id)
  WHERE deleted = 0 AND gewe_message_id IS NOT NULL;
