package gearth.misc.harble_api;

import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import org.json.JSONObject;
import sun.misc.Cache;

import javax.print.attribute.standard.Destination;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 10/11/2018.
 */
public class HarbleAPI {

    public class HarbleMessage {
        private HMessage.Side destination;
        private int headerId;
        private String hash;
        private String name;

        //name can be NULL
        public HarbleMessage(HMessage.Side destination, int headerId, String hash, String name) {
            this.destination = destination;
            this.headerId = headerId;
            this.hash = hash;
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public int getHeaderId() {
            return headerId;
        }
        public HMessage.Side getDestination() {
            return destination;
        }
        public String getHash() {
            return hash;
        }
    }

    private Map<Integer, HarbleMessage> headerIdToMessage_incoming = new HashMap<>();
    private Map<Integer, HarbleMessage> headerIdToMessage_outgoing = new HashMap<>();

    private Map<String, List<HarbleMessage>> hashToMessage_incoming = new HashMap<>();
    private Map<String, List<HarbleMessage>> hashToMessage_outgoing = new HashMap<>();

    private Map<String, HarbleMessage> nameToMessage_incoming = new HashMap<>();
    private Map<String, HarbleMessage> nameToMessage_outgoing = new HashMap<>();

    /**
     * cache file must be generated first within G-Earth, inb4 20 extensions requesting it at the same time
     * @param hotelversion
     */
    public HarbleAPI (String hotelversion) {
        if (Cacher.cacheFileExists(HarbleAPIFetcher.CACHE_PREFIX + hotelversion)) {
            JSONObject object = Cacher.getCacheContents(HarbleAPIFetcher.CACHE_PREFIX + hotelversion);
            parse(object);
        }
    }

    private void addMessage(HarbleMessage message) {
        Map<Integer, HarbleMessage> headerIdToMessage =
                message.getDestination() == HMessage.Side.TOCLIENT
                        ? headerIdToMessage_incoming :
                        headerIdToMessage_outgoing;

        Map<String, List<HarbleMessage>> hashToMessage =
                message.getDestination() == HMessage.Side.TOCLIENT
                        ? hashToMessage_incoming
                        : hashToMessage_outgoing;

        Map<String, HarbleMessage> nameToMessag =
                message.getDestination() == HMessage.Side.TOCLIENT
                        ? nameToMessage_incoming
                        : nameToMessage_outgoing;

        headerIdToMessage.put(message.getHeaderId(), message);
        hashToMessage.computeIfAbsent(message.getHash(), k -> new ArrayList<>());
        hashToMessage.get(message.getHash()).add(message);
        if (message.getName() != null && !message.getName().equals("null")) {
            nameToMessag.put(message.getName(), message);
        }
    }
    private void parse(JSONObject object) {
        JSONObject incoming = object.getJSONObject("IncomingMessages");
        JSONObject outgoing = object.getJSONObject("OutgoingMessages");

        if (incoming != null && outgoing != null) {
            for (String key : incoming.keySet()) {
                JSONObject inMsg = incoming.getJSONObject(key);
                String name = inMsg.getString("Name");
                String hash = inMsg.getString("Hash");
                Integer headerId = Integer.parseInt(key);
                HarbleMessage message = new HarbleMessage(HMessage.Side.TOCLIENT, headerId, hash, name);

                addMessage(message);
            }
            for (String key : outgoing.keySet()) {
                JSONObject outMsg = incoming.getJSONObject(key);
                String name = outMsg.getString("Name");
                String hash = outMsg.getString("Hash");
                Integer headerId = Integer.parseInt(key);
                HarbleMessage message = new HarbleMessage(HMessage.Side.TOSERVER, headerId, hash, name);

                addMessage(message);
            }
        }
    }

    public HarbleMessage getHarbleMessageFromHeaderId(HMessage.Side side, int headerId) {
        Map<Integer, HarbleMessage> headerIdToMessage =
                (side == HMessage.Side.TOSERVER
                        ? headerIdToMessage_outgoing
                        : headerIdToMessage_incoming);

        return headerIdToMessage.get(headerId);
    }
    public List<HarbleMessage> getHarbleMessagesFromHash(HMessage.Side side, String hash) {
        Map<String, List<HarbleMessage>> hashToMessage =
                (side == HMessage.Side.TOSERVER
                        ? hashToMessage_outgoing
                        : hashToMessage_incoming);

        List<HarbleMessage> result = hashToMessage.get(hash);
        return result == null ? new ArrayList<>() : result;
    }
    public HarbleMessage getHarbleMessageFromName(HMessage.Side side, String name) {
        Map<String, HarbleMessage> nameToMessage =
                (side == HMessage.Side.TOSERVER
                        ? nameToMessage_outgoing
                        : nameToMessage_incoming);

        return nameToMessage.get(name);
    }


}
