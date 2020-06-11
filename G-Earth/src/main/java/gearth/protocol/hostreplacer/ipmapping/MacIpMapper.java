package gearth.protocol.hostreplacer.ipmapping;

import java.util.ArrayList;
import java.util.List;

public class MacIpMapper extends IpMapper {
    @Override
    public void enable() {

    }

    @Override
    public void addMapping(String ip, int listenport, int connectport) {
        runCommand("ifconfig", "lo0", "alias", ip);
    }

    @Override
    public void deleteMapping(String ip, int listenport, int connectport) {
        runCommand("ifconfig", "lo0", "-alias", ip);
    }

    @Override
    public List<String> getCurrentMappings() {
        return new ArrayList<>();
    }
}
