# AI Sales Agent V1 Config And Auto Reply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first complete `ai-sales-agent` module version: Agent configuration, customer tags, sensitive rules, knowledge base, and an automatic reply pipeline for Gewe WeChat conversations.

**Architecture:** Keep all feature code inside `sales-module-agent` and `sales-ui-admin/src/api|views/agent`. Webhook ingestion stays the entry point; after saving an inbound message it invokes an auto-reply orchestrator that generates a reply draft from enabled knowledge items, checks sensitive rules, then either creates a pending-review AI message or sends it through Gewe `message/postText` when the Agent reply mode allows it and account credentials are configured.

**Tech Stack:** Spring Boot 3.5, MyBatis Plus, PostgreSQL JSONB, Vue 3, Element Plus, existing tenant/security/menu conventions.

---

### Task 1: Schema And Menus

**Files:**
- Modify: `sql/postgresql/agent.sql`
- Create: `sql/postgresql/agent-v1-config-auto-reply.sql`

- [ ] **Step 1: Add incremental SQL**

Create tables:
- `agent_contact_tag`
- `agent_contact_tag_rel`
- `agent_sensitive_rule`
- `agent_knowledge_item`

Alter `agent_wechat_account` with:
- `gewe_api_base_url varchar(255)`
- `gewe_token varchar(512)`

Add menus:
- `Agent 配置` -> `agent/agent/index`
- `客户标签` -> `agent/tag/index`
- `敏感规则` -> `agent/sensitiveRule/index`
- `知识库` -> `agent/knowledge/index`

- [ ] **Step 2: Apply incremental SQL to local PostgreSQL**

Run:

```bash
docker cp sql/postgresql/agent-v1-config-auto-reply.sql ai-sales-postgres:/tmp/agent-v1-config-auto-reply.sql
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -v ON_ERROR_STOP=1 -f /tmp/agent-v1-config-auto-reply.sql
```

Expected: `ALTER TABLE`, `CREATE TABLE`, `INSERT 0 ...`, no errors.

- [ ] **Step 3: Commit schema**

Run:

```bash
git add sql/postgresql/agent.sql sql/postgresql/agent-v1-config-auto-reply.sql
git commit -m "feat(agent): add v1 config schema"
```

### Task 2: Core Reply Rules Tests

**Files:**
- Create: `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/reply/AgentSensitiveRuleMatcherTest.java`
- Create: `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/reply/AgentKnowledgeReplyGeneratorTest.java`

- [ ] **Step 1: Write failing tests**

Cover these behaviors:
- keyword sensitive rules match case-insensitively and return the highest-risk match.
- regex sensitive rules match text and return the configured action.
- knowledge generation chooses the highest priority enabled item whose keyword appears in the customer message.
- generation returns blank when no enabled knowledge item matches.

- [ ] **Step 2: Run tests to verify red**

Run:

```bash
mvn -pl sales-module-agent -Dtest=AgentSensitiveRuleMatcherTest,AgentKnowledgeReplyGeneratorTest test
```

Expected: compilation failure because matcher/generator classes do not exist.

### Task 3: Backend Domain And CRUD APIs

**Files:**
- Create data objects, mappers, controllers, VOs, services under `sales-module-agent/src/main/java/cn/ai/sales/module/agent/`
- Modify: `AgentConstants.java`
- Modify: `ErrorCodeConstants.java`
- Modify: account DO/VO/service to include Gewe send credentials.

- [ ] **Step 1: Add DOs and mappers**

Create:
- `AgentContactTagDO`, `AgentContactTagRelDO`
- `AgentSensitiveRuleDO`
- `AgentKnowledgeItemDO`

Create mapper methods for page query, duplicate name checks, enabled-rule list, enabled-knowledge list, and contact tag relations.

- [ ] **Step 2: Add services/controllers**

Create CRUD APIs:
- `/agent/agent`
- `/agent/tag`
- `/agent/sensitive-rule`
- `/agent/knowledge`

Add contact tag assignment APIs:
- `GET /agent/contact/tags?contactId=`
- `PUT /agent/contact/tags`

- [ ] **Step 3: Run backend compile**

Run:

```bash
mvn -pl sales-module-agent -am -DskipTests package
```

Expected: compile success.

### Task 4: Auto Reply Pipeline

