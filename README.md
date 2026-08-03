# Enterprise Architecture

Spring Boot 3 多模块企业基础架构，开箱即用，覆盖统一响应、全局异常、JWT 鉴权、MyBatis-Plus、Redis、操作日志、OpenAPI 文档等能力。

## 技术栈

| 组件 | 版本/选型 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.4.5 |
| 安全 | Spring Security + JWT |
| ORM | MyBatis-Plus |
| 缓存 | Redis（local 可用内存缓存） |
| 文档 | SpringDoc OpenAPI |
| 构建 | Maven 多模块 |

## 模块说明

```text
enterprise-architecture
├── ea-common      # 公共模块：统一响应、异常、分页、注解、工具类
├── ea-framework   # 框架模块：Security/JWT、Redis、MyBatis、Web、OpenAPI、日志切面
├── ea-system      # 业务模块：认证、用户管理示例
└── ea-admin       # 启动模块：配置、初始化、打包入口
```

## 快速启动（本地零依赖）

默认激活 `local` 配置：H2 内存库 + 内存 Token 缓存，无需安装 MySQL/Redis。

```bash
mvn clean package -DskipTests
java -jar ea-admin/target/ea-admin-1.0.0-SNAPSHOT.jar
```

或：

```bash
mvn -pl ea-admin -am spring-boot:run
```

启动后访问：

- 健康检查：`GET http://localhost:8080/api/ping`
- 接口文档：`http://localhost:8080/swagger-ui.html`
- 默认账号：`admin / admin123`

## 开发环境（MySQL + Redis）

```bash
cd docker
docker compose up -d
```

```bash
mvn -pl ea-admin -am spring-boot:run -Dspring-boot.run.profiles=dev
```

## 核心接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 登录获取 JWT |
| POST | `/auth/register` | 注册 |
| POST | `/auth/logout` | 退出 |
| GET | `/auth/me` | 当前用户信息 |
| GET | `/system/user/page` | 用户分页 |
| POST | `/system/user` | 新增用户 |
| PUT | `/system/user` | 修改用户 |
| DELETE | `/system/user/{id}` | 删除用户 |

登录示例：

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

携带 Token：

```bash
curl -s http://localhost:8080/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

## 统一响应约定

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1710000000000
}
```

常见状态码：`200` 成功、`400` 参数校验失败、`401` 未认证、`403` 无权限、`500` 系统异常、`600` 业务异常。

## 配置说明

| 配置项 | 说明 |
| --- | --- |
| `ea.jwt.secret` | JWT 签名密钥（生产务必替换） |
| `ea.jwt.expire-seconds` | Token 过期秒数 |
| `ea.security.permit-all` | 匿名访问白名单 |
| `ea.cache.type` | `redis` / `memory` |
| `ea.cors.allowed-origins` | 跨域来源 |

生产环境建议使用 `prod` Profile，并通过环境变量注入：

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`
- `JWT_SECRET`

## 扩展建议

1. 在 `ea-system` 增加角色、菜单、部门、字典等系统管理能力  
2. 将操作日志落库，并补充数据权限、接口限流、多租户  
3. 新增业务模块时，保持 `controller / service / mapper / domain` 分层  
4. 生产环境关闭 H2、Swagger 公网暴露，并轮换 JWT 密钥  

## 许可证

内部项目模板，可按团队规范调整后使用。
