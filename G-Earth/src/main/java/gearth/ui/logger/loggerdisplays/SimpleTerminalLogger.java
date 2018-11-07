package gearth.ui.logger.loggerdisplays;

import gearth.protocol.HPacket;

/**
 * Created by Jonas on 04/04/18.
 */
class SimpleTerminalLogger implements PacketLogger {
    @Override
    public void start() {
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
    public void appendStructure(HPacket packet) {
        String expr = packet.toExpression();
        if (!expr.equals("")) {
            System.out.println(expr);
        }
    }
}
