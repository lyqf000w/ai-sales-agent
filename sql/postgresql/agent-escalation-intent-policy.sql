-- AI 销冠：回复策略收敛为营业时间 + 静默秒数，并补充人工升级规则触发类型

ALTER TABLE agent_agent
  ADD COLUMN IF NOT EXISTS quiet_seconds int4 NOT NULL DEFAULT 0;
UPDATE agent_agent
SET quiet_seconds = COALESCE(quiet_seconds, quiet_minutes * 60, 0)
WHERE quiet_seconds = 0 AND quiet_minutes IS NOT NULL AND quiet_minutes > 0;
COMMENT ON COLUMN agent_agent.quiet_seconds IS '静默秒数';
COMMENT ON COLUMN agent_agent.confidence_threshold IS '历史兼容字段：自动回复最低置信度';
COMMENT ON COLUMN agent_agent.max_continuous_auto_reply IS '历史兼容字段：单客户连续自动回复上限';
COMMENT ON COLUMN agent_agent.quiet_minutes IS '历史兼容字段：静默分钟数';

ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS quiet_seconds int4 NOT NULL DEFAULT 0;
UPDATE agent_wechat_account
SET quiet_seconds = COALESCE(quiet_seconds, quiet_minutes * 60, 0)
WHERE quiet_seconds = 0 AND quiet_minutes IS NOT NULL AND quiet_minutes > 0;
COMMENT ON COLUMN agent_wechat_account.quiet_seconds IS '微信号默认静默秒数';
COMMENT ON COLUMN agent_wechat_account.confidence_threshold IS '历史兼容字段：微信号默认自动回复最低置信度';
COMMENT ON COLUMN agent_wechat_account.max_continuous_auto_reply IS '历史兼容字段：微信号默认单客户连续自动回复上限';
COMMENT ON COLUMN agent_wechat_account.quiet_minutes IS '历史兼容字段：微信号默认静默分钟数';

ALTER TABLE agent_wechat_contact
  ADD COLUMN IF NOT EXISTS quiet_seconds int4 NULL;
UPDATE agent_wechat_contact
SET quiet_seconds = quiet_minutes * 60
WHERE quiet_seconds IS NULL AND quiet_minutes IS NOT NULL;
COMMENT ON COLUMN agent_wechat_contact.quiet_seconds IS '好友静默秒数覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.confidence_threshold IS '历史兼容字段：好友自动回复最低置信度覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.max_continuous_auto_reply IS '历史兼容字段：好友连续自动回复上限覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.quiet_minutes IS '历史兼容字段：好友静默分钟数覆盖，空表示继承';

ALTER TABLE agent_sensitive_rule
  ADD COLUMN IF NOT EXISTS trigger_type varchar(32) NOT NULL DEFAULT 'KEYWORD';
UPDATE agent_sensitive_rule
SET trigger_type = CASE match_type
  WHEN 2 THEN 'REGEX'
  WHEN 3 THEN 'INTENT'
  ELSE 'KEYWORD'
END
WHERE trigger_type IS NULL OR trigger_type = '';
COMMENT ON TABLE agent_sensitive_rule IS 'AI 销冠人工升级规则';
COMMENT ON COLUMN agent_sensitive_rule.match_type IS '历史兼容字段：1 关键词，2 正则，3 LLM 分类';
COMMENT ON COLUMN agent_sensitive_rule.trigger_type IS '触发类型：KEYWORD、REGEX、INTENT、SENTIMENT、CUSTOMER_LEVEL、RAG_MISS、REQUEST_HUMAN';
COMMENT ON COLUMN agent_sensitive_rule.action IS '动作：1 进入人工确认，3 人工接管；0/2 为历史兼容';
UPDATE agent_sensitive_rule
SET action = 3
WHERE action = 2;

UPDATE system_menu
SET name = '人工升级规则'
WHERE id = 6006 OR component = 'agent/sensitiveRule/index' OR path = 'sensitive-rule';
UPDATE system_menu
SET name = '人工升级规则创建'
WHERE id = 6040 OR permission = 'agent:sensitive-rule:create';
UPDATE system_menu
SET name = '人工升级规则修改'
WHERE id = 6041 OR permission = 'agent:sensitive-rule:update';
UPDATE system_menu
SET name = '人工升级规则删除'
WHERE id = 6042 OR permission = 'agent:sensitive-rule:delete';
