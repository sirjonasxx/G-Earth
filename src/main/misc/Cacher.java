package main.misc;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 05/04/18.
 */
public class Cacher {

    private static String getCacheDir() {
        return System.getProperty("user.home") + File.separator + ".G-Earth" + File.separator;
    }

    public static boolean exists(String key) {
        File f = new File(getCacheDir(), "cache.txt");
        if (f.exists()) {
            try {
                List<String> lines = Files.readAllLines(f.toPath());

                for (String line : lines) {
                    if (line.startsWith(key+":")) {
                        return true;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String get(String key) {
        File f = new File(getCacheDir(), "cache.txt");
        if (f.exists()) {
            try {
                List<String> lines = Files.readAllLines(f.toPath());

                for (String line : lines) {
                    if (line.startsWith(key+":")) {
                        return line.split(":")[1];
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static void remove(String key) {
        File targetFile = new File(getCacheDir() + File.separator + "cache.txt");
        File parent = targetFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
        if (!targetFile.exists()) {
            try {
                targetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(getCacheDir() + File.separator + "cache.txt");
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (!line.startsWith(key + ":"))
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

    public static void add(String key, String value) {
        File targetFile = new File(getCacheDir() + File.separator + "cache.txt");
        File parent = targetFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
        if (!targetFile.exists()) {
            try {
                targetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        File f = new File(getCacheDir(), "cache.txt");
//        if (!f.exists()) {
//            try {
//                PrintWriter writer = new PrintWriter(f.getPath(), "UTF-8");
//                writer.write("");
//                writer.close();
//            } catch (FileNotFoundException | UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }

        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(getCacheDir() + File.separator + "cache.txt");
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            boolean containmmm = false;
            while ((line = br.readLine()) != null)
            {
                if (line.startsWith(key+":"))
                    containmmm = true;
                lines.add(line);

            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            if (!containmmm)	{
                out.write(key+":"+value);
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

    public static void update(String key, String value) {
        remove(key);
        add(key, value);
    }


    public static void main(String[] args) {
//        System.out.println(exists("hallo"));
//        System.out.println(get("hallo"));
//
//        add("hallo","doei");
//
//        System.out.println(exists("hallo"));
//        System.out.println(get("hallo"));
//
//        remove("hallo");
//        System.out.println(get("hallo"));
        System.out.println(get("PRODUCTION-201804032203-770536283-pingHeader"));
    }
}
