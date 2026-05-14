package io.github.xxx.gb32960.callback.api;

import io.github.xxx.gb32960.core.model.*;

public interface Gb32960Callback {

    default void onSessionConnected(Session session) {}

    default void onSessionDisconnected(Session session, Throwable cause) {}

    default void onVehicleLogin(Session session, VehicleLoginMessage message) {}

    default void onVehicleLogout(Session session, VehicleLogoutMessage message) {}

    default void onRealtimeData(Session session, RealtimeDataMessage message) {}

    default void onHeartbeat(Session session, HeartbeatMessage message) {}

    default void onTimingResponse(Session session, TimingResponseMessage message) {}

    default void onRawMessage(Session session, RawMessage message) {}
}
