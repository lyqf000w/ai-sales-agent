-- Python backend bridge for Personal Sales Assistant console.
-- The Java console keeps bind sessions only as UI cache. GeWe credentials live
-- in the Python identity backend, so credential_id must be optional here.

ALTER TABLE agent_wechat_bind_session
  ALTER COLUMN credential_id DROP NOT NULL;

COMMENT ON COLUMN agent_wechat_bind_session.credential_id
  IS 'GeWe credential id; nullable when QR login is delegated to Python backend';
