package io.github.xxx.gb32960.transport.server;

import io.github.xxx.gb32960.callback.api.Session;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

public class Gb32960Session implements Session {

    private final String id;
    private final ChannelHandlerContext ctx;
    private String vin;

    public Gb32960Session(String id, ChannelHandlerContext ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String vin() {
        return vin;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) ctx.channel().remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return ctx.channel().isActive();
    }

    @Override
    public void send(byte[] data) {
        ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
    }

    @Override
    public void close() {
        ctx.close();
    }
}
