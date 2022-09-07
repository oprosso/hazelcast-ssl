package com.hazelcast.internal.server.tcp;


import com.hazelcast.config.SSLConfig;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.networking.ChannelOption;
import com.hazelcast.spi.properties.ClusterProperty;
import com.hazelcast.spi.properties.HazelcastProperties;

import java.util.concurrent.Executor;
import java.util.function.Function;

public class SSLUnifiedChannelInitializer extends SSLChannelInitializer {

    private final Function<Channel, Handlers<UnifiedProtocolDecoder, UnifiedProtocolEncoder>> handlerProvider;

    private final HazelcastProperties props;

    public SSLUnifiedChannelInitializer(final SSLConfig sslConfig,
                                        final HazelcastProperties props,
                                        final Executor executor,
                                        final Function<Channel, Handlers<UnifiedProtocolDecoder, UnifiedProtocolEncoder>> handlerProvider) {
        super(sslConfig, executor);
        this.handlerProvider = handlerProvider;
        this.props = props;
    }

    @Override
    protected boolean forClient() {
        return false;
    }

    @Override
    protected void initPipeline(final Channel channel) {
        final Handlers<UnifiedProtocolDecoder, UnifiedProtocolEncoder> pair = this.handlerProvider.apply(channel);

        channel.inboundPipeline().addLast(pair.getInboundHandler());
        channel.outboundPipeline().addLast(pair.getOutboundHandler());
    }

    @Override
    protected void configChannel(final Channel channel) {
        channel.options().setOption(ChannelOption.DIRECT_BUF, this.props.getBoolean(ClusterProperty.SOCKET_BUFFER_DIRECT))
                .setOption(ChannelOption.TCP_NODELAY, this.props.getBoolean(ClusterProperty.SOCKET_NO_DELAY))
                .setOption(ChannelOption.SO_KEEPALIVE, this.props.getBoolean(ClusterProperty.SOCKET_KEEP_ALIVE))
                .setOption(ChannelOption.SO_SNDBUF, this.props.getInteger(ClusterProperty.SOCKET_SEND_BUFFER_SIZE) * 1024)
                .setOption(ChannelOption.SO_RCVBUF, this.props.getInteger(ClusterProperty.SOCKET_RECEIVE_BUFFER_SIZE) * 1024)
                .setOption(ChannelOption.SO_LINGER, this.props.getSeconds(ClusterProperty.SOCKET_LINGER_SECONDS));
    }
}

