package gearth.protocol.hostreplacer.hostsfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 04/04/18.
 */
class UnixHostReplacer implements HostReplacer {

    private static final Logger LOG = LoggerFactory.getLogger(UnixHostReplacer.class);

    protected String hostsFileLocation;

    UnixHostReplacer() {
        hostsFileLocation = "/etc/hosts";
    }

    @Override
    public boolean addRedirect(String[] lines) {
        List<String> adders = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            adders.add(lines[i] + "\t# G-Earth replacement");
        }

        FileReader fr = null;
        BufferedReader br = null;
        FileWriter fw = null;
        BufferedWriter out = null;

        try
        {
            ArrayList<String> fileLines = new ArrayList<>();
            File f1 = new File(hostsFileLocation);
            fr = new FileReader(f1);
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                adders.remove(line);
                fileLines.add(line);
            }
            fr.close();
            br.close();

            fw = new FileWriter(f1);
            out = new BufferedWriter(fw);

            for (String li : adders) {
                out.write(li + System.lineSeparator());
            }

            for (int i = 0; i < fileLines.size(); i++)	{
                out.write(((i == 0) ? "" : System.lineSeparator())+ fileLines.get(i));
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            LOG.error("Error while adding redirect to hosts file", ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean removeRedirect(String[] lines) {
        ArrayList<String> removers = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            removers.add(lines[i] + "\t# G-Earth replacement");
        }

        FileReader fr = null;
        BufferedReader br = null;
        FileWriter fw = null;
        BufferedWriter out = null;

        try
        {
            ArrayList<String> fileLines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            fr = new FileReader(f1);
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (!removers.contains(line))
                    fileLines.add(line);
            }
            fr.close();
            br.close();

            fw = new FileWriter(f1);
            out = new BufferedWriter(fw);

            for (int i = 0; i < fileLines.size(); i++)	{
                out.write(fileLines.get(i));
                if (i != fileLines.size() - 1) out.write(System.lineSeparator());
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            LOG.error("Error while removing redirect from hosts file", ex);
            return false;
        }

        return true;
    }
}
