package gearth.services.internal_extensions.blockreplacepackets.rules;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

/**
 * Created by Jonas on 6/11/2018.
 */
public class ReplacePacketRule extends BlockReplaceRule {

    private Side side;
    private int headerId;
    private HPacket replacement;

    ReplacePacketRule(Side side, int headerId, HPacket replacement) {
        this.side = side;
        this.headerId = headerId;
        this.replacement = replacement;
    }

    @Override
    public void appendRuleToMessage(HMessage message) {
        if (side == Side.ALL
                || (message.getDestination() == HMessage.Direction.TOSERVER && side == Side.OUTGOING)
                || (message.getDestination() == HMessage.Direction.TOCLIENT && side ==Side.INCOMING)) {
            if (message.getPacket().headerId() == headerId) {
                message.getPacket().constructFromString(replacement.stringify());
                message.getPacket().overrideEditedField(true);
            }
        }
    }

    @Override
    public Option option() {
        return Option.REPLACE;
    }

    @Override
    public Type type() {
        return Type.PACKET;
    }

    @Override
    public Side side() {
        return side;
    }

    @Override
    public String value() {
        return headerId+"";
    }

    @Override
    public String replacement() {
        return replacement.toString();
    }
}
