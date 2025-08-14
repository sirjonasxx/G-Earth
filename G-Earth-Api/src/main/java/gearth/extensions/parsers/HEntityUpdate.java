package gearth.extensions.parsers;

import gearth.protocol.HPacket;

public class HEntityUpdate {
    private int index;
    private boolean isController = false;

    private HPoint tile;
    private HPoint movingTo = null;

    private HSign sign = null;
    private HStance stance = null;
    private HAction action = null;
    private HDirection headFacing;
    private HDirection bodyFacing;

    private HEntityUpdate(HPacket packet) {
        index = packet.readInteger();
        tile = new HPoint(packet.readInteger(), packet.readInteger(),
                Double.parseDouble(packet.readString()));

        headFacing = HDirection.values()[packet.readInteger()];
        bodyFacing = HDirection.values()[packet.readInteger()];

        String action = packet.readString();
        String[] actionData = action.split("/");

        for (String actionInfo : actionData) {
            String[] actionValues = actionInfo.split(" ");

            if (actionValues.length < 2) continue;
            if (actionValues[0].isEmpty()) continue;

            switch(actionValues[0]) {
                case "flatctrl":
                    isController = true;
                    this.action = HAction.None;
                    break;
                case "mv":
                    String[] values = actionValues[1].split(",");
                    if (values.length >= 3)
                        movingTo = new HPoint(Integer.decode(values[0]), Integer.decode(values[1]),
                                Double.parseDouble(values[2]));

                    this.action = HAction.Move;
                    break;
                case "sit":
                    this.action = HAction.Sit;
                    stance = HStance.Sit;
                    break;
                case "lay":
                    this.action = HAction.Lay;
                    stance = HStance.Lay;
                    break;
                case "sign":
                    sign = HSign.values()[Integer.decode(actionValues[1])];
                    this.action = HAction.Sign;
                    break;
            }
        }
    }


    public static HEntityUpdate[] parse(HPacket packet) {
        HEntityUpdate[] updates = new HEntityUpdate[packet.readInteger()];
        for (int i = 0; i < updates.length; i++)
            updates[i] = new HEntityUpdate(packet);

        return updates;
    }

    public int getIndex() {
        return index;
    }

    public boolean isController() {
        return isController;
    }

    public HPoint getTile() {
        return tile;
    }

    public HPoint getMovingTo() {
        return movingTo;
    }

    public HSign getSign() {
        return sign;
    }

    public HStance getStance() {
        return stance;
    }

    public HAction getAction() {
        return action;
    }

    public HDirection getHeadFacing() {
        return headFacing;
    }

    public HDirection getBodyFacing() {
        return bodyFacing;
    }
}