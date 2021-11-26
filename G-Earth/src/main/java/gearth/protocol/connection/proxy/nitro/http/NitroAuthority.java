package gearth.protocol.connection.proxy.nitro.http;

import org.littleshoot.proxy.mitm.Authority;

import java.io.File;

public class NitroAuthority extends Authority {

    private static final String CERT_ALIAS = "gearth-nitro";
    private static final String CERT_ORGANIZATION = "G-Earth Nitro";
    private static final String CERT_DESCRIPTION = "G-Earth nitro support";

    public NitroAuthority() {
        super(new File("."),
                CERT_ALIAS,
                "verysecure".toCharArray(),
                CERT_DESCRIPTION,
                CERT_ORGANIZATION,
                "Certificate Authority",
                CERT_ORGANIZATION,
                CERT_DESCRIPTION);
    }

}
