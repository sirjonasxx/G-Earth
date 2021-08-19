package gearth.services.internal_extensions.extensionstore.tools;

public class InstalledExtension {

    private final String name;
    private final String version;

    public InstalledExtension(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
