# 个人销售助手 PostgreSQL 测试部署说明

## 目标

测试环境先不假设公司已经提供 PostgreSQL。使用一套 `docker-compose` 同时启动：

- Java 后端：容器内 `48080`，宿主机默认 `48080`
- PostgreSQL：容器内 `5432`，宿主机默认 `15432`
- Redis：容器内 `6379`，宿主机默认 `16379`

后续如果公司提供正式 PostgreSQL，只需要通过环境变量切换数据库连接，不需要改代码。

## 部署文件

- Compose：`script/docker/docker-compose.postgres.yml`
- 环境变量模板：`script/docker/postgres.env.example`
- 初始化 SQL：
  - `sql/postgresql/ruoyi-vue-pro.sql`
  - `sql/postgresql/quartz.sql`
  - `sql/postgresql/agent.sql`
  - `sql/postgresql/agent-gewe-identity-v2.sql`
  - `sql/postgresql/agent-gewe-provider-callback-url.sql`
  - `sql/postgresql/agent-default-quiet-seconds-90.sql`

## 启动步骤

先构建 Java 包：

```bash
mvn -pl sales-server -am -DskipTests package
```

复制环境变量模板：

```bash
cp script/docker/postgres.env.example script/docker/.env
```

编辑 `script/docker/.env`，至少修改：

```bash
POSTGRES_PASSWORD=你的测试库密码
MASTER_DATASOURCE_PASSWORD=你的测试库密码
SLAVE_DATASOURCE_PASSWORD=你的测试库密码
PERSONAL_SALES_ASSISTANT_SKILL_KEY=Frontis分配或测试用SkillKey
AGENT_LLM_DEEPSEEK_API_KEY=DeepSeekKey
```

如果同一台机器已经运行另一套测试容器，可以同时调整：

```bash
SERVER_HOST_PORT=48081
POSTGRES_HOST_PORT=25432
REDIS_HOST_PORT=26379
SERVER_CONTAINER_NAME=ai-sales-server-v2
POSTGRES_CONTAINER_NAME=ai-sales-postgres-v2
REDIS_CONTAINER_NAME=ai-sales-redis-v2
```

启动：

```bash
docker compose --env-file script/docker/.env -f script/docker/docker-compose.postgres.yml up -d --build
```

查看状态：

```bash
docker compose --env-file script/docker/.env -f script/docker/docker-compose.postgres.yml ps
```

查看日志：

```bash
docker logs -f ai-sales-server
```

## 验证

验证 Frontis Skill manifest：

```bash
curl -H "X-Personal-Sales-Assistant-Skill-Key: ${PERSONAL_SALES_ASSISTANT_SKILL_KEY}" \
  http://127.0.0.1:48080/api/v1/skills/personal-sales-assistant/manifest
```

验证 GeWe 回调探针：

```bash
curl -i -X POST 'http://127.0.0.1:48080/api/v1/gewechat/callback?token=YourGeWeCallbackToken' \
  -H 'Content-Type: application/json' \
  -d '{}'
```

GeWe 回调地址必须保留 query 参数名 `token`，因为这是 GeWe 侧保存回调地址时使用的格式。Java 后端已让该公开回调路径跳过后台 OAuth token 过滤，实际鉴权走 GeWe callback token / 签名链路。

对外回调地址示例：

```text
https://lxqz-test-135.frontis.top/api/v1/gewechat/callback?token=YourGeWeCallbackToken
```

## 注意

PostgreSQL 初始化 SQL 只会在数据卷第一次创建时执行。如果已经启动过容器并生成了数据卷，新增 SQL 不会自动重新执行。测试环境需要重建数据库时再执行：

```bash
docker compose --env-file script/docker/.env -f script/docker/docker-compose.postgres.yml down -v
docker compose --env-file script/docker/.env -f script/docker/docker-compose.postgres.yml up -d --build
```

生产或准生产环境不要随意 `down -v`，必须先备份数据库。
