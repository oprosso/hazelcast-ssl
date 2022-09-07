package com.hazelcast.internal.server.tcp;

import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.OutboundHandler;
import com.hazelcast.internal.nio.IOUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

public class SSLOutboundHandler extends OutboundHandler<Void, ByteBuffer> {
  private final SSLEngine sslEngine;
  private final ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
  private final SSLExecutor tlsExecutor;
  private final ConcurrentMap attributeMap;

  public SSLOutboundHandler(SSLEngine sslEngine, SSLExecutor tlsExecutor, ConcurrentMap attributeMap) {
    this.sslEngine = sslEngine;
    this.tlsExecutor = tlsExecutor;
    this.attributeMap = attributeMap;
  }

  public void handlerAdded() {
    this.initDstBuffer(this.sslEngine.getSession().getPacketBufferSize());
  }

  public void interceptError(Throwable t) throws Throwable {
    if (t instanceof EOFException) {
      throw SSLInboundHandler.newSSLException(t);
    }
  }

  public HandlerStatus onWrite() throws Exception {
    IOUtil.compactOrClear(this.dst);

    while(true) {
      try {
        while(true) {
          SSLEngineResult.HandshakeStatus handshakeStatus = this.sslEngine.getHandshakeStatus();
          switch (handshakeStatus) {
            case FINISHED:
              break;
            case NEED_TASK:
              this.tlsExecutor.executeHandshakeTasks(this.sslEngine, this.channel);
              return HandlerStatus.BLOCKED;
            case NEED_WRAP:
              SSLEngineResult wrapResult = this.sslEngine.wrap(this.emptyBuffer, this.dst);
              SSLEngineResult.Status wrapResultStatus = wrapResult.getStatus();
              if (wrapResultStatus == SSLEngineResult.Status.OK) {
                break;
              }

              if (wrapResultStatus != SSLEngineResult.Status.BUFFER_OVERFLOW) {
                if (wrapResultStatus == SSLEngineResult.Status.CLOSED) {
                  return HandlerStatus.CLEAN;
                }

                throw new IllegalStateException("Unexpected wrapResult:" + wrapResult);
              }

              return HandlerStatus.DIRTY;
            case NEED_UNWRAP:
              this.channel.inboundPipeline().wakeup();
              return HandlerStatus.BLOCKED;
            case NOT_HANDSHAKING:
              if (!this.isTlsHandshakeBufferDrained()) {
                return HandlerStatus.DIRTY;
              }

              SSLUtil.publishRemoteCertificates(this.sslEngine, this.attributeMap);
              this.channel.outboundPipeline().replace(this, new SSLEncoder(this.sslEngine));
              this.channel.inboundPipeline().wakeup();
              return HandlerStatus.CLEAN;
            default:
              throw new IllegalStateException();
          }
        }
      } finally {
        this.dst.flip();
      }
    }
  }

  private boolean isTlsHandshakeBufferDrained() {
    return this.dst.position() == 0;
  }
}
