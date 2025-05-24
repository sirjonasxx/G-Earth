package gearth.app.ui;

import gearth.app.GEarth;
import gearth.services.extension_handler.extensions.GEarthExtension;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.app.services.extension_handler.extensions.implementations.network.NetworkExtensionServer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Adds a {@link TrayIcon} to the {@link SystemTray} for this G-Earth instance.
 *
 * @author Dorving
 */
public final class GEarthTrayIcon {

    private static final String TO_FRONT_LABEL = "To front";
    private static final String TO_BACK_LABEL = "To back";

    private static PopupMenu menu;

    public static void updateOrCreate(Image image) {

        if (!SystemTray.isSupported())
            return;

        final NetworkExtensionServer server = ExtensionProducerFactory.getExtensionServer();
        final BufferedImage awtImage = SwingFXUtils.fromFXImage(image, null);
        final String appTitle = "G-Earth " + GEarth.version + " (" + server.getPort() + ")";

        final Optional<TrayIcon> trayIcon = Stream.of(SystemTray.getSystemTray().getTrayIcons())
                .filter(other -> Objects.equals(other.getToolTip(), appTitle))
                .findFirst();
        if (trayIcon.isPresent()) {
            EventQueue.invokeLater(() -> trayIcon.get().setImage(awtImage));
        } else {
            menu = new PopupMenu();
            menu.add(createToFrontOrBackMenuItem());
            menu.addSeparator();
            menu.addSeparator();
            menu.add(createInstallMenuItem());
            try {
                SystemTray.getSystemTray().add(new TrayIcon(awtImage, appTitle, menu));
            } catch (AWTException e) {
                e.printStackTrace();
                menu = null;
            }
        }
    }

    private static MenuItem createToFrontOrBackMenuItem() {
        final MenuItem showMenuItem = new MenuItem(TO_FRONT_LABEL);
        showMenuItem.addActionListener(e -> {
            if (Objects.equals(showMenuItem.getLabel(), TO_FRONT_LABEL)) {
                showMenuItem.setLabel(TO_BACK_LABEL);
                Platform.runLater(() -> GEarth.main.getController().getStage().toFront());
            } else {
                showMenuItem.setLabel(TO_FRONT_LABEL);
                Platform.runLater(() -> GEarth.main.getController().getStage().toBack());
            }
        });
        return showMenuItem;
    }

    private static MenuItem createInstallMenuItem() {
        final MenuItem showMenuItem = new MenuItem("Install Extension...");
        showMenuItem.addActionListener(e ->
                Optional.ofNullable(GEarth.main.getController())
                        .map(c -> c.extensionsController)
                        .ifPresent(c -> Platform.runLater(() -> {
                            final Stage stage = c.parentController.getStage();
                            final boolean isOnTop = stage.isAlwaysOnTop();
                            stage.setAlwaysOnTop(true); // bit of a hack to force stage to front
                            c.installBtnClicked(null);
                            stage.setAlwaysOnTop(isOnTop);
                        })));
        return showMenuItem;
    }

    /**
     * Adds the argued extension as a menu item to {@link #menu}.
     *
     * @param extension the {@link GEarthExtension} to add to the {@link #menu}.
     */
    public static void addExtension(GEarthExtension extension) {

        if (menu == null)
            return;

        final MenuItem menuItem = new MenuItem("Show "+extension.getTitle());
        EventQueue.invokeLater(() -> menu.insert(menuItem, 2));
        menuItem
                .addActionListener(e -> Platform.runLater(() -> extension.getClickedObservable().fireEvent()));
        extension.getDeletedObservable()
                .addListener(() -> EventQueue.invokeLater(() -> menu.remove(menuItem)));
    }
}
