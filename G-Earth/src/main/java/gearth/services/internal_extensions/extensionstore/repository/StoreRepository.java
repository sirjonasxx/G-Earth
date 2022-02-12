package gearth.services.internal_extensions.extensionstore.repository;

import gearth.services.internal_extensions.extensionstore.repository.models.*;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;
import gearth.services.internal_extensions.extensionstore.tools.EncodingUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StoreRepository {

    public static StoreRepository EMPTY = new StoreRepository(new StoreData(new StoreConfig(new ArrayList<>(), new ArrayList<>()), new ArrayList<>()), "0.0", "sirjonasxx/G-ExtensionStore");

    private final String repoVersion;
    private final StoreData storeData;
    private final String source;

    public StoreRepository(StoreData storeData, String repoVersion, String source) {
        this.repoVersion = repoVersion;
        this.storeData = storeData;
        this.source = source;
    }

    public List<ExtCategory> getCategories() {
        return storeData.getConfig().getCategories();
    }

    public List<ExtFramework> getFrameworks() {
        return storeData.getConfig().getFrameworks();
    }

    public List<StoreExtension> getExtensions() {
        return storeData.getExtensions();
    }

    public List<StoreExtension> getExtensions(int offset, int length, String queryString,
                                              ExtensionOrdering ordering, List<String> filterOSes,
                                              List<String> filterClients, List<String> filterFrameworks,
                                              List<String> filterCategories, boolean includeOutdated, boolean inverse) {

        String queryLower = queryString.toLowerCase();

        Stream<StoreExtension> stream = getExtensions().stream()
                .filter(ext -> ext.getTitle().toLowerCase().contains(queryLower) || ext.getDescription().toLowerCase().contains(queryLower)
                        || ext.getAuthors().stream().anyMatch(author -> author.getName().toLowerCase().contains(queryLower)
                        || author.getUsername() != null && author.getUsername().toLowerCase().contains(queryLower))
                        || ext.getCategories().stream().anyMatch(extCategory -> extCategory.getName().toLowerCase().contains(queryLower))
                        || ext.getFramework().getFramework().getName().toLowerCase().contains(queryLower)
                        || ext.getLanguage().toLowerCase().contains(queryLower)
                        || ext.getCompatibility().getSystems().stream().anyMatch(s -> s.toLowerCase().contains(queryLower))
                        || ext.getCompatibility().getClients().stream().anyMatch(s -> s.toLowerCase().contains(queryLower)))
                .filter(ext -> filterOSes == null || ext.getCompatibility().getSystems().stream().anyMatch(filterOSes::contains))
                .filter(ext -> filterClients == null || ext.getCompatibility().getClients().stream().anyMatch(filterClients::contains))
                .filter(ext -> filterFrameworks == null || filterFrameworks.contains(ext.getFramework().getFramework().getName()))
                .filter(ext -> filterCategories == null || ext.getCategories().stream().anyMatch(c -> filterCategories.contains(c.getName())))
                .filter(ext -> includeOutdated || !ext.isOutdated())
                .sorted((o1, o2) -> {
                    int result = 0;
                    if (ordering == ExtensionOrdering.RATING) result = -Integer.compare(o1.getRating(), o2.getRating());
                    else if (ordering == ExtensionOrdering.LAST_UPDATED) result = -o1.getUpdateDate().compareTo(o2.getUpdateDate());
                    else if (ordering == ExtensionOrdering.NEW_RELEASES) result = -o1.getSubmissionDate().compareTo(o2.getSubmissionDate());
                    else if (ordering == ExtensionOrdering.ALPHABETICAL) result = o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
                    return inverse ? -result : result;
                })
                .skip(offset);

        if (length != -1) {
            stream = stream.limit(length);
        }
        return stream.collect(Collectors.toList());
    }

    public List<String> getOperatingSystems() {
        return Arrays.asList("Linux", "Windows", "Mac");
    }

    public List<String> getClients() {
        return Arrays.asList("Unity", "Flash", "Nitro");
    }

    public List<String> getLanguages() {
        Set<String> languages = new HashSet<>();
        getFrameworks().forEach(extFramework -> languages.addAll(extFramework.getLanguages()));
        return new ArrayList<>(languages);
    }

    public List<StoreExtension.Author> getAuthors() {
        Map<String, StoreExtension.Author> allAuthors = new HashMap<>();
        storeData.getExtensions().forEach((extension) -> {
            extension.getAuthors().forEach(author -> {
                if (!allAuthors.containsKey(author.getName())) {
                    allAuthors.put(author.getName(), author);
                }
                else {
                    StoreExtension.Author old = allAuthors.get(author.getName());
                    if ((old.getHotel() == null || old.getUsername() == null) &&
                        author.getHotel() != null && author.getUsername() != null) {
                        allAuthors.put(author.getName(), author);
                    }
                }
            });
        });

        return new ArrayList<>(allAuthors.values());
    }

    public List<ExtensionOrdering> getOrderings() {
        return Arrays.asList(ExtensionOrdering.values());
    }

    public String getRepoVersion() {
        return repoVersion;
    }

    public String getResourceUrl(String... resource) {
        return String.format("https://raw.githubusercontent.com/%s/repo/%s/%s", source, repoVersion,
                Arrays.stream(resource).map(EncodingUtil::encodeURIComponent).collect(Collectors.joining("/")));
    }
}





