package gearth.protocol.hostreplacer.hostsfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jonas on 04/04/18.
 */
class UnixHostReplacer implements HostReplacer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixHostReplacer.class);

    protected String hostsFileLocation;

    UnixHostReplacer() {
        hostsFileLocation = "/etc/hosts";
    }

    @Override
    public void addRedirect(String[] lines) {

        final List<String> adders = appendCommentToEachLine(lines);

        FileReader fr;
        BufferedReader br;
        FileWriter fw;
        BufferedWriter out;
        try
        {
            final ArrayList<String> fileLines = new ArrayList<>();
            final File hostsFile = new File(hostsFileLocation);

            LOGGER.debug("Replacing hosts at {}", hostsFile.getAbsolutePath());

            fr = new FileReader(hostsFile);
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null)
            {
                adders.remove(line);
                fileLines.add(line);
            }
            fr.close();
            br.close();

            fw = new FileWriter(hostsFile);
            out = new BufferedWriter(fw);

            for (String li : adders) {
                out.write(li + System.getProperty("line.separator"));
            }

            for (int i = 0; i < fileLines.size(); i++)	{
                out.write(((i == 0) ? "" : System.getProperty("line.separator"))+ fileLines.get(i));
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to add host redirects", ex);
        }
    }

    @Override
    public void removeRedirect(String[] lines) {
        final List<String> removers = appendCommentToEachLine(lines);

        FileReader fr;
        BufferedReader br;
        FileWriter fw;
        BufferedWriter out;

        try
        {
            final ArrayList<String> fileLines = new ArrayList<>();
            final File hostsFile = new File(hostsFileLocation);
            fr = new FileReader(hostsFile);
            br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null)
            {
                if (!removers.contains(line))
                    fileLines.add(line);
            }
            fr.close();
            br.close();

            fw = new FileWriter(hostsFile);
            out = new BufferedWriter(fw);

            for (int i = 0; i < fileLines.size(); i++)	{
                out.write(fileLines.get(i));
                if (i != fileLines.size() - 1)
                    out.write(System.getProperty("line.separator"));
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to remove host replace lines", ex);
        }
    }

    private static List<String> appendCommentToEachLine(String[] lines) {
        return Arrays
                .stream(lines)
                .map(line -> line + "\t# G-Earth replacement")
                .collect(Collectors.toList());
    }
}
