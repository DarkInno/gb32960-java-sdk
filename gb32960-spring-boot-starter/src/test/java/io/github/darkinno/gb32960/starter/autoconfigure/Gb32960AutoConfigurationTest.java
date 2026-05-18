package io.github.darkinno.gb32960.starter.autoconfigure;

import io.github.darkinno.gb32960.starter.properties.Gb32960Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.*;

class Gb32960AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("gb32960.enabled=false");

    @Test
    void shouldBindDefaultProperties() {
        contextRunner
                .withUserConfiguration(PropertiesOnlyConfig.class)
                .run(ctx -> {
                    Gb32960Properties props = ctx.getBean(Gb32960Properties.class);
                    assertThat(props.getServer().getPort()).isEqualTo(8600);
                    assertThat(props.getServer().getBossThreads()).isEqualTo(1);
                    assertThat(props.getServer().getWorkerThreads()).isEqualTo(0);
                    assertThat(props.getServer().getMaxConnections()).isEqualTo(100000);
                    assertThat(props.getServer().getIdleTimeoutSeconds()).isEqualTo(300);
                    assertThat(props.getAuth().getType()).isEqualTo("none");
                });
    }

    @Test
    void shouldBindCustomProperties() {
        contextRunner
                .withUserConfiguration(PropertiesOnlyConfig.class)
                .withPropertyValues(
                        "gb32960.enabled=false",
                        "gb32960.server.port=9999",
                        "gb32960.server.boss-threads=2",
                        "gb32960.server.worker-threads=8",
                        "gb32960.server.max-connections=5000",
                        "gb32960.server.idle-timeout-seconds=600",
                        "gb32960.auth.type=whitelist",
                        "gb32960.auth.whitelist[0]=VIN001",
                        "gb32960.auth.whitelist[1]=VIN002"
                )
                .run(ctx -> {
                    Gb32960Properties props = ctx.getBean(Gb32960Properties.class);
                    assertThat(props.getServer().getPort()).isEqualTo(9999);
                    assertThat(props.getServer().getBossThreads()).isEqualTo(2);
                    assertThat(props.getServer().getWorkerThreads()).isEqualTo(8);
                    assertThat(props.getServer().getMaxConnections()).isEqualTo(5000);
                    assertThat(props.getServer().getIdleTimeoutSeconds()).isEqualTo(600);
                    assertThat(props.getAuth().getType()).isEqualTo("whitelist");
                    assertThat(props.getAuth().getWhitelist()).containsExactly("VIN001", "VIN002");
                });
    }

    @Test
    void shouldNotStartServerWhenDisabled() {
        contextRunner
                .withUserConfiguration(Gb32960AutoConfiguration.class)
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("gb32960Server");
                });
    }

    @Test
    void shouldStartServerWhenEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(Gb32960AutoConfiguration.class)
                .withPropertyValues(
                        "gb32960.enabled=true",
                        "gb32960.server.port=0"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("gb32960Server");
                    assertThat(ctx).hasBean("authProvider");
                    assertThat(ctx).hasBean("callbackDispatcher");
                    assertThat(ctx).hasBean("cryptoProvider");
                });
    }

    @Test
    void shouldBindOutputDefaults() {
        contextRunner
                .withUserConfiguration(PropertiesOnlyConfig.class)
                .run(ctx -> {
                    Gb32960Properties props = ctx.getBean(Gb32960Properties.class);
                    assertThat(props.getOutput().getKafka().isEnabled()).isFalse();
                    assertThat(props.getOutput().getKafka().getTopic()).isEqualTo("gb32960");
                    assertThat(props.getOutput().getMqtt().getBrokerUrl()).isEqualTo("tcp://localhost:1883");
                });
    }

    @Test
    void shouldBindOutputCustomValues() {
        contextRunner
                .withUserConfiguration(PropertiesOnlyConfig.class)
                .withPropertyValues(
                        "gb32960.enabled=false",
                        "gb32960.output.kafka.topic=custom-topic",
                        "gb32960.output.mqtt.broker-url=tcp://broker:1883",
                        "gb32960.output.redis.stream-key=custom:stream"
                )
                .run(ctx -> {
                    Gb32960Properties props = ctx.getBean(Gb32960Properties.class);
                    assertThat(props.getOutput().getKafka().getTopic()).isEqualTo("custom-topic");
                    assertThat(props.getOutput().getMqtt().getBrokerUrl()).isEqualTo("tcp://broker:1883");
                    assertThat(props.getOutput().getRedis().getStreamKey()).isEqualTo("custom:stream");
                });
    }

    @Configuration
    @EnableConfigurationProperties(Gb32960Properties.class)
    static class PropertiesOnlyConfig {
    }
}
