# Reply Policy UX And Delay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate account policy from basic account information, restore per-field contact inheritance, and make quiet seconds mean delayed reply after the customer's last message.

**Architecture:** Account and contact policies keep the same persisted fields. `AgentReplyPolicyResolver` owns defaults and inheritance. `AgentAutoReplyServiceImpl` avoids generating or sending a reply while the quiet window is still open; UI explains and edits the policy without exposing non-configurable counters.

**Tech Stack:** Spring Boot, MyBatis Plus, JUnit 5, Vue 3, Element Plus, TypeScript.

---

### Task 1: Backend Policy Defaults And Quiet Window

**Files:**
- Modify: `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/reply/AgentReplyPolicyResolverTest.java`
- Modify: `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/reply/AgentAutoReplyPolicyEvaluatorTest.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/reply/AgentReplyPolicyResolver.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/reply/AgentAutoReplyPolicyEvaluator.java`
- Modify: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/reply/AgentAutoReplyServiceImpl.java`

- [ ] Add failing tests for account defaults: `quietSeconds = 60`, `businessHours = 08:00-22:00`.
- [ ] Add failing test that quiet window returns a no-op waiting decision instead of manual confirmation.
- [ ] Implement resolver defaults.
- [ ] Implement quiet-window decision reason and no-op handling.
- [ ] Run agent reply tests.

### Task 2: WeChat Account Policy UI

**Files:**
- Modify: `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/WechatAccountForm.vue`

- [ ] Split the form into visible sections: basic information, GeWe integration, conversation policy, status.
- [ ] Default quiet seconds to `60` and business hours to `08:00-22:00`.
- [ ] Require both time fields and quiet seconds in the UI.
- [ ] Add helper copy explaining that quiet seconds waits after the last customer message before analyzing the full conversation.

### Task 3: Customer Workbench Policy UI

**Files:**
- Modify: `sales-ui/sales-ui-admin/src/views/agent/conversation/index.vue`

- [ ] Move `恢复原策略` to the conversation header as a primary action.
- [ ] Remove `连续自动回复` from the reply control panel.
- [ ] Rework the contact policy editor so reply mode, quiet seconds, and business hours each have explicit inherit/override controls.
- [ ] Default override quiet seconds to `60` and override business hours to `08:00-22:00`.
- [ ] Add helper copy for quiet seconds.

### Task 4: Verification

**Files:**
- No source changes expected.

- [ ] Run `mvn -pl sales-module-agent -am -Dtest=AgentReplyPolicyResolverTest,AgentAutoReplyPolicyEvaluatorTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- [ ] Run `pnpm build:local` in `sales-ui/sales-ui-admin`.
- [ ] Repackage and restart the backend.
- [ ] Verify `http://localhost/agent/conversation` and `http://localhost/agent/wechatAccount` in the browser.
