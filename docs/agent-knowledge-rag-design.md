# AI 销冠知识库 / Java RAG 设计方案与可行性说明

## 1. 结论

知识库建议采用 **Java-only 架构**，不再让 Python 参与正式业务链路。

目标链路：

```text
控制台知识库管理
  -> Java 后端
  -> PostgreSQL + pgvector
  -> Java RAG 检索
  -> DeepSeek 生成回复
  -> 自动回复 / 人工确认 / Frontis Skill
```

正式业务链路不依赖 Python：

```text
Frontis / 控制台 / GeWe 回调
  -> Java 后端
  -> PostgreSQL / Redis / DeepSeek / GeWe / Embedding API
```

Python 旧服务只允许作为历史迁移源，不能继续作为线上知识库、RAG、自动回复或 Frontis Skill 的依赖。

## 2. 可行性说明

### 2.1 Java 完全可行

当前项目已经具备 Java RAG 的基础：

- 已有知识库基础表对应的 DO：
  - `AgentKnowledgeBaseDO`
  - `AgentKnowledgeItemDO`
  - `AgentKnowledgeChunkDO`
- 已有知识库管理 Service / Controller：
  - `AgentKnowledgeBaseService`
  - `AgentKnowledgeItemService`
  - `AgentKnowledgeBaseController`
  - `AgentKnowledgeItemController`
- 已有向量切片和检索：
  - `AgentKnowledgeChunker`
  - `AgentKnowledgeIndexService`
  - `AgentKnowledgeRetrievalService`
  - `OpenAiCompatibleAgentEmbeddingClient`
- 已有自动回复接入：
  - `AgentConversationContextBuilder` 会根据账号或 Agent 的 `knowledgeBaseId` 检索知识库。
  - `AgentKnowledgeReplyGenerator` 会使用知识命中和 DeepSeek 生成回复。
- 本地 Docker 已使用 `pgvector/pgvector:pg16` 镜像。

因此不需要引入 Python 才能做 RAG。

### 2.2 Java 方案的优点

- 单后端：控制台、Frontis、GeWe 回调、自动回复都走 Java。
- 单数据库：知识库、会话、客户、策略都在 PostgreSQL。
- 易排查：出问题只查 Java 日志、PostgreSQL、Redis、DeepSeek、GeWe。
- 易部署：只部署 `sales-server.jar`，不用同时维护 Python 环境。
- 易权限：沿用 Java 现有租户、用户、菜单、权限体系。
- 易审计：回复命中知识、DeepSeek 生成、自动发送结果都能落 Java 表。

### 2.3 如果继续保留 Python 的后果

不建议保留 Python 做正式 RAG。否则会出现：

- Frontis 有些数据来自 Java，有些来自 Python，排查困难。
- 控制台保存的知识库和 Python 使用的知识库可能不一致。
- 自动回复到底用了哪套知识很难审计。
- 部署时要同时维护 Java、Python、两个依赖环境和两个日志系统。
- 领导演示或客户交付时容易出现“Java 已改但页面还是旧结果”的问题。

## 3. 当前现状

### 3.1 已有能力

后端已有：

- 知识库基础管理：
  - `/admin-api/agent/knowledge-base/**`
  - `/admin-api/agent/knowledge/**`
- FAQ / 结构化资料形式：
  - 标题
  - 产品名
  - 分类
  - 关键词
  - 客户问题
  - 答案 / 资料内容
- 切片：
  - `chunk-size`
  - `chunk-overlap`
- Embedding：
  - OpenAI-compatible embedding API
  - 当前配置示例：
    - `AGENT_EMBEDDING_URL`
    - `AGENT_EMBEDDING_MODEL`
    - `AGENT_EMBEDDING_API_KEY`
- 检索：
  - 向量召回
  - 关键词兜底召回
  - TopK
  - maxDistance
- 自动回复上下文接入：
  - 根据账号或 Agent 绑定的 `knowledgeBaseId` 检索。

前端已有：

- `sales-ui-admin/src/views/agent/knowledge/index.vue`
- `sales-ui-admin/src/api/agent/knowledge/index.ts`

### 3.2 当前缺口

当前还不是完整正式知识库，缺口包括：

