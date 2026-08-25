# 企业级 Spring Boot 4.1 架构设计文档

> 技术栈：Spring Boot 4.1 · JDK 25 · MySQL · Elasticsearch · Redis · RabbitMQ · OpenFeign · Nacos · SkyWalking · ELK

---

## 目录

1. [整体架构](#1-整体架构)

2. [技术栈清单](#2-技术栈清单)

3. [项目模块结构](#3-项目模块结构)

4. [依赖管理体系](#4-依赖管理体系)

5. [Starter 清单与职责](#5-starter-清单与职责)

6. [各组件详细设计](#6-各组件详细设计)

   - 6.1 Gateway 网关

   - 6.2 用户上下文

   - 6.3 统一响应体

   - 6.4 全局异常处理（业务码 + HTTP Status）

   - 6.5 SpringDoc + Knife4j

   - 6.6 分布式锁

   - 6.7 接口限流

   - 6.8 接口幂等

   - 6.9 分布式 ID

   - 6.10 RabbitMQ 可靠性

   - 6.11 MyBatis-Plus

   - 6.12 Redis 规范

   - 6.13 Feign 熔断与错误传递

   - 6.14 链路追踪

   - 6.15 配置加密

   - 6.16 虚拟线程

   - 6.17 优雅停机

   - 6.18 异步线程池

   - 6.19 Nacos 动态配置

   - 6.20 接口与 Feign 调用日志（可配置 + 注解优先级）

   - 6.21 操作日志（审计追踪）

   - 6.22 数据脱敏

   - 6.23 数据库字段加密

   - 6.24 分页规范

   - 6.25 枚举统一处理规范

   - 6.26 日志 ELK 收集

   - 6.27 缓存设计规范（穿透 / 击穿 / 雪崩 / 一致性）

   - 6.28 Elasticsearch 规范

   - 6.29 分布式事务

   - 6.30 数据库设计规范

   - 6.31 认证鉴权与接口安全

   - 6.32 可观测性（Metrics 监控告警）

   - 6.33 测试规范

   - 6.34 容器化与 K8s 部署

7. [完整配置文件参考](#7-完整配置文件参考)

8. [优化与注意事项](#8-优化与注意事项)

---

## 1. 整体架构

```

客户端请求

    │

    ▼

┌─────────────────────────────────┐

│         Spring Cloud Gateway     │  Token 验证、用户信息解析、路由、限流、CORS

└─────────────────┬───────────────┘

                  │ Header 透传 X-User-Id / X-User-Name / X-Trace-Id

    ┌─────────────┴──────────────┐

    ▼                            ▼

┌─────────┐               ┌──────────┐

│ biz-web │               │ biz-web  │   Web 层：Controller、参数校验、用户上下文

└────┬────┘               └────┬─────┘

     │  OpenFeign + Resilience4j 熔断

     ▼

┌──────────────┐ 数据脱敏

│ biz-service  │   Service 层：业务逻辑、MQ、DB、ES、Redis、分布式锁

└──────────────┘

     │

     ├── MySQL（MyBatis-Plus）

     ├── Elasticsearch（ES Java Client）

     ├── Redis（Redisson）

     └── RabbitMQ（可靠性机制）

```

### 服务间关系

- **Gateway → Web 层**：HTTP 路由，Header 透传用户信息

- **Web 层 → Service 层**：同进程调用或 Feign（跨服务）

- **Web 层异步**：TTL 线程池，自动传递用户上下文

- **跨服务调用**：不透传 ThreadLocal，Feign 通过 Header 透传

---

## 2. 技术栈清单

| 分类      | 组件                                                | 版本                 |

| ------- | ------------------------------------------------- | ------------------ |

| 核心框架    | Spring Boot                                       | 4.1.x              |

| JDK     | OpenJDK / Eclipse Temurin                         | 25（LTS）            |

| 服务注册/配置 | Nacos                                             | 3.x                |

| 微服务框架   | Spring Cloud / Spring Cloud Alibaba               | 与 Boot 4.1 对应版本     |

| 网关      | Spring Cloud Gateway                              | 随 Spring Cloud     |

| 服务调用    | OpenFeign                                         | 随 Spring Cloud     |

| 熔断降级    | Resilience4j                                      | 2.x                |

| 分布式事务   | 本地消息表 / Seata（AT/TCC）                            | Seata 2.x          |

| 数据库迁移   | Flyway                                            | 随 Spring Boot      |

| 指标监控    | Micrometer + Prometheus + Grafana                 | 随 Spring Boot      |

| 单元/集成测试 | JUnit 5 + Mockito + Testcontainers                | —                  |

| 容器编排    | Docker + Kubernetes                               | —                  |

| 数据库     | MySQL                                             | 8.0+               |

| ORM     | MyBatis-Plus                                      | 3.5.x              |

| 搜索引擎    | Elasticsearch                                     | 8.x                |

| 缓存/锁    | Redis + Redisson                                  | Redis 7.x          |

| 消息队列    | RabbitMQ                                          | 3.x                |

| 链路追踪    | SkyWalking                                        | 9.x（Agent）         |

| 日志收集    | ELK（Filebeat + Logstash + Elasticsearch + Kibana） | 8.x                |

| API 文档  | SpringDoc OpenAPI 3 + Knife4j                     | 最新适配版              |

| 配置加密    | Jasypt                                            | 3.x                |

| 字段加密    | AES-256-GCM / 国密 SM4（可选）                          | —                  |

| 分布式 ID  | 自研 Snowflake + Redis WorkerId                     | —                  |

| 对象转换    | MapStruct                                         | 1.5.x              |

| 工具库     | Hutool / Guava                                    | —                  |

---

## 3. 项目模块结构

```

company-platform/

│

├── platform-parent/                  # 公司级父 POM（独立仓库，发布到 Nexus）

│

├── platform-starters/                # 基础设施 Starter 仓库

│   ├── my-security-starter           # 用户上下文

│   ├── my-web-starter                # 统一响应、异常(业务码+HTTP)、文档、线程池、分页、枚举、脱敏、Feign 错误传递

│   ├── my-log-starter                # Controller/Feign 调用日志、操作审计、ELK 日志规范

│   ├── my-crypto-starter             # 数据库字段加解密

│   ├── my-idempotent-starter         # 接口幂等

│   ├── my-id-starter                 # 分布式 ID

│   ├── my-rabbit-starter             # RabbitMQ 可靠性

│   ├── my-redis-starter              # Redis 规范化封装 + 分布式锁 + 限流

│   └── my-mybatis-starter            # MyBatis-Plus 配置

│

├── biz-gateway/                      # 网关（独立服务）

│

├── biz-web/                          # 业务 Web 层

│   ├── pom.xml

│   └── src/

│

└── biz-service/                      # 业务 Service 层

    ├── pom.xml

    └── src/

```

### 单服务内部分层（以 biz-web 为例）

```

biz-web/

├── controller/        # 接口入口，仅做参数接收和响应封装

├── dto/               # 请求 DTO（带校验注解）

├── vo/                # 响应 VO

├── config/            # 业务级配置

└── [BizWebApplication.java](http://BizWebApplication.java)

```

---

## 4. 依赖管理体系

### 四级依赖管理策略

```

公司父 POM（platform-parent）

├── <parent>  spring-boot-starter-parent    # 锁定 Spring 生态版本

├── <dependencyManagement>                  # 锁版本，不引入

│   ├── 内部 Starter 版本

│   ├── 第三方组件版本（RabbitMQ、Redisson、MyBatis-Plus...）

│   └── 子模块互引版本

├── <dependencies>                          # 全局直接引入

│   ├── lombok（provided）

│   └── spring-boot-starter-test（test）

└── <profiles>                              # 环境 Profile（dev/test/prod）

```

### 依赖分类原则

| 类型         | 位置                                 | 示例                |

| ---------- | ---------------------------------- | ----------------- |

| 全局必用       | 父 POM `<dependencies>`             | Lombok、Test       |

| 常用但非全局     | 父 POM `<dependencyManagement>`     | Validation、Web、MQ |

| 模块独有       | 子模块 POM（无需写版本）                     | Mapper、ES Client  |

| 内部 Starter | 父 POM `<dependencyManagement>` 锁版本 | my-redis-starter  |

### 关键注意

- Lombok 为 `provided` 作用域，**不传递依赖**，每个需要 Lombok 的仓库都必须声明（版本由父 POM 统一管控）

- Starter 内部对外依赖尽量使用 `<optional>true</optional>`，避免强制传递

- 自定义 Starter 避免使用 `@ComponentScan`，使用 `@AutoConfiguration` + `AutoConfiguration.imports` 注册

---

## 5. Starter 清单与职责

| Starter                 | 核心职责                                                                                  | 主要引入方                 |

| ----------------------- | ------------------------------------------------------------------------------------- | --------------------- |

| `my-security-starter`   | TTL 用户上下文、Web 拦截器、Header 解析                                                           | biz-web               |

| `my-web-starter`        | 统一响应 Result、全局异常（业务码+HTTP Status）、参数校验、SpringDoc+Knife4j、TTL 异步线程池、序列化、分页、枚举、脱敏、Feign ErrorDecoder | biz-web               |

| `my-log-starter`        | 请求/Feign 调用日志（全局配置+注解优先级）`@OperationLog` 审计、结构化 JSON 日志、ELK 采集约定 | biz-web / biz-service |

| `my-crypto-starter`     | 字段加解密工具、MyBatis TypeHandler、密钥配置                                                      | biz-service           |

| `my-idempotent-starter` | Redis 幂等`@Idempotent` AOP                                                            | biz-web               |

| `my-id-starter`         | Snowflake + Redis WorkerId 分配`IdHelper`、MyBatis-Plus 集成                              | biz-service           |

| `my-rabbit-starter`     | Publisher Confirm、死信队列、手动 ACK 规范                                                      | biz-service           |

| `my-redis-starter`      | Redis 序列化、key 命名规范、Redisson Client、`@DistributedLock` AOP、`LockTemplate`、`@RateLimit` 滑动窗口限流 | biz-web / biz-service |

| `my-mybatis-starter`    | MyBatis-Plus 插件配置、自动填充、乐观锁、防全表更新                                                      | biz-service           |

### Starter 内部结构规范

```

my-xxx-spring-boot-starter/

├── my-xxx-spring-boot-autoconfigure/    # 配置逻辑（核心）

│   └── src/main/resources/META-INF/spring/

│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports

└── my-xxx-spring-boot-starter/          # 空壳聚合，仅做依赖编排

```

---

## 6. 各组件详细设计

---

### 6.1 Gateway 网关

#### 职责

- Token 验证与用户信息解析，注入下游 Header

- 统一路由转发

- 全局 CORS 跨域配置

- 白名单放行

#### Token 验证 Filter

```java

@Component

public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(

        "/auth/login", "/auth/refresh"

    );

    @Override

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (WHITE_[LIST.stream](http://LIST.stream)().anyMatch(path::startsWith)) {

            return chain.filter(exchange);

        }

        String token = exchange.getRequest().getHeaders()

            .getFirst(HttpHeaders.AUTHORIZATION);

        UserInfo userInfo = tokenService.parseToken(token);

        if (userInfo == null) {

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();

        }

        // 生成/透传链路 ID（与 SkyWalking tid 并存，用于无 Agent 环境的日志串联）

        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");

        if (StrUtil.isBlank(traceId)) {

            traceId = UUID.randomUUID().toString().replace("-", "");

        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()

            // 先剥离客户端可能伪造的用户身份 Header，再写入网关解析结果，防伪造

            .headers(h -> {

                h.remove("X-User-Id");

                h.remove("X-User-Name");

            })

            .header("X-User-Id", userInfo.getUserId())

            .header("X-User-Name",

                URLEncoder.encode(userInfo.getUserName(), StandardCharsets.UTF_8))

            .header("X-Trace-Id", traceId)

            .build();

        return chain.filter(exchange.mutate().request(mutated).build());

    }

    @Override

    public int getOrder() { return -100; }

}

```

> **安全要点**：下游服务通过 `X-User-Id` 建立用户上下文（见 6.2），本质是**信任网关注入的 Header**。因此必须保证下游服务**不可被外部直接访问**（K8s NetworkPolicy / 内网隔离 / Service Mesh mTLS），否则可绕过网关伪造任意用户身份。网关在注入前先剥离同名 Header，防止客户端伪造透传。更高安全等级可对透传信息做网关签名、下游验签。

#### 全局 CORS 配置

> **安全警示**`allowedOriginPattern="*"` 与 `allowCredentials=true` **不可同时使用**（浏览器会拒绝，且属高风险配置）。生产必须配置**明确的域名白名单**，从配置中心下发，禁止通配。

```java

@Bean

public CorsWebFilter corsWebFilter(GatewayCorsProperties props) {

    CorsConfiguration config = new CorsConfiguration();

    // 从配置读取白名单域名，如 [https://admin.company.com、https://app.company.com](https://admin.company.com、https://app.company.com)

    config.setAllowedOrigins(props.getAllowedOrigins());

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));

    config.setAllowCredentials(true);   // 携带 Cookie 时，Origin 必须是明确白名单，不能为 *

    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", config);

    return new CorsWebFilter(source);

}

```

```yaml

gateway:

  cors:

    allowed-origins:

      - [https://admin.company.com](https://admin.company.com)

      - [https://app.company.com](https://app.company.com)

```

#### 路由配置（application.yml）

```yaml

spring:

  cloud:

    gateway:

      routes:

        - id: biz-web

          uri: lb://biz-web

          predicates:

            - Path=/api/biz/**

          filters:

            - StripPrefix=1

```

---

### 6.2 用户上下文（my-security-starter）

#### 核心：TransmittableThreadLocal

原生 `ThreadLocal` 不跨线程`@Async`、线程池场景会丢失用户信息，使用阿里 TTL 解决。

```java

public class UserContext {

    private static final TransmittableThreadLocal<UserInfo> CONTEXT

        = new TransmittableThreadLocal<>();

    public static void set(UserInfo user)  { CONTEXT.set(user); }

    public static UserInfo get()           { return CONTEXT.get(); }

    public static String getUserId()       {

        return Optional.ofNullable(CONTEXT.get())

                       .map(UserInfo::getUserId).orElse(null);

    }

    // 请求结束必须调用，防止线程池复用时信息污染

    public static void remove()            { CONTEXT.remove(); }

}

```

#### Web 层拦截器（从 Gateway Header 解析）

```java

@Component

public class UserInterceptor implements HandlerInterceptor {

    @Override

    public boolean preHandle(HttpServletRequest request,

                             HttpServletResponse response, Object handler) {

        String userId   = request.getHeader("X-User-Id");

        String userName = request.getHeader("X-User-Name");

        if (StrUtil.isNotBlank(userId)) {

            UserContext.set(new UserInfo(

                userId,

                URLDecoder.decode(userName, StandardCharsets.UTF_8)

            ));

        }

        return true;

    }

    @Override

    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,

                                Object handler, Exception ex) {

        UserContext.remove(); // 必须清理

    }

}

```

---

### 6.3 统一响应体（my-web-starter）

#### 响应约定：HTTP Status + Result 并存

| 层级 | 职责 | 示例 |

|---|---|---|

| HTTP Status | 错误大类（监控、网关、Feign、缓存可感知） | `200` / `400` / `409` / `500` |

| `Result.code` | **业务错误码**（前后端约定、跨服务透传） | `0` 成功 / `10001` 库存不足 |

| `Result.message` | 可读说明 | `库存不足` |

| `Result.data` | 成功=业务数据；失败=可选错误上下文（Map / VO / List） | `{ skuId, available }` |

| `Result.traceId` | 链路追踪 | SkyWalking tid |

> **禁止**一律 HTTP 200 只靠 `Result.code` 判断成败。成功`200` + `code=0`；失败：对应 4xx/5xx + 业务 `code`。  

> 错误附加信息**复用 `data`**`Object` / 泛型），不另建 Map 字段；键值场景直接传 `Map` 即可。

```java

@Data

@NoArgsConstructor

@AllArgsConstructor

public class Result<T> {

    private int    code;       // 业务码：0=成功，非 0=业务失败（不是 HTTP Status）

    private String message;

    private T      data;       // 成功=业务数据；失败=可选错误上下文

    private String traceId;

    private long   timestamp;

    public static final int SUCCESS_CODE = 0;

    public static <T> Result<T> ok(T data) {

        return new Result<>(SUCCESS_CODE, "success", data,

            TraceContext.traceId(), System.currentTimeMillis());

    }

    public static <T> Result<T> fail(int code, String message) {

        return fail(code, message, null);

    }

    public static <T> Result<T> fail(int code, String message, T data) {

        return new Result<>(code, message, data,

            TraceContext.traceId(), System.currentTimeMillis());

    }

    public boolean isSuccess() {

        return code == SUCCESS_CODE;

    }

}

public class TraceContext {

    public static String traceId() {

        String tid = MDC.get("tid");

        return StrUtil.isBlank(tid) ? "" : tid;

    }

}

```

#### Jackson 全局序列化配置

```java

@Bean

public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {

    return builder -> builder

        .serializerByType(Long.class, ToStringSerializer.instance)

        .serializerByType(Long.TYPE, ToStringSerializer.instance)

        .serializerByType(BigDecimal.class, ToStringSerializer.instance)

        .simpleDateFormat("yyyy-MM-dd HH:mm:ss");

}

```

---

### 6.4 全局异常处理（my-web-starter）

#### 核心原则

- 错误枚举里的 *`code` = 业务码（进 Result）**，**不是** HTTP Status

- 枚举同时声明 *`httpStatus`**，决定响应的 HTTP 状态

- 抛业务异常时：默认用枚举上的 HTTP；需要时可覆盖

- `message` 支持占位符；**推荐 `{key}` + Map**，同一份参数填充文案并进入 `data`

#### ErrorCode / 错误枚举

```java

public interface ErrorCode {

    int getCode();              // Result.code（业务码）

    String getMessage();

    default HttpStatus getHttpStatus() {

        return HttpStatus.BAD_REQUEST; // 业务异常默认 400

    }

}

/** 平台公共错误码 */

@Getter

@AllArgsConstructor

public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "success", HttpStatus.OK),

    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(40300, "无权限", HttpStatus.FORBIDDEN),

    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),

    TOO_MANY_REQUESTS(42900, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),

    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),

    SERVICE_UNAVAILABLE(50300, "下游服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;

    private final String message;

    private final HttpStatus httpStatus;

}

/** 业务域错误码（按模块分段，如订单 1xxxx） */

@Getter

@AllArgsConstructor

public enum OrderErrorCode implements ErrorCode {

    // message 支持 {key} 命名占位；也可用 {} 按序占位（见 ErrorMessageFormatter）

    STOCK_NOT_ENOUGH(10001,

        "商品{skuId}库存不足，当前可用{available}，需要{required}",

        HttpStatus.CONFLICT),

    ORDER_NOT_FOUND(10004, "订单不存在", HttpStatus.NOT_FOUND),

    ORDER_STATUS_INVALID(10009,

        "订单状态不允许此操作，当前状态={status}",

        HttpStatus.UNPROCESSABLE_ENTITY);

    private final int code;

    private final String message;

    private final HttpStatus httpStatus;

}

```

业务码分段建议`0` 成功 · `4xxxx` 平台通用客户端错误 · `5xxxx` 平台系统/依赖错误 · `1xxxx` 订单 · `2xxxx` 用户 …（各域自洽，禁止复用 HTTP 数字当业务码）。

#### 错误信息占位符填充

枚举里的 `message` 可写模板，抛异常时用参数填充；**同一份参数**既生成可读 `message`，也写入 `Result.data`（Map 场景）。

支持两种写法：

| 写法 | 模板示例 | 传参 | 结果 |

|---|---|---|---|

| 命名 `{key}`（推荐） | `商品{skuId}库存不足，可用{available}` | `Map.of("skuId", 1, "available", 2)` | `商品1库存不足，可用2` |

| 顺序 `{}`（类 SLF4J） | `商品{}库存不足，可用{}` | `args: 1, 2` | `商品1库存不足，可用2` |

```java

public final class ErrorMessageFormatter {

    private static final Pattern NAMED = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private ErrorMessageFormatter() {}

    /** 命名占位：{skuId} → params.get("skuId")；缺 key 则保留原占位 */

    public static String format(String template, Map<String, ?> params) {

        if (template == null || params == null || params.isEmpty()) {

            return template;

        }

        Matcher m = NAMED.matcher(template);

        StringBuffer sb = new StringBuffer();

        while (m.find()) {

            Object val = params.get([m.group](http://m.group)(1));

            m.appendReplacement(sb,

                Matcher.quoteReplacement(val == null ? [m.group](http://m.group)(0) : String.valueOf(val)));

        }

        m.appendTail(sb);

        return sb.toString();

    }

    /** 顺序占位：与 SLF4J 类似，按出现顺序消费 args */

    public static String format(String template, Object... args) {

        if (template == null || args == null || args.length == 0) {

            return template;

        }

        StringBuilder sb = new StringBuilder(template.length() + 16);

        int argIdx = 0;

        for (int i = 0; i < template.length(); i++) {

            char c = template.charAt(i);

            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {

                Object val = argIdx < args.length ? args[argIdx++] : "{}";

                sb.append(val);

                i++; // skip '}'

            } else {

                sb.append(c);

            }

        }

        return sb.toString();

    }

}

```

#### 错误附加数据：用 `Object`，不用单独 `Map` 字段

| 方案 | 结论 |

|---|---|

| 新增 `Map<String, Object> extra` | **不推荐**作唯一类型：校验错误常要 `List`，复杂上下文更适合 VO；前端还要记两个字段 |

| 复用 `Result.data`（运行时可为 Object） | **推荐**：成功/失败结构一致；Map、VO、List 都能塞 |

| BizException 持有 `Object data` | 与 Result 对齐；Map 场景同时用于填充 message |

```java

@Getter

public class BizException extends RuntimeException {

    private final int code;

    private final String message;   // 已填充后的最终文案

    private final HttpStatus httpStatus;

    /** 给前端的错误上下文：Map / VO / List，可为 null */

    private final Object data;

    public BizException(ErrorCode errorCode) {

        this(errorCode.getCode(), errorCode.getMessage(),

            errorCode.getHttpStatus(), null);

    }

    public BizException(ErrorCode errorCode, Object data) {

        this(errorCode.getCode(),

            resolveMessage(errorCode.getMessage(), data),

            errorCode.getHttpStatus(), data);

    }

    public BizException(ErrorCode errorCode, HttpStatus httpStatus, Object data) {

        this(errorCode.getCode(),

            resolveMessage(errorCode.getMessage(), data),

            httpStatus, data);

    }

    public BizException(int code, String message,

                        HttpStatus httpStatus, Object data) {

        super(message);

        this.code = code;

        this.message = message;

        this.httpStatus = httpStatus == null ? HttpStatus.BAD_REQUEST : httpStatus;

        [this.data](http://this.data) = data;

    }

    /** 便捷构造：默认 HTTP 400，无附加 data */

    public BizException(int code, String message) {

        this(code, message, HttpStatus.BAD_REQUEST, null);

    }

    /** 便捷构造：指定 HTTP，无附加 data */

    public BizException(int code, String message, HttpStatus httpStatus) {

        this(code, message, httpStatus, null);

    }

    /** 命名占位 + 参数进 data（推荐） */

    public static BizException of(ErrorCode errorCode, Map<String, ?> params) {

        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), params);

        return new BizException(

            errorCode.getCode(), msg, errorCode.getHttpStatus(), params);

    }

    /** 顺序占位 {}；args 仅用于填文案，默认不进 data（需要给前端时用 of(Map)） */

    public static BizException of(ErrorCode errorCode, Object... args) {

        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), args);

        return new BizException(

            errorCode.getCode(), msg, errorCode.getHttpStatus(), null);

    }

    /** 顺序占位，同时把 args 按 arg0/arg1... 放进 data（少用，优先命名 Map） */

    public static BizException ofWithArgs(ErrorCode errorCode, Object... args) {

        String msg = ErrorMessageFormatter.format(errorCode.getMessage(), args);

        Map<String, Object> data = new LinkedHashMap<>();

        for (int i = 0; i < args.length; i++) {

            data.put("arg" + i, args[i]);

        }

        return new BizException(

            errorCode.getCode(), msg, errorCode.getHttpStatus(), data);

    }

    @SuppressWarnings("unchecked")

    private static String resolveMessage(String template, Object data) {

        if (data instanceof Map<?, ?> map) {

            return ErrorMessageFormatter.format(template, (Map<String, ?>) map);

        }

        return template;

    }

}

```

使用示例：

```java

// 无附加数据

throw new BizException(OrderErrorCode.ORDER_NOT_FOUND);

// 命名占位（推荐）：message 填充 + data 原样返回前端

throw BizException.of(OrderErrorCode.STOCK_NOT_ENOUGH, Map.of(

    "skuId", skuId,

    "available", stock,

    "required", reqQty

));

// → message: "商品10086库存不足，当前可用2，需要5"

// → data:    { skuId, available, required }

// 顺序占位

throw BizException.of(

    // 枚举 message 需写成：商品{}库存不足，当前可用{}，需要{}

    OrderErrorCode.STOCK_NOT_ENOUGH_ORDERED,

    skuId, stock, reqQty

);

// VO：结构稳定；若 VO 字段名与 {key} 不一致，请先转 Map 或手写 message

throw new BizException(OrderErrorCode.STOCK_NOT_ENOUGH,

    new StockShortageVO(skuId, stock, reqQty));

```

#### 全局异常处理器（ResponseEntity 写 HTTP Status）

```java

@RestControllerAdvice

@Slf4j

public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)

    public ResponseEntity<Result<Object>> handleBizException(BizException e) {

        log.warn("业务异常：http={}, code={}, message={}",

            e.getHttpStatus().value(), e.getCode(), e.getMessage());

        return ResponseEntity

            .status(e.getHttpStatus())

            .body([Result.fail](http://Result.fail)(e.getCode(), e.getMessage(), e.getData()));

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<Result<Object>> handleValidException(

            MethodArgumentNotValidException e) {

        List<Map<String, String>> fields = e.getBindingResult().getFieldErrors()

            .stream()

            .map(err -> Map.of(

                "field", err.getField(),

                "message", StrUtil.nullToDefault(err.getDefaultMessage(), "不合法")))

            .toList();

        String message = [fields.stream](http://fields.stream)()

            .map(m -> m.get("message"))

            .collect(Collectors.joining("; "));

        return ResponseEntity

            .status(HttpStatus.BAD_REQUEST)

            .body([Result.fail](http://Result.fail)(

                CommonErrorCode.BAD_REQUEST.getCode(), message, fields));

    }

    @ExceptionHandler(ConstraintViolationException.class)

    public ResponseEntity<Result<Object>> handleConstraintViolation(

            ConstraintViolationException e) {

        List<Map<String, String>> fields = e.getConstraintViolations().stream()

            .map(v -> Map.of(

                "field", v.getPropertyPath().toString(),

                "message", v.getMessage()))

            .toList();

        String message = [fields.stream](http://fields.stream)()

            .map(m -> m.get("message"))

            .collect(Collectors.joining("; "));

        return ResponseEntity

            .status(HttpStatus.BAD_REQUEST)

            .body([Result.fail](http://Result.fail)(

                CommonErrorCode.BAD_REQUEST.getCode(), message, fields));

    }

    @ExceptionHandler(Exception.class)

    public ResponseEntity<Result<Object>> handleException(Exception e) {

        log.error("系统异常", e);

        return ResponseEntity

            .status(HttpStatus.INTERNAL_SERVER_ERROR)

            .body([Result.fail](http://Result.fail)(

                CommonErrorCode.SYSTEM_ERROR.getCode(),

                CommonErrorCode.SYSTEM_ERROR.getMessage()));

    }

}

```

响应示例：

```http

HTTP/1.1 409 Conflict

{

  "code": 10001,

  "message": "商品10086库存不足，当前可用2，需要5",

  "data": { "skuId": "10086", "available": 2, "required": 5 },

  "traceId": "xxx",

  "timestamp": 1730000000000

}

```

#### 规范

- 失败时 `data` 可选；没有上下文就 `null`，不要塞空 Map

- 只放**前端渲染需要**的字段，禁止堆栈、SQL、内部密钥

- 模板优先用 *`{key}` 命名占位**，与 `Map` 参数、前端 `data` 三方对齐

- 缺 key 时保留 `{key}` 原文并打 warn，避免静默吃掉配置错误

- 跨服务：Feign `ErrorDecoder` 透传已填充的 `message` + `data`（下游填好即可，上游不再二次 format）

- 文档：稳定结构优先 VO；要占位填充时用 `Map` + `{key}`

---

### 6.5 SpringDoc + Knife4j（my-web-starter）

```java

@AutoConfiguration

@ConditionalOnProperty(prefix = "springdoc", name = "enabled",

    havingValue = "true", matchIfMissing = true)

@EnableConfigurationProperties(DocProperties.class)

public class DocAutoConfiguration {

    @Bean

    @ConditionalOnMissingBean

    public OpenAPI openAPI(DocProperties props, Environment env) {

        // 自动读取服务名称

        String appName = env.getProperty("[spring.application.name](http://spring.application.name)", "API 文档");

        return new OpenAPI()

            .info(new Info()

                .title(StrUtil.isBlank(props.getTitle()) ? appName : props.getTitle())

                .description(props.getDescription())

                .version(props.getVersion())

                .contact(new Contact().name(props.getContactName()))

            );

    }

    @Bean

    public GroupedOpenApi defaultApi() {

        return GroupedOpenApi.builder()

            .group("default")

            .pathsToMatch("/api/**")

            .build();

    }

}

@ConfigurationProperties(prefix = "springdoc")

@Data

public class DocProperties {

    private boolean enabled     = true;

    private String  title;           // 不配则默认读取 [spring.application.name](http://spring.application.name)

    private String  description;

    private String  version         = "1.0.0";

    private String  contactName;

}

```

```yaml

# 业务服务配置

springdoc:

  enabled: true          # 生产环境设为 false 关闭文档

  version: 1.0.0

  contact-name: 研发团队

```

---

### 6.6 分布式锁（my-redis-starter）

#### 注解定义

```java

@Target(ElementType.METHOD)

@Retention(RetentionPolicy.RUNTIME)

@Documented

public @interface DistributedLock {

    // SpEL 表达式，支持多参数拼接，如 {"#orderId", "#userId"}

    String[] keys() default {};

    // key 前缀，不填则默认 类名:方法名

    String prefix() default "";

    // 等待获取锁超时（默认 3s）

    long waitTime() default 3;

    // 持锁时间（-1 = watchdog 自动续期）

    long leaseTime() default -1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 是否公平锁

    boolean fair() default false;

    // 获取锁失败提示

    String message() default "操作频繁，请稍后重试";

    // 自定义异常类型（需有 String 构造器）

    Class<? extends RuntimeException> exception() default LockException.class;

}

```

#### AOP 实现

```java

@Aspect

@Component

@RequiredArgsConstructor

public class DistributedLockAspect {

    private final RedissonClient        redissonClient;

    private final SpelExpressionParser  parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")

    public Object around(ProceedingJoinPoint pjp,

                         DistributedLock distributedLock) throws Throwable {

        String lockKey = buildKey(pjp, distributedLock);

        RLock lock = distributedLock.fair()

            ? redissonClient.getFairLock(lockKey)

            : redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(

            distributedLock.waitTime(),

            distributedLock.leaseTime(),

            distributedLock.timeUnit()

        );

        if (!acquired) {

            throw distributedLock.exception()

                .getConstructor(String.class)

                .newInstance(distributedLock.message());

        }

        try {

            return pjp.proceed();

        } finally {

            if (lock.isHeldByCurrentThread()) {

                lock.unlock();

            }

        }

    }

    private String buildKey(ProceedingJoinPoint pjp, DistributedLock lock) {

        MethodSignature  sig        = (MethodSignature) pjp.getSignature();

        String[]         paramNames = sig.getParameterNames();

        Object[]         args       = pjp.getArgs();

        EvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < paramNames.length; i++) {

            context.setVariable(paramNames[i], args[i]);

        }

        String keySuffix = [Arrays.stream](http://Arrays.stream)(lock.keys())

            .map(spel -> parser.parseExpression(spel).getValue(context, String.class))

            .collect(Collectors.joining(":"));

        String prefix = StrUtil.isBlank(lock.prefix())

            ? sig.getDeclaringType().getSimpleName() + ":" + sig.getName()

            : lock.prefix();

        return "lock:" + prefix + ":" + keySuffix;

    }

}

```

#### LockTemplate（编程式，适合动态 key 场景）

```java

@Component

@RequiredArgsConstructor

public class LockTemplate {

    private final RedissonClient redissonClient;

    public <T> T execute(String lockKey, long waitTime,

                         long leaseTime, Supplier<T> supplier) {

        RLock lock = redissonClient.getLock(lockKey);

        try {

            if (!lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {

                throw new LockException("获取锁失败：" + lockKey);

            }

            return supplier.get();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new LockException("获取锁被中断");

        } finally {

            if (lock.isHeldByCurrentThread()) lock.unlock();

        }

    }

    public void execute(String lockKey, Runnable runnable) {

        execute(lockKey, 3, -1, () -> { [runnable.run](http://runnable.run)(); return null; });

    }

}

```

#### 使用示例

```java

// 注解方式

@DistributedLock(

    keys      = {"#orderId", "#userId"},

    prefix    = "order:create",

    waitTime  = 5,

    fair      = true,

    message   = "订单正在处理中，请勿重复提交",

    exception = BizException.class

)

public void createOrder(Long orderId, Long userId) { ... }

// 模板方式

lockTemplate.execute("stock:deduct:" + skuId, () -> {

    // 扣减库存逻辑

});

```

---

### 6.7 接口限流（my-redis-starter）

基于 Redis 滑动窗口，防止接口被高频打爆。

#### 注解定义

```java

@Target(ElementType.METHOD)

@Retention(RetentionPolicy.RUNTIME)

public @interface RateLimit {

    // SpEL 指定 key，不填则默认 IP + 接口路径

    String key() default "";

    // 时间窗口内最大请求次数

    long  limit() default 100;

    // 时间窗口大小

    long  window() default 1;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    String message() default "请求过于频繁，请稍后再试";

}

```

#### AOP + Lua 脚本实现

```java

@Aspect

@Component

@RequiredArgsConstructor

public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    // 滑动窗口 Lua 脚本

    // 注意：member 必须唯一，否则同一毫秒多个请求会因 member 相同而互相覆盖，导致计数失真、限流失效。

    //       这里用 ARGV[4] 传入「毫秒时间戳 + 唯一后缀（UUID/自增）」作为 member，score 仍用毫秒时间戳。

    private static final String SLIDING_WINDOW_SCRIPT = """

        local key = KEYS[1]

        local now = tonumber(ARGV[1])

        local window = tonumber(ARGV[2])

        local limit = tonumber(ARGV[3])

        local member = ARGV[4]

        [redis.call](http://redis.call)('ZREMRANGEBYSCORE', key, 0, now - window)

        local count = [redis.call](http://redis.call)('ZCARD', key)

        if count < limit then

            [redis.call](http://redis.call)('ZADD', key, now, member)

            [redis.call](http://redis.call)('PEXPIRE', key, window)

            return 1

        end

        return 0

        """;

    private final DefaultRedisScript<Long> script =

        new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, Long.class);

    @Around("@annotation(rateLimit)")

    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {

        String key        = buildKey(pjp, rateLimit);

        long   now        = System.currentTimeMillis();

        long   windowMs   = rateLimit.timeUnit().toMillis(rateLimit.window());

        // 唯一 member，避免同毫秒并发覆盖

        String member     = now + ":" + ThreadLocalRandom.current().nextLong();

        Long result = redisTemplate.execute(

            script,

            List.of(key),

            String.valueOf(now),

            String.valueOf(windowMs),

            String.valueOf(rateLimit.limit()),

            member

        );

        if (result == null || result == 0) {

            // 使用平台通用错误码，禁止把 HTTP 数字（429）直接当业务码

            throw new BizException(

                CommonErrorCode.TOO_MANY_REQUESTS.getCode(),

                rateLimit.message(),

                HttpStatus.TOO_MANY_REQUESTS);

        }

        return pjp.proceed();

    }

    private String buildKey(ProceedingJoinPoint pjp, RateLimit rateLimit) {

        if (StrUtil.isNotBlank(rateLimit.key())) {

            return "rate:" + rateLimit.key();

        }

        // 默认以 IP + 接口为 key

        HttpServletRequest request = ((ServletRequestAttributes)

            RequestContextHolder.getRequestAttributes()).getRequest();

        return "rate:" + getClientIp(request) + ":"

            + pjp.getSignature().getDeclaringTypeName() + "."

            + pjp.getSignature().getName();

    }

}

```

#### 使用示例

```java

// 每分钟最多 10 次

@RateLimit(limit = 10, window = 1, timeUnit = TimeUnit.MINUTES, message = "操作过于频繁")

@PostMapping("/order")

public Result<Void> createOrder(@RequestBody @Validated OrderCreateReq req) { ... }

```

---

### 6.8 接口幂等（my-idempotent-starter）

#### 两态语义：处理中 / 已完成

单纯「占位 + 失败删除」只能防重复提交，无法应对「首个请求还在执行、后到请求想拿结果」的场景。规范采用**两态**：

- **PENDING（处理中）**：首个请求抢占成功后写入，短 TTL；重复请求命中 PENDING → 返回「处理中，请勿重复提交」

- **DONE（已完成）**：业务成功后回写结果并延长 TTL；重复请求命中 DONE → **直接返回首次结果**（可选，需方法有返回值可缓存）

- **执行失败**：删除 key，允许后续重试

```java

@Target(ElementType.METHOD)

@Retention(RetentionPolicy.RUNTIME)

public @interface Idempotent {

    // SpEL 指定 key，不填则取请求 Header 中的 Idempotent-Token

    String key() default "";

    // 处理中占位的 TTL（防止实例宕机后 key 永久占用）

    long expire() default 60;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 是否缓存并返回首次执行结果（true 时命中 DONE 直接返回，天然幂等）

    boolean cacheResult() default false;

    // 结果缓存 TTL（秒），cacheResult=true 时生效

    long resultExpire() default 300;

    String message() default "请勿重复提交";

}

```

```java

@Aspect

@Component

@RequiredArgsConstructor

public class IdempotentAspect {

    private static final String PENDING = "PENDING";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper        objectMapper;

    @Around("@annotation(idempotent)")

    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {

        String key = buildKey(pjp, idempotent);

        Boolean acquired = redisTemplate.opsForValue()

            .setIfAbsent(key, PENDING, idempotent.expire(), idempotent.timeUnit());

        if (Boolean.FALSE.equals(acquired)) {

            String cached = redisTemplate.opsForValue().get(key);

            // 命中已完成结果 → 直接返回，保证幂等

            if (idempotent.cacheResult() && cached != null && !PENDING.equals(cached)) {

                Class<?> returnType = ((MethodSignature) pjp.getSignature())

                    .getReturnType();

                return objectMapper.readValue(cached, returnType);

            }

            // 仍在处理中或未开启结果缓存 → 拒绝重复提交

            throw new BizException(

                CommonErrorCode.BAD_REQUEST.getCode(), idempotent.message());

        }

        try {

            Object result = pjp.proceed();

            if (idempotent.cacheResult() && result != null) {

                redisTemplate.opsForValue().set(key,

                    objectMapper.writeValueAsString(result),

                    idempotent.resultExpire(), TimeUnit.SECONDS);

            } else {

                redisTemplate.delete(key); // 不缓存结果则执行完立即释放

            }

            return result;

        } catch (Exception e) {

            redisTemplate.delete(key); // 执行失败，释放 key 允许重试

            throw e;

        }

    }

    private String buildKey(ProceedingJoinPoint pjp, Idempotent idempotent) {

        if (StrUtil.isNotBlank(idempotent.key())) {

            // SpEL 解析

            return "idempotent:" + resolveSpel(pjp, idempotent.key());

        }

        // 从 Header 取客户端传入的 token

        HttpServletRequest request = ((ServletRequestAttributes)

            RequestContextHolder.getRequestAttributes()).getRequest();

        String token = request.getHeader("Idempotent-Token");

        if (StrUtil.isBlank(token)) {

            throw new BizException(

                CommonErrorCode.BAD_REQUEST.getCode(), "幂等 Token 不能为空");

        }

        return "idempotent:" + token;

    }

}

```

> `cacheResult=true` 适合「创建订单」这类希望重放拿到同一结果的场景；对「仅防重复点击」的写操作用默认 `false` 即可。

---

### 6.9 分布式 ID（my-id-starter）

#### WorkerId 自动分配（Redis）

> **可靠性关键**：workerId 必须唯一且在实例存活期间不被回收，否则会产生重复 ID。因此需要：① 用 Redis Set 记录「已占用」的 workerId，分配时跳过被占用值，避免 `seq % 1024` 回绕撞号；② 分配后由定时任务持续续约心跳，实例存活期间 key 永不过期；③ 心跳过期（实例宕机）后该 workerId 才可被回收再分配。

```java

@Component

@RequiredArgsConstructor

@Slf4j

public class WorkerIdAssigner {

    private final StringRedisTemplate redisTemplate;

    private final Environment         environment;

    private static final String USED_SET_KEY     = "id:worker:used";     // 已占用 workerId 集合

    private static final String HEARTBEAT_PREFIX  = "id:worker:hb:";      // 每个 workerId 的心跳

    private static final String INSTANCE_PREFIX   = "id:worker:instance:";// ip:port → workerId 反查

    private static final long   MAX_WORKER_ID     = 1023L;

    private static final long   HEARTBEAT_TTL_SEC = 60L;

    @Getter

    private volatile long workerId = -1L;

    private String instanceKey;

    @PostConstruct

    public void init() {

        this.instanceKey = getInstanceKey();

        this.workerId    = assignWorkerId();

        [log.info](http://log.info)("分配 workerId={} for instance={}", workerId, instanceKey);

    }

    private long assignWorkerId() {

        // 1. 服务重启：复用原有 workerId（心跳未过期时）

        String reuse = redisTemplate.opsForValue().get(INSTANCE_PREFIX + instanceKey);

        if (reuse != null && Boolean.TRUE.equals(

                redisTemplate.hasKey(HEARTBEAT_PREFIX + reuse))) {

            heartbeat(Long.parseLong(reuse));

            return Long.parseLong(reuse);

        }

        // 2. 新实例：找一个未被占用的槽位（心跳存在=占用中）

        for (long id = 0; id <= MAX_WORKER_ID; id++) {

            if (Boolean.FALSE.equals(redisTemplate.hasKey(HEARTBEAT_PREFIX + id))) {

                Boolean ok = redisTemplate.opsForValue().setIfAbsent(

                    HEARTBEAT_PREFIX + id, instanceKey, HEARTBEAT_TTL_SEC, TimeUnit.SECONDS);

                if (Boolean.TRUE.equals(ok)) {

                    redisTemplate.opsForSet().add(USED_SET_KEY, String.valueOf(id));

                    redisTemplate.opsForValue().set(

                        INSTANCE_PREFIX + instanceKey, String.valueOf(id));

                    return id;

                }

            }

        }

        throw new IdGenerateException("workerId 已耗尽（0~1023 全部占用）");

    }

    /** 定时续约：实例存活期间持续刷新心跳，防止 workerId 被回收 */

    @Scheduled(fixedRate = 20_000) // 20s 续约一次，TTL 60s，容忍两次失败

    public void renew() {

        if (workerId >= 0) {

            heartbeat(workerId);

        }

    }

    private void heartbeat(long id) {

        redisTemplate.opsForValue().set(

            HEARTBEAT_PREFIX + id, instanceKey, HEARTBEAT_TTL_SEC, TimeUnit.SECONDS);

    }

    /** 优雅停机时主动释放，加速槽位回收 */

    @PreDestroy

    public void release() {

        if (workerId >= 0) {

            redisTemplate.delete(HEARTBEAT_PREFIX + workerId);

            redisTemplate.opsForSet().remove(USED_SET_KEY, String.valueOf(workerId));

        }

    }

    private String getInstanceKey() {

        String port = environment.getProperty("server.port", "8080");

        try {

            return InetAddress.getLocalHost().getHostAddress() + ":" + port;

        } catch (UnknownHostException e) {

            return UUID.randomUUID().toString();

        }

    }

}

```

> 需在启动类开启 `@EnableScheduling`。若对 Redis 可用性要求极高，可改用 Nacos/Zookeeper 临时节点或 Statefulset 序号`POD_NAME` 末位）分配 workerId。

#### Snowflake 核心算法

```java

public class SnowflakeIdGenerator {

    private static final long EPOCH          = 1704067200000L; // 2024-01-01 00:00:00 UTC

    private static final long WORKER_BITS    = 10L;

    private static final long SEQUENCE_BITS  = 12L;

    private static final long MAX_WORKER_ID  = ~(-1L << WORKER_BITS);   // 1023

    private static final long MAX_SEQUENCE   = ~(-1L << SEQUENCE_BITS); // 4095

    private static final long WORKER_SHIFT   = SEQUENCE_BITS;

    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS;

    private final long workerId;

    // 用 ReentrantLock 而非 synchronized：JDK 21 下 synchronized 会 pin 虚拟线程；

    // JDK 24+（JEP 491）虽已解除 pinning，但 Lock 语义更清晰、可控，统一采用。

    private final ReentrantLock lock = new ReentrantLock();

    private long sequence    = 0L;

    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {

            throw new IllegalArgumentException("workerId 超出范围: " + workerId);

        }

        this.workerId = workerId;

    }

    public long nextId() {

        lock.lock();

        try {

            return doNextId();

        } finally {

            lock.unlock();

        }

    }

    private long doNextId() {

        long current = System.currentTimeMillis();

        if (current < lastTimestamp) {

            long offset = lastTimestamp - current;

            if (offset <= 5) {

                // 回拨 5ms 内等待

                try { Thread.sleep(offset << 1); }

                catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                current = System.currentTimeMillis();

            } else {

                throw new IdGenerateException("时钟回拨过大，差值=" + offset + "ms");

            }

        }

        if (current == lastTimestamp) {

            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {

                current = waitNextMillis(lastTimestamp);

            }

        } else {

            sequence = 0L;

        }

        lastTimestamp = current;

        return ((current - EPOCH) << TIMESTAMP_SHIFT)

            | (workerId << WORKER_SHIFT)

            | sequence;

    }

    private long waitNextMillis(long last) {

        long ts = System.currentTimeMillis();

        while (ts <= last) ts = System.currentTimeMillis();

        return ts;

    }

}

```

#### 对外工具类

```java

@Component

@RequiredArgsConstructor

public class IdHelper {

    private final SnowflakeIdGenerator generator;

    public long   nextId()    { return generator.nextId(); }

    // 前端用 String，避免 JS 精度丢失

    public String nextIdStr() { return String.valueOf(generator.nextId()); }

}

```

#### MyBatis-Plus 集成

```java

// 自动配置注册 IdentifierGenerator，实体使用 @TableId(type = IdType.ASSIGN_ID) 自动生效

@Bean

@ConditionalOnMissingBean

public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator generator) {

    return entity -> generator.nextId();

}

```

---

### 6.10 RabbitMQ 可靠性（my-rabbit-starter）

#### 核心配置

```yaml

spring:

  rabbitmq:

    publisher-confirm-type: correlated   # 生产者确认

    publisher-returns: true              # 路由失败回调

    template:

      mandatory: true

    listener:

      simple:

        acknowledge-mode: manual         # 手动 ACK

        prefetch: 10                     # 流量控制

        retry:

          enabled: true

          max-attempts: 3

          initial-interval: 2000ms

          multiplier: 2.0

          max-interval: 10000ms

```

#### 死信队列配置

```java

@Configuration

public class RabbitDeadLetterConfig {

    // 业务队列绑定死信交换机

    @Bean

    public Queue orderQueue() {

        return QueueBuilder.durable("order.queue")

            .withArgument("x-dead-letter-exchange", "[dlx.exchange](http://dlx.exchange)")

            .withArgument("x-dead-letter-routing-key", "dlx.order")

            .build();

    }

    @Bean

    public DirectExchange dlxExchange() {

        return new DirectExchange("[dlx.exchange](http://dlx.exchange)");

    }

    @Bean

    public Queue dlxOrderQueue() {

        return QueueBuilder.durable("dlx.order.queue").build();

    }

    @Bean

    public Binding dlxBinding() {

        return BindingBuilder.bind(dlxOrderQueue())

            .to(dlxExchange()).with("dlx.order");

    }

}

```

#### 消费者规范

```java

@RabbitListener(queues = "order.queue")

public void consume(Message message, Channel channel) throws IOException {

    long deliveryTag = message.getMessageProperties().getDeliveryTag();

    try {

        doProcess(message);

        channel.basicAck(deliveryTag, false);

    } catch (BizException e) {

        // 业务异常：不重试，进死信队列

        log.warn("消费业务异常，消息进死信队列", e);

        channel.basicNack(deliveryTag, false, false);

    } catch (Exception e) {

        // 系统异常：requeue 重试

        log.error("消费系统异常，消息重新入队", e);

        channel.basicNack(deliveryTag, false, true);

    }

}

```

---

### 6.11 MyBatis-Plus（my-mybatis-starter）

#### 插件配置

```java

@Bean

public MybatisPlusInterceptor mybatisPlusInterceptor() {

    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

    // 分页插件

    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

    // 乐观锁插件（实体字段加 @Version 注解）

    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

    // 防止全表更新/删除（生产环境安全保障）

    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

    return interceptor;

}

```

#### 自动填充

```java

@Component

public class MetaObjectFillHandler implements MetaObjectHandler {

    @Override

    public void insertFill(MetaObject metaObject) {

        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);

        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        this.strictInsertFill(metaObject, "createBy",

            () -> Optional.ofNullable(UserContext.get())

                .map(UserInfo::getUserId).orElse("system"), String.class);

        this.strictInsertFill(metaObject, "updateBy",

            () -> Optional.ofNullable(UserContext.get())

                .map(UserInfo::getUserId).orElse("system"), String.class);

    }

    @Override

    public void updateFill(MetaObject metaObject) {

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        this.strictUpdateFill(metaObject, "updateBy",

            () -> Optional.ofNullable(UserContext.get())

                .map(UserInfo::getUserId).orElse("system"), String.class);

    }

}

```

#### 实体基类

```java

@Data

public abstract class BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)  // 使用自研 Snowflake ID

    private Long id;

    @TableField(fill = FieldFill.INSERT)

    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)

    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)

    private String updateBy;

    @Version

    @TableField(fill = FieldFill.INSERT)

    private Integer version;   // 乐观锁版本号

    @TableLogic

    private Integer deleted;   // 逻辑删除（0正常 1删除）

}

```

---

### 6.12 Redis 规范（my-redis-starter）

#### key 命名规范

```

格式：{appName}:{业务模块}:{具体key}

示例：biz-order:stock:sku_10086

      biz-user:session:uid_10001

```

#### 序列化配置

```java

@Bean

public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

    RedisTemplate<String, Object> template = new RedisTemplate<>();

    template.setConnectionFactory(factory);

    // key 使用 String 序列化

    StringRedisSerializer stringSerializer = new StringRedisSerializer();

    template.setKeySerializer(stringSerializer);

    template.setHashKeySerializer(stringSerializer);

    // value 使用 Jackson 序列化

    GenericJackson2JsonRedisSerializer jsonSerializer =

        new GenericJackson2JsonRedisSerializer();

    template.setValueSerializer(jsonSerializer);

    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();

    return template;

}

```

#### key 工具类（统一加前缀）

```java

@Component

public class RedisKeyHelper {

    @Value("${[spring.application.name](http://spring.application.name)}")

    private String appName;

    public String buildKey(String module, String key) {

        return appName + ":" + module + ":" + key;

    }

}

```

---

### 6.13 Feign 熔断与错误传递

跨服务约定与对外 API 一致：**成功 HTTP 200 + `Result.code=0`；失败非 2xx + body 仍为 `Result`（含业务 code/message/traceId）**。调用方通过 `ErrorDecoder` 还原为 `BizException`，避免吞掉下游真实错误。

#### 传递链路

```

下游抛 BizException

  → GlobalExceptionHandler 写出 HTTP 4xx/5xx + Result(code,message,traceId)

    → 上游 Feign ErrorDecoder 解析 body

      → 再抛 BizException(code, message, httpStatus)

        → 上游 Advice 再次按同样规则写给更上层 / 前端

```

熔断/超时/连接失败属于**基础设施故障**，走 Fallback，使用 `SERVICE_UNAVAILABLE`，**不得**用 Fallback 覆盖下游已返回的业务错误。

#### ErrorDecoder（透传业务错误）

```java

@Slf4j

@RequiredArgsConstructor

public class FeignErrorDecoder implements ErrorDecoder {

    // 复用 Spring 容器统一配置的 ObjectMapper，避免每次 new 及配置不一致

    private final ObjectMapper objectMapper;

    private final ErrorDecoder defaultDecoder = new Default();

    @Override

    public Exception decode(String methodKey, Response response) {

        // 2xx 不会进 ErrorDecoder；此处处理 4xx/5xx

        if (response.body() == null) {

            return new BizException(

                CommonErrorCode.SERVICE_UNAVAILABLE.getCode(),

                "下游无响应体: " + methodKey,

                HttpStatus.valueOf(response.status()),

                null);

        }

        try {

            Result<?> result = objectMapper.readValue(

                response.body().asInputStream(), Result.class);

            HttpStatus status = HttpStatus.resolve(response.status());

            if (status == null) {

                status = HttpStatus.INTERNAL_SERVER_ERROR;

            }

            int code = result.getCode() == 0

                ? CommonErrorCode.SYSTEM_ERROR.getCode()

                : result.getCode();

            String message = StrUtil.blankToDefault(

                result.getMessage(), "下游调用失败");

            // data 一并透传（可能是 Map / List / 对象）

            return new BizException(code, message, status, result.getData());

        } catch (IOException e) {

            log.error("解析 Feign 错误体失败, method={}", methodKey, e);

            return defaultDecoder.decode(methodKey, response);

        }

    }

}

```

```java

@AutoConfiguration

public class FeignAutoConfiguration {

    @Bean

    @ConditionalOnMissingBean

    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {

        return new FeignErrorDecoder(objectMapper);

    }

}

```

#### 调用方用法

```java

@FeignClient(

    name            = "biz-service",

    fallbackFactory = BizServiceFallbackFactory.class

)

public interface BizServiceFeignClient {

    @GetMapping("/internal/order/{id}")

    Result<OrderDTO> getOrder(@PathVariable Long id);

}

// Service 中：业务失败会以 BizException 抛出，无需再判 Result.code

public OrderDTO getOrder(Long id) {

    Result<OrderDTO> result = bizServiceFeignClient.getOrder(id);

    // 仅成功（HTTP 200）会走到这里；仍建议防御性校验

    if (result == null || !result.isSuccess()) {

        throw new BizException(CommonErrorCode.SYSTEM_ERROR);

    }

    return result.getData();

}

```

> 可选增强：自定义 `Decoder` 在 HTTP 200 时直接解包 `data`，Feign 方法签名改为 `OrderDTO getOrder(...)`；业务失败仍走 `ErrorDecoder`。

#### Fallback：仅基础设施降级

```java

@Component

@Slf4j

public class BizServiceFallbackFactory implements FallbackFactory<BizServiceFeignClient> {

    @Override

    public BizServiceFeignClient create(Throwable cause) {

        // cause 若已是 BizException，说明是下游业务错误被错误地进了 Fallback——应直接抛出

        if (cause instanceof BizException bizEx) {

            return id -> { throw bizEx; };

        }

        return id -> {

            log.error("Feign 基础设施降级，orderId={}", id, cause);

            throw new BizException(CommonErrorCode.SERVICE_UNAVAILABLE);

        };

    }

}

```

#### Resilience4j 配置

```yaml

spring:

  cloud:

    openfeign:

      circuitbreaker:

        enabled: true

resilience4j:

  circuitbreaker:

    configs:

      default:

        failure-rate-threshold: 50

        slow-call-rate-threshold: 80

        slow-call-duration-threshold: 2s

        wait-duration-in-open-state: 10s

        sliding-window-size: 10

        permitted-number-of-calls-in-half-open-state: 5

        # 业务 4xx 默认可不计入失败（按需忽略），避免业务校验打满熔断

        ignore-exceptions:

          - [com.company](http://com.company).common.exception.BizException

  timelimiter:

    configs:

      default:

        timeout-duration: 3s

```

#### 规范小结

| 场景 | HTTP | Result.code | 上游行为 |

|---|---|---|---|

| 成功 | 200 | 0 | 取 `data` |

| 业务失败（库存不足等） | 4xx（如 409） | 业务码（如 10001） | `ErrorDecoder` → `BizException` 原样透传 |

| 未登录 / 无权限 | 401 / 403 | 40100 / 40300 | 同上 |

| 下游系统异常 | 500 | 50000 | 同上；对外可再包装文案 |

| 超时 / 熔断 / 连接失败 | — | 50300 | Fallback → `SERVICE_UNAVAILABLE` |

> Feign 入参/出参打印见 [6.20](#620-接口与-feign-调用日志my-log-starter)`my.log.feign` + `@FeignLog`，优先级方法 > 类 > 全局。

---

### 6.14 链路追踪（SkyWalking）

#### Java Agent 启动配置

```bash

java -javaagent:/opt/skywalking-agent/skywalking-agent.jar \

     -Dskywalking.agent.service_name=${SPRING_APPLICATION_NAME} \

     -Dskywalking.collector.backend_service=skywalking-oap:11800 \

     -jar app.jar

```

#### 日志集成（logback-spring.xml）

```xml

<dependency>

    <groupId>org.apache.skywalking</groupId>

    <artifactId>apm-toolkit-logback-1.x</artifactId>

</dependency>

```

```xml

<!-- logback-spring.xml -->

<pattern>%d{yyyy-MM-dd HH:mm:ss} [%tid] [%thread] %-5level %logger{36} - %msg%n</pattern>

<!--                                     ↑ SkyWalking TraceId 占位符 -->

```

> SkyWalking Agent 自动支持 Spring MVC、Feign、RabbitMQ、MySQL、Redis 的链路追踪，无需代码侵入`%tid` 会自动打印当前请求的 TraceId。

---

### 6.15 配置加密（Jasypt）

```xml

<dependency>

    <groupId>com.github.ulisesbocchio</groupId>

    <artifactId>jasypt-spring-boot-starter</artifactId>

    <version>3.0.5</version>

</dependency>

```

```yaml

jasypt:

  encryptor:

    password: ${JASYPT_PASSWORD}          # 从环境变量注入，不进代码仓库

    algorithm: PBEWITHHMACSHA512ANDAES_256

spring:

  datasource:

    password: ENC(加密后的密文)

  redis:

    password: ENC(加密后的密文)

  rabbitmq:

    password: ENC(加密后的密文)

```

加密命令：

```bash

java -cp jasypt.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \

  input="明文密码" \

  password="JASYPT_PASSWORD" \

  algorithm=PBEWITHHMACSHA512ANDAES_256

```

---

### 6.16 虚拟线程

```yaml

spring:

  threads:

    virtual:

      enabled: true   # 一行开启：Tomcat 请求线程、未指定执行器的 @Async、RabbitMQ 监听走虚拟线程

```

> JDK 25 + Spring Boot 4.1 虚拟线程为正式特性，IO 密集型场景（MySQL、ES、Redis、Feign 调用）吞吐量显著提升，无需手动配置线程池大小。

#### 虚拟线程 vs 自定义线程池：如何取舍（重要）

开启虚拟线程后与 6.18 的 `ThreadPoolTaskExecutor` **不冲突**，二者按场景分工：

| 场景 | 用哪种 | 原因 |

|---|---|---|

| Web 请求处理、IO 密集的 `@Async`（无并发上限诉求） | **虚拟线程**（不指定执行器） | 廉价、无需调池大小 |

| 需要**并发限流 / 背压**（如批量调用下游、限制 DB 并发） | **平台线程池**`@Async("bizAsyncExecutor")`） | 虚拟线程「无上限」会瞬间打爆下游；固定池 + 队列可控 |

| CPU 密集任务（加解密、压缩、大量计算） | **平台线程池** | 虚拟线程不减少 CPU 竞争，反而增加调度开销 |

- 一旦在 `@Async` 上**显式指定** `bizAsyncExecutor`，即走平台线程池，不再是虚拟线程；不指定则跟随全局虚拟线程。

- 虚拟线程中**避免长时间持有 `synchronized` 锁 / `ThreadLocal` 大对象**；本文分布式 ID 已改用 `ReentrantLock`（见 6.9）。

- 数据库/HTTP 连接池仍是瓶颈：虚拟线程会放大并发，务必同步调大 HikariCP / Feign 连接池，否则大量虚拟线程阻塞在获取连接上。

---

### 6.17 优雅停机

```yaml

server:

  shutdown: graceful           # 等待当前请求处理完成再停止

spring:

  lifecycle:

    timeout-per-shutdown-phase: 30s   # 最长等待 30s

management:

  endpoints:

    web:

      exposure:

        include: health, info, prometheus

  endpoint:

    health:

      show-details: when-authorized

```

> K8s 滚动发布时，Readiness Probe 检测到服务下线后，Gateway 停止转发流量，服务等待已有请求处理完毕后才退出，保证零请求丢失。

#### 摘流时序（避免停机瞬间仍有流量打入）

Pod 收到 SIGTERM 与 Endpoints 摘除是**并行**发生的，若不等待，kube-proxy/网关可能还在转发新请求到正在关闭的实例。规范用 `preStop` 先 sleep，给注册中心/负载均衡足够时间摘流：

```yaml

# deployment.yaml 片段

lifecycle:

  preStop:

    exec:

      command: ["sh", "-c", "sleep 15"]   # 先睡眠等待摘流，再让进程收 SIGTERM

terminationGracePeriodSeconds: 60         # 需 > preStop + 优雅停机超时(30s)

```

时序`preStop sleep 15s`（Readiness 转不健康、Nacos 下线、网关摘流）→ 进程收到 SIGTERM → Spring 优雅停机等待在途请求（≤30s）→ 退出。同时应从 Nacos 主动 `deregisterspring-cloud` 关停钩子默认会做）。

---

### 6.18 异步线程池（my-web-starter）

> 与 6.16 虚拟线程配合使用：本线程池用于**需要并发上限 / 背压 / CPU 密集**的异步任务；不指定执行器的 `@Async` 仍走全局虚拟线程。通过 `@Async("bizAsyncExecutor")` 显式选用本池。

```java

@Configuration

@EnableAsync

public class AsyncConfig {

    @Bean("bizAsyncExecutor")

    public Executor bizAsyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);

        executor.setMaxPoolSize(50);

        executor.setQueueCapacity(200);

        executor.setThreadNamePrefix("biz-async-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        // TTL 包装：@Async 时自动复制 TransmittableThreadLocal（用户上下文）

        return TtlExecutors.getTtlExecutorService(executor.getThreadPoolExecutor());

    }

}

```

使用：

```java

@Async("bizAsyncExecutor")

public void asyncSendNotification(Long userId) {

    // 此处 UserContext.get() 能拿到发起请求的用户信息

    notificationService.send(userId, UserContext.getUserId());

}

```

---

### 6.19 Nacos 动态配置

```yaml

spring:

  cloud:

    nacos:

      config:

        server-addr: nacos:8848

        namespace: ${NACOS_NAMESPACE}

        file-extension: yaml

        shared-configs:

          - data-id: common.yaml    # 公共配置

            refresh: true

```

```java

// 动态刷新配置类（热更新无需重启）

@Component

@RefreshScope

@ConfigurationProperties(prefix = "biz.feature")

@Data

public class FeatureProperties {

    private boolean orderDoubleCheck = false;  // 开关类配置

    private int     maxRetryCount    = 3;

}

```

---

### 6.20 接口与 Feign 调用日志（my-log-starter）

Controller 与 Feign **同一套控制模型**：配置文件给全局默认，注解可打在类或方法上，优先级 **方法 > 类 > 全局配置**。可分别开关「是否打日志 / 是否打印入参 / 是否打印出参」。

#### 优先级与决策

```

解析顺序（每一项：enabled / logRequest / logResponse）：

  1. 方法上 @XxxLog 若该属性 ≠ DEFAULT → 采用

  2. 否则类上 @XxxLog 若该属性 ≠ DEFAULT → 采用

  3. 否则用 yaml 全局配置

```

| 场景 | 结果 |

|---|---|

| 无注解 | 完全跟随 `my.log.controller.*` / `my.log.feign.*` |

| 类上 `@RequestLog(response = ON)` | 该类默认打印出参，入参仍跟全局 |

| 方法上 `@RequestLog(enabled = OFF)` | 该方法完全不打，覆盖类与全局 |

| 方法 `@FeignLog(request = OFF, response = ON)` | 只打印 Feign 出参 |

#### 三态枚举（解决 boolean 无法表达「跟随上级」）

```java

public enum LogSwitch {

    DEFAULT,  // 跟随上一级（方法→类→全局）

    ON,

    OFF

}

```

#### 全局配置

```yaml

my:

  log:

    # ---------- Controller 接口日志 ----------

    controller:

      enabled: true           # 总开关

      log-request: true       # 打印入参

      log-response: false     # 打印出参（默认关，体量大）

      max-body-length: 2048   # 入参/出参截断

      ignore-params:          # 全局忽略字段名（脱敏前直接丢弃）

        - password

        - oldPassword

        - newPassword

        - token

      exclude-paths:

        - /actuator/**

        - /v3/api-docs/**

    # ---------- Feign 调用日志 ----------

    feign:

      enabled: true

      log-request: true

      log-response: true      # 跨服务排障默认打开，生产可改 false

      max-body-length: 2048

      ignore-params:

        - password

        - token

```

```java

@Data

@ConfigurationProperties(prefix = "my.log")

public class InvokeLogProperties {

    private ChannelLogProperties controller = new ChannelLogProperties();

    private ChannelLogProperties feign = new ChannelLogProperties();

    @Data

    public static class ChannelLogProperties {

        private boolean enabled = true;

        private boolean logRequest = true;

        private boolean logResponse = false;

        private int maxBodyLength = 2048;

        private List<String> ignoreParams = List.of("password", "token");

        private List<String> excludePaths = List.of(); // 仅 controller 使用

    }

}

```

#### 注解定义（Controller / Feign 各一份，语义一致）

```java

/** Controller 接口日志，可标在类或方法 */

@Target({ElementType.TYPE, ElementType.METHOD})

@Retention(RetentionPolicy.RUNTIME)

@Documented

public @interface RequestLog {

    LogSwitch enabled()  default LogSwitch.DEFAULT;

    LogSwitch request()  default LogSwitch.DEFAULT;  // 入参

    LogSwitch response() default LogSwitch.DEFAULT;  // 出参

    String[] ignoreParams() default {};              // 追加忽略字段，与全局合并

}

/** Feign 客户端日志，可标在 @FeignClient 接口或单个方法 */

@Target({ElementType.TYPE, ElementType.METHOD})

@Retention(RetentionPolicy.RUNTIME)

@Documented

public @interface FeignLog {

    LogSwitch enabled()  default LogSwitch.DEFAULT;

    LogSwitch request()  default LogSwitch.DEFAULT;

    LogSwitch response() default LogSwitch.DEFAULT;

    String[] ignoreParams() default {};

}

```

#### 决策器（Controller / Feign 共用）

```java

public record LogDecision(boolean enabled, boolean logRequest, boolean logResponse,

                          Set<String> ignoreParams) {}

public final class LogDecisionResolver {

    public static LogDecision resolve(LogSwitch methodEnabled, LogSwitch methodReq,

                                      LogSwitch methodResp, String[] methodIgnore,

                                      LogSwitch classEnabled, LogSwitch classReq,

                                      LogSwitch classResp, String[] classIgnore,

                                      InvokeLogProperties.ChannelLogProperties global) {

        boolean enabled = pick(methodEnabled, classEnabled, global.isEnabled());

        boolean req     = pick(methodReq, classReq, global.isLogRequest());

        boolean resp    = pick(methodResp, classResp, global.isLogResponse());

        Set<String> ignore = new LinkedHashSet<>(global.getIgnoreParams());

        if (classIgnore != null)  ignore.addAll(Arrays.asList(classIgnore));

        if (methodIgnore != null) ignore.addAll(Arrays.asList(methodIgnore));

        return new LogDecision(enabled, req, resp, ignore);

    }

    private static boolean pick(LogSwitch method, LogSwitch type, boolean global) {

        if (method != null && method != LogSwitch.DEFAULT) return method == LogSwitch.ON;

        if (type != null && type != LogSwitch.DEFAULT) return type == LogSwitch.ON;

        return global;

    }

}

```

#### Controller AOP

```java

@Aspect

@Component

@Slf4j

@Order(Ordered.LOWEST_PRECEDENCE - 10)

@RequiredArgsConstructor

public class RequestLogAspect {

    private final InvokeLogProperties props;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")

    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        Class<?> clazz = method.getDeclaringClass();

        if (pathExcluded(props.getController().getExcludePaths())) {

            return pjp.proceed();

        }

        RequestLog onMethod = method.getAnnotation(RequestLog.class);

        RequestLog onClass  = clazz.getAnnotation(RequestLog.class);

        LogDecision d = LogDecisionResolver.resolve(

            ann(onMethod, RequestLog::enabled), ann(onMethod, RequestLog::request),

            ann(onMethod, RequestLog::response), onMethod == null ? null : onMethod.ignoreParams(),

            ann(onClass, RequestLog::enabled), ann(onClass, RequestLog::request),

            ann(onClass, RequestLog::response), onClass == null ? null : onClass.ignoreParams(),

            props.getController()

        );

        if (!d.enabled()) {

            return pjp.proceed();

        }

        long start = System.currentTimeMillis();

        HttpServletRequest request = currentRequest();

        String in = d.logRequest()

            ? truncate(safeToJson(filterArgs(pjp, d.ignoreParams())),

                props.getController().getMaxBodyLength())

            : "-";

        Object result = null;

        Throwable error = null;

        try {

            result = pjp.proceed();

            return result;

        } catch (Throwable t) {

            error = t;

            throw t;

        } finally {

            String out = "-";

            if (d.logResponse() && error == null) {

                out = truncate(safeToJson(result),

                    props.getController().getMaxBodyLength());

            }

            [log.info](http://log.info)("REQUEST_LOG traceId={} userId={} {} {} ip={} cost={}ms ok={} req={} resp={} err={}",

                TraceContext.traceId(), UserContext.getUserId(),

                request.getMethod(), request.getRequestURI(), getClientIp(request),

                System.currentTimeMillis() - start, error == null,

                in, out, error == null ? "" : error.getMessage());

        }

    }

    private static LogSwitch ann(RequestLog a,

                                 java.util.function.Function<RequestLog, LogSwitch> f) {

        return a == null ? LogSwitch.DEFAULT : f.apply(a);

    }

}

```

使用示例：

```java

@RestController

@RequestMapping("/orders")

@RequestLog(response = LogSwitch.OFF)          // 类：默认不打出参

public class OrderController {

    @GetMapping("/{id}")

    public Result<OrderVO> detail(@PathVariable Long id) { ... }

    @PostMapping

    @RequestLog(response = LogSwitch.ON)       // 方法：强制打出参（覆盖类）

    public Result<Long> create(@RequestBody OrderCreateReq req) { ... }

    @GetMapping("/export")

    @RequestLog(enabled = LogSwitch.OFF)       // 方法：完全关闭

    public void export() { ... }

}

```

#### Feign AOP（切 @FeignClient 接口）

```java

@Aspect

@Component

@Slf4j

@RequiredArgsConstructor

public class FeignLogAspect {

    private final InvokeLogProperties props;

    @Around("execution(* *(..)) && @within([org.springframework.cloud](http://org.springframework.cloud).openfeign.FeignClient)")

    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        // JDK 动态代理：取接口上的注解

        Class<?> feignType = resolveFeignType(pjp);

        Method ifaceMethod = feignType.getMethod(method.getName(), method.getParameterTypes());

        FeignLog onMethod = ifaceMethod.getAnnotation(FeignLog.class);

        FeignLog onClass  = feignType.getAnnotation(FeignLog.class);

        LogDecision d = LogDecisionResolver.resolve(

            ann(onMethod, FeignLog::enabled), ann(onMethod, FeignLog::request),

            ann(onMethod, FeignLog::response), onMethod == null ? null : onMethod.ignoreParams(),

            ann(onClass, FeignLog::enabled), ann(onClass, FeignLog::request),

            ann(onClass, FeignLog::response), onClass == null ? null : onClass.ignoreParams(),

            props.getFeign()

        );

        if (!d.enabled()) {

            return pjp.proceed();

        }

        String client = feignType.getSimpleName() + "#" + ifaceMethod.getName();

        long start = System.currentTimeMillis();

        String in = d.logRequest()

            ? truncate(safeToJson(filterArgs(pjp, d.ignoreParams())),

                props.getFeign().getMaxBodyLength())

            : "-";

        Object result = null;

        Throwable error = null;

        try {

            result = pjp.proceed();

            return result;

        } catch (Throwable t) {

            error = t;

            throw t;

        } finally {

            String out = "-";

            if (d.logResponse()) {

                out = error != null

                    ? ("EX:" + error.getClass().getSimpleName() + ":" + error.getMessage())

                    : truncate(safeToJson(result), props.getFeign().getMaxBodyLength());

            }

            [log.info](http://log.info)("FEIGN_LOG traceId={} client={} cost={}ms ok={} req={} resp={}",

                TraceContext.traceId(), client,

                System.currentTimeMillis() - start, error == null, in, out);

        }

    }

    private static LogSwitch ann(FeignLog a,

                                 java.util.function.Function<FeignLog, LogSwitch> f) {

        return a == null ? LogSwitch.DEFAULT : f.apply(a);

    }

}

```

使用示例：

```java

@FeignClient(name = "biz-service", fallbackFactory = ...)

@FeignLog(response = LogSwitch.ON)                 // 接口级：打出参

public interface BizServiceFeignClient {

    @GetMapping("/internal/order/{id}")

    Result<OrderDTO> getOrder(@PathVariable Long id);

    @PostMapping("/internal/order/sync")

    @FeignLog(request = LogSwitch.OFF)             // 方法：入参含大报文，关闭入参

    Result<Void> sync(@RequestBody OrderSyncReq req);

    @GetMapping("/internal/health")

    @FeignLog(enabled = LogSwitch.OFF)             // 方法：探活不打日志

    Result<Void> health();

}

```

#### 规范

| 项 | 约定 |

|---|---|

| 优先级 | **方法注解 > 类注解 > yaml 全局**（逐项 `LogSwitch` 决策） |

| 总开关 | `enabled=OFF` 时忽略 request/response，整段不打 |

| 敏感字段 | 全局 `ignore-params` + 注解追加；再走脱敏工具 |

| 体量 | `max-body-length` 截断；大对象接口注解关 response/request |

| 日志前缀 | Controller`REQUEST_LOG`，Feign`FEIGN_LOG`，便于 ELK 过滤 |

| 与审计区别 | 本节约排障`@OperationLog` 才是业务审计落库 |

| 生产建议 | Controller 默认 `log-response=false`；Feign 可开，大流量服务再关 |

---

### 6.21 操作日志（审计追踪`@OperationLog`（my-log-starter）

用于「谁在何时对什么做了什么」的业务审计，异步落库（或发 MQ），与请求日志分离存储。

#### 注解定义

```java

@Target(ElementType.METHOD)

@Retention(RetentionPolicy.RUNTIME)

@Documented

public @interface OperationLog {

    /** 业务模块，如「订单」「用户」 */

    String module();

    /** 操作类型：CREATE / UPDATE / DELETE / EXPORT / LOGIN ... */

    String type();

    /** 操作描述，支持 SpEL，如「创建订单：#{#req.orderNo}」 */

    String content() default "";

    /** 是否记录方法入参（脱敏后） */

    boolean saveParams() default true;

    /** 是否记录返回值 */

    boolean saveResult() default false;

}

```

#### 审计实体（落库字段）

```java

@Data

@TableName("sys_operation_log")

public class OperationLogEntity {

    @TableId(type = IdType.ASSIGN_ID)

    private Long id;

    private String module;       // 模块

    private String type;         // 操作类型

    private String content;      // 可读描述

    private String method;       // 类名#方法名

    private String requestUri;

    private String requestMethod;

    private String params;       // 脱敏后 JSON

    private String result;       // 可选

    private String operatorId;

    private String operatorName;

    private String ip;

    private String traceId;

    private Integer status;      // 0失败 1成功

    private String errorMsg;

    private Long   costMs;

    private LocalDateTime createTime;

}

```

#### AOP + 异步落库

```java

@Aspect

@Component

@RequiredArgsConstructor

public class OperationLogAspect {

    private final OperationLogService operationLogService; // 异步写入

    @Around("@annotation(operationLog)")

    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {

        long start = System.currentTimeMillis();

        OperationLogEntity entity = buildBase(pjp, operationLog);

        try {

            Object result = pjp.proceed();

            entity.setStatus(1);

            if (operationLog.saveResult()) {

                entity.setResult(safeToJson(result));

            }

            return result;

        } catch (Throwable t) {

            entity.setStatus(0);

            entity.setErrorMsg(StrUtil.sub(t.getMessage(), 0, 500));

            throw t;

        } finally {

            entity.setCostMs(System.currentTimeMillis() - start);

            entity.setContent(resolveSpel(pjp, operationLog.content()));

            // 异步，避免拖慢主链路；失败仅打错误日志，不影响业务

            operationLogService.saveAsync(entity);

        }

    }

}

```

#### 使用示例

```java

@OperationLog(module = "订单", type = "CREATE", content = "创建订单：#{#req.orderNo}")

@PostMapping("/order")

public Result<Long> create(@RequestBody @Validated OrderCreateReq req) { ... }

@OperationLog(module = "用户", type = "UPDATE", content = "修改手机号：#{#userId}")

@PutMapping("/user/{userId}/mobile")

public Result<Void> updateMobile(@PathVariable Long userId,

                                 @RequestBody UpdateMobileReq req) { ... }

```

#### 规范

- 写操作（增删改、导出、登录、权限变更）必须打 `@OperationLog`

- 查询类接口默认不打，避免表膨胀

- 落库与业务库可同库分表，或独立审计库；保留周期按合规要求（建议 ≥ 180 天）

- 异步失败要有本地补偿或告警，审计不可静默丢失

---

### 6.22 数据脱敏（my-web-starter）

接口出参、日志中的敏感信息统一打码，**展示层脱敏 ≠ 存储层加密**（见 6.23）。

#### 脱敏类型

```java

public enum DesensitizeType {

    MOBILE,      // 138****8000

    ID_CARD,     // 110***********1234

    EMAIL,       // [a***@example.com](mailto:a***@example.com)

    BANK_CARD,   // **** **** **** 1234

    NAME,        // 张*

    PASSWORD,    // ******

    CUSTOM       // 自定义保留前后位数

}

```

#### 注解 + Jackson 序列化

```java

@Target(ElementType.FIELD)

@Retention(RetentionPolicy.RUNTIME)

@JacksonAnnotationsInside

@JsonSerialize(using = DesensitizeSerializer.class)

public @interface Desensitize {

    DesensitizeType type();

    int prefixKeep() default 0;  // CUSTOM 时生效

    int suffixKeep() default 0;

}

public class DesensitizeSerializer extends JsonSerializer<String>

        implements ContextualSerializer {

    private DesensitizeType type;

    private int prefixKeep;

    private int suffixKeep;

    @Override

    public void serialize(String value, JsonGenerator gen,

                          SerializerProvider serializers) throws IOException {

        gen.writeString(DesensitizeUtil.desensitize(value, type, prefixKeep, suffixKeep));

    }

    @Override

    public JsonSerializer<?> createContextual(SerializerProvider prov,

                                              BeanProperty property) {

        Desensitize ann = property.getAnnotation(Desensitize.class);

        // 绑定 type / prefixKeep / suffixKeep 后返回 serializer 实例

        return buildFrom(ann);

    }

}

```

#### VO 使用示例

```java

@Data

public class UserVO {

    private Long id;

    @Desensitize(type = [DesensitizeType.NAME](http://DesensitizeType.NAME))

    private String realName;

    @Desensitize(type = [DesensitizeType.MOBILE](http://DesensitizeType.MOBILE))

    private String mobile;

    @Desensitize(type = [DesensitizeType.ID](http://DesensitizeType.ID)_CARD)

    private String idCard;

}

```

#### 规范

| 场景        | 要求                               |

| --------- | -------------------------------- |

| 对外 API VO | 手机号、证件号、银行卡、邮箱必须脱敏               |

| 请求/操作日志   | 复用同一套 `DesensitizeUtil`，禁止明文落日志  |

| 内部管理端     | 可通过权限开关返回明文（单独 VO / 字段级权限），默认仍脱敏 |

| 导出 Excel  | 与页面展示同级脱敏，除非持有「明文导出」权限并记审计日志     |

---

### 6.23 数据库字段加密（my-crypto-starter）

身份证、手机号等需**落库密文**，应用层加解密。与 Jasypt（配置文件加密）分工：Jasypt 管配置，本 Starter 管业务字段。

#### 算法与密钥

```yaml

my:

  crypto:

    algorithm: AES_GCM          # AES_GCM | SM4_GCM

    # 密钥仅从环境变量 / KMS 注入，禁止写进仓库

    secret-key: ${FIELD_CRYPTO_KEY}

    # 用于等值查询的 HMAC 盐（不可逆）

    hash-salt: ${FIELD_CRYPTO_HASH_SALT}

```

#### 注解 + TypeHandler

```java

@Target(ElementType.FIELD)

@Retention(RetentionPolicy.RUNTIME)

@Documented

public @interface FieldEncrypt {

    /** 是否同时写入哈希列，供等值查询（如按手机号登录） */

    boolean hashQuery() default false;

}

@MappedTypes(String.class)

public class EncryptTypeHandler extends BaseTypeHandler<String> {

    @Override

    public void setNonNullParameter(PreparedStatement ps, int i,

                                    String parameter, JdbcType jdbcType)

            throws SQLException {

        ps.setString(i, CryptoHelper.encrypt(parameter));

    }

    @Override

    public String getNullableResult(ResultSet rs, String columnName)

            throws SQLException {

        return CryptoHelper.decrypt(rs.getString(columnName));

    }

    // getNullableResult(columnIndex / CallableStatement) 同理

}

```

#### 实体示例

```java

@Data

@TableName("biz_user")

public class UserEntity extends BaseEntity {

    /** 密文存储 */

    @TableField(typeHandler = EncryptTypeHandler.class)

    @FieldEncrypt(hashQuery = true)

    private String mobile;

    /** 等值查询用，存 HMAC(mobile)，不可逆 */

    private String mobileHash;

    @TableField(typeHandler = EncryptTypeHandler.class)

    private String idCard;

}

```

#### 查询约定

```java

// 按手机号查询：先算 hash，再等值匹配（密文列不支持 LIKE）

String hash = CryptoHelper.hmac(mobile);

UserEntity user = userMapper.selectOne(

    Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getMobileHash, hash)

);

```

#### 规范

- 仅对高敏感字段加密，避免全表加密拖垮性能与索引

- 密文列**不做模糊查询**；等值查询走 hash 列；全文检索需求走 ES 且需单独合规评估

- 密钥轮换需提供「旧密钥解密 → 新密钥加密」批处理工具

- SQL 日志中密文可打印，**解密后的明文禁止进日志**

---

### 6.24 分页规范（my-web-starter）

统一入参、出参与页大小上限，避免 `select *` 无界查询。

#### 请求 / 响应模型

```java

@Data

public class PageQuery implements Serializable {

    @Min(1)

    private long pageNum  = 1;

    @Min(1)

    @Max(100)                 // 硬上限，防止一次拉爆

    private long pageSize = 20;

    /** 排序字段白名单由业务校验，禁止直接拼 SQL */

    private String orderBy;

    /** asc / desc */

    private String orderDir = "desc";

    public long offset() {

        return (pageNum - 1) * pageSize;

    }

}

@Data

@AllArgsConstructor

@NoArgsConstructor

public class PageResult<T> implements Serializable {

    private List<T> records;

    private long    total;

    private long    pageNum;

    private long    pageSize;

    public static <T> PageResult<T> of(IPage<T> page) {

        return new PageResult<>(page.getRecords(), page.getTotal(),

            page.getCurrent(), page.getSize());

    }

    public long getPages() {

        if (pageSize <= 0) return 0;

        return (total + pageSize - 1) / pageSize;

    }

}

```

#### Controller / Service 用法

```java

@GetMapping("/orders")

public Result<PageResult<OrderVO>> page(@Validated PageQuery query,

                                        OrderPageReq cond) {

    return Result.ok([orderService.page](http://orderService.page)(query, cond));

}

public PageResult<OrderVO> page(PageQuery query, OrderPageReq cond) {

    Page<OrderEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

    // orderBy 必须走白名单映射，例如 create_time / update_time

    applySafeOrder(page, query);

    IPage<OrderEntity> result = orderMapper.selectPage(page, buildWrapper(cond));

    return PageResult.of(result.convert(orderConverter::toVO));

}

```

#### 规范

| 项   | 约定                                           |

| --- | -------------------------------------------- |

| 默认  | `pageNum=1pageSize=20`                    |

| 上限  | `pageSize ≤ 100`；导出走异步任务，不走大分页               |

| 排序  | `orderBy` 白名单；默认 `id` 或 `create_time desc`   |

| 深分页 | 超大偏移改用游标 / `where id > ? limit n`            |

| 响应  | 统一 `PageResult`，禁止各服务自定义 `{list, count}` 字段名 |

---

### 6.25 枚举统一处理规范（my-web-starter + my-mybatis-starter）

前后端、DB 统一用 **code**，展示用 **desc**，禁止魔法数字散落。

#### 基础枚举约定

```java

public interface BaseEnum {

    int getCode();

    String getDesc();

    static <E extends Enum<E> & BaseEnum> E of(Class<E> type, Integer code) {

        if (code == null) return null;

        for (E e : type.getEnumConstants()) {

            if (e.getCode() == code) return e;

        }

        throw new IllegalArgumentException(

            type.getSimpleName() + " 非法枚举值: " + code);

    }

}

@Getter

@AllArgsConstructor

public enum OrderStatus implements BaseEnum {

    PENDING(10, "待支付"),

    PAID(20, "已支付"),

    CLOSED(30, "已关闭"),

    REFUNDED(40, "已退款");

    private final int code;

    private final String desc;

}

```

#### Jackson：入参 / 出参按 code

```java

// 序列化：枚举 → {"code":20,"desc":"已支付"} 或仅 code（由配置决定）

public class BaseEnumSerializer extends JsonSerializer<BaseEnum> {

    @Override

    public void serialize(BaseEnum value, JsonGenerator gen,

                          SerializerProvider serializers) throws IOException {

        gen.writeStartObject();

        gen.writeNumberField("code", value.getCode());

        gen.writeStringField("desc", value.getDesc());

        gen.writeEndObject();

    }

}

// 反序列化：支持传 20 或 "20" 或 {"code":20}

public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum>

        implements ContextualDeserializer {

    private Class<? extends BaseEnum> enumType;

    // createContextual 绑定具体枚举类型后，从 json 解析 code → BaseEnum.of

}

```

#### MyBatis-Plus：存 code

```java

@MappedTypes(BaseEnum.class)

public class BaseEnumTypeHandler<E extends Enum<E> & BaseEnum>

        extends BaseTypeHandler<E> {

    private final Class<E> type;

    public BaseEnumTypeHandler(Class<E> type) {

        this.type = type;

    }

    @Override

    public void setNonNullParameter(PreparedStatement ps, int i,

                                    E parameter, JdbcType jdbcType)

            throws SQLException {

        ps.setInt(i, parameter.getCode());

    }

    @Override

    public E getNullableResult(ResultSet rs, String columnName)

            throws SQLException {

        return BaseEnum.of(type, rs.getInt(columnName));

    }

}

```

实体字段：

```java

@TableField(typeHandler = BaseEnumTypeHandler.class)

private OrderStatus status;

```

#### SpringDoc 展示

```java

@Schema(description = "订单状态", implementation = Integer.class,

        allowableValues = {"10", "20", "30", "40"})

private OrderStatus status;

```

或在枚举上补充 `@JsonValue` / 自定义 ModelConverter，让文档生成 code + desc 说明。

#### 规范

| 项    | 约定                                           |

| ---- | -------------------------------------------- |

| 命名   | 业务枚举一律 `implements BaseEnum`                 |

| 存储   | DB 存 `code`（int），不存 name 字符串                 |

| 接口   | 入参收 code；出参推荐 `{code, desc}`，列表筛选项可另提供枚举字典接口 |

| 字典接口 | `GET /api/enums/{type}` 统一下发，避免前端硬编码文案       |

| 扩展   | 新增枚举值只加不改 code；废弃值保留并标注 `@Deprecated`        |

---

### 6.26 日志 ELK 收集（my-log-starter）

应用输出**结构化 JSON 日志**，经 Filebeat → Logstash → Elasticsearch，Kibana 检索；与 SkyWalking 通过 `traceId` 关联。

#### 架构

```

应用 (logback JSON)

    → 本地文件 / stdout

        → Filebeat

            → Logstash（过滤、脱敏加固、加 env/app 标签）

                → Elasticsearch（按天索引）

                    → Kibana

```

#### logback-spring.xml（JSON）

```xml

<dependency>

    <groupId>net.logstash.logback</groupId>

    <artifactId>logstash-logback-encoder</artifactId>

</dependency>

```

```xml

<!-- logback-spring.xml 核心片段 -->

<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">

    <file>logs/${SPRING_APPLICATION_NAME}.json.log</file>

    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">

        <fileNamePattern>logs/${SPRING_APPLICATION_NAME}.json.%d{yyyy-MM-dd}.log</fileNamePattern>

        <maxHistory>7</maxHistory>

    </rollingPolicy>

    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">

        <providers>

            <timestamp>

                <fieldName>@timestamp</fieldName>

            </timestamp>

            <pattern>

                <pattern>

                    {

                      "app": "${SPRING_APPLICATION_NAME}",

                      "env": "${SPRING_PROFILES_ACTIVE}",

                      "level": "%level",

                      "thread": "%thread",

                      "logger": "%logger",

                      "traceId": "%mdc{tid:-}",

                      "userId": "%mdc{userId:-}",

                      "msg": "%message"

                    }

                </pattern>

            </pattern>

            <stackTrace>

                <fieldName>stackTrace</fieldName>

            </stackTrace>

        </providers>

    </encoder>

</appender>

<root level="INFO">

    <appender-ref ref="JSON_FILE"/>

    <appender-ref ref="CONSOLE"/>   <!-- 本地开发可读；生产可只留 JSON -->

</root>

```

MDC 写入（与请求日志 / 用户上下文配合）：

```java

// 在 UserInterceptor.preHandle 中

MDC.put("userId", userId);

// afterCompletion 中 MDC.clear()

```

#### Filebeat 采集示例

```yaml

filebeat.inputs:

  - type: log

    enabled: true

    paths:

      - /var/log/app/*.json.log

    json.keys_under_root: true

    json.add_error_key: true

    fields:

      collect_type: app-log

    fields_under_root: true

output.logstash:

  hosts: ["logstash:5044"]

```

#### 索引与检索约定

| 项    | 约定                                                        |

| ---- | --------------------------------------------------------- |

| 索引名  | `app-log-{env}-{yyyy.MM.dd}`                              |

| 必带字段 | `@timestampappenvleveltraceIduserIdmsg` |

| 请求日志 | `msg` 以 `REQUEST_LOG` 前缀，或单独字段 `logType=REQUEST`          |

| 审计日志 | 以 DB 为准；必要时双写一条 `logType=OPERATION` 到 ELK                 |

| 保留期  | 热数据 7～15 天，冷数据按合规归档                                       |

| 关联排查 | Kibana 按 `traceId` 过滤 ? SkyWalking UI 同一 TraceId          |

#### 规范

- 生产禁止 `System.out`；统一 SLF4J

- 明文密码、Token、证件号不得进入 ELK（编码器侧可加 `MaskingJsonGeneratorDecorator` 或业务侧先脱敏）

- 错误日志带 `stackTrace`，但需在 Logstash 限制字段长度

- 日志级别：生产默认 INFO；DEBUG 仅临时打开并设 TTL

---

### 6.27 缓存设计规范（my-redis-starter）

6.12 解决 Redis 的「怎么连、怎么序列化、怎么命名」；本节解决**缓存正确性与稳定性**三大问题：穿透、击穿、雪崩，以及缓存与 DB 的一致性。

#### 三大问题与对策

| 问题 | 现象 | 对策 |

|---|---|---|

| **穿透** | 查一个 DB 里根本不存在的 key，缓存永远不命中，请求全压 DB | ① 缓存空值（短 TTL，如 60s）；② 布隆过滤器前置拦截非法 key |

| **击穿** | 某个**热点** key 到期瞬间，大量并发同时回源 DB | 互斥锁（分布式锁）只放一个线程回源；或热点 key 逻辑过期不真删 |

| **雪崩** | 大量 key **同一时刻**集中失效，或 Redis 宕机，DB 被打垮 | ① TTL 加随机抖动；② 多级缓存（Caffeine 本地 + Redis）；③ Redis 高可用 + 熔断降级 |

#### 通用缓存模板（Cache-Aside + 防穿透 + 防击穿）

```java

@Component

@RequiredArgsConstructor

public class CacheTemplate {

    private final StringRedisTemplate redisTemplate;

    private final RedissonClient      redissonClient;

    private final ObjectMapper        objectMapper;

    private static final String NULL_VALUE = "\u0000NULL\u0000"; // 空值占位

    /**

     * 读缓存：未命中则加锁回源，回源结果（含 null）写回，防穿透 + 防击穿。

     * @param ttl        正常值 TTL

     * @param nullTtl    空值 TTL（远小于正常值）

     */

    public <T> T getWithCache(String key, Class<T> type, Duration ttl,

                              Duration nullTtl, Supplier<T> dbLoader) {

        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {

            return NULL_VALUE.equals(cached) ? null : deserialize(cached, type);

        }

        // 未命中：加锁，只允许一个线程回源（防击穿）

        RLock lock = redissonClient.getLock("cache:lock:" + key);

        try {

            lock.lock(3, TimeUnit.SECONDS);

            // 双检：可能已被其它线程写入

            cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {

                return NULL_VALUE.equals(cached) ? null : deserialize(cached, type);

            }

            T value = dbLoader.get();

            if (value == null) {

                // 缓存空值防穿透，TTL 短

                redisTemplate.opsForValue().set(key, NULL_VALUE, nullTtl);

                return null;

            }

            // TTL 加随机抖动，防雪崩

            long jitter = ThreadLocalRandom.current().nextLong(0, ttl.toSeconds() / 5 + 1);

            redisTemplate.opsForValue().set(key, serialize(value),

                ttl.plusSeconds(jitter));

            return value;

        } finally {

            if (lock.isHeldByCurrentThread()) lock.unlock();

        }

    }

}

```

#### 缓存与 DB 一致性：Cache-Aside + 延迟双删

约定统一采用 **Cache-Aside（旁路缓存）**：读走上面的模板；**写操作先更新 DB，再删除缓存**（不是更新缓存）。为消除「更新 DB 与删缓存之间的并发读把旧值又写回」的窗口，对一致性要求高的场景加**延迟双删**：

```java

@Transactional

public void updateUser(UserUpdateReq req) {

    userMapper.updateById(convert(req));      // 1. 更新 DB

    redisTemplate.delete(userKey(req.getId())); // 2. 删缓存

    // 3. 延迟再删一次（覆盖并发读回填旧值的窗口）

    delayedDoubleDelete(userKey(req.getId()), Duration.ofMillis(500));

}

```

- 删除失败要有兜底：发一条 MQ 异步重试删除，或订阅 binlog（Canal）删缓存，保证最终一致。

- 强一致场景（余额、库存）**不要用缓存兜底扣减**，以 DB 行锁 / 乐观锁 / 分布式锁为准。

#### 规范

| 项 | 约定 |

|---|---|

| 模式 | 统一 Cache-Aside；写操作**删缓存**而非更新缓存 |

| 防穿透 | 缓存空值（短 TTL）+ 布隆过滤器（海量非法 key 场景） |

| 防击穿 | 热点 key 回源加分布式锁 / 逻辑过期 |

| 防雪崩 | TTL 加随机抖动；本地 Caffeine 二级缓存；Redis 故障时熔断降级读 DB |

| 一致性 | 高一致性场景延迟双删 + 删除失败 MQ/binlog 兜底 |

| 禁止 | 缓存与 DB 双写都用「更新」；强一致资金/库存仅靠缓存 |

---

### 6.28 Elasticsearch 规范（biz-service）

技术栈中的 ES 承担全文检索、多维筛选、日志/聚合分析。统一使用官方 **Elasticsearch Java Client（8.x）**，禁止再用已废弃的 `TransportClient` / `RestHighLevelClient`。

#### 索引与 Mapping 规范

| 项 | 约定 |

|---|---|

| 命名 | 全小写`{业务}_{实体}_v{版本}`，如 `order_search_v1`，配 `aliasorder_search`）对外，重建索引切别名不停机 |

| 分片 | 单分片 ≤ 30~50GB；副本生产 ≥ 1；避免过度分片 |

| Mapping | **显式定义，关闭 `dynamic` 或设 `strict`**，禁止字段爆炸 |

| 字段 | 需检索用 `text`（配分词器）+ `keyword` 子字段；精确匹配/聚合/排序用 `keyword`/数值/日期 |

| 分词 | 中文用 IK`ik_max_word` 建索引`ik_smart` 查询）|

| 时间 | 统一 UTC 存储`epoch_millis` 或 `strict_date_optional_time` |

```json

PUT order_search_v1

{

  "settings": { "number_of_shards": 3, "number_of_replicas": 1 },

  "mappings": {

    "dynamic": "strict",

    "properties": {

      "orderId":   { "type": "keyword" },

      "title":     { "type": "text", "analyzer": "ik_max_word",

                     "search_analyzer": "ik_smart",

                     "fields": { "kw": { "type": "keyword", "ignore_above": 256 } } },

      "status":    { "type": "integer" },

      "amount":    { "type": "scaled_float", "scaling_factor": 100 },

      "createTime":{ "type": "date", "format": "epoch_millis" }

    }

  }

}

```

#### MySQL → ES 数据同步

| 方案 | 适用 | 说明 |

|---|---|---|

| **应用双写** | 实时性要求高、写量可控 | 事务提交后发 MQ，消费端写 ES；**不要**在 DB 事务里同步写 ES（ES 失败会回滚业务） |

| **Binlog（Canal/Debezium）** | 解耦、不侵入业务 | 订阅 binlog → MQ → 写 ES，最终一致，推荐作为主方案 |

| **定时全量/增量对账** | 兜底 | 按 `updateTime` 增量补偿，修复漏同步 |

> 统一约定：**DB 为准（source of truth），ES 为查询副本**。同步失败必须进死信 + 告警 + 对账，禁止「ES 没数据就当没有」的强依赖用于资金类判断。

#### 查询封装与规范

```java

@Repository

@RequiredArgsConstructor

public class OrderSearchRepository {

    private final ElasticsearchClient client;

    public PageResult<OrderDoc> search(OrderSearchReq req, PageQuery page) throws IOException {

        SearchResponse<OrderDoc> resp = [client.search](http://client.search)(s -> s

            .index("order_search")

            .query(q -> q.bool(b -> {

                if (StrUtil.isNotBlank(req.getKeyword())) {

                    b.must(m -> m.match(t -> t.field("title").query(req.getKeyword())));

                }

                if (req.getStatus() != null) {

                    b.filter(f -> f.term(t -> t.field("status").value(req.getStatus())));

                }

                return b;

            }))

            .from((int) page.offset())

            .size((int) page.getPageSize())

            .sort(so -> so.field(f -> f.field("createTime").order(SortOrder.Desc)))

        , OrderDoc.class);

        List<OrderDoc> list = resp.hits().hits().stream()

            .map(Hit::source).toList();

        long total = resp.hits().total() == null ? 0 : resp.hits().total().value();

        return new PageResult<>(list, total, page.getPageNum(), page.getPageSize());

    }

}

```

#### 规范

- **深分页禁止 `from + size` 超过 1w**：超大翻页用 `search_after`（带排序游标），导出用 `scroll` / PIT。

- 精确筛选用 `filter`（可缓存、不算分），全文相关性才用 `must/should`。

- 写入用 `bulk` 批量；刷新频率按业务容忍度调 `refresh_interval`，不要每写必 `refresh=true`。

- 大字段（正文、附件内容）不放主索引，或设 `index:false` 只存不检索。

- ES 故障要熔断降级：可回退到 DB 精确查询或返回「检索暂不可用」，不拖垮主链路。

---

### 6.29 分布式事务

跨服务写操作（如「下单扣库存 + 生成订单 + 扣积分」）不能依赖单机 `@Transactional`。选型原则：**能用最终一致就不用强一致**，优先 MQ 事务消息 / 本地消息表。

#### 方案选型

| 方案 | 一致性 | 侵入性 | 适用场景 |

|---|---|---|---|

| **本地消息表 + MQ** | 最终一致 | 中 | 主流首选：业务表与消息表同库事务，异步投递下游 |

| **RabbitMQ / RocketMQ 事务消息** | 最终一致 | 中 | 有事务消息能力的 MQ，省一张消息表 |

| **Seata AT** | 弱强一致（自动补偿） | 低（近似无侵入） | 快速改造、DB 支持、并发不极端 |

| **TCC** | 强一致 | 高（三段接口） | 资金、库存等强一致且性能敏感 |

| **Saga** | 最终一致 | 中 | 长流程、多步骤可补偿的业务编排 |

#### 推荐范式一：本地消息表（最终一致）

```

下单服务（本地事务）

├── INSERT 订单

└── INSERT 消息表(status=待发送)      ← 与业务同一个事务，同生共死

        │  事务提交后

        ▼

定时任务/事务提交回调 → 投递 MQ → 标记消息表 status=已发送

        │

        ▼

库存服务消费（幂等）→ 扣库存 → ACK

```

- 消费端**必须幂等**（见 6.8，用 messageId / 业务唯一键去重）。

- 投递失败重试有上限，超限进死信 + 告警 + 人工/对账补偿。

- 与 6.10 RabbitMQ 可靠性（Publisher Confirm + 手动 ACK + 死信）配套使用。

#### 推荐范式二：Seata AT（快速改造）

```yaml

seata:

  enabled: true

  tx-service-group: biz-tx-group

  registry:

    type: nacos

  service:

    vgroup-mapping:

      biz-tx-group: default

```

```java

// 全局事务发起方一个注解即可，下游各自本地事务由 Seata 协调回滚

@GlobalTransactional(rollbackFor = Exception.class)

public void createOrder(OrderCreateReq req) {

    orderService.create(req);                 // 本服务 DB

    stockFeignClient.deduct(req.getSkuId());  // 跨服务，失败自动全局回滚

    pointFeignClient.deduct(req.getUserId());

}

```

> AT 模式需每个库建 `undo_log` 表；注意全局锁带来的并发下降，热点行争用严重时改用 TCC/消息最终一致。

#### 规范

- 默认走**最终一致 + 幂等 + 对账**，把强一致成本降到最低。

- 一次分布式事务内**尽量减少参与方**，长事务拆异步。

- 无论哪种方案，**幂等、重试、对账、告警**四件套缺一不可。

- 禁止用「跨服务大事务包住多个远程调用同步等待」的伪强一致写法。

---

### 6.30 数据库设计规范

统一 MySQL 8.0+ / InnoDB / utf8mb4。目标：可维护、可扩展、防止慢 SQL 与线上事故。

#### 建表规范

| 项 | 约定 |

|---|---|

| 引擎/字符集 | InnoDB + `utf8mb4` + `utf8mb4_0900_ai_ci` |

| 主键 | `bigint unsigned`，用 6.9 Snowflake，禁止自增主键跨库冲突 |

| 必备字段 | `create_timeupdate_timecreate_byupdate_byversiondeleted`（对应 6.11 `BaseEntity`）|

| 命名 | 表/字段小写下划线；布尔用 `is_xxx`；金额用 `decimal(N,2)`，禁止 `float/double` |

| 时间 | `datetime`（存本地）或 `bigint`（存毫秒），全库统一 |

| NULL | 尽量 `NOT NULL DEFAULT`，避免 NULL 参与索引/计算的坑 |

| 注释 | 表与字段必须写 `COMMENT` |

#### 索引规范

- 单表索引数量控制（建议 ≤ 5），遵循**最左前缀**，高频查询建**联合索引**而非多个单列。

- 区分度低的列（性别、状态）单独建索引意义不大，优先放联合索引靠后位置。

- 避免索引失效：列上函数运算、隐式类型转换（字符串列传数字）`LIKE '%x'` 前缀模糊。

- 覆盖索引减少回表`ORDER BY` / `GROUP BY` 尽量走索引。

#### SQL 与事务规范

- 禁止 `SELECT *`，只取所需列；禁止无 `WHERE` / 无 `LIMIT` 的更新删除（6.11 已用 `BlockAttackInnerInterceptor` 兜底）。

- 单条 SQL 扫描行数、事务时长有阈值；大事务拆批（批量 `IN` 控制在 500~1000）。

- 分页深翻页用游标`where id > ? limit n`），见 6.24。

- 慢 SQL：开启 `slow_query_loglong_query_time=1s`，纳入监控告警（见 6.32）。

#### 大表 DDL 与容量

- 在线 DDL 用 `ALGORITHM=INPLACE, LOCK=NONE`，大表变更走 gh-ost / pt-online-schema-change。

- 单表数据量预估超千万级提前规划**分库分表（ShardingSphere-JDBC）**或归档；分片键随业务查询维度选取。

- 读多写少可配**读写分离**（ShardingSphere / MySQL 主从），注意主从延迟导致的读旧数据，强一致读走主库。

#### 迁移管理

- 使用 **Flyway**（或 Liquibase）管理 DDL 版本，脚本入仓库、随应用发布执行，禁止手工改生产库结构。

```

src/main/resources/db/migration/

├── V1__init_order.sql

├── V2__add_index_order_user.sql

└── V3__add_column_order_channel.sql

```

```yaml

spring:

  flyway:

    enabled: true

    baseline-on-migrate: true

    locations: classpath:db/migration

```

---

### 6.31 认证鉴权与接口安全

6.1 网关只做「解析 token、注入用户」；本节补齐**认证中心、令牌机制、接口级权限、通用攻击防护**。

#### 认证与令牌（JWT + 刷新）

- 独立**认证中心（auth-service）** 负责登录、签发/刷新令牌、注销；业务服务不碰密码。

- 采用 **Access Token（短，如 30min）+ Refresh Token（长，如 7d）**：

  - Access 无状态 JWT，网关本地校验签名（用公钥），减少每请求查库。

  - Refresh 存 Redis，可主动失效（改密/登出/风控踢人时删除，实现「可撤销」）。

- 令牌载荷只放 `userId / 角色 / 过期`，**不放敏感信息**；签名用 RS256（非对称，网关只持公钥）。

```

登录 → auth-service 校验 → 签发 AT(JWT)+RT

请求 → Gateway 校验 AT 签名+有效期 → 注入 X-User-Id（见 6.1）

AT 过期 → 前端用 RT 换新 AT → auth-service 校验 RT(Redis) → 发新 AT

登出/改密/风控 → 删除 Redis 中 RT + AT 黑名单（可选）

```

#### 接口级权限（RBAC）

网关做粗粒度（登录/白名单），业务侧做细粒度（按钮/数据权限）。用 Spring Security 注解：

```java

@PreAuthorize("hasAuthority('order:create')")

@PostMapping("/order")

public Result<Long> create(@RequestBody @Validated OrderCreateReq req) { ... }

```

- **越权防护**：

  - 纵向越权`@PreAuthorize` 校验权限点，禁止仅靠前端隐藏按钮。

  - 横向越权：查询/修改必须带**数据归属校验**`where user_id = 当前用户` 或租户隔离），禁止仅凭前端传入 ID 直接操作。

#### 通用攻击防护

| 风险 | 对策 |

|---|---|

| SQL 注入 | MyBatis 用 `#{}` 预编译；动态排序字段走白名单（见 6.24），禁止拼接 |

| XSS | 输出侧转义；富文本用白名单过滤（如 OWASP Java HTML Sanitizer）；响应头 `Content-Security-Policy` |

| CSRF | 纯 Token（Header 承载）天然免疫；用 Cookie 时开启 CSRF Token |

| 重放攻击 | 敏感接口加**签名 + timestamp + nonce**，nonce 存 Redis 防重放；见下 |

| 敏感数据 | 传输 HTTPS；出参脱敏（6.22）；落库加密（6.23）|

| 暴力破解 | 登录接口限流（6.7）+ 验证码 + 账号锁定 |

#### 开放接口签名（防篡改 + 防重放）

```

客户端：sign = HMAC-SHA256(appSecret, sortedParams + timestamp + nonce)

请求头：X-App-Id / X-Timestamp / X-Nonce / X-Sign

服务端：

  1. 校验 timestamp 在 ±5min 内（防重放窗口）

  2. 校验 nonce 未使用过（Redis setnx，TTL=窗口大小）

  3. 用 appSecret 重算 sign 比对

```

#### 规范

- 密码存储用 **BCrypt/Argon2**，禁止 MD5/SHA1 明文哈希。

- 权限点集中管理，接口默认「非白名单即拒绝」。

- 所有对外接口默认经网关鉴权；内部接口`/internal/**`）仅内网可达，见 6.1 安全要点。

---

### 6.32 可观测性（Metrics 监控告警）

已有 **SkyWalking（链路）+ ELK（日志）**，本节补齐**指标监控（Metrics）** 三大支柱的最后一块：Micrometer → Prometheus → Grafana + Alertmanager。

#### 指标暴露

```xml

<dependency>

    <groupId>io.micrometer</groupId>

    <artifactId>micrometer-registry-prometheus</artifactId>

</dependency>

```

```yaml

management:

  endpoints:

    web:

      exposure:

        include: health, info, prometheus, metrics

  metrics:

    tags:

      application: ${[spring.application.name](http://spring.application.name)}   # 所有指标带上服务名，便于聚合

  endpoint:

    health:

      probes:

        enabled: true          # 暴露 /actuator/health/liveness、/readiness 供 K8s 探针

```

> Prometheus 抓 `/actuator/prometheus`；Grafana 出面板；关键指标接 Alertmanager 告警。

#### 关键指标与告警阈值（建议）

| 类别 | 指标 | 告警参考 |

|---|---|---|

| 流量 | QPS`http.server.requests` | 突增/骤降偏离基线 |

| 时延 | P99 响应时间 | P99 > 1s 持续 5min |

| 错误 | 5xx 比例、业务失败率 | 5xx > 1% 持续 5min |

| JVM | 堆使用率、GC 停顿、线程数 | 老年代 > 80%、Full GC 频繁 |

| 连接池 | Hikari 活跃/等待连接 | 等待连接数 > 0 持续 |

| 中间件 | Redis/MQ 连接、MQ 积压、消费延迟 | 队列积压 > 阈值 |

| 依赖 | Feign 调用失败率、熔断器状态 | 熔断器 Open |

#### 自定义业务指标

```java

@Component

@RequiredArgsConstructor

public class OrderMetrics {

    private final MeterRegistry registry;

    public void countCreated(String channel) {

        registry.counter("biz.order.created", "channel", channel).increment();

    }

    public <T> T recordCreateTimer(Supplier<T> action) {

        return registry.timer("biz.order.create.timer").record(action);

    }

}

```

#### 规范

- 三支柱用 `traceId` 串联：Metrics 发现异常 → ELK 按 traceId 查日志 → SkyWalking 看调用链。

- 健康检查区分 **liveness（存活，挂了重启）** 与 **readiness（就绪，未就绪摘流）**，配合 6.17 优雅停机。

- 告警要**分级 + 收敛**（避免告警风暴），关键告警接值班（电话/IM），非关键进看板。

- 指标 tag 基数（cardinality）要控制，禁止把 userId/订单号等高基数值当 tag。

---

### 6.33 测试规范

分层测试策略，保证 Starter 与业务代码质量，纳入 CI 门禁。

#### 测试金字塔

| 层级 | 范围 | 工具 | 占比 |

|---|---|---|---|

| 单元测试 | 单类/方法，Mock 依赖 | JUnit 5 + Mockito | 多（快、稳） |

| 切片测试 | Web 层 / MyBatis 层 | `@WebMvcTest` / `@MybatisPlusTest` | 中 |

| 集成测试 | 真实中间件 | **Testcontainers**（MySQL/Redis/RabbitMQ/ES） | 少（关键路径） |

| 契约/E2E | 跨服务 | Spring Cloud Contract | 极少 |

#### 单元测试示例

```java

@ExtendWith(MockitoExtension.class)

class OrderServiceTest {

    @Mock  OrderMapper orderMapper;

    @InjectMocks OrderServiceImpl orderService;

    @Test

    void create_should_throw_when_stock_not_enough() {

        when(orderMapper.selectStock(anyLong())).thenReturn(0);

        BizException ex = assertThrows(BizException.class,

            () -> orderService.create(new OrderCreateReq()));

        assertEquals(OrderErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());

    }

}

```

#### 集成测试（Testcontainers）

```java

@SpringBootTest

@Testcontainers

class OrderRepositoryIT {

    @Container

    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource

    static void props(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", mysql::getJdbcUrl);

        registry.add("spring.datasource.username", mysql::getUsername);

        registry.add("spring.datasource.password", mysql::getPassword);

    }

    @Autowired OrderMapper orderMapper;

    @Test

    void insert_and_query() { /* 用真实 MySQL 验证 SQL / TypeHandler / 逻辑删除 */ }

}

```

#### 规范

- 核心业务、公共 Starter **单测覆盖率门禁**（如行覆盖 ≥ 70%），CI 不达标阻断合并。

- 测试**不依赖外部环境**：中间件用 Testcontainers，外部服务用 WireMock/Mock。

- 测试数据隔离、可重复；禁止依赖执行顺序、禁止连生产/测试库跑用例。

- 加密、幂等、限流、分布式锁等 Starter 必须有针对性测试（含并发用例）。

---

### 6.34 容器化与 K8s 部署

统一容器化交付，配合 6.17 优雅停机、6.32 探针，实现零停机滚动发布。

#### Dockerfile（分层构建 + JDK 25）

```dockerfile

# 构建阶段

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests

# 运行阶段：精简镜像，非 root 运行

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build /app/target/*.jar app.jar

# SkyWalking Agent（见 6.14）可在此 COPY 进镜像或用 sidecar/initContainer 注入

USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

```

> 容器内存用 `MaxRAMPercentage` 按 limit 百分比分配，不要写死 `-Xmx`；JDK 25 可用 ZGC/G1，按延迟诉求选。

#### K8s Deployment（探针 + 优雅停机 + 资源）

```yaml

apiVersion: apps/v1

kind: Deployment

metadata:

  name: biz-web

spec:

  replicas: 3

  strategy:

    type: RollingUpdate

    rollingUpdate:

      maxSurge: 1

      maxUnavailable: 0        # 保证滚动期间可用副本数不下降

  template:

    spec:

      containers:

        - name: biz-web

          image: registry.company.com/biz-web:1.0.0

          ports:

            - containerPort: 8080

          env:

            - name: SPRING_PROFILES_ACTIVE

              value: prod

            - name: JASYPT_PASSWORD

              valueFrom: { secretKeyRef: { name: biz-secret, key: jasypt } }

          resources:

            requests: { cpu: "500m", memory: "1Gi" }

            limits:   { cpu: "2",    memory: "2Gi" }

          readinessProbe:

            httpGet: { path: /api/actuator/health/readiness, port: 8080 }

            initialDelaySeconds: 20

            periodSeconds: 5

          livenessProbe:

            httpGet: { path: /api/actuator/health/liveness, port: 8080 }

            initialDelaySeconds: 40

            periodSeconds: 10

          lifecycle:

            preStop:

              exec: { command: ["sh", "-c", "sleep 15"] }   # 见 6.17 摘流

      terminationGracePeriodSeconds: 60

```

#### 敏感信息与配置

- 密钥（Jasypt 口令、字段加密密钥、DB 密码）走 **K8s Secret / KMS**，禁止进镜像与仓库（呼应 6.15/6.23）。

- 环境差异走 Nacos + `SPRING_PROFILES_ACTIVE`，镜像一份多环境复用（Build once, run anywhere）。

#### 发布策略

| 策略 | 说明 |

|---|---|

| 滚动发布 | 默认`maxUnavailable=0` 保证不掉可用副本 |

| 蓝绿 | 两套环境切流量，回滚快，成本高 |

| 灰度/金丝雀 | 网关按比例/标签路由到新版本，逐步放量（配合 6.1 路由）|

#### 规范

- 镜像**非 root 运行**、最小基础镜像、打标签用版本号（禁止 `latest` 上生产）。

- 每个服务配 `requests/limits`，防止资源争抢与 OOM 连锁。

- CI/CD：代码合并 → 测试门禁（6.33）→ 构建镜像 → 推仓库 → 部署（Helm/Argo CD），全程可追溯、可回滚。

---

## 7. 完整配置文件参考

```yaml

# application.yml（公共配置）

spring:

  application:

    name: biz-web

  profiles:

    active: @profiles.active@

  threads:

    virtual:

      enabled: true

  lifecycle:

    timeout-per-shutdown-phase: 30s

server:

  port: 8080

  shutdown: graceful

  servlet:

    context-path: /api

# Nacos

  cloud:

    nacos:

      discovery:

        server-addr: ${NACOS_ADDR:nacos:8848}

      config:

        server-addr: ${NACOS_ADDR:nacos:8848}

        file-extension: yaml

# Jasypt

jasypt:

  encryptor:

    password: ${JASYPT_PASSWORD}

    algorithm: PBEWITHHMACSHA512ANDAES_256

# SpringDoc

springdoc:

  enabled: true

  version: 1.0.0

# 请求日志 / Feign 日志 / 审计

my:

  log:

    controller:

      enabled: true

      log-request: true

      log-response: false

      max-body-length: 2048

      ignore-params:

        - password

        - token

      exclude-paths:

        - /actuator/**

        - /v3/api-docs/**

    feign:

      enabled: true

      log-request: true

      log-response: true

      max-body-length: 2048

      ignore-params:

        - password

        - token

    operation:

      enabled: true

      async: true

  crypto:

    algorithm: AES_GCM

    secret-key: ${FIELD_CRYPTO_KEY:}

    hash-salt: ${FIELD_CRYPTO_HASH_SALT:}

  page:

    default-size: 20

    max-size: 100

# Actuator

management:

  endpoints:

    web:

      exposure:

        include: health, info

  endpoint:

    health:

      show-details: when-authorized

```

```yaml

# application-dev.yml（开发环境）

spring:

  datasource:

    url: jdbc:mysql://localhost:3306/biz_db?characterEncoding=utf8&useSSL=false

    username: root

    password: ENC(开发环境密文)

    hikari:

      maximum-pool-size: 20

      minimum-idle: 5

      connection-timeout: 30000

  redis:

    host: [localhost](http://localhost)

    port: 6379

  rabbitmq:

    host: [localhost](http://localhost)

    port: 5672

    username: guest

    password: ENC(开发环境密文)

    publisher-confirm-type: correlated

    publisher-returns: true

    listener:

      simple:

        acknowledge-mode: manual

        prefetch: 10

# MyBatis-Plus

mybatis-plus:

  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:

    map-underscore-to-camel-case: true

    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

springdoc:

  enabled: true    # 开发环境开启文档

```

```yaml

# application-prod.yml（生产环境）

springdoc:

  enabled: false   # 生产环境关闭文档

mybatis-plus:

  configuration:

    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl  # 生产不打 SQL

```

---

## 8. 优化与注意事项

### 关键注意点汇总

| 场景            | 问题                | 解决方案                                        |

| ------------- | ----------------- | ------------------------------------------- |

| 异步线程          | ThreadLocal 丢失    | TTL 线程池包装                                   |

| Feign 跨服务     | 用户信息不传递           | Header 透传，不依赖 ThreadLocal                   |

| Feign 错误       | 降级吞掉下游业务码         | ErrorDecoder 还原 BizException；Fallback 仅超时/熔断 |

| 错误码设计         | 业务码与 HTTP 混用       | 枚举 code=业务码，另挂 httpStatus；成功 Result.code=0 |

| 一律 HTTP 200   | 监控/熔断失真           | 失败用 4xx/5xx + Result 错误体                    |

| Long 型 ID     | 前端 JS 精度丢失        | Jackson 全局 Long → String                    |

| BigDecimal 金额 | 精度丢失              | Jackson 全局 BigDecimal → String              |

| 大表 DDL        | 锁表                | ALGORITHM=INPLACE, LOCK=NONE                |

| MySQL 全表更新    | 误操作风险             | BlockAttackInnerInterceptor                 |

| RabbitMQ 消费   | 消息丢失              | 手动 ACK + 死信队列                               |

| Redis key 冲突  | 多服务共用 Redis       | 统一加 appName 前缀                              |

| Starter 扫描    | 扫描到业务代码           | 禁止 @ComponentScan，用 @AutoConfiguration      |

| Bean 覆盖       | Starter Bean 无法替换 | 所有 Starter Bean 加 @ConditionalOnMissingBean |

| 生产环境文档        | 接口信息泄露            | springdoc.enabled=false 关闭                  |

| 敏感配置          | 密码明文              | Jasypt 加密 + 环境变量注入密钥                        |

| 时钟回拨          | 分布式 ID 重复         | Snowflake 回拨 5ms 内等待，超出拒绝生成                 |

| 服务重启          | workerId 重复分配     | Redis 心跳 + ip:port 复用机制                     |

| 请求日志过大        | ELK / 磁盘被打满       | 截断 + 排除路径；注解关出参；方法>类>全局关闭噪音接口     |

| Feign 日志噪音      | 全量出入参拖垮日志       | `my.log.feign` 全局开关 + `@FeignLog` 方法级覆盖      |

| 审计丢失          | 异步落库失败无感知         | 失败告警 / 本地补偿，关键操作同步写                         |

| 敏感信息出参        | 手机号证件号明文          | `@Desensitize` + 日志复用脱敏工具                   |

| 库内明文          | 拖库即泄露             | `@FieldEncrypt` + HMAC 等值查询列                |

| 密文模糊查         | LIKE 失效           | 禁止对密文列模糊查，等值走 hash                          |

| 分页无界          | 一次拉取过大            | PageQuery 上限 100，导出走异步                      |

| 枚举魔法值         | 前后端不一致            | 统一 `BaseEnum` code/desc + TypeHandler       |

| 日志难检索         | 纯文本无 traceId      | JSON 日志 + MDC + ELK 按 traceId 关联            |

| 限流计数失真        | 同毫秒并发 member 覆盖   | ZSET member 用「时间戳+唯一后缀」，PEXPIRE 毫秒过期        |

| 幂等拿不到结果       | 只占位不缓存结果          | 两态 PENDING/DONE`cacheResult` 重放返回首次结果      |

| 缓存穿透          | 查不存在 key 压垮 DB    | 缓存空值 + 布隆过滤器                                 |

| 缓存击穿          | 热点 key 到期并发回源     | 回源加分布式锁 / 逻辑过期                               |

| 缓存雪崩          | 大量 key 同时失效       | TTL 随机抖动 + 本地二级缓存 + Redis 降级                 |

| 缓存不一致         | 双写更新旧值回填          | Cache-Aside 删缓存 + 延迟双删 + binlog 兜底          |

| ES 深分页        | from+size 过大 OOM   | search_after / scroll，DB 为准 ES 为副本          |

| 跨服务写一致        | 大事务同步等待伪强一致       | 本地消息表/事务消息 最终一致 + 幂等 + 对账                    |

| 虚拟线程 pinning  | synchronized 钉住载体线程 | 改 ReentrantLock；下游连接池需同步调大                   |

| 停机丢流量         | SIGTERM 与摘流并行     | preStop sleep 15s 先摘流，再优雅停机                  |

| Header 伪造身份   | 下游直信 X-User-Id     | 下游内网隔离 + 网关注入前剥离同名 Header                    |

| CORS 过宽       | `*` + credentials 高危 | 白名单域名，禁止通配 + 携带凭证                            |

| 越权访问          | 只校验登录不校验权限        | `@PreAuthorize` + 数据归属校验（横向/纵向）              |

| 无指标监控         | 只有日志/链路，故障后知后觉    | Micrometer + Prometheus + 分级告警               |

| 生产库结构漂移       | 手工改表无版本           | Flyway 脚本入仓随发布执行                             |

| 容器 OOM        | 写死 -Xmx 与 limit 不符 | MaxRAMPercentage 按 limit 分配 + 非 root 运行      |

### Starter 开发规范

1. 所有 `@Bean` 必须加 `@ConditionalOnMissingBean`，保留业务侧覆盖能力

2. 禁止在 Starter 中使用 `@ComponentScan`，使用 `AutoConfiguration.imports` 显式注册

3. 对外依赖尽量声明为 `<optional>true</optional>`，避免强制传递

4. Starter 配置属性统一用 `@ConfigurationProperties`，不散落 `@Value`

5. 每个 Starter 单独发布版本，互相独立迭代

### 目录：Starter 与引入方关系

| Starter               | biz-gateway | biz-web | biz-service |

| --------------------- | ----------- | ------- | ----------- |

| my-security-starter   | —           | ?       | —           |

| my-web-starter        | —           | ?       | —           |

| my-log-starter        | —           | ?       | ?           |

| my-crypto-starter     | —           | —       | ?           |

| my-idempotent-starter | —           | ?       | —           |

| my-id-starter         | —           | —       | ?           |

| my-rabbit-starter     | —           | —       | ?           |

| my-redis-starter      | —           | ?       | ?           |

| my-mybatis-starter    | —           | —       | ?           |

---

> 文档版本：v1.6 | 最后更新：2026-08

>

> v1.6 变更：新增缓存设计（6.27）、Elasticsearch（6.28）、分布式事务（6.29）、数据库设计（6.30）、认证鉴权与接口安全（6.31）、可观测性 Metrics（6.32）、测试规范（6.33）、容器化与 K8s 部署（6.34）；修复限流 Lua member 覆盖、幂等两态、WorkerId 续期与虚拟线程 pinning、BizException 构造函数、CORS/Header 信任等安全与一致性问题。

