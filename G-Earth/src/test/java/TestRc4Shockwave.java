import gearth.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.protocol.crypto.RC4Base64;
import gearth.protocol.crypto.RC4Cipher;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.memory.habboclient.external.MemoryClient;
import gearth.protocol.packethandler.EncryptedPacketHandler;
import gearth.protocol.packethandler.shockwave.ShockwavePacketOutgoingHandler;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class TestRc4Shockwave {

    private final byte[][] encryptedBuffers = new byte[][]{
            Hex.decode("724148504d776b51545841624c6776583352355750713537696931482f39684e2f566466507841756a484f62684e6b4d666c44636e55676d30396c625573304b71754a303966786a5555503342536c32336b63323971557737395651"),
            Hex.decode("2f2b43384577657a5a4d65756b6258347436775061496a6b5539517a324b654a5332"),
            Hex.decode("6e4e4e4c3351666334")
    };

    private final byte[][] potentialTables = new byte[][] {
            Hex.decode("cfa4debb4f28d4279d0f668aabba13810b8f7f4c7917eb1618296937c852d05a5d5f41ffc65c4d63bdacf22ebea27e913f56016fd6dae11ab749e068d59a99656ca8c91bf5779b4b03eda560fdc2742a2421517afa6242470e87f8c45edfbc59e650826e4a4810b9ca7df31e1db16175723354c564a67820e3b4dd5b3dd7b39f73a30d96053c71c795ae5707b611a04ead268eb5220cf9e7b2ee23f6bf9e76a7e5a1e9587c408583af3a8b7be8d13000fc86452f929738350206b0c398fb8dfe09f7c1e41ff18470d86d672d1c8ccc806bd332f025ef90a9ea08d26a89dbb8cbec4615f4e29c5544aa53c0d9883b433493ce0adc12cd2c1914942b36043e3139"),
            Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6236c41f299c8a043960d986e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d9ae5750a0ee31139b2e4646f0f69c7a2ba0cf4dd7b3e57c69ca5dff11cb3da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d546949fd42e382d97f9f3be21ad71b4d1ef2ccf68807f78cc86150092dba7e050a89e904e17a9367e7754278c30b0a6d2f8"),
            Hex.decode("b5c5af8acedbabb84f16481afa427a795967d1654b05c237382609191013f684e995bf8ecd2b4aad9da3a585fd4935ffd3bb249ba4b72572ec1e5b28886014b623d6ddf299787c43960d7b6e4dee80731fa87d520704fef08194635e4caeebcb708b5a47d9d5cf8301570644665f74338691daf5c1ed5840e2fc21de8761c303933d2a76d700f78d3ed0750ae0e3113953e4646f3169c02eaad24e46989aefb36ccc5cb9d4ba6b55e51dcaaca7fbc9f4e7081ca10e3a20188fd80fe81229b197020b2cdfc4f350c6716a82b46d7f6841bcc7f91b32c8bea09fdc3f51e19cf13ca2455de6b22d923b2215622f56eabd899e903417a9367e7754278c30b0a60cf8"),
            Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6236c41f299c8a043960d986e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d9ae5750a0ee31139b2e4646f0f69c7a2ba0cf4dd7b3e57c69ca5dff11cb3da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d546949fd42e382d97f9f3be21ad71b4d1ef2ccf68807f78cc86150092dba7e050a89e904e17a9367e7754278c30b0a6d2f8"),
            Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6239c41f299c8a043960d7b6e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d3ee5750a0ee31139b2e4646f0f69c7a2bad24e46989aefb36ccca1b9d4c6da47d0310b72e995093420b87c5ceafbf1fa1055bdcbeec4c9bc2fc01bca67df1daa447308d66053d5dd949f1c2e382d97f9f3be21ad71b4d1572ccf68807f78a586150092dba7e050a89e90f417a9367e7754278c30b0a60cf8"),
    };

    private final Semaphore waitSemaphore = new Semaphore(0);
    private final AtomicReference<RC4Cipher> cipher = new AtomicReference<>();

    private final MemoryClient mockShockwaveMemoryClient = new MemoryClient(null) {
        @Override
        public List<byte[]> getRC4Tables() {
            return Arrays.asList(potentialTables);
        }
    };

    private final HConnection mockConnection = new HConnection() {
        @Override
        public HClient getClientType() {
            return HClient.SHOCKWAVE;
        }

        @Override
        public void abort() {
            waitSemaphore.release();
        }
    };

    private final EncryptedPacketHandler mockEncryptedPacketHandler = new ShockwavePacketOutgoingHandler(null, null, null) {
        @Override
        public boolean isEncryptedStream() {
            return true;
        }

        @Override
        public boolean sendToStream(byte[] buffer) {
            return false;
        }

        @Override
        protected void writeOut(byte[] buffer) { }

        @Override
        public void setRc4(RC4Cipher rc4) {
            cipher.set(rc4);
            waitSemaphore.release();
        }
    };

    @Test
    public void testMoveUpDown() {
        final RC4Base64 rc = new RC4Base64(potentialTables[0], 0, 0);

        final byte[] tableA = rc.getState().clone();
        final int tableA_X = rc.getQ();
        final int tableA_Y = rc.getJ();

        rc.moveUp();
        rc.moveDown();

        final byte[] tableB = rc.getState().clone();
        final int tableB_X = rc.getQ();
        final int tableB_Y = rc.getJ();

        assertArrayEquals(tableA, tableB);
        assertEquals(tableA_X, tableB_X);
        assertEquals(tableA_Y, tableB_Y);
    }

    @Test
    public void testRc4Base64() {
        final RC4Base64 c = new RC4Base64(
                Hex.decode("D6EAA2D902B1797E759D5F8C26175B93BEC1235764E6F26972A6D85343B259CA715CB9418A19CC984EDB617F3E9E0947EB5A7D46ECAEC26E1C5D62E33D226D39337B0BD4783F49AC6A1FB8AB0A14CD7CC6F3D701895EE4E8D3F9FF8BF628E70058A183BD1B32813B31060F1DDC9B35E58D7740A320EE731584D5B30536A01116DF4854FA37742E2C50B6AF4B9A4D0D4F6BBF9CC3666CCE45D2A525FB4A8E182F3C2776A7F499C438210EA87AB5043463136512958F4CFC68C9B7C7C042BA109786A43A08DA2B1E55B4D0DDE9871A6FCF30F0E185FE5192800CDE29BCADEF2D03C5CBE2E0A9EDD12A5652FDF896F1F79491F5679F24600790BBC84482B070AA88"),
                152,
                211
        );

        final byte[] out = c.decipher(Hex.decode("3270635A4F67"));

        assertEquals("01020304", Hex.toHexString(out));

        final RC4Base64 c2 = new RC4Base64(
                Hex.decode("D6EAA2D902B1797E759D5F8C26175B93BEC1235764E6F26972A6D85343B259CA715CB9418A19CC984EDB617F3E9E0947EB5A7D46ECAEC26E1C5D62E33D226D39337B0BD4783F49AC6A1FB8AB0A14CD7CC6F3D701895EE4E8D3F9FF8BF628E70058A183BD1B32813B31060F1DDC9B35E58D7740A320EE731584D5B30536A01116DF4854FA37742E2C50B6AF4B9A4D0D4F6BBF9CC3666CCE45D2A525FB4A8E182F3C2776A7F499C438210EA87AB5043463136512958F4CFC68C9B7C7C042BA109786A43A08DA2B1E55B4D0DDE9871A6FCF30F0E185FE5192800CDE29BCADEF2D03C5CBE2E0A9EDD12A5652FDF896F1F79491F5679F24600790BBC84482B070AA88"),
                152,
                211
        );

        final byte[] out2 = c2.decipher(Hex.decode("3270635A4F714A4D742F43545551"));

        assertEquals("0102030405060708090a", Hex.toHexString(out2));

        // Test with undo.
        final RC4Base64 c3 = new RC4Base64(
                Hex.decode("F2FD7883352075B654143213705596EBE2D166331F49A8A9B750D7DDE580F77BFC3982AA7D28F5E92E1785005947194136275BE0254F91F8606EC09A05FA5161C87FFF5286CD9BFBC4A15DB06C694EEEB388E399AE72F01C5608ADA44C93373F9D6D34121558BA84C60D7E897A8DF4D8D96A3A8C31A6EA90CF7C4A57B8D6ED792AAF7607DC03733C5F6230CEDF6511F9F11B2C106394FEB2BCDB640CCB2DCADE9E2FCCDA040AE1C240BD2B6838290EE7B1AC81928A2224425CC1B5A3EF71B477AB9F1E9723A0F6443B3D5A4B95C3438F450BD3A57467D2069821BF09C5161AE8F36B9C8BD5A2A701876F5EC7BBE68E48B93E0FE4D4ECC9465302D01D264D18BE"),
                156,
                238
        );

        c3.undoRc4(4);

        final byte[] out3 = c3.decipher(Hex.decode("4A422B2B4441"));

        assertEquals("01020304", Hex.toHexString(out3));
    }

    @Test
    public void testRc4Obtainer() throws Exception {
        final byte[] initialTable = Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6236c41f299c8a043960d986e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d9ae5750a0ee31139b2e4646f0f69c7a2ba0cf4dd7b3e57c69ca5dff11cb3da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d546949fd42e382d97f9f3be21ad71b4d1ef2ccf68807f78cc86150092dba7e050a89e904e17a9367e7754278c30b0a6d2f8");
        final int initialQ = 152;
        final int initialJ = 242;

        // Mock HabboClientFactory to inject our mocked G-MemZ client.
        final MockedStatic<HabboClientFactory> mock = mockStatic(HabboClientFactory.class);

        mock.when(() -> HabboClientFactory.get(mockConnection)).thenReturn(mockShockwaveMemoryClient);

        // Run the RC4 obtainer.
        final Rc4Obtainer obtainer = new Rc4Obtainer(mockConnection);

        obtainer.setFlashPacketHandlers(mockEncryptedPacketHandler);

        for (byte[] buffer : encryptedBuffers) {
            mockEncryptedPacketHandler.act(buffer);
        }

        waitSemaphore.acquire();

        final RC4Cipher c = cipher.get();

        // Validate an exact match.
        assertNotNull(c);
        assertArrayEquals(initialTable, c.getState());
        assertEquals(initialQ, c.getQ());
        assertEquals(initialJ, c.getJ());

        mock.close();
    }

    @Test
    public void testRc4StateMutation() {
        final byte[] startTable = Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6236c41f299c8a043960d986e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d9ae5750a0ee31139b2e4646f0f69c7a2ba0cf4dd7b3e57c69ca5dff11cb3da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d546949fd42e382d97f9f3be21ad71b4d1ef2ccf68807f78cc86150092dba7e050a89e904e17a9367e7754278c30b0a6d2f8");
        final int startQ = 152;
        final int startJ = 242;

        final byte[] h1Table = Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6236c41f299c8a043960d7b6e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d9ae5750a0ee31139b2e4646f0f69c7a2bad24e46983e57c69ca5dff11cb3da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d5dd949fd42e382d97f9f3be21ad71b4d1ef2ccf68807f78cc86150092dba7e050a89e90f417a9367e7754278c30b0a60cf8");
        final int h1Q = 156;
        final int h1J = 74;

        final byte[] h2Table = Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6239c41f299c8a043960d7b6e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d3ee5750a0ee31139b2e4646f0f69c7a2bad24e46989aefb36ca5dff11cc6da47d0310b72e995093420b87c5ceafbb9fa1055bdcbeec4c9bc2fc01bca67a11daa447308d66053d5dd949fd42e382d97f9f3be21ad71b4d1572ccf68807f78cc86150092dba7e050a89e90f417a9367e7754278c30b0a60cf8");
        final int h2Q = 160;
        final int h2J = 65;

        final byte[] h3Table = Hex.decode("b5c5af8ace02ab824f16481a18427a795929e7654b05c2373226b1198f13f684563abf8ecd2b4a519da36285fd4935ffd3bb249ba4b725acec1e5b28886d14b6239c41f299c8a043960d7b6e4d12e66a1f898b520704fef081dc635e4caeebe8707d5ad8d95d4583013c06e1665f74333b916bf5c1ed5840e2fc3fde8761c303933d2a76d722f78d3ee5750a0ee31139b2e4646f0f69c7a2bad24e46989aefb36ccca1b9d4c6da47d0310b72e995093420b87c5ceafbf1fa1055bdcbeec4c9bc2fc01bca67df1daa447308d66053d5dd949f1c2e382d97f9f3be21ad71b4d1572ccf68807f78a586150092dba7e050a89e90f417a9367e7754278c30b0a60cf8");
        final int h3Q = 164;
        final int h3J = 210;

        // Create cipher.
        final RC4Base64 cipher = new RC4Base64(startTable, startQ, startJ);

        // First header.
        cipher.cipher(new byte[4]);

        assertArrayEquals(h1Table, cipher.getState());
        assertEquals(h1Q, cipher.getQ());
        assertEquals(h1J, cipher.getJ());

        // Second header.
        cipher.cipher(new byte[4]);

        assertArrayEquals(h2Table, cipher.getState());
        assertEquals(h2Q, cipher.getQ());
        assertEquals(h2J, cipher.getJ());

        // Third header.
        cipher.cipher(new byte[4]);

        assertArrayEquals(h3Table, cipher.getState());
        assertEquals(h3Q, cipher.getQ());
        assertEquals(h3J, cipher.getJ());
    }

    @Test
    public void testSplitPackets() {
        final RC4Cipher c = new RC4Base64(
                Hex.decode("d102ecab2e8d0a851000a393a483de68f2182f879b884bb6be77595701ffc0900db9f00f415332fd9fe35dcc8ceaa5c480214cc8ee661627e23736795b444a3d5a3f721cfbd4b370d7daaae7f8b1769a78569c3065d6c7607438752c39ced51992634e15a850efcd1a5fd0b782cb6254f4c598f36f485e35f7bf86a9fc03344d8ae9fe07db1355bc7d7f2de8e42052b424e0954f8f71df4612b0c95c7c252aacc394edbb7e45b540f6cadcf18443678ebd513169580e913a3b6b0664290c11fac173a26e1f09ada66199b2b805d8d9d38117331b0447262b1d8b08ddcf7b42e63e97289d23896d1ea70b147af5e19ed2aeebaf6c3cbac2a16a96e522c6f949a0"),
                152,
                79
        );

        final ShockwaveOutBuffer buffer = new ShockwaveOutBuffer();

        buffer.setCipher(c);

        // State.
        int received = 0;

        // Packet 1.
        buffer.push(Hex.decode("4b78316b61774f426838576553476f48355478684946554c7368525749373856326a784b723762"));
        received += buffer.receive().length;

        buffer.push(Hex.decode("7866797279636c313743324a34714a392f5361584e656a6a6b6e53657550476b7638684637302b354d42484d373350434c434a3977"));
        received += buffer.receive().length;

        // Packet 2.
        buffer.push(Hex.decode("3634616845413754796c6371324d346c"));
        received += buffer.receive().length;

        buffer.push(Hex.decode("546f616b62484d55346a5943694c4b386c4a"));
        received += buffer.receive().length;

        // Packet 3.
        buffer.push(Hex.decode("4750"));
        received += buffer.receive().length;

        buffer.push(Hex.decode("41364d67417330"));
        received += buffer.receive().length;

        assertEquals(3, received);
        assertTrue(buffer.isEmpty());
    }
}
