package com.hazelcast.internal.server.tcp;

import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.InboundHandler;
import com.hazelcast.internal.nio.IOUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

public class SSLInboundHandler extends InboundHandler<ByteBuffer, Void> {
  private final SSLEngine sslEngine;
  private final SSLExecutor tlsExecutor;
  private final ByteBuffer appBuffer;
  private final ConcurrentMap attributeMap;

  public SSLInboundHandler(SSLEngine sslEngine, SSLExecutor tlsExecutor, ConcurrentMap attributeMap) {
    this.sslEngine = sslEngine;
    this.tlsExecutor = tlsExecutor;
    this.appBuffer = IOUtil.newByteBuffer(sslEngine.getSession().getApplicationBufferSize(), false);
    this.attributeMap = attributeMap;
  }

  public void handlerAdded() {
    this.initSrcBuffer(this.sslEngine.getSession().getPacketBufferSize());
  }

  public void interceptError(Throwable t) throws Throwable {
    if (t instanceof EOFException) {
      throw newSSLException(t);
    }
  }

  static SSLException newSSLException(Throwable t) {
    return new SSLException("Remote socket closed during SSL/TLS handshake.  This is probably caused by a SSL/TLS authentication problem resulting in the remote side closing the socket.", t);
  }

  public HandlerStatus onRead() throws Exception {
    this.src.flip();

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
              this.channel.outboundPipeline().wakeup();
              return HandlerStatus.BLOCKED;
            case NEED_UNWRAP:
              SSLEngineResult unwrapResult = this.sslEngine.unwrap(this.src, this.appBuffer);
              SSLEngineResult.Status unwrapStatus = unwrapResult.getStatus();
              if (unwrapStatus == SSLEngineResult.Status.OK) {
                break;
              }

              if (unwrapStatus != SSLEngineResult.Status.CLOSED) {
                if (unwrapStatus != SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                  throw new IllegalStateException("Unexpected " + unwrapResult);
                }

                if (this.sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                  break;
                }

                return HandlerStatus.CLEAN;
              }

              return HandlerStatus.CLEAN;
            case NOT_HANDSHAKING:
              if (this.appBuffer.position() != 0) {
                throw new IllegalStateException("Unexpected data in the appBuffer, it should be empty " + IOUtil.toDebugString("appBuffer", this.appBuffer));
              }

              SSLUtil.publishRemoteCertificates(this.sslEngine, this.attributeMap);
              SSLDecoder tlsDecoder = new SSLDecoder(this.sslEngine);
              this.channel.inboundPipeline().replace(this, tlsDecoder);
              tlsDecoder.src().put(this.src);
              this.channel.outboundPipeline().wakeup();
              return HandlerStatus.DIRTY;
            default:
              throw new IllegalStateException();
          }
        }
      } finally {
        IOUtil.compactOrClear(this.src);
      }
    }
  }
}
