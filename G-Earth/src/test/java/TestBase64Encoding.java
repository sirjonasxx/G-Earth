import gearth.encoding.Base64Encoding;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBase64Encoding {

    @Test
    public void testBase64Encoding() {
        testDecode(0, "@@");
        testDecode(202, "CJ");
        testDecode(206, "CN");
        testDecode(277, "DU");
        testDecode(1337, "@Ty");
    }

    private void testDecode(int expected, String input) {
        final byte[] header = input.getBytes(StandardCharsets.ISO_8859_1);

        assertEquals(expected, Base64Encoding.decode(header));
    }

}