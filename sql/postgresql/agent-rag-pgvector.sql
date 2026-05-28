-- ai-sales-agent RAG schema: PostgreSQL + pgvector.

CREATE EXTENSION IF NOT EXISTS vector;

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

CREATE SEQUENCE IF NOT EXISTS agent_knowledge_base_seq START 1;

ALTER TABLE agent_agent
  ADD COLUMN IF NOT EXISTS knowledge_base_id int8 NULL;

ALTER TABLE agent_wechat_account
  ADD COLUMN IF NOT EXISTS knowledge_base_id int8 NULL;

ALTER TABLE agent_knowledge_item
  ADD COLUMN IF NOT EXISTS knowledge_base_id int8 NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS product_name varchar(128) NULL,
  ADD COLUMN IF NOT EXISTS category varchar(64) NULL,
  ADD COLUMN IF NOT EXISTS embedding vector NULL,
  ADD COLUMN IF NOT EXISTS embedding_status varchar(32) NOT NULL DEFAULT 'PENDING';

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

CREATE SEQUENCE IF NOT EXISTS agent_knowledge_chunk_seq START 1;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_chunk_tenant_item_no
  ON agent_knowledge_chunk(tenant_id, knowledge_item_id, chunk_no)
  WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_chunk_base_status
  ON agent_knowledge_chunk(tenant_id, knowledge_base_id, status)
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_agent_knowledge_item_tenant_title;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_item_tenant_title
  ON agent_knowledge_item(tenant_id, knowledge_base_id, title)
  WHERE deleted = 0;

INSERT INTO agent_knowledge_base (id, name, description, status, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, '默认商品库', '用于销售商品信息、报价、部署、售后等问答资料', 0,
       'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_knowledge_base WHERE tenant_id = 1 AND name = '默认商品库' AND deleted = 0
);

UPDATE agent_knowledge_item
SET knowledge_base_id = 1
WHERE knowledge_base_id = 0;

SELECT setval('agent_knowledge_base_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM agent_knowledge_base), 1));

UPDATE agent_agent SET reply_mode = 'MANUAL_CONFIRM' WHERE reply_mode = 'MANUAL_ONLY';
UPDATE agent_wechat_account SET reply_mode = 'MANUAL_CONFIRM' WHERE reply_mode = 'MANUAL_ONLY';
UPDATE agent_wechat_contact SET reply_mode = 'MANUAL_CONFIRM' WHERE reply_mode = 'MANUAL_ONLY';
