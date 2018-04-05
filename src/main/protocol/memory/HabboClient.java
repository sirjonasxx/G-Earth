package main.protocol.memory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class HabboClient {


    private static final String[] potentialProcessNames = {"--ppapi-flash-args", "plugin-container"};

    private int PID;
    private List<long[]> maps;

    private static final boolean DEBUG = false;

    public static HabboClient create() {
        File folder = new File("/proc");
                HabboClient client = null;

        do {
            File[] fileList = folder.listFiles();
            for (File file : fileList) {
                if (file.isDirectory() && stringIsNumeric(file.getName())) {
                    String path = "/proc/" + file.getName() + "/cmdline";
                    boolean isHabboProcess = false;
                    for (String s : potentialProcessNames) {
                        if (fileContainsString(path, s)) {
                            isHabboProcess = true;
                            break;
                        }
                    }
                    if (isHabboProcess) {
                        client = new HabboClient();
                        client.PID = Integer.parseInt(file.getName());
                        client.maps = new ArrayList<>();
                    }
                }
            }
        } while (client == null);


        if (DEBUG) System.out.println("* Found flashclient process: " + client.PID);
        return client;
    }
    public void refreshMemoryMaps() {
        String filename = "/proc/"+this.PID+"/maps";
        BufferedReader reader;
        maps = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(filename));
            String line;

            while ((line = reader.readLine()) != null)	{
                String[] split = line.split("[- ]");
                if (split[2].startsWith("rw")) {  //if (split[2].startsWith("rw")) {

                    try {
                        long start = Long.parseLong(split[0], 16);
                        long end = Long.parseLong(split[1], 16);
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

    public List<MemorySnippet> createMemorySnippetList () {
        refreshMemoryMaps();
        List<MemorySnippet> result = new ArrayList<>();

        for (long[] map : maps) {
            long begin = map[0];
            long end = map[1];

            MemorySnippet snippet = new MemorySnippet(begin, new byte[(int)(end - begin)] );
            result.add(snippet);
        }
        return result;
    }
    public void fetchMemory(List<MemorySnippet> snippets) {
        for (MemorySnippet snippet : snippets) {
            fetchMemory(snippet);
        }
    }
    public void fetchMemory(MemorySnippet snippet) {
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
    public List<MemorySnippet> differentiate2(List<MemorySnippet> original, int minChangedBytes, int maxChangedBytes, int range) {
        List<MemorySnippet> upToDate = new ArrayList<>();
        for (MemorySnippet memorySnippet : original) {
            upToDate.add(new MemorySnippet(memorySnippet.getOffset(), new byte[memorySnippet.getData().length]));
        }
        fetchMemory(upToDate);
        List<MemorySnippet> result = new ArrayList<>();
        Queue<Integer> wachter = new LinkedList<>();
        for (int i = 0; i < original.size(); i++) {
            wachter.clear();
            int wachtersize = 0;

            MemorySnippet org = original.get(i);
            byte[] orgdata = org.getData();
            MemorySnippet upd = upToDate.get(i);
            byte[] upddata = upd.getData();

            int curstartoffset = -1;
            int lastendbuffer = -1;

            for (int p = 0; p < org.getData().length; p++) {
                if (wachtersize > 0 && p == wachter.peek()) {
                    wachter.poll();
                    wachtersize--;
                }
                if (orgdata[p] != upddata[p]) {
                    wachter.add(p + range);
                    wachtersize++;
                }

                if (p >= range - 1 && wachtersize >= minChangedBytes && wachtersize <= maxChangedBytes) {
                    if (curstartoffset == -1) {
                        curstartoffset = p - range + 1;
                    }
                    else if (lastendbuffer < p - range) {
                        MemorySnippet snippet = new MemorySnippet(curstartoffset + org.getOffset(), new byte[lastendbuffer - curstartoffset + 1]);
                        result.add(snippet);
                        curstartoffset = p - range + 1;
                    }
                    lastendbuffer = p;
                }
            }
            if (curstartoffset != -1) {
                MemorySnippet snippet = new MemorySnippet(curstartoffset + org.getOffset(), new byte[lastendbuffer - curstartoffset + 1]);
                result.add(snippet);
            }
        }
        fetchMemory(result);
        return result;
    }

    @SuppressWarnings("Duplicates")
    public void pauseProcess() {
        String[] args = new String[] {"kill", "-STOP", PID+""};
        Process proc;
        try {
            proc = new ProcessBuilder(args).start();
            proc.waitFor();
            proc.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("Duplicates")
    public void resumeProcess()  {
        String[] args = new String[] {"kill", "-CONT", PID+""};
        Process proc;
        try {
            proc = new ProcessBuilder(args).start();
            proc.waitFor();
            proc.destroy();
        } catch (IOException | InterruptedException e) {
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
}
