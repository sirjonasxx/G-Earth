package gearth.protocol.memory.habboclient.windows;

import gearth.misc.Cacher;
import gearth.misc.StringUtils;
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
import java.util.*;

/**
 * Created by Jonas on 27/06/2018.
 */

public class WindowsHabboClient extends HabboClient {
    public WindowsHabboClient(HConnection connection) {
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
                result.add(StringUtils.hexStringToByteArray(s));
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

        String g_winmem = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "\\G-Mem.exe";
        String clientHost = hConnection.isRawIpMode() ? "null" : hConnection.getClientHost();
        String clientPort = hConnection.isRawIpMode() ? "null" : hConnection.getClientPort() + "";

        if (!useCache)
            pb = new ProcessBuilder(g_winmem, clientHost , clientPort);
        else
            pb = new ProcessBuilder(g_winmem, clientHost , clientPort, "-c" + joiner.toString());


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
                result.add(StringUtils.hexStringToByteArray(possibleHexStr));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return result;
    }
}
