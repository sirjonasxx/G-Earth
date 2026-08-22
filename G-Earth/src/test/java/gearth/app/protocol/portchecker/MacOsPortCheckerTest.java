package gearth.app.protocol.portchecker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MacOsPortCheckerTest {

    @Test
    void rejectsInvalidPorts() {
        MacOsPortChecker checker = new MacOsPortChecker();

        assertThrows(IllegalArgumentException.class, () -> checker.getProcessUsingPort(0));
        assertThrows(IllegalArgumentException.class, () -> checker.getProcessUsingPort(65536));
    }
}
