package com.hazelcast.internal.server.tcp;

import com.hazelcast.client.impl.protocol.util.ClientMessageDecoder;
import com.hazelcast.client.impl.protocol.util.ClientMessageEncoder;
import com.hazelcast.config.EndpointConfig;
import com.hazelcast.instance.ProtocolType;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.server.ServerConnection;
import com.hazelcast.internal.server.ServerContext;

import java.util.concurrent.Executor;

public class SSLClientChannelInitializer extends MultiSocketSSLChannelInitializer {
    public SSLClientChannelInitializer(final EndpointConfig endpointConfig, final Executor tlsExecutor, final ServerContext serverContext) {
        super(endpointConfig, tlsExecutor, serverContext);
    }

    protected void initPipeline(final Channel channel) {
        final ServerConnection connection = (ServerConnection) channel.attributeMap().get(ServerConnection.class);
        final SingleProtocolDecoder protocolDecoder = new SingleProtocolDecoder(ProtocolType.CLIENT, new ClientMessageDecoder(connection, this.serverContext.getClientEngine(), serverContext.properties()));
        channel.outboundPipeline().addLast(new ClientMessageEncoder());
        channel.inboundPipeline().addLast(protocolDecoder);
    }
}
