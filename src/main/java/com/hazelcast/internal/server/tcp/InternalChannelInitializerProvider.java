package com.hazelcast.internal.server.tcp;

import com.hazelcast.config.EndpointConfig;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.internal.server.ServerContext;

/**
 * The only way to create default ChannelInitializer's in class from different package
 * is to have them created in com.hazelcast.nio.tcp package :(
 */
public class InternalChannelInitializerProvider {

  public static ChannelInitializer provideMemberChannelInitializer(ServerContext serverContext, EndpointConfig endpointConfig) {
    return new MemberChannelInitializer(serverContext, endpointConfig);
  }


  public static ChannelInitializer provideClientChannelInitializer(ServerContext serverContext, EndpointConfig endpointConfig) {
    return new ClientChannelInitializer(serverContext, endpointConfig);
  }

}
