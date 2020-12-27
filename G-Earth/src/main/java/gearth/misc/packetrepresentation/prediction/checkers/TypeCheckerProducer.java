package gearth.misc.packetrepresentation.prediction.checkers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeCheckerProducer {

    public static volatile boolean UNITY_PACKETS = false;

    public static List<TypeChecker> getValidators(HPacket packet) {
        List<TypeChecker> typeCheckers = new ArrayList<>(Arrays.asList(
                new BooleanChecker(packet),
                new ByteChecker(packet),
                new IntegerChecker(packet),
                new StringChecker(packet)));

        if (UNITY_PACKETS) {
            typeCheckers.add(new LongChecker(packet));
            typeCheckers.add(new ShortChecker(packet));
        }

        return typeCheckers;
    }

}
