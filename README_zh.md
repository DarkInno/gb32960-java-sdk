# DarkInno gb32960-java-sdk

> *We are DarkInno. Like a stout beer, our best ideas are brewed slowly in the dark, away from the hype.*\
> *我们是 DarkInno。如同黑啤，最好的想法在黑暗中缓慢酿造，远离喧嚣。*

GB/T 32960-2016 电动汽车远程服务与管理系统通信协议 Java SDK — 高性能 Netty TCP 传输层，解析 GB32960 原始报文为结构化对象，支持 Spring Boot Starter 一键集成，可扩展输出至 Kafka / RocketMQ / RabbitMQ / Redis Streams / MQTT。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-brightgreen)](https://maven.apache.org)

---

## 模块结构

```
gb32960-java/
├── gb32960-core/                    核心协议引擎
├── gb32960-callback/                回调钩子系统
├── gb32960-auth/                    可插拔认证
├── gb32960-transport/               Netty TCP 传输层
├── gb32960-output-kafka/            Kafka 适配器
├── gb32960-output-rocketmq/         RocketMQ 适配器
├── gb32960-output-rabbitmq/         RabbitMQ 适配器
├── gb32960-output-redis-stream/     Redis Streams 适配器
├── gb32960-output-mqtt/             MQTT 适配器
├── gb32960-spring-boot-starter/     Spring Boot 自动配置
└── gb32960-example/                 使用示例
```

## 快速开始

### Maven

```xml
<dependency>
    <groupId>io.github.darkinno</groupId>
    <artifactId>gb32960-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置

```yaml
gb32960:
  server:
    port: 8600
    max-connections: 100000
    idle-timeout-seconds: 300
  auth:
    type: whitelist    # none | whitelist
    whitelist:
      - LSVAM40E7GA000001
  output:
    kafka:
      enabled: true
      topic: vehicle-data
```

### 示例代码

```java
@SpringBootApplication
public class VehicleMonitorApp {
    @Bean
    public OutputAdapter dataHandler() {
        return new OutputAdapter() {
            @Override
            public void onRealtimeData(Session s, RealtimeDataMessage m) {
                VehicleData v = m.getVehicleData();
                PositionData p = m.getPositionData();
                log.info("{}: SOC={}%, 速度={}km/h, 经度={}, 纬度={}",
                    s.vin(), v.getSoc(), v.getSpeed(),
                    p.getLongitude(), p.getLatitude());
            }
        };
    }
}
```

## 协议覆盖

| CMD | 方向 | 功能 | 自动应答 |
|-----|------|------|---------|
| 0x01 | 终端→平台 | 车辆登入 | SUCCESS |
| 0x02 | 终端→平台 | 实时信息上报 | SUCCESS |
| 0x03 | 终端→平台 | 补发信息上报 | SUCCESS |
| 0x04 | 终端→平台 | 车辆登出 | SUCCESS |
| 0x05 | 平台→终端 | 平台登入 | SUCCESS |
| 0x06 | 平台→终端 | 平台登出 | SUCCESS |
| 0x07 | 终端→平台 | 心跳 | SUCCESS |
| 0x08 | 平台→终端 | 终端校时 | SUCCESS + BCD时间 |

### 数据信息类型

| 类型 | 解析字段 | 状态 |
|------|---------|------|
| 0x01 整车数据 | 速度/里程/电压/电流/SOC/档位/绝缘/加速/制动 | ✅ |
| 0x02 驱动电机 | 状态/转速/扭矩/温度/电压/电流 | ✅ |
| 0x03 燃料电池 | 电压/电流/燃料消耗率/探针温度/氢压 | ✅ |
| 0x04 发动机 | 状态/转速/燃料消耗率 | ✅ |
| 0x05 车辆位置 | 经度/纬度/速度/方向/有效标志 | ✅ |
| 0x06 极值数据 | 最高最低电压/温度+子系统编号 | ✅ |
| 0x07 报警数据 | 报警等级+4类报警标志及代码 | ✅ |
| 0x08 电池电压 | 子系统+单体电压 | ✅ |
| 0x09 电池温度 | 子系统+探针温度 | ✅ |

## 测试数据

### 协议合规审计

经逐字节比对 GB/T 32960-2016 标准，**14项全部通过**：

| 检查项 | 字节数 | 结果 |
|--------|--------|------|
| 报文结构 (起止符/CMD/RESP/VIN/ENC/LEN/BCC) | 24+HDR | PASS |
| BCC 校验范围 | Byte2→LastData | PASS |
| 数据长度编码 (Big-Endian) | 2字节 | PASS |
| 整车数据解码 | 21字节 | PASS |
| 驱动电机解码 | 12字节/电机 | PASS |
| 燃料电池解码 (含探针温度-40℃偏移) | 可变 | PASS |
| 发动机解码 | 5字节 | PASS |
| 位置数据解码 | 13字节 | PASS |
| 极值数据解码 | 14字节 | PASS |
| 报警数据解码 | 可变 | PASS |
| 电池电压数据解码 | 可变 | PASS |
| 电池温度数据解码 | 可变 | PASS |
| 车辆登入格式 | 31字节 | PASS |
| 平台登入格式 | 41字节 | PASS |

### 单元测试 (31 tests)

| 测试类 | 数量 | 覆盖 |
|--------|------|------|
| BccUtilTest | 6 | XOR计算、BCC校验、可交换性 |
| MessageDecoderTest | 12 | 全部8种CMD解码、无效报文拒绝 |
| MessageEncoderTest | 6 | 编解码往返、应答构建、VIN填充 |
| VehicleSimulatorTest | 7 | 生命周期、并发、高吞吐、电池报警 |

### 压力测试

#### 解码吞吐 / DecoderBenchmark

| 指标 | 单线程 | 多线程 (8) |
|------|--------|-----------|
| 消息量 | 100,000 | 100,000 |
| 吞吐量 | **7,120,000 msg/s** | **1,540,000 msg/s** |
| 单条耗时 | 0.14 μs | 0.65 μs |

#### 连接洪峰 / ConnectionFloodTest

| 指标 | 数值 |
|------|------|
| 车辆数 | 500 |
| 总消息量 | 5,500 |
| 总耗时 | 162 ms |
| 连接速率 | **3,087 连接/秒** |
| 消息速率 | **33,953 消息/秒** |
| 错误数 | 0 |

#### 长时间稳定性 / StabilityTest

| 指标 | 数值 |
|------|------|
| 连接数 | 100 |
| 持续时间 | 30 秒 |
| 客户端发送 | 3,800 条 |
| 服务端接收 | 3,800 条 |
| 服务端发送 | 3,800 条 |
| 消息速率 | 126 msg/s |
| 掉线数 | 0 |
| 内存变化 | -1.18 MB (无泄漏) |

### 并发模拟 (100车, 30s)

| 指标 | 数值 |
|------|------|
| 并发车数 | 100 |
| 完成车辆 | 100/100 (100%) |
| 错误 | 0 |

### 高吞吐模拟 (10,000条)

| 指标 | 数值 |
|------|------|
| 消息量 | 10,000 |
| 解码成功率 | 100% |

## 代码审查结果

### 已修复 HIGH 级别问题 (5)

| 文件 | 问题 | 修复 |
|------|------|------|
| ConnectionRateLimitProvider | banMap 无界增长导致内存泄漏 | 过期条目惰性清理 |
| ConnectionRateLimitProvider | null VIN 导致 CHM NPE | null/empty VIN 前置检查 |
| CallbackDispatcher | 异步开关泄漏线程池 | 切换前关闭旧线程池 |
| Gb32960AutoConfiguration | Spring 分派器被内部重建覆盖 | 注入外部分派器给 Config |
| MqttOutputAdapter | topic 参数未使用 | 存入字段用于 MQTT 路径 |

### 性能优化

| 模块 | 优化 |
|------|------|
| RawMessage | isCommand/isSuccess/isError 使用无符号比较 `(b & 0xFF) == 0xFE` |
| compiler | `--release 17` 替代 `-source/-target`，消除系统模块路径警告 |

## 构建

```bash
mvn clean compile              # 完整编译
mvn test                       # 运行测试 (含压测)
mvn clean install -DskipTests  # 安装到本地仓库
```

需求: Java 17+, Maven 3.9+

## 许可证

MIT License

Copyright (c) 2026 DarkInno

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

<p align="center">
  <a href="https://github.com/DarkInno/gb32960-java-sdk/stargazers">
    <img src="https://img.shields.io/github/stars/DarkInno/gb32960-java-sdk?style=social" alt="GitHub stars">
  </a>
  &nbsp;
  <a href="https://github.com/DarkInno/gb32960-java-sdk/network/members">
    <img src="https://img.shields.io/github/forks/DarkInno/gb32960-java-sdk?style=social" alt="GitHub forks">
  </a>
  &nbsp;
  <a href="https://github.com/DarkInno/gb32960-java-sdk/watchers">
    <img src="https://img.shields.io/github/watchers/DarkInno/gb32960-java-sdk?style=social" alt="GitHub watchers">
  </a>
</p>
