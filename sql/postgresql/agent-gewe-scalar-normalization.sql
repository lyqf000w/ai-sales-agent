-- AI sales agent: normalize GeWe scalar wrapper values in business fields.
-- Run after backing up agent_wechat_contact, agent_conversation, agent_message,
-- agent_reply_decision and agent_contact_tag_rel.

BEGIN;

CREATE OR REPLACE FUNCTION pg_temp.agent_unwrap_gewe_scalar(input_text text)
RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
    result text := btrim(input_text);
    json_payload jsonb;
    scalar_key text;
BEGIN
    IF result IS NULL OR result = '' THEN
        RETURN result;
    END IF;

    <<unwrap_loop>>
    FOR i IN 1..3 LOOP
        FOREACH scalar_key IN ARRAY ARRAY[
            'string', 'str', 'text', 'value', 'long', 'int',
            'integer', 'number', 'bool', 'boolean', 'float', 'double'
        ] LOOP
            IF starts_with(result, '{' || scalar_key || '=') AND right(result, 1) = '}' THEN
                result := btrim(substring(result FROM char_length(scalar_key) + 3
                    FOR char_length(result) - char_length(scalar_key) - 3));
                CONTINUE unwrap_loop;
            END IF;
        END LOOP;

        IF result ~ '^\s*\{".*"\s*:' THEN
            BEGIN
                json_payload := result::jsonb;
                FOREACH scalar_key IN ARRAY ARRAY[
                    'string', 'str', 'text', 'value', 'long', 'int',
                    'integer', 'number', 'bool', 'boolean', 'float', 'double'
                ] LOOP
                    IF json_payload ? scalar_key THEN
                        result := btrim(json_payload ->> scalar_key);
                        CONTINUE unwrap_loop;
                    END IF;
                END LOOP;
            EXCEPTION WHEN others THEN
                NULL;
            END;
        END IF;

        EXIT;
    END LOOP;

    RETURN result;
END;
$$;

CREATE TEMP TABLE tmp_agent_contact_scalar_merge AS
SELECT
    wrapped.id AS wrapped_contact_id,
    clean.id AS clean_contact_id,
    wrapped_conversation.id AS wrapped_conversation_id,
    clean_conversation.id AS clean_conversation_id
FROM agent_wechat_contact wrapped
JOIN agent_wechat_contact clean
    ON clean.deleted = 0
   AND clean.tenant_id = wrapped.tenant_id
   AND clean.wechat_account_id = wrapped.wechat_account_id
   AND clean.external_user_id = pg_temp.agent_unwrap_gewe_scalar(wrapped.external_user_id)
LEFT JOIN agent_conversation wrapped_conversation
    ON wrapped_conversation.deleted = 0
   AND wrapped_conversation.contact_id = wrapped.id
LEFT JOIN agent_conversation clean_conversation
    ON clean_conversation.deleted = 0
   AND clean_conversation.contact_id = clean.id
WHERE wrapped.deleted = 0
  AND pg_temp.agent_unwrap_gewe_scalar(wrapped.external_user_id) <> wrapped.external_user_id;

UPDATE agent_message message
SET contact_id = merge.clean_contact_id,
    conversation_id = COALESCE(merge.clean_conversation_id, message.conversation_id),
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE message.deleted = 0
  AND (message.contact_id = merge.wrapped_contact_id
       OR message.conversation_id = merge.wrapped_conversation_id);

UPDATE agent_reply_decision decision
SET conversation_id = merge.clean_conversation_id,
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE decision.deleted = 0
  AND decision.conversation_id = merge.wrapped_conversation_id
  AND merge.clean_conversation_id IS NOT NULL;

UPDATE agent_contact_tag_rel tag_rel
SET contact_id = merge.clean_contact_id,
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE tag_rel.deleted = 0
  AND tag_rel.contact_id = merge.wrapped_contact_id
  AND NOT EXISTS (
      SELECT 1
      FROM agent_contact_tag_rel existed
      WHERE existed.deleted = 0
        AND existed.tenant_id = tag_rel.tenant_id
        AND existed.contact_id = merge.clean_contact_id
        AND existed.tag_id = tag_rel.tag_id
  );

UPDATE agent_contact_tag_rel tag_rel
SET deleted = 1,
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE tag_rel.deleted = 0
  AND tag_rel.contact_id = merge.wrapped_contact_id;

UPDATE agent_wechat_contact clean
SET last_message_time = greatest(coalesce(clean.last_message_time, timestamp '1970-01-01'),
                                 coalesce(wrapped.last_message_time, timestamp '1970-01-01')),
    last_conversation_status = coalesce(clean.last_conversation_status, wrapped.last_conversation_status),
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
JOIN agent_wechat_contact wrapped ON wrapped.id = merge.wrapped_contact_id
WHERE clean.id = merge.clean_contact_id;

UPDATE agent_conversation conversation
SET last_message_id = latest.message_id,
    last_message_time = latest.message_time,
    updater = 'system',
    update_time = now()
FROM (
    SELECT DISTINCT ON (message.conversation_id)
        message.conversation_id,
        message.id AS message_id,
        message.message_time
    FROM agent_message message
    JOIN tmp_agent_contact_scalar_merge merge
        ON merge.clean_conversation_id = message.conversation_id
    WHERE message.deleted = 0
    ORDER BY message.conversation_id, message.message_time DESC, message.id DESC
) latest
WHERE conversation.id = latest.conversation_id;

UPDATE agent_conversation conversation
SET deleted = 1,
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE conversation.id = merge.wrapped_conversation_id;

UPDATE agent_wechat_contact contact
SET deleted = 1,
    updater = 'system',
    update_time = now()
FROM tmp_agent_contact_scalar_merge merge
WHERE contact.id = merge.wrapped_contact_id;

UPDATE agent_wechat_contact contact
SET external_user_id = pg_temp.agent_unwrap_gewe_scalar(external_user_id),
    wechat_id = pg_temp.agent_unwrap_gewe_scalar(wechat_id),
    nickname = pg_temp.agent_unwrap_gewe_scalar(nickname),
    remark = pg_temp.agent_unwrap_gewe_scalar(remark),
    updater = 'system',
    update_time = now()
WHERE contact.deleted = 0
  AND (pg_temp.agent_unwrap_gewe_scalar(external_user_id) <> external_user_id
       OR pg_temp.agent_unwrap_gewe_scalar(wechat_id) <> wechat_id
       OR pg_temp.agent_unwrap_gewe_scalar(nickname) <> nickname
       OR pg_temp.agent_unwrap_gewe_scalar(remark) <> remark);

UPDATE agent_message message
SET content = pg_temp.agent_unwrap_gewe_scalar(content),
    updater = 'system',
    update_time = now()
WHERE message.deleted = 0
  AND message.content IS NOT NULL
  AND pg_temp.agent_unwrap_gewe_scalar(content) <> content;

COMMIT;
