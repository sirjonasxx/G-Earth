package gearth.protocol.memory.habboclient.macos;

import gearth.misc.StringUtils;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.memory.habboclient.HabboClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link  HabboClient} implementation for the MacOS operating system.
 *
 * @author sirjonasxx / dorving (revised)
 */
public class MacOSHabboClient extends HabboClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MacOSHabboClient.class);

    private static final String G_MEM_EXECUTABLE_FILE_NAME = "/g_mem_mac";

    /**
     * The header id (opcode) of the packet that contains the value for the {@link #production} field.
     */
    private static final int PRODUCTION_ID = 4000;

    private String production = "";

    /**
     * Create a new {@link MacOSHabboClient} instance.
     *
     * @param connection the {@link HConnection connection} with the Habbo server.
     */
    public MacOSHabboClient(HConnection connection) {
        super(connection);
        listenForProductionPacket(connection);
    }

    private void listenForProductionPacket(HConnection connection) {
        connection.addTrafficListener(0, message -> {
            if (message.getDestination() == HMessage.Direction.TOSERVER) {
                final HPacket packet = message.getPacket();
                if (packet.headerId() == PRODUCTION_ID) {
                    production = packet.readString();
                    LOGGER.debug("Read production packet from connection {}, set `production` to {}", connection, production);
                }
            }
        });
    }

    @Override
    public List<byte[]> getRC4cached() {
        return new ArrayList<>();
    }


    @Override
    public List<byte[]> getRC4possibilities() {
        final List<byte[]> result = new ArrayList<>();
        try {
            for (String possibleHexStr : readPossibleBytes())
                result.add(StringUtils.hexStringToByteArray(possibleHexStr));
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Failed to parse line as hex string", e);
        }
        return result;
    }

    private ArrayList<String> readPossibleBytes() throws IOException, URISyntaxException {
        final String pathToGMemExecutable = getPathToGMemExecutable();
        final String clientHost = hConnection.getClientHost();
        final String clientPort = Integer.toString(hConnection.getClientPort());
        LOGGER.debug("Attempting to execute G-Mem executable {} with host {} at port {}", pathToGMemExecutable, clientHost, clientPort);
        final Process process = new ProcessBuilder(pathToGMemExecutable, clientHost, clientPort)
                .start();
        final ArrayList<String> possibleData = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            int count = 0;
            String line;
            while((line = reader.readLine()) !=  null) {
                if (line.length() > 1 && (count++ % 2 != 0))
                    possibleData.add(line);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute G-Mem", e);
        } finally {
            process.destroy();
        }
        LOGGER.debug("Read {} from G-Mem output stream", possibleData);
        return possibleData;
    }

    private String getPathToGMemExecutable() throws URISyntaxException {
        return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + G_MEM_EXECUTABLE_FILE_NAME;
    }
}
