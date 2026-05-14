package io.github.xxx.gb32960.starter.autoconfigure;

import io.github.xxx.gb32960.auth.api.AuthProvider;
import io.github.xxx.gb32960.auth.provider.NoopAuthProvider;
import io.github.xxx.gb32960.auth.provider.VinWhitelistAuthProvider;
import io.github.xxx.gb32960.callback.api.OutputAdapter;
import io.github.xxx.gb32960.callback.dispatcher.CallbackDispatcher;
import io.github.xxx.gb32960.starter.properties.Gb32960Properties;
import io.github.xxx.gb32960.transport.server.Gb32960Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(Gb32960Properties.class)
@ConditionalOnProperty(prefix = "gb32960", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Gb32960AutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(Gb32960AutoConfiguration.class);

    @Bean
    public CallbackDispatcher callbackDispatcher() {
        return new CallbackDispatcher();
    }

    @Bean
    public AuthProvider authProvider(Gb32960Properties props) {
        String type = props.getAuth().getType();
        if ("whitelist".equalsIgnoreCase(type)) {
            VinWhitelistAuthProvider provider = new VinWhitelistAuthProvider();
            for (String vin : props.getAuth().getWhitelist()) {
                provider.add(vin);
            }
            return provider;
        }
        return new NoopAuthProvider();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnProperty(prefix = "gb32960.output.kafka", name = "enabled", havingValue = "true")
    public OutputAdapter kafkaAdapter(Gb32960Properties props,
                                        org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate) {
        return new io.github.xxx.gb32960.output.kafka.KafkaOutputAdapter(
                kafkaTemplate, props.getOutput().getKafka().getTopic());
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
    @ConditionalOnProperty(prefix = "gb32960.output.rocketmq", name = "enabled", havingValue = "true")
    public OutputAdapter rocketMQAdapter(Gb32960Properties props,
                                           org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate) {
        return new io.github.xxx.gb32960.output.rocketmq.RocketMQOutputAdapter(
                rocketMQTemplate, props.getOutput().getRocketmq().getTopic());
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
    @ConditionalOnProperty(prefix = "gb32960.output.rabbitmq", name = "enabled", havingValue = "true")
    public OutputAdapter rabbitMQAdapter(Gb32960Properties props,
                                           org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        return new io.github.xxx.gb32960.output.rabbitmq.RabbitMQOutputAdapter(
                rabbitTemplate,
                props.getOutput().getRabbitmq().getExchange(),
                props.getOutput().getRabbitmq().getRoutingKey());
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnProperty(prefix = "gb32960.output.redis", name = "enabled", havingValue = "true")
    public OutputAdapter redisStreamAdapter(Gb32960Properties props,
                                              org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate) {
        return new io.github.xxx.gb32960.output.redis.RedisStreamOutputAdapter(
                stringRedisTemplate, props.getOutput().getRedis().getStreamKey());
    }

    @Bean
    @ConditionalOnClass(name = "org.eclipse.paho.client.mqttv3.MqttClient")
    @ConditionalOnProperty(prefix = "gb32960.output.mqtt", name = "enabled", havingValue = "true")
    public OutputAdapter mqttAdapter(Gb32960Properties props) {
        try {
            return new io.github.xxx.gb32960.output.mqtt.MqttOutputAdapter(
                    props.getOutput().getMqtt().getBrokerUrl(),
                    props.getOutput().getMqtt().getClientIdPrefix(),
                    props.getOutput().getMqtt().getTopic());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MQTT adapter", e);
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Gb32960Server gb32960Server(Gb32960Properties props,
                                       CallbackDispatcher dispatcher,
                                       AuthProvider authProvider,
                                       List<OutputAdapter> callbacks) {
        List<OutputAdapter> allCallbacks = new ArrayList<>(callbacks);

        for (String className : props.getCallbacks()) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof OutputAdapter oa) {
                    allCallbacks.add(oa);
                } else {
                    log.warn("Callback class {} does not implement OutputAdapter, skipping", className);
                }
            } catch (Exception e) {
                log.warn("Failed to instantiate callback class {}: {}", className, e.getMessage());
            }
        }

        for (OutputAdapter adapter : allCallbacks) {
            try {
                adapter.init();
                log.info("Initialized OutputAdapter: {}", adapter.name());
            } catch (Exception e) {
                log.error("Failed to initialize OutputAdapter: {}", adapter.name(), e);
            }
        }

        return Gb32960Server.create(c -> {
            c.port(props.getServer().getPort());
            c.bossThreads(props.getServer().getBossThreads());
            c.workerThreads(props.getServer().getWorkerThreads());
            c.authProvider(authProvider);
            c.dispatcher(dispatcher);
            c.callbacks(allCallbacks);
        });
    }
}
