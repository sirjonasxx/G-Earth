package gearth.app.protocol.crypto;

public interface HeaderCipher {

    byte[] decipher(byte[] data, int offset, int length);

}
