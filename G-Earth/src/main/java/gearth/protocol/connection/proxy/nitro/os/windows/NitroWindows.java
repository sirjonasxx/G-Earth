package gearth.protocol.connection.proxy.nitro.os.windows;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import gearth.misc.RuntimeUtil;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;

import java.io.File;
import java.io.IOException;

public class NitroWindows implements NitroOsFunctions {

    /**
     * Semicolon separated hosts to ignore for proxying.
     */
    // habba.io;
    private static final String PROXY_IGNORE = "discord.com;discordapp.com;github.com;challenges.cloudflare.com;";

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
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean installRootCertificate(File certificate) {
        final String certificatePath = certificate.getAbsolutePath();

        // Prompt UAC elevation.
        WinDef.HINSTANCE result = NitroWindowsShell32.INSTANCE.ShellExecuteA(null, "runas", "cmd.exe", "/S /C 'certutil -addstore root \"" + certificatePath + "\"'", null, 1);

        // Wait for exit.
        Kernel32.INSTANCE.WaitForSingleObject(result, WinBase.INFINITE);

        // Exit code for certutil.
        final IntByReference statusRef = new IntByReference(-1);
        Kernel32.INSTANCE.GetExitCodeProcess(result, statusRef);

        // Check if process exited without errors
        if (statusRef.getValue() != -1) {
            System.out.printf("Certutil command exited with exit code %s%n", statusRef.getValue());
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
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean unregisterSystemProxy() {
        try {
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 0 /f");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
