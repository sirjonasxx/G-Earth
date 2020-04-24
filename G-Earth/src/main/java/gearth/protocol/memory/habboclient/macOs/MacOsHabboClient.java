package gearth.protocol.memory.habboclient.macOs;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.memory.habboclient.HabboClient;
import org.json.JSONArray;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class MacOsHabboClient extends HabboClient {

    public MacOsHabboClient(HConnection connection) {
        super(connection);

        connection.addTrafficListener(0, message -> {
            if (message.getDestination() == HMessage.Direction.TOSERVER && message.getPacket().headerId() == PRODUCTION_ID) {
                production = message.getPacket().readString();
            }
        });
    }


    private static final String OFFSETS_CACHE_KEY = "RC4Offsets";
    private static final int PRODUCTION_ID = 4000;
    private String production = "";

    @Override
    public List<byte[]> getRC4cached() {
        List<byte[]> result = new ArrayList<>();
        try {
            List<String> possibleResults = readPossibleBytes(true);

            if (possibleResults == null)
                return new ArrayList<>();

            for (String s : possibleResults)
                result.add(hexStringToByteArray(s));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return result;
    }

    private ArrayList<String> readPossibleBytes(boolean useCache) throws IOException, URISyntaxException {
        ProcessBuilder pb;

        JSONObject revisionList = (JSONObject) Cacher.get(OFFSETS_CACHE_KEY);
        if (revisionList == null) {
            Cacher.put(OFFSETS_CACHE_KEY, new JSONObject());
            revisionList = (JSONObject) Cacher.get(OFFSETS_CACHE_KEY);
        }

        assert revisionList != null;
        JSONArray cachedOffsets;
        if (revisionList.has(production))
            cachedOffsets = (JSONArray) revisionList.get(production);
        else
            cachedOffsets = null;

        StringJoiner joiner = new StringJoiner(" ");

        if (useCache) {
            if (cachedOffsets == null) {
                return null;
            }

            for (Object s : cachedOffsets) {
                joiner.add((String)s);
            }
        }

        String g_mem = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "/G-Mem";
        if (!useCache)
            pb = new ProcessBuilder(g_mem, hConnection.getClientHost() , Integer.toString(hConnection.getClientPort()));
        else
            pb = new ProcessBuilder(g_mem, hConnection.getClientHost() , Integer.toString(hConnection.getClientPort()), "-c" + joiner.toString());


        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        ArrayList<String> possibleData = new ArrayList<>();

        if (cachedOffsets == null) {
            cachedOffsets = new JSONArray();
        }


        int count = 0;
        while((line = reader.readLine()) !=  null) {
            if (line.length() > 1) {
                if (!useCache && (count++ % 2 == 0)) {
                    if (!cachedOffsets.toList().contains(line)) {
                        cachedOffsets.put(line);
                        System.out.println("[+] " + line);
                    }
                }
                else
                    possibleData.add(line);
            }
        }

        revisionList.put(production, cachedOffsets);
        Cacher.put(OFFSETS_CACHE_KEY, revisionList);
        p.destroy();
        return possibleData;
    }

    @Override
    public List<byte[]> getRC4possibilities() {
        List<byte[]> result = new ArrayList<>();
        try {
            ArrayList<String> possibleData = readPossibleBytes(false);

            for (String possibleHexStr : possibleData) {
                result.add(hexStringToByteArray(possibleHexStr));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
