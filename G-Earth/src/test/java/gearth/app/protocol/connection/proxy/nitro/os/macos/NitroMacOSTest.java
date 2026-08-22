package gearth.app.protocol.connection.proxy.nitro.os.macos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NitroMacOSTest {

    @TempDir
    Path tempDirectory;

    @Test
    void parsesEnabledNetworkServicesAndKeepsSpaces() {
        String output = "An asterisk (*) denotes that a network service is disabled.\n"
                + "Wi-Fi\n"
                + "USB 10/100/1000 LAN\n"
                + "*Thunderbolt Bridge\n";

        assertEquals(
                Arrays.asList("Wi-Fi", "USB 10/100/1000 LAN"),
                NitroMacOS.parseNetworkServices(output)
        );
    }

    @Test
    void parsesProxyEndpoint() {
        NitroMacOS.ProxyEndpoint endpoint = NitroMacOS.parseProxyEndpoint(
                "Enabled: Yes\nServer: proxy.example\nPort: 8080\nAuthenticated Proxy Enabled: 0\n"
        );

        assertTrue(endpoint.enabled);
        assertEquals("proxy.example", endpoint.server);
        assertEquals(8080, endpoint.port);
    }

    @Test
    void toleratesEmptyProxyEndpoint() {
        NitroMacOS.ProxyEndpoint endpoint = NitroMacOS.parseProxyEndpoint(
                "Enabled: No\nServer: \nPort: 0\n"
        );

        assertFalse(endpoint.enabled);
        assertEquals("", endpoint.server);
        assertEquals(0, endpoint.port);
    }

    @Test
    void parsesAndClearsBypassDomains() {
        assertEquals(
                Arrays.asList("localhost", "*.example.com"),
                NitroMacOS.parseBypassDomains("localhost\n*.example.com\n")
        );
        assertEquals(
                Collections.emptyList(),
                NitroMacOS.parseBypassDomains("There aren't any bypass domains set on Wi-Fi.\n")
        );
    }

    @Test
    void installsCertificateThroughJxaWithoutEmbeddingItsPath() throws IOException {
        Path certificate = Files.createFile(tempDirectory.resolve("root cert's.pem"));
        List<String> capturedCommand = new ArrayList<>();
        NitroMacOS nitro = new NitroMacOS(command -> {
            capturedCommand.addAll(command);
            return new NitroMacOS.CommandResult(0, "");
        });

        assertTrue(nitro.installRootCertificate(certificate.toFile()));
        assertEquals("/usr/bin/osascript", capturedCommand.get(0));
        assertEquals(Arrays.asList("-l", "JavaScript", "-e"), capturedCommand.subList(1, 4));
        assertTrue(capturedCommand.get(4).contains("administratorPrivileges: true"));
        assertFalse(capturedCommand.get(4).contains(certificate.toString()));
        assertEquals(certificate.toAbsolutePath().toString(), capturedCommand.get(5));
    }

    @Test
    void configuresAndRestoresTheOriginalProxySettings() {
        List<List<String>> commands = new ArrayList<>();
        NitroMacOS nitro = new NitroMacOS(command -> {
            commands.add(new ArrayList<>(command));
            if (command.contains("-listallnetworkservices")) {
                return new NitroMacOS.CommandResult(0, "Wi-Fi\n");
            }
            if (command.contains("-getwebproxy")) {
                return new NitroMacOS.CommandResult(
                        0,
                        "Enabled: Yes\nServer: old.proxy\nPort: 3128\n"
                );
            }
            if (command.contains("-getsecurewebproxy")) {
                return new NitroMacOS.CommandResult(
                        0,
                        "Enabled: No\nServer: secure.proxy\nPort: 4443\n"
                );
            }
            if (command.contains("-getproxybypassdomains")) {
                return new NitroMacOS.CommandResult(0, "localhost\nintranet.example\n");
            }
            return new NitroMacOS.CommandResult(0, "");
        });

        assertTrue(nitro.registerSystemProxy("127.0.0.1", 9090));
        assertTrue(nitro.unregisterSystemProxy());
        assertTrue(commands.contains(Arrays.asList(
                "/usr/sbin/networksetup", "-setwebproxy", "Wi-Fi", "old.proxy", "3128"
        )));
        assertTrue(commands.contains(Arrays.asList(
                "/usr/sbin/networksetup", "-setwebproxystate", "Wi-Fi", "on"
        )));
        assertTrue(commands.contains(Arrays.asList(
                "/usr/sbin/networksetup", "-setsecurewebproxystate", "Wi-Fi", "off"
        )));
        assertTrue(commands.contains(Arrays.asList(
                "/usr/sbin/networksetup", "-setproxybypassdomains", "Wi-Fi",
                "localhost", "intranet.example"
        )));
    }
}
