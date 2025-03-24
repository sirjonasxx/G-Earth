package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.crt.CertUtil;
import com.github.monkeywie.proxyee.server.HttpProxyCACertFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NitroCertificateFactory implements HttpProxyCACertFactory {

    private static final Logger log = LoggerFactory.getLogger(NitroCertificateFactory.class);

    private final File caCertFile;
    private final File caKeyFile;

    private X509Certificate caCert;
    private PrivateKey caKey;

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
}
