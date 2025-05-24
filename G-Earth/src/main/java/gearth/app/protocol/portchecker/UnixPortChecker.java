package gearth.app.protocol.portchecker;

import java.io.IOException;

public class UnixPortChecker implements PortChecker {

    @Override
    public String getProcessUsingPort(int port) throws IOException {
        String netstatOut = getCommandOutput(new String[] {"/bin/sh", "-c", " netstat -nlp | grep :" + port});
        int index = netstatOut.lastIndexOf("LISTEN      ");
        return netstatOut.substring(index + 12);
    }
}
