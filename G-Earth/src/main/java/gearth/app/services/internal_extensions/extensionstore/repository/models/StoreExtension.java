package gearth.app.services.internal_extensions.extensionstore.repository.models;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StoreExtension {

    public StoreExtension(String title, String description, List<Author> authors, String version, List<ExtCategory> categories, String source, String readme, String releases, boolean stable, Framework framework, String language, Commands commands, Compatibility compatibility, LocalDateTime submissionDate, LocalDateTime updateDate, boolean isOutdated, int rating) {
        this.title = title;
        this.description = description;
        this.authors = authors;
        this.version = version;
        this.categories = categories;
        this.source = source;
        this.readme = readme;
        this.releases = releases;
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

    public StoreExtension(JSONObject object, StoreConfig storeConfig) {
        this.title = object.getString("title");
        this.description = object.getString("description");
        this.authors = object.getJSONArray("authors").toList().stream().map(o -> new Author(new JSONObject((Map)o))).collect(Collectors.toList());
        this.version = object.getString("version");
        this.categories = storeConfig.getCategories().stream().filter(c -> object.getJSONArray("categories")
                .toList().stream().anyMatch(j -> j.equals(c.getName()))).collect(Collectors.toList());
        this.source = object.getString("source");
        this.readme = object.has("readme") ? object.getString("readme") : null;
        this.releases = object.has("releases") ? object.getString("releases") : null;
        this.stable = object.getBoolean("stable");
        this.framework = new Framework(object.getJSONObject("framework"), storeConfig);
        this.language = object.getString("language");
        this.commands = new Commands(object.getJSONObject("commands"));
        this.compatibility = new Compatibility(object.getJSONObject("compatibility"));
        this.submissionDate = LocalDateTime.parse(object.getString("submissionDate"), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        this.updateDate = LocalDateTime.parse(object.getString("updateDate"), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        this.isOutdated = object.getBoolean("isOutdated");
        this.rating = object.getInt("rating");
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

        public Author(JSONObject object) {
            this.name = object.getString("name");
            this.discord = object.has("discord") ? object.getString("discord") : null;
            this.hotel = object.has("hotel") ? object.getString("hotel") : null;
            this.username = object.has("username") ? object.getString("username") : null;
            this.extensionsCount = object.getInt("extensionsCount");
            this.reputation = object.getInt("reputation");
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

        public Framework(JSONObject object, StoreConfig storeConfig) {
            this.framework = storeConfig.getFrameworks().stream().filter(e -> e.getName().equals(object.getString("name"))).findFirst().get();
            this.version = object.getString("version");
        }

        public ExtFramework getFramework() {
            return framework;
        }

        public String getVersion() {
            return version;
        }
    }

    public static class Commands {
        private final List<String> defaultCommand;
        private final List<String> linux;
        private final List<String> windows;
        private final List<String> mac;

        public Commands(List<String> defaultCommand, List<String> linux, List<String> windows, List<String> mac) {
            this.defaultCommand = defaultCommand;
            this.linux = linux;
            this.windows = windows;
            this.mac = mac;
        }

        public Commands(JSONObject object) {
            this.defaultCommand = object.getJSONArray("default").toList().stream()
                    .map(s -> (String)s).collect(Collectors.toList());
            this.linux = object.has("linux") ?object.getJSONArray("linux").toList().stream()
                    .map(s -> (String)s).collect(Collectors.toList()) : null;
            this.windows = object.has("windows") ? object.getJSONArray("windows").toList().stream()
                    .map(s -> (String)s).collect(Collectors.toList()) : null;
            this.mac = object.has("mac") ? object.getJSONArray("mac").toList().stream()
                    .map(s -> (String)s).collect(Collectors.toList()) : null;
        }

        public List<String> getDefault() {
            return defaultCommand;
        }

        public List<String> getLinux() {
            return linux;
        }

        public List<String> getWindows() {
            return windows;
        }

        public List<String> getMac() {
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

        public Compatibility(JSONObject object) {
            this.systems = object.getJSONArray("systems").toList().stream().map(s -> (String)s).collect(Collectors.toList());
            this.clients = object.getJSONArray("clients").toList().stream().map(s -> (String)s).collect(Collectors.toList());
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
    private final String releases;

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

    public String getReleases() {
        return releases;
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
