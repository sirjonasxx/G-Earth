package main.protocol.memory.habboclient.windows;

import main.protocol.HConnection;
import main.protocol.memory.habboclient.HabboClient;

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
    }

    private ArrayList<String> readPossibleBytes() throws IOException, URISyntaxException {
        ProcessBuilder pb = new ProcessBuilder(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "\\G-WinMem.exe", hConnection.getClientHostAndPort().substring(0, hConnection.getClientHostAndPort().indexOf(':')) , Integer.toString(hConnection.getPort()));
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        ArrayList<String> possibleData = new ArrayList<>();

        while((line = reader.readLine()) !=  null) {
            if (line.length() > 1) {
                possibleData.add(line);
            }
        }
        p.destroy();
        return possibleData;
    }

    @Override
    public List<byte[]> getRC4possibilities() {
        List<byte[]> result = new ArrayList<>();
        try {
            ArrayList<String> possibleData = readPossibleBytes();

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
