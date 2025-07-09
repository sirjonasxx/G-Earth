package gearth.app.protocol.hostreplacer.hostsfile;

public interface HostReplacer {

    boolean addRedirect(String[] lines);

    boolean removeRedirect(String[] lines);

}
