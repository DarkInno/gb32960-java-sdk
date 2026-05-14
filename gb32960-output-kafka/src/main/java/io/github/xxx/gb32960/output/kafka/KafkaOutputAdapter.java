package io.github.xxx.gb32960.output.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.xxx.gb32960.callback.api.OutputAdapter;
import io.github.xxx.gb32960.callback.api.Session;
import io.github.xxx.gb32960.core.model.HeartbeatMessage;
import io.github.xxx.gb32960.core.model.RealtimeDataMessage;
import io.github.xxx.gb32960.core.model.VehicleLoginMessage;
import io.github.xxx.gb32960.core.model.VehicleLogoutMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

public class KafkaOutputAdapter implements OutputAdapter {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutputAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public KafkaOutputAdapter(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void onVehicleLogin(Session session, VehicleLoginMessage message) {
        send(session.vin(), "vehicle_login", message);
    }

    @Override
    public void onVehicleLogout(Session session, VehicleLogoutMessage message) {
        send(session.vin(), "vehicle_logout", message);
    }

    @Override
    public void onRealtimeData(Session session, RealtimeDataMessage message) {
        send(session.vin(), "realtime_data", message);
    }

    @Override
    public void onHeartbeat(Session session, HeartbeatMessage message) {
        send(session.vin(), "heartbeat", message);
    }

    private void send(String vin, String type, Object data) {
        try {
            String json = objectMapper.writeValueAsString(new Gb32960Message(type, vin, data));
            kafkaTemplate.send(topic, vin, json);
            log.debug("Kafka sent: type={}, vin={}, topic={}", type, vin, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: type={}, vin={}", type, vin, e);
        } catch (Exception e) {
            log.error("Failed to send to Kafka: type={}, vin={}", type, vin, e);
        }
    }

    public static class Gb32960Message {
        private final String type;
        private final String vin;
        private final String timestamp;
        private final Object data;

        public Gb32960Message(String type, String vin, Object data) {
            this.type = type;
            this.vin = vin;
            this.timestamp = Instant.now().toString();
            this.data = data;
        }

        public String getType() { return type; }
        public String getVin() { return vin; }
        public String getTimestamp() { return timestamp; }
        public Object getData() { return data; }
    }
}
