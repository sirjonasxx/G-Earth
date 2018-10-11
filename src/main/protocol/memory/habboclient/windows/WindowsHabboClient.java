package main.protocol.memory.habboclient.windows;

import main.protocol.HConnection;
import main.protocol.memory.habboclient.HabboClient;

import javax.xml.bind.DatatypeConverter;
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
                System.out.println(line);
            }
        }
        p.destroy();
        return possibleData;
    }

    @Override
    public List<byte[]> getRC4possibilities() {
        List<byte[]> result = new ArrayList<>();
        try {
            Thread.sleep(3000);
            ArrayList<String> possibleData = readPossibleBytes();

            for (String possibleHexStr : possibleData) {
                result.add(DatatypeConverter.parseHexBinary(possibleHexStr));
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}