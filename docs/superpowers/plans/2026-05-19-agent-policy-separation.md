# Agent Policy Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Agent persona/model configuration from account/contact reply policy while preserving the existing Gewe message loop.

**Architecture:** `agent_agent` becomes persona and model configuration. `agent_wechat_account` owns default reply policy. `agent_wechat_contact` owns nullable field-level policy overrides. Runtime reply flow resolves an `AgentReplyPolicy` from contact > account > defaults, then evaluates policy independently of Agent persona.

**Tech Stack:** Spring Boot, MyBatis Plus, PostgreSQL JSONB, Vue 3, Element Plus, existing agent module patterns.

---

## File Structure

- Modify backend Agent persona files under `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/agent`, `service/agent`, `dal/dataobject/AgentDO.java`.
- Modify WeChat account policy files under `controller/admin/account`, `service/account`, `dal/dataobject/AgentWechatAccountDO.java`.
- Modify contact policy override files under `controller/admin/contact`, `service/contact`, `dal/dataobject/AgentWechatContactDO.java`.
- Create `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/reply/AgentReplyPolicy.java`.
- Create `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/reply/AgentReplyPolicyResolver.java`.
- Modify `AgentAutoReplyPolicyEvaluator`, `AgentAutoReplyServiceImpl`, and `AgentReplyContext`.
- Modify SQL in `sql/postgresql/agent.sql` and add `sql/postgresql/agent-policy-separation.sql`.
- Modify frontend API/forms in `sales-ui/sales-ui-admin/src/api/agent/**` and `sales-ui/sales-ui-admin/src/views/agent/**`.

## Task 1: Tests First

- [ ] Add `AgentReplyPolicyResolverTest` covering account fallback and contact field-level overrides.
- [ ] Add `AgentAutoReplyPolicyEvaluatorTest` coverage proving evaluator accepts `AgentReplyPolicy`.
- [ ] Extend `AgentServiceImplTest` to assert publish snapshots include `systemPrompt`, `llmProvider`, and `llmModel`.
- [ ] Run focused tests and verify RED before implementation fills missing classes/fields.

## Task 2: Backend Data Model And VOs

- [ ] Extend `AgentDO`, `AgentSaveReqVO`, `AgentRespVO` with `systemPrompt`, `llmProvider`, `llmModel`.
- [ ] Remove Agent policy fields from the frontend-facing Agent form later, but keep backend fields temporarily for database compatibility.
- [ ] Extend `AgentWechatAccountDO`, save/resp VOs with account-level `replyMode`, `confidenceThreshold`, `maxContinuousAutoReply`, `quietMinutes`, `businessHours`.
- [ ] Extend `AgentWechatContactDO`, resp/update VOs with nullable contact-level override fields.
- [ ] Add a contact strategy update endpoint under `/agent/contact/update-reply-policy`.

## Task 3: Runtime Policy Resolution

- [ ] Add `AgentReplyPolicy` record.
- [ ] Add `AgentReplyPolicyResolver` that resolves defaults, account policy, and non-null contact overrides.
- [ ] Change `AgentAutoReplyPolicyEvaluator.evaluate(...)` to accept `AgentReplyPolicy`.
- [ ] Change `AgentAutoReplyServiceImpl` to resolve policy from account/contact and pass it into evaluator.
- [ ] Add Agent persona/model fields and effective policy into reply context/decision snapshot.

## Task 4: Publishing And Compatibility

- [ ] Update Agent create/update defaults so new Agents default to `DEEPSEEK` and `deepseek-v4-pro`.
- [ ] Expose backend LLM model options for `deepseek-v4-pro` and `deepseek-v4-flash`, and validate Agent saves against them.
- [ ] Update Agent publish snapshot to include persona/model fields and stop treating account/contact policy as Agent runtime policy.
- [ ] Keep old Agent policy columns present for compatibility, but runtime should use account/contact policy only.

## Task 5: SQL Migration

- [ ] Update baseline `sql/postgresql/agent.sql`.
- [ ] Add incremental `sql/postgresql/agent-policy-separation.sql`.
- [ ] Migration adds Agent model columns, account policy columns, contact override columns.
- [ ] Migration copies existing Agent policy fields to bound WeChat accounts.

## Task 6: Frontend

- [ ] Agent API and page expose persona/model fields and hide policy fields.
- [ ] WeChat account API/form expose Agent binding and default reply policy.
- [ ] Contact API/page expose policy override dialog with “继承微信号策略” state.
- [ ] Keep layouts dense and operational; no landing/marketing surface.

## Task 7: Verification And Commit

- [ ] Apply incremental SQL to Docker PostgreSQL.
- [ ] Run focused backend tests.
- [ ] Run `mvn -pl sales-server -am -DskipTests package`.
- [ ] Run filtered `vue-tsc` check for agent files.
- [ ] Run `pnpm build:local`.
- [ ] Restart backend on `48080`.
- [ ] Smoke Agent, WeChat account, and contact policy pages.
- [ ] Commit the completed implementation.
