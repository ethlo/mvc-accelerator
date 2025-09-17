package com.ethlo.mvc.filter;

import com.ethlo.mvc.MvcAcceleratorConfig;
import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Utility for flattening nested filters (CompositeFilter / FilterChainProxy)
 * and selecting only those that match a given predicate.
 */
public final class FilterUtils {
    private static final Logger logger = LoggerFactory.getLogger(FilterUtils.class);

    private FilterUtils() {
    }

    /**
     * Groups matchers by filter while preserving the "first-seen" order of the filters.
     *
     * @param matcherToFiltersMap A map where keys are matchers and values are lists of filters.
     *                            This map should preserve order (e.g., a LinkedHashMap)
     *                            for the result to be meaningfully ordered.
     * @return A List of Map.Entry objects, where each entry contains a Filter
     * and the complete list of its associated RequestMatchers, in order.
     */
    public static List<Map.Entry<Filter, List<RequestMatcher>>> groupMatchersByFilterOrdered(
            Map<RequestMatcher, List<Filter>> matcherToFiltersMap) {

        if (matcherToFiltersMap == null) {
            return Collections.emptyList();
        }

        // Use a LinkedHashMap to preserve the insertion order of filters as they are first seen.
        Map<Filter, List<RequestMatcher>> orderedGrouping = new LinkedHashMap<>();

        // Iterate through the source map (e.g., flatFilters)
        for (Map.Entry<RequestMatcher, List<Filter>> entry : matcherToFiltersMap.entrySet()) {
            RequestMatcher matcher = entry.getKey();
            List<Filter> filters = entry.getValue();

            if (filters != null) {
                // For each filter, add the current matcher to its list.
                for (Filter filter : filters) {
                    // computeIfAbsent ensures a filter is only added to the map once,
                    // locking in its position.
                    orderedGrouping.computeIfAbsent(filter, k -> new ArrayList<>()).add(matcher);
                }
            }
        }

        // Convert the entry set of the ordered map into a list.
        return new ArrayList<>(orderedGrouping.entrySet());
    }

    private static RequestMatcher extractMatcher(SecurityFilterChain chain) {
        try {
            Field f = chain.getClass().getDeclaredField("requestMatcher");
            f.setAccessible(true);
            return (RequestMatcher) f.get(chain);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to extract RequestMatcher from chain: " + chain.getClass(), e
            );
        }
    }

    public static Map<RequestMatcher, List<Filter>> mapFiltersByMatcher(List<Filter> filters) {
        Map<RequestMatcher, List<Filter>> orderedMap = new LinkedHashMap<>();

        for (Filter f : filters) {
            if (f instanceof FilterChainProxy fcp) {
                fcp.getFilterChains().forEach(chain -> {
                    orderedMap.computeIfAbsent(extractMatcher(chain), k -> new ArrayList<>())
                            .addAll(chain.getFilters());
                });
            } else if (f.getClass().getSimpleName().equals("CompositeFilter")) {
                List<Filter> inner = FilterUtils.getFiltersFromComposite(f);
                orderedMap.computeIfAbsent(AnyRequestMatcher.INSTANCE, k -> new ArrayList<>())
                        .addAll(inner);
            } else {
                // Simple filter, assume match-all
                orderedMap.computeIfAbsent(AnyRequestMatcher.INSTANCE, k -> new ArrayList<>())
                        .add(f);
            }
        }
        return orderedMap;
    }


    private static List<Filter> getFiltersFromComposite(Filter composite) {
        if (!(composite.getClass().getSimpleName().equals("CompositeFilter"))) {
            return Collections.singletonList(composite);
        }

        try {
            Field f = composite.getClass().getDeclaredField("filters");
            f.setAccessible(true);
            Object value = f.get(composite);
            if (value instanceof List<?> list) {
                List<Filter> result = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Filter filter) {
                        result.add(filter);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CompositeFilter filters", e);
        }
        return Collections.emptyList();
    }

    public static List<Map.Entry<Filter, List<RequestMatcher>>> prepareFilters(final MvcAcceleratorConfig mvcAcceleratorConfig, final List<Filter> allFilters) {
        final List<String> filtersToKeep = mvcAcceleratorConfig.getFastFilterChain().getIncludedFilters();
        final Map<RequestMatcher, List<Filter>> flatFilters = FilterUtils.mapFiltersByMatcher(allFilters);
        logger.info("All filters in encountered order:\n{}",
                StringUtils.collectionToDelimitedString(
                        flatFilters.values().stream()
                                .flatMap(List::stream)
                                .map(f -> f.getClass().getName())
                                .toList(),
                        "\n"
                )
        );
        final List<Map.Entry<Filter, List<RequestMatcher>>> allOrderedFilters = FilterUtils.groupMatchersByFilterOrdered(flatFilters);

        if (filtersToKeep.size() == 1 && "*".equals(filtersToKeep.get(0))) {
            logFiltersIncluded(allOrderedFilters);
            return allOrderedFilters;
        }

        final List<Map.Entry<Filter, List<RequestMatcher>>> selectedOrderedFilters = new ArrayList<>();
        for (List<Filter> inOrderList : flatFilters.values()) {
            for (Filter inOrder : inOrderList) {
                final String name = inOrder.getClass().getName();
                final boolean keep = filtersToKeep.contains(name);
                if (keep) {
                    final Optional<Map.Entry<Filter, List<RequestMatcher>>> foundEntry = allOrderedFilters.stream().filter(f -> f.getKey().getClass().getName().equals(name)).findFirst();
                    foundEntry.ifPresentOrElse(selectedOrderedFilters::add, () -> {
                        throw new IllegalStateException("Cannot find filter " + name);
                    });
                }
                logger.debug("Filter {}: {} ", name, keep ? "Yes" : "No");
            }
        }

        logFiltersIncluded(selectedOrderedFilters);

        return selectedOrderedFilters;
    }

    private static void logFiltersIncluded(List<Map.Entry<Filter, List<RequestMatcher>>> orderedFilters) {
        logger.info("Included filters (in order):\n{}", StringUtils.collectionToDelimitedString(orderedFilters.stream().map(e -> e.getKey().getClass().getName()).toList(), "\n"));
    }
}
