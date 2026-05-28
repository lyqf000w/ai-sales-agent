-- AI 销冠：会话记录升级为客户工作台，客户好友菜单暂时隐藏，升级规则收敛到 Agent 意图识别。

UPDATE system_menu
SET name = '客户工作台'
WHERE id = 6003 OR component = 'agent/conversation/index';

UPDATE system_menu
SET visible = false
WHERE id = 6002 OR component = 'agent/contact/index';

COMMENT ON COLUMN agent_sensitive_rule.trigger_type IS '触发类型：INTENT、SENTIMENT、REQUEST_HUMAN；升级规则统一由 Agent 意图识别触发';
COMMENT ON COLUMN agent_sensitive_rule.action IS '动作：1 进入人工确认，3 转人工接管；恢复原策略只清除临时接管态，不改写好友或微信号回复策略，0/2 为历史兼容';

UPDATE agent_sensitive_rule
SET trigger_type = 'SENTIMENT',
    match_type = 3,
    pattern = 'angry',
    action = 3,
    risk_level = 2
WHERE name = '客户愤怒、投诉、威胁差评' AND deleted = 0;

UPDATE agent_sensitive_rule
SET name = '退款投诉意图',
    trigger_type = 'INTENT',
    match_type = 3,
    pattern = 'refund_or_complaint',
    action = 3,
    risk_level = 2
WHERE name = '退款、合同、价格特殊承诺' AND deleted = 0;

INSERT INTO agent_sensitive_rule (id, name, agent_id, route_app, match_type, trigger_type, pattern, action, risk_level,
                                  sort, status, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('agent_sensitive_rule_seq'), '合同价格特殊承诺', NULL, 'GEWE', 3, 'INTENT',
       'special_commitment', 3, 2, 25, 0, '由 Agent 意图识别触发', 'admin', CURRENT_TIMESTAMP,
       'admin', CURRENT_TIMESTAMP, 0, 1
WHERE NOT EXISTS (
  SELECT 1 FROM agent_sensitive_rule WHERE tenant_id = 1 AND name = '合同价格特殊承诺' AND deleted = 0
);

UPDATE agent_sensitive_rule
SET name = '知识库不可回答意图',
    trigger_type = 'INTENT',
    match_type = 3,
    pattern = 'knowledge_gap',
    action = 1,
    risk_level = 1
WHERE name = '模型低置信度或无法引用知识库' AND deleted = 0;

UPDATE agent_sensitive_rule
SET name = '重点客户项目推进意图',
    trigger_type = 'INTENT',
    match_type = 3,
    pattern = 'key_customer_project',
    action = 1,
    risk_level = 1
WHERE name = '重点客户或大客户项目推进' AND deleted = 0;
