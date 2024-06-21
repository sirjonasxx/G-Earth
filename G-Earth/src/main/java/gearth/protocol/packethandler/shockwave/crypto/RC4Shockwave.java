package gearth.protocol.packethandler.shockwave.crypto;

/**
 * Habbo Shockwave RC4 is broken, meaning this is not a standard RC4 implementation.
 * Thanks to <a href="https://github.com/aromaa">Joni</a> and <a href="https://github.com/scottstamp">DarkStar851</a> for discovering this.
 */
public class RC4Shockwave {

    private static final int TABLE_SIZE = 256;

    private final int[] table;

    private int x;
    private int y;

    public RC4Shockwave(int key, byte[] artificialKey) {
        this.table = buildTable(key, artificialKey);
    }

    public byte[] crypt(byte[] data) {
        byte[] result = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            x = (x + 1) % TABLE_SIZE;
            y = (y + table[x] & 0xff) % TABLE_SIZE;

            swap(table, x, y);

            int xorIndex = ((table[x] & 0xff) + (table[y] & 0xff)) % TABLE_SIZE;

            result[i] = (byte) (data[i] ^ table[xorIndex & 0xff]);
        }

        return result;
    }

    private static int[] buildTable(int key, byte[] artificialKey) {
        byte[] modKey = new byte[20];

        for (int i = 0, j = 0; i < modKey.length; i++, j++) {
            if (j >= artificialKey.length) {
                j = 0;
            }

            modKey[i] = (byte) (key & modKey[j]);
        }

        int[] table = new int[TABLE_SIZE];

        for (int i = 0; i < TABLE_SIZE; i++) {
            table[i] = (byte) i;
        }

        for (int q = 0, j = 0; q < TABLE_SIZE; q++) {
            j = (j + (table[q] & 0xff) + modKey[q % modKey.length]) % TABLE_SIZE;

            swap(table, q, j);
        }

        return table;
    }

    private static void swap(int[] table, int i, int j) {
        int temp = table[i];
        table[i] = table[j];
        table[j] = temp;
    }
}
