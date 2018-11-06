package extensions.blockreplacepackets.rules;

import gearth.protocol.HMessage;

/**
 * Created by Jonas on 6/11/2018.
 */
public abstract class BlockReplaceRule {

    public enum Option {
        BLOCK,
        REPLACE
    }

    public enum Type {
        PACKET,
        INTEGER,
        STRING,
        SUBSTRING
    }

    public enum Side {
        INCOMING,
        OUTGOING,
        ALL
    }


    public abstract void appendRuleToMessage(HMessage message);

    public abstract Option option();
    public abstract Type type();
    public abstract Side side();

    public abstract String value();
    public abstract String replacement();

}
