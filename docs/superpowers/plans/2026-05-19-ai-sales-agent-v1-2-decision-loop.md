# AI Sales Agent V1.2 Decision Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete V1.2 Agent 决策闭环 without pulling in V1.3 风险可视 or V1.4 发布复盘. Customer messages should produce auditable reply decisions, operations should have a standalone review queue, and reviewers can approve, edit, or reject suggestions with records preserved.

**Scope:** `sales-module-agent`, `sales-ui-admin/src/api/agent`, `sales-ui-admin/src/views/agent`, PostgreSQL SQL seed files.

**Out Of Scope:** Full confidence threshold policy, business hours, red-risk dashboard, config publish/history, knowledge URL ingestion, daily recap.

## Task 1: Schema And Roadmap

- [x] Keep the roadmap naming as V1.2 Agent 决策闭环, V1.3 自动回复与风控, V1.4 运营配置, with operating themes noted separately.
- [x] Add `agent_reply_decision` with inbound message, suggested message, sent message, decision type, risk, confidence, context snapshot, knowledge refs, guardrail hits, review status, review note, reviewer, and review time.
- [x] Add menu and permissions for `AI 销冠 / 回复审核`.
- [x] Update baseline SQL and create an incremental SQL file.

## Task 2: Tests First

- [x] Add a backend test for reply review edit/reject behavior against in-memory service dependencies where feasible.
- [x] Add a backend test for reply decision status transitions when approving, editing, and rejecting.
- [x] Verify the new tests fail before production code exists or while production code lacks the behavior.

## Task 3: Backend Decision Records

- [x] Add `AgentReplyDecisionDO` and mapper.
- [x] Insert a decision record whenever auto-reply generates a pending review or sends automatically.
- [x] Add a review service/controller with page query, approve, and reject APIs.
- [x] Approval can optionally replace suggested content before sending.
- [x] Rejection records a reason and does not send to Gewe.
- [x] Existing conversation approve/reject endpoints should delegate to the same decision-aware service where practical.

## Task 4: Frontend Review Queue

- [x] Add `src/api/agent/review`.
- [x] Add `src/views/agent/review/index.vue`.
- [x] Support filtering pending/all decisions.
- [x] Show suggested content, decision reason, risk level, confidence, and audit metadata.
- [x] Support approve, edit-and-send, and reject-with-reason flows.

## Task 5: Verification And Commit

- [x] Apply incremental SQL to local PostgreSQL.
- [x] Run focused backend tests.
- [x] Run `mvn -pl sales-server -am -DskipTests package`.
- [x] Run frontend build and filtered vue-tsc check for agent files.
- [x] Restart backend if needed and smoke a Gewe callback into a pending review decision.
- [x] Commit the completed V1.2 work.
