package com.hazelcast.internal.server.tcp;

import com.hazelcast.internal.networking.ChannelOption;
import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.OutboundHandler;
import com.hazelcast.internal.nio.IOUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.nio.ByteBuffer;

public class SSLEncoder extends OutboundHandler<ByteBuffer, ByteBuffer> {
  private final SSLEngine sslEngine;

  public SSLEncoder(SSLEngine sslEngine) {
    this.sslEngine = sslEngine;
  }

  public void handlerAdded() {
    int sendBufferSize = this.channel.options().getOption(ChannelOption.SO_SNDBUF);
    int packetBufferSize = this.sslEngine.getSession().getPacketBufferSize();
    this.initDstBuffer(Math.max(sendBufferSize, packetBufferSize));
  }

  public HandlerStatus onWrite() throws Exception {
    IOUtil.compactOrClear(this.dst);

    try {
      SSLEngineResult wrapResult = this.sslEngine.wrap(this.src, this.dst);
      HandlerStatus handlerStatus;
      switch (wrapResult.getStatus()) {
        case BUFFER_OVERFLOW:
          handlerStatus = HandlerStatus.DIRTY;
          return handlerStatus;
        case OK:
          if (this.src.remaining() > 0) {
            handlerStatus = HandlerStatus.DIRTY;
            return handlerStatus;
          }

          handlerStatus = HandlerStatus.CLEAN;
          return handlerStatus;
        case CLOSED:
          handlerStatus = HandlerStatus.CLEAN;
          return handlerStatus;
        default:
          throw new IllegalStateException("Unexpected " + wrapResult);
      }
    } finally {
      this.dst.flip();
    }
  }
}
