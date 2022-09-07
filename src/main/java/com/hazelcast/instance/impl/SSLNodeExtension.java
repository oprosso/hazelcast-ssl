package com.hazelcast.instance.impl;

import com.hazelcast.config.SSLConfig;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.internal.server.ServerContext;
import com.hazelcast.ssl.node.SslChannelInitializerFunction;

import java.util.function.Function;

public class SSLNodeExtension extends DefaultNodeExtension {
    public SSLNodeExtension(final Node node) {
        super(node);
    }

    @Override
    public Function<EndpointQualifier, ChannelInitializer> createChannelInitializerFn(ServerContext serverContext) {
        SSLConfig sslConfig = this.node.getConfig().getNetworkConfig().getSSLConfig();

        if (sslConfig != null && sslConfig.isEnabled()) {
            SslChannelInitializerFunction provider = new SslChannelInitializerFunction(serverContext, node);
            provider.init();
            return provider;
        }

        return super.createChannelInitializerFn(serverContext);
    }
}
