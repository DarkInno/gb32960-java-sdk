package io.github.darkinno.gb32960.output.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.darkinno.gb32960.callback.api.OutputAdapter;
import io.github.darkinno.gb32960.callback.api.Session;
import io.github.darkinno.gb32960.core.model.HeartbeatMessage;
import io.github.darkinno.gb32960.core.model.RawMessage;
import io.github.darkinno.gb32960.core.model.RealtimeDataMessage;
import io.github.darkinno.gb32960.core.model.TimingResponseMessage;
import io.github.darkinno.gb32960.core.model.VehicleLoginMessage;
import io.github.darkinno.gb32960.core.model.VehicleLogoutMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class MqttOutputAdapter implements OutputAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttOutputAdapter.class);

    private final MqttClient mqttClient;
    private final String topicPrefix;
    private final ObjectMapper objectMapper;

    public MqttOutputAdapter(String brokerUrl, String clientId, String topicPrefix) throws MqttException {
        this.mqttClient = new MqttClient(brokerUrl, clientId);
        this.topicPrefix = (topicPrefix != null && !topicPrefix.isEmpty()) ? topicPrefix : "gb32960";
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        mqttClient.connect(options);
        log.info("MQTT connected to broker: {}, clientId: {}", brokerUrl, clientId);
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

    @Override
    public void onSessionConnected(Session session) {
        send(session.vin(), "session_connected", null);
    }

    @Override
    public void onSessionDisconnected(Session session, Throwable cause) {
        send(session.vin(), "session_disconnected", cause != null ? cause.getMessage() : "normal");
    }

    @Override
    public void onTimingResponse(Session session, TimingResponseMessage message) {
        send(session.vin(), "timing_response", message);
    }

    @Override
    public void onRawMessage(Session session, RawMessage message) {
        send(session.vin(), "raw_message", message);
    }

    private void send(String vin, String type, Object data) {
        try {
            String topic = topicPrefix + "/" + vin + "/" + type;
            String payload = objectMapper.writeValueAsString(new Gb32960Message(type, vin, data));

            MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
            log.debug("MQTT sent: type={}, vin={}, topic={}", type, vin, topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: type={}, vin={}", type, vin, e);
        } catch (MqttException e) {
            log.error("Failed to send MQTT message: type={}, vin={}", type, vin, e);
        }
    }

    @Override
    public void close() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT client disconnected");
            }
            mqttClient.close();
        } catch (MqttException e) {
            log.error("Failed to close MQTT client", e);
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
