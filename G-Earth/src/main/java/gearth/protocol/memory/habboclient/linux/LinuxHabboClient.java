package gearth.protocol.memory.habboclient.linux;

import gearth.protocol.HConnection;
import gearth.protocol.memory.habboclient.HabboClient;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class LinuxHabboClient extends HabboClient {


    private static final String[] potentialProcessNames = {"--ppapi-flash-args", "plugin-container"};

    List<PotentialHabboProcess> potentialProcesses = new ArrayList<>();

    private class PotentialHabboProcess {
        public int PID;
        public List<long[]> maps;
    }

    private volatile int PID;
    private volatile List<long[]> maps;

    private static final boolean DEBUG = false;

    public LinuxHabboClient(HConnection connection) {
        super(connection);

        File folder = new File("/proc");

        boolean found = false;

        do {
            File[] fileList = folder.listFiles();
            for (File file : fileList) {
                if (file.isDirectory() && stringIsNumeric(file.getName())) {
                    String path = "/proc/" + file.getName() + "/cmdline";
                    boolean isHabboProcess = false;
                    for (String s : potentialProcessNames) {
                        if (fileContainsString(path, s)) {
                            isHabboProcess = true;
                        }
                    }
                    if (isHabboProcess) {
                        PotentialHabboProcess process = new PotentialHabboProcess();
                        process.PID = Integer.parseInt(file.getName());
                        process.maps = new ArrayList<>();
                        potentialProcesses.add(process);
                        found = true;
                    }
                }
            }
        } while (!found);

        if (DEBUG) System.out.println("* Found flashclient " + potentialProcesses.size() + " potential processes");
    }

    @Override
    public List<byte[]> getRC4cached() {
        return new ArrayList<>();
    }


    private void refreshMemoryMaps() {
        String filename = "/proc/"+this.PID+"/maps";
        BufferedReader reader;
        maps = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(filename));
            String line;

            while ((line = reader.readLine()) != null)	{
                String[] split = line.split("[ ]");
                if (split.length == 5 && split[1].equals("rw-p") && split[2].equals("00000000") && split[3].equals("00:00") && split[4].equals("0")) {  //if (split[2].startsWith("rw")) {

                    try {
                        long start = Long.parseLong(split[0].split("-")[0], 16);
                        long end = Long.parseLong(split[0].split("-")[1], 16);
                        maps.add(new long[]{start, end});
                    }
                    catch (Exception e){
                        //this is nothing really
                    }

                }
            }
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (DEBUG) System.out.println("* Found memory maps (amount: " + maps.size() + ")");
    }

    private static List<LinuxMemorySnippet> createMemorySnippetList (List<long[]> maps) {
        List<LinuxMemorySnippet> result = new ArrayList<>();

        for (long[] map : maps) {
            long begin = map[0];
            long end = map[1];

            LinuxMemorySnippet snippet = new LinuxMemorySnippet(begin, new byte[(int)(end - begin)] );
            result.add(snippet);
        }
        return result;
    }

    private void fetchMemory(List<LinuxMemorySnippet> snippets) {
        for (LinuxMemorySnippet snippet : snippets) {
            fetchMemory(snippet);
        }
    }
    private void fetchMemory(LinuxMemorySnippet snippet) {
        String memoryPath = "/proc/" + PID + "/mem";
        long begin = snippet.offset;
        try {
            RandomAccessFile raf = new RandomAccessFile(memoryPath, "r");
            raf.seek(begin);
            raf.read(snippet.getData());
            raf.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    static boolean stringIsNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
    static boolean fileContainsString(String path, String contains) {

        try {
            List<String> lines = Files.readAllLines(new File(path).toPath());
            for (String line : lines) {
                if (line.contains(contains)) return true;
            }
        } catch (Exception e) {
            // process of specified path not running anymore
        }
        return false;

    }

    public List<byte[]> getRC4possibilities() {

        int offset = 4;
        List<byte[]> resultSet = new ArrayList<>();

        for (PotentialHabboProcess process : potentialProcesses) {
            PID = process.PID;
            maps = process.maps;

            List<LinuxMemorySnippet> possibilities = createMemorySnippetListForRC4();
            fetchMemory(possibilities);

            for (LinuxMemorySnippet snippet : possibilities) {
                if (snippet.getData().length >= 1024 && snippet.getData().length <= 1024+2*offset) {
                    for (int i = 0; i < (snippet.getData().length - ((256 - 1) * offset)); i+=offset) {
                        byte[] wannabeRC4data = Arrays.copyOfRange(snippet.getData(), i, 1024 + i);
                        byte[] data = new byte[256]; // dis is the friggin key

                        boolean isvalid = true;
                        for (int j = 0; j < 1024; j++) {
                            if (j % 4 != 0 && wannabeRC4data[j] != 0) {
                                isvalid = false;
                                break;
                            }
                            if (j % 4 == 0) {
                                data[j/4] = wannabeRC4data[j];
                            }
                        }
                        if (isvalid) {
                            resultSet.add(data);
                        }
                    }
                }
            }
        }
        return resultSet;
    }

    private List<LinuxMemorySnippet> createMemorySnippetListForRC4() {

        Object lock = new Object();

        refreshMemoryMaps();
        String memoryPath = "/proc/" + PID + "/mem";

        int[] count = {0};

        List<LinuxMemorySnippet> result = new ArrayList<>();
        for (long[] map : maps) {
            new Thread(() -> {
                int offset = 4;
                long start = map[0];
                long end = map[1];

                byte[] data = new byte[(int)(end - start)];
                try {
                    RandomAccessFile raf = new RandomAccessFile(memoryPath, "r");
                    raf.seek(start);
                    raf.read(data);
                    raf.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }


                int maskCount = 0;
                int[] nToMap = new int[256];
                int[] removeMap = new int[256];
                for (int i = 0; i < removeMap.length; i++) {
                    removeMap[i] = -1;
                    nToMap[i] = -1;
                }


                int matchStart = -1;
                int matchEnd = -1;

                for (int i = 0; i < data.length; i+=offset) {
                    int b = (((int)data[i]) + 128) % 256;
                    int indInMap = (i/4) % 256;

                    int deletedNumber = removeMap[indInMap];
                    if (deletedNumber != -1) {
                        nToMap[deletedNumber] = -1;
                        maskCount --;
                        removeMap[indInMap] = -1;
                    }

                    if (nToMap[b] == -1) {
                        maskCount ++;
                        removeMap[indInMap] = b;
                        nToMap[b] = indInMap;
                    }
                    else {
                        removeMap[nToMap[b]] = -1;
                        removeMap[indInMap] = b;
                        nToMap[b] = indInMap;
                    }

                    if (maskCount == 256) {
                        if (matchStart == -1) {
                            matchStart = i - ((256 - 1) * offset);
                            matchEnd = i;
                        }

                        if (matchEnd < i - ((256 - 1) * offset)) {
                            result.add(new LinuxMemorySnippet(start + matchStart, new byte[matchEnd - matchStart + 4]));
                            matchStart = i - ((256 - 1) * offset);
                        }
                        matchEnd = i;
                    }

                }

                synchronized (lock) {
                    if (matchStart != -1) {
                        result.add(new LinuxMemorySnippet(start + matchStart, new byte[matchEnd - matchStart + 4]));
                    }
                    count[0] ++;
                }

            }).start();
        }

        while (count[0] < maps.size()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
