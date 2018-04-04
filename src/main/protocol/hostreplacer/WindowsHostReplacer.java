package main.protocol.hostreplacer;

/**
 * Created by Jonas on 04/04/18.
 */
class WindowsHostReplacer extends LinuxHostReplacer {

    WindowsHostReplacer() {
        super();
        hostsFileLocation = System.getenv("WinDir") + "\\system32\\drivers\\etc\\hosts";
    }

}
