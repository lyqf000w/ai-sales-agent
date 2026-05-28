# Sales Insight Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split reply-control concepts from sales-insight concepts so operators see reply strategy/queues separately from customer buying signals and tags.

**Architecture:** Add sales insight fields to `agent_wechat_contact`, expose them through the contact page API, and keep legacy risk fields internal for compatibility. Update conversation and contact pages so primary UI shows reply control plus sales insight instead of green/yellow/red risk.

**Tech Stack:** Spring Boot, MyBatis Plus, PostgreSQL, Vue 3, Element Plus, Vite.

---

### Task 1: Document The Decision

**Files:**
- Create: `docs/agent-sales-insight-policy.md`
- Create: `docs/superpowers/plans/2026-05-20-sales-insight-policy.md`

- [ ] Explain why risk level is no longer the primary UI concept.
- [ ] Define reply control, sales insight, and compatibility behavior.
- [ ] Document default sales tags.

### Task 2: Add Backend Sales Insight Fields

**Files:**
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/AgentConstants.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentWechatContactDO.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/contact/vo/AgentWechatContactPageReqVO.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/contact/vo/AgentWechatContactRespVO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/contact/vo/AgentWechatContactUpdateSalesInsightReqVO.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentWechatContactMapper.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/contact/AgentWechatContactService.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/contact/AgentWechatContactServiceImpl.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/contact/AgentWechatContactController.java`

- [ ] Add constants for purchase intention, sales stage, customer sentiment, and follow-up priority.
- [ ] Add four fields to contact DO, response VO, and page request VO.
- [ ] Add an update request and service method for sales insight.
- [ ] Extend mapper page filters to support the new fields.

### Task 3: Add SQL Migration And Defaults

**Files:**
- Modify: `sql/postgresql/agent.sql`
- Create: `sql/postgresql/agent-sales-insight.sql`

- [ ] Add `purchase_intention`, `sales_stage`, `customer_sentiment`, and `follow_up_priority` columns.
- [ ] Add column comments explaining sales-insight semantics.
- [ ] Seed default tags: 价格敏感、关注交付、决策人、竞品对比.

### Task 4: Update Frontend API

**Files:**
- Modify: `sales-ui/sales-ui-admin/src/api/agent/contact/index.ts`

- [ ] Add sales insight fields to `AgentWechatContactVO`.
- [ ] Add an update request type.
- [ ] Add `updateWechatContactSalesInsight`.

### Task 5: Update Customer Contacts Page

**Files:**
- Modify: `sales-ui/sales-ui-admin/src/views/agent/contact/index.vue`

- [ ] Remove risk filter and risk column from the primary table.
- [ ] Add sales insight filters.
- [ ] Render sales insight and customer tags in a single table column.
- [ ] Replace separate tag editing with a sales insight dialog that edits fields and tags together.

### Task 6: Update Conversation Page

**Files:**
- Modify: `sales-ui/sales-ui-admin/src/views/agent/conversation/index.vue`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/AgentConstants.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentConversationMapper.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/conversation/vo/AgentConversationPageReqVO.java`

- [ ] Replace “风险” queue with “人工接管”.
- [ ] Remove risk-level filter from the page.
- [ ] Show reply control and sales insight in the right side panel.
- [ ] Load and render contact tags for the active conversation.

### Task 7: Verify

**Commands:**
- `mvn -pl sales-module-agent -am -Dtest=AgentConversationMapperTest,AgentWechatContactMapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl sales-server -am -DskipTests package`
- `pnpm build:local` from `sales-ui/sales-ui-admin`

- [ ] Run backend focused tests.
- [ ] Run backend package build.
- [ ] Run frontend build.
- [ ] Apply the PostgreSQL migration to the local Docker database.
- [ ] Verify customer and conversation pages in the browser.
