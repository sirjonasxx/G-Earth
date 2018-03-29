package main.protocol.memory;

import main.irrelevant.Timer;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlashClient {


    public class MemorySnippet {
        long offset;
        byte[] data;

        public MemorySnippet(long offset, byte[] data) {
            this.offset = offset;
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }

        public long getOffset() {
            return offset;
        }
    }

    private int PID;
    private List<long[]> maps;

    public static FlashClient create() {
        File folder = new File("/proc");
                FlashClient client = null;

        do {
            File[] fileList = folder.listFiles();
            for (File file : fileList) {
                if (file.isDirectory() && MemoryUtils.stringIsNumeric(file.getName())) {
                    String path = "/proc/" + file.getName() + "/cmdline";
                    if (MemoryUtils.fileContainsString(path, "--ppapi-flash-args") ||
                            MemoryUtils.fileContainsString(path, "plugin-container")) {
                        client = new FlashClient();
                        client.PID = Integer.parseInt(file.getName());
                        client.maps = new ArrayList<>();
                    }
                }
            }
        } while (client == null);


        System.out.println("* Found flashclient process: " + client.PID);
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

        System.out.println("* Found memory maps (amount: " + maps.size() + ")");
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
        String memoryPath = "/proc/" + PID + "/mem";
        for (MemorySnippet snippet : snippets) {
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
    }

    /**
     * can remove & add & edit maps of memorysnipperlist
     */
    public void updateMapLocationsSnippetList(List<MemorySnippet> snippets) {
        refreshMemoryMaps();

        List<MemorySnippet> list2 = new ArrayList<>();
        List<long[]> not_added = new ArrayList<>();

        // readd all maps that stayed the same
        for (long[] map : maps) {
            //if was in  snippetlist:
            boolean found = false;
            for (MemorySnippet snippet : snippets) {
                if (snippet.offset == map[0] && snippet.offset + snippet.getData().length == map[1]) {
                    list2.add(snippet);
                    snippets.remove(snippet);
                    found = true;
                    break;
                }
            }
            if (!found) {
                not_added.add(map);
            }

        }

        for (int i = 0; i < not_added.size(); i++) {
            long[] map = not_added.get(i);
            //find potential overlap snippet
            MemorySnippet snippet = new MemorySnippet(map[0], new byte[(int)(map[1] - map[0])]);
            byte[] data = snippet.getData();

            for (MemorySnippet potential : snippets) {
                //if there is overlap
                if ((potential.offset >= map[0] && potential.offset < map[1]) ||
                        (potential.offset + potential.getData().length >= map[0] && potential.offset + potential.getData().length< map[1]) ||
                        (potential.offset < map[0] && potential.offset + potential.getData().length >= map[1])  ) {

                    int start = Math.max((int)(potential.offset - map[0]), 0);
                    int offset2 = -(int)(potential.offset - map[0]);

                    for (int j = start; j < Math.min(map[1] - map[0], potential.getData().length - offset2); j++) {
                        data[j] = potential.getData()[j+offset2];
                    }
                }
            }
            list2.add(snippet);
        }

        snippets.clear();
        for (MemorySnippet snippet : list2) {
            snippets.add(snippet);
        }

    }
    /**
     * creates a new memorysnippet list of data that changed (or not) from the original list with a given left&right buffer
     */
    public List<MemorySnippet> differentiate(List<MemorySnippet> original, boolean isChanged, int leftRightbytebuffer) {
        //fill a new memorysnippet list with live data
        List<MemorySnippet> upToDate = new ArrayList<>();
        for (MemorySnippet memorySnippet : original) {
            upToDate.add(new MemorySnippet(memorySnippet.getOffset(), new byte[memorySnippet.getData().length]));
        }
        fetchMemory(upToDate);

        List<MemorySnippet> result = new ArrayList<>();

        long totalBytes = 0;

        for (int i = 0; i < original.size(); i++) {
            long offset = original.get(i).getOffset();
            byte[] old = original.get(i).getData();
            byte[] curr = upToDate.get(i).getData();

            if (!isChanged) {
                // find all non-changed stuff and put in result
                long pre = offset;
                for (int j = 0; j < old.length; j++) {
                    if (old[j] != curr[j]) {
                        //calculate previous block length
                        int len = j - ((int)(pre - offset));
                        if (len >= leftRightbytebuffer && len > 0) {
                            result.add(new MemorySnippet(pre, new byte[len]));
                            totalBytes += len;
                        }
                        pre = offset + j + 1;
                    }
                }
                int len = old.length - ((int)(pre - offset));
                if (len >= leftRightbytebuffer && len > 0) {
                    result.add(new MemorySnippet(pre, new byte[len]));
                    totalBytes += len;
                }
            }
            else {
                //find all changed stuff and put result
                long pre = offset;
                int downCount = -1; //if downCount reaches zero, buffer should be written out
                for (int j = 0; j < old.length; j++) {
                    if (old[j] != curr[j]) {
                        if (downCount <= 0) {
                            pre = Math.max(offset, offset + j - leftRightbytebuffer);
                        }
                        downCount = leftRightbytebuffer;
                    }
                    else { downCount -= 1; }
                    if (downCount == 0) {
                        int len = j - ((int)(pre - offset));
                        result.add(new MemorySnippet(pre, new byte[len]));
                        totalBytes += len;
                    }
                }
                int len = old.length - ((int)(pre - offset));
                if (downCount > 0 && len >= leftRightbytebuffer) {
                    result.add(new MemorySnippet(pre, new byte[len]));
                    totalBytes += len;
                }
            }
        }
        fetchMemory(result);

        System.out.println("totalbytes after diff: " + totalBytes);

        return result;
    }

    //currently not being used functions:
    List<Long> searchOffsetForByteArray(byte[] toFind) {
        if (toFind.length == 0) return null;

        System.out.println("*** Start searching for: " + new String(toFind));
        String memoryPath = "/proc/" + PID + "/mem";
        List<Long> results = new ArrayList<>();

        Timer t = new Timer();
        t.start();

        for (long[] map : maps) {
            long begin = map[0];
            long end = map[1];
            try {
                RandomAccessFile raf = new RandomAccessFile(memoryPath, "r");
                byte[] wholeBuffer = new byte[(int)(end - begin)];
                raf.seek(begin);
                raf.read(wholeBuffer);
                raf.close();

                for (int i = 0; i < wholeBuffer.length; i++) {

                    int subIndex = 0;
                    while (subIndex < toFind.length && wholeBuffer[i + subIndex] == toFind[subIndex]) subIndex++;

                    if (subIndex == toFind.length) {
                        long result = (long)i + begin;
                        results.add(result);
                        System.out.println("* Found match for " + new String(toFind) + " at address: " + result);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("*** End searching for: " + new String(toFind) + " in " + t.delta() + "ms");
        return results;
    }
    List<Long> searchOffsetForString(String keyString) {
        return searchOffsetForByteArray(keyString.getBytes());
    }

    public List<String> findSharedKey (List<MemorySnippet> snippets){

        byte[] bounds = "09afAF".getBytes();

        List<String> results = new ArrayList<>();

        int size = 54;

        for (MemorySnippet snippet : snippets) {
            int count = 0;
            byte[] data = snippet.getData();
            if (data.length >= size) {
                for (int i = 0; i < data.length; i++) {
                    byte b = data[i];
                    if ((b >= bounds[0] && b <= bounds[1]) ||
                            (b >= bounds[2] && b <= bounds[3]) ||
                            (b >= bounds[4] && b <= bounds[5])) {
                        count++;
                    }
                    else {
                        count = 0;
                    }
                    if (count == size && (i + 1 == data.length || !((data[i+1] >= bounds[0] && data[i+1] <= bounds[1]) ||
                            (data[i+1] >= bounds[2] && data[i+1] <= bounds[3]) ||
                            (data[i+1] >= bounds[4] && data[i+1] <= bounds[5]))) ) {
                        results.add(new String(Arrays.copyOfRange(data, i - size + 1, i + 1)) + " on location: " + (snippet.getOffset() + i));
                    }
                }
            }

        }
        return results;
    }

    public List<String> findSharedKey2() {
        List<MemorySnippet> memorySnippets = new ArrayList<>();

        int buff = 15000000;

        long verystart = maps.get(0)[0];
        long veryend = maps.get(0)[1];

        for (int i = 1; i < maps.size(); i++) {
            long[] map = maps.get(i);
            if (map[1] - veryend <= buff) {
                veryend = map[1];
            }
            else {
                memorySnippets.add(new MemorySnippet(verystart, new byte[(int)(veryend - verystart + buff)]));
                verystart = maps.get(i)[0];
                veryend = maps.get(i)[1];
            }
        }
        memorySnippets.add(new MemorySnippet(verystart, new byte[(int)(veryend - verystart + buff)]));

        fetchMemory(memorySnippets);
        return findSharedKey(memorySnippets);
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

    public static void main(String[] args) throws InterruptedException {
        FlashClient client = FlashClient.create();
        client.refreshMemoryMaps();

//        List<Long> gameHostOccurences = client.searchOffsetForString("game-nl.habbo.com");
//        List<Long> rsaOccurences = client.searchOffsetForString("xIBlMDUyODA4YzFhYmVmNjlhMWE2MmMzOTYzOTZiODU5NTVlMmZmNTIy");
//        List<Long> occ = client.searchOffsetForString("sirjonasxx");

//        List<MemorySnippet> l = client.createMemorySnippetList();
//        client.fetchMemory(l);
//        Thread.sleep(1000);
//        //what didnt change in the last 1000ms?
//        List<MemorySnippet> l2 = client.differentiate(l, false, 0);
//        Thread.sleep(1000);
//        //what changed in the last 1000ms?
//        List<MemorySnippet> l3 = client.differentiate(l2, true, 0);
//        Thread.sleep(1000);
//        //what didnt change in the last 1000ms?
//        List<MemorySnippet> l4 = client.differentiate(l3, false, 0);
//        Thread.sleep(1000);
//        //what changed in the last 1000ms?
//        List<MemorySnippet> l5 = client.differentiate(l4, true, 0);

//        client.updateMapLocationsSnippetList(l);

        List<String> res = client.findSharedKey2();
        System.out.println(res);

        System.out.println("test");
    }

}
