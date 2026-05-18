import gearth.app.protocol.crypto.BobbaChaChaKey;
import gearth.app.protocol.crypto.BobbaCrypto;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TestBobbaShockwave {

    private static final String CLIENT_PRIVATE_KEY = "2850347938414144668820877677992174644524083767401247295221530192865252110248619307161533532210837512002990280808886972288370986778883";
    private static final String SERVER_PUBLIC_KEY = "316632261431661033862824059724497616927577678865196282319412363082835395806757224665726566407041595936678652613134648768216704878918998";

    @Test
    public void testKeyDerivation() {
        final BobbaCrypto crypto = new BobbaCrypto(new BigInteger(CLIENT_PRIVATE_KEY));

        crypto.setServerPublicKey(SERVER_PUBLIC_KEY);

        assertArrayEquals(Hex.decode("72ec2e6c65a610643eb15243b1d14fed7535fc8afcd9c87ecc195cb81d7f088a"), crypto.getC2sData().getKey());
        assertArrayEquals(Hex.decode("6ee202f397feac1bf9082d53"), crypto.getC2sData().getNonce());

        assertArrayEquals(Hex.decode("fb514e77257713e63842aab5bc267fe5aec73eda5a4dacfd69276e5804ddeb67"), crypto.getC2sHeader().getKey());
        assertArrayEquals(Hex.decode("cd97401adda136f7accebad1"), crypto.getC2sHeader().getNonce());

        assertArrayEquals(Hex.decode("da93622389db7b12fb367c82edce334df91648a173a1b2f87e4a00a86d01ab64"), crypto.getS2cData().getKey());
        assertArrayEquals(Hex.decode("655a67e3b7242f498f0c9304"), crypto.getS2cData().getNonce());

        assertArrayEquals(Hex.decode("9b65fcf109892a183accb81fa54e9349150e229a241ce8fcbb2a3c5d7441f03a"), crypto.getS2cHeader().getKey());
        assertArrayEquals(Hex.decode("42f755239d18c4d9032ad684"), crypto.getS2cHeader().getNonce());
    }

    @Test
    public void testNonce() {
        final BobbaChaChaKey key = new BobbaChaChaKey(
                Hex.decode("f43ae54f3f645723946df60b270a7bb393396fea11328924ffa1c790be2290a4"),
                Hex.decode("8feafcdeda1a1904acd4155e")
        );

        assertArrayEquals(Hex.decode("8feafcdeda1a1904acd4155e"), key.getNextNonce());
        assertArrayEquals(Hex.decode("8feafcdedb1a1904acd4155e"), key.getNextNonce());
        assertArrayEquals(Hex.decode("8feafcdedc1a1904acd4155e"), key.getNextNonce());
        assertArrayEquals(Hex.decode("8feafcdedd1a1904acd4155e"), key.getNextNonce());
    }

    @Test
    public void testNonceWrapAround() {
        final BobbaChaChaKey key = new BobbaChaChaKey(
                Hex.decode("1b848a223d407b38c4e8ce307dc5cde45e7a8a4abb5f4b63cacc3ca93e53fe15"),
                Hex.decode("f2199ffbfe8c1a6383c8cfdb")
        );

        assertArrayEquals(Hex.decode("f2199ffbfe8c1a6383c8cfdb"), key.getNextNonce());
        assertArrayEquals(Hex.decode("f2199ffbff8c1a6383c8cfdb"), key.getNextNonce());
        assertArrayEquals(Hex.decode("f2199ffb008d1a6383c8cfdb"), key.getNextNonce());
        assertArrayEquals(Hex.decode("f2199ffb018d1a6383c8cfdb"), key.getNextNonce());
    }
}
