package gearth.protocol.hostreplacer.hostsfile;

public interface HostReplacer {

    void addRedirect(String[] lines);

    void removeRedirect(String[] lines);

}
