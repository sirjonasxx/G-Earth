package main.protocol.memory.habboclient.windows;

//import com.sun.jna.Memory;
//import com.sun.jna.Native;
//import com.sun.jna.Pointer;
//import com.sun.jna.platform.win32.Kernel32;
//import com.sun.jna.platform.win32.User32;
//import com.sun.jna.platform.win32.WinBase;
//import com.sun.jna.platform.win32.WinNT;
//import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import main.protocol.HConnection;
import main.protocol.memory.habboclient.HabboClient;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/**
 * Created by Jeunez on 27/06/2018.
 */


/*
 * not functional class
 */

public class WindowsHabboClient extends HabboClient {
    public WindowsHabboClient(HConnection connection) {
        super(connection);
    }

    private static final boolean DEBUG = false;
    private List<WindowsTask> possibleFlashTasks;

    static Kernel32 kernel32 = (Kernel32) Native.loadLibrary("kernel32",Kernel32.class);
    static User32 user32 = (User32) Native.loadLibrary("user32", User32.class);

    public static int PROCESS_VM_READ= 0x0010;
    public static int PROCESS_VM_WRITE = 0x0020;
    public static int PROCESS_VM_OPERATION = 0x0008;

    static class WindowsTask {
        public String name;
        public int PID;
        public String session_name;
        public int sessionNumber;
        public int mem_usage;

        public WindowsTask(String name, int PID, String sessions_name, int sessionNumber, int mem_usage) {
            this.name = name;
            this.PID = PID;
            this.session_name = sessions_name;
            this.sessionNumber = sessionNumber;
            this.mem_usage = mem_usage;
        }

        @Override
        public String toString() {
            return "name: " + name + ", PID: " + PID + ", memory: " + mem_usage;
        }
    }

    private static List<String> execute_command(String command) {
        List<String> result = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader=new BufferedReader( new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null){
                result.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    private static List<String> splitStringExtra(String s, String regex ) {
        String[] split = s.split(regex);

        List<String> realSplit = new ArrayList<>();
        for (String spli : split) {
            if (!spli.equals("") && !spli.equals(" ")) {
                realSplit.add(spli);
            }
        }

        return realSplit;
    }
    private static List<WindowsTask> parseTaskList(List<String> lines) {
        List<WindowsTask> windowsTasks = new ArrayList<>();

        final int ARG_COUNT = 5;
        boolean listHasStarted = false;
        int[] paramLengths = new int[ARG_COUNT];
        for (String line : lines) {

            if (!listHasStarted && line.startsWith("=")) {
                List<String> splitted = splitStringExtra(line, " ");
                if (splitted.size() == ARG_COUNT) {
                    listHasStarted = true;
                    for (int i = 0; i < ARG_COUNT; i++) {
                        paramLengths[i] = splitted.get(i).length();
                    }
                }
            }
            else if (listHasStarted && splitStringExtra(line, " ").size() >= 5) {
                int v = 0;
                String[] args = new String[ARG_COUNT];
                for (int i = 0; i < ARG_COUNT; i++) {
                    int endindex = v + paramLengths[i];
                    args[i] = trim(line.substring(v, endindex));
                    v = endindex + 1;
                }

                WindowsTask task = new WindowsTask(
                        args[0],
                        Integer.parseInt(args[1]),
                        args[2],
                        Integer.parseInt(args[3]),
                        obtainMemorySizeFromCMDString(args[4])
                );

                windowsTasks.add(task);
            }

        }

        return windowsTasks;
    }
    private static String trim(String s) {
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') start++;
            else break;
        }

        int end = s.length();
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == ' ') end--;
            else break;
        }

