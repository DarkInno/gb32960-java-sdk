package io.github.xxx.gb32960.output.redis;

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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Map;

public class RedisStreamOutputAdapter implements OutputAdapter {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamOutputAdapter.class);

    private final StringRedisTemplate redisTemplate;
    private final String streamKey;
    private final ObjectMapper objectMapper;

    public RedisStreamOutputAdapter(StringRedisTemplate redisTemplate, String streamKey) {
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
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
            String json = objectMapper.writeValueAsString(data);
            Map<String, String> fields = Map.of(
                    "type", type,
                    "vin", vin,
                    "timestamp", Instant.now().toString(),
                    "data", json
            );
            redisTemplate.opsForStream().add(org.springframework.data.redis.connection.stream.MapRecord.create(streamKey, fields));
            log.debug("Redis Stream sent: type={}, vin={}, stream={}", type, vin, streamKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: type={}, vin={}", type, vin, e);
        } catch (Exception e) {
            log.error("Failed to send to Redis Stream: type={}, vin={}", type, vin, e);
        }
    }
}
