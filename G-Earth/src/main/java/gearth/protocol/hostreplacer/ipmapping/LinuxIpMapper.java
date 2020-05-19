package gearth.protocol.hostreplacer.ipmapping;

import java.util.List;

public class LinuxIpMapper extends IpMapper {
    @Override
    public void enable() {

    }

    @Override
    public void addMapping(String ip) {
        runCommand("iptables", "-t", "nat", "-A", "OUTPUT",
                "-p", "all", "-d", ip, "-j", "DNAT", "--to-destination", "127.0.0.1");
    }

    @Override
    public void deleteMapping(String ip) {
        runCommand("iptables", "-t", "nat", "-D", "OUTPUT",
                "-p", "all", "-d", ip, "-j", "DNAT", "--to-destination", "127.0.0.1");
    }

    @Override
    public List<String> getCurrentMappings() {
        return null;
    }
}
