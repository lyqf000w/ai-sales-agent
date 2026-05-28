-- GeWe 托管微信账号绑定：租户级凭证 + 扫码绑定会话

CREATE SEQUENCE IF NOT EXISTS agent_gewe_credential_seq START 1;
CREATE SEQUENCE IF NOT EXISTS agent_wechat_bind_session_seq START 1;

CREATE TABLE IF NOT EXISTS agent_gewe_credential (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  gewe_api_base_url varchar(255) NOT NULL,
  gewe_token varchar(512) NULL,
  callback_token varchar(64) NOT NULL,
  callback_secret varchar(128) NULL,
  callback_url varchar(512) NULL,
  callback_configured_time timestamp NULL,
  status int2 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_gewe_credential PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_gewe_credential_callback_token
  ON agent_gewe_credential(callback_token)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_gewe_credential_tenant_status
  ON agent_gewe_credential(tenant_id, status)
  WHERE deleted = 0;

COMMENT ON TABLE agent_gewe_credential IS 'AI 销冠 GeWe 托管服务凭证';
COMMENT ON COLUMN agent_gewe_credential.gewe_api_base_url IS 'GeWe API 地址';
COMMENT ON COLUMN agent_gewe_credential.gewe_token IS 'GeWe API Token';
COMMENT ON COLUMN agent_gewe_credential.callback_token IS '租户级回调令牌';
COMMENT ON COLUMN agent_gewe_credential.callback_url IS '配置到 GeWe 的回调地址';

CREATE TABLE IF NOT EXISTS agent_wechat_bind_session (
  id int8 NOT NULL,
  credential_id int8 NULL,
  agent_id int8 NOT NULL,
  owner_user_id int8 NOT NULL,
  app_id varchar(128) NULL,
  uuid varchar(128) NULL,
  qr_data text NULL,
  qr_img_base64 text NULL,
  verify_url varchar(512) NULL,
  nick_name varchar(128) NULL,
  avatar varchar(512) NULL,
  status varchar(32) NOT NULL,
  expires_at timestamp NULL,
  bind_account_id int8 NULL,
  error_message varchar(512) NULL,
  raw_response jsonb NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_wechat_bind_session PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_agent_wechat_bind_session_tenant_status
  ON agent_wechat_bind_session(tenant_id, status, create_time)
  WHERE deleted = 0;

COMMENT ON TABLE agent_wechat_bind_session IS 'AI 销冠微信扫码绑定会话';
COMMENT ON COLUMN agent_wechat_bind_session.credential_id IS 'GeWe 凭证编号';
COMMENT ON COLUMN agent_wechat_bind_session.status IS '绑定状态';

ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS gewe_credential_id int8 NULL;
ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS mobile varchar(32) NULL;
ALTER TABLE agent_wechat_account
  ALTER COLUMN gewe_app_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_credential_app
  ON agent_wechat_account(tenant_id, gewe_credential_id, gewe_app_id)
  WHERE deleted = 0 AND gewe_credential_id IS NOT NULL AND gewe_app_id IS NOT NULL;

COMMENT ON COLUMN agent_wechat_account.gewe_credential_id IS 'GeWe 凭证编号';
COMMENT ON COLUMN agent_wechat_account.mobile IS '托管微信绑定手机号';
