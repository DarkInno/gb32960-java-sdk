package io.github.xxx.gb32960.callback.dispatcher;

import io.github.xxx.gb32960.callback.api.Gb32960Callback;
import io.github.xxx.gb32960.callback.api.Session;
import io.github.xxx.gb32960.core.constant.CommandFlag;
import io.github.xxx.gb32960.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CallbackDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CallbackDispatcher.class);

    private final List<Gb32960Callback> callbacks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean asyncEnabled = new AtomicBoolean(false);
    private volatile ExecutorService executor;

    public void register(Gb32960Callback callback) {
        callbacks.add(callback);
    }

    public void remove(Gb32960Callback callback) {
        callbacks.remove(callback);
    }

    public void setAsync(boolean async) {
        if (asyncEnabled.compareAndSet(!async, async)) {
            if (async) {
                ExecutorService old = executor;
                executor = Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "gb32960-callback");
                    t.setDaemon(true);
                    return t;
                });
                if (old != null) {
                    old.shutdown();
                }
            } else {
                ExecutorService old = executor;
                executor = null;
                if (old != null) {
                    old.shutdown();
                }
            }
        }
    }

    public void dispatchSessionConnected(Session session) {
        invoke(c -> c.onSessionConnected(session), "onSessionConnected");
    }

    public void dispatchSessionDisconnected(Session session, Throwable cause) {
        invoke(c -> c.onSessionDisconnected(session, cause), "onSessionDisconnected");
    }

    public void dispatch(Session session, Object decodedMessage) {
        if (decodedMessage instanceof VehicleLoginMessage m) {
            invoke(c -> c.onVehicleLogin(session, m), "onVehicleLogin");
        } else if (decodedMessage instanceof VehicleLogoutMessage m) {
            invoke(c -> c.onVehicleLogout(session, m), "onVehicleLogout");
        } else if (decodedMessage instanceof RealtimeDataMessage m) {
            invoke(c -> c.onRealtimeData(session, m), "onRealtimeData");
        } else if (decodedMessage instanceof HeartbeatMessage m) {
            invoke(c -> c.onHeartbeat(session, m), "onHeartbeat");
        } else if (decodedMessage instanceof TimingResponseMessage m) {
            invoke(c -> c.onTimingResponse(session, m), "onTimingResponse");
        } else if (decodedMessage instanceof RawMessage m) {
            invoke(c -> c.onRawMessage(session, m), "onRawMessage");
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void invoke(CallbackInvoker invoker, String method) {
        for (Gb32960Callback callback : callbacks) {
            if (asyncEnabled.get() && executor != null) {
                executor.submit(() -> safeInvoke(callback, invoker, method));
            } else {
                safeInvoke(callback, invoker, method);
            }
        }
    }

    private void safeInvoke(Gb32960Callback callback, CallbackInvoker invoker, String method) {
        try {
            invoker.invoke(callback);
        } catch (Exception e) {
            log.error("Callback [{}] threw exception: {}", method, e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface CallbackInvoker {
        void invoke(Gb32960Callback callback);
    }
}
