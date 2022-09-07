package com.hazelcast.ssl.client;

import com.hazelcast.client.config.SocketOptions;
import com.hazelcast.client.impl.clientside.DefaultClientExtension;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.internal.util.ConcurrencyUtil;

import java.util.concurrent.Executor;

public class SSLClientExtension extends DefaultClientExtension {
  @Override
  public ChannelInitializer createChannelInitializer(SSLConfig sslConfig, SocketOptions socketOptions) {
    if (sslConfig == null || !sslConfig.isEnabled()) {
      return super.createChannelInitializer(sslConfig, socketOptions);
    }

    Executor executor = ConcurrencyUtil.getDefaultAsyncExecutor();
    return new SSLClientSideChannelInitializer(client, sslConfig, socketOptions, executor);
  }
}
