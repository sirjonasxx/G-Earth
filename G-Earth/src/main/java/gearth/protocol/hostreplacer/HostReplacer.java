package gearth.protocol.hostreplacer;

public interface HostReplacer {

    void addRedirect(String[] lines);

    void removeRedirect(String[] lines);

}
