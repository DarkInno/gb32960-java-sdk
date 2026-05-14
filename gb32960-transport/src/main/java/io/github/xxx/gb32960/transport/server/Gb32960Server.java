package io.github.xxx.gb32960.transport.server;

import io.github.xxx.gb32960.auth.api.AuthProvider;
import io.github.xxx.gb32960.auth.provider.NoopAuthProvider;
import io.github.xxx.gb32960.callback.api.Gb32960Callback;
import io.github.xxx.gb32960.callback.dispatcher.CallbackDispatcher;
import io.github.xxx.gb32960.transport.handler.MessageHandler;
import io.github.xxx.gb32960.transport.pipeline.Gb32960Pipeline;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class Gb32960Server {

    private static final Logger log = LoggerFactory.getLogger(Gb32960Server.class);

    private final Config config;
    private final CallbackDispatcher dispatcher;
    private final AuthProvider authProvider;
    private final ConcurrentHashMap<String, Gb32960Session> sessions = new ConcurrentHashMap<>();
    private final LongAdder messagesReceived = new LongAdder();
    private final LongAdder messagesSent = new LongAdder();

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel channel;

    private Gb32960Server(Config config, CallbackDispatcher dispatcher, AuthProvider authProvider) {
        this.config = config;
        this.dispatcher = dispatcher;
        this.authProvider = authProvider;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(config.bossThreads);
        workerGroup = new NioEventLoopGroup(config.workerThreads);

        var bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        if (config.maxConnections > 0 && sessions.size() >= config.maxConnections) {
                            log.warn("Max connections reached ({}/{})", sessions.size(), config.maxConnections);
                            ch.close();
                            return;
                        }
                        var handler = new MessageHandler(authProvider, dispatcher, sessions,
                                messagesReceived, messagesSent);
                        Gb32960Pipeline.configure(ch.pipeline(), handler, config.idleTimeoutSeconds);
                    }
                });

        var future = bootstrap.bind(config.port).sync();
        channel = future.channel();
        log.info("GB32960 server started on port {}", config.port);
    }

    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        dispatcher.shutdown();
        log.info("GB32960 server stopped");
    }

    public int getActualPort() {
        if (channel != null && channel.localAddress() instanceof InetSocketAddress addr) {
            return addr.getPort();
        }
        return config.port;
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public int getMaxConnections() {
        return config.maxConnections;
    }

    public int getActiveVinCount() {
        var vins = new HashSet<String>();
        for (var session : sessions.values()) {
            var vin = session.vin();
            if (vin != null && !vin.isEmpty()) {
                vins.add(vin);
            }
        }
        return vins.size();
    }

    public long getMessagesReceived() {
        return messagesReceived.sum();
    }

    public long getMessagesSent() {
        return messagesSent.sum();
    }

    public Collection<Gb32960Session> getSessions() {
        return sessions.values();
    }

    public static Gb32960Server create(Consumer<Config> consumer) {
        var config = new Config();
        consumer.accept(config);
        return config.build();
    }

    public static class Config {

        int port = 8600;
        int bossThreads = 1;
        int workerThreads = Runtime.getRuntime().availableProcessors();
        int maxConnections = 100000;
        int idleTimeoutSeconds = 300;
        AuthProvider authProvider;
        List<? extends Gb32960Callback> callbacks = new ArrayList<>();
        CallbackDispatcher dispatcher;

        public Config port(int port) {
            this.port = port;
            return this;
        }

        public int getPort() { return port; }

        public Config bossThreads(int bossThreads) {
            this.bossThreads = bossThreads;
            return this;
        }

        public int getBossThreads() { return bossThreads; }

        public Config workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        public int getWorkerThreads() { return workerThreads; }

        public Config maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public int getMaxConnections() { return maxConnections; }

        public Config idleTimeoutSeconds(int idleTimeoutSeconds) {
            this.idleTimeoutSeconds = idleTimeoutSeconds;
            return this;
        }

        public int getIdleTimeoutSeconds() { return idleTimeoutSeconds; }

        public Config authProvider(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public AuthProvider getAuthProvider() { return authProvider; }

        public Config callbacks(List<? extends Gb32960Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        public List<? extends Gb32960Callback> getCallbacks() { return callbacks; }

        public Config dispatcher(CallbackDispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public CallbackDispatcher getDispatcher() { return dispatcher; }

        Gb32960Server build() {
            var disp = dispatcher != null ? dispatcher : new CallbackDispatcher();
            for (var callback : callbacks) {
                disp.register(callback);
            }
            var auth = authProvider != null ? authProvider : new NoopAuthProvider();
            return new Gb32960Server(this, disp, auth);
        }
    }
}
