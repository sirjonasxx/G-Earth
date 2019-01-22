package gearth.misc.harble_api;

import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        private String structure;

        //name can be NULL
        public HarbleMessage(HMessage.Side destination, int headerId, String hash, String name, String structure) {
            this.destination = destination;
            this.headerId = headerId;
            this.hash = hash;
            this.name = (name == null || name.equals("null") ? null : name);
            this.structure = (structure == null || structure.equals("null") ? null : structure);
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

        public String getStructure() {
            return structure;
        }

        public String toString() {
            String s = (headerId + ": " + "[" + hash + "][" + name + "][" + structure + "]");
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
    private String fullPath = null;
    private String revision = null;

    /**
     * cache file must be generated first within G-Earth, inb4 20 extensions requesting it at the same time
     *
     * @param hotelversion
     */

    public static HarbleAPI get(String hotelversion) {
        HarbleAPI wannabe = new HarbleAPI(hotelversion);
        if (!wannabe.success) {
            return null;
        }
        return wannabe;
    }

    public HarbleAPI(String hotelversion) {
        String possibleCachedMessagesPath = HarbleAPIFetcher.CACHE_PREFIX + hotelversion;
        if (Cacher.cacheFileExists(possibleCachedMessagesPath)) {
            JSONObject object = Cacher.getCacheContents(possibleCachedMessagesPath);
            success = true;
            revision = hotelversion;
            fullPath = Cacher.getCacheDir() + File.separator + possibleCachedMessagesPath;
            parse(object);
        }
    }

    public HarbleAPI(String hotelversion, String path_to_file) {

        File f = new File(path_to_file);
        if (f.exists() && !f.isDirectory()) {
            try {
                String contents = String.join("\n", Files.readAllLines(f.toPath()));
                JSONObject object = new JSONObject(contents);
                success = true;
                revision = hotelversion;
                fullPath = path_to_file;
                parse(object);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addMessage(HMessage.Side side, JSONObject object) {
        String name;
        try {
            name = object.getString("Name");
        } catch (Exception e) {
            name = null;
        }
        String hash = object.getString("Hash");
        Integer headerId = object.getInt("Id");
        String structure;

        try {
            structure = object.getString("Structure");
        } catch (Exception e) {
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
            JSONArray incoming = object.getJSONArray("Incoming");
            JSONArray outgoing = object.getJSONArray("Outgoing");

            if (incoming != null && outgoing != null) {
                for (int i = 0; i < incoming.length(); i++) {
                    try {
                        JSONObject message = incoming.getJSONObject(i);
                        addMessage(HMessage.Side.TOCLIENT, message);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                for (int i = 0; i < outgoing.length(); i++) {
                    try{
                        JSONObject message = outgoing.getJSONObject(i);
                        addMessage(HMessage.Side.TOSERVER, message);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
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

    public String getRevision() {
        return revision;
    }

    @Override
    public String toString() {
        if (success) {
            return fullPath;
        }
        return "null";
    }
}
