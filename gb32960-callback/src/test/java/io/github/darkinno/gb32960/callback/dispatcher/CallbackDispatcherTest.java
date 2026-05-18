package io.github.darkinno.gb32960.callback.dispatcher;

import io.github.darkinno.gb32960.callback.api.Gb32960Callback;
import io.github.darkinno.gb32960.callback.api.Session;
import io.github.darkinno.gb32960.core.model.*;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CallbackDispatcherTest {

    @Test
    void shouldDispatchVehicleLogin() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        VehicleLoginMessage msg = new VehicleLoginMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertTrue(called.get());
    }

    @Test
    void shouldDispatchRealtimeData() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        RealtimeDataMessage msg = new RealtimeDataMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertTrue(called.get());
    }

    @Test
    void shouldDispatchHeartbeat() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertTrue(called.get());
    }

    @Test
    void shouldDispatchSessionConnected() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        dispatcher.dispatchSessionConnected(mockSession());
        assertTrue(called.get());
    }

    @Test
    void shouldDispatchSessionDisconnected() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        dispatcher.dispatchSessionDisconnected(mockSession(), null);
        assertTrue(called.get());
    }

    @Test
    void shouldDispatchToMultipleCallbacks() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicInteger count = new AtomicInteger(0);

        dispatcher.register(newTestCallback(cb -> count.incrementAndGet()));
        dispatcher.register(newTestCallback(cb -> count.incrementAndGet()));

        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertEquals(2, count.get());
    }

    @Test
    void shouldNotDispatchAfterRemoval() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        Gb32960Callback callback = newTestCallback(cb -> called.set(true));
        dispatcher.register(callback);
        dispatcher.remove(callback);

        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertFalse(called.get());
    }

    @Test
    void shouldSurviveCallbackException() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicInteger count = new AtomicInteger(0);

        dispatcher.register(new Gb32960Callback() {
            @Override
            public void onHeartbeat(Session session, HeartbeatMessage message) {
                throw new RuntimeException("test exception");
            }
        });
        dispatcher.register(newTestCallback(cb -> count.incrementAndGet()));

        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        assertEquals(1, count.get());
    }

    @Test
    void shouldRunAsync() throws InterruptedException {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        dispatcher.setAsync(true);
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(newTestCallback(cb -> called.set(true)));

        HeartbeatMessage msg = new HeartbeatMessage();
        msg.setRaw(rawMessage());
        dispatcher.dispatch(mockSession(), msg);

        Thread.sleep(200);
        assertTrue(called.get());
    }

    @Test
    void shouldShutdownCleanly() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        dispatcher.setAsync(true);
        dispatcher.shutdown();
    }

    @Test
    void shouldHandleRawMessageFallback() {
        CallbackDispatcher dispatcher = new CallbackDispatcher();
        AtomicBoolean called = new AtomicBoolean(false);

        dispatcher.register(new Gb32960Callback() {
            @Override
            public void onRawMessage(Session session, RawMessage message) {
                called.set(true);
            }
        });

        dispatcher.dispatch(mockSession(), rawMessage());
        assertTrue(called.get());
    }

    private static Session mockSession() {
        return new Session() {
            @Override public String id() { return "test-session"; }
            @Override public String vin() { return "TESTVIN00000001"; }
            @Override public InetSocketAddress remoteAddress() {
                return new InetSocketAddress("127.0.0.1", 12345);
            }
            @Override public boolean isConnected() { return true; }
            @Override public void send(byte[] data) {}
            @Override public void close() {}
        };
    }

    private static RawMessage rawMessage() {
        return RawMessage.builder()
                .commandFlag((byte) 0x07)
                .vin("TESTVIN00000001")
                .build();
    }

    private static Gb32960Callback newTestCallback(java.util.function.Consumer<Gb32960Callback> action) {
        return new Gb32960Callback() {
            @Override public void onSessionConnected(Session session) { action.accept(this); }
            @Override public void onSessionDisconnected(Session session, Throwable cause) { action.accept(this); }
            @Override public void onVehicleLogin(Session session, VehicleLoginMessage message) { action.accept(this); }
            @Override public void onVehicleLogout(Session session, VehicleLogoutMessage message) { action.accept(this); }
            @Override public void onRealtimeData(Session session, RealtimeDataMessage message) { action.accept(this); }
            @Override public void onHeartbeat(Session session, HeartbeatMessage message) { action.accept(this); }
            @Override public void onTimingResponse(Session session, TimingResponseMessage message) { action.accept(this); }
            @Override public void onRawMessage(Session session, RawMessage message) { action.accept(this); }
        };
    }
}
