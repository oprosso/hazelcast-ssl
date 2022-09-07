package com.hazelcast.internal.server.tcp;

import com.hazelcast.internal.networking.ChannelOption;
import com.hazelcast.internal.networking.HandlerStatus;
import com.hazelcast.internal.networking.InboundHandler;
import com.hazelcast.internal.nio.IOUtil;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;

public class SSLDecoder extends InboundHandler<ByteBuffer, ByteBuffer> {
  private final SSLEngine sslEngine;
  private final SSLSession sslSession;
  private ByteBuffer appBuffer;

  public SSLDecoder(SSLEngine sslEngine) {
    this.sslEngine = sslEngine;
    this.sslSession = sslEngine.getSession();
  }

  public void handlerAdded() {
    int socketReceiveBuffer = this.channel.options().getOption(ChannelOption.SO_RCVBUF);
    int packetBufferSize = this.sslEngine.getSession().getPacketBufferSize();
    this.initSrcBuffer(Math.max(socketReceiveBuffer, packetBufferSize));
    this.appBuffer = IOUtil.newByteBuffer(this.sslSession.getApplicationBufferSize(), this.channel.options().getOption(ChannelOption.DIRECT_BUF));
  }

  public HandlerStatus onRead() throws Exception {
    if (!this.drainAppBuffer()) {
      return HandlerStatus.DIRTY;
    } else {
      this.src.flip();

      while (true) {
        try {
          while(true) {
            SSLEngineResult unwrapResult;
            try {
              unwrapResult = this.sslEngine.unwrap(this.src, this.appBuffer);
            } catch (SSLException e) {
              throw new SSLException(IOUtil.toDebugString("src", this.src) + " " + IOUtil.toDebugString("app", this.appBuffer) + " " + IOUtil.toDebugString("dst", this.dst), e);
            }

            HandlerStatus handlerStatus;
            switch (unwrapResult.getStatus()) {
              case BUFFER_OVERFLOW:
                if (this.appBuffer.capacity() >= this.sslSession.getApplicationBufferSize()) {
                  handlerStatus = HandlerStatus.DIRTY;
                  return handlerStatus;
                }

                this.appBuffer = this.newAppBuffer();
                break;
              case BUFFER_UNDERFLOW:
              case CLOSED:
                handlerStatus = HandlerStatus.CLEAN;
                return handlerStatus;
              case OK:
                if (this.drainAppBuffer()) {
                  if (this.src.remaining() != 0) {
                    break;
                  }

                  handlerStatus = HandlerStatus.CLEAN;
                  return handlerStatus;
                }

                handlerStatus = HandlerStatus.DIRTY;
                return handlerStatus;
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

  private ByteBuffer newAppBuffer() {
    return IOUtil.newByteBuffer(this.sslSession.getApplicationBufferSize(), this.channel.options().getOption(ChannelOption.DIRECT_BUF));
  }

  private boolean drainAppBuffer() {
    this.appBuffer.flip();
    int available = this.appBuffer.remaining();
    if (this.dst.remaining() < available) {
      int oldLimit = this.appBuffer.limit();
      this.appBuffer.limit(this.appBuffer.position() + this.dst.remaining());
      this.dst.put(this.appBuffer);
      this.appBuffer.limit(oldLimit);
    } else {
      this.dst.put(this.appBuffer);
    }

    if (this.appBuffer.hasRemaining()) {
      IOUtil.compactOrClear(this.appBuffer);
      return false;
    } else {
      this.appBuffer.clear();
      return true;
    }
  }
}
