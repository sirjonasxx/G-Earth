package gearth.protocol.connection.proxy.nitro.os.macos;

import gearth.misc.RuntimeUtil;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;

import java.io.File;
import java.io.IOException;

public class NitroMacOS implements NitroOsFunctions {

    /**
     * Semicolon separated hosts to ignore for proxying.
     */
    private static final String PROXY_IGNORE = "discord.com;discordapp.com;github.com;";

    /**
     * Checks if the certificate is trusted by the local machine.
     * @param certificate Absolute path to the certificate.
     * @return true if trusted
     */
    @Override
    public boolean isRootCertificateTrusted(File certificate) {
        try {
            final String output = RuntimeUtil.getCommandOutput(new String[] {"sh", "-c", "security verify-cert -c \"" + certificate.getAbsolutePath() + "\""});

            return !output.contains("CSSMERR_TP_NOT_TRUSTED");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean installRootCertificate(File certificate) {
        final String certificatePath = certificate.getAbsolutePath();

        try {
            // Install the certificate
            Process process = new ProcessBuilder("sh", "-c", "sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain \"" + certificatePath + "\"")
                    .inheritIO()
                    .start();
            
            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.printf("security add-trusted-cert command exited with exit code %d%n", exitCode);
                return false;
            }

            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return false;
    }

}