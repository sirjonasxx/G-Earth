package extensions.blockreplacepackets.rules;

import gearth.protocol.HPacket;

/**
 * Created by Jonas on 6/11/2018.
 */
public class RuleFactory {

    public static BlockReplaceRule getRule(String type, String side, String value, String replacement) {
        BlockReplaceRule.Option rOption = BlockReplaceRule.Option.valueOf(type.split(" ")[0].toUpperCase());
        BlockReplaceRule.Type rType = BlockReplaceRule.Type.valueOf(type.split(" ")[1].toUpperCase());
        BlockReplaceRule.Side rSide = BlockReplaceRule.Side.valueOf(side.toUpperCase());

        if (rOption == BlockReplaceRule.Option.BLOCK) {
            return new BlockPacketRule(rSide, Integer.parseInt(value));
        }
        if (rOption == BlockReplaceRule.Option.REPLACE) {
            if (rType == BlockReplaceRule.Type.INTEGER) {
                return new ReplaceIntegerRule(rSide, Integer.parseInt(value), Integer.parseInt(replacement));
            }
            if (rType == BlockReplaceRule.Type.PACKET) {
                return new ReplacePacketRule(rSide, Integer.parseInt(value), new HPacket(replacement));
            }
            if (rType == BlockReplaceRule.Type.STRING) {
                return new ReplaceStringRule(rSide, value, replacement);
            }
            if (rType == BlockReplaceRule.Type.SUBSTRING) {
                return new ReplaceSubstringRule(rSide, value, replacement);
            }
        }
        return null;
    }

}
