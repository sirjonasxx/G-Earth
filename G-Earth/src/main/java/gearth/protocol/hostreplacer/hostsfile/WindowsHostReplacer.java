package gearth.protocol.hostreplacer.hostsfile;

/**
 * Created by Jonas on 04/04/18.
 */
class WindowsHostReplacer extends UnixHostReplacer {

    WindowsHostReplacer() {
        super();
        hostsFileLocation = System.getenv("WinDir") + "\\system32\\drivers\\etc\\hosts";
    }

}
