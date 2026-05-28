-- ai-sales-agent V1 config, tags, rules, knowledge, and auto-reply schema.

ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS gewe_api_base_url varchar(255) NULL,
  ADD COLUMN IF NOT EXISTS gewe_token varchar(512) NULL;

COMMENT ON COLUMN agent_wechat_account.gewe_api_base_url IS 'Gewe API 地址';
COMMENT ON COLUMN agent_wechat_account.gewe_token IS 'Gewe API Token';

CREATE TABLE IF NOT EXISTS agent_contact_tag (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  color varchar(32) NULL,
  description varchar(255) NULL,
  sort int4 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_contact_tag PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_contact_tag_tenant_name
  ON agent_contact_tag(tenant_id, name)
  WHERE deleted = 0;

COMMENT ON TABLE agent_contact_tag IS 'AI 销冠客户标签';
COMMENT ON COLUMN agent_contact_tag.color IS '标签颜色';

CREATE TABLE IF NOT EXISTS agent_contact_tag_rel (
  id int8 NOT NULL,
  contact_id int8 NOT NULL,
  tag_id int8 NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_contact_tag_rel PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_contact_tag_rel_contact_tag
  ON agent_contact_tag_rel(tenant_id, contact_id, tag_id)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_contact_tag_rel_tag
  ON agent_contact_tag_rel(tenant_id, tag_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_contact_tag_rel IS 'AI 销冠客户标签关系';

CREATE TABLE IF NOT EXISTS agent_sensitive_rule (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  match_type int2 NOT NULL DEFAULT 1,
  pattern varchar(512) NOT NULL,
  action int2 NOT NULL DEFAULT 1,
  risk_level int2 NOT NULL DEFAULT 1,
  sort int4 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  remark varchar(255) NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_sensitive_rule PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_sensitive_rule_tenant_name
  ON agent_sensitive_rule(tenant_id, name)
  WHERE deleted = 0;

COMMENT ON TABLE agent_sensitive_rule IS 'AI 销冠敏感规则';
COMMENT ON COLUMN agent_sensitive_rule.match_type IS '匹配方式：1 关键词，2 正则';
COMMENT ON COLUMN agent_sensitive_rule.action IS '动作：1 进入人工确认，2 阻断自动发送';

CREATE TABLE IF NOT EXISTS agent_knowledge_item (
  id int8 NOT NULL,
  title varchar(128) NOT NULL,
  keywords varchar(512) NULL,
  question varchar(512) NULL,
  answer text NOT NULL,
  sort int4 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_knowledge_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_item_tenant_title
  ON agent_knowledge_item(tenant_id, title)
  WHERE deleted = 0;

COMMENT ON TABLE agent_knowledge_item IS 'AI 销冠知识库条目';
COMMENT ON COLUMN agent_knowledge_item.keywords IS '关键词，逗号分隔';

CREATE SEQUENCE IF NOT EXISTS agent_contact_tag_seq START 1;
CREATE SEQUENCE IF NOT EXISTS agent_contact_tag_rel_seq START 1;
CREATE SEQUENCE IF NOT EXISTS agent_sensitive_rule_seq START 1;
CREATE SEQUENCE IF NOT EXISTS agent_knowledge_item_seq START 1;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6004, 'Agent 配置', 'agent:agent:query', 2, 5, 6000, 'agent', 'ep:setting', 'agent/agent/index', 'AgentConfig', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6005, '客户标签', 'agent:tag:query', 2, 40, 6000, 'tag', 'ep:collection-tag', 'agent/tag/index', 'AgentTag', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6006, '敏感规则', 'agent:sensitive-rule:query', 2, 50, 6000, 'sensitive-rule', 'ep:warning', 'agent/sensitiveRule/index', 'AgentSensitiveRule', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6007, '知识库', 'agent:knowledge:query', 2, 60, 6000, 'knowledge', 'ep:notebook', 'agent/knowledge/index', 'AgentKnowledge', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6020, 'Agent 创建', 'agent:agent:create', 3, 1, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6021, 'Agent 修改', 'agent:agent:update', 3, 2, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6022, 'Agent 删除', 'agent:agent:delete', 3, 3, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6030, '客户标签创建', 'agent:tag:create', 3, 1, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6031, '客户标签修改', 'agent:tag:update', 3, 2, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6032, '客户标签删除', 'agent:tag:delete', 3, 3, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6040, '敏感规则创建', 'agent:sensitive-rule:create', 3, 1, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6041, '敏感规则修改', 'agent:sensitive-rule:update', 3, 2, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6042, '敏感规则删除', 'agent:sensitive-rule:delete', 3, 3, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6050, '知识库创建', 'agent:knowledge:create', 3, 1, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6051, '知识库修改', 'agent:knowledge:update', 3, 2, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6052, '知识库删除', 'agent:knowledge:delete', 3, 3, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6060, '会话发送消息', 'agent:conversation:send', 3, 1, 6003, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6061), false);
