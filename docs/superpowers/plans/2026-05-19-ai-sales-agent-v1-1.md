# AI Sales Agent V1.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the V1.1 access loop for ai-sales-agent: create the independent `sales-module-agent`, receive Gewe webhook events, persist WeChat accounts, contacts, conversations, messages, and expose basic admin pages.

**Architecture:** Add a new Maven module that follows the existing `controller -> service -> dal` pattern. Gewe callbacks enter through a public tenant-ignored controller, resolve `tenant_id` from the bound WeChat account, then write all business records inside `TenantUtils.execute(...)`. The frontend uses backend-driven menus and Vue views under `src/views/agent`.

**Tech Stack:** Spring Boot 3.5, MyBatis Plus, PostgreSQL, Redis, Vue 3, Element Plus, Vite, Gewe webhook v1.0 payload shape.

---

## References

- Product design: `docs/agent/ai-sales-agent-module-design.md`
- Roadmap: `docs/agent/ai-sales-agent-roadmap.md`
- Gewe webhook structure: https://doc.geweapi.com/doc-3599831
- Gewe webhook subscription notes: https://doc.geweapi.com/doc-3146208
- Gewe text send endpoint for the later V1.2/V1.3 send loop: https://doc.ylianx.com/api-295158413

## Scope

V1.1 implements only the access loop:

- Create `sales-module-agent`.
- Add PostgreSQL tables and menu records for V1.1.
- Add basic Agent seed support because WeChat accounts can optionally bind an Agent.
- Add WeChat account CRUD.
- Add Gewe callback receiver.
- Save raw webhook events.
- Deduplicate by `Appid + Data.NewMsgId`, matching Gewe's documented guidance.
- Save or update WeChat contacts.
- Create or update one conversation per WeChat account and contact.
- Save inbound customer messages.
- Add admin pages for WeChat accounts, contacts, and conversations.

V1.1 does not generate LLM replies, send messages, configure customer tags, configure guardrails, or manage knowledge sources. Those are V1.2 through V1.4 in the roadmap.

## File Structure

Create:

- `sales-module-agent/pom.xml`: module dependencies.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/ErrorCodeConstants.java`: module error codes.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/AgentConstants.java`: stable integer constants used by DOs and VOs.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/*.java`: V1.1 persistent entities.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/*.java`: MyBatis Plus mappers.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/**/*.java`: admin and callback controllers.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/**/vo/*.java`: request and response VOs.
- `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/**/*.java`: business services.
- `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/webhook/GeweCallbackParserTest.java`: parser unit tests.
- `sql/postgresql/agent.sql`: V1.1 schema and menu seed.
- `sales-ui/sales-ui-admin/src/api/agent/wechatAccount/index.ts`: account API.
- `sales-ui/sales-ui-admin/src/api/agent/contact/index.ts`: contact API.
- `sales-ui/sales-ui-admin/src/api/agent/conversation/index.ts`: conversation API.
- `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/index.vue`: account list page.
- `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/WechatAccountForm.vue`: account form.
- `sales-ui/sales-ui-admin/src/views/agent/contact/index.vue`: contact list page.
- `sales-ui/sales-ui-admin/src/views/agent/conversation/index.vue`: conversation list and message drawer.

Modify:

- `pom.xml`: add `sales-module-agent` module.
- `sales-server/pom.xml`: add dependency on `sales-module-agent`.
- `script/docker/docker-compose.local.yml`: mount `sql/postgresql/agent.sql` for fresh local databases.

## Task 1: Scaffold `sales-module-agent`

**Files:**

- Create: `sales-module-agent/pom.xml`
- Modify: `pom.xml`
- Modify: `sales-server/pom.xml`

- [ ] **Step 1: Add the Maven module to the root project**

Modify `pom.xml` inside `<modules>` and add:

```xml
        <module>sales-module-agent</module>
```

Place it after `sales-module-infra`.

- [ ] **Step 2: Create module POM**

Create `sales-module-agent/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>cn.ai.sales</groupId>
        <artifactId>sales</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>sales-module-agent</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>AI 销冠 Agent 模块，提供 Gewe 微信接入、客户会话和消息运营能力。</description>

    <dependencies>
        <dependency>
            <groupId>cn.ai.sales</groupId>
            <artifactId>sales-module-system</artifactId>
            <version>${revision}</version>
        </dependency>
        <dependency>
            <groupId>cn.ai.sales</groupId>
            <artifactId>sales-spring-boot-starter-biz-tenant</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.ai.sales</groupId>
            <artifactId>sales-spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.ai.sales</groupId>
            <artifactId>sales-spring-boot-starter-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Add server dependency**

Modify `sales-server/pom.xml` and add after `sales-module-infra`:

```xml
        <dependency>
            <groupId>cn.ai.sales</groupId>
            <artifactId>sales-module-agent</artifactId>
            <version>${revision}</version>
        </dependency>
