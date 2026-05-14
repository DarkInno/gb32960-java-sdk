## 项目概览

- **名称**: gb32960-java (GB/T 32960-2016 Java SDK)
- **语言**: Java 17 LTS
- **构建工具**: Maven
- **包名**: io.github.xxx.gb32960
- **SDK 形态**: Spring Boot Starter (核心 jar + 自动配置)
- **网络框架**: Netty (10k-100k 并发 TCP 连接)
- **核心业务**: GB/T 32960-2016 电动汽车远程服务与管理系统通信协议 Java SDK

### 目标用户
新能源汽车监控平台开发者，需要接入 GB32960 协议的车辆终端数据。

### 核心业务场景
1. **车辆终端 TCP 连接管理**: 接收车辆终端 TCP 长连接，维持 10k-100k 并发
2. **协议解析**: 解析 GB32960 原始报文为结构化 Java 对象
3. **数据分发**: 解析后的车辆数据通过回调钩子推送给业务系统
4. **消息队列转发**: 可选将数据推送到 Kafka/RocketMQ/MQTT/Redis Streams/RabbitMQ
5. **认证管理**: 可配置的车辆终端认证模式

## 数据存储与外部依赖

| 类型 | 选型 | 用途 |
|---|---|---|
| RDBMS | 无（SDK 不预设） | 业务方自行决定 |
| Cache | 无（SDK 不预设） | 业务方自行决定 |
| MQ | 可扩展多适配器 | Kafka/RocketMQ/MQTT/Redis Streams/RabbitMQ |
| 认证 | 可配置 | 可选认证或不认证 |

## 架构设计

### 应用架构

- **模式**: 模块化单体 SDK
- **分层**: Protocol → Transport → Pipeline → Callback → Integration
- **Maven 多模块结构**:

```
gb32960-java/
├── gb32960-core/                    # 核心协议引擎
│   ├── model/                       # GB32960 数据模型 (报文、车况、电池等)
│   ├── codec/                       # 编解码器 (报文编解码、加解密)
│   └── constant/                    # 常量/枚举定义
├── gb32960-transport/               # 传输层 (Netty TCP 服务端)
│   ├── server/                      # Netty Server 实现
│   ├── handler/                     # ChannelHandler 实现
│   └── pipeline/                    # 处理管道配置
├── gb32960-callback/                # 回调钩子系统
│   ├── api/                         # 回调接口定义
│   └── dispatcher/                  # 事件分发器
├── gb32960-auth/                    # 认证模块 (可选)
│   ├── api/                         # 认证接口
│   └── provider/                    # 认证提供者实现
├── gb32960-spring-boot-starter/     # Spring Boot Starter
│   ├── autoconfigure/               # 自动配置
│   └── properties/                  # 配置属性
├── gb32960-output-kafka/            # Kafka 输出适配器 (可选)
├── gb32960-output-rocketmq/         # RocketMQ 输出适配器 (可选)
├── gb32960-output-rabbitmq/         # RabbitMQ 输出适配器 (可选)
├── gb32960-output-redis-stream/     # Redis Streams 输出适配器 (可选)
├── gb32960-output-mqtt/             # MQTT 输出适配器 (可选)
└── gb32960-example/                 # 使用示例
```

### 分层职责

| 层 | 职责 | 依赖方向 |
|---|---|---|
| **Protocol (Core)** | GB32960 报文编解码、数据模型定义 | 无 |
| **Transport (Netty)** | TCP 连接管理、报文收发 | → Protocol |
| **Pipeline** | 消息处理管道 (解码→校验→分发) | → Protocol, Callback |
| **Callback** | 回调接口定义与事件分发 | → Protocol |
| **Auth** | 终端认证 (可插拔) | → Protocol, Transport |
| **Output (MQ)** | 消息队列转发适配器 | → Protocol, Callback |
| **Starter** | Spring Boot 自动装配 | → Transport, Callback, Auth, Output |

### 通信模式

| 模式 | 说明 |
|---|---|
| 终端 → SDK | TCP 长连接 (GB32960 原始报文) |
| SDK → 业务系统 | 回调钩子 (同步/异步) |
| SDK → 消息队列 | Kafka/MQTT/RocketMQ/etc (异步推送) |

### 部署架构

SDK 以 jar 形式嵌入业务应用，无需独立部署。

### 配置管理

- Spring Boot `application.yml` 驱动
- 关键配置: TCP 端口、认证模式、回调 SPI、MQ 连接信息
- 全部配置外部化，支持环境变量覆盖

### 可观测性

- 结构化日志 (SLF4J + Logback)
- 连接数/消息量指标 (Micrometer)
- 健康检查: TCP Server 状态

## GB32960 协议接口拆解

### 协议报文结构

```
起始符 ## (0x23 0x23, 2 bytes)
命令标识 CMD (1 byte)
应答标志 RESP (1 byte)
车辆识别码 VIN (17 bytes, ASCII)
数据加密方式 ENC (1 byte)
数据单元长度 LEN (2 bytes, big-endian)
数据单元 DATA (N bytes)
校验码 BCC (1 byte, XOR from CMD to last byte of DATA)
```

### 命令标识 (CMD)