- 没有完整 SQL 迁移脚本确认 pgvector extension、vector 维度、索引。
- 没有文档上传和解析：
  - PDF
  - Word
  - Markdown
  - TXT
  - Excel / CSV
- 没有知识文档表：
  - 文档文件
  - 解析状态
  - 来源文件
  - 原文哈希
- 没有检索测试接口：
  - 输入一句客户问题
  - 返回命中的知识片段、分数、距离、来源
- 没有检索日志：
  - 哪次客户消息命中了哪些知识
  - DeepSeek 是否使用
  - 最终是否自动回复
- 缺少销售场景分类和风险治理：
  - 报价
  - 合同
  - 退款
  - 投诉
  - 法务
  - 售后
  - 产品介绍
  - 竞品对比
- 控制台页面有乱码，需要修复中文文案。
- 缺少知识库质量验收标准。

## 4. 目标架构

```text
用户在控制台新增/上传知识
  -> AgentKnowledgeController
  -> AgentKnowledgeService
  -> 文档解析 / 文本清洗
  -> AgentKnowledgeChunker 切片
  -> AgentEmbeddingClient 生成向量
  -> PostgreSQL + pgvector 存储
  -> AgentKnowledgeRetrievalService 检索
  -> AgentConversationContextBuilder 构造回复上下文
  -> DeepSeek 生成回复
  -> AgentAutoReplyService 决策发送/人工确认/人工接管
```

## 5. 数据模型设计

### 5.1 知识库表：agent_knowledge_base

已有基础表，建议补充字段：

```text
id
tenant_id
name
description
scope_type        // GLOBAL / AGENT / ACCOUNT
agent_id
wechat_account_id
default_enabled
status
creator
create_time
updater
update_time
deleted
```

说明：

- `scope_type=GLOBAL`：租户默认知识库。
- `scope_type=AGENT`：某个 Agent 专用知识库。
- `scope_type=ACCOUNT`：某个微信账号专用知识库。

当前自动回复已经支持从 `Agent` 或微信账号读取 `knowledgeBaseId`，后续要在控制台把绑定关系做完整。

### 5.2 知识条目表：agent_knowledge_item

已有基础表，建议保留并扩展：

```text
id
tenant_id
knowledge_base_id
document_id        // 可空，手工 FAQ 没有 document_id
title
product_name
category
keywords
question
answer
content_type       // FAQ / DOCUMENT / SCRIPT / POLICY / CASE
risk_level         // GREEN / YELLOW / RED
auto_reply_allowed // 是否允许自动回复使用
manual_confirm_required // 是否必须人工确认
embedding
embedding_status   // PENDING / READY / FAILED
sort
status
creator
create_time
updater
update_time
deleted
```

说明：

- `answer` 是可用于回复的核心内容。
- `risk_level=RED` 的知识只能用于提醒人工，不允许自动发送。
- 报价、合同、退款、投诉类知识应默认 `manual_confirm_required=true`。

### 5.3 知识切片表：agent_knowledge_chunk

已有基础表，建议扩展：

```text
id
tenant_id
knowledge_base_id
knowledge_item_id
document_id
chunk_no
title
product_name
category
keywords
question
content
content_hash
token_count
embedding vector
embedding_status
sort
status
creator
create_time
updater
update_time
deleted
```

pgvector 建议：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

向量维度要和 embedding 模型一致。比如 qwen3-embedding-4b 如果输出维度为 2560，则字段为：

```sql
embedding vector(2560)
```

实际维度必须通过一次 embedding API 返回确认，不能猜。

索引建议：

```sql
CREATE INDEX idx_agent_knowledge_chunk_vector
ON agent_knowledge_chunk
USING hnsw (embedding vector_cosine_ops)
WHERE deleted = 0 AND status = 0 AND embedding IS NOT NULL;
```

如果 PostgreSQL / pgvector 版本暂不支持 HNSW，可先用 ivfflat：

