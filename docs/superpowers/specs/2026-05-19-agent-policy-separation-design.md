# Agent 与回复策略拆分设计

## 背景

V1.4 目前把 Agent 人设字段和回复策略字段都放在 `agent_agent` 表里。这样会让 Agent 同时承担“LLM 销售人格”和“运营回复策略”两个职责，后续会越来越难维护。

现在产品方向已经明确：

- Agent 配置回答：“AI 应该如何思考、如何表达？”
- 微信账号策略回答：“这个托管微信号默认应该如何回复？”
- 微信好友策略回答：“这个具体好友是否需要更高优先级的个性化覆盖？”

本设计会拆开这些职责，同时保留已经跑通的 Gewe 回调、消息保存、知识库匹配、人工审核、风控和统计流程。

## 产品规则

1. Agent 配置只配置回复 Agent 本身。
   - Agent 负责专业销售人设、系统提示词、LLM 供应商和 LLM 模型。
   - Agent 可以保留欢迎语、转人工话术这类可复用话术字段。
   - Agent 不再配置回复模式、置信阈值、回复间隔、营业时间或连续自动回复上限。

2. 托管微信账号配置默认回复策略。
   - 每个托管微信账号绑定一个 Agent。
   - 绑定的 Agent 决定这个微信账号收到消息时使用哪个人设和模型处理。
   - 微信账号策略决定默认回复行为：人工确认、自动回复、仅人工、仅记录、置信阈值、营业时间、静默间隔、连续自动回复上限。

3. 微信好友配置可以覆盖微信账号策略。
   - 好友策略优先级高于微信账号策略。
   - 覆盖是字段级覆盖：字段为空表示继承微信账号策略。
   - 这样可以支持一个重要客户走人工确认，而同一个微信号下其他好友仍然自动回复。

4. 默认 LLM 模型是 `deepseek-v4-pro`。
   - 初始供应商值为 `DEEPSEEK`。
   - 初始模型值为 `deepseek-v4-pro`。
   - 后端提供可选模型清单：`deepseek-v4-pro` 和 `deepseek-v4-flash`。
   - 未来增加其他供应商时，不需要改变策略解析逻辑。

## 数据模型

### `agent_agent`

`agent_agent` 作为 Agent 人设和模型配置表。

保留字段：
- `id`
- `name`
- `alias_name`
- `scene`
- `target_customer_desc`
- `tone`
- `welcome_message`
- `handover_message`
- `status`
- `draft_version`
- `online_version`
- `published_config`

新增字段：
- `system_prompt text`
- `llm_provider varchar(32) not null default 'DEEPSEEK'`
- `llm_model varchar(128) not null default 'deepseek-v4-pro'`

从 Agent UI 移除，并迁移到微信账号/好友策略中的字段：
- `owner_user_id`
- `reply_mode`
- `confidence_threshold`
- `max_continuous_auto_reply`
- `quiet_minutes`
- `business_hours`
- `follow_up_policy`
- `material_priority`

迁移期间这些旧字段可以先保留在数据库里，保证兼容已有数据。但引入策略表字段后，新代码不应继续从 `agent_agent` 读取回复策略。

### `agent_wechat_account`

`agent_wechat_account` 作为微信账号级默认策略的配置表。

保留现有字段：
- `agent_id`
- `owner_user_id`
- Gewe 连接字段
- 微信账号身份字段
- `status`

新增账号级策略字段：
- `reply_mode varchar(32) not null default 'MANUAL_CONFIRM'`
- `confidence_threshold numeric(5,2) not null default 0.70`
- `max_continuous_auto_reply int4 not null default 3`
- `quiet_minutes int4 not null default 0`
- `business_hours jsonb null`

这里的 `owner_user_id` 表示托管微信账号负责人或运营人。

### `agent_wechat_contact`

`agent_wechat_contact` 作为好友级策略覆盖配置表。

保留现有字段：
- 客户身份字段
- `customer_level`
- `owner_user_id`
- 风险和会话状态字段

新增好友级覆盖字段：
- `reply_mode varchar(32) null`
- `confidence_threshold numeric(5,2) null`
- `max_continuous_auto_reply int4 null`
- `quiet_minutes int4 null`
- `business_hours jsonb null`

这些字段为空时，表示继承微信账号级策略。

## 策略解析

新增有效策略对象：

```java
public record AgentReplyPolicy(
        String replyMode,
        BigDecimal confidenceThreshold,
        Integer maxContinuousAutoReply,
        Integer quietMinutes,
        Map<String, Object> businessHours,
        String source
) {
}
```

策略解析顺序：

1. 从系统默认值开始。
2. 应用微信账号策略字段。
3. 只在好友字段非空时，应用好友策略字段覆盖。

