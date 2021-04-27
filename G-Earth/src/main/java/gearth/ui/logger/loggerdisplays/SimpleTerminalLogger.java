package gearth.ui.logger.loggerdisplays;

import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

/**
 * Created by Jonas on 04/04/18.
 */
class SimpleTerminalLogger implements PacketLogger {

    protected PacketInfoManager packetInfoManager = null;

    @Override
    public void start(HConnection hConnection) {
        packetInfoManager = hConnection.getPacketInfoManager();
//        System.out.println("-- START OF SESSION --");
    }

    @Override
    public void stop() {
//        System.out.println("-- END OF SESSION --");
    }

    @Override
    public void appendSplitLine() {
        System.out.println("-----------------------------------");
    }

    @Override
    public void appendMessage(HPacket packet, int types) {
        StringBuilder output = new StringBuilder();

        if ((types & MESSAGE_TYPE.BLOCKED.getValue()) != 0) {
            output.append("[BLOCKED] ");
        }
        else if ((types & MESSAGE_TYPE.REPLACED.getValue()) != 0) {
            output.append("[REPLACED] ");
        }

        output.append(
                (types & MESSAGE_TYPE.INCOMING.getValue()) != 0 ?
                        "INCOMING " :
                        "OUTGOING "
        );

        if ((types & MESSAGE_TYPE.SHOW_ADDITIONAL_DATA.getValue()) != 0) {
            output.append("(h:").append(packet.headerId()).append(", l:").append(packet.length()).append(") ");
        }

        output.append("--> ");

        output.append( (types & MESSAGE_TYPE.SKIPPED.getValue()) != 0 ?
                "<packet skipped>" :
                packet.toString()
        );

        System.out.println(output.toString());
    }

    @Override
    public void appendStructure(HPacket packet, HMessage.Direction direction) {
        String expr = packet.toExpression(direction, packetInfoManager, true);
        if (!expr.equals("")) {
            System.out.println(expr);
        }
    }
}
