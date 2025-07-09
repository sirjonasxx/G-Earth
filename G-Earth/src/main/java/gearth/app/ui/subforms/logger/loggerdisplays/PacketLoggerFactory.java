package gearth.app.ui.subforms.logger.loggerdisplays;

import gearth.app.GEarth;
import gearth.app.extensions.InternalExtensionFormLauncher;
import gearth.app.misc.OSValidator;
import gearth.app.services.extension_handler.ExtensionHandler;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.app.services.internal_extensions.uilogger.UiLogger;
import gearth.app.services.internal_extensions.uilogger.UiLoggerCreator;

/**
 * Created by Jonas on 04/04/18.
 */
public class PacketLoggerFactory implements ExtensionProducer {

    private UiLogger uiLogger;

    public static boolean usesUIlogger() {
        return (!GEarth.hasFlag("-t"));
    }

    public PacketLoggerFactory(ExtensionHandler handler) {
        handler.addExtensionProducer(this);
    }


    public PacketLogger get() {
        if (usesUIlogger()) {
//            return new UiLogger(); //now an extension
            return uiLogger;
        }

        if (OSValidator.isUnix()) {
            return new LinuxTerminalLogger();
        }
        return new SimpleTerminalLogger();
    }

    @Override
    public void startProducing(ExtensionProducerObserver observer) {
        if (usesUIlogger()) {
            uiLogger = new InternalExtensionFormLauncher<UiLoggerCreator, UiLogger>()
                    .launch(new UiLoggerCreator(), observer);
        }
    }
}
