package gearth.protocol.hostreplacer.ipmapping;

import java.util.List;

// always map to 127.0.0.1, same port
public interface IpMapper {

    void enable();

    void addMapping(String ip);

    void deleteMapping(String ip);

    List<String> getCurrentMappings();

}
