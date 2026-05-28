-- AI 销冠会话记录演示数据
-- 用于本地演示销售洞察：购买意愿、销售阶段、客户情绪、跟进优先级、客户标签、AI/人工回复差异。

BEGIN;

-- 保证演示标签存在，避免只导入本脚本时标签关系为空。
SELECT setval('agent_contact_tag_seq', GREATEST((SELECT last_value FROM agent_contact_tag_seq),
  (SELECT COALESCE(MAX(id), 1) FROM agent_contact_tag)), true);

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

-- 让现有 7 个本地 mock 客户呈现不同销售阶段。
UPDATE agent_wechat_contact
SET nickname = '初次咨询-华东连锁门店',
    remark = '初次咨询-华东连锁门店',
    customer_level = 1,
    last_conversation_status = 0,
    reply_mode = NULL,
    purchase_intention = 'MEDIUM',
    sales_stage = 'NEEDS_CONFIRMED',
    customer_sentiment = 'POSITIVE',
    follow_up_priority = 'NORMAL',
    last_message_time = TIMESTAMP '2026-05-20 09:16:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 1 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '价格敏感-报价对比客户',
    remark = '价格敏感-报价对比客户',
    customer_level = 1,
    last_conversation_status = 2,
    reply_mode = 'MANUAL_CONFIRM',
    purchase_intention = 'HIGH',
    sales_stage = 'QUOTE_NEGOTIATION',
    customer_sentiment = 'NEUTRAL',
    follow_up_priority = 'FOCUS',
    last_message_time = TIMESTAMP '2026-05-20 09:42:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 2 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '交付关注-私域运营团队',
    remark = '交付关注-私域运营团队',
    customer_level = 1,
    last_conversation_status = 2,
    reply_mode = 'MANUAL_CONFIRM',
    purchase_intention = 'HIGH',
    sales_stage = 'PRODUCT_INTRO',
    customer_sentiment = 'POSITIVE',
    follow_up_priority = 'FOCUS',
    last_message_time = TIMESTAMP '2026-05-20 10:08:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 3 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '决策人-成交推进客户',
    remark = '决策人-成交推进客户',
    customer_level = 2,
    last_conversation_status = 2,
    reply_mode = 'MANUAL_CONFIRM',
    purchase_intention = 'STRONG',
    sales_stage = 'DEAL_PROGRESS',
    customer_sentiment = 'POSITIVE',
    follow_up_priority = 'URGENT',
    last_message_time = TIMESTAMP '2026-05-20 10:36:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 4 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '竞品对比-方案替换客户',
    remark = '竞品对比-方案替换客户',
    customer_level = 1,
    last_conversation_status = 2,
    reply_mode = 'MANUAL_CONFIRM',
    purchase_intention = 'MEDIUM',
    sales_stage = 'QUOTE_NEGOTIATION',
    customer_sentiment = 'NEUTRAL',
    follow_up_priority = 'FOCUS',
    last_message_time = TIMESTAMP '2026-05-20 11:05:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 5 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '售后投诉-需要人工接管',
    remark = '售后投诉-需要人工接管',
    customer_level = 2,
    last_conversation_status = 3,
    reply_mode = NULL,
    purchase_intention = 'LOW',
    sales_stage = 'AFTER_SALES',
    customer_sentiment = 'NEGATIVE',
    follow_up_priority = 'URGENT',
    last_message_time = TIMESTAMP '2026-05-20 11:28:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 6 AND deleted = 0;

UPDATE agent_wechat_contact
SET nickname = '自动回复样例-产品能力客户',
    remark = '自动回复样例-产品能力客户',
    customer_level = 1,
    last_conversation_status = 1,
    reply_mode = 'AUTO_REPLY',
    purchase_intention = 'HIGH',
    sales_stage = 'PRODUCT_INTRO',
    customer_sentiment = 'POSITIVE',
    follow_up_priority = 'NORMAL',
    last_message_time = TIMESTAMP '2026-05-20 11:52:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 7 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 0,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 09:16:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 1 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 2,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 09:42:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 2 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 2,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 10:08:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 3 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 2,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 10:36:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 4 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 2,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 11:05:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 5 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 3,
    risk_level = 0,
    continuous_auto_reply_count = 0,
    human_takeover_user_id = 1,
    human_takeover_time = TIMESTAMP '2026-05-20 11:28:00',
    last_message_time = TIMESTAMP '2026-05-20 11:28:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 6 AND deleted = 0;

UPDATE agent_conversation
SET agent_id = 1,
    status = 1,
    risk_level = 0,
    continuous_auto_reply_count = 2,
    human_takeover_user_id = NULL,
    human_takeover_time = NULL,
    last_message_time = TIMESTAMP '2026-05-20 11:52:00',
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 1 AND id = 7 AND deleted = 0;

