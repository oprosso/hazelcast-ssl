package com.hazelcast.internal.server.tcp;

import com.hazelcast.config.EndpointConfig;
import com.hazelcast.instance.ProtocolType;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.nio.ascii.MemcacheTextDecoder;
import com.hazelcast.internal.nio.ascii.RestApiTextDecoder;
import com.hazelcast.internal.nio.ascii.TextDecoder;
import com.hazelcast.internal.nio.ascii.TextEncoder;
import com.hazelcast.internal.server.ServerContext;

import java.util.concurrent.Executor;

public class SSLTextChannelInitializer extends MultiSocketSSLChannelInitializer {
    private final boolean rest;

    public SSLTextChannelInitializer(final EndpointConfig endpointConfig, final Executor tlsExecutor, final ServerContext serverContext, final boolean rest) {
        super(endpointConfig, tlsExecutor, serverContext);
        this.rest = rest;
    }

    protected void initPipeline(final Channel channel) {
        final TcpServerConnection connection = (TcpServerConnection) channel.attributeMap().get(TcpServerConnection.class);
        final TextEncoder encoder = new TextEncoder(connection);
        final TextDecoder decoder = this.rest ? new RestApiTextDecoder(connection, encoder, true) : new MemcacheTextDecoder(connection, encoder, true);
        channel.outboundPipeline().addLast(encoder);
        channel.inboundPipeline().addLast(new TextHandshakeDecoder(this.rest ? ProtocolType.REST : ProtocolType.MEMCACHE, decoder));
    }
}