| CMD | 方向 | 功能 | 数据单元 |
|---|---|---|---|
| 0x01 | 终端→平台 | 车辆登入 | 采集时间 + 登入流水号 + ICCID + 电池子系统数量/编码长度 |
| 0x02 | 终端→平台 | 实时信息上报 | 采集时间 + 整车数据 + 驱动电机 + 燃料电池 + 发动机 + GPS + 极值 + 报警 + 电池 |
| 0x03 | 终端→平台 | 补发信息上报 | 同 0x02 |
| 0x04 | 终端→平台 | 车辆登出 | 采集时间 + 登出流水号 |
| 0x05 | 平台→终端 | 平台登入 | 采集时间 + 登入流水号 + 平台用户名 + 密码 + 加密 |
| 0x06 | 平台→终端 | 平台登出 | 采集时间 + 登出流水号 |
| 0x07 | 终端→平台 | 心跳 | 无数据单元 |
| 0x08 | 平台→终端 | 终端校时 | 无数据单元 (平台下发无数据单元,终端应答含时间) |

### 应答标志 (RESP)

| RESP | 含义 |
|---|---|
| 0x01 | 成功 |
| 0x02 | 错误 |
| 0x03 | VIN 重复 |
| 0xFE | 命令(表示该报文是命令而非应答) |

### 实时数据 (CMD 0x02/0x03) 信息类型

| 信息类型标志 | 内容 |
|---|---|
| 0x01 | 整车数据 |
| 0x02 | 驱动电机数据 |
| 0x03 | 燃料电池数据 |
| 0x04 | 发动机数据 |
| 0x05 | 车辆位置数据 |
| 0x06 | 极值数据 |
| 0x07 | 报警数据 |
| 0x08 | 可充电储能装置电压数据 |
| 0x09 | 可充电储能装置温度数据 |

### 回调接口设计

```java
public interface Gb32960Callback {
    /** 车辆登入 */
    void onVehicleLogin(Session session, VehicleLoginMessage message);
    /** 车辆登出 */
    void onVehicleLogout(Session session, VehicleLogoutMessage message);
    /** 实时/补发信息上报 */
    void onRealtimeData(Session session, RealtimeDataMessage message);
    /** 心跳 */
    void onHeartbeat(Session session, HeartbeatMessage message);
    /** 终端校时应答 */
    void onTimingResponse(Session session, TimingResponseMessage message);
    /** 连接建立 */
    void onSessionConnected(Session session);
    /** 连接断开 */
    void onSessionDisconnected(Session session, Throwable cause);
}
```

## 技术选型

### 架构级组件

| 类别 | 选择 | 理由 |
|---|---|---|
| 编解码 | 自研 (基于 GB32960 标准) | 无现成 Java 实现 |
| 传输层 | Netty 4.x | 高性能 TCP, 10万级并发 |
| 认证 | 可插拔 SPI | 支持不认证/简单认证/VIN白名单 |
| 序列化 | 原始二进制 + Jackson (JSON 输出) | 终端使用二进制, 业务侧可用 JSON |

### 语言级组件

| 类别 | 选择 | 版本 | 理由 |
|---|---|---|---|
| 语言 | Java | 17 LTS | 用户指定 |
| 构建 | Maven | 3.9+ | 用户指定 |
| 网络框架 | Netty | 4.1.x | 高性能 TCP 服务 |
| 日志 | SLF4J + Logback | 1.7+/1.4+ | Java 日志标准 |
| 测试 | JUnit 5 | 5.10+ | Spring 生态标准 |
| 校验 | Jakarta Validation | 3.0 | Bean Validation 标准 |
| JSON | Jackson | 2.16+ | Spring Boot 内置 |
| 监控 | Micrometer | 1.12+ | Spring Boot Actuator 标准 |
| MQTT SDK | Eclipse Paho | 1.2.5 | MQTT 客户端成熟实现 |

### 基础设施 (业务方提供)

| 类别 | 选择 | 说明 |
|---|---|---|
| Kafka | spring-kafka | 如有 Kafka 集群 |
| RocketMQ | rocketmq-spring-boot | 如有 RocketMQ 集群 |
| RabbitMQ | spring-amqp | 如有 RabbitMQ |
| Redis | spring-data-redis | 如有 Redis |

## 并行开发规划

### 模块依赖矩阵

| 模块 | 依赖 | 可并行 | 说明 |
|---|---|---|---|
| gb32960-core | 无 | ✅ | 完全独立 |
| gb32960-transport | core | ❌ | 需 core 完成后 |
| gb32960-callback | core | ✅ | 与 transport 可并行 |
| gb32960-auth | core, callback | ✅ | 可并行 |
| gb32960-output-* | core, callback | ✅ | 各适配器间可并行 |
| gb32960-starter | 所有模块 | ❌ | 最后集成 |

### Layer 1: 基础设施 (顺序)

1. Maven 多模块项目骨架 + 父 POM
2. gb32960-core: 数据模型 + 编解码器 + 常量定义

### Layer 2: 核心模块 (并行 x4)

- Agent A: gb32960-transport (Netty Server + Pipeline)
- Agent B: gb32960-callback (回调接口 + 事件分发)
- Agent C: gb32960-auth (认证接口 + 提供者)
- Agent D: gb32960-output-* (MQ 适配器 5个, 内部并行)

### Layer 3: 聚合集成

- gb32960-spring-boot-starter: 自动配置装配所有模块
