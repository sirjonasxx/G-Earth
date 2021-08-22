package gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.misc.OSValidator;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;

import java.util.Collections;
import java.util.List;

public class SearchedQueryOverview extends QueriedExtensionOverview {

    private final String queryString;
    private final ExtensionOrdering ordering;
    private final List<String> filterClients;
    private final List<String> filterFrameworks;
    private final List<String> filterCategories;
    private final boolean includeOutdated;
    private final boolean invert;

    public SearchedQueryOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository, String queryString, ExtensionOrdering ordering, List<String> filterClients, List<String> filterFrameworks, List<String> filterCategories, boolean includeOutdated, boolean invert) {
        super(parent, startIndex, size, storeRepository);
        this.queryString = queryString;
        this.ordering = ordering;
        this.filterClients = filterClients;
        this.filterFrameworks = filterFrameworks;
        this.filterCategories = filterCategories;
        this.includeOutdated = includeOutdated;
        this.invert = invert;
    }

    @Override
    protected List<StoreExtension> query(int startIndex, int size) {
        return storeRepository.getExtensions(startIndex, size, queryString, ordering,
                Collections.singletonList(OSValidator.getOSFull()), filterClients, filterFrameworks,
                filterCategories, includeOutdated, invert);

    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/search.png";
            }

            @Override
            public String title() {
                return "Search";
            }

            @Override
            public String description() {
                return "Find the extension that fits your needs";
            }

            @Override
            public String contentTitle() {
                return "Search results";
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new SearchedQueryOverview(parent, startIndex, size, storeRepository,
                queryString, ordering, filterClients, filterFrameworks, filterCategories,
                includeOutdated, invert);
    }
}
