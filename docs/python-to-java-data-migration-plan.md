# Python Legacy Data to Java Agent Data Migration Plan

## Current Database Reality

In the current local environment, the Python backend and Java backend both point to the same PostgreSQL database:

```text
host: 127.0.0.1
port: 15432
database: ai_sales
```

Therefore, the migration is not a cross-database copy in local development. It is a table-model migration:

```text
legacy Python tables -> Java agent_* tables
```

## Backup First

Before running migration SQL, create a PostgreSQL custom-format backup:

```bash
pg_dump -U ai_sales -Fc ai_sales > ai_sales_pre_java_migration_YYYYMMDD-HHMMSS.dump
```

Local backup created during development:

```text
backups/ai_sales_pre_java_migration_20260522-135925.dump
```

Do not run full migration on company test or production data without a fresh backup.

## Migration Script

Main script:

```text
sql/postgresql/agent-python-to-java-migration.sql
```

The script creates and uses:

```text
agent_python_migration_map
```

This mapping table records:

```text
source_table + source_id -> target_table + target_id
```

This is required for:

- idempotent reruns
- auditability
- cleanup and rollback analysis
- avoiding tenant/account/message cross-linking

## Mapping Rules

| Legacy Python table | Java target table | Key rule |
|---|---|---|
| `tenants` | `agent_agent` | one default Personal Sales Assistant agent per tenant |
| `gewe_providers` | `agent_gewe_credential` | provider becomes Java GeWe credential |
| `gewechat_accounts` | `agent_wechat_account` | `provider + wxid` is the stable identity |
| `wechat_contacts` | `agent_wechat_contact` | account + external wxid/chatroom id |
| `message_threads` | `agent_conversation` | account + contact |
| `wechat_messages` | `agent_message` | conversation + message key |
| `reply_policies` | `agent_wechat_contact` policy fields | thread policy becomes contact-level override |

## Identity Rule

Do not use GeWe `app_id` as the primary ownership identity.

Use:

```text
callback token -> GeWe credential/provider -> wxid -> WeChat account
```

`app_id` is only the current GeWe cloud-device id. It may change if a GeWe node is deleted and recreated.

## Smoke Verification Result

A temporary smoke dataset was inserted into legacy Python tables and migrated.

Verified target result:

```text
contact: Smoke Thread
message: hello from python legacy
reply_mode: AUTO_REPLY
quiet_seconds: 10
```

The smoke data was cleaned from both legacy and Java target tables after verification.

## Recommended Full Migration Order

1. Stop write traffic or enter maintenance mode.
2. Create a fresh PostgreSQL backup.
3. Apply required Java schema migrations.
4. Run `agent-python-to-java-migration.sql`.
5. Verify migration counts by mapping table.
6. Verify console:
   - WeChat account list
   - conversation list
   - reply policy update
   - auto-reply flow
7. Verify Frontis:
   - account status
   - recent messages
   - generated reply
   - account isolation
8. Only after validation, switch GeWe callback and Frontis Skill endpoint to the Java backend.

## Rollback Principle

Rollback should prefer database restore from backup for full migration failures.

For small-batch tests, use `agent_python_migration_map` to locate target rows created from specific legacy rows.
