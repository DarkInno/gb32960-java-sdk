package io.github.xxx.gb32960.example;

import io.github.xxx.gb32960.callback.api.Gb32960Callback;
import io.github.xxx.gb32960.callback.api.Session;
import io.github.xxx.gb32960.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExampleApplication {

    private static final Logger log = LoggerFactory.getLogger(ExampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    @Bean
    public Gb32960Callback loggingCallback() {
        return new Gb32960Callback() {
            @Override
            public void onSessionConnected(Session session) {
                log.info("Vehicle connected: id={}, remote={}", session.id(), session.remoteAddress());
            }

            @Override
            public void onSessionDisconnected(Session session, Throwable cause) {
                log.info("Vehicle disconnected: id={}, vin={}, cause={}",
                        session.id(), session.vin(),
                        cause != null ? cause.getMessage() : "normal");
            }

            @Override
            public void onVehicleLogin(Session session, VehicleLoginMessage message) {
                log.info("Vehicle login: vin={}, iccid={}, sn={}",
                        session.vin(), message.getIccid(), message.getSerialNumber());
            }

            @Override
            public void onVehicleLogout(Session session, VehicleLogoutMessage message) {
                log.info("Vehicle logout: vin={}", session.vin());
            }

            @Override
            public void onRealtimeData(Session session, RealtimeDataMessage message) {
                VehicleData vd = message.getVehicleData();
                PositionData pos = message.getPositionData();

                if (vd != null && pos != null) {
                    log.info("Realtime: vin={}, soc={}%, speed={}km/h, lng={}, lat={}",
                            session.vin(), vd.getSoc(), vd.getSpeed(),
                            pos.getLongitude(), pos.getLatitude());
                } else if (vd != null) {
                    log.info("Realtime: vin={}, soc={}%, speed={}km/h, odometer={}km",
                            session.vin(), vd.getSoc(), vd.getSpeed(), vd.getOdometer());
                }
            }

            @Override
            public void onHeartbeat(Session session, HeartbeatMessage message) {
                log.debug("Heartbeat: vin={}", session.vin());
            }
        };
    }
}