```sql
CREATE INDEX idx_agent_knowledge_chunk_vector
ON agent_knowledge_chunk
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

### 5.4 知识文档表：agent_knowledge_document

新增表：

```text
id
tenant_id
knowledge_base_id
name
file_name
file_url
file_type
file_size
content_hash
parse_status      // PENDING / PARSING / PARSED / FAILED
parse_error
chunk_count
embedding_status  // PENDING / READY / FAILED
status
creator
create_time
updater
update_time
deleted
```

### 5.5 检索日志表：agent_knowledge_retrieval_log

新增表：

```text
id
tenant_id
conversation_id
message_id
contact_id
wechat_account_id
agent_id
knowledge_base_id
query_text
hit_count
top_score
top_distance
hits_json
used_by_llm
generation_source
reply_decision_id
create_time
```

用途：

- 证明自动回复用了哪些知识。
- 排查“为什么没回复 / 为什么答错”。
- 给控制台链路诊断展示。

## 6. 后端模块设计

### 6.1 Controller

已有：

```text
AgentKnowledgeBaseController
AgentKnowledgeItemController
```

需要新增：

```text
AgentKnowledgeDocumentController
AgentKnowledgeRetrievalController
```

建议接口：

```http
GET    /admin-api/agent/knowledge-base/page
GET    /admin-api/agent/knowledge-base/simple-list
POST   /admin-api/agent/knowledge-base/create
PUT    /admin-api/agent/knowledge-base/update
DELETE /admin-api/agent/knowledge-base/delete

GET    /admin-api/agent/knowledge/page
POST   /admin-api/agent/knowledge/create
PUT    /admin-api/agent/knowledge/update
DELETE /admin-api/agent/knowledge/delete
POST   /admin-api/agent/knowledge/rebuild-index

GET    /admin-api/agent/knowledge-document/page
POST   /admin-api/agent/knowledge-document/upload
POST   /admin-api/agent/knowledge-document/parse
POST   /admin-api/agent/knowledge-document/rebuild-index
DELETE /admin-api/agent/knowledge-document/delete

POST   /admin-api/agent/knowledge/retrieval-test
GET    /admin-api/agent/knowledge/retrieval-log/page
```

### 6.2 Service

已有：

```text
AgentKnowledgeBaseService
AgentKnowledgeItemService
AgentKnowledgeIndexService
AgentKnowledgeRetrievalService
AgentEmbeddingClient
AgentKnowledgeChunker
```

需要新增：

```text
AgentKnowledgeDocumentService
AgentKnowledgeDocumentParser
AgentKnowledgeRetrievalLogService
AgentKnowledgeQualityService
```

建议职责：

- `AgentKnowledgeDocumentService`
  - 上传文档
  - 保存文档记录
  - 调用 parser
  - 生成 item / chunk
  - 重建索引
- `AgentKnowledgeDocumentParser`
  - 解析 PDF / Word / Markdown / TXT / Excel
  - 第一阶段可先支持 Markdown / TXT / 手工 FAQ
  - 第二阶段支持 PDF / Word / Excel
- `AgentKnowledgeRetrievalLogService`
  - 保存每次检索日志
  - 给链路诊断页面使用
- `AgentKnowledgeQualityService`
  - 判断知识是否允许自动回复
  - 判断风险分类
  - 输出低置信度原因

## 7. 文档解析方案

### 7.1 第一阶段

优先支持：

- 手工 FAQ
- Markdown
- TXT
- CSV / Excel 的 Q&A 表格

原因：销售知识最常见的是 FAQ、话术、规则、案例，结构化文本比 PDF 更适合第一阶段上线。

### 7.2 第二阶段

支持：

- PDF：Apache PDFBox
- Word：Apache POI
- Excel：Apache POI
- 通用文档：Apache Tika

### 7.3 暂不建议第一阶段做

- 图片 OCR
- 语音转文字
- 复杂版式 PDF 还原

这些不是销售 Agent 第一阶段核心，容易拖慢交付。

## 8. 检索策略

采用混合检索：

```text
向量检索
  + 关键词召回
  + 业务权重
  + 风险过滤
  + TopK 重排
```

### 8.1 向量检索

输入客户消息，调用 embedding API 生成 query embedding。

SQL：

```sql
SELECT *,
       embedding <=> :queryEmbedding AS distance
FROM agent_knowledge_chunk
WHERE deleted = 0
  AND status = 0
  AND knowledge_base_id = :knowledgeBaseId
  AND embedding IS NOT NULL
