package io.github.darkinno.gb32960.transport.pipeline;

import io.github.darkinno.gb32960.core.codec.MessageDecoder;
import io.github.darkinno.gb32960.transport.handler.MessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Gb32960Pipeline {

    private static final Logger log = LoggerFactory.getLogger(Gb32960Pipeline.class);

    private Gb32960Pipeline() {
    }

    public static void configure(ChannelPipeline pipeline, MessageHandler messageHandler, int idleTimeoutSeconds) {
        if (idleTimeoutSeconds > 0) {
            pipeline.addLast(new IdleStateHandler(idleTimeoutSeconds, 0, 0, TimeUnit.SECONDS));
            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                    if (evt instanceof IdleStateEvent) {
                        log.warn("Channel idle timeout, closing: {}", ctx.channel().remoteAddress());
                        ctx.close();
                    }
                }
            });
        }
        pipeline.addLast(new Gb32960FrameDecoder());
        pipeline.addLast(messageHandler);
        pipeline.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                log.error("Unhandled exception in pipeline, remote={}", ctx.channel().remoteAddress(), cause);
                ctx.close();
            }
        });
    }

    static class Gb32960FrameDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            while (in.readableBytes() >= 24) {
                in.markReaderIndex();

                if (in.readByte() != 0x23 || in.readByte() != 0x23) {
                    in.resetReaderIndex();
                    in.skipBytes(1);
                    continue;
                }

                in.skipBytes(20);
                int dataLen = in.readUnsignedShort();
                in.resetReaderIndex();

                int totalLen = 24 + dataLen + 1;
                if (in.readableBytes() < totalLen) {
                    return;
                }

                var bytes = new byte[totalLen];
                in.readBytes(bytes);

                try {
                    out.add(MessageDecoder.decodeRaw(bytes));
                } catch (MessageDecoder.DecodeException e) {
                    log.warn("Frame decode failed: {}", e.getMessage());
                }
            }
        }
    }
}
