package gearth.protocol.crypto;

public interface RC4Cipher {

    byte[] cipher(byte[] data);

    byte[] cipher(byte[] data, int offset, int length);

    byte[] decipher(byte[] data);

    byte[] decipher(byte[] data, int offset, int length);

    int[] getState();

    int getQ();

    int getJ();

    RC4Cipher deepCopy();

}