**Files:**
- Create: `service/reply/AgentAutoReplyService.java`
- Create: `service/reply/AgentAutoReplyServiceImpl.java`
- Create: `service/reply/AgentSensitiveRuleMatcher.java`
- Create: `service/reply/AgentKnowledgeReplyGenerator.java`
- Create: `service/reply/AgentGeneratedReply.java`
- Create: `service/gewe/GeweMessageClient.java`
- Create: `service/gewe/GeweTextSendResult.java`
- Modify: `AgentWebhookServiceImpl.java`
- Modify: `AgentConversationController.java`
- Modify: `AgentConversationService.java`
- Modify: `AgentConversationServiceImpl.java`

- [ ] **Step 1: Implement matcher and generator**

Make tests from Task 2 pass. Keep generation deterministic: return the matched knowledge item answer content and write matched knowledge title into audit metadata.

- [ ] **Step 2: Invoke auto reply after inbound messages**

Only trigger when:
- message direction is inbound
- account has an enabled Agent
- Agent reply mode is `MANUAL_CONFIRM` or `AUTO_REPLY`

If no knowledge matches, do nothing. If sensitive rules match customer message or generated reply, create an AI outbound message with `SEND_STATUS_PENDING_REVIEW`, set `matched_policy` and `audit_note`, set conversation risk/status to waiting confirmation. If clear:
- `MANUAL_CONFIRM`: create pending-review AI message.
- `AUTO_REPLY`: call Gewe `POST {baseUrl}/gewe/v2/api/message/postText`; on success create sent AI message and increment continuous auto reply count; on failure create failed AI message with audit note.

- [ ] **Step 3: Add manual send and review endpoints**

Add:
- `POST /agent/conversation/send-message`
- `POST /agent/conversation/approve-message`
- `POST /agent/conversation/reject-message`

Manual send and approval use the same Gewe client when account credentials are configured.

- [ ] **Step 4: Run backend tests**

Run:

```bash
mvn -pl sales-module-agent -Dtest=GeweCallbackParserTest,AgentSensitiveRuleMatcherTest,AgentKnowledgeReplyGeneratorTest test
```

Expected: all tests pass.

### Task 5: Frontend Admin Pages

**Files:**
- Create APIs under `sales-ui/sales-ui-admin/src/api/agent/agent|tag|sensitiveRule|knowledge`
- Create views under `sales-ui/sales-ui-admin/src/views/agent/agent|tag|sensitiveRule|knowledge`
- Modify: `sales-ui/sales-ui-admin/src/api/agent/wechatAccount/index.ts`
- Modify: `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/WechatAccountForm.vue`
- Modify: `sales-ui/sales-ui-admin/src/api/agent/contact/index.ts`
- Modify: `sales-ui/sales-ui-admin/src/views/agent/contact/index.vue`
- Modify: `sales-ui/sales-ui-admin/src/api/agent/conversation/index.ts`
- Modify: `sales-ui/sales-ui-admin/src/views/agent/conversation/index.vue`

- [ ] **Step 1: Add CRUD pages**

Build compact admin pages for Agent, tags, sensitive rules, and knowledge items using existing `ContentWrap`, `Pagination`, `Icon`, and Element Plus patterns.

- [ ] **Step 2: Enrich contact and conversation pages**

Contact page supports tag assignment. Conversation drawer shows AI draft status, approve/reject/send actions, and manual reply input.

- [ ] **Step 3: Verify frontend**

Run:

```bash
pnpm build:local
NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit --pretty false 2>&1 | rg 'src/(api|views)/agent' || true
```

Expected: build succeeds and filtered TypeScript output is empty.

### Task 6: Local Smoke Test And Commit

**Files:**
- All files changed above.

- [ ] **Step 1: Rebuild server**

Run:

```bash
mvn -pl sales-server -am -DskipTests package
```

Expected: build success.

- [ ] **Step 2: Restart backend and exercise webhook**

Run local backend with `--spring.profiles.active=local`, seed one enabled Agent, account, knowledge item, and callback payload. Send a Gewe-like inbound text callback and verify one inbound message plus one AI reply draft/sent message exists.

- [ ] **Step 3: Commit**

Run:

```bash
git add .
git commit -m "feat(agent): add tags knowledge rules and auto reply"
```

Expected: clean working tree.

