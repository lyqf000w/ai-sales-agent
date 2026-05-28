# AI Sales Agent V1.4 Operational Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete V1.4 运营配置 so operations can enrich Agent configuration, publish/version it, see basic operating statistics, and ensure customer tags participate in reply context.

**Architecture:** Keep the existing `sales-module-agent` layering. Extend `agent_agent` as the editable draft source, add `agent_config_version` for published snapshots, add a small statistics service for today metrics, and enrich reply context with customer level/tag names. Frontend changes stay inside existing Agent pages plus one lightweight statistics page.

**Tech Stack:** Spring Boot, MyBatis Plus, PostgreSQL JSONB, Vue 3, Element Plus, existing `request` API wrappers.

---

## Task 1: Schema And Plan Tracking

- [x] Add Agent operation config fields: tone, welcome message, handover message, follow-up policy, material priority, published config.
- [x] Add `agent_config_version` table and sequence.
- [x] Add statistics menu permissions.
- [x] Update baseline SQL and create incremental V1.4 SQL.

## Task 2: Tests First

- [x] Add publishing service test that creates a config version and updates online version.
- [x] Add draft update test that increments draft version after an Agent has been published.
- [x] Add reply context test proving customer level and tag names enter the context snapshot.
- [x] Verify the new tests fail before implementation is complete.

## Task 3: Backend Publishing And Version History

- [x] Add `AgentConfigVersionDO`, mapper, VOs, service methods, and controller endpoints.
- [x] Extend Agent save/response VOs and DO fields.
- [x] Update Agent update behavior so edits save a draft without changing online version.
- [x] Publish Agent config by snapshotting current Agent fields and writing version history.

## Task 4: Backend Context And Statistics

- [x] Extend reply context with customer id, customer level, and tag names.
- [x] Load tag names in `AgentConversationContextBuilder`.
- [x] Persist customer tags and level in reply decision context snapshot.
- [x] Add `/agent/statistics/summary` with today message, auto reply, pending review, and risk conversation counts.

## Task 5: Frontend Operational Controls

- [x] Add operation config controls to Agent form.
- [x] Add publish action and version history drawer to Agent page.
- [x] Add `src/api/agent/statistics` and `src/views/agent/statistics/index.vue`.
- [x] Add menu seed for operational statistics.

## Task 6: Verification And Commit

- [x] Apply incremental SQL to local PostgreSQL.
- [x] Run focused backend tests.
- [x] Run `mvn -pl sales-server -am -DskipTests package`.
- [x] Run frontend build and filtered vue-tsc check for agent files.
- [x] Restart backend and smoke publish/version/statistics behavior.
- [x] Commit the completed V1.4 work.
