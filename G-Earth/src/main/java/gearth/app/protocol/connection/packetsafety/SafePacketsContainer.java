package gearth.app.protocol.connection.packetsafety;

import gearth.protocol.HMessage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SafePacketsContainer {

    private final Set<Integer> safeIncomingIds = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> safeOutgoingIds = Collections.synchronizedSet(new HashSet<>());

    public void validateSafePacket(int headerId, HMessage.Direction direction) {
        Set<Integer> headerIds = direction == HMessage.Direction.TOCLIENT ? safeIncomingIds : safeOutgoingIds;
        headerIds.add(headerId);
    }

    public boolean isPacketSafe(int headerId, HMessage.Direction direction) {
        Set<Integer> headerIds = direction == HMessage.Direction.TOCLIENT ? safeIncomingIds : safeOutgoingIds;
        return headerIds.contains(headerId);
    }

}
