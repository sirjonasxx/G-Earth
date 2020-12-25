package gearth.protocol.packethandler;

public class ByteArrayUtils {

    public static byte[] combineByteArrays(byte[] arr1, byte[] arr2)	{
        byte[] combined = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1,0,combined,0         ,arr1.length);
        System.arraycopy(arr2,0,combined,arr1.length,arr2.length);
        return combined;
    }

}
