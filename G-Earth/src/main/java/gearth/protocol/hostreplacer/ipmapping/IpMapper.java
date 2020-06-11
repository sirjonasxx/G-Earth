package gearth.protocol.hostreplacer.ipmapping;

import java.io.IOException;
import java.util.List;

// always map to 127.0.0.1, same port
public abstract class IpMapper {

    void runCommand(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract public void enable();

    abstract public void addMapping(String ip, int listenport, int connectport);

    abstract public void deleteMapping(String ip, int listenport, int connectport);

    abstract public List<String> getCurrentMappings();

}