```

- [ ] **Step 4: Create package directories**

Run:

```bash
mkdir -p sales-module-agent/src/main/java/cn/ai/sales/module/agent/{controller/admin/{account,contact,conversation,webhook},dal/{dataobject,mysql},enums,service/{account,contact,conversation,webhook}}
mkdir -p sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/webhook
```

- [ ] **Step 5: Verify scaffold build**

Run:

```bash
mvn -pl sales-module-agent -am -DskipTests package
```

Expected: build succeeds and includes `sales-module-agent` in the reactor.

- [ ] **Step 6: Commit scaffold**

```bash
git add pom.xml sales-server/pom.xml sales-module-agent/pom.xml
git commit -m "feat(agent): scaffold agent module"
```

## Task 2: Add PostgreSQL Schema And Menus

**Files:**

- Create: `sql/postgresql/agent.sql`
- Modify: `script/docker/docker-compose.local.yml`

- [ ] **Step 1: Create schema SQL**

Create `sql/postgresql/agent.sql`:

```sql
-- ai-sales-agent V1.1 schema and menu seed

CREATE TABLE IF NOT EXISTS agent_agent (
  id int8 NOT NULL,
  name varchar(64) NOT NULL,
  alias_name varchar(64) NULL,
  owner_user_id int8 NULL,
  scene varchar(128) NULL,
  target_customer_desc varchar(255) NULL,
  reply_mode varchar(32) NOT NULL DEFAULT 'MANUAL_CONFIRM',
  status int2 NOT NULL DEFAULT 0,
  draft_version int4 NOT NULL DEFAULT 1,
  online_version int4 NOT NULL DEFAULT 0,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_agent PRIMARY KEY (id)
);

COMMENT ON TABLE agent_agent IS 'AI 销冠 Agent';
COMMENT ON COLUMN agent_agent.name IS 'Agent 名称';
COMMENT ON COLUMN agent_agent.reply_mode IS '回复模式';
COMMENT ON COLUMN agent_agent.status IS '状态';
COMMENT ON COLUMN agent_agent.tenant_id IS '租户编号';

