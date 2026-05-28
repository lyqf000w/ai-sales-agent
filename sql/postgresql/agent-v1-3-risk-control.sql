-- ai-sales-agent V1.3 auto reply and risk control schema.

ALTER TABLE agent_agent
  ADD COLUMN IF NOT EXISTS confidence_threshold numeric(5,2) NOT NULL DEFAULT 0.70,
  ADD COLUMN IF NOT EXISTS max_continuous_auto_reply int4 NOT NULL DEFAULT 3,
  ADD COLUMN IF NOT EXISTS quiet_minutes int4 NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS business_hours jsonb NULL;

COMMENT ON COLUMN agent_agent.confidence_threshold IS '自动回复最低置信度';
COMMENT ON COLUMN agent_agent.max_continuous_auto_reply IS '单客户连续自动回复上限';
COMMENT ON COLUMN agent_agent.quiet_minutes IS '静默分钟数';
COMMENT ON COLUMN agent_agent.business_hours IS '营业时间配置';

ALTER TABLE agent_sensitive_rule
  ADD COLUMN IF NOT EXISTS agent_id int8 NULL,
  ADD COLUMN IF NOT EXISTS route_app varchar(64) NULL;

COMMENT ON COLUMN agent_sensitive_rule.agent_id IS 'Agent 编号，空表示全局规则';
COMMENT ON COLUMN agent_sensitive_rule.route_app IS '接入应用，空表示全部';
COMMENT ON COLUMN agent_sensitive_rule.match_type IS '匹配方式：1 关键词，2 正则，3 LLM 分类';
COMMENT ON COLUMN agent_sensitive_rule.action IS '动作：0 放行，1 进入人工确认，2 阻断自动发送，3 人工接管';
COMMENT ON COLUMN agent_reply_decision.decision_type IS '决策类型：AUTO_SEND、MANUAL_CONFIRM、HUMAN_TAKEOVER、RECORD_ONLY';

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6009, '风险会话', 'agent:risk:query', 2, 36, 6000, 'risk', 'ep:warning-filled', 'agent/risk/index', 'AgentRisk', 0, false, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  permission = EXCLUDED.permission,
  type = EXCLUDED.type,
  sort = EXCLUDED.sort,
  parent_id = EXCLUDED.parent_id,
  path = EXCLUDED.path,
  icon = EXCLUDED.icon,
  component = EXCLUDED.component,
  component_name = EXCLUDED.component_name,
  status = EXCLUDED.status,
  visible = EXCLUDED.visible,
  updater = 'admin',
  update_time = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6062, '风险会话处理', 'agent:risk:update', 3, 1, 6009, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  permission = EXCLUDED.permission,
  type = EXCLUDED.type,
  sort = EXCLUDED.sort,
  parent_id = EXCLUDED.parent_id,
  status = EXCLUDED.status,
  visible = EXCLUDED.visible,
  updater = 'admin',
  update_time = CURRENT_TIMESTAMP,
  deleted = 0;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6063), false);
