package gearth.protocol.hostreplacer.ipmapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsIpMapper implements IpMapper {

    private void runCommand(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enable() {
        runCommand("netsh", "interface", "ip", "set", "dns", "1", "dhcp");
    }

    @Override
    public void addMapping(String ip) {
        runCommand("netsh", "interface", "ip", "add", "address", "\"Loopback\"", ip, "255.255.255.255");
    }

    @Override
    public void deleteMapping(String ip) {
        runCommand("netsh", "interface", "ip", "delete", "address", "\"Loopback\"", ip);
    }

    //todo implement, or don't
    @Override
    public List<String> getCurrentMappings() {
        return new ArrayList<>();
    }
}