CREATE TABLE IF NOT EXISTS agent_wechat_account (
  id int8 NOT NULL,
  agent_id int8 NULL,
  owner_user_id int8 NULL,
  gewe_app_id varchar(128) NOT NULL,
  gewe_account_id varchar(128) NULL,
  wechat_id varchar(128) NULL,
  nickname varchar(128) NULL,
  avatar varchar(512) NULL,
  callback_token varchar(64) NOT NULL,
  callback_secret varchar(128) NULL,
  callback_url varchar(512) NULL,
  login_status int2 NOT NULL DEFAULT 0,
  status int2 NOT NULL DEFAULT 0,
  last_heartbeat_time timestamp NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_wechat_account PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_callback_token
  ON agent_wechat_account(callback_token)
  WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_account_tenant_app
  ON agent_wechat_account(tenant_id, gewe_app_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_wechat_account IS 'AI 销冠 Gewe 托管微信账号';
COMMENT ON COLUMN agent_wechat_account.gewe_app_id IS 'Gewe Appid 或设备 ID';
COMMENT ON COLUMN agent_wechat_account.callback_token IS '回调令牌';
COMMENT ON COLUMN agent_wechat_account.login_status IS '登录状态';

CREATE TABLE IF NOT EXISTS agent_wechat_contact (
  id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  external_user_id varchar(128) NOT NULL,
  wechat_id varchar(128) NULL,
  nickname varchar(128) NULL,
  remark varchar(128) NULL,
  avatar varchar(512) NULL,
  customer_level int2 NOT NULL DEFAULT 0,
  owner_user_id int8 NULL,
  risk_level int2 NOT NULL DEFAULT 0,
  last_message_time timestamp NULL,
  last_conversation_status int2 NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_wechat_contact PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_wechat_contact_account_external
  ON agent_wechat_contact(tenant_id, wechat_account_id, external_user_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_wechat_contact IS 'AI 销冠微信好友';
COMMENT ON COLUMN agent_wechat_contact.customer_level IS '客户等级：普通、目标客户、重要客户';
COMMENT ON COLUMN agent_wechat_contact.risk_level IS '风险等级：绿、黄、红';

CREATE TABLE IF NOT EXISTS agent_conversation (
  id int8 NOT NULL,
  agent_id int8 NULL,
  wechat_account_id int8 NOT NULL,
  contact_id int8 NOT NULL,
  status int2 NOT NULL DEFAULT 0,
  risk_level int2 NOT NULL DEFAULT 0,
  last_message_id int8 NULL,
  last_message_time timestamp NULL,
  continuous_auto_reply_count int4 NOT NULL DEFAULT 0,
  human_takeover_user_id int8 NULL,
  human_takeover_time timestamp NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_conversation PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_conversation_account_contact
  ON agent_conversation(tenant_id, wechat_account_id, contact_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_conversation IS 'AI 销冠客户会话';
COMMENT ON COLUMN agent_conversation.status IS '会话状态';
COMMENT ON COLUMN agent_conversation.risk_level IS '风险等级';

CREATE TABLE IF NOT EXISTS agent_message (
  id int8 NOT NULL,
  conversation_id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  contact_id int8 NOT NULL,
  direction int2 NOT NULL,
  sender_type int2 NOT NULL,
  message_type int2 NOT NULL,
  content text NULL,
  raw_payload jsonb NULL,
  gewe_message_id varchar(128) NULL,
  send_status int2 NOT NULL DEFAULT 0,
  intent varchar(128) NULL,
  matched_policy varchar(255) NULL,
  audit_note varchar(512) NULL,
  operator_user_id int8 NULL,
  message_time timestamp NOT NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_message PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_message_account_gewe_msg
  ON agent_message(tenant_id, wechat_account_id, gewe_message_id)
  WHERE deleted = 0 AND gewe_message_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_agent_message_conversation_time
  ON agent_message(tenant_id, conversation_id, message_time);

COMMENT ON TABLE agent_message IS 'AI 销冠会话消息';
COMMENT ON COLUMN agent_message.direction IS '消息方向';
COMMENT ON COLUMN agent_message.sender_type IS '发送方类型';
COMMENT ON COLUMN agent_message.message_type IS '消息类型';

CREATE TABLE IF NOT EXISTS agent_webhook_event (
  id int8 NOT NULL,
  wechat_account_id int8 NOT NULL,
  event_id varchar(128) NOT NULL,
  event_type varchar(64) NOT NULL,
  signature_valid boolean NOT NULL DEFAULT false,
  raw_payload jsonb NOT NULL,
  process_status int2 NOT NULL DEFAULT 0,
  error_message varchar(512) NULL,
  creator varchar(64) NULL DEFAULT '',
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) NULL DEFAULT '',
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int2 NOT NULL DEFAULT 0,
  tenant_id int8 NOT NULL DEFAULT 0,
  CONSTRAINT pk_agent_webhook_event PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_webhook_account_event
  ON agent_webhook_event(tenant_id, wechat_account_id, event_id)
  WHERE deleted = 0;

COMMENT ON TABLE agent_webhook_event IS 'AI 销冠 Gewe 原始回调事件';
COMMENT ON COLUMN agent_webhook_event.event_id IS '事件唯一键，使用 Appid + Data.NewMsgId';

DROP SEQUENCE IF EXISTS agent_agent_seq;
CREATE SEQUENCE agent_agent_seq START 1;
DROP SEQUENCE IF EXISTS agent_wechat_account_seq;
CREATE SEQUENCE agent_wechat_account_seq START 1;
DROP SEQUENCE IF EXISTS agent_wechat_contact_seq;
CREATE SEQUENCE agent_wechat_contact_seq START 1;
DROP SEQUENCE IF EXISTS agent_conversation_seq;
CREATE SEQUENCE agent_conversation_seq START 1;
DROP SEQUENCE IF EXISTS agent_message_seq;
CREATE SEQUENCE agent_message_seq START 1;
DROP SEQUENCE IF EXISTS agent_webhook_event_seq;
CREATE SEQUENCE agent_webhook_event_seq START 1;

-- Menu seed. IDs use the 6000+ range, above the current system_menu_seq START 5986.
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6000, 'AI 销冠', '', 1, 35, 0, '/agent', 'ep:chat-dot-square', NULL, NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6001, '微信账号', 'agent:wechat-account:query', 2, 10, 6000, 'wechat-account', 'ep:connection', 'agent/wechatAccount/index', 'AgentWechatAccount', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6002, '客户好友', 'agent:contact:query', 2, 20, 6000, 'contact', 'ep:user', 'agent/contact/index', 'AgentContact', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6003, '会话记录', 'agent:conversation:query', 2, 30, 6000, 'conversation', 'ep:chat-line-round', 'agent/conversation/index', 'AgentConversation', 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES
  (6010, '微信账号创建', 'agent:wechat-account:create', 3, 1, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6011, '微信账号修改', 'agent:wechat-account:update', 3, 2, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6012, '微信账号删除', 'agent:wechat-account:delete', 3, 3, 6001, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
  (6013, '客户好友修改', 'agent:contact:update', 3, 1, 6002, '', '', '', NULL, 0, true, true, true, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6014), false);
```

- [ ] **Step 2: Add local Docker import**

Modify `script/docker/docker-compose.local.yml` under the PostgreSQL service volumes and add:

```yaml
      - ../../sql/postgresql/agent.sql:/docker-entrypoint-initdb.d/03-agent.sql:ro
```

- [ ] **Step 3: Apply schema to current local PostgreSQL**

Run:

```bash
docker cp sql/postgresql/agent.sql ai-sales-postgres:/tmp/agent.sql
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -v ON_ERROR_STOP=1 -f /tmp/agent.sql
```

Expected: SQL completes with no errors.

- [ ] **Step 4: Verify schema and menu**

Run:

```bash
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from information_schema.tables where table_schema='public' and table_name like 'agent_%';"
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select name from system_menu where id in (6000,6001,6002,6003) order by id;"
```

Expected first output: `6`.

Expected second output:

```text
AI 销冠
微信账号
客户好友
会话记录
```

- [ ] **Step 5: Commit schema**

```bash
git add sql/postgresql/agent.sql script/docker/docker-compose.local.yml
git commit -m "feat(agent): add v1.1 schema and menus"
```

## Task 3: Add Backend Constants, DOs, And Mappers

**Files:**

- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/ErrorCodeConstants.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums/AgentConstants.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentWechatAccountDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentWechatContactDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentConversationDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentMessageDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/dataobject/AgentWebhookEventDO.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentMapper.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentWechatAccountMapper.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentWechatContactMapper.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentConversationMapper.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentMessageMapper.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal/mysql/AgentWebhookEventMapper.java`

- [ ] **Step 1: Add error codes**

Create `ErrorCodeConstants.java`:

```java
package cn.ai.sales.module.agent.enums;

import cn.ai.sales.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode AGENT_NOT_EXISTS = new ErrorCode(1_010_000_000, "Agent 不存在");
    ErrorCode WECHAT_ACCOUNT_NOT_EXISTS = new ErrorCode(1_010_001_000, "微信账号不存在");
    ErrorCode WECHAT_ACCOUNT_CALLBACK_TOKEN_DUPLICATE = new ErrorCode(1_010_001_001, "微信账号回调令牌已存在");
    ErrorCode WECHAT_ACCOUNT_GEWE_APP_DUPLICATE = new ErrorCode(1_010_001_002, "Gewe Appid 已存在");
    ErrorCode WECHAT_CONTACT_NOT_EXISTS = new ErrorCode(1_010_002_000, "微信好友不存在");
    ErrorCode CONVERSATION_NOT_EXISTS = new ErrorCode(1_010_003_000, "会话不存在");
    ErrorCode WEBHOOK_ACCOUNT_NOT_FOUND = new ErrorCode(1_010_004_000, "回调对应的微信账号不存在");
    ErrorCode WEBHOOK_SIGNATURE_INVALID = new ErrorCode(1_010_004_001, "Gewe 回调签名不正确");
    ErrorCode WEBHOOK_PAYLOAD_INVALID = new ErrorCode(1_010_004_002, "Gewe 回调内容格式不正确");

}
```

- [ ] **Step 2: Add constants**

Create `AgentConstants.java`:

```java
package cn.ai.sales.module.agent.enums;

public interface AgentConstants {

    int STATUS_ENABLE = 0;
    int STATUS_DISABLE = 1;

    int LOGIN_STATUS_UNKNOWN = 0;
    int LOGIN_STATUS_ONLINE = 1;
    int LOGIN_STATUS_OFFLINE = 2;
    int LOGIN_STATUS_EXPIRED = 3;

    int CUSTOMER_LEVEL_NORMAL = 0;
    int CUSTOMER_LEVEL_TARGET = 1;
    int CUSTOMER_LEVEL_IMPORTANT = 2;

    int RISK_LEVEL_GREEN = 0;
    int RISK_LEVEL_YELLOW = 1;
    int RISK_LEVEL_RED = 2;

    int CONVERSATION_STATUS_OPEN = 0;
    int CONVERSATION_STATUS_AI_AUTO = 1;
    int CONVERSATION_STATUS_WAITING_CONFIRM = 2;
    int CONVERSATION_STATUS_HUMAN_TAKEOVER = 3;
    int CONVERSATION_STATUS_CLOSED = 4;

    int MESSAGE_DIRECTION_INBOUND = 1;
    int MESSAGE_DIRECTION_OUTBOUND = 2;

    int SENDER_CUSTOMER = 1;
    int SENDER_AI_AGENT = 2;
    int SENDER_HUMAN_ADVISOR = 3;
    int SENDER_SYSTEM = 4;

    int MESSAGE_TYPE_TEXT = 1;
    int MESSAGE_TYPE_IMAGE = 3;
    int MESSAGE_TYPE_VOICE = 34;
    int MESSAGE_TYPE_VIDEO = 43;
    int MESSAGE_TYPE_FILE_OR_LINK = 49;
    int MESSAGE_TYPE_UNKNOWN = 0;

    int SEND_STATUS_RECEIVED = 0;
    int SEND_STATUS_PENDING_REVIEW = 1;
    int SEND_STATUS_SENT = 2;
    int SEND_STATUS_FAILED = 3;
    int SEND_STATUS_REJECTED = 4;

    int WEBHOOK_STATUS_NEW = 0;
    int WEBHOOK_STATUS_PROCESSED = 1;
    int WEBHOOK_STATUS_DUPLICATE = 2;
    int WEBHOOK_STATUS_FAILED = 3;

}
```

- [ ] **Step 3: Add DO classes**

Create each DO with `@TableName`, `@KeySequence`, `@TableId`, Lombok `@Data`, and extends `TenantBaseDO`. Use Java fields matching the SQL columns exactly in camel case.

Example shape for `AgentWechatAccountDO.java`:

```java
package cn.ai.sales.module.agent.dal.dataobject;

import cn.ai.sales.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("agent_wechat_account")
@KeySequence("agent_wechat_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWechatAccountDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long agentId;
    private Long ownerUserId;
    private String geweAppId;
    private String geweAccountId;
    private String wechatId;
    private String nickname;
    private String avatar;
    private String callbackToken;
    private String callbackSecret;
    private String callbackUrl;
    private Integer loginStatus;
    private Integer status;
    private LocalDateTime lastHeartbeatTime;

}
```

The other DOs use these field lists:

```text
AgentDO: id, name, aliasName, ownerUserId, scene, targetCustomerDesc, replyMode, status, draftVersion, onlineVersion
AgentWechatContactDO: id, wechatAccountId, externalUserId, wechatId, nickname, remark, avatar, customerLevel, ownerUserId, riskLevel, lastMessageTime, lastConversationStatus
AgentConversationDO: id, agentId, wechatAccountId, contactId, status, riskLevel, lastMessageId, lastMessageTime, continuousAutoReplyCount, humanTakeoverUserId, humanTakeoverTime
AgentMessageDO: id, conversationId, wechatAccountId, contactId, direction, senderType, messageType, content, rawPayload, geweMessageId, sendStatus, intent, matchedPolicy, auditNote, operatorUserId, messageTime
AgentWebhookEventDO: id, wechatAccountId, eventId, eventType, signatureValid, rawPayload, processStatus, errorMessage
```

For JSONB columns `rawPayload`, use `@TableField(typeHandler = JacksonTypeHandler.class)` and `@TableName(value = "...", autoResultMap = true)` with field type `Map<String, Object>`.

- [ ] **Step 4: Add mappers**

Each mapper extends `BaseMapperX<T>` and adds page helpers where the admin UI needs paging.

Example `AgentWechatAccountMapper.java`:

```java
package cn.ai.sales.module.agent.dal.mysql;

import cn.ai.sales.framework.common.pojo.PageResult;
import cn.ai.sales.framework.mybatis.core.mapper.BaseMapperX;
import cn.ai.sales.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.ai.sales.module.agent.controller.admin.account.vo.AgentWechatAccountPageReqVO;
import cn.ai.sales.module.agent.dal.dataobject.AgentWechatAccountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentWechatAccountMapper extends BaseMapperX<AgentWechatAccountDO> {

    default PageResult<AgentWechatAccountDO> selectPage(AgentWechatAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AgentWechatAccountDO>()
                .likeIfPresent(AgentWechatAccountDO::getNickname, reqVO.getKeyword())
                .eqIfPresent(AgentWechatAccountDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AgentWechatAccountDO::getLoginStatus, reqVO.getLoginStatus())
                .orderByDesc(AgentWechatAccountDO::getId));
    }

    default AgentWechatAccountDO selectByCallbackToken(String callbackToken) {
        return selectOne(AgentWechatAccountDO::getCallbackToken, callbackToken);
    }

    default AgentWechatAccountDO selectByGeweAppId(String geweAppId) {
        return selectOne(AgentWechatAccountDO::getGeweAppId, geweAppId);
    }

}
```

Add mapper helper requirements:

```text
AgentMapper: selectById only is enough for V1.1.
AgentWechatContactMapper: selectPage by keyword, customerLevel, riskLevel; selectByAccountAndExternalUserId.
AgentConversationMapper: selectPage by wechatAccountId, contactId, status, riskLevel; selectByAccountAndContact.
AgentMessageMapper: selectListByConversationId ordered by messageTime asc and id asc; selectByGeweMessageId.
AgentWebhookEventMapper: selectByAccountAndEventId.
```

- [ ] **Step 5: Compile**

Run:

```bash
mvn -pl sales-module-agent -am -DskipTests compile
```

Expected: compile succeeds.

- [ ] **Step 6: Commit data layer**

```bash
git add sales-module-agent/src/main/java/cn/ai/sales/module/agent/enums sales-module-agent/src/main/java/cn/ai/sales/module/agent/dal
git commit -m "feat(agent): add v1.1 data model"
```

## Task 4: Add Admin CRUD APIs For WeChat Account, Contact, Conversation

**Files:**

- Create VO files under `controller/admin/account/vo`, `contact/vo`, `conversation/vo`
- Create controllers under `controller/admin/account`, `contact`, `conversation`
- Create services under `service/account`, `contact`, `conversation`

- [ ] **Step 1: Add account VOs**

Create:

```text
AgentWechatAccountPageReqVO extends PageParam: keyword, status, loginStatus, createTime
AgentWechatAccountSaveReqVO: id, agentId, ownerUserId, geweAppId, geweAccountId, wechatId, nickname, avatar, callbackSecret, status
AgentWechatAccountRespVO: all visible account fields plus callbackUrl and createTime
```

Validation:

```text
geweAppId: @NotEmpty on create/update
nickname: optional
status: @NotNull
```

- [ ] **Step 2: Add account service**

Methods:

```java
Long createWechatAccount(@Valid AgentWechatAccountSaveReqVO createReqVO);
void updateWechatAccount(@Valid AgentWechatAccountSaveReqVO updateReqVO);
void deleteWechatAccount(Long id);
AgentWechatAccountDO getWechatAccount(Long id);
PageResult<AgentWechatAccountDO> getWechatAccountPage(AgentWechatAccountPageReqVO pageReqVO);
AgentWechatAccountDO getWechatAccountByCallbackToken(String callbackToken);
```

Creation rules:

```text
callbackToken = IdUtil.fastSimpleUUID()
callbackUrl = "/admin-api/agent/gewe/callback/" + callbackToken
loginStatus = LOGIN_STATUS_UNKNOWN
status = STATUS_ENABLE unless request provides a status
```

Duplicate checks:

```text
callbackToken must be unique
geweAppId must be unique inside the current tenant
```

- [ ] **Step 3: Add account controller**

Controller:

```text
@RestController
@RequestMapping("/agent/wechat-account")
```

Endpoints:

```text
POST /create -> agent:wechat-account:create
PUT /update -> agent:wechat-account:update
DELETE /delete?id= -> agent:wechat-account:delete
GET /get?id= -> agent:wechat-account:query
GET /page -> agent:wechat-account:query
```

- [ ] **Step 4: Add contact read/update APIs**

Methods:

```text
GET /agent/contact/page
PUT /agent/contact/update-level
PUT /agent/contact/update-owner
```

VOs:

```text
AgentWechatContactPageReqVO extends PageParam: wechatAccountId, keyword, customerLevel, riskLevel
AgentWechatContactRespVO: id, wechatAccountId, externalUserId, wechatId, nickname, remark, avatar, customerLevel, ownerUserId, riskLevel, lastMessageTime, lastConversationStatus, createTime
AgentWechatContactUpdateLevelReqVO: id, customerLevel
AgentWechatContactUpdateOwnerReqVO: id, ownerUserId
```

- [ ] **Step 5: Add conversation read APIs**

Methods:

```text
GET /agent/conversation/page
GET /agent/conversation/messages?conversationId=
```

VOs:

```text
AgentConversationPageReqVO extends PageParam: wechatAccountId, contactId, status, riskLevel
AgentConversationRespVO: id, agentId, wechatAccountId, contactId, status, riskLevel, lastMessageId, lastMessageTime, continuousAutoReplyCount, humanTakeoverUserId, humanTakeoverTime, createTime
AgentMessageRespVO: id, conversationId, direction, senderType, messageType, content, geweMessageId, sendStatus, intent, matchedPolicy, auditNote, operatorUserId, messageTime, createTime
```

- [ ] **Step 6: Compile**

Run:

```bash
mvn -pl sales-module-agent -am -DskipTests compile
```

Expected: compile succeeds.

- [ ] **Step 7: Commit admin APIs**

```bash
git add sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin sales-module-agent/src/main/java/cn/ai/sales/module/agent/service
git commit -m "feat(agent): add v1.1 admin APIs"
```

## Task 5: Add Gewe Webhook Parser And Callback Flow

**Files:**

- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/webhook/GeweCallbackMessage.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/webhook/GeweCallbackParser.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/webhook/AgentWebhookService.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/webhook/AgentWebhookServiceImpl.java`
- Create: `sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/webhook/GeweCallbackController.java`
- Create: `sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/webhook/GeweCallbackParserTest.java`

- [ ] **Step 1: Write parser tests**

Create a test for the documented Gewe v1.0 payload shape:

```java
package cn.ai.sales.module.agent.service.webhook;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeweCallbackParserTest {

    private final GeweCallbackParser parser = new GeweCallbackParser();

    @Test
    void parseTextMessage() {
        Map<String, Object> payload = Map.of(
                "TypeName", "AddMsg",
                "Appid", "wx-app-001",
                "Wxid", "wxid_owner",
                "Data", Map.of(
                        "NewMsgId", 3768973957878705021L,
                        "MsgType", 1,
                        "FromUserName", Map.of("string", "wxid_customer"),
                        "ToUserName", Map.of("string", "wxid_owner"),
                        "Content", Map.of("string", "你好，想了解产品"),
                        "CreateTime", 1779163000L,
                        "PushContent", "客户: 你好，想了解产品"
                )
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.eventId()).isEqualTo("wx-app-001:3768973957878705021");
        assertThat(message.eventType()).isEqualTo("AddMsg");
        assertThat(message.geweAppId()).isEqualTo("wx-app-001");
        assertThat(message.ownerWxid()).isEqualTo("wxid_owner");
        assertThat(message.contactWxid()).isEqualTo("wxid_customer");
        assertThat(message.geweMessageId()).isEqualTo("3768973957878705021");
        assertThat(message.messageType()).isEqualTo(1);
        assertThat(message.content()).isEqualTo("你好，想了解产品");
    }

    @Test
    void parseSelfSentMessageUsesToUserAsContact() {
        Map<String, Object> payload = Map.of(
                "TypeName", "AddMsg",
                "Appid", "wx-app-001",
                "Wxid", "wxid_owner",
                "Data", Map.of(
                        "NewMsgId", 10001L,
                        "MsgType", 1,
                        "FromUserName", Map.of("string", "wxid_owner"),
                        "ToUserName", Map.of("string", "wxid_customer"),
                        "Content", Map.of("string", "我发你资料"),
                        "CreateTime", 1779163001L
                )
        );

        GeweCallbackMessage message = parser.parse(payload);

        assertThat(message.selfSent()).isTrue();
        assertThat(message.contactWxid()).isEqualTo("wxid_customer");
    }

}
```

- [ ] **Step 2: Run parser tests and confirm they fail**

Run:

```bash
mvn -pl sales-module-agent -am -Dtest=GeweCallbackParserTest test
```

Expected: compile fails because parser classes do not exist.

- [ ] **Step 3: Implement parser record**

Create `GeweCallbackMessage.java`:

```java
package cn.ai.sales.module.agent.service.webhook;

import java.time.LocalDateTime;
import java.util.Map;

public record GeweCallbackMessage(
        String eventId,
        String eventType,
        String geweAppId,
        String ownerWxid,
        String contactWxid,
        String geweMessageId,
        Integer messageType,
        String content,
        LocalDateTime messageTime,
        boolean selfSent,
        Map<String, Object> rawPayload
) {
}
```

- [ ] **Step 4: Implement parser**

Create `GeweCallbackParser.java`. Use safe map reads for:

```text
TypeName
Appid
Wxid
Data.NewMsgId
Data.MsgType
Data.FromUserName.string
Data.ToUserName.string
Data.Content.string
Data.CreateTime
```

Rules:

```text
selfSent = Data.FromUserName.string equals Wxid
contactWxid = selfSent ? Data.ToUserName.string : Data.FromUserName.string
eventId = Appid + ":" + Data.NewMsgId
messageTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(CreateTime), ZoneId.systemDefault())
```

Throw `ServiceExceptionUtil.exception(WEBHOOK_PAYLOAD_INVALID)` when Appid, Wxid, NewMsgId, FromUserName, or ToUserName is missing.

- [ ] **Step 5: Run parser tests and confirm they pass**

Run:

```bash
mvn -pl sales-module-agent -am -Dtest=GeweCallbackParserTest test
```

Expected: tests pass.

- [ ] **Step 6: Implement callback controller**

Create `GeweCallbackController.java`:

```text
@RestController
@RequestMapping("/agent/gewe")
@Validated
```

Endpoint:

```text
POST /callback/{callbackToken}
```

Annotations:

```text
@PermitAll
@TenantIgnore
```

Input:

```text
@PathVariable String callbackToken
@RequestBody Map<String, Object> payload
@RequestHeader(value = "X-GEWE-SIGNATURE", required = false) String signature
```

Return:

```text
CommonResult<Boolean>
```

- [ ] **Step 7: Implement webhook service**

`AgentWebhookService.handleGeweCallback(callbackToken, signature, payload)` must:

1. Query account by `callbackToken` with `TenantUtils.executeIgnore(() -> accountMapper.selectByCallbackToken(callbackToken))`.
2. Validate account exists and enabled.
3. Validate signature when `callbackSecret` is not blank. Use HMAC-SHA256 of the raw JSON string if the provider is configured to send signatures; while V1.1 stores `signatureValid`, it accepts empty signature when no secret is configured.
4. Execute the rest inside `TenantUtils.execute(account.getTenantId(), () -> {...})`.
5. Parse payload.
6. Find duplicate event by `wechatAccountId + eventId`.
7. If duplicate exists, insert no message and return success.
8. Insert `agent_webhook_event` with status `NEW`.
9. Upsert `agent_wechat_contact` by `wechatAccountId + contactWxid`.
10. Upsert `agent_conversation` by `wechatAccountId + contactId`.
11. Insert `agent_message` with inbound customer direction for non-self messages and outbound human direction for self-sent messages.
12. Update conversation last message fields.
13. Update contact last message fields.
14. Update webhook event status to `PROCESSED`.
15. If business processing fails, update webhook event status to `FAILED` with error message and rethrow.

- [ ] **Step 8: Run backend build**

Run:

```bash
mvn -pl sales-module-agent -am test
```

Expected: tests pass.

- [ ] **Step 9: Commit webhook flow**

```bash
git add sales-module-agent/src/main/java/cn/ai/sales/module/agent/controller/admin/webhook sales-module-agent/src/main/java/cn/ai/sales/module/agent/service/webhook sales-module-agent/src/test/java/cn/ai/sales/module/agent/service/webhook
git commit -m "feat(agent): receive gewe webhook messages"
```

## Task 6: Add Frontend V1.1 Pages

**Files:**

- Create: `sales-ui/sales-ui-admin/src/api/agent/wechatAccount/index.ts`
- Create: `sales-ui/sales-ui-admin/src/api/agent/contact/index.ts`
- Create: `sales-ui/sales-ui-admin/src/api/agent/conversation/index.ts`
- Create: `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/index.vue`
- Create: `sales-ui/sales-ui-admin/src/views/agent/wechatAccount/WechatAccountForm.vue`
- Create: `sales-ui/sales-ui-admin/src/views/agent/contact/index.vue`
- Create: `sales-ui/sales-ui-admin/src/views/agent/conversation/index.vue`

- [ ] **Step 1: Add TypeScript API modules**

API endpoints:

```text
/agent/wechat-account/page
/agent/wechat-account/get
/agent/wechat-account/create
/agent/wechat-account/update
/agent/wechat-account/delete
/agent/contact/page
/agent/contact/update-level
/agent/contact/update-owner
/agent/conversation/page
/agent/conversation/messages
```

Use the same `request.get/post/put/delete` style as `src/api/system/tenant/index.ts`.

- [ ] **Step 2: Add WeChat account page**

`views/agent/wechatAccount/index.vue` must include:

```text
Search fields: keyword, loginStatus, status
Table columns: id, nickname, wechatId, geweAppId, loginStatus, status, callbackUrl, createTime
Actions: create, update, delete, copy callbackUrl
Permissions: agent:wechat-account:create/update/delete
```

Use `ContentWrap`, `Pagination`, `el-table`, `el-button`, and `Icon` consistent with system pages.

- [ ] **Step 3: Add WeChat account form**

`WechatAccountForm.vue` fields:

```text
geweAppId required
geweAccountId optional
wechatId optional
nickname optional
avatar optional
callbackSecret optional
status required
```

On create and update, call the corresponding API and emit `success`.

- [ ] **Step 4: Add contact page**

`views/agent/contact/index.vue` must include:

```text
Search fields: wechatAccountId, keyword, customerLevel, riskLevel
Table columns: id, nickname, remark, wechatId, customerLevel, riskLevel, ownerUserId, lastMessageTime
Action: set level to 普通/目标客户/重要客户
```

Use integer level values:

```text
0 普通
1 目标客户
2 重要客户
```

- [ ] **Step 5: Add conversation page**

`views/agent/conversation/index.vue` must include:

```text
Search fields: wechatAccountId, contactId, status, riskLevel
Table columns: id, wechatAccountId, contactId, status, riskLevel, lastMessageTime
Action: view messages
Drawer: chronological message list with senderType, direction, content, messageTime
```

- [ ] **Step 6: Type check frontend**

Run:

```bash
cd sales-ui/sales-ui-admin
pnpm ts:check
```

Expected: TypeScript check succeeds.

- [ ] **Step 7: Commit frontend**

```bash
git add sales-ui/sales-ui-admin/src/api/agent sales-ui/sales-ui-admin/src/views/agent
git commit -m "feat(agent): add v1.1 admin pages"
```

## Task 7: End-To-End Verification

**Files:**

- No new files.

- [ ] **Step 1: Build backend**

Run:

```bash
mvn -DskipTests package
```

Expected: build succeeds and `sales-server/target/sales-server.jar` is created.

- [ ] **Step 2: Restart backend**

Stop the current backend process, then run:

```bash
java -jar sales-server/target/sales-server.jar --spring.profiles.active=local
```

Expected: Tomcat starts on `48080`.

- [ ] **Step 3: Log in and create a WeChat account**

Run:

```bash
TOKEN=$(curl -sS -X POST 'http://localhost:48080/admin-api/system/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'tenant-id: 1' \
  --data '{"username":"admin","password":"admin123","captchaVerification":""}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -sS -X POST 'http://localhost:48080/admin-api/agent/wechat-account/create' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'tenant-id: 1' \
  -H 'Content-Type: application/json' \
  --data '{"geweAppId":"wx-app-001","geweAccountId":"gewe-account-001","wechatId":"wxid_owner","nickname":"销售微信 1","status":0}'
```

Expected: JSON response `{"code":0,...}` with a numeric account id.

- [ ] **Step 4: Read callback token**

Run:

```bash
curl -sS 'http://localhost:48080/admin-api/agent/wechat-account/page?pageNo=1&pageSize=10&keyword=销售微信' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'tenant-id: 1'
```

Expected: response includes one account and a `callbackUrl` ending with `/admin-api/agent/gewe/callback/{callbackToken}`.

- [ ] **Step 5: Simulate Gewe callback**

Extract the callback token from Step 4:

```bash
CALLBACK_TOKEN=$(curl -sS 'http://localhost:48080/admin-api/agent/wechat-account/page?pageNo=1&pageSize=10&keyword=销售微信' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'tenant-id: 1' \
  | sed -n 's/.*callback\\/\\([^"]*\\).*/\\1/p')

curl -sS -X POST "http://localhost:48080/admin-api/agent/gewe/callback/$CALLBACK_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{
    "TypeName": "AddMsg",
    "Appid": "wx-app-001",
    "Wxid": "wxid_owner",
    "Data": {
      "NewMsgId": 3768973957878705021,
      "MsgType": 1,
      "FromUserName": { "string": "wxid_customer_001" },
      "ToUserName": { "string": "wxid_owner" },
      "Content": { "string": "你好，我想了解你们的产品" },
      "CreateTime": 1779163000,
      "PushContent": "客户: 你好，我想了解你们的产品"
    }
  }'
```

Expected: JSON response `{"code":0,"msg":"","data":true}`.

- [ ] **Step 6: Verify database records**

Run:

```bash
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from agent_webhook_event;"
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from agent_wechat_contact;"
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from agent_conversation;"
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from agent_message;"
```

Expected:

```text
1
1
1
1
```

- [ ] **Step 7: Verify idempotency**

Run the same callback curl from Step 5 again, then run:

```bash
docker exec ai-sales-postgres psql -U ai_sales -d ai_sales -tAc "select count(*) from agent_message where gewe_message_id='3768973957878705021';"
```

Expected: `1`.

- [ ] **Step 8: Verify frontend**

Open:

```text
http://localhost:3000/
```

Expected:

```text
AI 销冠 menu is visible.
微信账号 page shows the created account.
客户好友 page shows wxid_customer_001.
会话记录 page shows one conversation and one text message in the drawer.
```

- [ ] **Step 9: Commit final V1.1 verification fixups**

If verification required small fixes:

```bash
git add sales-module-agent sales-ui/sales-ui-admin/src/api/agent sales-ui/sales-ui-admin/src/views/agent sql/postgresql/agent.sql script/docker/docker-compose.local.yml pom.xml sales-server/pom.xml
git commit -m "fix(agent): complete v1.1 access loop verification"
```

If no fixes were needed, skip this commit.

## Self-Review Checklist

- [ ] V1.1 creates an independent backend module.
- [ ] V1.1 has schema and menu seed.
- [ ] V1.1 can receive public Gewe callbacks without login.
- [ ] Callback processing restores tenant context before writing tenant tables.
- [ ] Webhook events are deduplicated by `Appid + Data.NewMsgId`.
- [ ] Contacts, conversations, and messages are persisted.
- [ ] Admin pages can inspect the persisted records.
- [ ] Backend build passes.
- [ ] Frontend type check passes.
- [ ] Manual callback smoke test passes.
