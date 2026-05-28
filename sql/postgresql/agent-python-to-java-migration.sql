-- Personal Sales Assistant: migrate legacy Python tables into Java agent_* tables.
-- This script is designed to be idempotent and auditable.
--
-- Source tables:
--   tenants, users, gewe_providers, gewechat_accounts, wechat_contacts,
--   message_threads, wechat_messages, reply_policies
--
-- Target tables:
--   agent_agent, agent_gewe_credential, agent_wechat_account,
--   agent_wechat_contact, agent_conversation, agent_message

BEGIN;

CREATE TABLE IF NOT EXISTS agent_python_migration_map (
    source_table varchar(64) NOT NULL,
    source_id bigint NOT NULL,
    target_table varchar(64) NOT NULL,
    target_id bigint NOT NULL,
    migration_batch varchar(64) NOT NULL DEFAULT 'python_to_java_v1',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_table, source_id, target_table)
);

CREATE INDEX IF NOT EXISTS idx_agent_python_migration_map_target
    ON agent_python_migration_map (target_table, target_id);

-- 1. Ensure every legacy tenant has one Java agent.
WITH source_tenants AS (
    SELECT id AS tenant_id, COALESCE(NULLIF(name, ''), '个人销售助手') AS tenant_name
    FROM tenants
    WHERE status <> 'deleted'
),
existing_agent AS (
    SELECT tenant_id, MIN(id) AS agent_id
    FROM agent_agent
    WHERE deleted = 0
    GROUP BY tenant_id
),
inserted AS (
    INSERT INTO agent_agent (
        id, name, alias_name, owner_user_id, scene, target_customer_desc, system_prompt,
        llm_provider, llm_model, reply_mode, confidence_threshold, max_continuous_auto_reply,
        quiet_minutes, quiet_seconds, business_hours, status, draft_version, online_version,
        creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_agent_seq'),
           '个人销售助手',
           st.tenant_name || ' - 个人销售助手',
           tm.user_id,
           'PERSONAL_SALES_ASSISTANT',
           '企业销售微信客户',
           '你是个人销售助手，负责基于客户消息生成安全、专业、可追踪的销售回复。',
           'DEEPSEEK',
           'deepseek-chat',
           'MANUAL_CONFIRM',
           0.70,
           3,
           0,
           180,
           '{"start":"08:00","end":"22:00"}'::jsonb,
           0,
           1,
           0,
           'migration',
           CURRENT_TIMESTAMP,
           'migration',
           CURRENT_TIMESTAMP,
           0,
           st.tenant_id
    FROM source_tenants st
    LEFT JOIN existing_agent ea ON ea.tenant_id = st.tenant_id
    LEFT JOIN LATERAL (
        SELECT user_id
        FROM tenant_members
        WHERE tenant_id = st.tenant_id AND status = 'active'
        ORDER BY CASE role WHEN 'TENANT_ADMIN' THEN 0 WHEN 'MANAGER' THEN 1 ELSE 2 END, id
        LIMIT 1
    ) tm ON true
    WHERE ea.agent_id IS NULL
    RETURNING tenant_id, id
),
agent_mapping AS (
    SELECT st.tenant_id, COALESCE(ea.agent_id, i.id) AS agent_id
    FROM source_tenants st
    LEFT JOIN existing_agent ea ON ea.tenant_id = st.tenant_id
    LEFT JOIN inserted i ON i.tenant_id = st.tenant_id
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'tenants', tenant_id, 'agent_agent', agent_id
FROM agent_mapping
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 2. Migrate GeWe providers into Java credentials.
WITH source_providers AS (
    SELECT gp.id AS provider_id,
           COALESCE(ga.tenant_id, 1) AS tenant_id,
           COALESCE(NULLIF(gp.name, ''), 'GeWe') AS name,
           COALESCE(NULLIF(gp.api_base, ''), 'https://api.geweapi.com') AS api_base,
           NULLIF(gp.token_cipher, '') AS gewe_token,
           NULLIF(gp.callback_token_cipher, '') AS callback_token,
           gp.status
    FROM gewe_providers gp
    LEFT JOIN LATERAL (
        SELECT tenant_id
        FROM gewechat_accounts
        WHERE provider_id = gp.id
        ORDER BY id
        LIMIT 1
    ) ga ON true
    WHERE gp.status <> 'deleted'
),
existing_credential AS (
    SELECT sp.provider_id, agc.id AS credential_id
    FROM source_providers sp
    JOIN agent_gewe_credential agc
      ON agc.deleted = 0
     AND agc.tenant_id = sp.tenant_id
     AND (
            agc.callback_token = sp.callback_token
         OR agc.gewe_token = sp.gewe_token
         OR agc.gewe_api_base_url = sp.api_base
     )
),
inserted AS (
    INSERT INTO agent_gewe_credential (
        id, name, gewe_api_base_url, gewe_token, callback_token, callback_url,
        status, creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_gewe_credential_seq'),
           sp.name,
           sp.api_base,
           sp.gewe_token,
           COALESCE(sp.callback_token, md5('provider:' || sp.provider_id || ':' || clock_timestamp())),
           '/api/v1/gewechat/callback?token=' || COALESCE(sp.callback_token, md5('provider:' || sp.provider_id || ':' || clock_timestamp())),
           CASE WHEN sp.status = 'active' THEN 0 ELSE 1 END,
           'migration',
           CURRENT_TIMESTAMP,
           'migration',
           CURRENT_TIMESTAMP,
           0,
           sp.tenant_id
    FROM source_providers sp
    LEFT JOIN existing_credential ec ON ec.provider_id = sp.provider_id
    WHERE ec.credential_id IS NULL
    RETURNING id, tenant_id, gewe_api_base_url, callback_token
),
provider_mapping AS (
    SELECT sp.provider_id,
           COALESCE(ec.credential_id, i.id) AS credential_id
    FROM source_providers sp
    LEFT JOIN existing_credential ec ON ec.provider_id = sp.provider_id
    LEFT JOIN inserted i ON i.tenant_id = sp.tenant_id
        AND i.gewe_api_base_url = sp.api_base
        AND (sp.callback_token IS NULL OR i.callback_token = sp.callback_token)
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'gewe_providers', provider_id, 'agent_gewe_credential', credential_id
FROM provider_mapping
WHERE credential_id IS NOT NULL
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 3. Migrate WeChat accounts. wxid is the stable identity; app_id is only a GeWe cloud-device id.
WITH source_accounts AS (
    SELECT ga.*
    FROM gewechat_accounts ga
    WHERE ga.status <> 'deleted' AND NULLIF(ga.wxid, '') IS NOT NULL
),
resolved AS (
    SELECT sa.*,
           pm.target_id AS credential_id,
           am.target_id AS agent_id
    FROM source_accounts sa
    JOIN agent_python_migration_map pm
      ON pm.source_table = 'gewe_providers'
     AND pm.source_id = sa.provider_id
     AND pm.target_table = 'agent_gewe_credential'
    LEFT JOIN agent_python_migration_map am
      ON am.source_table = 'tenants'
     AND am.source_id = sa.tenant_id
     AND am.target_table = 'agent_agent'
),
upserted AS (
    INSERT INTO agent_wechat_account (
        id, gewe_credential_id, agent_id, owner_user_id, gewe_app_id, gewe_account_id,
        wechat_id, nickname, callback_token, callback_url, gewe_api_base_url, gewe_token,
        reply_mode, confidence_threshold, max_continuous_auto_reply, quiet_minutes,
        quiet_seconds, business_hours, login_status, status, last_heartbeat_time,
        creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_wechat_account_seq'),
           r.credential_id,
           r.agent_id,
           r.user_id,
           NULLIF(r.app_id, ''),
           r.wxid,
           r.wxid,
           r.nickname,
           md5('wechat-account:' || r.id || ':' || r.wxid),
           agc.callback_url,
           agc.gewe_api_base_url,
           agc.gewe_token,
           'MANUAL_CONFIRM',
           0.70,
           3,
           0,
           180,
           '{"start":"08:00","end":"22:00"}'::jsonb,
           CASE WHEN r.login_status = 'online' THEN 1 WHEN r.login_status = 'offline' THEN 2 ELSE 0 END,
           CASE WHEN r.status = 'active' THEN 0 ELSE 1 END,
           CASE WHEN r.login_status = 'online' THEN COALESCE(r.updated_at, CURRENT_TIMESTAMP) ELSE NULL END,
           'migration',
           COALESCE(r.created_at, CURRENT_TIMESTAMP),
           'migration',
           COALESCE(r.updated_at, CURRENT_TIMESTAMP),
           0,
           r.tenant_id
    FROM resolved r
    JOIN agent_gewe_credential agc ON agc.id = r.credential_id
    ON CONFLICT (tenant_id, gewe_credential_id, wechat_id)
    WHERE deleted = 0 AND gewe_credential_id IS NOT NULL AND wechat_id IS NOT NULL AND wechat_id <> ''
    DO UPDATE SET
        gewe_app_id = COALESCE(EXCLUDED.gewe_app_id, agent_wechat_account.gewe_app_id),
        owner_user_id = COALESCE(EXCLUDED.owner_user_id, agent_wechat_account.owner_user_id),
        nickname = COALESCE(EXCLUDED.nickname, agent_wechat_account.nickname),
        login_status = EXCLUDED.login_status,
        status = EXCLUDED.status,
        last_heartbeat_time = COALESCE(EXCLUDED.last_heartbeat_time, agent_wechat_account.last_heartbeat_time),
        update_time = CURRENT_TIMESTAMP
    RETURNING id, tenant_id, gewe_credential_id, wechat_id
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'gewechat_accounts', r.id, 'agent_wechat_account', awa.id
FROM resolved r
JOIN agent_wechat_account awa
  ON awa.deleted = 0
 AND awa.tenant_id = r.tenant_id
 AND awa.gewe_credential_id = r.credential_id
 AND awa.wechat_id = r.wxid
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- Backfill account mappings in a separate statement so rows inserted by the
-- previous statement are visible on the first migration run.
WITH source_accounts AS (
    SELECT ga.*
    FROM gewechat_accounts ga
    WHERE ga.status <> 'deleted' AND NULLIF(ga.wxid, '') IS NOT NULL
),
resolved AS (
    SELECT sa.*,
           pm.target_id AS credential_id
    FROM source_accounts sa
    JOIN agent_python_migration_map pm
      ON pm.source_table = 'gewe_providers'
     AND pm.source_id = sa.provider_id
     AND pm.target_table = 'agent_gewe_credential'
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'gewechat_accounts', r.id, 'agent_wechat_account', awa.id
FROM resolved r
JOIN agent_wechat_account awa
  ON awa.deleted = 0
 AND awa.tenant_id = r.tenant_id
 AND awa.gewe_credential_id = r.credential_id
 AND awa.wechat_id = r.wxid
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 4. Migrate contacts.
WITH source_contacts AS (
    SELECT wc.*
    FROM wechat_contacts wc
    WHERE NULLIF(wc.wxid, '') IS NOT NULL
),
resolved AS (
    SELECT sc.*,
           am.target_id AS agent_account_id
    FROM source_contacts sc
    JOIN agent_python_migration_map am
      ON am.source_table = 'gewechat_accounts'
     AND am.source_id = sc.gewechat_account_id
     AND am.target_table = 'agent_wechat_account'
),
upserted AS (
    INSERT INTO agent_wechat_contact (
        id, wechat_account_id, external_user_id, wechat_id, nickname, remark, avatar,
        customer_level, owner_user_id, risk_level, last_message_time, last_conversation_status,
        purchase_intention, sales_stage, customer_sentiment, follow_up_priority,
        creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_wechat_contact_seq'),
           r.agent_account_id,
           r.wxid,
           r.wxid,
           COALESCE(NULLIF(r.display_name, ''), NULLIF(r.nickname, ''), r.wxid),
           NULLIF(r.remark_name, ''),
           r.avatar_url,
           0,
           r.user_id,
           0,
           r.last_sync_at,
           0,
           'MEDIUM',
           'NEW_LEAD',
           'NEUTRAL',
           'NORMAL',
           'migration',
           COALESCE(r.created_at, CURRENT_TIMESTAMP),
           'migration',
           COALESCE(r.updated_at, CURRENT_TIMESTAMP),
           0,
           r.tenant_id
    FROM resolved r
    ON CONFLICT (tenant_id, wechat_account_id, external_user_id)
    WHERE deleted = 0
    DO UPDATE SET
        wechat_id = COALESCE(EXCLUDED.wechat_id, agent_wechat_contact.wechat_id),
        nickname = COALESCE(EXCLUDED.nickname, agent_wechat_contact.nickname),
        remark = COALESCE(EXCLUDED.remark, agent_wechat_contact.remark),
        avatar = COALESCE(EXCLUDED.avatar, agent_wechat_contact.avatar),
        last_message_time = COALESCE(EXCLUDED.last_message_time, agent_wechat_contact.last_message_time),
        update_time = CURRENT_TIMESTAMP
    RETURNING id, tenant_id, wechat_account_id, external_user_id
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'wechat_contacts', r.id, 'agent_wechat_contact', awc.id
FROM resolved r
JOIN agent_wechat_contact awc
  ON awc.deleted = 0
 AND awc.tenant_id = r.tenant_id
 AND awc.wechat_account_id = r.agent_account_id
 AND awc.external_user_id = r.wxid
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

WITH source_contacts AS (
    SELECT wc.*
    FROM wechat_contacts wc
    WHERE NULLIF(wc.wxid, '') IS NOT NULL
),
resolved AS (
    SELECT sc.*,
           am.target_id AS agent_account_id
    FROM source_contacts sc
    JOIN agent_python_migration_map am
      ON am.source_table = 'gewechat_accounts'
     AND am.source_id = sc.gewechat_account_id
     AND am.target_table = 'agent_wechat_account'
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'wechat_contacts', r.id, 'agent_wechat_contact', awc.id
FROM resolved r
JOIN agent_wechat_contact awc
  ON awc.deleted = 0
 AND awc.tenant_id = r.tenant_id
 AND awc.wechat_account_id = r.agent_account_id
 AND awc.external_user_id = r.wxid
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 5. Migrate threads. If no explicit contact row exists, create one from the thread.
WITH source_threads AS (
    SELECT mt.*
    FROM message_threads mt
),
resolved AS (
    SELECT st.*,
           am.target_id AS agent_account_id,
           COALESCE(NULLIF(st.group_room_id, ''), NULLIF(st.source_session_id, '')) AS external_user_id
    FROM source_threads st
    JOIN agent_python_migration_map am
      ON am.source_table = 'gewechat_accounts'
     AND am.source_id = st.gewechat_account_id
     AND am.target_table = 'agent_wechat_account'
),
insert_missing_contacts AS (
    INSERT INTO agent_wechat_contact (
        id, wechat_account_id, external_user_id, wechat_id, nickname, customer_level,
        risk_level, last_message_time, last_conversation_status,
        purchase_intention, sales_stage, customer_sentiment, follow_up_priority,
        creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_wechat_contact_seq'),
           r.agent_account_id,
           r.external_user_id,
           r.external_user_id,
           COALESCE(NULLIF(r.display_name, ''), r.external_user_id),
           0,
           0,
           r.last_message_at,
           0,
           'MEDIUM',
           'NEW_LEAD',
           'NEUTRAL',
           'NORMAL',
           'migration',
           COALESCE(r.created_at, CURRENT_TIMESTAMP),
           'migration',
           COALESCE(r.updated_at, CURRENT_TIMESTAMP),
           0,
           r.tenant_id
    FROM resolved r
    WHERE r.external_user_id IS NOT NULL
    ON CONFLICT (tenant_id, wechat_account_id, external_user_id)
    WHERE deleted = 0
    DO UPDATE SET
        nickname = COALESCE(EXCLUDED.nickname, agent_wechat_contact.nickname),
        last_message_time = COALESCE(EXCLUDED.last_message_time, agent_wechat_contact.last_message_time),
        update_time = CURRENT_TIMESTAMP
    RETURNING id
),
thread_targets AS (
    SELECT r.*,
           awc.id AS agent_contact_id
    FROM resolved r
    JOIN agent_wechat_contact awc
      ON awc.deleted = 0
     AND awc.tenant_id = r.tenant_id
     AND awc.wechat_account_id = r.agent_account_id
     AND awc.external_user_id = r.external_user_id
),
upserted AS (
    INSERT INTO agent_conversation (
        id, agent_id, wechat_account_id, contact_id, status, risk_level,
        last_message_time, continuous_auto_reply_count,
        creator, create_time, updater, update_time, deleted, tenant_id
    )
    SELECT nextval('agent_conversation_seq'),
           awa.agent_id,
           tt.agent_account_id,
           tt.agent_contact_id,
           CASE WHEN tt.thread_status = 'closed' THEN 4 ELSE 0 END,
           0,
           tt.last_message_at,
           0,
           'migration',
           COALESCE(tt.created_at, CURRENT_TIMESTAMP),
           'migration',
           COALESCE(tt.updated_at, CURRENT_TIMESTAMP),
           0,
           tt.tenant_id
    FROM thread_targets tt
    JOIN agent_wechat_account awa ON awa.id = tt.agent_account_id
    ON CONFLICT (tenant_id, wechat_account_id, contact_id)
    WHERE deleted = 0
    DO UPDATE SET
        last_message_time = GREATEST(agent_conversation.last_message_time, EXCLUDED.last_message_time),
        status = EXCLUDED.status,
        update_time = CURRENT_TIMESTAMP
    RETURNING id, tenant_id, wechat_account_id, contact_id
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'message_threads', tt.id, 'agent_conversation', ac.id
FROM thread_targets tt
JOIN agent_conversation ac
  ON ac.deleted = 0
 AND ac.tenant_id = tt.tenant_id
 AND ac.wechat_account_id = tt.agent_account_id
 AND ac.contact_id = tt.agent_contact_id
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

WITH source_threads AS (
    SELECT mt.*
    FROM message_threads mt
),
resolved AS (
    SELECT st.*,
           am.target_id AS agent_account_id,
           COALESCE(NULLIF(st.group_room_id, ''), NULLIF(st.source_session_id, '')) AS external_user_id
    FROM source_threads st
    JOIN agent_python_migration_map am
      ON am.source_table = 'gewechat_accounts'
     AND am.source_id = st.gewechat_account_id
     AND am.target_table = 'agent_wechat_account'
),
thread_targets AS (
    SELECT r.*,
           awc.id AS agent_contact_id
    FROM resolved r
    JOIN agent_wechat_contact awc
      ON awc.deleted = 0
     AND awc.tenant_id = r.tenant_id
     AND awc.wechat_account_id = r.agent_account_id
     AND awc.external_user_id = r.external_user_id
)
INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'message_threads', tt.id, 'agent_conversation', ac.id
FROM thread_targets tt
JOIN agent_conversation ac
  ON ac.deleted = 0
 AND ac.tenant_id = tt.tenant_id
 AND ac.wechat_account_id = tt.agent_account_id
 AND ac.contact_id = tt.agent_contact_id
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 6. Migrate messages.
WITH source_messages AS (
    SELECT wm.*
    FROM wechat_messages wm
    WHERE wm.thread_id IS NOT NULL
),
resolved AS (
    SELECT sm.*,
           cm.target_id AS conversation_id,
           ac.wechat_account_id AS agent_account_id,
           ac.contact_id AS agent_contact_id
    FROM source_messages sm
    JOIN agent_python_migration_map cm
      ON cm.source_table = 'message_threads'
     AND cm.source_id = sm.thread_id
     AND cm.target_table = 'agent_conversation'
    JOIN agent_conversation ac ON ac.id = cm.target_id
)
INSERT INTO agent_message (
    id, conversation_id, wechat_account_id, contact_id, direction, sender_type,
    message_type, content, raw_payload, gewe_message_id, send_status,
    operator_user_id, message_time, creator, create_time, updater, update_time,
    deleted, tenant_id
)
SELECT nextval('agent_message_seq'),
       r.conversation_id,
       r.agent_account_id,
       r.agent_contact_id,
       CASE WHEN lower(r.direction) = 'outbound' THEN 2 ELSE 1 END,
       CASE
           WHEN lower(r.direction) = 'outbound' THEN 3
           ELSE 1
       END,
       CASE lower(COALESCE(r.message_type, ''))
           WHEN 'text' THEN 1
           WHEN 'image' THEN 3
           WHEN 'voice' THEN 34
           WHEN 'video' THEN 43
           WHEN 'file' THEN 49
           WHEN 'link' THEN 49
           ELSE 0
       END,
       r.content_text,
       COALESCE(r.raw_payload, '{}'::jsonb),
       COALESCE(NULLIF(r.source_message_key, ''), 'python:' || r.id),
       CASE WHEN lower(r.direction) = 'outbound' THEN 2 ELSE 0 END,
       r.user_id,
       COALESCE(r.sent_at, r.created_at, CURRENT_TIMESTAMP),
       'migration',
       COALESCE(r.created_at, CURRENT_TIMESTAMP),
       'migration',
       COALESCE(r.updated_at, CURRENT_TIMESTAMP),
       0,
       r.tenant_id
FROM resolved r
ON CONFLICT (tenant_id, wechat_account_id, contact_id, gewe_message_id)
WHERE deleted = 0 AND gewe_message_id IS NOT NULL
DO UPDATE SET
    content = COALESCE(EXCLUDED.content, agent_message.content),
    raw_payload = EXCLUDED.raw_payload,
    message_time = COALESCE(EXCLUDED.message_time, agent_message.message_time),
    update_time = CURRENT_TIMESTAMP;

INSERT INTO agent_python_migration_map (source_table, source_id, target_table, target_id)
SELECT 'wechat_messages', wm.id, 'agent_message', am.id
FROM wechat_messages wm
JOIN agent_python_migration_map cm
  ON cm.source_table = 'message_threads'
 AND cm.source_id = wm.thread_id
 AND cm.target_table = 'agent_conversation'
JOIN agent_conversation ac ON ac.id = cm.target_id
JOIN agent_message am
  ON am.deleted = 0
 AND am.tenant_id = wm.tenant_id
 AND am.wechat_account_id = ac.wechat_account_id
 AND am.contact_id = ac.contact_id
 AND am.gewe_message_id = COALESCE(NULLIF(wm.source_message_key, ''), 'python:' || wm.id)
ON CONFLICT (source_table, source_id, target_table)
DO UPDATE SET target_id = EXCLUDED.target_id;

-- 7. Refresh conversation latest message fields after message migration.
WITH latest_message AS (
    SELECT DISTINCT ON (conversation_id)
           conversation_id, id AS message_id, message_time
    FROM agent_message
    WHERE deleted = 0
    ORDER BY conversation_id, message_time DESC, id DESC
)
UPDATE agent_conversation c
SET last_message_id = lm.message_id,
    last_message_time = lm.message_time,
    update_time = CURRENT_TIMESTAMP
FROM latest_message lm
WHERE c.id = lm.conversation_id
  AND c.deleted = 0;

-- 8. Migrate thread-level reply policies into contact-level Java policy overrides.
WITH source_policies AS (
    SELECT rp.*
    FROM reply_policies rp
    WHERE rp.status <> 'deleted'
      AND rp.thread_id IS NOT NULL
),
resolved AS (
    SELECT sp.*,
           cm.target_id AS conversation_id
    FROM source_policies sp
    JOIN agent_python_migration_map cm
      ON cm.source_table = 'message_threads'
     AND cm.source_id = sp.thread_id
     AND cm.target_table = 'agent_conversation'
)
UPDATE agent_wechat_contact awc
SET reply_mode = CASE lower(r.policy_mode)
        WHEN 'auto_reply' THEN 'AUTO_REPLY'
        WHEN 'auto' THEN 'AUTO_REPLY'
        WHEN 'record_only' THEN 'RECORD_ONLY'
        WHEN 'no_reply' THEN 'RECORD_ONLY'
        ELSE 'MANUAL_CONFIRM'
    END,
    quiet_seconds = COALESCE((r.config_json ->> 'quiet_seconds')::integer,
                             (r.config_json ->> 'delay_seconds')::integer,
                             awc.quiet_seconds),
    business_hours = COALESCE(r.config_json -> 'business_hours', awc.business_hours),
    update_time = CURRENT_TIMESTAMP
FROM resolved r
JOIN agent_conversation ac ON ac.id = r.conversation_id
WHERE awc.id = ac.contact_id
  AND awc.deleted = 0;

COMMIT;
