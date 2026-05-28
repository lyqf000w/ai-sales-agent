# Personal Sales Assistant Java Migration Deploy Notes

## Scope

This note records the current Java-side migration requirements for the Personal Sales Assistant service.

The Java backend is now the target service for:

- Frontis Skill calls
- GeWe public callback
- Console data access
- WeChat account identity lookup
- Reply policy management

## Required Database Migrations

Apply these PostgreSQL scripts before starting the new backend package:

1. `sql/postgresql/agent-gewe-identity-v2.sql`
2. `sql/postgresql/agent-gewe-provider-callback-url.sql`
3. `sql/postgresql/agent-default-quiet-seconds-180.sql`

These scripts provide:

- `callback_token -> GeWe credential/provider`
- `provider + wxid -> WeChat account`
- provider-level callback URL
- default quiet window of 180 seconds

## Runtime Requirements

Start the Java service with Asia/Shanghai timezone:

```bash
java -Duser.timezone=Asia/Shanghai -jar sales-server.jar
```

When running in Docker, pass the JVM argument before `-jar`:

```bash
docker run ... eclipse-temurin:17-jre \
  java -Duser.timezone=Asia/Shanghai -jar /app/sales-server.jar ...
```

## Important API Entrypoints

GeWe callback:

```text
POST /api/v1/gewechat/callback?token=<gewe-provider-callback-token>
```

Frontis Skill:

```text
GET  /api/v1/skills/personal-sales-assistant/manifest
POST /api/v1/skills/personal-sales-assistant/run
```

## Skill Actions Currently Supported

- `service_session_init`
- `wechat_account_status`
- `customer_threads`
- `recent_messages`
- `send_reply`
- `reply_policy_list`
- `reply_policy_set`
- `reply_policy_delete`
- `reply_decision_list`
- `approve_reply`
- `reject_reply`

## Verification Checklist

1. `GET /api/v1/skills/personal-sales-assistant/manifest` returns `200`.
2. `POST /api/v1/gewechat/callback?token=...` with `{}` returns callback probe success.
3. A real GeWe inbound message can be stored by `callback token + wxid`.
4. The same wxid is resolved to the correct tenant/user/account even when GeWe appId changes.
5. If the reply policy quiet window is 180 seconds:
   - first inbound message creates a pending reply,
   - second inbound message before timeout resets the pending message,
   - pending due time is message time plus 180 seconds.
6. Skill actions for reply policy return success and do not return HTTP 500.

## Notes

- Do not use appId as the primary identity for WeChat ownership. appId is a GeWe cloud-device identifier and may change after deleting and recreating the node.
- Do not store real API keys or GeWe tokens in this document or in committed source files.
- Frontis and the console should call the same Java APIs to avoid split state.
