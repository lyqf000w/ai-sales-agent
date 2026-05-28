# AI Sales Agent V1.3 Auto Reply And Risk Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete V1.3 自动回复与风控. The system should auto-send only when policy permits, route uncertain or risky replies into review, pause red-risk conversations into takeover, and provide a standalone risk conversation view.

**Scope:** `sales-module-agent`, `sales-ui-admin/src/api/agent`, `sales-ui-admin/src/views/agent`, PostgreSQL SQL seed files.

**Out Of Scope:** V1.4 publish/version history, daily recap, full LLM classifier implementation. V1.3 reserves LLM classifier rule type but keeps deterministic keyword/regex matching active.

## Task 1: Schema And Menu

- [x] Add Agent policy fields: confidence threshold, max continuous auto reply, quiet minutes, business hours.
- [x] Add sensitive rule scope fields: Agent and route app.
- [x] Add risk conversation menu and permissions.
- [x] Update baseline SQL and create an incremental SQL file.

## Task 2: Tests First

- [x] Add a policy test for confidence threshold routing to manual review.
- [x] Add a policy test for continuous auto reply limit routing to manual review.
- [x] Add a policy test for human takeover pausing auto reply.
- [x] Add a policy test for red sensitive rule causing human takeover.
- [x] Verify the tests fail before production behavior exists or while the behavior is incomplete.

## Task 3: Backend Risk Policy

- [x] Add pure policy evaluator for auto reply and risk decisions.
- [x] Update auto reply service to use confidence, max continuous count, business hours, quiet minutes, sensitive rule action, and human takeover state.
- [x] Persist decision reason, risk level, guardrail hits, and review status consistently.
- [x] Reset or pause conversation state when routed to manual review or takeover.

## Task 4: Risk APIs

- [x] Add risk page request/response VOs.
- [x] Add mapper query for risk conversations.
- [x] Add risk service/controller with page, messages, takeover, and close/resolve endpoints.
- [x] Ensure takeover records user and time and prevents later auto replies.

## Task 5: Frontend Risk And Configuration

- [x] Add Agent form controls for V1.3 policy fields.
- [x] Add sensitive rule controls for Agent, route app, LLM classifier placeholder, and action.
- [x] Add `src/api/agent/risk`.
- [x] Add `src/views/agent/risk/index.vue` with filters, context drawer, takeover and close actions.

## Task 6: Verification And Commit

- [x] Apply incremental SQL to local PostgreSQL.
- [x] Run focused backend tests.
- [x] Run `mvn -pl sales-server -am -DskipTests package`.
- [x] Run frontend build and filtered vue-tsc check for agent files.
- [x] Restart backend and smoke red-rule/takeover behavior.
- [x] Commit the completed V1.3 work.
