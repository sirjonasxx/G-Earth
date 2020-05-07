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
                new HPacket(4002, 0, ""),
                new HPacket("[0][0][0]F[0] [0][0][0] [0][0][0][5][0]\u0013ACH_FriendListSize5[0][0][0]U[0][0][0] [0][0][0][0][0][0][0][0][0][0][0]Z[0][0][6]social[0][0][0][0][0][13][0][0][0][2]"),
                new HPacket("[0][0][0]?[0] [0][0][0][4][0][0][0][4][0][10]ACH_Login4[0][0][0]\u000F[0][0][0]\u001C[0][0][0][0][0][0][0][0][0][0][0][2][0][0][8]identity[0][0][0][0][0]\u0014[0][0][0][0]"),
                new HPacket("[0][0][0][6]\u000Ew[0][0][0][0]"),
                new HPacket("[0][0][0]'[3] [0][5]Login[0][6]socket[0]\u000Eclient.auth_ok[0][0][0][0][0][0]"),
                new HPacket(4002, false, ""),
//                new HPacket("[0][0][1]p[13]1[0][0][0][12][0][10]MOUSE_ZOOM[0][0][1][0]\u0015HABBO_CLUB_OFFER_BETA[0][0][1][0]\u000EUSE_GUIDE_TOOL[0]&requirement.unfulfilled.helper_level_4[0][0]\u000FBUILDER_AT_WORK[0](requirement.unfulfilled.group_membership[0][0]\u000FCALL_ON_HELPERS[0][0][1][0]\u001FNAVIGATOR_ROOM_THUMBNAIL_CAMERA[0][0][1][0][7]CITIZEN[0][0][1][0]\u0012JUDGE_CHAT_REVIEWS[0]&requirement.unfulfilled.helper_level_6[0][0][5]TRADE[0][0][1][0][6]CAMERA[0][0][1][0]\u0014VOTE_IN_COMPETITIONS[0][0][1][0]\u0018NAVIGATOR_PHASE_TWO_2014[0][0][1]")

        };

        for (HPacket packet : packets) {
            StructurePredictor structurePredictor = new StructurePredictor(packet);
            System.out.println(structurePredictor.getStructure());
            System.out.println(structurePredictor.getExpression());
        }
    }
}
