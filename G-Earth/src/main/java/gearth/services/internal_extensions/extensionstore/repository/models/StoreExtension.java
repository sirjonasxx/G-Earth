package gearth.services.internal_extensions.extensionstore.repository.models;

import java.time.LocalDateTime;
import java.util.List;

public class StoreExtension {

    public StoreExtension(String title, String description, List<Author> authors, String version, List<ExtCategory> categories, String source, String readme, boolean stable, Framework framework, String language, Commands commands, Compatibility compatibility, LocalDateTime submissionDate, LocalDateTime updateDate, boolean isOutdated, int rating) {
        this.title = title;
        this.description = description;
        this.authors = authors;
        this.version = version;
        this.categories = categories;
        this.source = source;
        this.readme = readme;
        this.stable = stable;
        this.framework = framework;
        this.language = language;
        this.commands = commands;
        this.compatibility = compatibility;
        this.submissionDate = submissionDate;
        this.updateDate = updateDate;
        this.isOutdated = isOutdated;
        this.rating = rating;
    }

    public static class Author {

        private final String name;
        private final String discord;
        private final String hotel;
        private final String username;

        private final int extensionsCount;
        private final int reputation;

        public Author(String name, String discord, String hotel, String username, int extensionsCount, int reputation) {
            this.name = name;
            this.discord = discord;
            this.hotel = hotel;
            this.username = username;
            this.extensionsCount = extensionsCount;
            this.reputation = reputation;
        }

        public String getName() {
            return name;
        }

        public String getDiscord() {
            return discord;
        }

        public String getHotel() {
            return hotel;
        }

        public String getUsername() {
            return username;
        }

        public int getExtensionsCount() {
            return extensionsCount;
        }

        public int getReputation() {
            return reputation;
        }
    }

    public static class Framework {

        private final ExtFramework framework;
        private final String version;

        public Framework(ExtFramework framework, String version) {
            this.framework = framework;
            this.version = version;
        }

        public ExtFramework getFramework() {
            return framework;
        }

        public String getVersion() {
            return version;
        }
    }

    public static class Commands {
        private final String defaultCommand;
        private final String linux;
        private final String windows;
        private final String mac;

        public Commands(String defaultCommand, String linux, String windows, String mac) {
            this.defaultCommand = defaultCommand;
            this.linux = linux;
            this.windows = windows;
            this.mac = mac;
        }

        public String getDefault() {
            return defaultCommand;
        }

        public String getLinux() {
            return linux;
        }

        public String getWindows() {
            return windows;
        }

        public String getMac() {
            return mac;
        }
    }

    public static class Compatibility {
        private final List<String> systems;
        private final List<String> clients;

        public Compatibility(List<String> systems, List<String> clients) {
            this.systems = systems;
            this.clients = clients;
        }

        public List<String> getSystems() {
            return systems;
        }

        public List<String> getClients() {
            return clients;
        }
    }



    private final String title;
    private final String description;

    private final List<Author> authors;
    private final String version;
    private final List<ExtCategory> categories;

    private final String source;
    private final String readme;

    private final boolean stable;

    private final Framework framework;
    private final String language;
    private final Commands commands;

    private final Compatibility compatibility;

    private final LocalDateTime submissionDate;
    private final LocalDateTime updateDate;

    private final boolean isOutdated;

    private final int rating;


    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public String getVersion() {
        return version;
    }

    public List<ExtCategory> getCategories() {
        return categories;
    }

    public String getSource() {
        return source;
    }

    public String getReadme() {
        return readme;
    }

    public boolean isStable() {
        return stable;
    }

    public Framework getFramework() {
        return framework;
    }

    public String getLanguage() {
        return language;
    }

    public Commands getCommands() {
        return commands;
    }

    public Compatibility getCompatibility() {
        return compatibility;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public boolean isOutdated() {
        return isOutdated;
    }

    public int getRating() {
        return rating;
    }
}
