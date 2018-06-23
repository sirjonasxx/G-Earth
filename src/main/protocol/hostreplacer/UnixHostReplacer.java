package main.protocol.hostreplacer;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Jeunez on 04/04/18.
 */
class UnixHostReplacer implements HostReplacer {

    protected String hostsFileLocation;

    UnixHostReplacer() {
        hostsFileLocation = "/etc/hosts";
    }

    @Override
    public void addRedirect(String original, String redirect) {
        String text = redirect + " " + original + "\t# G-Earth replacement";

        FileReader fr = null;
        BufferedReader br = null;
        FileWriter fw = null;
        BufferedWriter out = null;

        try
        {
            ArrayList<String> lines = new ArrayList<>();
            File f1 = new File(hostsFileLocation);
            fr = new FileReader(f1);
            br = new BufferedReader(fr);
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

            fw = new FileWriter(f1);
            out = new BufferedWriter(fw);

            if (!containmmm)	{
                out.write(text);
            }

            for (int i = 0; i < lines.size(); i++)	{
                out.write(((containmmm && i == 0) ? "" : System.getProperty("line.separator"))+ lines.get(i));
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeRedirect(String original, String redirect) {
        String text = redirect + " " + original + "\t# G-Earth replacement";

        FileReader fr = null;
        BufferedReader br = null;
        FileWriter fw = null;
        BufferedWriter out = null;

        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            fr = new FileReader(f1);
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (!line.equals(text))
                    lines.add(line);

            }
            fr.close();
            br.close();

            fw = new FileWriter(f1);
            out = new BufferedWriter(fw);

            for (int i = 0; i < lines.size(); i++)	{
                out.write(lines.get(i));
                if (i != lines.size() - 1) out.write(System.getProperty("line.separator"));
            }
            out.flush();
            fw.close();
            out.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
