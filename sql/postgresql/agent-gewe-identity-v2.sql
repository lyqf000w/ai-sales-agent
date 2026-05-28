-- GeWe Account Identity V2
-- callback_token identifies the GeWe provider/credential.
-- wechat_id(wxid) identifies the real logged-in WeChat account.
-- gewe_app_id is only the current cloud-device id and may change after node deletion.

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_credential_wxid
  ON agent_wechat_account(tenant_id, gewe_credential_id, wechat_id)
  WHERE deleted = 0
    AND gewe_credential_id IS NOT NULL
    AND wechat_id IS NOT NULL
    AND wechat_id <> '';

CREATE INDEX IF NOT EXISTS idx_agent_wechat_account_credential_app
  ON agent_wechat_account(tenant_id, gewe_credential_id, gewe_app_id)
  WHERE deleted = 0
    AND gewe_credential_id IS NOT NULL
    AND gewe_app_id IS NOT NULL
    AND gewe_app_id <> '';
