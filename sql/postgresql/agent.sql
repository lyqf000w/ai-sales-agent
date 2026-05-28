-- ai-sales-agent V1.1 schema and menu seed

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agent_agent (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  alias_name varchar(64) NULL,
  owner_user_id int8 NULL,
  scene varchar(128) NULL,
  target_customer_desc varchar(255) NULL,
  system_prompt text NULL,
  llm_provider varchar(64) NULL DEFAULT 'DEEPSEEK',
  llm_model varchar(128) NULL DEFAULT 'deepseek-v4-pro',
  knowledge_base_id int8 NULL,
  reply_mode varchar(32) NOT NULL DEFAULT 'MANUAL_CONFIRM',
  confidence_threshold numeric(5,2) NOT NULL DEFAULT 0.70,
  max_continuous_auto_reply int4 NOT NULL DEFAULT 3,
  quiet_minutes int4 NOT NULL DEFAULT 0,
  quiet_seconds int4 NOT NULL DEFAULT 180,
  business_hours jsonb NULL DEFAULT '{"start":"08:00","end":"22:00"}'::jsonb,
  tone varchar(128) NULL,
  welcome_message varchar(512) NULL,
  handover_message varchar(512) NULL,
  follow_up_policy jsonb NULL,
  material_priority jsonb NULL,
  status int2 NOT NULL DEFAULT 0,
  draft_version int4 NOT NULL DEFAULT 1,
  online_version int4 NOT NULL DEFAULT 0,
  published_config jsonb NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_agent PRIMARY KEY (id)
);

COMMENT ON TABLE agent_agent IS 'AI 销冠 Agent';
COMMENT ON COLUMN agent_agent.name IS 'Agent 名称';
COMMENT ON COLUMN agent_agent.system_prompt IS '系统提示词，用于约束销售人格和回复风格';
COMMENT ON COLUMN agent_agent.llm_provider IS '大模型供应商';
COMMENT ON COLUMN agent_agent.llm_model IS '大模型名称';
COMMENT ON COLUMN agent_agent.reply_mode IS '回复模式';
COMMENT ON COLUMN agent_agent.confidence_threshold IS '历史兼容字段：自动回复最低置信度';
COMMENT ON COLUMN agent_agent.max_continuous_auto_reply IS '历史兼容字段：单客户连续自动回复上限';
COMMENT ON COLUMN agent_agent.quiet_minutes IS '历史兼容字段：静默分钟数';
COMMENT ON COLUMN agent_agent.quiet_seconds IS '静默秒数';
COMMENT ON COLUMN agent_agent.business_hours IS '营业时间配置';
COMMENT ON COLUMN agent_agent.tone IS '回复语气';
COMMENT ON COLUMN agent_agent.welcome_message IS '欢迎语';
COMMENT ON COLUMN agent_agent.handover_message IS '转人工话术';
COMMENT ON COLUMN agent_agent.follow_up_policy IS '跟进策略';
COMMENT ON COLUMN agent_agent.material_priority IS '素材优先级';
COMMENT ON COLUMN agent_agent.status IS '状态';
COMMENT ON COLUMN agent_agent.published_config IS '已发布配置快照';
COMMENT ON COLUMN agent_agent.tenant_id IS '租户编号';

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

