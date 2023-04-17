package gearth.services.unity_tools;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import wasm.disassembly.InvalidOpCodeException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UnityWebModifyer {

    public final static String UNITY_DATA = "habbo2020-global-prod.data.gz";
    public final static String UNITY_CODE = "habbo2020-global-prod.wasm.gz";
    public final static String UNITY_FRAMEWORK = "habbo2020-global-prod.framework.js.gz";
    public final static String UNITY_LOADER = "habbo2020-global-prod.loader.js";

    private final static String UNITYFILES_URL = "https://images.habbo.com/habbo-webgl-clients/{revision}/WebGL/habbo2020-global-prod/Build/";

    private String revision;
    private File saveFolder;
    private String currentUrl;


    public synchronized boolean modifyAllFiles(String revision, String saveFolderName) {
        this.revision = revision;
        currentUrl = UNITYFILES_URL.replace("{revision}", revision);
        saveFolder = new File(saveFolderName);

        if (saveFolder.exists()) {
            return true;
        }
        saveFolder.mkdirs();

        try {
            modifyDataFile();
            modifyCodeFile();
            modifyFrameworkFile();
            modifyUnityLoader();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                FileUtils.deleteDirectory(saveFolder);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private void downloadToFile(URL url, File file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
            fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
        fileOutputStream.close();
        in.close();
    }
    //
    private void modifyDataFile() throws IOException {
        File dataFile = new File(saveFolder, UNITY_DATA);
        URL dataUrl = new URL(currentUrl + UNITY_DATA);
        downloadToFile(dataUrl, dataFile);
    }

    private void modifyCodeFile() throws IOException, InvalidOpCodeException {
        File codeFile = new File(saveFolder, UNITY_CODE);
        URL codeUrl = new URL(currentUrl + UNITY_CODE);
        downloadToFile(codeUrl, codeFile);

        new WasmCodePatcher(codeFile.getAbsolutePath()).patch();
    }


    private String insertFrameworkCode(String fileContents, int index, String codeName) throws IOException {
        BufferedReader code = new BufferedReader(new InputStreamReader(UnityWebModifyer.class.getResourceAsStream(codeName), StandardCharsets.UTF_8));

        String firstPart = fileContents.substring(0, index);
        String lastPart = fileContents.substring(index);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(firstPart);

        stringBuilder.append("\n");
        String line;
        while ((line = code.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }

        stringBuilder.append(lastPart);
        return stringBuilder.toString();
    }

    private void modifyFrameworkFile() throws IOException {
        File frameworkFile = new File(saveFolder, UNITY_FRAMEWORK);
        URL frameworkUrl = new URL(currentUrl + UNITY_FRAMEWORK);
        downloadToFile(frameworkUrl, frameworkFile);


        byte[] encoded = IOUtils.toByteArray(new GZIPInputStream(new FileInputStream(frameworkFile)));
        String contents = new String(encoded, StandardCharsets.UTF_8);

        contents = insertFrameworkCode(contents, 0, "js_code/unity_code.js");

        String exportSearch = "Module[\"asm\"]=exports;";
        int exportIndex = contents.indexOf(exportSearch) + exportSearch.length();
        contents = insertFrameworkCode(contents, exportIndex, "js_code/unity_exports.js");

        String importSearch = "\"LANG\":lang,\"_\":getExecutableName()};";
        int importIndex = contents.indexOf(importSearch) + importSearch.length();
        contents = insertFrameworkCode(contents, importIndex, "js_code/unity_imports.js");

        contents = contents
                .replace("var _free", "_free")
                .replace("var _malloc", "_malloc")
                .replace("var Module=typeof Module!==\"undefined\"?Module:{};", "var Module=typeof Module!==\"undefined\"?Module:{}; _module = Module")
                .replace("{{RevisionName}}", revision);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(frameworkFile))));
        writer.write(contents);
        writer.close();
    }

    private void modifyUnityLoader() throws IOException {
        File loaderFile = new File(saveFolder, UNITY_LOADER);
        URL loaderUrl = new URL(currentUrl + UNITY_LOADER);
        downloadToFile(loaderUrl, loaderFile);

        byte[] encoded = Files.readAllBytes(Paths.get(loaderFile.getAbsolutePath()));
        String contents = new String(encoded, StandardCharsets.UTF_8);

        contents = contents.replace("r.headers.get(e)==t.headers.get(e)", "false");
//        contents = contents.replace("a.responseHeaders[e]=o.getResponseHeader(e)",
//                "const genRanHex = size => [...Array(size)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');\n" +
//                        "                if (e === \"ETag\") {\n" +
//                        "                    a.responseHeaders[e] = \"W/\\\"\" + genRanHex(6) + \"-\" + genRanHex(13) + \"\\\"\"\n" +
//                        "                }\n" +
//                        "                else {\n" +
//                        "                    a.responseHeaders[e] = o.getResponseHeader(e)\n" +
//                        "                }");
        contents = contents.replace("!r.headers.get(\"ETag\")", "!r.headers.set(\"Etag\", `W/\"${[...Array(6)].map(() => Math.floor(Math.random() * 16).toString(16)).join('')}-${[...Array(13)].map(() => Math.floor(Math.random() * 16).toString(16)).join('')}\"`)");

        BufferedWriter writer = new BufferedWriter(new FileWriter(loaderFile));
        writer.write(contents);
        writer.close();
    }


}
