package io.github.darkinno.gb32960.transport.handler;

import io.github.darkinno.gb32960.auth.api.AuthProvider;
import io.github.darkinno.gb32960.callback.dispatcher.CallbackDispatcher;
import io.github.darkinno.gb32960.core.codec.MessageDecoder;
import io.github.darkinno.gb32960.core.codec.MessageEncoder;
import io.github.darkinno.gb32960.core.constant.CommandFlag;
import io.github.darkinno.gb32960.core.constant.EncryptionType;
import io.github.darkinno.gb32960.core.constant.ResponseFlag;
import io.github.darkinno.gb32960.core.crypto.CryptoProvider;
import io.github.darkinno.gb32960.core.crypto.NoopCryptoProvider;
import io.github.darkinno.gb32960.core.model.PlatformLoginMessage;
import io.github.darkinno.gb32960.core.model.RawMessage;
import io.github.darkinno.gb32960.core.model.VehicleLoginMessage;
import io.github.darkinno.gb32960.transport.server.Gb32960Session;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class MessageHandler extends SimpleChannelInboundHandler<RawMessage> {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final AuthProvider authProvider;
    private final CryptoProvider cryptoProvider;
    private final CallbackDispatcher dispatcher;
    private final ConcurrentHashMap<String, Gb32960Session> sessions;
    private final LongAdder messagesReceived;
    private final LongAdder messagesSent;

    public MessageHandler(AuthProvider authProvider,
                          CryptoProvider cryptoProvider,
                          CallbackDispatcher dispatcher,
                          ConcurrentHashMap<String, Gb32960Session> sessions,
                          LongAdder messagesReceived,
                          LongAdder messagesSent) {
        this.authProvider = authProvider;
        this.cryptoProvider = cryptoProvider != null ? cryptoProvider : new NoopCryptoProvider();
        this.dispatcher = dispatcher;
        this.sessions = sessions;
        this.messagesReceived = messagesReceived;
        this.messagesSent = messagesSent;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        var sessionId = UUID.randomUUID().toString();
        var session = new Gb32960Session(sessionId, ctx);
        sessions.put(sessionId, session);
        ctx.channel().attr(SESSION_KEY).set(session);
        log.info("Session connected: id={}, remote={}", sessionId, session.remoteAddress());
        dispatcher.dispatchSessionConnected(session);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RawMessage raw) {
        var session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            log.warn("No session for channel");
            ctx.close();
            return;
        }

        messagesReceived.increment();

        var authResult = authProvider.authenticate(raw);
        if (!authResult.isPassed()) {
            log.warn("Auth failed for VIN={}: {}", raw.getVin(), authResult.getReason());
            if (raw.isCommand()) {
                var reply = MessageEncoder.buildResponse(raw, ResponseFlag.ERROR, null);
                ctx.writeAndFlush(Unpooled.wrappedBuffer(reply));
            }
            ctx.close();
            return;
        }

        if (raw.isCommand()) {
            autoReply(ctx, raw);
        }

        try {
            var decryptedRaw = decryptDataUnit(raw);
            var decoded = MessageDecoder.decode(decryptedRaw);
            dispatcher.dispatch(session, decoded);

            if (decoded instanceof VehicleLoginMessage) {
                session.setVin(raw.getVin());
            }
            if (decoded instanceof PlatformLoginMessage) {
                session.setVin(raw.getVin());
            }
        } catch (MessageDecoder.DecodeException e) {
            log.error("Decode failed for VIN={}: {}", raw.getVin(), e.getMessage());
            dispatcher.dispatch(session, raw);
        }
    }

    private void autoReply(ChannelHandlerContext ctx, RawMessage raw) {
        byte cmd = raw.getCommandFlag();
        switch (cmd) {
            case CommandFlag.VEHICLE_LOGIN:
            case CommandFlag.VEHICLE_LOGOUT:
            case CommandFlag.HEARTBEAT:
            case CommandFlag.REALTIME_REPORT:
            case CommandFlag.REISSUE_REPORT:
            case CommandFlag.PLATFORM_LOGIN: {
                var reply = MessageEncoder.buildResponse(raw, ResponseFlag.SUCCESS, null);
                ctx.writeAndFlush(Unpooled.wrappedBuffer(reply));
                messagesSent.increment();
                break;
            }
            case CommandFlag.TERMINAL_TIMING: {
                var now = LocalDateTime.now();
                var timeBytes = new byte[6];
                timeBytes[0] = intToBcd(now.getYear() % 100);
                timeBytes[1] = intToBcd(now.getMonthValue());
                timeBytes[2] = intToBcd(now.getDayOfMonth());
                timeBytes[3] = intToBcd(now.getHour());
                timeBytes[4] = intToBcd(now.getMinute());
                timeBytes[5] = intToBcd(now.getSecond());
                var reply = MessageEncoder.buildResponse(raw, ResponseFlag.SUCCESS, timeBytes);
                ctx.writeAndFlush(Unpooled.wrappedBuffer(reply));
                messagesSent.increment();
                break;
            }
            default:
                break;
        }
    }

    private static byte intToBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var session = ctx.channel().attr(SESSION_KEY).getAndSet(null);
        if (session != null) {
            sessions.remove(session.id());
            log.info("Session disconnected: id={}, vin={}", session.id(), session.vin());
            dispatcher.dispatchSessionDisconnected(session, null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception: remote={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    private RawMessage decryptDataUnit(RawMessage raw) {
        byte encType = raw.getEncryptionType();
        byte[] dataUnit = raw.getDataUnit();
        if (dataUnit != null && cryptoProvider.supports(encType) && encType != EncryptionType.NONE) {
            byte[] decrypted = cryptoProvider.decrypt(encType, dataUnit);
            raw.setDataUnit(decrypted);
            raw.setDataLength(decrypted != null ? decrypted.length : 0);
        }
        return raw;
    }

    public static final io.netty.util.AttributeKey<Gb32960Session> SESSION_KEY =
            io.netty.util.AttributeKey.valueOf("session");
}
