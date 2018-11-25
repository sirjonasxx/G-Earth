package gearth.misc.harble_api;

import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.misc.Cache;

import javax.print.attribute.standard.Destination;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Jonas on 10/11/2018.
 */
public class HarbleAPI {

    public class HarbleMessage {
        private HMessage.Side destination;
        private int headerId;
        private String hash;
        private String name;
        private List<String> structure;

        //name can be NULL
        public HarbleMessage(HMessage.Side destination, int headerId, String hash, String name, List<String> structure) {
            this.destination = destination;
            this.headerId = headerId;
            this.hash = hash;
            this.name = (name == null || name.equals("null") ? null : name);
            this.structure = structure;
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
        public List<String> getStructure() {
            return structure;
        }

        public String toString() {
            String s = (headerId+": " + "["+hash+"]["+name+"]["+ structure+"]");
            return s;
        }
    }

    private Map<Integer, HarbleMessage> headerIdToMessage_incoming = new HashMap<>();
    private Map<Integer, HarbleMessage> headerIdToMessage_outgoing = new HashMap<>();

    private Map<String, List<HarbleMessage>> hashToMessage_incoming = new HashMap<>();
    private Map<String, List<HarbleMessage>> hashToMessage_outgoing = new HashMap<>();

    private Map<String, HarbleMessage> nameToMessage_incoming = new HashMap<>();
    private Map<String, HarbleMessage> nameToMessage_outgoing = new HashMap<>();

    private boolean success = false;

    /**
     * cache file must be generated first within G-Earth, inb4 20 extensions requesting it at the same time
     * @param hotelversion
     */

    public static HarbleAPI get(String hotelversion) {
        HarbleAPI wannabe = new HarbleAPI(hotelversion);
        if (!wannabe.success) {
            return null;
        }
        return wannabe;
    }

    public HarbleAPI (String hotelversion) {
        if (Cacher.cacheFileExists(HarbleAPIFetcher.CACHE_PREFIX + hotelversion)) {
            JSONObject object = Cacher.getCacheContents(HarbleAPIFetcher.CACHE_PREFIX + hotelversion);
            success = true;
            parse(object);
        }
    }

    private void addMessage(HMessage.Side side, JSONObject object, String id) {
        String name;
        try {
            name = object.getString("Name");
        }
        catch (Exception e) {
            name = null;
        }
        String hash = object.getString("Hash");
        Integer headerId = Integer.parseInt(id);
        List<String> structure;

        try {
            structure = new ArrayList<>();
            JSONArray array = object.getJSONArray("Structure");
            for (Object o : array) {
                structure.add((String)o);
            }
        }
        catch (Exception e){
            structure = null;
        }


        HarbleMessage message = new HarbleMessage(side, headerId, hash, name, structure);


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
        try {
            JSONObject incoming = object.getJSONObject("Incoming");
            JSONObject outgoing = object.getJSONObject("Outgoing");

            if (incoming != null && outgoing != null) {
                for (String key : incoming.keySet()) {
                    try {
                        JSONObject inMsg = incoming.getJSONObject(key);
                        addMessage(HMessage.Side.TOCLIENT, inMsg, key);
                    }
                    catch( Exception e) {
                        e.printStackTrace();
                    }
                }
                for (String key : outgoing.keySet()) {
                    try {
                        JSONObject outMsg = outgoing.getJSONObject(key);
                        addMessage(HMessage.Side.TOSERVER, outMsg, key);
                    }
                    catch( Exception e) {}
                }
            }
        }
        catch (Exception e) {
            success = false;
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