ORDER BY embedding <=> :queryEmbedding
LIMIT :limit
```

### 8.2 关键词兜底

如果 embedding 不可用，或向量命中少，则使用：

- 产品名
- 分类
- 标题
- 关键词
- 客户问题
- 内容包含

当前 `AgentKnowledgeRetrievalServiceImpl` 已经有关键词兜底，后续要修复分隔符乱码并扩展中文标点。

### 8.3 重排权重

建议排序：

```text
1. distance 越小越靠前
2. keywordScore 越高越靠前
3. sort 越高越靠前
4. 更新时间越新越靠前
```

### 8.4 置信度

建议：

- `distance <= 0.45`：高置信度
- `0.45 < distance <= 0.75`：中置信度
- `distance > 0.75`：低置信度，不自动回复

实际阈值要根据线上数据调。

## 9. 销售知识分类

知识库不是简单文档仓库，要围绕销售场景组织。

建议分类：

```text
产品介绍
功能说明
适用客户
价格/报价
合同/付款
交付/部署
售后/服务
退款/投诉
竞品对比
客户案例
异议处理
跟进话术
开场话术
沉默唤醒
风险规则
内部规则
```

### 9.1 自动回复安全规则

允许自动回复：

- 产品介绍
- 功能说明
- 适用客户
- 普通售后说明
- 普通开场 / 跟进话术

必须人工确认：

- 价格 / 报价
- 折扣
- 合同
- 付款
- 退款
- 投诉
- 法务
- 强情绪
- 承诺收益
- 公司正式表态

## 10. 自动回复接入

当前流程应保持：

```text
GeWe 回调
  -> AgentWebhookService
  -> 保存消息
  -> AgentAutoReplyService
  -> 敏感规则判断
  -> AgentConversationContextBuilder
  -> AgentKnowledgeRetrievalService
  -> AgentKnowledgeReplyGenerator / DeepSeek
  -> 生成决策
  -> 自动发送或人工确认
```

规则：

- 群聊默认不自动回复。
- RAG 未命中或低置信度，进入人工确认或人工接管。
- 命中红色风险知识，不自动回复。
- 自动回复必须写 `agent_reply_decision`，记录：
  - generationSource
  - llmProvider
  - llmModel
  - matchedKnowledgeTitle
  - knowledgeHits

## 11. 控制台页面设计

路径建议：

```text
/agent/knowledge
```

页面结构：

```text
顶部筛选：
  知识库 / 分类 / 状态 / 关键词

左侧：
  知识库列表

中间：
  知识条目表格
  标题 / 分类 / 产品 / 问题 / 答案摘要 / 向量状态 / 风险 / 状态 / 操作

右侧或弹窗：
  新增/编辑知识
  上传文档
  检索测试
