-- AI 销冠销售洞察字段与默认销售标签。

ALTER TABLE agent_wechat_contact
  ADD COLUMN IF NOT EXISTS purchase_intention varchar(32) NOT NULL DEFAULT 'MEDIUM',
  ADD COLUMN IF NOT EXISTS sales_stage varchar(32) NOT NULL DEFAULT 'NEW_LEAD',
  ADD COLUMN IF NOT EXISTS customer_sentiment varchar(32) NOT NULL DEFAULT 'NEUTRAL',
  ADD COLUMN IF NOT EXISTS follow_up_priority varchar(32) NOT NULL DEFAULT 'NORMAL';

UPDATE agent_wechat_contact
SET purchase_intention = COALESCE(NULLIF(purchase_intention, ''), 'MEDIUM'),
    sales_stage = COALESCE(NULLIF(sales_stage, ''), 'NEW_LEAD'),
    customer_sentiment = COALESCE(NULLIF(customer_sentiment, ''), 'NEUTRAL'),
    follow_up_priority = COALESCE(NULLIF(follow_up_priority, ''), 'NORMAL');

COMMENT ON COLUMN agent_wechat_contact.risk_level IS '历史兼容字段：风险等级，主界面使用销售洞察和回复控制';
COMMENT ON COLUMN agent_wechat_contact.purchase_intention IS '销售洞察：购买意愿 LOW、MEDIUM、HIGH、STRONG';
COMMENT ON COLUMN agent_wechat_contact.sales_stage IS '销售洞察：销售阶段 NEW_LEAD、NEEDS_CONFIRMED、PRODUCT_INTRO、QUOTE_NEGOTIATION、DEAL_PROGRESS、AFTER_SALES';
COMMENT ON COLUMN agent_wechat_contact.customer_sentiment IS '销售洞察：客户情绪 POSITIVE、NEUTRAL、NEGATIVE';
COMMENT ON COLUMN agent_wechat_contact.follow_up_priority IS '销售洞察：跟进优先级 NORMAL、FOCUS、URGENT';

SELECT setval('agent_contact_tag_seq', GREATEST((SELECT last_value FROM agent_contact_tag_seq),
  (SELECT COALESCE(MAX(id), 1) FROM agent_contact_tag)), false);

INSERT INTO agent_contact_tag (id, name, color, description, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_contact_tag_seq'), '价格敏感', '#E6A23C', '客户明显关注价格、折扣、报价对比', 10, 0,
       'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_contact_tag WHERE tenant_id = 1 AND name = '价格敏感' AND deleted = 0
);

INSERT INTO agent_contact_tag (id, name, color, description, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_contact_tag_seq'), '关注交付', '#409EFF', '客户关注部署周期、交付质量、售后响应', 20, 0,
       'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_contact_tag WHERE tenant_id = 1 AND name = '关注交付' AND deleted = 0
);

INSERT INTO agent_contact_tag (id, name, color, description, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_contact_tag_seq'), '决策人', '#67C23A', '客户具备采购或项目推进决策权', 30, 0,
       'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_contact_tag WHERE tenant_id = 1 AND name = '决策人' AND deleted = 0
);

INSERT INTO agent_contact_tag (id, name, color, description, sort, status, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_contact_tag_seq'), '竞品对比', '#909399', '客户正在比较竞品、替代方案或供应商', 40, 0,
       'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_contact_tag WHERE tenant_id = 1 AND name = '竞品对比' AND deleted = 0
);

SELECT setval('agent_contact_tag_seq', GREATEST((SELECT last_value FROM agent_contact_tag_seq),
  (SELECT COALESCE(MAX(id), 1) FROM agent_contact_tag)), false);
