package gearth.protocol.connection.proxy.nitro.os.windows;

import com.sun.jna.platform.win32.WinDef;
import gearth.misc.RuntimeUtil;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class NitroWindows implements NitroOsFunctions {

    private static final Logger log = LoggerFactory.getLogger(NitroWindows.class);

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
            final String output = RuntimeUtil.getCommandOutput(new String[] {"cmd", "/c", " certutil.exe -f -verify \"" + certificate.getAbsolutePath() + "\""});

            return !output.contains("CERT_TRUST_IS_UNTRUSTED_ROOT") &&
                    output.contains("dwInfoStatus=10c dwErrorStatus=0");
        } catch (IOException e) {
            log.error("Failed to check if root certificate is trusted", e);
        }

        return false;
    }

    @Override
    public boolean installRootCertificate(File certificate) {
        final String certificatePath = certificate.toPath().normalize().toAbsolutePath().toString();

        // Correct the command for certutil
        final String installCommand = "/c certutil -addstore root \"" + certificatePath + "\"";

        log.debug("Installing root certificate with command: {}", installCommand);

        // Prompt UAC elevation using ShellExecuteA with "runas"
        WinDef.HINSTANCE result = NitroWindowsShell32.INSTANCE.ShellExecuteA(
                null,                               // Handle to parent window (optional)
                "runas",                            // Use "runas" to request elevation
                "cmd.exe",                          // Program to execute
                installCommand,                     // Command to run with cmd.exe /c
                null,                               // Directory (optional)
                1                                   // Show the window
        );

        final int resultValue = result.toNative().hashCode();

        if (resultValue <= 32) { // If the result is <= 32, an error occurred
            log.error("Failed to start process for installing root certificate. Error code: {}", resultValue);
            return false;
        }

        return true;
    }

    @Override
    public boolean registerSystemProxy(String host, int port) {
        try {
            final String proxy = String.format("%s:%d", host, port);
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /t REG_SZ /d \"" + proxy + "\" /f");
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyOverride /t REG_SZ /d \"" + PROXY_IGNORE + "\" /f");
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 1 /f");
            return true;
        } catch (Exception e) {
            log.error("Failed to register system proxy", e);
        }

        return false;
    }

    @Override
    public boolean unregisterSystemProxy() {
        try {
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 0 /f");
            return true;
        } catch (Exception e) {
            log.error("Failed to unregister system proxy", e);
        }

        return false;
    }

}