```

需要功能：

- 新增知识库
- 编辑知识库
- 新增 FAQ / 话术 / 规则
- 编辑知识
- 删除知识
- 启用 / 停用
- 重建索引
- 上传文档
- 查看切片
- 检索测试
- 查看命中结果

注意：当前页面中文存在乱码，开发时要修复为 UTF-8 文案。

## 12. Frontis Skill 更新

Frontis 的 `draft_reply` 应返回知识命中信息：

```json
{
  "conversation_id": 1001,
  "draft": "建议回复内容",
  "confidence": 0.82,
  "matched_knowledge_title": "企业版功能说明",
  "generation_source": "DEEPSEEK",
  "llm_provider": "DEEPSEEK",
  "llm_model": "deepseek-v4-pro",
  "manual_confirm_required": true,
  "send_allowed": false
}
```

后续建议新增 Skill action：

```text
knowledge_search
knowledge_base_status
```

示例：

```json
{
  "action": "knowledge_search",
  "input": {
    "query": "你们企业版多少钱",
    "knowledge_base_id": 1,
    "limit": 5
  }
}
```

用于 Frontis 直接查询知识，不一定生成回复。

## 13. 开发优先级

### P0：必须先完成

1. 确认数据库 SQL：
   - `CREATE EXTENSION vector`
   - `agent_knowledge_base`
   - `agent_knowledge_item`
   - `agent_knowledge_chunk`
   - vector 字段维度
   - vector 索引
2. 修复知识库控制台中文乱码。
3. 确保新增 / 编辑知识后能自动生成 chunk 和 embedding。
4. 增加检索测试接口。
5. 自动回复中记录知识命中日志。
6. Frontis `draft_reply` 返回知识命中来源。

### P1：正式可用

1. 文档上传：
   - TXT
   - Markdown
   - Excel / CSV FAQ
2. 文档解析状态。
3. 文档切片查看。
4. 知识库绑定到 Agent / 微信账号。
5. RAG 低置信度转人工。
6. 控制台查看 retrieval log。

### P2：增强

1. PDF / Word 解析。
2. 检索结果重排。
3. 批量导入销售话术。
4. 知识质量评分。
5. 知识过期提醒。
6. 多知识库融合检索。

## 14. 给 Codex 的开发步骤

开发时按以下顺序做，不要跳步：

1. 阅读现有知识库代码：
   - `AgentKnowledgeBaseDO`
   - `AgentKnowledgeItemDO`
   - `AgentKnowledgeChunkDO`
   - `AgentKnowledgeRetrievalServiceImpl`
   - `AgentKnowledgeIndexServiceImpl`
   - `AgentConversationContextBuilder`
   - `AgentKnowledgeReplyGenerator`
2. 补 SQL migration：
   - pgvector extension
   - knowledge tables
   - vector column
   - vector index
3. 补后端接口：
   - retrieval-test
   - retrieval-log
   - document upload / parse
4. 补 Service：
   - DocumentService
   - Parser
   - RetrievalLogService
5. 修复控制台知识库页面乱码和交互。
6. 接入自动回复日志。
7. 补 Frontis Skill action。
8. 写测试。
9. 打包部署。

## 15. 测试要求

后端测试：

- 新增知识后生成 chunk。
- embedding API 成功时 chunk 状态为 READY。
- embedding API 失败时状态为 FAILED，但关键词兜底仍能检索。
- query 命中正确知识。
- 低置信度不自动回复。
- 红色风险知识不自动回复。
- `draft_reply` 返回 `matched_knowledge_title`。
- 自动回复写入 `agent_reply_decision.knowledge_refs`。
- retrieval log 写入命中详情。

前端测试：

- 能新增知识库。
- 能新增 FAQ。
- 能上传文档。
- 能重建索引。
- 能看到向量状态。
- 检索测试能展示命中片段。
- 页面中文无乱码。

联调测试：

1. 控制台新增知识：
   - 标题：企业版功能说明
   - 问题：企业版支持什么功能
   - 答案：企业版支持多微信账号、客户工作台、RAG 知识库和 DeepSeek 自动回复。
2. 在检索测试输入：
   - 企业版能做什么
3. 应命中该知识。
4. 给客户发送类似问题。
5. 自动回复 / 草稿应包含该知识内容。
6. 决策日志应记录命中的知识标题。

## 16. 部署要求

环境变量：

```text
AGENT_EMBEDDING_URL=http://10.200.2.244:8081/v1/embeddings
AGENT_EMBEDDING_MODEL=qwen3-embedding-4b
AGENT_EMBEDDING_API_KEY=
AGENT_RAG_TOP_K=5
AGENT_RAG_MAX_DISTANCE=0.75
AGENT_RAG_CHUNK_SIZE=900
AGENT_RAG_CHUNK_OVERLAP=120
```

数据库：

```text
PostgreSQL 16
pgvector
```

部署顺序：

1. 备份数据库。
2. 执行 SQL migration。
3. 部署新的 `sales-server.jar`。
4. 部署新的控制台前端包。
5. 验证 `/admin-api/agent/knowledge/**`。
6. 验证 Frontis `draft_reply`。
7. 验证真实 GeWe 消息自动回复链路。

## 17. 验收标准

知识库达到正式可用，必须满足：

- 控制台能创建知识库和知识条目。
- 新增知识后自动生成 chunk 和 embedding。
- 检索测试能命中正确知识。
- 自动回复能使用知识库和 DeepSeek 生成回复。
- 敏感知识不会自动发送。
- Frontis 能看到知识命中来源。
- 后端日志能追踪每次检索和回复决策。
- 所有链路只走 Java 后端，不依赖 Python。

## 18. 重要约束

- 不要新增 Python 服务。
- 不要让 Frontis 直接调用 Python。
- 不要让控制台保存到另一套数据库。
- 不要把 DeepSeek Key、GeWe Token、数据库密码写死进代码。
- 不要把报价、合同、退款、投诉类知识直接用于自动发送。
- 不要把 `wxid`、原始 GeWe 字段或乱码展示给销售人员。
