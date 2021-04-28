package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gearth.services.Constants.UNITY_PACKETS;

public class TypeCheckerProducer {

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
