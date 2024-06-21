package gearth.protocol.memory.habboclient.rust;

import gearth.encoding.HexEncoding;
import gearth.protocol.HConnection;
import gearth.protocol.memory.habboclient.HabboClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class RustHabboClient extends HabboClient {
    public RustHabboClient(HConnection connection) {
        super(connection);
    }

    @Override
    public List<byte[]> getRC4cached() {
        return new ArrayList<>();
    }

    public List<byte[]> getRC4possibilities() {
        ArrayList<String> possibleData = new ArrayList<>();

        try {
            String g_mem = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "/G-Mem";
            ProcessBuilder pb = new ProcessBuilder(g_mem, hConnection.getClientHost() , Integer.toString(hConnection.getClientPort()));


            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while((line = reader.readLine()) !=  null) {
                if (line.length() > 1) {
                    System.out.println("[+] " + line);
                    possibleData.add(line);
                }
            }

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        List<byte[]> ret = new ArrayList<>();

        for (String possibleHexStr : possibleData)
            ret.add(HexEncoding.toBytes(possibleHexStr));

        return ret;
    }
}
