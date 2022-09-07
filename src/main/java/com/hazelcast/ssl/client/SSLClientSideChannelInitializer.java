package com.hazelcast.ssl.client;

import com.hazelcast.client.config.SocketOptions;
import com.hazelcast.client.impl.clientside.HazelcastClientInstanceImpl;
import com.hazelcast.client.impl.connection.tcp.ClientProtocolEncoder;
import com.hazelcast.client.impl.connection.tcp.TcpClientConnection;
import com.hazelcast.client.impl.protocol.util.ClientMessageDecoder;
import com.hazelcast.client.impl.protocol.util.ClientMessageEncoder;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.internal.networking.Channel;
import com.hazelcast.internal.server.tcp.SSLChannelInitializer;

import java.util.concurrent.Executor;

import static com.hazelcast.client.config.SocketOptions.KILO_BYTE;
import static com.hazelcast.internal.networking.ChannelOption.*;
import static com.hazelcast.spi.properties.ClusterProperty.SOCKET_CLIENT_BUFFER_DIRECT;

public class SSLClientSideChannelInitializer extends SSLChannelInitializer {
  private SocketOptions socketOptions;
  private HazelcastClientInstanceImpl client;

  public SSLClientSideChannelInitializer(
          HazelcastClientInstanceImpl client,
          SSLConfig sslConfig,
          SocketOptions socketOptions,
          Executor executor
  ) {
    super(sslConfig, executor);
    this.client = client;
    this.socketOptions = socketOptions;
  }

  @Override
  protected boolean forClient() {
    return true;
  }

  @Override
  protected void initPipeline(Channel channel) {
    final TcpClientConnection connection = (TcpClientConnection) channel.attributeMap().get(TcpClientConnection.class);
    ClientMessageDecoder decoder = new ClientMessageDecoder(connection, connection::handleClientMessage, null);

    channel.inboundPipeline().addLast(decoder);
    channel.outboundPipeline().addLast(new ClientMessageEncoder());
    channel.outboundPipeline().addLast(new ClientProtocolEncoder());
  }

  @Override
  protected void configChannel(Channel channel) {
    channel.options()
            .setOption(SO_SNDBUF, KILO_BYTE * socketOptions.getBufferSize())
            .setOption(SO_RCVBUF, KILO_BYTE * socketOptions.getBufferSize())
            .setOption(SO_REUSEADDR, socketOptions.isReuseAddress())
            .setOption(SO_KEEPALIVE, socketOptions.isKeepAlive())
            .setOption(SO_LINGER, socketOptions.getLingerSeconds())
            .setOption(SO_TIMEOUT, 0)
            .setOption(TCP_NODELAY, socketOptions.isTcpNoDelay())
            .setOption(DIRECT_BUF,  client.getProperties().getBoolean(SOCKET_CLIENT_BUFFER_DIRECT));

  }
}
