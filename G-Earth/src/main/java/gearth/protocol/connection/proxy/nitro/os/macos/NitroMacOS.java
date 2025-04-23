package gearth.protocol.connection.proxy.nitro.os.macos;

import gearth.misc.RuntimeUtil;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NitroMacOS implements NitroOsFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(NitroMacOS.class);

    /**
     * Semicolon separated hosts to ignore for proxying.
     */
    private static final String PROXY_IGNORE = "discord.com;discordapp.com;canary.discord.com;canary.discordapp.com;github.com;gateway.discord.gg;";

    /**
     * Checks if the certificate is trusted by the local machine.
     * @param certificate Absolute path to the certificate.
     * @return true if trusted
     */
    @Override
    public boolean isRootCertificateTrusted(File certificate) {
        try {
            final String certificatePath = certificate.getCanonicalFile().getAbsolutePath();
            final String output = RuntimeUtil.getCommandOutput(new String[] {"sh", "-c", "security verify-cert -c \"" + certificatePath + "\""});

            return !output.contains("CSSMERR_TP_NOT_TRUSTED");
        } catch (IOException e) {
            LOG.error("Error while checking certificate trust", e);
        }

        return false;
    }

    @Override
    public boolean installRootCertificate(File certificate) {
        try {
            final String certificatePath = certificate.getCanonicalFile().getAbsolutePath();

            // Create shell script.
            final Path scriptPath = Files.createTempFile("install_cert", ".sh");

            final String scriptContent = "#!/bin/bash\n" +
                    "echo \"Installing G-Earth root certificate...\"\n" +
                    "echo \"Please enter your password to install the certificate.\"\n" +
                    "sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain \"" + certificatePath + "\"\n";

            Files.write(scriptPath, scriptContent.getBytes());

            if (!scriptPath.toFile().setExecutable(true)) {
                LOG.error("Failed to set executable permission for script {}", scriptPath);
                return false;
            }

            // Install the certificate
            final Process process = new ProcessBuilder("open", "-a", "Terminal", scriptPath.toString()).start();
            final StringBuilder output = new StringBuilder();

            RuntimeUtil.readStream(output, process.getInputStream());
            RuntimeUtil.readStream(output, process.getErrorStream());

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.warn("Install root certificate terminal exited with exit code {}", exitCode);
                return false;
            }

            return true;
        } catch (IOException | InterruptedException e) {
            LOG.error("Error while checking installing certificate", e);
        }

        return false;
    }

    @Override
    public boolean registerSystemProxy(String host, int port) {
        try {
            final String[] ignoreHosts = PROXY_IGNORE.split(";");

            // Enable proxy
            Runtime.getRuntime().exec("networksetup -setwebproxy Wi-Fi " + host + " " + port);
            Runtime.getRuntime().exec("networksetup -setsecurewebproxy Wi-Fi " + host + " " + port);

            // Set proxy bypass domains
            for (String ignoreHost : ignoreHosts) {
                Runtime.getRuntime().exec("networksetup -setproxybypassdomains Wi-Fi " + ignoreHost);
            }

            return true;
        } catch (IOException e) {
            LOG.error("Error while registering system proxy", e);
        }

        return false;
    }

    @Override
    public boolean unregisterSystemProxy() {
        try {
            // Disable proxy
            Runtime.getRuntime().exec("networksetup -setwebproxystate Wi-Fi off");
            Runtime.getRuntime().exec("networksetup -setsecurewebproxystate Wi-Fi off");

            return true;
        } catch (IOException e) {
            LOG.error("Error while unregistering system proxy", e);
        }

        return false;
    }
}