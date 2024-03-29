package gearth.services.unity_tools;

import gearth.GEarth;
import gearth.misc.Cacher;
import gearth.ui.themes.ThemeFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.Random;

public class GUnityFileServer extends HttpServlet
{

    public final static int FILESERVER_PORT = 9089;
    private final static UnityWebModifyer modifyer = new UnityWebModifyer();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try {
            response.addHeader("Access-Control-Allow-Origin", "*");
            String path = request.getPathInfo();

            if (path.equals("/ping")) {
                response.setStatus(200);
                return;
            }

            String url = request.getParameter("blabla");
            if (url == null || url.isEmpty()) {
                response.setStatus(404);
                return;
            }

            response.addHeader("ETag", createETag());

            String revision = url.split("/")[4];

            if (path.equals("/data")) getData(revision, response);
            else if (path.equals("/wasm")) getWasmCode(revision, response);
            else if (path.equals("/framework")) getWasmFramework(revision, response);
            else if (path.equals("/loader")) getLoader(revision, response);
            else if (path.equals("/version")) getVersion(revision, response, url);
            else if (path.equals("/logo")) getLogo(response);
            else {
                response.setStatus(404);
            }

            response.setStatus(200);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }

    }

    private static String getRandomHexString(int numchars){
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, numchars);
    }

    private String createETag() {
        return "W/\"" + getRandomHexString(6) + "-" + getRandomHexString(13) + "\"";
    }

    private String getDir(String revision) {
        return Cacher.getCacheDir() + File.separator + "UNITY-" + revision + File.separator;
    }


    private void fileResponse(String file, HttpServletResponse response, String contentType, boolean gzip) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        InputStream in = new FileInputStream(file);
        if (contentType != null) {
            response.setContentType(contentType);
        }

        if (gzip) {
            response.setHeader("Content-Encoding", "gzip");
        }

        byte[] bytes = new byte[4096];
        int bytesRead;

        while ((bytesRead = in.read(bytes)) != -1) {
            out.write(bytes, 0, bytesRead);
        }

        in.close();
        out.close();
    }


    private void getData(String revision, HttpServletResponse response) throws IOException {
        modifyer.modifyAllFiles(revision, getDir(revision));

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_DATA, response, null, true);
    }

    private void getWasmCode(String revision, HttpServletResponse response) throws IOException {
        modifyer.modifyAllFiles(revision, getDir(revision));

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_CODE, response, "application/wasm", true);
    }

    private void getWasmFramework(String revision, HttpServletResponse response) throws IOException {
        modifyer.modifyAllFiles(revision, getDir(revision));

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_FRAMEWORK, response, "text/javascript", true);
    }

    private void getLoader(String revision, HttpServletResponse response) throws IOException {
        modifyer.modifyAllFiles(revision, getDir(revision));

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_LOADER, response, "text/javascript", false);
    }

    private void getVersion(String revision, HttpServletResponse response, String url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

        String version = in.readLine();
        String realVersion = version.split(" ")[0];

        response.getOutputStream().print(realVersion + " - G-Earth by sirjonasxx");
        response.getOutputStream().close();
    }

    private void getLogo(HttpServletResponse response) throws IOException {
        OutputStream out = response.getOutputStream();
        InputStream in = GEarth.class.getResourceAsStream(String.format("/gearth/ui/themes/%s/logo.png", ThemeFactory.getDefaultTheme().internalName()));

        byte[] bytes = new byte[4096];
        int bytesRead;

        while ((bytesRead = in.read(bytes)) != -1) {
            out.write(bytes, 0, bytesRead);
        }

        in.close();
        out.close();
    }

}

