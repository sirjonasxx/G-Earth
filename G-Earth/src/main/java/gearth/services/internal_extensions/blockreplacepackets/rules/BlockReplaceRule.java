package gearth.services.internal_extensions.blockreplacepackets.rules;

import gearth.protocol.HMessage;
import javafx.beans.InvalidationListener;

import java.util.ArrayList;
import java.util.List;

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

    private List<InvalidationListener> onDeleteListeners = new ArrayList<>();
    public void onDelete(InvalidationListener listener) {
        onDeleteListeners.add(listener);
    }
    public void delete() {
        for (int i = onDeleteListeners.size() - 1; i >= 0; i--) {
            onDeleteListeners.get(i).invalidated(null);
        }
    }

}