`source` 的取值：
- `ACCOUNT`：没有任何好友字段覆盖，最终策略来自微信账号。
- `CONTACT`：至少有一个好友字段覆盖了微信账号策略。

现有 `AgentAutoReplyPolicyEvaluator` 应改为评估 `AgentReplyPolicy`，不再直接评估 `AgentDO`。

## 回复生成流程

消息处理流程调整为：

1. Gewe 回调收到消息。
2. 系统保存入站消息，并解析微信账号、微信好友和会话。
3. 通过微信账号的 `agent_id` 加载 Agent 人设配置。
4. 构建上下文：
   - 入站消息内容
   - 最近消息
   - 启用状态的知识库条目
   - 客户等级
   - 客户标签
   - Agent `systemPrompt`
   - Agent `llmProvider`
   - Agent `llmModel`
5. 回复生成器生成回复内容或回复建议。
6. 策略解析器根据微信账号和微信好友计算最终有效策略。
7. 策略评估器判断自动发送、人工确认、仅记录或人工接管。
8. 决策快照保存：
   - `agentId`
   - `llmProvider`
   - `llmModel`
   - `effectivePolicy`
   - `policySource`
   - 客户等级
   - 客户标签
   - 知识库引用
   - 风控命中信息

## 前端变化

### Agent 配置

Agent 表单展示：
- Agent 名称
- 别名
- 销售场景
- 目标客户
- 回复语气
- 系统提示词
- 欢迎语
- 转人工话术
- LLM 供应商
- LLM 模型
- 状态
- 发布/版本历史

Agent 表单移除或隐藏：
- 负责人
- 回复模式
- 置信阈值
- 连续上限
- 静默分钟
- 跟进策略
- 素材优先级
- 营业时间

### 微信账号

微信账号表单展示：
- 绑定 Agent
- 微信账号负责人
- 默认回复模式
- 默认置信阈值
- 默认连续自动回复上限
- 默认静默分钟
- 默认营业时间
- Gewe 连接字段
- 状态

### 客户好友

客户好友页面新增策略配置弹窗：
- 回复模式覆盖
- 置信阈值覆盖
- 连续自动回复上限覆盖
- 静默分钟覆盖
- 营业时间覆盖

每个覆盖控件都需要支持“继承微信号策略”状态。

## 数据迁移

创建增量 SQL 迁移：

1. 给 `agent_agent` 增加 Agent 人设和模型字段。
2. 给 `agent_wechat_account` 增加账号级策略字段。
3. 给 `agent_wechat_contact` 增加好友级策略覆盖字段。
4. 把现有 Agent 策略值复制到已绑定该 Agent 的微信账号上：
   - 对每个有 `agent_id` 的微信账号，从对应 Agent 复制策略字段。
   - 如果微信账号没有绑定 Agent，或 Agent 策略为空，则使用默认值。
5. 旧 Agent 策略字段保留在数据库中，但从新 UI 和运行时策略解析中移除。

## 兼容性

迁移期间：

- 已绑定 Agent 的微信账号继续可用。
- 现有 Agent 策略会迁移为微信账号默认策略。
- 现有好友策略字段初始都为空，默认继承微信账号策略。
- 现有版本历史仍然可读。
- 新的 Agent 发布快照只包含人设和模型字段，不再包含账号/好友策略。

## 测试

后端测试：

1. Agent 更新/发布快照包含 `systemPrompt`、`llmProvider`、`llmModel`。
2. 微信账号默认策略可以保存和返回。
3. 好友策略覆盖字段可以保存空值和非空值。
4. 策略解析器使用好友字段覆盖微信账号字段。
5. 好友字段为空时，策略解析器继承微信账号字段。
6. 自动回复策略评估器读取 `AgentReplyPolicy`，不再读取 `AgentDO`。
7. 决策上下文快照包含 Agent 模型元数据和最终有效策略。

前端检查：

1. Agent 表单不再暴露策略字段。
2. Agent 表单暴露系统提示词和 LLM 字段，并从后端读取 DeepSeek V4 Pro/Flash 模型选项。
3. 微信账号表单暴露默认策略字段。
4. 客户好友页面暴露覆盖控件和继承状态。
5. Agent、微信账号、客户好友页面通过过滤后的 `vue-tsc` 检查。

## 实施顺序

集中一个迭代完成：

1. 为策略解析和 Agent 人设发布补测试。
2. 增加数据库、DO、VO 字段。
3. 把策略评估器输入改为最终有效策略。
4. 更新回调自动回复链路。
5. 更新 Agent、微信账号、客户好友三个前端页面。
6. 本地应用 SQL，运行后端测试，构建前端，重启后端并做冒烟验证。