CREATE TABLE IF NOT EXISTS agent_wechat_account (
  id int8 NOT NULL,
  gewe_credential_id int8 NULL,
  agent_id int8 NULL,
  knowledge_base_id int8 NULL,
  owner_user_id int8 NULL,
  gewe_app_id varchar(128) NULL,
  gewe_account_id varchar(128) NULL,
  wechat_id varchar(128) NULL,
  nickname varchar(128) NULL,
  avatar varchar(512) NULL,
  mobile varchar(32) NULL,
  callback_token varchar(64) NOT NULL,
  callback_secret varchar(128) NULL,
  callback_url varchar(512) NULL,
  gewe_api_base_url varchar(255) NULL,
  gewe_token varchar(512) NULL,
  reply_mode varchar(32) NOT NULL DEFAULT 'MANUAL_CONFIRM',
  confidence_threshold numeric(5,2) NOT NULL DEFAULT 0.70,
  max_continuous_auto_reply int4 NOT NULL DEFAULT 3,
  quiet_minutes int4 NOT NULL DEFAULT 0,
  quiet_seconds int4 NOT NULL DEFAULT 180,
  business_hours jsonb NULL DEFAULT '{"start":"08:00","end":"22:00"}'::jsonb,
  login_status int2 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  last_heartbeat_time timestamp NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_wechat_account PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_callback_token
  ON agent_wechat_account(callback_token)
  WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_tenant_app
  ON agent_wechat_account(tenant_id, gewe_app_id)
  WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_credential_app
  ON agent_wechat_account(tenant_id, gewe_credential_id, gewe_app_id)
  WHERE deleted = 0 AND gewe_credential_id IS NOT NULL AND gewe_app_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_credential_wxid
  ON agent_wechat_account(tenant_id, gewe_credential_id, wechat_id)
  WHERE deleted = 0 AND gewe_credential_id IS NOT NULL AND wechat_id IS NOT NULL AND wechat_id <> '';

COMMENT ON TABLE agent_wechat_account IS 'AI 销冠 Gewe 托管微信账号';
COMMENT ON COLUMN agent_wechat_account.gewe_credential_id IS 'GeWe 凭证编号';
COMMENT ON COLUMN agent_wechat_account.knowledge_base_id IS '微信号覆盖知识库编号，空表示使用 Agent 默认知识库';
COMMENT ON COLUMN agent_wechat_account.gewe_app_id IS 'Gewe Appid 或设备 ID';
COMMENT ON COLUMN agent_wechat_account.callback_token IS '回调令牌';
COMMENT ON COLUMN agent_wechat_account.gewe_api_base_url IS 'Gewe API 地址';
COMMENT ON COLUMN agent_wechat_account.gewe_token IS 'Gewe API Token';
COMMENT ON COLUMN agent_wechat_account.reply_mode IS '微信号默认回复模式';
COMMENT ON COLUMN agent_wechat_account.confidence_threshold IS '历史兼容字段：微信号默认自动回复最低置信度';
COMMENT ON COLUMN agent_wechat_account.max_continuous_auto_reply IS '历史兼容字段：微信号默认单客户连续自动回复上限';
COMMENT ON COLUMN agent_wechat_account.quiet_minutes IS '历史兼容字段：微信号默认静默分钟数';
COMMENT ON COLUMN agent_wechat_account.quiet_seconds IS '微信号默认静默秒数';
COMMENT ON COLUMN agent_wechat_account.business_hours IS '微信号默认营业时间配置';
COMMENT ON COLUMN agent_wechat_account.login_status IS '登录状态';

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

CREATE TABLE IF NOT EXISTS agent_wechat_contact (
  id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  external_user_id varchar(128) NOT NULL,
  wechat_id varchar(128) NULL,
  nickname varchar(128) NULL,
  remark varchar(128) NULL,
  avatar varchar(512) NULL,
  customer_level int2 NOT NULL DEFAULT 0,
  owner_user_id int8 NULL,
  risk_level int2 NOT NULL DEFAULT 0,
  last_message_time timestamp NULL,
  last_conversation_status int2 NULL,
  reply_mode varchar(32) NULL,
  purchase_intention varchar(32) NOT NULL DEFAULT 'MEDIUM',
  sales_stage varchar(32) NOT NULL DEFAULT 'NEW_LEAD',
  customer_sentiment varchar(32) NOT NULL DEFAULT 'NEUTRAL',
  follow_up_priority varchar(32) NOT NULL DEFAULT 'NORMAL',
  confidence_threshold numeric(5,2) NULL,
  max_continuous_auto_reply int4 NULL,
  quiet_minutes int4 NULL,
  quiet_seconds int4 NULL,
  business_hours jsonb NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_wechat_contact PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_contact_account_external
  ON agent_wechat_contact(tenant_id, wechat_account_id, external_user_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_wechat_contact IS 'AI 销冠微信好友';
COMMENT ON COLUMN agent_wechat_contact.customer_level IS '客户等级：普通、目标客户、重要客户';
COMMENT ON COLUMN agent_wechat_contact.risk_level IS '历史兼容字段：风险等级，主界面使用销售洞察和回复控制';
COMMENT ON COLUMN agent_wechat_contact.reply_mode IS '好友回复模式覆盖，空表示继承微信号策略';
COMMENT ON COLUMN agent_wechat_contact.purchase_intention IS '销售洞察：购买意愿 LOW、MEDIUM、HIGH、STRONG';
COMMENT ON COLUMN agent_wechat_contact.sales_stage IS '销售洞察：销售阶段 NEW_LEAD、NEEDS_CONFIRMED、PRODUCT_INTRO、QUOTE_NEGOTIATION、DEAL_PROGRESS、AFTER_SALES';
COMMENT ON COLUMN agent_wechat_contact.customer_sentiment IS '销售洞察：客户情绪 POSITIVE、NEUTRAL、NEGATIVE';
COMMENT ON COLUMN agent_wechat_contact.follow_up_priority IS '销售洞察：跟进优先级 NORMAL、FOCUS、URGENT';
COMMENT ON COLUMN agent_wechat_contact.confidence_threshold IS '历史兼容字段：好友自动回复最低置信度覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.max_continuous_auto_reply IS '历史兼容字段：好友连续自动回复上限覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.quiet_minutes IS '历史兼容字段：好友静默分钟数覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.quiet_seconds IS '好友静默秒数覆盖，空表示继承';
COMMENT ON COLUMN agent_wechat_contact.business_hours IS '好友营业时间覆盖，空表示继承';

CREATE TABLE IF NOT EXISTS agent_conversation (
  id int8 NOT NULL,
  agent_id int8 NULL,
  wechat_account_id int8 NOT NULL,
  contact_id int8 NOT NULL,
  status int2 NOT NULL DEFAULT 0,
  risk_level int2 NOT NULL DEFAULT 0,
  last_message_id int8 NULL,
  last_message_time timestamp NULL,
  pending_reply_message_id int8 NULL,
  pending_reply_due_time timestamp NULL,
  continuous_auto_reply_count int4 NOT NULL DEFAULT 0,
  human_takeover_user_id int8 NULL,
  human_takeover_time timestamp NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_conversation PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_conversation_account_contact
  ON agent_conversation(tenant_id, wechat_account_id, contact_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_conversation IS 'AI 销冠客户会话';
COMMENT ON COLUMN agent_conversation.status IS '会话状态';
COMMENT ON COLUMN agent_conversation.risk_level IS '风险等级';
COMMENT ON COLUMN agent_conversation.pending_reply_message_id IS '静默期结束后待处理的最后一条客户消息';
COMMENT ON COLUMN agent_conversation.pending_reply_due_time IS '静默期结束后的计划回复时间';

CREATE TABLE IF NOT EXISTS agent_message (
  id int8 NOT NULL,
  conversation_id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  contact_id int8 NOT NULL,
  direction int2 NOT NULL,
  sender_type int2 NOT NULL,
  message_type int2 NOT NULL,
  content text NULL,
  raw_payload jsonb NULL,
  gewe_message_id varchar(128) NULL,
  send_status int2 NOT NULL DEFAULT 0,
  intent varchar(128) NULL,
  matched_policy varchar(255) NULL,
  audit_note varchar(512) NULL,
  operator_user_id int8 NULL,
  message_time timestamp NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_message PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_message_account_gewe_msg
  ON agent_message(tenant_id, wechat_account_id, contact_id, gewe_message_id)
  WHERE deleted = 0 AND gewe_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_time
  ON agent_message(tenant_id, conversation_id, message_time);

COMMENT ON TABLE agent_message IS 'AI 销冠会话消息';
COMMENT ON COLUMN agent_message.direction IS '消息方向';
COMMENT ON COLUMN agent_message.sender_type IS '发送方类型';
COMMENT ON COLUMN agent_message.message_type IS '消息类型';

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
COMMENT ON COLUMN agent_reply_decision.decision_type IS '决策类型：AUTO_SEND、MANUAL_CONFIRM、HUMAN_TAKEOVER、RECORD_ONLY';
COMMENT ON COLUMN agent_reply_decision.review_status IS '审核状态：PENDING、APPROVED、EDITED、REJECTED、SENT';

CREATE TABLE IF NOT EXISTS agent_sensitive_rule (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  agent_id int8 NULL,
  route_app varchar(64) NULL,
  match_type int2 NOT NULL DEFAULT 1,
  trigger_type varchar(32) NOT NULL DEFAULT 'KEYWORD',
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

COMMENT ON TABLE agent_sensitive_rule IS 'AI 销冠人工升级规则';
COMMENT ON COLUMN agent_sensitive_rule.agent_id IS 'Agent 编号，空表示全局规则';
COMMENT ON COLUMN agent_sensitive_rule.route_app IS '接入应用，空表示全部';
COMMENT ON COLUMN agent_sensitive_rule.match_type IS '历史兼容字段：1 关键词，2 正则，3 LLM 分类';
COMMENT ON COLUMN agent_sensitive_rule.trigger_type IS '触发类型：KEYWORD、REGEX、INTENT、SENTIMENT、CUSTOMER_LEVEL、RAG_MISS、REQUEST_HUMAN';
COMMENT ON COLUMN agent_sensitive_rule.action IS '动作：1 进入人工确认，3 转人工接管；恢复原策略只清除临时接管态，不改写好友或微信号回复策略，0/2 为历史兼容';

CREATE TABLE IF NOT EXISTS agent_knowledge_base (
  id int8 NOT NULL,
  name varchar(128) NOT NULL,
  description varchar(512) NULL,
  status int2 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_knowledge_base PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_base_tenant_name
  ON agent_knowledge_base(tenant_id, name)
  WHERE deleted = 0;

COMMENT ON TABLE agent_knowledge_base IS 'AI 销冠知识库';

CREATE TABLE IF NOT EXISTS agent_knowledge_item (
  id int8 NOT NULL,
  knowledge_base_id int8 NOT NULL DEFAULT 0,
  title varchar(128) NOT NULL,
  product_name varchar(128) NULL,
  category varchar(64) NULL,
  keywords varchar(512) NULL,
  question varchar(512) NULL,
  answer text NOT NULL,
  embedding vector NULL,
  embedding_status varchar(32) NOT NULL DEFAULT 'PENDING',
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
  ON agent_knowledge_item(tenant_id, knowledge_base_id, title)
  WHERE deleted = 0;

COMMENT ON TABLE agent_knowledge_item IS 'AI 销冠知识库条目';
COMMENT ON COLUMN agent_knowledge_item.knowledge_base_id IS '知识库编号';
COMMENT ON COLUMN agent_knowledge_item.keywords IS '关键词，逗号分隔';
COMMENT ON COLUMN agent_knowledge_item.embedding IS 'pgvector 语义向量';
COMMENT ON COLUMN agent_knowledge_item.embedding_status IS '向量化状态：PENDING、READY、FAILED';

CREATE TABLE IF NOT EXISTS agent_knowledge_chunk (
  id int8 NOT NULL,
  knowledge_base_id int8 NOT NULL,
  knowledge_item_id int8 NOT NULL,
  chunk_no int4 NOT NULL,
  title varchar(128) NOT NULL,
  product_name varchar(128) NULL,
  category varchar(64) NULL,
  keywords varchar(512) NULL,
  question varchar(512) NULL,
  content text NOT NULL,
  embedding vector NULL,
  embedding_status varchar(32) NOT NULL DEFAULT 'PENDING',
  sort int4 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_knowledge_chunk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_chunk_tenant_item_no
  ON agent_knowledge_chunk(tenant_id, knowledge_item_id, chunk_no)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_chunk_base_status
  ON agent_knowledge_chunk(tenant_id, knowledge_base_id, status)
  WHERE deleted = 0;

COMMENT ON TABLE agent_knowledge_chunk IS 'AI 销冠知识库切片';
COMMENT ON COLUMN agent_knowledge_chunk.knowledge_item_id IS '知识条目编号';
COMMENT ON COLUMN agent_knowledge_chunk.chunk_no IS '切片序号';
COMMENT ON COLUMN agent_knowledge_chunk.content IS '切片内容';
COMMENT ON COLUMN agent_knowledge_chunk.embedding IS 'pgvector 语义向量';
COMMENT ON COLUMN agent_knowledge_chunk.embedding_status IS '向量化状态：PENDING、READY、FAILED';

CREATE TABLE IF NOT EXISTS agent_webhook_event (
  id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  event_id varchar(128) NOT NULL,
  event_type varchar(64) NOT NULL,
  signature_valid boolean NOT NULL DEFAULT false,
  raw_payload jsonb NOT NULL,
  process_status int2 NOT NULL DEFAULT 0,
  error_message varchar(512) NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_webhook_event PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_webhook_account_event
  ON agent_webhook_event(tenant_id, wechat_account_id, event_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_webhook_event IS 'AI 销冠 Gewe 原始回调事件';
COMMENT ON COLUMN agent_webhook_event.event_id IS '事件唯一键，使用 Appid + Data.NewMsgId';

DROP SEQUENCE IF EXISTS agent_agent_seq;
CREATE SEQUENCE agent_agent_seq START 1;
DROP SEQUENCE IF EXISTS agent_config_version_seq;
CREATE SEQUENCE agent_config_version_seq START 1;
DROP SEQUENCE IF EXISTS agent_gewe_credential_seq;
CREATE SEQUENCE agent_gewe_credential_seq START 1;
DROP SEQUENCE IF EXISTS agent_wechat_bind_session_seq;
CREATE SEQUENCE agent_wechat_bind_session_seq START 1;
DROP SEQUENCE IF EXISTS agent_wechat_account_seq;
CREATE SEQUENCE agent_wechat_account_seq START 1;
DROP SEQUENCE IF EXISTS agent_contact_tag_seq;
CREATE SEQUENCE agent_contact_tag_seq START 1;
DROP SEQUENCE IF EXISTS agent_contact_tag_rel_seq;
CREATE SEQUENCE agent_contact_tag_rel_seq START 1;
DROP SEQUENCE IF EXISTS agent_wechat_contact_seq;
CREATE SEQUENCE agent_wechat_contact_seq START 1;
DROP SEQUENCE IF EXISTS agent_conversation_seq;
CREATE SEQUENCE agent_conversation_seq START 1;
DROP SEQUENCE IF EXISTS agent_message_seq;
CREATE SEQUENCE agent_message_seq START 1;
DROP SEQUENCE IF EXISTS agent_reply_decision_seq;
CREATE SEQUENCE agent_reply_decision_seq START 1;
DROP SEQUENCE IF EXISTS agent_webhook_event_seq;
CREATE SEQUENCE agent_webhook_event_seq START 1;
DROP SEQUENCE IF EXISTS agent_sensitive_rule_seq;
CREATE SEQUENCE agent_sensitive_rule_seq START 1;
DROP SEQUENCE IF EXISTS agent_knowledge_base_seq;
CREATE SEQUENCE agent_knowledge_base_seq START 1;
DROP SEQUENCE IF EXISTS agent_knowledge_item_seq;
CREATE SEQUENCE agent_knowledge_item_seq START 1;
DROP SEQUENCE IF EXISTS agent_knowledge_chunk_seq;
CREATE SEQUENCE agent_knowledge_chunk_seq START 1;

INSERT INTO agent_knowledge_base (id, name, description, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES (1, '默认商品库', '用于销售商品信息、报价、部署、售后等问答资料', 0, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1)
ON CONFLICT (id) DO NOTHING;
SELECT setval('agent_knowledge_base_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM agent_knowledge_base), 1));

INSERT INTO agent_contact_tag (id, name, color, description, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (1, '价格敏感', '#E6A23C', '客户明显关注价格、折扣、报价对比', 10, 0, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (2, '关注交付', '#409EFF', '客户关注部署周期、交付质量、售后响应', 20, 0, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (3, '决策人', '#67C23A', '客户具备采购或项目推进决策权', 30, 0, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (4, '竞品对比', '#909399', '客户正在比较竞品、替代方案或供应商', 40, 0, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1)
ON CONFLICT (id) DO NOTHING;
SELECT setval('agent_contact_tag_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM agent_contact_tag), 1));

-- Menu seed. IDs use the 6000+ range, above the current system_menu_seq START 5986.
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6000, 'AI 销冠', '', 1, 35, 0, '/agent', 'ep:chat-dot-square', NULL, NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6004, 'Agent 配置', 'agent:agent:query', 2, 5, 6000, 'agent', 'ep:setting', 'agent/agent/index', 'AgentConfig', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6001, '微信账号', 'agent:wechat-account:query', 2, 10, 6000, 'wechat-account', 'ep:connection', 'agent/wechatAccount/index', 'AgentWechatAccount', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6066, '链路诊断', 'agent:diagnostics:query', 2, 15, 6000, 'diagnostics', 'ep:monitor', 'agent/diagnostics/index', 'AgentDiagnostics', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6002, '客户好友', 'agent:contact:query', 2, 20, 6000, 'contact', 'ep:user', 'agent/contact/index', 'AgentContact', 0, false, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6003, '客户工作台', 'agent:conversation:query', 2, 30, 6000, 'conversation', 'ep:chat-line-round', 'agent/conversation/index', 'AgentConversation', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6008, '回复审核', 'agent:review:query', 2, 35, 6000, 'review', 'ep:finished', 'agent/review/index', 'AgentReview', 0, false, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6009, '风险会话', 'agent:risk:query', 2, 36, 6000, 'risk', 'ep:warning-filled', 'agent/risk/index', 'AgentRisk', 0, false, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6063, '运营统计', 'agent:statistics:query', 2, 37, 6000, 'statistics', 'ep:data-analysis', 'agent/statistics/index', 'AgentStatistics', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6005, '客户标签', 'agent:tag:query', 2, 40, 6000, 'tag', 'ep:collection-tag', 'agent/tag/index', 'AgentTag', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6006, '人工升级规则', 'agent:sensitive-rule:query', 2, 50, 6000, 'sensitive-rule', 'ep:warning', 'agent/sensitiveRule/index', 'AgentSensitiveRule', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6007, '知识库', 'agent:knowledge:query', 2, 60, 6000, 'knowledge', 'ep:notebook', 'agent/knowledge/index', 'AgentKnowledge', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6010, '微信账号创建', 'agent:wechat-account:create', 3, 1, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6011, '微信账号修改', 'agent:wechat-account:update', 3, 2, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6012, '微信账号删除', 'agent:wechat-account:delete', 3, 3, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6013, '客户好友修改', 'agent:contact:update', 3, 1, 6002, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6020, 'Agent 创建', 'agent:agent:create', 3, 1, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6021, 'Agent 修改', 'agent:agent:update', 3, 2, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6022, 'Agent 删除', 'agent:agent:delete', 3, 3, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6064, 'Agent 发布', 'agent:agent:publish', 3, 4, 6004, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6030, '客户标签创建', 'agent:tag:create', 3, 1, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6031, '客户标签修改', 'agent:tag:update', 3, 2, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6032, '客户标签删除', 'agent:tag:delete', 3, 3, 6005, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6040, '人工升级规则创建', 'agent:sensitive-rule:create', 3, 1, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6041, '人工升级规则修改', 'agent:sensitive-rule:update', 3, 2, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6042, '人工升级规则删除', 'agent:sensitive-rule:delete', 3, 3, 6006, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6050, '知识库创建', 'agent:knowledge:create', 3, 1, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6051, '知识库修改', 'agent:knowledge:update', 3, 2, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6052, '知识库删除', 'agent:knowledge:delete', 3, 3, 6007, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6060, '会话发送消息', 'agent:conversation:send', 3, 1, 6003, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6061, '回复审核处理', 'agent:review:update', 3, 1, 6008, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6062, '风险会话处理', 'agent:risk:update', 3, 1, 6009, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6067), false);
