package com.softwareverde.http;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NaiveTrustManager extends X509ExtendedTrustManager {
    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType, final Socket socket) throws CertificateException { }

    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType, final Socket socket) throws CertificateException { }

    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType, final SSLEngine sslEngine) throws CertificateException { }

    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType, final SSLEngine sslEngine) throws CertificateException { }

    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType) throws CertificateException { }

    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType) throws CertificateException { }

    @Override
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
}
