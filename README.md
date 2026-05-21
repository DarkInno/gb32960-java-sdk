# DarkInno gb32960-java-sdk

> *We are DarkInno. Like a stout beer, our best ideas are brewed slowly in the dark, away from the hype.*

A high-performance Java SDK implementing the GB/T 32960-2016 communication protocol for electric vehicle (EV) remote service and management systems. Built on Netty TCP transport, it parses raw GB32960 binary messages into structured Java objects, supports one-click Spring Boot Starter integration, and can optionally forward data to Kafka / RocketMQ / RabbitMQ / Redis Streams / MQTT.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-brightgreen)](https://maven.apache.org)

---

## Module Overview

```
gb32960-java/
├── gb32960-core/                    Core protocol engine
├── gb32960-callback/                Callback hook system
├── gb32960-auth/                    Pluggable authentication
├── gb32960-transport/               Netty TCP transport layer
├── gb32960-output-kafka/            Kafka output adapter
├── gb32960-output-rocketmq/         RocketMQ output adapter
├── gb32960-output-rabbitmq/         RabbitMQ output adapter
├── gb32960-output-redis-stream/     Redis Streams output adapter
├── gb32960-output-mqtt/             MQTT output adapter
├── gb32960-spring-boot-starter/     Spring Boot auto-configuration
└── gb32960-example/                 Usage example
```

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.github.darkinno</groupId>
    <artifactId>gb32960-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> Published to **GitHub Packages**. Configure `~/.m2/settings.xml`:
>
> ```xml
> <server>
>     <id>github</id>
>     <username>YOUR_USERNAME</username>
>     <password>YOUR_GITHUB_TOKEN</password>
> </server>
> ```
>
> And add the repository:
>
> ```xml
> <repository>
>     <id>github</id>
>     <url>https://maven.pkg.github.com/DarkInno/gb32960-java-sdk</url>
> </repository>
> ```

### Configuration

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

### Example Code

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
                log.info("{}: SOC={}%, Speed={}km/h, Lng={}, Lat={}",
                    s.vin(), v.getSoc(), v.getSpeed(),
                    p.getLongitude(), p.getLatitude());
            }
        };
    }
}
```

## Protocol Coverage

| CMD | Direction | Function | Auto-Reply |
|-----|-----------|----------|------------|
| 0x01 | Terminal → Platform | Vehicle Login | SUCCESS |
| 0x02 | Terminal → Platform | Real-time Data Report | SUCCESS |
| 0x03 | Terminal → Platform | Reissue Data Report | SUCCESS |
| 0x04 | Terminal → Platform | Vehicle Logout | SUCCESS |
| 0x05 | Platform → Terminal | Platform Login | SUCCESS |
| 0x06 | Platform → Terminal | Platform Logout | SUCCESS |
| 0x07 | Terminal → Platform | Heartbeat | SUCCESS |
| 0x08 | Platform → Terminal | Terminal Time Sync | SUCCESS + BCD Time |

### Encryption

| Type | Status |
|------|--------|
| 0x01 NONE (plain text) | ✅ |
| 0x02 RSA (key exchange) | ✅ (RsaCryptoProvider) |
| 0x03 AES-128 (data encryption) | ✅ (AesCryptoProvider) |

```yaml
gb32960:
  crypto:
    type: aes            # none | aes
    key: "<base64-encoded-128-bit-key>"
