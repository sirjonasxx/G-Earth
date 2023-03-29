package gearth.ui;

import gearth.GEarth;
import gearth.misc.BindingsUtil;
import gearth.misc.Cacher;
import gearth.misc.HostInfo;
import gearth.protocol.connection.HClient;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

/**
 * Handles G-Earth's observable properties.
 *
 * @author Dorving
 */
public final class GEarthProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(GEarthProperties.class);

    public static final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public static final StringBinding themeTitleBinding;

    public static final ObjectBinding<HostInfo> hostInfoBinding;

    public static final ObjectBinding<Image> logoImageBinding;
    public static final ObjectBinding<Image> logoSmallImageBinding;
    public static final ObservableList<Image> icons = FXCollections.observableArrayList();

    public static final ObjectBinding<String> styleSheetBinding;
    public static final ObservableList<String> styleSheets = FXCollections.observableArrayList();

    public static final ObjectProperty<HClient> clientTypeProperty = new SimpleObjectProperty<>();

    public static final BooleanProperty autoDetectProperty = new SimpleBooleanProperty(false);
    public static final StringProperty hostProperty = new SimpleStringProperty();
    public static final IntegerProperty portProperty = new SimpleIntegerProperty();

    public static final BooleanProperty alwaysOnTopProperty = new SimpleBooleanProperty(false);
    public static final BooleanProperty enableDeveloperModeProperty = new SimpleBooleanProperty(false);
    public static final BooleanProperty enableDebugProperty = new SimpleBooleanProperty(false);
    public static final BooleanProperty disablePacketDecryptionProperty = new SimpleBooleanProperty(false);
    public static final BooleanProperty enableSocksProperty = new SimpleBooleanProperty(false);
    public static final StringProperty socksHostProperty = new SimpleStringProperty();
    public static final IntegerProperty socksPortProperty = new SimpleIntegerProperty();
    public static final BooleanProperty enableGPythonProperty = new SimpleBooleanProperty(false);
    public static final BooleanProperty alwaysAdminProperty = new SimpleBooleanProperty(false);
    public static final StringProperty notesProperty = new SimpleStringProperty();

    static {
        themeProperty.addListener((observable, oldValue, newValue) -> Cacher.put("theme", newValue.title()));
        final Theme value = Cacher.getCacheContents().has("theme") ?
                ThemeFactory.themeForTitle(Cacher.getCacheContents().getString("theme")) :
                ThemeFactory.getDefaultTheme();

        LOGGER.debug("Loading theme {}", value);

        themeProperty.set(Objects.requireNonNull(value, "Unable to load theme"));

        styleSheetBinding = createThemeStylesheetBinding();
        logoSmallImageBinding = createThemeImageBinding("logoSmall.png");
        logoImageBinding = createThemeImageBinding("logo.png");
        hostInfoBinding = createThemeHostInfoBinding();
        themeTitleBinding = createThemeTitleBinding();

        BindingsUtil.addAndBindContent(icons, logoSmallImageBinding);
        BindingsUtil.addAndBindContent(styleSheets, styleSheetBinding);

        Cacher.bindEnum("last_client_mode", HClient.class, clientTypeProperty);
        Cacher.bindBoolean("auto_detect", autoDetectProperty);
        Cacher.bindString("host", hostProperty);
        Cacher.bindNumber("port", portProperty);
        convertOldConnectionSettingsIfPresent();

        Cacher.bindBoolean("always_on_top", alwaysOnTopProperty);
        Cacher.bindBoolean("always_admin", alwaysAdminProperty);
        Cacher.bindBoolean("develop_mode", enableDeveloperModeProperty);
        Cacher.bindBoolean("debug_mode_enabled", enableDebugProperty);
        Cacher.bindBoolean("packet_decryption_disabled", disablePacketDecryptionProperty);
        Cacher.bindBoolean("socks_enabled", enableSocksProperty);
        Cacher.bindString("socks_host", socksHostProperty);
        Cacher.bindNumber("socks_port", socksPortProperty);
        Cacher.bindBoolean("use_gpython", enableGPythonProperty);
        Cacher.bindString("notepad_text", notesProperty);
    }

    public static Theme getTheme() {
        return themeProperty.get();
    }

    public static String getThemeTitle() {
        return themeTitleBinding.get();
    }

    public static HostInfo getHostInfo() {
        return hostInfoBinding.get();
    }

    public static Image getLogoImage() {
        return logoImageBinding.get();
    }

    public static Image getLogoSmallImage() {
        return logoSmallImageBinding.get();
    }

    public static String getStyleSheet() {
        return styleSheetBinding.get();
    }

    public static HClient getClientType() {
        return clientTypeProperty.get();
    }

    public static boolean isPacketDecryptionDisabled() {
        return disablePacketDecryptionProperty.get();
    }

    public static boolean isDebugModeEnabled() {
        return enableDebugProperty.get();
    }

    public static boolean isDeveloperModeEnabled() {
        return enableDeveloperModeProperty.get();
    }

    public static boolean isAlwaysOnTop() {
        return alwaysOnTopProperty.get();
    }

    public static String getSocksHost() {
        return socksHostProperty.get();
    }

    public static int getSocksPort() {
        return socksPortProperty.get();
    }

    private static StringBinding createThemeTitleBinding() {
        return Bindings.createStringBinding(() -> {
            final Theme theme = getTheme();
            return theme.overridesTitle() ? theme.title() : ThemeFactory.getDefaultTheme().title();
        }, themeProperty);
    }

    private static ObjectBinding<HostInfo> createThemeHostInfoBinding() {
        return Bindings.createObjectBinding(() -> new HostInfo(
                "G-Earth",
                GEarth.version,
                new HashMap<>(Collections.singletonMap("theme", getTheme().title()))
        ), themeProperty);
    }

    private static ObjectBinding<String> createThemeStylesheetBinding() {
        return Bindings.createObjectBinding(() -> {
            final String pathToStyleSheet = getThemeRelativePath("styling.css");
            final URL styleSheetInput = GEarth.class.getResource(pathToStyleSheet);
            if (styleSheetInput == null) {
                LOGGER.error("Could not load style sheet `{}`, input-stream is null for {}", "styling.css", pathToStyleSheet);
                return null;
            }
            return styleSheetInput.toExternalForm();
        }, themeProperty);
    }

    private static ObjectBinding<Image> createThemeImageBinding(String imageName) {
        return Bindings.createObjectBinding(() -> {
            final Theme theme = getTheme();
            final String pathToLogo = getThemeRelativePath(theme.overridesLogo()
                    ? theme
                    : ThemeFactory.getDefaultTheme(), imageName);
            try (final InputStream imageInput = GEarth.class.getResourceAsStream(pathToLogo)) {
                if (imageInput == null) {
                    LOGGER.error("Could not load image `{}`, input-stream is null for {}", imageName, pathToLogo);
                    return null;
                }
                return new Image(imageInput);
            } catch (Exception e) {
                LOGGER.error("Failed to load image `{}", imageName, e);
                return null;
            }
        }, themeProperty);
    }

    private static String getThemeRelativePath(String fileName) {
        return getThemeRelativePath(getTheme(), fileName);
    }

    private static String getThemeRelativePath(Theme theme, String fileName) {
        return String.format("/gearth/ui/themes/%s/" + fileName, theme.internalName());
    }

    private static final String KEY_LAST_CONNECTION_SETTINGS = "last_connection_settings";
    private static final String KEY_AUTODETECT = "auto_detect";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";

    private static void convertOldConnectionSettingsIfPresent() {
        /*
        BACKWARDS COMPATABILITY
         */
        if (Cacher.getCacheContents().has(KEY_LAST_CONNECTION_SETTINGS)) {
            final JSONObject jsonObject = Cacher.getCacheContents().getJSONObject(KEY_LAST_CONNECTION_SETTINGS);
            if (jsonObject != null) {
                autoDetectProperty.set(jsonObject.getBoolean(KEY_AUTODETECT));
                hostProperty.set(jsonObject.getString(KEY_HOST));
                portProperty.set(jsonObject.getInt(KEY_PORT));
                Cacher.getCacheContents().remove(KEY_LAST_CONNECTION_SETTINGS);
            }
        }
    }
}
