package gearth.protocol.connection.proxy.nitro.os.windows;

import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;

import java.io.File;

public class NitroWindows implements NitroOsFunctions {

    @Override
    public boolean installRootCertificate(File certificate) {
        // TODO: Prompt registration
        System.out.println(certificate.toString());
        return true;
    }

    @Override
    public boolean registerSystemProxy(String host, int port) {
        try {
            final String proxy = String.format("%s:%d", host, port);
            Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /t REG_SZ /d \"" + proxy + "\" /f");
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
