package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.crt.CertUtil;
import com.github.monkeywie.proxyee.server.HttpProxyCACertFactory;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
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

import javax.net.ssl.SSLEngine;
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

public class NitroCertificateFactory implements HttpProxyCACertFactory {

    private static final Logger log = LoggerFactory.getLogger(NitroCertificateFactory.class);

    private final File caCertFile;
    private final File caKeyFile;

    private X509Certificate caCert;
    private PrivateKey caKey;
    private HttpProxyServerConfig config;

    public NitroCertificateFactory() {
        this.caCertFile = new File(String.format("./%s.crt", NitroAuthority.CERT_ALIAS));
        this.caKeyFile = new File(String.format("./%s.key", NitroAuthority.CERT_ALIAS));
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
                    NitroAuthority.CERT_ORGANIZATION,
                    NitroAuthority.CERT_DESCRIPTION);

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

    public void setServerConfig(HttpProxyServerConfig config) {
        this.config = config;
    }

    public SSLEngine websocketSslEngine(String commonName) {
        if (this.config == null) {
            throw new IllegalStateException("Server config not set");
        }

        try {
            final X509Certificate cert = generateServerCert(commonName,
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1"));

            final SslContext ctx = SslContextBuilder.forServer(this.config.getServerPriKey(), cert).build();

            return ctx.newEngine(ByteBufAllocator.DEFAULT);
        } catch (Exception e) {
            log.error("Failed to create SSLEngine", e);
            return null;
        }
    }

    private X509Certificate generateServerCert(String commonName, GeneralName... san) throws Exception {
        final String issuer = this.config.getIssuer();
        final PrivateKey caPriKey = this.config.getCaPriKey();
        final Date caNotBefore = this.config.getCaNotBefore();
        final Date caNotAfter = this.config.getCaNotAfter();
        final PublicKey serverPubKey = this.config.getServerPubKey();

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
                caNotBefore,
                caNotAfter,
                new X500Name(subject),
                serverPubKey);

        jv3Builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(san));

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);

        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }
}
