-- ai-sales-agent V1.4 operational config migration

ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS tone varchar(128) NULL;
ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS welcome_message varchar(512) NULL;
ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS handover_message varchar(512) NULL;
ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS follow_up_policy jsonb NULL;
ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS material_priority jsonb NULL;
ALTER TABLE agent_agent ADD COLUMN IF NOT EXISTS published_config jsonb NULL;

COMMENT ON COLUMN agent_agent.tone IS '回复语气';
COMMENT ON COLUMN agent_agent.welcome_message IS '欢迎语';
COMMENT ON COLUMN agent_agent.handover_message IS '转人工话术';
COMMENT ON COLUMN agent_agent.follow_up_policy IS '跟进策略';
COMMENT ON COLUMN agent_agent.material_priority IS '素材优先级';
COMMENT ON COLUMN agent_agent.published_config IS '已发布配置快照';

CREATE TABLE IF NOT EXISTS agent_config_version (
  id int8 NOT NULL,
  agent_id int8 NOT NULL,
  version int4 NOT NULL,
  config_snapshot jsonb NOT NULL,
  change_summary varchar(512) NULL,
  publish_user_id int8 NULL,
  publish_time timestamp NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_config_version PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_config_version_agent_version
  ON agent_config_version(tenant_id, agent_id, version)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_config_version_agent
  ON agent_config_version(tenant_id, agent_id, publish_time)
  WHERE deleted = 0;

COMMENT ON TABLE agent_config_version IS 'AI 销冠 Agent 配置发布版本';
COMMENT ON COLUMN agent_config_version.config_snapshot IS '配置快照';
COMMENT ON COLUMN agent_config_version.change_summary IS '变更说明';

CREATE SEQUENCE IF NOT EXISTS agent_config_version_seq START 1;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6063, '运营统计', 'agent:statistics:query', 2, 37, 6000, 'statistics', 'ep:data-analysis', 'agent/statistics/index', 'AgentStatistics', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6064, 'Agent 发布', 'agent:agent:publish', 3, 4, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6065), false);
