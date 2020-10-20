package gearth.services.gpython;

import gearth.Main;
import gearth.ui.extra.ExtraController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GPythonShell {

    private final String extensionName;
    private final int port;
    private final String cookie;

    public GPythonShell(String extensionName, int port, String cookie) {
        this.extensionName = extensionName;
        this.port = port;
        this.cookie = cookie;
    }

    public void launch(OnQtConsoleLaunch onLaunch) {
        new Thread(() -> {
            try {
                // launch the jupyter console
                ProcessBuilder gConsoleBuilder = new ProcessBuilder("python", "-m", "jupyter", "console", "--simple-prompt");
                Process gConsole = gConsoleBuilder.start();

                InputStreamReader in = new InputStreamReader(gConsole.getInputStream());
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(gConsole.getOutputStream()));

                readUntilExpectInput(in);

                // obtain jupyter kernel name
                List<String> sysargs = enterCommandAndAwait(out, in, "import sys; sys.argv");
                String kernelName = extractKernelName(sysargs);

                InputStream initScriptResource = getClass().getResourceAsStream("init_script.py");
                List<String> initScript = new BufferedReader(new InputStreamReader(initScriptResource,
                                    StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

                for (String line : initScript) {
                    line = line.replace("$G_PYTHON_SHELL_TITLE$", extensionName)
                            .replace("$G_EARTH_PORT$", "" + port)
                            .replace("$COOKIE$", cookie);
                    enterCommandAndAwait(out, in, line);
                }

                ProcessBuilder qtConsoleBuilder = new ProcessBuilder("python", "-m", "jupyter", "qtconsole",
                        "--ConsoleWidget.include_other_output", "True",
                        "--ConsoleWidget.gui_completion", "'droplist'",
//                        "--ConsoleWidget.other_output_prefix", "'[G-Earth]'",
//                        "--JupyterWidget.in_prompt", "'>>>: '",
//                        "--JupyterWidget.out_prompt", "''",
                        "--KernelManager.autorestart", "False",
                        "--JupyterConsoleApp.confirm_exit", "False",
                        "--JupyterConsoleApp.existing", kernelName);
                Process qtConsole = qtConsoleBuilder.start();

                new Thread(() -> {
                    try {
                        qtConsole.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        enterCommandAndAwait(out, in, "ext.stop()");
                        enterCommand(out, "exit()");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                onLaunch.launched(false);
            }
            catch (Exception e) {
                e.printStackTrace();
                showError();
                onLaunch.launched(true);
            }
        }).start();
    }

    private List<String> readUntilExpectInput(InputStreamReader in) throws IOException {
        List<String> readings = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        int c;
        while ((c = in.read()) != -1) {
            char character = (char)c;
            if (character == '\n') {
                String reading = builder.toString();
                if (reading.endsWith("\r")) {
                    reading = reading.substring(0, reading.length() - 1);
                }
                if (reading.matches("Out\\[[0-9+]]: .*")) {
                    reading = reading.replaceAll("^Out\\[[0-9+]]: ", "");
                    if (reading.equals("")) {
                        builder = new StringBuilder();
                        continue;
                    }
                }

                readings.add(reading);
                builder = new StringBuilder();
            }
            else {
                builder.append(character);

                if (builder.toString().matches("In \\[[0-9]+]: ") && !in.ready()) {
                    return readings;
                }
            }
        }
        return null;
    }
    private void enterCommand(BufferedWriter out, String command) throws IOException {
        out.write(command);
        out.newLine();
        out.flush();
    }
    private List<String> enterCommandAndAwait(BufferedWriter out, InputStreamReader in, String command) throws IOException {
        enterCommand(out, command);
        return readUntilExpectInput(in);
    }

    private String extractKernelName(List<String> inputArgs) {
        // null if not found

        String joined = String.join("", inputArgs);
        String paramsFull = joined.replaceAll("^[^\\[]*\\[", "").replaceAll("][^]]*$", "");
        List<String> params = new ArrayList<>();

        boolean backslashed = false;
        int beginParameterIndex = -1;
        for (int i = 0; i < paramsFull.length(); i++) {
            char c = paramsFull.charAt(i);
            if (c == '\'' && !backslashed) {
                if (beginParameterIndex == -1) {
                    beginParameterIndex = i + 1;
                }
                else {
                    params.add(paramsFull.substring(beginParameterIndex, i));
                    beginParameterIndex = -1;
                }
            }

            backslashed = c == '\\' && !backslashed;
        }

        for (int i = 0; i < params.size() - 1; i++) {
            if (params.get(i).equals("-f")) {
                return "'" + params.get(i+1) + "'";
            }
        }
        return null;
    }

    private void showError() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "G-Python error", ButtonType.OK);
            alert.setTitle("G-Python error");

            FlowPane fp = new FlowPane();
            Label lbl = new Label("Something went wrong launching the G-Python shell," +
                    System.lineSeparator() + "are you sure you followed the installation guide correctly?" +
                    System.lineSeparator() + System.lineSeparator() + "More information here:");
            Hyperlink link = new Hyperlink(ExtraController.INFO_URL_GPYTHON);
            fp.getChildren().addAll(lbl, link);
            link.setOnAction(event -> {
                Main.main.getHostServices().showDocument(link.getText());
                event.consume();
            });

            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.getDialogPane().setContent(fp);
            alert.show();
        });
    }

    public String getExtensionName() {
        return extensionName;
    }

    public static void main(String[] args) {
        GPythonShell shell = new GPythonShell("test", 9092, "cookie");
        shell.launch((b) -> {
            System.out.println("launched");
        });
    }

}