        return s.substring(start, end);
    }
    private static int obtainMemorySizeFromCMDString(String s) {
        s =    s.replaceAll("[^0-9A-Z]","")
                .replace("K","000")
                .replace("M", "000000")
                .replace("G", "000000000");
        return Integer.parseInt(s);
    }

    private void obtain_PIDs() {
        int headPID = -1;

        String command1 = "cmd /C netstat -a -o -n | findstr "+hConnection.getClientHostAndPort()+" | findstr ESTABLISHED";
        List<String> connections = execute_command(command1);
        for (String s : connections) {
            List<String> realSplit = splitStringExtra(s, " ");

            if (realSplit.size() > 1 && realSplit.get(1).equals(hConnection.getClientHostAndPort())) {
                headPID = Integer.parseInt(realSplit.get(4));
            }
        }



        String command2 = "cmd /C tasklist";
        List<String> tasks = execute_command(command2);
        List<WindowsTask> taskList = parseTaskList(tasks);

        WindowsTask matchWithPID = null;
        int i = 0;
        while (matchWithPID == null && i < taskList.size()) {
            WindowsTask task = taskList.get(i);
            if (task.PID == headPID) {
                matchWithPID = task;
            }
            i++;
        }

        possibleFlashTasks = new ArrayList<>();
        if (matchWithPID != null) {
            for (WindowsTask task : taskList) {
                if (task.name.equals(matchWithPID.name)) {
                    possibleFlashTasks.add(task);
                }
            }
        }
    }

    private ArrayList<String> readPossibleBytes() throws IOException, URISyntaxException {
        ProcessBuilder pb = new ProcessBuilder(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + "\\G-WinMem.exe");
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
        System.out.println("Getting Win Possibilities");
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

        /*

        enableDebugPrivilege();
        obtain_PIDs();

        List<byte[]> possibilities = new ArrayList<>();

        int[] count = {0};
        for (int i = 0; i < possibleFlashTasks.size(); i++) {
            WindowsTask task = possibleFlashTasks.get(i);
            if (DEBUG) System.out.println("Potential task " + task);

            new Thread(() -> {
                List<byte[]> sublist = getRC4possibilities(task.PID, task.mem_usage);

                synchronized (count) {
                    possibilities.addAll(sublist);
                    count[0] ++;
                }

            }).start();
        }

        while (count[0] != possibleFlashTasks.size() + 1) { // the +1 is temporary, to keep this function blocking untill it's functional
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return possibilities;
        */
    }

    public List<byte[]> getRC4possibilities(int processID, int processMemorySize) {
        List<byte[]> result = new ArrayList<>();

        WinNT.HANDLE process = kernel32.OpenProcess(PROCESS_VM_READ|PROCESS_VM_OPERATION, true, processID);

        Memory out = new Memory(processMemorySize);
        IntByReference t = new IntByReference();
        kernel32.ReadProcessMemory(process, process.getPointer(), out, processMemorySize, t);


        System.out.println("read " + t.getValue());


        int[] counter = new int[256];
        int p = 0;
        while (p < out.size()) {
            counter[((out.getByte(p)) + 256) % 256] ++;
            p += 4;
        }

        HashMap<Integer, ArrayList<Integer>> mapper = new HashMap<>();
        HashSet<Integer> allvalues = new HashSet<>();
        for (int i = 0; i < counter.length; i++) {
            if (!mapper.containsKey(counter[i])) {
                mapper.put(counter[i], new ArrayList<>());
            }
            mapper.get(counter[i]).add(i);
            allvalues.add(counter[i]);
        }
//        System.out.println(allvalues.size());
        ArrayList<Integer> allvalues2 = new ArrayList<>(allvalues);
        allvalues2.sort(Integer::compareTo);

        StringBuilder sttt = new StringBuilder();
        sttt.append("process ").append(processID).append(", ");
        for (int i = 1; i < Math.min(4, allvalues2.size()+1); i++) {
            int occ = allvalues2.get(allvalues2.size() - i);
            sttt    .append(i)
                    .append(": ")
                    .append(mapper.get(occ).get(0))
                    .append(" with ")
                    .append(occ)
                    .append(" occurences, ");
        }
        System.out.println(sttt);






        System.out.println(process.getPointer());

        return result;
    }

    private static void enableDebugPrivilege() {
        WinNT.HANDLEByReference hToken = new WinNT.HANDLEByReference();
        boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, hToken);
        if (!success) {
            System.out.println("OpenProcessToken failed. Error: {}" + Native.getLastError());
            return;
        }
        WinNT.LUID luid = new WinNT.LUID();
        success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
        if (!success) {
            System.out.println("LookupprivilegeValue failed. Error: {}" + Native.getLastError());
            return;
        }
        WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
        tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new WinDef.DWORD(WinNT.SE_PRIVILEGE_ENABLED));
        success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
        if (!success) {
            System.out.println("AdjustTokenPrivileges failed. Error: {}" + Native.getLastError());
        }
        Kernel32.INSTANCE.CloseHandle(hToken.getValue());
    }

    public static void main(String[] args) {
        String command2 = "cmd /C tasklist";
        List<String> tasks = execute_command(command2);
        List<WindowsTask> taskList = parseTaskList(tasks);

        System.out.println("t");
    }

}
