package gearth.protocol.connection.proxy.nitro.os.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface NitroWindowsShell32 extends ShellAPI, StdCallLibrary {
    NitroWindowsShell32 INSTANCE = Native.loadLibrary("shell32", NitroWindowsShell32.class);

    WinDef.HINSTANCE ShellExecuteA(WinDef.HWND hwnd,
                                   String lpOperation,
                                   String lpFile,
                                   String lpParameters,
                                   String lpDirectory,
                                   int nShowCmd);
}
