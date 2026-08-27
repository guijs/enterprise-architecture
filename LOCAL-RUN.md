# 本地运行指南 (Local Run Guide)

本指南描述如何在本地环境运行完整的订单链路：Gateway → Web → Service。

## 路径契约 (Path Contract)

| 入口 | 端口 | Context Path | 说明 |
|------|------|--------------|------|
| biz-gateway | 9000 | / | 公共入口，处理认证、路由 |
| biz-web | 8080 | /api | 业务 Web 层，控制器挂载在 /orders |
| biz-service | 8081 | /api | 业务 Service 层，内部接口 /internal/order |

### 路由规则

- `POST /auth/login` → Gateway 本地处理（不转发）
- `/api/**` → 转发至 biz-web，**不做 StripPrefix**（Web 的 context-path=/api）
- Feign 调用：Web → Service 使用 `/api/internal/order/{id}` 路径

## 前置条件

- JDK 25+
- Maven 3.9+
- Docker & Docker Compose

## 一键启动基础设施

```bash
cd docker
docker compose up -d
```

这将启动：
- MySQL 8 (3306) - 数据库 biz_db，用户 root/root
- Redis 7 (6379)
- RabbitMQ 3 (5672, 管理界面 15672)

## 构建项目

```bash
mvn -DskipTests package
```

## 启动服务

按顺序启动（每个终端一个）：

```bash
# 1. 启动 biz-service (8081)
java -jar biz-service/target/biz-service.jar --spring.profiles.active=local

# 2. 启动 biz-web (8080)
java -jar biz-web/target/biz-web.jar --spring.profiles.active=local

# 3. 启动 biz-gateway (9000)
java -jar biz-gateway/target/biz-gateway.jar --spring.profiles.active=local
```

或者使用 Maven 插件：

```bash
# 分别在三个终端执行
cd biz-service && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd biz-web && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd biz-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 冒烟测试

### 1. 登录获取 Token

```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

### 2. 创建订单

```bash
TOKEN="<上一步获取的 accessToken>"

curl -X POST http://localhost:9000/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"skuId":1001,"quantity":2,"orderNo":"TEST-001"}'
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": 1234567890123456789
}
```

### 3. 查询订单

```bash
ORDER_ID="<上一步返回的订单ID>"

curl http://localhost:9000/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN"
```

### 4. 未认证访问（应返回 401）

```bash
curl -v http://localhost:9000/api/orders
# HTTP/1.1 401 Unauthorized
```

## JWT 配置

| 属性 | 环境变量 | 默认值 | 说明 |
|------|----------|--------|------|
| gateway.jwt.secret | JWT_SECRET | local-dev-... | HS256 密钥（生产必须配置强随机值，至少32字符） |
| gateway.jwt.access-token-expire-minutes | - | 30 | Token 过期时间（分钟） |

**注意**：生产环境必须通过 `JWT_SECRET` 环境变量配置强密钥，不要使用默认值！

## 本地用户

仅供本地开发演示：

| 用户名 | 密码 | userId |
|--------|------|--------|
| admin | admin123 | 1 |

## 关闭服务

```bash
cd docker
docker compose down
```

## 故障排查

1. **MySQL 连接失败**：确认 docker compose 已启动，检查 3306 端口
2. **Flyway 迁移失败**：检查 biz_db 数据库是否存在
3. **Feign 调用 404**：确认 biz-service 已启动且路径包含 `/api` 前缀
4. **401 Unauthorized**：检查 Token 是否过期，Bearer 前缀是否正确
