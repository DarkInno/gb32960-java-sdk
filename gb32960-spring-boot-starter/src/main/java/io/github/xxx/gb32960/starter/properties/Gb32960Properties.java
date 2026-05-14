package io.github.xxx.gb32960.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gb32960")
public class Gb32960Properties {

    private boolean enabled = true;

    private Server server = new Server();

    private Auth auth = new Auth();

    private String[] callbacks = {};

    private Output output = new Output();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public String[] getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(String[] callbacks) {
        this.callbacks = callbacks;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public static class Server {

        private int port = 8600;

        private int bossThreads = 1;

        private int workerThreads = 0;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getBossThreads() {
            return bossThreads;
        }

        public void setBossThreads(int bossThreads) {
            this.bossThreads = bossThreads;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }
    }

    public static class Auth {

        private String type = "none";

        private List<String> whitelist = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }
    }

    public static class Output {

        private Kafka kafka = new Kafka();

        private Rocketmq rocketmq = new Rocketmq();

        private Rabbitmq rabbitmq = new Rabbitmq();

        private Redis redis = new Redis();

        private Mqtt mqtt = new Mqtt();

        public Kafka getKafka() {
            return kafka;
        }

        public void setKafka(Kafka kafka) {
            this.kafka = kafka;
        }

        public Rocketmq getRocketmq() {
            return rocketmq;
        }

        public void setRocketmq(Rocketmq rocketmq) {
            this.rocketmq = rocketmq;
        }

        public Rabbitmq getRabbitmq() {
            return rabbitmq;
        }

        public void setRabbitmq(Rabbitmq rabbitmq) {
            this.rabbitmq = rabbitmq;
        }

        public Redis getRedis() {
            return redis;
        }

        public void setRedis(Redis redis) {
            this.redis = redis;
        }

        public Mqtt getMqtt() {
            return mqtt;
        }

        public void setMqtt(Mqtt mqtt) {
            this.mqtt = mqtt;
        }
    }

    public static class Kafka {

        private boolean enabled = false;

        private String topic = "gb32960";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class Rocketmq {

        private boolean enabled = false;

        private String topic = "gb32960";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class Rabbitmq {

        private boolean enabled = false;

        private String exchange = "gb32960";

        private String routingKey = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }

    public static class Redis {

        private boolean enabled = false;

        private String streamKey = "gb32960:stream";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }
    }

    public static class Mqtt {

        private boolean enabled = false;

        private String topic = "gb32960";

        private String brokerUrl = "tcp://localhost:1883";

        private String clientIdPrefix = "gb32960-output";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getBrokerUrl() {
            return brokerUrl;
        }

        public void setBrokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
        }

        public String getClientIdPrefix() {
            return clientIdPrefix;
        }

        public void setClientIdPrefix(String clientIdPrefix) {
            this.clientIdPrefix = clientIdPrefix;
        }
    }
}
