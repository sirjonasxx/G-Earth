package gearth.protocol.connection.proxy.http;

import com.github.monkeywie.proxyee.crt.CertUtil;
import com.github.monkeywie.proxyee.server.HttpProxyCACertFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpProxyCertificateFactory implements HttpProxyCACertFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyCertificateFactory.class);

    private final File caCertFile;
    private final File caKeyFile;

    private X509Certificate caCert;
    private PrivateKey caKey;

    public HttpProxyCertificateFactory() {
        final String overrideDataDir = System.getProperty("gearth.data.dir");

        if (overrideDataDir != null) {
            this.caCertFile = new File(overrideDataDir, String.format("%s.crt", HttpProxyAuthority.CERT_ALIAS));
            this.caKeyFile = new File(overrideDataDir, String.format("%s.key", HttpProxyAuthority.CERT_ALIAS));
        } else {
            this.caCertFile = new File(String.format("./%s.crt", HttpProxyAuthority.CERT_ALIAS));
            this.caKeyFile = new File(String.format("./%s.key", HttpProxyAuthority.CERT_ALIAS));
        }
    }

    public File getCaCertFile() {
        return caCertFile;
    }

    public File getCaKeyFile() {
        return caKeyFile;
    }

    public boolean loadOrCreate() {
        if (this.caCertFile.exists() && this.caKeyFile.exists()) {
            return this.loadCertificate();
        }

        // Delete any existing files
        if (caCertFile.exists()) {
            caCertFile.delete();
        }

        if (caKeyFile.exists()) {
            caKeyFile.delete();
        }

        // Create the certificate and key files
        return this.createCertificate();
    }

    private boolean createCertificate() {
        try {
            final KeyPair keyPair = CertUtil.genKeyPair();

            final String subject = String.format("O=%s, OU=Certificate Authority, CN=%s",
                    HttpProxyAuthority.CERT_ORGANIZATION,
                    HttpProxyAuthority.CERT_DESCRIPTION);

            final X509Certificate rootCertificate = CertUtil.genCACert(subject,
                    new Date(),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
                    keyPair);

            this.caCert = rootCertificate;
            this.caKey = keyPair.getPrivate();

            Files.write(Paths.get(this.caCertFile.toURI()), this.caCert.getEncoded());
            Files.write(Paths.get(this.caKeyFile.toURI()), new PKCS8EncodedKeySpec(this.caKey.getEncoded()).getEncoded());

            return true;
        } catch (Exception e) {
            log.error("Failed to create root certificate", e);
        }

        return false;
    }

    private boolean loadCertificate() {
        try {
            this.caCert = CertUtil.loadCert(this.caCertFile.toURI());
            this.caKey = CertUtil.loadPriKey(this.caKeyFile.toURI());

            return true;
        } catch (Exception e) {
            log.error("Failed to load root certificate", e);
        }

        return false;
    }

    @Override
    public X509Certificate getCACert() {
        return this.caCert;
    }

    @Override
    public PrivateKey getCAPriKey() {
        return this.caKey;
    }

    public SslHandler createSslHandler(final String commonName) {
        if (this.caCert == null) {
            throw new IllegalStateException("CA certificate not loaded");
        }

        if (this.caKey == null) {
            throw new IllegalStateException("CA private key not loaded");
        }

        try {
            final KeyPair keyPair = CertUtil.genKeyPair();

            final X509Certificate cert = generateServerCert(keyPair.getPublic(),
                    commonName,
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1"));

            final SslContext ctx = SslContextBuilder.forServer(keyPair.getPrivate(), cert).build();

            return ctx.newHandler(ByteBufAllocator.DEFAULT);
        } catch (Exception e) {
            log.error("Failed to create SSLHandler", e);
            return null;
        }
    }

    private X509Certificate generateServerCert(final PublicKey serverPubKey, final String commonName, final GeneralName... san) throws Exception {
        final String issuer = CertUtil.getSubject(this.caCert);
        final PrivateKey caPriKey = this.getCAPriKey();

        // Replace "CN" in cert authority
        final String subject = Stream.of(issuer.split(", ")).map(item -> {
            String[] arr = item.split("=");
            if ("CN".equals(arr[0])) {
                return "CN=" + commonName;
            } else {
                return item;
            }
        }).collect(Collectors.joining(", "));

        final JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
                this.caCert.getNotBefore(),
                this.caCert.getNotAfter(),
                new X500Name(subject),
                serverPubKey);

        jv3Builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(san));

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);

        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }
}
