package gearth.services.nitro;

import gearth.misc.Cacher;
import gearth.misc.ConfirmationDialog;
import gearth.ui.titlebar.TitleBarAlert;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

public class RemoteNitroHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteNitroHelper.class);
    private static final String PERMISSION_KEY = "nitrohelper_%s";
    private static final String URL_HABBOCITY = "https://nitro.sulek.dev/habbocity";

    private final String hotel;
    private final String permissionKey;

    public RemoteNitroHelper(final String hotel) {
        this.hotel = hotel;
        this.permissionKey = String.format(PERMISSION_KEY, hotel);
    }

    public boolean hasPermission() {
        return Cacher.get(permissionKey) != null;
    }

    public void askPermission() {
        final Semaphore waitForDialog = new Semaphore(0);

        Platform.runLater(() -> {
            try {
                final Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.WARNING, null,
                        LanguageBundle.get("alert.nitrohelper.title"), null,
                        "", null,
                        ButtonType.YES, ButtonType.NO
                );

                alert.getDialogPane().setContent(new Label(String.format(LanguageBundle.get("alert.nitrohelper.content"), this.hotel).replaceAll("\\\\n", System.lineSeparator())));

                LOG.debug("Showing nitro permission dialog");

                final boolean permissionResult = TitleBarAlert
                        .create(alert)
                        .showAlertAndWait()
                        .filter(t -> t == ButtonType.YES)
                        .isPresent();

                LOG.debug("Permission result: {}", permissionResult);

                Cacher.put(this.permissionKey, true);
            } catch (Exception e) {
                LOG.error("Failed to show nitro helper permission dialog", e);
            } finally {
                waitForDialog.release();
            }
        });

        // Wait for dialog choice.
        try {
            waitForDialog.acquire();
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for user input", e);
        }
    }

    public HabboCityResponse fetchHabboCity(final byte[] wasmFile) {
        // Ensure we are only submitting a wasm file.
        if (wasmFile[0] != 0x00 || wasmFile[1] != 0x61 || wasmFile[2] != 0x73 || wasmFile[3] != 0x6D) {
            LOG.error("Invalid file specified, not a wasm file");
            return null;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpPost request = new HttpPost(URL_HABBOCITY);

            request.setEntity(MultipartEntityBuilder
                    .create()
                    .addBinaryBody("wasm", wasmFile, ContentType.APPLICATION_OCTET_STREAM, "data.wasm")
                    .build());

            return client.execute(request, res -> {
                if (res.getCode() == 200) {
                    final String resBody = EntityUtils.toString(res.getEntity());
                    return HabboCityResponse.fromJson(new JSONObject(resBody));
                }

                LOG.error("Failed to fetch from {}, status code: {}", URL_HABBOCITY, res.getCode());
                return null;
            });
        } catch (Exception e) {
            LOG.error("Failed to fetch from {}", URL_HABBOCITY, e);
        }

        return null;
    }

    public static class HabboCityResponse {

        private final String status;
        private final Result result;

        public HabboCityResponse(String status, Result result) {
            this.status = status;
            this.result = result;
        }

        public static HabboCityResponse fromJson(JSONObject jsonObject) {
            return new HabboCityResponse(jsonObject.getString("status"), Result.fromJson(jsonObject.getJSONObject("result")));
        }

        public String getStatus() {
            return status;
        }

        public Result getResult() {
            return result;
        }

        public static class Result {

            private final String aesKey;
            private final String aesIv;
            private final String[] saltKey;
            private final String[] saltIv;

            public Result(String aesKey, String aesIv, String[] saltKey, String[] saltIv) {
                this.aesKey = aesKey;
                this.aesIv = aesIv;
                this.saltKey = saltKey;
                this.saltIv = saltIv;
            }

            public String getAesKey() {
                return aesKey;
            }

            public String getAesIv() {
                return aesIv;
            }

            public String[] getSaltKey() {
                return saltKey;
            }

            public String[] getSaltIv() {
                return saltIv;
            }

            public static Result fromJson(JSONObject jsonObject) {
                return new Result(
                        jsonObject.getString("aes_key"),
                        jsonObject.getString("aes_iv"),
                        jsonObject.getJSONArray("salt_key").toList().toArray(new String[0]),
                        jsonObject.getJSONArray("salt_iv").toList().toArray(new String[0]));
            }
        }
    }

}
