package gearth.protocol.hostreplacer.ipmapping;

import gearth.misc.OSValidator;

public class IpMapperFactory {

    public static IpMapper get() {

        if (OSValidator.isWindows()) return new WindowsIpMapper();
        if (OSValidator.isUnix()) return new LinuxIpMapper();
        else return new MacIpMapper();
    }

}