```

### Data Information Types

| Info Type | Parsed Fields | Status |
|-----------|--------------|--------|
| 0x01 Vehicle Data | Speed / Odometer / Voltage / Current / SOC / Gear / Insulation / Accelerator / Brake | ✅ |
| 0x02 Drive Motor | Status / RPM / Torque / Temperature / Voltage / Current | ✅ |
| 0x03 Fuel Cell | Voltage / Current / Fuel Consumption Rate / Probe Temp / Hydrogen Pressure | ✅ |
| 0x04 Engine | Status / RPM / Fuel Consumption Rate | ✅ |
| 0x05 Position | Longitude / Latitude / Speed / Heading / Validity Flag | ✅ |
| 0x06 Extremum Data | Max/Min Voltage / Temperature + Subsystem Number | ✅ |
| 0x07 Alarm Data | Alarm Level + 4 Categories of Alarm Flags & Codes | ✅ |
| 0x08 Battery Voltage | Subsystem + Cell Voltages | ✅ |
| 0x09 Battery Temperature | Subsystem + Probe Temperatures | ✅ |

## Test Data

### Protocol Compliance Audit

Byte-by-byte verified against the GB/T 32960-2016 standard — **all 14 checks passed**:

| Check Item | Byte Size | Result |
|------------|-----------|--------|
| Message Structure (Markers/CMD/RESP/VIN/ENC/LEN/BCC) | 24+HDR | PASS |
| BCC Verification Range | Byte2→LastData | PASS |
| Data Length Encoding (Big-Endian) | 2 bytes | PASS |
| Vehicle Data Decoding | 21 bytes | PASS |
| Drive Motor Decoding | 12 bytes/motor | PASS |
| Fuel Cell Decoding (incl. probe temp -40°C offset) | Variable | PASS |
| Engine Decoding | 5 bytes | PASS |
| Position Data Decoding | 13 bytes | PASS |
| Extremum Data Decoding | 14 bytes | PASS |
| Alarm Data Decoding | Variable | PASS |
| Battery Voltage Data Decoding | Variable | PASS |
| Battery Temperature Data Decoding | Variable | PASS |
| Vehicle Login Format | 31 bytes | PASS |
| Platform Login Format | 41 bytes | PASS |

### Unit Tests (71 tests)

| Test Class | Count | Coverage |
|------------|-------|----------|
| BccUtilTest | 6 | XOR calculation, BCC verification, commutativity |
| MessageDecoderTest | 12 | All 8 CMD decoding, invalid message rejection |
| MessageEncoderTest | 6 | Encode/decode roundtrip, response building, VIN padding |
| VehicleSimulatorTest | 7 | Lifecycle, concurrency, high-throughput, battery alarms |
| NoopAuthProviderTest | 2 | Always-pass auth |
| VinWhitelistAuthProviderTest | 7 | Whitelist add/remove/clear/contains/auth |
| CompositeAuthProviderTest | 4 | Composite auth chain, first-failure semantics |
| ConnectionRateLimitProviderTest | 8 | Rate limit, ban, thread safety, custom limits |
| CallbackDispatcherTest | 11 | All 8 callback types, multiple registrations, async, resilience |
| Gb32960AutoConfigurationTest | 6 | Property binding, bean creation, server lifecycle |

### Stress Tests

#### Decoder Throughput / DecoderBenchmark

| Metric | Single-threaded | Multi-threaded (8) |
|--------|----------------|---------------------|
| Message Count | 100,000 | 100,000 |
| Throughput | **7,120,000 msg/s** | **1,540,000 msg/s** |
| Per Message | 0.14 μs | 0.65 μs |

#### Connection Flood / ConnectionFloodTest

| Metric | Value |
|--------|-------|
| Vehicles | 500 |
| Total Messages | 5,500 |
| Total Time | 162 ms |
| Connection Rate | **3,087 conn/s** |
| Message Rate | **33,953 msg/s** |
| Errors | 0 |

#### Long-term Stability / StabilityTest

| Metric | Value |
|--------|-------|
| Connections | 100 |
| Duration | 30 s |
| Client Sent | 3,800 msgs |
| Server Received | 3,800 msgs |
| Server Sent | 3,800 msgs |
| Message Rate | 126 msg/s |
| Dropped | 0 |
| Memory Delta | -1.18 MB (no leak) |

### Concurrent Simulation (100 Vehicles, 30s)

| Metric | Value |
|--------|-------|
| Concurrent Vehicles | 100 |
| Completed | 100/100 (100%) |
| Errors | 0 |

### High-Throughput Simulation (10,000 Messages)

| Metric | Value |
|--------|-------|
| Message Count | 10,000 |
| Decode Success Rate | 100% |

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `gb32960.enabled` | `true` | Enable GB32960 auto-configuration |
| `gb32960.server.port` | `8600` | TCP server port |
| `gb32960.server.boss-threads` | `1` | Netty boss threads |
| `gb32960.server.worker-threads` | `0` | Netty worker threads (0 = CPU count) |
| `gb32960.server.max-connections` | `100000` | Maximum concurrent connections |
| `gb32960.server.idle-timeout-seconds` | `300` | Idle timeout before disconnecting |
| `gb32960.auth.type` | `none` | `none` \| `whitelist` \| `rate_limit` |
| `gb32960.auth.whitelist` | `[]` | Allowed VIN list |
| `gb32960.auth.rate-limit.max-attempts-per-second` | `3` | Rate limit threshold |
| `gb32960.auth.rate-limit.ban-duration-seconds` | `60` | Ban duration after exceeding limit |
| `gb32960.crypto.type` | `none` | `none` \| `aes` |
| `gb32960.crypto.key` | `""` | AES key (base64 encoded) |
| `gb32960.output.kafka.enabled` | `false` | Enable Kafka output |
| `gb32960.output.kafka.topic` | `gb32960` | Kafka topic |
| `gb32960.output.rocketmq.enabled` | `false` | Enable RocketMQ output |
| `gb32960.output.rocketmq.topic` | `gb32960` | RocketMQ topic |
| `gb32960.output.rabbitmq.enabled` | `false` | Enable RabbitMQ output |
| `gb32960.output.rabbitmq.exchange` | `gb32960` | RabbitMQ exchange |
| `gb32960.output.rabbitmq.routing-key` | `""` | RabbitMQ routing key |
| `gb32960.output.redis.enabled` | `false` | Enable Redis Stream output |
| `gb32960.output.redis.stream-key` | `gb32960:stream` | Redis Stream key |
| `gb32960.output.mqtt.enabled` | `false` | Enable MQTT output |
| `gb32960.output.mqtt.topic` | `gb32960` | MQTT topic prefix |
| `gb32960.output.mqtt.broker-url` | `tcp://localhost:1883` | MQTT broker |
| `gb32960.output.mqtt.client-id-prefix` | `gb32960-output` | MQTT client ID prefix |

### Callback Events

| Event | Trigger |
|-------|---------|
| `onSessionConnected` | TCP connection established |
| `onSessionDisconnected` | TCP connection closed |
| `onVehicleLogin` | Vehicle terminal login |
| `onVehicleLogout` | Vehicle terminal logout |
| `onRealtimeData` | Real-time data report |
| `onHeartbeat` | Heartbeat message |
| `onTimingResponse` | Timing synchronization response |
| `onRawMessage` | Raw (undecoded or decode-failed) message |

### Platform API

```java
// Find sessions by VIN
List<Gb32960Session> sessions = server.findSessionsByVin("VIN001");

// Send command to a specific vehicle
server.sendCommand("VIN001", encodedBytes);

// Broadcast to all connected vehicles
server.broadcast(encodedBytes);
```

## Build

```bash
mvn clean compile              # Compile all modules
mvn test                       # Run all tests (including benchmarks)
mvn clean install -DskipTests  # Install to local repository
```

Requirements: Java 17+, Maven 3.9+

## License

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
