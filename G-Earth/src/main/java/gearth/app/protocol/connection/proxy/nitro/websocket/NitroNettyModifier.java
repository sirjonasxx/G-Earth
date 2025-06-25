package gearth.app.protocol.connection.proxy.nitro.websocket;

public interface NitroNettyModifier {

    byte[] modify(byte[] data) throws Exception;

}
