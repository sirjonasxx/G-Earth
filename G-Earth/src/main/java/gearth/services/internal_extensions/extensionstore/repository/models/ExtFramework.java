package gearth.services.internal_extensions.extensionstore.repository.models;

import java.util.List;

public class ExtFramework {

    private final String name;
    private final List<String> developers;
    private final List<String> languages;
    private final String source;

    private final boolean installationRequired;
    private final String installationInstructions;

    public ExtFramework(String name, List<String> developers, List<String> languages, String source, boolean installationRequired, String installationInstructions) {
        this.name = name;
        this.developers = developers;
        this.languages = languages;
        this.source = source;
        this.installationRequired = installationRequired;
        this.installationInstructions = installationInstructions;
    }

    public String getName() {
        return name;
    }

    public List<String> getDevelopers() {
        return developers;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getSource() {
        return source;
    }

    public boolean isInstallationRequired() {
        return installationRequired;
    }

    public String getInstallationInstructions() {
        return installationInstructions;
    }
}
