package extensions.blockreplacepackets.rules;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

/**
 * Created by Jonas on 6/11/2018.
 */
public class BlockPacketRule extends BlockReplaceRule{

    private int headerId;
    private Side side;

    BlockPacketRule(Side side, int headerId) {
        this.headerId = headerId;
        this.side = side;
    }

    @Override
    public void appendRuleToMessage(HMessage message) {
        if (side == Side.ALL
                || (message.getDestination() == HMessage.Side.TOSERVER && side == Side.OUTGOING)
                || (message.getDestination() == HMessage.Side.TOCLIENT && side ==Side.INCOMING)) {
            if (message.getPacket().headerId() == headerId) {
                message.setBlocked(true);
            }
        }
    }

    @Override
    public Option option() {
        return Option.BLOCK;
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
        return "/";
    }
}
