package gearth.misc.packetrepresentation.prediction;

import gearth.misc.packetrepresentation.PacketStringUtils;
import gearth.misc.packetrepresentation.prediction.checkers.TypeChecker;
import gearth.misc.packetrepresentation.prediction.checkers.TypeCheckerProducer;
import gearth.protocol.HPacket;

import java.util.List;

public class StructurePredictor {

    HPacket packet;
    String structure; // null if not found/ didnt try to find

    public StructurePredictor(HPacket packet) {
        this.packet = packet;
        if (packet.isCorrupted()) {
            structure = null;
        }
        else {
            predictStructure();
        }
    }

    private static class SubStructure {
        int previous;
        String type;
        double logScore;

        public SubStructure(int previous, String type, double logScore) {
            this.previous = previous;
            this.type = type;
            this.logScore = logScore;
        }
    }

    private void predictStructure() {
        List<TypeChecker> typeCheckers = TypeCheckerProducer.getValidators(packet);

        SubStructure[] dynamic = new SubStructure[packet.getBytesLength() - 6 + 1];
        dynamic[0] = new SubStructure(-1, "", 0.0);

        int index = 6;
        while (index < packet.getBytesLength()) {
            double currentLogScore = dynamic[index - 6].logScore;
            for (TypeChecker typeChecker : typeCheckers) {
                if (typeChecker.canRead(index)) {
                    double score = typeChecker.score(index);
                    double newScore = currentLogScore + Math.log(score);
                    int nextIndex = typeChecker.nextIndex(index) - 6;
                    if (dynamic[nextIndex] == null || newScore > dynamic[nextIndex].logScore) {
                        dynamic[nextIndex] = new SubStructure(
                                index - 6,
                                typeChecker.getStructCode(),
                                newScore
                        );
                    }
                }
            }
            index++;
        }

        StringBuilder stringBuilder = new StringBuilder();
        SubStructure current = dynamic[dynamic.length - 1];
        while (current.previous != -1) {
            stringBuilder.append(current.type);
            current = dynamic[current.previous];
        }

        structure = stringBuilder.reverse().toString();
    }

    public String getExpression() {
        if (structure == null) {
            return "";
        }
        return PacketStringUtils.toExpressionFromGivenStructure(packet, structure);
    }

    public String getStructure() {
        return structure;
    }

    public static void main(String[] args) {
        HPacket[] packets = new HPacket[] {
                new HPacket("{l}{u:500}{i:20}"),
                new HPacket(4002, "test"),
                new HPacket(4002, "test", 0, true),
                new HPacket(4002, "test", "testtsd", 54452, true, false),
                new HPacket(4002, "test", 46564554, "testtsd", 54452, true, false),
                new HPacket(4002, "test", 0, 46564554, "testtsd", 54452, true, false),
                new HPacket(4002, -1, "test", 0, 46564554, "testtsd", -1, 54452, true, false),
                new HPacket(4002, -1, "test", 0, 46564554, "testtsd", -1, 54452, false, true, ""),
                new HPacket(4002, -1, "test", 0, 46564554, "testtsd", -1, 54452, false, true, ""),
                new HPacket(4002, -1, "test", 0, 46564554, "testtsd", -1, 54452, false, true, 0),
                new HPacket(4002, -1, (byte) 5, "test", 0, 46564554, "testtsd", -1, 54452, false, true, 0),
                new HPacket(4002, "", 20, 0),
                new HPacket(4002, 1),
                new HPacket(4002, 0, 0),
                new HPacket(4002, 0, 0, 42, false),
                new HPacket(4002, 0, "")

        };

        for (HPacket packet : packets) {
            StructurePredictor structurePredictor = new StructurePredictor(packet);
            System.out.println(structurePredictor.getStructure());
            System.out.println(structurePredictor.getExpression());
        }
    }
}
