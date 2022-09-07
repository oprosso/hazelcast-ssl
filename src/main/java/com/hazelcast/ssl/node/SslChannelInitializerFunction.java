/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.ssl.node;

import com.hazelcast.config.*;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.internal.nio.ascii.TextChannelInitializer;
import com.hazelcast.internal.server.ServerContext;
import com.hazelcast.internal.server.tcp.*;
import com.hazelcast.logging.ILogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class SslChannelInitializerFunction implements Function<EndpointQualifier, ChannelInitializer> {

    private final ServerContext serverContext;
    private final ChannelInitializer uniChannelInitializer;
    private final Config config;
    private final Map<EndpointQualifier, ChannelInitializer> initializerMap = new HashMap<>();
    private final ChannelInitializer tlsChannelInitializer;
    private final ILogger logger;
    private final Node node;
    private final boolean unifiedSslEnabled;
    private final Executor sslExecutor;


    public SslChannelInitializerFunction(final ServerContext serverContext, final Node node) {
        this.serverContext = serverContext;
        this.uniChannelInitializer = new UnifiedChannelInitializer(serverContext);
        this.config = node.getConfig();
        this.node = node;
        this.logger = serverContext.getLoggingService()
                .getLogger(SslChannelInitializerFunction.class);
        this.unifiedSslEnabled = this.unifiedSslEnabled();
        this.sslExecutor = node.nodeEngine.getExecutionService().getGlobalTaskScheduler();
        this.tlsChannelInitializer = this.createUnifiedTlsChannelInitializer();
    }

    public void init() {
        final AdvancedNetworkConfig advancedNetworkConfig = config.getAdvancedNetworkConfig();
        if (!advancedNetworkConfig.isEnabled()
                || advancedNetworkConfig.getEndpointConfigs().isEmpty()) {
            return;
        }

        for (final EndpointConfig endpointConfig : advancedNetworkConfig.getEndpointConfigs().values()) {

            switch (endpointConfig.getProtocolType()) {
                case MEMBER:
                    initializerMap.put(EndpointQualifier.MEMBER, provideMemberChannelInitializer(endpointConfig));
                    break;
                case CLIENT:
                    initializerMap.put(EndpointQualifier.CLIENT, provideClientChannelInitializer(endpointConfig));
                    break;
                case REST:
                    initializerMap.put(EndpointQualifier.REST, provideTextChannelInitializer(endpointConfig, true));
                    break;
                case MEMCACHE:
                    initializerMap.put(EndpointQualifier.MEMCACHE, provideTextChannelInitializer(endpointConfig, false));
                    break;
                case WAN:
                    initializerMap.put(endpointConfig.getQualifier(), provideMemberChannelInitializer(endpointConfig));
                    break;
                default:
                    throw new IllegalStateException("Cannot build channel initializer for protocol type "
                            + endpointConfig.getProtocolType());
            }
        }
    }


    private ChannelInitializer provideMemberChannelInitializer(final EndpointConfig endpointConfig) {
        if (this.endpointSslEnabled(endpointConfig)) {
            return new SSLMemberChannelInitializer(endpointConfig, this.sslExecutor, this.serverContext);
        } else {
            return InternalChannelInitializerProvider.provideMemberChannelInitializer(serverContext, endpointConfig);
        }
    }

    private ChannelInitializer provideClientChannelInitializer(final EndpointConfig endpointConfig) {
        if (this.endpointSslEnabled(endpointConfig)) {
            return new SSLClientChannelInitializer(endpointConfig, this.sslExecutor, this.serverContext);
        } else {
            return InternalChannelInitializerProvider.provideClientChannelInitializer(serverContext, endpointConfig);
        }
    }

    private ChannelInitializer provideTextChannelInitializer(final EndpointConfig endpointConfig, final boolean rest) {
        if (this.endpointSslEnabled(endpointConfig)) {
            return new SSLTextChannelInitializer(endpointConfig, this.sslExecutor, this.serverContext, rest);
        } else {
            return new TextChannelInitializer(serverContext, endpointConfig, rest);
        }
    }

    protected ChannelInitializer provideWanChannelInitializer(final EndpointConfig endpointConfig) {
        if (this.endpointSslEnabled(endpointConfig)) {
            return this.tlsChannelInitializer;
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    private ChannelInitializer provideUnifiedChannelInitializer() {
        return this.unifiedSslEnabled ? this.tlsChannelInitializer : uniChannelInitializer;
    }

    private ChannelInitializer createUnifiedTlsChannelInitializer() {
        final NetworkConfig networkConfig = this.node.getConfig().getNetworkConfig();
        final SSLConfig sslConfig = networkConfig.getSSLConfig();
        if (this.unifiedSslEnabled) {
            final SymmetricEncryptionConfig symmetricEncryptionConfig = networkConfig.getSymmetricEncryptionConfig();
            if (symmetricEncryptionConfig != null && symmetricEncryptionConfig.isEnabled()) {
                throw new RuntimeException("SSL and SymmetricEncryption cannot be both enabled!");
            } else {
                this.logger.info("SSL is enabled");

                return new SSLUnifiedChannelInitializer(sslConfig, this.node.getProperties(), this.sslExecutor, (channel) -> {
                    final UnifiedProtocolEncoder encoder = new UnifiedProtocolEncoder(this.serverContext);
                    final UnifiedProtocolDecoder decoder = new UnifiedProtocolDecoder(this.serverContext, encoder);
                    return new Handlers<>(decoder, encoder);
                });
            }
        } else {
            return null;
        }
    }

    private boolean endpointSslEnabled(final EndpointConfig endpointConfig) {
        return endpointConfig != null && endpointConfig.getSSLConfig() != null && endpointConfig.getSSLConfig()
                .isEnabled();
    }

    private boolean unifiedSslEnabled() {
        final SSLConfig sslConfig = this.node.getConfig().getNetworkConfig().getSSLConfig();
        return sslConfig != null && sslConfig.isEnabled();
    }

    @Override
    public ChannelInitializer apply(EndpointQualifier qualifier) {
        return initializerMap.isEmpty() ? provideUnifiedChannelInitializer() : initializerMap.get(qualifier);
    }
}
