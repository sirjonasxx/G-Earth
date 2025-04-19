package gearth.misc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.HashMap;

public class Logging {

    public static void bridgeJavaLoggingToSlf4j() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void setLogLevel(boolean debug) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        logger.setLevel(debug ? Level.DEBUG : Level.INFO);
    }

    public static class RollOncePerSessionTriggeringPolicy<E> extends TriggeringPolicyBase<E> {
        private static final HashMap<String, Boolean> doRolling = new HashMap<>();

        @Override
        public boolean isTriggeringEvent(File activeFile, E event) {
            return doRolling.putIfAbsent(activeFile.getAbsolutePath(), true) == null;
        }
    }
}
