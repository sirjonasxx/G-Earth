package main.protocol.memory.habboclient.windows;

import main.protocol.HConnection;
import main.protocol.memory.habboclient.HabboClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeunez on 27/06/2018.
 */
public class WindowsHabboClient extends HabboClient {

    private static final boolean DEBUG = true;
    private int[] PID; // list of potential PIDs

    public WindowsHabboClient(HConnection connection) {
        super(connection);


    }

    private void obtain_PID () {
        String command="cmd /C netstat -a -o -n | findstr "+hConnection.getClientHostAndPort()+" | findstr ESTABLISHED";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null){
                String[] split = s.split(" ");

                List<String> realSplit = new ArrayList<>();
                for (String spli : split) {
                    if (!spli.equals("") && !spli.equals(" ")) {
                        realSplit.add(spli);
                    }
                }

                if (realSplit.get(1).equals(hConnection.getClientHostAndPort())) {
//                    PID = Integer.parseInt(realSplit.get(4));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<byte[]> getRC4possibilities() {
        obtain_PID();
        if (DEBUG) System.out.println("FLASH PROCESS ID: " + PID);

        while (true) {}
//        return null;
    }
}
