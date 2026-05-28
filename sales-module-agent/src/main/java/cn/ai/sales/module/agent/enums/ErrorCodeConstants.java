package cn.ai.sales.module.agent.enums;

import cn.ai.sales.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode AGENT_NOT_EXISTS = new ErrorCode(1_010_000_000, "Agent 不存在");
    ErrorCode AGENT_NAME_DUPLICATE = new ErrorCode(1_010_000_001, "Agent 名称已存在");
    ErrorCode AGENT_LLM_MODEL_INVALID = new ErrorCode(1_010_000_002, "Agent 大模型配置不支持");
    ErrorCode WECHAT_ACCOUNT_NOT_EXISTS = new ErrorCode(1_010_001_000, "微信账号不存在");
    ErrorCode WECHAT_ACCOUNT_CALLBACK_TOKEN_DUPLICATE = new ErrorCode(1_010_001_001, "微信账号回调令牌已存在");
    ErrorCode WECHAT_ACCOUNT_GEWE_APP_DUPLICATE = new ErrorCode(1_010_001_002, "Gewe Appid 已存在");
    ErrorCode GEWE_CREDENTIAL_NOT_EXISTS = new ErrorCode(1_010_001_003, "GeWe 凭证不存在");
    ErrorCode GEWE_CREDENTIAL_TOKEN_MISSING = new ErrorCode(1_010_001_004, "请先配置 GeWe Token");
    ErrorCode WECHAT_BIND_SESSION_NOT_EXISTS = new ErrorCode(1_010_001_005, "微信绑定会话不存在");
    ErrorCode WECHAT_BIND_SESSION_EXPIRED = new ErrorCode(1_010_001_006, "微信绑定二维码已过期");
    ErrorCode WECHAT_BIND_SESSION_FAILED = new ErrorCode(1_010_001_007, "微信绑定失败");
    ErrorCode WECHAT_CONTACT_NOT_EXISTS = new ErrorCode(1_010_002_000, "微信好友不存在");
    ErrorCode CONTACT_TAG_NOT_EXISTS = new ErrorCode(1_010_002_100, "客户标签不存在");
    ErrorCode CONTACT_TAG_NAME_DUPLICATE = new ErrorCode(1_010_002_101, "客户标签名称已存在");
    ErrorCode CONVERSATION_NOT_EXISTS = new ErrorCode(1_010_003_000, "会话不存在");
    ErrorCode MESSAGE_NOT_EXISTS = new ErrorCode(1_010_003_001, "消息不存在");
    ErrorCode MESSAGE_STATUS_INVALID = new ErrorCode(1_010_003_002, "消息状态不允许执行该操作");
    ErrorCode GEWE_SEND_CONFIG_MISSING = new ErrorCode(1_010_003_003, "微信账号未配置 Gewe 发送地址或 Token");
    ErrorCode GEWE_SEND_FAILED = new ErrorCode(1_010_003_004, "Gewe 消息发送失败");
    ErrorCode REPLY_DECISION_NOT_EXISTS = new ErrorCode(1_010_003_005, "回复决策不存在");
    ErrorCode REPLY_DECISION_STATUS_INVALID = new ErrorCode(1_010_003_006, "回复决策状态不允许执行该操作");
    ErrorCode CONVERSATION_STATUS_INVALID = new ErrorCode(1_010_003_007, "当前会话状态不允许恢复原策略");
    ErrorCode WEBHOOK_ACCOUNT_NOT_FOUND = new ErrorCode(1_010_004_000, "回调对应的微信账号不存在");
    ErrorCode WEBHOOK_SIGNATURE_INVALID = new ErrorCode(1_010_004_001, "Gewe 回调签名不正确");
    ErrorCode WEBHOOK_PAYLOAD_INVALID = new ErrorCode(1_010_004_002, "Gewe 回调内容格式不正确");
    ErrorCode SENSITIVE_RULE_NOT_EXISTS = new ErrorCode(1_010_005_000, "人工升级规则不存在");
    ErrorCode SENSITIVE_RULE_NAME_DUPLICATE = new ErrorCode(1_010_005_001, "人工升级规则名称已存在");
    ErrorCode SENSITIVE_RULE_PATTERN_INVALID = new ErrorCode(1_010_005_002, "人工升级规则触发条件不合法");
    ErrorCode KNOWLEDGE_BASE_NOT_EXISTS = new ErrorCode(1_010_006_010, "知识库不存在");
    ErrorCode KNOWLEDGE_BASE_NAME_DUPLICATE = new ErrorCode(1_010_006_011, "知识库名称已存在");
    ErrorCode KNOWLEDGE_ITEM_NOT_EXISTS = new ErrorCode(1_010_006_000, "知识库条目不存在");
    ErrorCode KNOWLEDGE_ITEM_TITLE_DUPLICATE = new ErrorCode(1_010_006_001, "知识库标题已存在");

}
