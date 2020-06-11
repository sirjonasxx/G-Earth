package gearth.protocol.hostreplacer.ipmapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsIpMapper extends IpMapper {

    @Override
    public void enable() {
        runCommand("netsh", "interface", "ip", "set", "dns", "1", "dhcp");
    }

    @Override
    public void addMapping(String ip, int listenport, int connectport) {
        runCommand("netsh", "interface", "ip", "add", "address", "\"Loopback\"", ip, "255.255.255.255");
    }

    @Override
    public void deleteMapping(String ip, int listenport, int connectport) {
        runCommand("netsh", "interface", "ip", "delete", "address", "\"Loopback\"", ip);
    }

    //todo implement, or don't
    @Override
    public List<String> getCurrentMappings() {
        return new ArrayList<>();
    }
}
