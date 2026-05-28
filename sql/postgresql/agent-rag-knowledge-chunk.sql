-- Personal Sales Assistant RAG chunk index migration.
-- Run after agent-rag-pgvector.sql on existing PostgreSQL environments.

CREATE EXTENSION IF NOT EXISTS vector;

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

COMMENT ON TABLE agent_knowledge_chunk IS 'AI 销冠知识库切片';
COMMENT ON COLUMN agent_knowledge_chunk.knowledge_item_id IS '知识条目编号';
COMMENT ON COLUMN agent_knowledge_chunk.chunk_no IS '切片序号';
COMMENT ON COLUMN agent_knowledge_chunk.content IS '切片内容';
COMMENT ON COLUMN agent_knowledge_chunk.embedding IS 'pgvector 语义向量';
COMMENT ON COLUMN agent_knowledge_chunk.embedding_status IS '向量化状态：PENDING、READY、FAILED';
