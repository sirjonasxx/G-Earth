package gearth.misc.packetrepresentation.prediction.checkers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeCheckerProducer {

    public static volatile boolean USE_LONG_DATATYPE = false;

    public static List<TypeChecker> getValidators(HPacket packet) {
        List<TypeChecker> typeCheckers = new ArrayList<>(Arrays.asList(
                new BooleanChecker(packet),
                new ByteChecker(packet),
                new IntegerChecker(packet),
                new StringChecker(packet)));

        if (USE_LONG_DATATYPE) {
            typeCheckers.add(new LongChecker(packet));
        }

        return typeCheckers;
    }

}
