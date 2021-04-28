package gearth.services.packet_representation.prediction;

import gearth.services.packet_representation.prediction.checkers.TypeChecker;
import gearth.services.packet_representation.prediction.checkers.TypeCheckerProducer;
import gearth.protocol.HPacket;

import java.util.List;

public class StructurePredictor {

    private HPacket packet;
    private String structure; // null if not found/ didnt try to find

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
                if (!typeChecker.canRead(index)) continue;

                double newScore = currentLogScore + Math.log(typeChecker.score(index));
                int nextIndex = typeChecker.nextIndex(index) - 6;
                if (dynamic[nextIndex] == null || newScore > dynamic[nextIndex].logScore) {
                    dynamic[nextIndex] = new SubStructure(index - 6,  typeChecker.getStructCode(), newScore);
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

    public String getStructure() {
        return structure;
    }
}
