package main.protocol.hostreplacer;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Jeunez on 04/04/18.
 */
class LinuxHostReplacer implements HostReplacer {

    protected String hostsFileLocation;

    LinuxHostReplacer() {
        hostsFileLocation = "/etc/hosts";
    }

    @Override
    public void addRedirect(String original, String redirect) {
        String text = redirect + " " + original;

        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            boolean containmmm = false;
            while ((line = br.readLine()) != null)
            {
                if (line.equals(text))
                    containmmm = true;
                lines.add(line);

            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            if (!containmmm)	{
                out.write(text);
            }

            for (int i = 0; i < lines.size(); i++)	{
                out.write("\n"+ lines.get(i));
            }

            out.flush();
            out.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeRedirect(String original, String redirect) {
        String text = redirect + " " + original;

        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (!line.contains(text))
                    lines.add(line);

            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < lines.size(); i++)	{
                out.write(lines.get(i));
                if (i != lines.size() - 1) out.write("\n");
            }
            out.flush();
            out.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
