package gearth.protocol.portchecker;

import java.io.IOException;

public class WindowsPortChecker implements PortChecker{
    @Override
    public String getProcessUsingPort(int port) throws IOException {
        try {
            String s = getCommandOutput(new String[] {"cmd", "/c", " netstat -ano | findstr LISTENING | findstr " + port});
            int index = s.lastIndexOf(" ");
            String pid = s.substring(index);

            return getProcessNameFromPid(pid);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String getProcessNameFromPid(String pid) throws IOException {
        String task = getCommandOutput(new String[] {"tasklist /fi \"pid eq " + pid + "\" /nh /fo:CSV"});
        int index = task.indexOf(',');
        return task.substring(0, index);
    }
}
