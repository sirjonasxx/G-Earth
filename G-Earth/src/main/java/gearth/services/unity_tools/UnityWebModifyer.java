package gearth.services.unity_tools;

import org.codehaus.plexus.util.FileUtils;
import wasm.disassembly.InvalidOpCodeException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UnityWebModifyer {

    public final static String UNITY_PROD = "habbo2020-global-prod.json";
    public final static String UNITY_DATA = "habbo2020-global-prod.data.unityweb";
    public final static String UNITY_CODE = "habbo2020-global-prod.wasm.code.unityweb";
    public final static String UNITY_FRAMEWORK = "habbo2020-global-prod.wasm.framework.unityweb";
    public final static String UNITY_LOADER = "UnityLoader.js";

    private final static String UNITYFILES_URL = "https://images.habbo.com/habbo-webgl-clients/{revision}/WebGL/habbo2020-global-prod/Build/";

    private final String revision;
    private final File saveFolder;
    private final String currentUrl;


    public UnityWebModifyer(String revision, String saveFolder) {
        this.revision = revision;
        this.currentUrl = UNITYFILES_URL.replace("{revision}", revision);
        this.saveFolder = new File(saveFolder);
    }

    public boolean modifyAllFiles() {
        if (saveFolder.exists()) {
            return true;
        }
        saveFolder.mkdirs();

        try {
            modifyProdFile();
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

    // return urls for: data, code & framework file
    private void modifyProdFile() throws IOException {
        String prodUrl = currentUrl + UNITY_PROD;

        URLConnection connection = new URL(prodUrl).openConnection();
        InputStream is = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        FileWriter fileWriter = new FileWriter(new File(saveFolder, UNITY_PROD));
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        String line;
        while ((line = in.readLine()) != null) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
        in.close();
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


        byte[] encoded = Files.readAllBytes(Paths.get(frameworkFile.getAbsolutePath()));
        String contents = new String(encoded, StandardCharsets.UTF_8);

        contents = insertFrameworkCode(contents, 0, "js_code/unity_code.js");

        String exportSearch = "Module.asmLibraryArg,buffer);Module[\"asm\"]=asm;";
        int exportIndex = contents.indexOf(exportSearch) + exportSearch.length();
        contents = insertFrameworkCode(contents, exportIndex, "js_code/unity_exports.js");

        String importSearch = "if(!env[\"tableBase\"]){env[\"tableBase\"]=0}";
        int importIndex = contents.indexOf(importSearch) + importSearch.length();
        contents = insertFrameworkCode(contents, importIndex, "js_code/unity_imports.js");

        contents = contents
                .replace("var _free", "_free")
                .replace("var _malloc", "_malloc")
                .replace("{{RevisionName}}", revision);

        BufferedWriter writer = new BufferedWriter(new FileWriter(frameworkFile));
        writer.write(contents);
        writer.close();
    }

    private void modifyUnityLoader() throws IOException {
        File loaderFile = new File(saveFolder, UNITY_LOADER);
        URL loaderUrl = new URL(currentUrl + UNITY_LOADER);
        downloadToFile(loaderUrl, loaderFile);

        byte[] encoded = Files.readAllBytes(Paths.get(loaderFile.getAbsolutePath()));
        String contents = new String(encoded, StandardCharsets.UTF_8);

        contents = contents.replace("o.result.responseHeaders[e]==a.getResponseHeader(e)", "false");
        contents = contents.replace("i.responseHeaders[e]=o.getResponseHeader(e)",
                "const genRanHex = size => [...Array(size)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');\n" +
                        "                if (e === \"ETag\") {\n" +
                        "                    i.responseHeaders[e] = \"W/\\\"\" + genRanHex(6) + \"-\" + genRanHex(13) + \"\\\"\"\n" +
                        "                }\n" +
                        "                else {\n" +
                        "                    i.responseHeaders[e] = o.getResponseHeader(e)\n" +
                        "                }");

        BufferedWriter writer = new BufferedWriter(new FileWriter(loaderFile));
        writer.write(contents);
        writer.close();
    }


}
