-- GeWe provider callback URL backfill.
-- GeWe callback identity is credential/provider token first, then wxid.
-- Account callback_token remains account-local for compatibility; callback_url should point to provider callback URL.

UPDATE agent_wechat_account a
SET callback_url = c.callback_url,
    update_time = CURRENT_TIMESTAMP
FROM agent_gewe_credential c
WHERE a.gewe_credential_id = c.id
  AND a.deleted = 0
  AND c.deleted = 0
  AND c.callback_url IS NOT NULL
  AND c.callback_url <> ''
  AND COALESCE(a.callback_url, '') <> c.callback_url;
