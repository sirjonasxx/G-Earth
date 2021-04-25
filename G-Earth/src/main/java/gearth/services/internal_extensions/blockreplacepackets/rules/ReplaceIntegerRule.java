package gearth.services.internal_extensions.blockreplacepackets.rules;

import gearth.protocol.HMessage;

/**
 * Created by Jonas on 6/11/2018.
 */
public class ReplaceIntegerRule extends BlockReplaceRule {

    Side side;
    int value;
    int replacement;

    ReplaceIntegerRule(Side side, int value, int replacement) {
        this.side = side;
        this.value = value;
        this.replacement = replacement;
    }

    @Override
    public void appendRuleToMessage(HMessage message) {
        if (side == Side.ALL
                || (message.getDestination() == HMessage.Direction.TOSERVER && side == Side.OUTGOING)
                || (message.getDestination() == HMessage.Direction.TOCLIENT && side ==Side.INCOMING)) {
            message.getPacket().replaceAllIntegers(value, replacement);
        }
    }

    @Override
    public Option option() {
        return Option.REPLACE;
    }

    @Override
    public Type type() {
        return Type.INTEGER;
    }

    @Override
    public Side side() {
        return side;
    }

    @Override
    public String value() {
        return value+"";
    }

    @Override
    public String replacement() {
        return replacement+"";
    }
}
