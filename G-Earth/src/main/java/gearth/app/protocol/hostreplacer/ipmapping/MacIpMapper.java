package gearth.app.protocol.hostreplacer.ipmapping;

import java.util.ArrayList;
import java.util.List;

public class MacIpMapper extends IpMapper {
    private static final String IFCONFIG = "/sbin/ifconfig";

    @Override
    public void enable() {

    }

    @Override
    public void addMapping(String ip, int listenport, int connectport) {
        runCommand(IFCONFIG, "lo0", "alias", ip);
    }

    @Override
    public void deleteMapping(String ip, int listenport, int connectport) {
        runCommand(IFCONFIG, "lo0", "-alias", ip);
    }

    @Override
    public List<String> getCurrentMappings() {
        return new ArrayList<>();
    }
}
