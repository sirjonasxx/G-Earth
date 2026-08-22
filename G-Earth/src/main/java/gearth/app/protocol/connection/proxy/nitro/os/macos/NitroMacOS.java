package gearth.app.protocol.connection.proxy.nitro.os.macos;

import gearth.app.protocol.connection.proxy.nitro.os.NitroOsFunctions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** macOS integration for certificate trust and the Nitro HTTP proxy. */
public class NitroMacOS implements NitroOsFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(NitroMacOS.class);

    private static final String NETWORK_SETUP = "/usr/sbin/networksetup";
    private static final String SECURITY = "/usr/bin/security";
    private static final String OSASCRIPT = "/usr/bin/osascript";

    private static final List<String> PROXY_BYPASS_DOMAINS = Collections.unmodifiableList(Arrays.asList(
            "localhost",
            "127.0.0.1",
            "::1",
            "discord.com",
            "*.discord.com",
            "discordapp.com",
            "*.discordapp.com",
            "github.com",
            "*.github.com"
    ));

    /*
     * The shutdown hook creates a fresh NitroMacOS instance. Keep the state
     * process-wide so that instance can still restore the user's settings.
     */
    private static final Object PROXY_LOCK = new Object();
    private static final Map<String, ServiceProxyState> ORIGINAL_PROXY_STATES = new LinkedHashMap<>();
    private static final Set<String> CONFIGURED_SERVICES = new LinkedHashSet<>();

    private final CommandRunner commandRunner;

    public NitroMacOS() {
        this(new ProcessCommandRunner());
    }

    NitroMacOS(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    @Override
    public boolean isRootCertificateTrusted(File certificate) {
        if (certificate == null || !certificate.isFile()) {
            return false;
        }

        CommandResult result = execute(Arrays.asList(
                SECURITY,
                "verify-cert",
                "-c",
                certificate.getAbsolutePath()
        ));
        return result != null && result.exitCode == 0;
    }

    @Override
    public boolean installRootCertificate(File certificate) {
        if (certificate == null || !certificate.isFile()) {
            return false;
        }

        // osascript displays the native macOS administrator prompt. A GUI app
        // cannot safely rely on sudo having an attached terminal.
        String script = "function run(argv) {\n"
                + "var app = Application.currentApplication();\n"
                + "app.includeStandardAdditions = true;\n"
                + "var path = String(argv[0]);\n"
                + "var quote = String.fromCharCode(39);\n"
                + "var quotedPath = quote + path.split(quote)"
                + ".join(String.fromCharCode(39, 92, 39, 39)) + quote;\n"
                + "app.doShellScript(\"/usr/bin/security add-trusted-cert -d -r trustRoot "
                + "-k /Library/Keychains/System.keychain \" + quotedPath, "
                + "{administratorPrivileges: true});\n"
                + "}";

        CommandResult result = execute(Arrays.asList(
                OSASCRIPT,
                "-l",
                "JavaScript",
                "-e",
                script,
                certificate.getAbsolutePath()
        ));
        return result != null && result.exitCode == 0;
    }

    @Override
    public boolean registerSystemProxy(String host, int port) {
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535) {
            return false;
        }

        synchronized (PROXY_LOCK) {
            List<String> services = listEnabledNetworkServices();
            if (services.isEmpty()) {
                LOG.error("No enabled macOS network service was found");
                return false;
            }

            List<String> changedServices = new ArrayList<>();
            for (String service : services) {
                ServiceProxyState original = captureProxyState(service);
                if (original == null) {
                    restoreServices(changedServices);
                    return false;
                }

                ORIGINAL_PROXY_STATES.putIfAbsent(service, original);
                if (!configureProxy(service, host, port)) {
                    if (restoreProxyState(service, ORIGINAL_PROXY_STATES.get(service))) {
                        CONFIGURED_SERVICES.remove(service);
                        ORIGINAL_PROXY_STATES.remove(service);
                    } else {
                        // Keep enough state for unregisterSystemProxy or the
                        // shutdown hook to retry after a partial failure.
                        CONFIGURED_SERVICES.add(service);
                    }
                    restoreServices(changedServices);
                    return false;
                }

                CONFIGURED_SERVICES.add(service);
                changedServices.add(service);
            }

            return true;
        }
    }

    @Override
    public boolean unregisterSystemProxy() {
        synchronized (PROXY_LOCK) {
            return restoreServices(new ArrayList<>(CONFIGURED_SERVICES));
        }
    }

    private List<String> listEnabledNetworkServices() {
        CommandResult result = execute(Arrays.asList(NETWORK_SETUP, "-listallnetworkservices"));
        if (result == null || result.exitCode != 0) {
            return Collections.emptyList();
        }
        return parseNetworkServices(result.output);
    }

    static List<String> parseNetworkServices(String output) {
        List<String> services = new ArrayList<>();
        if (output == null) {
            return services;
        }

        for (String rawLine : output.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()
                    || line.startsWith("An asterisk")
                    || line.startsWith("*")) {
                continue;
            }
            services.add(line);
        }
        return services;
    }

    private ServiceProxyState captureProxyState(String service) {
        CommandResult web = execute(Arrays.asList(NETWORK_SETUP, "-getwebproxy", service));
        CommandResult secureWeb = execute(Arrays.asList(NETWORK_SETUP, "-getsecurewebproxy", service));
        CommandResult bypass = execute(Arrays.asList(NETWORK_SETUP, "-getproxybypassdomains", service));

        if (!succeeded(web) || !succeeded(secureWeb) || !succeeded(bypass)) {
            return null;
        }

        return new ServiceProxyState(
                parseProxyEndpoint(web.output),
                parseProxyEndpoint(secureWeb.output),
                parseBypassDomains(bypass.output)
        );
    }

    static ProxyEndpoint parseProxyEndpoint(String output) {
        boolean enabled = false;
        String server = "";
        int port = 0;

        if (output != null) {
            for (String rawLine : output.split("\\R")) {
                int separator = rawLine.indexOf(':');
                if (separator < 0) {
                    continue;
                }

                String key = rawLine.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                String value = rawLine.substring(separator + 1).trim();
                if ("enabled".equals(key)) {
                    enabled = "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
                } else if ("server".equals(key)) {
                    server = value;
                } else if ("port".equals(key)) {
                    try {
                        port = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        port = 0;
                    }
                }
            }
        }

        return new ProxyEndpoint(enabled, server, port);
    }

    static List<String> parseBypassDomains(String output) {
        List<String> domains = new ArrayList<>();
        if (output == null) {
            return domains;
        }

        for (String rawLine : output.split("\\R")) {
            String line = rawLine.trim();
            if (!line.isEmpty() && !line.toLowerCase(Locale.ROOT).startsWith("there aren't any")) {
                domains.add(line);
            }
        }
        return domains;
    }

    private boolean configureProxy(String service, String host, int port) {
        String portString = Integer.toString(port);
        if (!runSuccessfully(NETWORK_SETUP, "-setwebproxy", service, host, portString)
                || !runSuccessfully(NETWORK_SETUP, "-setsecurewebproxy", service, host, portString)) {
            return false;
        }

        List<String> bypassCommand = new ArrayList<>();
        bypassCommand.add(NETWORK_SETUP);
        bypassCommand.add("-setproxybypassdomains");
        bypassCommand.add(service);
        bypassCommand.addAll(PROXY_BYPASS_DOMAINS);

        return succeeded(execute(bypassCommand))
                && runSuccessfully(NETWORK_SETUP, "-setwebproxystate", service, "on")
                && runSuccessfully(NETWORK_SETUP, "-setsecurewebproxystate", service, "on");
    }

    private boolean restoreServices(List<String> services) {
        boolean restored = true;
        for (String service : services) {
            ServiceProxyState original = ORIGINAL_PROXY_STATES.get(service);
            if (original == null || !restoreProxyState(service, original)) {
                restored = false;
                continue;
            }
            CONFIGURED_SERVICES.remove(service);
            ORIGINAL_PROXY_STATES.remove(service);
        }
        return restored;
    }

    private boolean restoreProxyState(String service, ServiceProxyState state) {
        boolean restored = restoreEndpoint(service, "webproxy", state.web)
                & restoreEndpoint(service, "securewebproxy", state.secureWeb);

        List<String> bypassCommand = new ArrayList<>();
        bypassCommand.add(NETWORK_SETUP);
        bypassCommand.add("-setproxybypassdomains");
        bypassCommand.add(service);
        if (state.bypassDomains.isEmpty()) {
            bypassCommand.add("Empty");
        } else {
            bypassCommand.addAll(state.bypassDomains);
        }

        return succeeded(execute(bypassCommand)) & restored;
    }

    private boolean restoreEndpoint(String service, String endpointName, ProxyEndpoint endpoint) {
        boolean restored = true;
        if (!endpoint.server.isEmpty() && endpoint.port > 0) {
            restored = runSuccessfully(
                    NETWORK_SETUP,
                    "-set" + endpointName,
                    service,
                    endpoint.server,
                    Integer.toString(endpoint.port)
            );
        }

        return runSuccessfully(
                NETWORK_SETUP,
                "-set" + endpointName + "state",
                service,
                endpoint.enabled ? "on" : "off"
        ) && restored;
    }

    private boolean runSuccessfully(String... command) {
        return succeeded(execute(Arrays.asList(command)));
    }

    private static boolean succeeded(CommandResult result) {
        return result != null && result.exitCode == 0;
    }

    private CommandResult execute(List<String> command) {
        try {
            CommandResult result = commandRunner.run(command);
            if (result.exitCode != 0) {
                LOG.error(String.join(" ", command) + " failed: " + result.output.trim());
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while running " + command.get(0));
        } catch (IOException e) {
            LOG.error("Unable to run " + command.get(0) + ": " + e.getMessage());
        }
        return null;
    }

    interface CommandRunner {
        CommandResult run(List<String> command) throws IOException, InterruptedException;
    }

    static final class ProcessCommandRunner implements CommandRunner {
        @Override
        public CommandResult run(List<String> command) throws IOException, InterruptedException {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            return new CommandResult(process.waitFor(), output.toString());
        }
    }

    static final class CommandResult {
        final int exitCode;
        final String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }

    static final class ProxyEndpoint {
        final boolean enabled;
        final String server;
        final int port;

        ProxyEndpoint(boolean enabled, String server, int port) {
            this.enabled = enabled;
            this.server = server;
            this.port = port;
        }
    }

    private static final class ServiceProxyState {
        final ProxyEndpoint web;
        final ProxyEndpoint secureWeb;
        final List<String> bypassDomains;

        ServiceProxyState(ProxyEndpoint web, ProxyEndpoint secureWeb, List<String> bypassDomains) {
            this.web = web;
            this.secureWeb = secureWeb;
            this.bypassDomains = new ArrayList<>(bypassDomains);
        }
    }
}
