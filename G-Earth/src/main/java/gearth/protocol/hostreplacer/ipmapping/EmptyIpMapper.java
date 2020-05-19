package gearth.protocol.hostreplacer.ipmapping;

import java.util.ArrayList;
import java.util.List;

// Temporary class for the sake of not getting nullpointers on linux&mac until they have an IpMapper as well
public class EmptyIpMapper extends IpMapper {
    @Override
    public void enable() {

    }

    @Override
    public void addMapping(String ip) {

    }

    @Override
    public void deleteMapping(String ip) {

    }

    @Override
    public List<String> getCurrentMappings() {
        return new ArrayList<>();
    }
}
