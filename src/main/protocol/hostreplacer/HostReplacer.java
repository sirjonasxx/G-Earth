package main.protocol.hostreplacer;

public interface HostReplacer {

    void addRedirect(String original, String redirect);

    void removeRedirect(String original, String redirect);

}
