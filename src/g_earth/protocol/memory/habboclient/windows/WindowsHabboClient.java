package g_earth.protocol.memory.habboclient.windows;


import g_earth.misc.Cacher;
import g_earth.protocol.HConnection;
import g_earth.protocol.HMessage;
import g_earth.protocol.TrafficListener;
import g_earth.protocol.memory.habboclient.HabboClient;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by Jeunez on 27/06/2018.
 */

public class WindowsHabboClient extends HabboClient {
    public WindowsHabboClient(HConnection connection) {
        super(connection);

        connection.addTrafficListener(0, message -> {
            if (message.getDestination() == HMessage.Side.TOSERVER && message.getPacket().headerId() == PRODUCTIONID) {
                production = message.getPacket().readString();
            }
        });
    }

    private static final int PRODUCTIONID = 4000;
    private String production = "";

    private String getOffsetsCacheKey() {
        return "RC4Offsets";
    }

    private String getOffsetsRevision() {
        return production;
    }

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
        ProcessBuilder pb = null;
        JSONObject revisionList = (JSONObject) Cacher.get(getOffsetsCacheKey());

        if (revisionList == null) {
            Cacher.put(getOffsetsCacheKey(), new JSONObject());
            revisionList = (JSONObject) Cacher.get(getOffsetsCacheKey()); // refresh
        }

        List<String> cachedOffsets = (List<String>) revisionList.get(getOffsetsRevision());
        StringJoiner joiner = new StringJoiner(" ");

        if (useCache) {
            if (cachedOffsets == null) {
                return null;
            }

            for (String s : cachedOffsets) {
                joiner.add(s);
            }
        }

        if (!useCache)
            pb = new ProcessBuilder(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "\\G-WinMem.exe", hConnection.getClientHostAndPort().substring(0, hConnection.getClientHostAndPort().indexOf(':')) , Integer.toString(hConnection.getPort()));
        else
            pb = new ProcessBuilder(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "\\G-WinMem.exe", hConnection.getClientHostAndPort().substring(0, hConnection.getClientHostAndPort().indexOf(':')) , Integer.toString(hConnection.getPort()), "-c" + joiner.toString());


        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        ArrayList<String> possibleData = new ArrayList<>();

        if (cachedOffsets == null) {
            cachedOffsets = new ArrayList<>();
        }


        int count = 0;
        while((line = reader.readLine()) !=  null) {
            if (line.length() > 1) {
                if (!useCache && (count++ % 2 == 0)) {
                    if (!cachedOffsets.contains(line)) {
                        cachedOffsets.add(line);
                    }
                }
                else
                    possibleData.add(line);
            }
        }
        revisionList.put(getOffsetsRevision(), cachedOffsets);
        Cacher.put(getOffsetsCacheKey(), revisionList);
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

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
