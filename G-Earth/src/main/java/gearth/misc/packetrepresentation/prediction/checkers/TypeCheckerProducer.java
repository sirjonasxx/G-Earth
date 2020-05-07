package gearth.misc.packetrepresentation.prediction.checkers;

import gearth.protocol.HPacket;

import java.util.Arrays;
import java.util.List;

public class TypeCheckerProducer {

    public static List<TypeChecker> getValidators(HPacket packet) {
        return Arrays.asList(
                new BooleanChecker(packet),
                new ByteChecker(packet),
                new IntegerChecker(packet),
                new StringChecker(packet)
        );
    }

}
