package com.hazelcast.internal.server.tcp;


import com.hazelcast.config.EndpointConfig;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.instance.ProtocolType;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.networking.InboundHandler;
import com.hazelcast.internal.networking.OutboundHandler;
import com.hazelcast.internal.server.ServerConnection;
import com.hazelcast.internal.server.ServerContext;

import java.util.concurrent.Executor;

public class SSLMemberChannelInitializer extends MultiSocketSSLChannelInitializer {
    public SSLMemberChannelInitializer(final EndpointConfig endpointConfig, final Executor tlsExecutor, final ServerContext serverContext) {
        super(endpointConfig, tlsExecutor, serverContext);
    }

    protected void initPipeline(final Channel channel) {
        final ServerConnection connection = (ServerConnection) channel.attributeMap().get(ServerConnection.class);
        final OutboundHandler[] outboundHandlers = this.serverContext.createOutboundHandlers(EndpointQualifier.MEMBER, connection);
        final InboundHandler[] inboundHandlers = this.serverContext.createInboundHandlers(EndpointQualifier.MEMBER, connection);
        final MemberProtocolEncoder protocolEncoder = new MemberProtocolEncoder(outboundHandlers);
        final SingleProtocolDecoder protocolDecoder = new SingleProtocolDecoder(
                ProtocolType.MEMBER,
                inboundHandlers,
                protocolEncoder
        );
        channel.outboundPipeline().addLast(protocolEncoder);
        channel.inboundPipeline().addLast(protocolDecoder);
    }
}