DELETE FROM agent_contact_tag_rel WHERE tenant_id = 1 AND contact_id BETWEEN 1 AND 7;

INSERT INTO agent_contact_tag_rel (id, contact_id, tag_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_contact_tag_rel_seq'), contact_id, tag_id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
  SELECT 1 contact_id, 2 tag_id UNION ALL
  SELECT 2, 1 UNION ALL SELECT 2, 4 UNION ALL
  SELECT 3, 2 UNION ALL
  SELECT 4, 2 UNION ALL SELECT 4, 3 UNION ALL
  SELECT 5, 1 UNION ALL SELECT 5, 4 UNION ALL
  SELECT 6, 2 UNION ALL
  SELECT 7, 2
) tags;

DELETE FROM agent_message WHERE tenant_id = 1 AND conversation_id BETWEEN 1 AND 7;

INSERT INTO agent_message (
  id, conversation_id, wechat_account_id, contact_id, direction, sender_type, message_type,
  content, raw_payload, gewe_message_id, send_status, intent, matched_policy, audit_note,
  operator_user_id, message_time, creator, create_time, updater, update_time, deleted, tenant_id
) VALUES
  (nextval('agent_message_seq'), 1, 1, 1, 1, 1, 1,
   '你好，我们有 20 多家门店，销售每天都在微信上跟客户聊，想先了解 AI 销冠能不能帮忙统一回复口径？',
   '{}'::jsonb, 'mock-sales-c1-in-1', 0, '产品咨询', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 09:12:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 1, 1, 1, 2, 3, 1,
   '可以的。你们这种多门店场景，建议先把常见商品介绍、报价边界和售后承诺放进知识库，再按门店微信号绑定统一 Agent。我们可以先从 2 个账号试点，验证回复质量和人工接管流程。',
   '{}'::jsonb, 'mock-sales-c1-human-1', 2, '需求澄清', '人工基于 AI 建议发送', '已确认客户是多门店试点诉求', 1,
   TIMESTAMP '2026-05-20 09:16:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 2, 1, 2, 1, 1, 1,
   '你们报价是按微信号还是按坐席？我们现在也在看另一家，价格差异比较大。',
   '{}'::jsonb, 'mock-sales-c2-in-1', 0, '价格咨询', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 09:38:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 2, 1, 2, 2, 2, 1,
   '建议回复：我们通常按托管微信号和使用量组合报价。为了避免只比单价，建议我先帮你拆一下：需要托管几个微信号、每天大概多少客户咨询、是否需要知识库和人工审核。这样能给到更准确的版本和报价。',
   '{}'::jsonb, 'mock-sales-c2-ai-1', 1, '价格敏感', '命中：价格敏感 + 竞品对比，建议人工确认后发送', '客户直接提到竞品价格，等待销售确认措辞', NULL,
   TIMESTAMP '2026-05-20 09:42:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 3, 1, 3, 1, 1, 1,
   '我们比较关心上线周期。商品资料很多，销售团队也不太会整理知识库，交付这块你们怎么做？',
   '{}'::jsonb, 'mock-sales-c3-in-1', 0, '交付咨询', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 10:02:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 3, 1, 3, 2, 2, 1,
   '建议回复：轻量版本可以先从 30-50 条高频商品问答开始，不需要一次性整理完整资料。我们会帮你们把商品卖点、报价规则、交付周期和售后口径拆成知识库条目，先跑一周数据再补充。',
   '{}'::jsonb, 'mock-sales-c3-ai-1', 1, '关注交付', '命中：关注交付，建议人工确认承诺边界', '涉及交付周期和服务承诺，等待人工确认', NULL,
   TIMESTAMP '2026-05-20 10:08:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 4, 1, 4, 1, 1, 1,
   '我是项目负责人，预算这周要定。你们如果能把自动回复、人工审核和敏感规则一起演示清楚，我可以约采购一起看。',
   '{}'::jsonb, 'mock-sales-c4-in-1', 0, '成交推进', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 10:30:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 4, 1, 4, 2, 2, 1,
   '建议回复：可以，建议我们安排 30 分钟演示：前 10 分钟看微信接入和自动回复，后 10 分钟看人工确认和升级规则，最后 10 分钟按你们商品资料现场演示知识库问答。我也可以提前发一份试点清单给你。',
   '{}'::jsonb, 'mock-sales-c4-ai-1', 1, '强购买意愿', '命中：决策人 + 成交推进，建议优先处理', '重点客户，建议销售确认时间后发送', NULL,
   TIMESTAMP '2026-05-20 10:36:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 5, 1, 5, 1, 1, 1,
   '竞品说他们也能接微信，还能自动回复。你们的优势主要在哪里？如果差不多我们就选便宜的。',
   '{}'::jsonb, 'mock-sales-c5-in-1', 0, '竞品对比', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 10:58:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 5, 1, 5, 2, 2, 1,
   '建议回复：如果只看“能不能自动回复”，差异确实不明显。我们的重点是销售可控：每个微信号和客户都能配置不同策略，敏感问题自动升级，知识库可追溯，人工接管后还能保留完整会话复盘。',
   '{}'::jsonb, 'mock-sales-c5-ai-1', 1, '竞品对比', '命中：竞品对比 + 价格敏感，建议人工确认', '客户有替换方案，需要突出差异化价值', NULL,
   TIMESTAMP '2026-05-20 11:05:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 6, 1, 6, 1, 1, 1,
   '昨天演示说能引用知识库，今天测试好几次都答不上来。这个如果解决不了，我们就不继续推进了。',
   '{}'::jsonb, 'mock-sales-c6-in-1', 0, '售后投诉', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 11:20:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 6, 1, 6, 2, 4, 1,
   '系统已升级为人工接管：客户表达明显不满，且涉及知识库命中失败。',
   '{}'::jsonb, 'mock-sales-c6-system-1', 2, '人工升级', '命中：负向情绪 + RAG 未命中', '需要人工处理，暂停自动回复', NULL,
   TIMESTAMP '2026-05-20 11:21:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 6, 1, 6, 2, 3, 1,
   '抱歉，这个问题我来跟进。你刚才测试的问题应该是知识库条目没有绑定到当前微信号，我先帮你核对配置，10 分钟内给你一个明确处理结果。',
   '{}'::jsonb, 'mock-sales-c6-human-1', 2, '人工安抚', '人工接管后回复', '已由人工安抚并承诺反馈时间', 1,
   TIMESTAMP '2026-05-20 11:28:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),

  (nextval('agent_message_seq'), 7, 1, 7, 1, 1, 1,
   'AI 回复时可以引用商品知识库吗？比如客户问型号差异、交付周期、售后范围这些。',
   '{}'::jsonb, 'mock-sales-c7-in-1', 0, '知识库咨询', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 11:47:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 7, 1, 7, 2, 2, 1,
   '可以。你可以给每个 Agent 绑定知识库，里面保存商品卖点、型号差异、报价说明、交付周期和售后范围。客户提问时，系统会先检索相关知识，再生成更像专业销售的回复。',
   '{}'::jsonb, 'mock-sales-c7-ai-1', 2, '知识库咨询', '好友策略：AUTO_REPLY，未命中人工升级规则', 'Mock 自动发送样例：页面展示用，未实际调用 GeWe', NULL,
   TIMESTAMP '2026-05-20 11:50:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 7, 1, 7, 1, 1, 1,
   '那如果知识库找不到答案，会不会乱答？',
   '{}'::jsonb, 'mock-sales-c7-in-2', 0, '风险咨询', NULL, NULL, NULL,
   TIMESTAMP '2026-05-20 11:51:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
  (nextval('agent_message_seq'), 7, 1, 7, 2, 2, 1,
   '不会直接强答。可以配置“无法引用知识库”时升级人工确认，也可以让 AI 只给回复建议。这样专业问题既能借助 RAG，又能保证关键承诺由人工把关。',
   '{}'::jsonb, 'mock-sales-c7-ai-2', 2, '风险咨询', '好友策略：AUTO_REPLY，知识库低置信转安全回复', 'Mock 自动发送样例：页面展示用，未实际调用 GeWe', NULL,
   TIMESTAMP '2026-05-20 11:52:00', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1);

UPDATE agent_conversation c
SET last_message_id = latest.id,
    last_message_time = latest.message_time,
    update_time = CURRENT_TIMESTAMP
FROM (
  SELECT DISTINCT ON (conversation_id) conversation_id, id, message_time
  FROM agent_message
  WHERE tenant_id = 1 AND deleted = 0 AND conversation_id BETWEEN 1 AND 7
  ORDER BY conversation_id, message_time DESC, id DESC
) latest
WHERE c.tenant_id = 1 AND c.id = latest.conversation_id AND c.deleted = 0;

UPDATE agent_wechat_contact wc
SET last_message_time = c.last_message_time,
    last_conversation_status = c.status,
    update_time = CURRENT_TIMESTAMP
FROM agent_conversation c
WHERE wc.tenant_id = 1
  AND c.tenant_id = 1
  AND wc.id = c.contact_id
  AND wc.deleted = 0
  AND c.deleted = 0
  AND c.id BETWEEN 1 AND 7;

SELECT setval('agent_contact_tag_rel_seq', GREATEST((SELECT last_value FROM agent_contact_tag_rel_seq),
  (SELECT COALESCE(MAX(id), 1) FROM agent_contact_tag_rel)), true);
SELECT setval('agent_message_seq', GREATEST((SELECT last_value FROM agent_message_seq),
  (SELECT COALESCE(MAX(id), 1) FROM agent_message)), true);

COMMIT;
