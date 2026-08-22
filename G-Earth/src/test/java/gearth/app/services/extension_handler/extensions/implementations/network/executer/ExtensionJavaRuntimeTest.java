package gearth.app.services.extension_handler.extensions.implementations.network.executer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionJavaRuntimeTest {

    @Test
    void parsesOracleJava8UpdateVersion() {
        ExtensionJavaRuntime.ParsedVersion version = ExtensionJavaRuntime.parseVersion(
                "java version \"1.8.0_441\"\nJava(TM) SE Runtime Environment"
        );

        assertNotNull(version);
        assertEquals(8, version.major);
        assertEquals(441, version.update);
    }

    @Test
    void parsesOpenJdkJava8UpdateVersion() {
        ExtensionJavaRuntime.ParsedVersion version = ExtensionJavaRuntime.parseVersion(
                "openjdk version \"1.8.0_442\"\nOpenJDK Runtime Environment"
        );

        assertNotNull(version);
        assertEquals(8, version.major);
        assertEquals(442, version.update);
    }

    @Test
    void parsesModernJavaWithoutAcceptingItAsJava8() {
        ExtensionJavaRuntime.ParsedVersion version = ExtensionJavaRuntime.parseVersion(
                "openjdk version \"21.0.8\" 2025-07-15 LTS"
        );

        assertNotNull(version);
        assertEquals(21, version.major);
        assertEquals(0, version.update);
    }

    @Test
    void rejectsUnrecognizedVersionOutput() {
        assertNull(ExtensionJavaRuntime.parseVersion("not a Java runtime"));
    }

    @Test
    void recognizesOnlyUnqualifiedJavaCommands() {
        assertTrue(ExtensionJavaRuntime.isUnqualifiedJavaCommand("java"));
        assertTrue(ExtensionJavaRuntime.isUnqualifiedJavaCommand("java.exe"));
        assertTrue(ExtensionJavaRuntime.isUnqualifiedJavaCommand("javaw"));
        assertTrue(ExtensionJavaRuntime.isUnqualifiedJavaCommand("JAVAW.EXE"));

        assertFalse(ExtensionJavaRuntime.isUnqualifiedJavaCommand("/usr/bin/java"));
        assertFalse(ExtensionJavaRuntime.isUnqualifiedJavaCommand("C:\\Java\\bin\\java.exe"));
        assertFalse(ExtensionJavaRuntime.isUnqualifiedJavaCommand("python"));
        assertFalse(ExtensionJavaRuntime.isUnqualifiedJavaCommand(null));
    }
}
