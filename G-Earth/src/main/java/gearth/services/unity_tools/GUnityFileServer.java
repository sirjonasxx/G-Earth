package gearth.services.unity_tools;

import gearth.Main;
import gearth.misc.Cacher;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class GUnityFileServer extends HttpServlet
{

    public final static int FILESERVER_PORT = 9089;

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

            if (path.equals("/prod")) getProd(revision, response);
            else if (path.equals("/data")) getData(revision, response);
            else if (path.equals("/wasm/code")) getWasmCode(revision, response);
            else if (path.equals("/wasm/framework")) getWasmFramework(revision, response);
            else if (path.equals("/unityloader")) getUnityLoader(revision, response);
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


    private void fileResponse(String file, HttpServletResponse response, String contentType) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        InputStream in = new FileInputStream(file);
//        response.setContentType(contentType);

        byte[] bytes = new byte[4096];
        int bytesRead;

        while ((bytesRead = in.read(bytes)) != -1) {
            out.write(bytes, 0, bytesRead);
        }

        in.close();
        out.close();
    }


    private void getProd(String revision, HttpServletResponse response) throws IOException {
        UnityWebModifyer unitywebModifyer = new UnityWebModifyer(revision, getDir(revision));
        unitywebModifyer.modifyAllFiles();

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_PROD, response, "application/json");
    }

    private void getData(String revision, HttpServletResponse response) throws IOException {
        // application/vnd.unity
        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_DATA, response, "application/vnd.unity");
    }

    private void getWasmCode(String revision, HttpServletResponse response) throws IOException {
        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_CODE, response, "application/vnd.unity");
    }

    private void getWasmFramework(String revision, HttpServletResponse response) throws IOException {
        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_FRAMEWORK, response, "application/vnd.unity");
    }

    private void getUnityLoader(String revision, HttpServletResponse response) throws IOException {
        UnityWebModifyer unitywebModifyer = new UnityWebModifyer(revision, getDir(revision));
        unitywebModifyer.modifyAllFiles();

        fileResponse(getDir(revision) + UnityWebModifyer.UNITY_LOADER, response, "text/javascript");
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
        InputStream in = Main.class.getResourceAsStream("G-EarthLogo.png");

        byte[] bytes = new byte[4096];
        int bytesRead;

        while ((bytesRead = in.read(bytes)) != -1) {
            out.write(bytes, 0, bytesRead);
        }

        in.close();
        out.close();
    }

}

