package com.guidewire.tarot;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class LDAPSocketFactory extends SSLSocketFactory {
  public static final SocketFactory DEFAULT = new LDAPSocketFactory();

  public static final TrustManager[] BLINDLY_TRUST_EVERYONE = new TrustManager[]{
      new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      }
  };

  public static SocketFactory getDefault() {
    return new LDAPSocketFactory();
  }

  public static String factoryName() {
    return LDAPSocketFactory.class.getName();
  }

  private SSLSocketFactory factory;

  private LDAPSocketFactory() {
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, BLINDLY_TRUST_EVERYONE, new SecureRandom());
      factory = ctx.getSocketFactory();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return factory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return factory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
    return factory.createSocket(socket, string, i, bln);
  }

  @Override
  public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
    return factory.createSocket(string, i);
  }

  @Override
  public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
    return factory.createSocket(string, i, ia, i1);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i) throws IOException {
    return factory.createSocket(ia, i);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
    return factory.createSocket(ia, i, ia1, i1);
  }
}
