-- ai-sales-agent V1.2 Agent decision loop schema.

CREATE TABLE IF NOT EXISTS agent_reply_decision (
  id int8 NOT NULL,
  conversation_id int8 NOT NULL,
  inbound_message_id int8 NULL,
  suggested_message_id int8 NULL,
  sent_message_id int8 NULL,
  decision_type varchar(32) NOT NULL,
  risk_level int2 NOT NULL DEFAULT 0,
  confidence numeric(5,2) NULL,
  llm_model varchar(128) NULL,
  prompt_snapshot text NULL,
  context_snapshot jsonb NULL,
  knowledge_refs jsonb NULL,
  guardrail_hits jsonb NULL,
  decision_reason varchar(512) NULL,
  review_status varchar(32) NOT NULL DEFAULT 'PENDING',
  review_note varchar(512) NULL,
  review_user_id int8 NULL,
  review_time timestamp NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_reply_decision PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_agent_reply_decision_status
  ON agent_reply_decision(tenant_id, review_status, create_time)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_reply_decision_conversation
  ON agent_reply_decision(tenant_id, conversation_id, create_time)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_reply_decision_suggested_message
  ON agent_reply_decision(tenant_id, suggested_message_id)
  WHERE deleted = 0 AND suggested_message_id IS NOT NULL;

COMMENT ON TABLE agent_reply_decision IS 'AI 销冠回复决策记录';
COMMENT ON COLUMN agent_reply_decision.decision_type IS '决策类型：AUTO_SEND、MANUAL_CONFIRM、RECORD_ONLY';
COMMENT ON COLUMN agent_reply_decision.review_status IS '审核状态：PENDING、APPROVED、EDITED、REJECTED、SENT';

CREATE SEQUENCE IF NOT EXISTS agent_reply_decision_seq START 1;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6008, '回复审核', 'agent:review:query', 2, 35, 6000, 'review', 'ep:finished', 'agent/review/index', 'AgentReview', 0, false, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
  visible = false,
  updater = 'admin',
  update_time = CURRENT_TIMESTAMP,
  deleted = 0;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6061, '回复审核处理', 'agent:review:update', 3, 1, 6008, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6062), false);
