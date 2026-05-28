-- ai-sales-agent 策略拆层迁移
-- 目标：
-- 1. Agent 只承载销售人格、系统提示词和模型配置。
-- 2. 微信号承载默认回复策略。
-- 3. 好友承载可空覆盖策略，空表示继承微信号策略。

ALTER TABLE agent_agent
  ADD COLUMN IF NOT EXISTS system_prompt text NULL,
  ADD COLUMN IF NOT EXISTS llm_provider varchar(64) NULL DEFAULT 'DEEPSEEK',
  ADD COLUMN IF NOT EXISTS llm_model varchar(128) NULL DEFAULT 'deepseek-v4-pro';

COMMENT ON COLUMN agent_agent.system_prompt IS '系统提示词，用于约束销售人格和回复风格';
COMMENT ON COLUMN agent_agent.llm_provider IS '大模型供应商';
COMMENT ON COLUMN agent_agent.llm_model IS '大模型名称';

ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS reply_mode varchar(32) NOT NULL DEFAULT 'MANUAL_CONFIRM',
  ADD COLUMN IF NOT EXISTS confidence_threshold numeric(5,2) NOT NULL DEFAULT 0.70,
  ADD COLUMN IF NOT EXISTS max_continuous_auto_reply int4 NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS quiet_minutes int4 NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS business_hours jsonb NULL;

COMMENT ON COLUMN agent_wechat_account.reply_mode IS '微信号默认回复模式';
COMMENT ON COLUMN agent_wechat_account.confidence_threshold IS '微信号默认自动回复最低置信度';
COMMENT ON COLUMN agent_wechat_account.max_continuous_auto_reply IS '微信号默认单客户连续自动回复上限';
COMMENT ON COLUMN agent_wechat_account.quiet_minutes IS '微信号默认静默分钟数';
COMMENT ON COLUMN agent_wechat_account.business_hours IS '微信号默认营业时间配置';

ALTER TABLE agent_wechat_contact
  ADD COLUMN IF NOT EXISTS reply_mode varchar(32) NULL,
  ADD COLUMN IF NOT EXISTS confidence_threshold numeric(5,2) NULL,
  ADD COLUMN IF NOT EXISTS max_continuous_auto_reply int4 NULL,
  ADD COLUMN IF NOT EXISTS quiet_minutes int4 NULL,
  ADD COLUMN IF NOT EXISTS business_hours jsonb NULL;

COMMENT ON COLUMN agent_wechat_contact.reply_mode IS '好友回复模式覆盖，空表示继承微信号策略';
COMMENT ON COLUMN agent_wechat_contact.confidence_threshold IS '好友自动回复最低置信度覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.max_continuous_auto_reply IS '好友连续自动回复上限覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.quiet_minutes IS '好友静默分钟数覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.business_hours IS '好友营业时间覆盖，空表示继承';

UPDATE agent_wechat_account account
SET reply_mode = COALESCE(agent.reply_mode, account.reply_mode, 'MANUAL_CONFIRM'),
    confidence_threshold = COALESCE(agent.confidence_threshold, account.confidence_threshold, 0.70),
    max_continuous_auto_reply = COALESCE(agent.max_continuous_auto_reply, account.max_continuous_auto_reply, 3),
    quiet_minutes = COALESCE(agent.quiet_minutes, account.quiet_minutes, 0),
    business_hours = COALESCE(agent.business_hours, account.business_hours)
FROM agent_agent agent
WHERE account.agent_id = agent.id
  AND account.deleted = 0
  AND agent.deleted = 0;

UPDATE agent_agent
SET llm_provider = COALESCE(NULLIF(llm_provider, ''), 'DEEPSEEK'),
    llm_model = COALESCE(NULLIF(llm_model, ''), 'deepseek-v4-pro')
WHERE deleted = 0;
