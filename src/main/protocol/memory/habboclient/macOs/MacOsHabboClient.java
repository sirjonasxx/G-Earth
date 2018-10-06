package main.protocol.memory.habboclient.macOs;

import main.protocol.HConnection;
import main.protocol.memory.habboclient.HabboClient;


import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MacOsHabboClient extends HabboClient {

    public MacOsHabboClient(HConnection connection) {
        super(connection);
    }

    private ArrayList<String> readPossibleBytes() throws IOException, URISyntaxException {
        ProcessBuilder pb = new ProcessBuilder(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "/G-Mem");
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        ArrayList<String> possibleData = new ArrayList<>();

        while((line = reader.readLine()) !=  null) {
            possibleData.add(line);
        }
        p.destroy();
        return possibleData;
    }

    @Override
    public List<byte[]> getRC4possibilities() {

        ArrayList<byte[]> possibilities = new ArrayList<>();
        try {
            ArrayList<String> possibleData = readPossibleBytes();

            for (String possibleHexStr : possibleData) {
                possibilities.add(DatatypeConverter.parseHexBinary(possibleHexStr));
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return possibilities;
    }
}
