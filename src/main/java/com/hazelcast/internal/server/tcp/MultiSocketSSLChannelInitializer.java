package com.hazelcast.internal.server.tcp;

import com.hazelcast.config.EndpointConfig;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.nio.IOUtil;
import com.hazelcast.internal.server.ServerContext;

import java.util.concurrent.Executor;

public abstract class MultiSocketSSLChannelInitializer extends SSLChannelInitializer {
    protected final EndpointConfig config;
    protected final ServerContext serverContext;

    public MultiSocketSSLChannelInitializer(final EndpointConfig endpointConfig, final Executor sslExecutor, final ServerContext serverContext) {
        super(endpointConfig.getSSLConfig(), sslExecutor);
        this.config = endpointConfig;
        this.serverContext = serverContext;
    }

    protected boolean forClient() {
        return false;
    }

    protected void configChannel(final Channel channel) {
        IOUtil.setChannelOptions(channel, this.config);
    }
}
