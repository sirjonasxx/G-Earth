package gearth.app.services.nitro;

public class NitroAsset {

    private final String host;
    private final String uri;

    public NitroAsset(String host, String uri) {
        this.host = host;
        this.uri = uri;
    }

    public String getHost() {
        return host;
    }

    public String getUri() {
        return uri;
    }

    public boolean matches(final String host, final String uri) {
        return this.host.equals(host) && this.uri.equals(uri);
    }
}
