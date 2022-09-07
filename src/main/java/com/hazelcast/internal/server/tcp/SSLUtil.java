package com.hazelcast.internal.server.tcp;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.util.Map;

public class SSLUtil {
  private static final Certificate[] EMPTY_CERTS = new Certificate[0];

  private SSLUtil() {
  }

  static void publishRemoteCertificates(SSLEngine sslEngine, Map attributeMap) {
    if (sslEngine.getUseClientMode() || sslEngine.getNeedClientAuth() || sslEngine.getWantClientAuth()) {
      Certificate[] certs;
      try {
        certs = sslEngine.getSession().getPeerCertificates();
      } catch (SSLPeerUnverifiedException e) {
        certs = EMPTY_CERTS;
      }

      attributeMap.putIfAbsent(Certificate.class, certs);
    }
  }
}
