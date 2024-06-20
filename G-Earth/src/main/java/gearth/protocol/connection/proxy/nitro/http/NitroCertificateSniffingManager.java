package gearth.protocol.connection.proxy.nitro.http;

import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.mitm.*;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * {@link MitmManager} that uses the common name and subject alternative names
 * from the upstream certificate to create a dynamic certificate with it.
 */
public class NitroCertificateSniffingManager implements MitmManager {

    private static final boolean DEBUG = false;

    private final BouncyCastleSslEngineSource sslEngineSource;
    private final Authority authority;

    public NitroCertificateSniffingManager(Authority authority) {
        this.authority = authority;
        try {
            sslEngineSource = new BouncyCastleSslEngineSource(authority, true, true, null);
        } catch (final Exception e) {
            throw new RuntimeException(new RootCertificateException("Errors during assembling root CA.", e));
        }
    }

    public Authority getAuthority() {
        return authority;
    }

    public SSLEngine websocketSslEngine(String commonName) {
        final SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();

        san.addDomainName("localhost");
        san.addIpAddress("127.0.0.1");

        try {
            return sslEngineSource.createCertForHost(commonName, san);
        } catch (Exception e) {
            throw new FakeCertificateException("Failed to create WebSocket certificate", e);
        }
    }

    public SSLEngine serverSslEngine(String peerHost, int peerPort) {
        return sslEngineSource.newSslEngine(peerHost, peerPort);
    }

    public SSLEngine serverSslEngine() {
        return sslEngineSource.newSslEngine();
    }

    public SSLEngine clientSslEngineFor(HttpRequest httpRequest, SSLSession serverSslSession) {
        try {
            X509Certificate upstreamCert = getCertificateFromSession(serverSslSession);
            // TODO store the upstream cert by commonName to review it later

            // A reasons to not use the common name and the alternative names
            // from upstream certificate from serverSslSession to create the
            // dynamic certificate:
            //
            // It's not necessary. The host name is accepted by the browser.
            //
            String commonName = getCommonName(upstreamCert);

            SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();

            san.addAll(upstreamCert.getSubjectAlternativeNames());

            if (DEBUG) {
                System.out.println("[NitroCertificateSniffingManager] Subject Alternative Names");

                for (List<?> name : upstreamCert.getSubjectAlternativeNames()) {
                    System.out.printf("[NitroCertificateSniffingManager] - %s%n", name.toString());
                }
            }

            return sslEngineSource.createCertForHost(commonName, san);
        } catch (Exception e) {
            throw new FakeCertificateException("Creation dynamic certificate failed", e);
        }
    }

    private X509Certificate getCertificateFromSession(SSLSession sslSession) throws SSLPeerUnverifiedException {
        Certificate[] peerCerts = sslSession.getPeerCertificates();
        Certificate peerCert = peerCerts[0];
        if (peerCert instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) peerCert;
        }
        throw new IllegalStateException("Required java.security.cert.X509Certificate, found: " + peerCert);
    }

    private String getCommonName(X509Certificate c) {
        if (DEBUG) {
            System.out.printf("[NitroCertificateSniffingManager] Subject DN principal name: %s%n", c.getSubjectDN().getName());
        }

        for (String each : c.getSubjectDN().getName().split(",\\s*")) {
            if (each.startsWith("CN=")) {
                String result = each.substring(3);

                if (DEBUG) {
                    System.out.printf("[NitroCertificateSniffingManager] Common Name: %s%n", c.getSubjectDN().getName());
                }

                return result;
            }
        }

        throw new IllegalStateException("Missed CN in Subject DN: " + c.getSubjectDN());
    }
}