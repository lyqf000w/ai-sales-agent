-- Normalize callback fields for WeChat accounts created before credential-level GeWe callbacks.
-- Run after backing up agent_wechat_account and agent_gewe_credential.

WITH account_tokens AS (
    SELECT
        a.id,
        a.gewe_credential_id,
        NULLIF(a.callback_url, '') AS current_callback_url,
        COALESCE(NULLIF(a.callback_token, ''), md5('account:' || a.id || ':' || clock_timestamp())) AS account_callback_token
    FROM agent_wechat_account a
),
prepared AS (
    SELECT
        a.id,
        a.account_callback_token,
        CASE
            WHEN c.id IS NOT NULL THEN
                COALESCE(NULLIF(c.callback_url, ''), '/api/v1/gewechat/callback?token=' || c.callback_token)
            ELSE
                COALESCE(a.current_callback_url, '/api/v1/gewechat/callback?token=' || a.account_callback_token)
        END AS callback_url
    FROM account_tokens a
    LEFT JOIN agent_gewe_credential c ON c.id = a.gewe_credential_id
)
UPDATE agent_wechat_account a
SET callback_token = p.account_callback_token,
    callback_url = p.callback_url
FROM prepared p
WHERE a.id = p.id
  AND (
      a.callback_token IS NULL
      OR a.callback_token = ''
      OR a.callback_url IS NULL
      OR a.callback_url = ''
      OR (
          a.gewe_credential_id IS NOT NULL
          AND a.callback_url IS DISTINCT FROM p.callback_url
      )
  );
