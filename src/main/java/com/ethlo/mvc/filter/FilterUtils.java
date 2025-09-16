package com.ethlo.mvc.filter;

import jakarta.servlet.Filter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.filter.CompositeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for flattening nested filters (CompositeFilter / FilterChainProxy)
 * and selecting only those that match a given predicate.
 */
public final class FilterUtils
{
    private FilterUtils()
    {
    }

    /**
     * Recursively flatten filters, preserving order.
     *
     * @param filters list of top-level filters
     * @return flat list of all actual filters
     */
    public static List<Filter> flattenFilters(List<Filter> filters)
    {
        List<Filter> result = new ArrayList<>();
        for (Filter f : filters)
        {
            if (f instanceof CompositeFilter cf)
            {
                result.addAll(flattenFilters(getFiltersFromComposite(cf)));
            }
            else if (f instanceof FilterChainProxy fcp)
            {
                fcp.getFilterChains().forEach(chain -> result.addAll(flattenFilters(chain.getFilters())));
            }
            else
            {
                result.add(f);
            }
        }
        return result;
    }

    private static List<Filter> getFiltersFromComposite(Filter composite)
    {
        if (!(composite.getClass().getSimpleName().equals("CompositeFilter")))
        {
            return Collections.singletonList(composite);
        }

        try
        {
            Field f = composite.getClass().getDeclaredField("filters");
            f.setAccessible(true);
            Object value = f.get(composite);
            if (value instanceof List<?> list)
            {
                List<Filter> result = new ArrayList<>();
                for (Object o : list)
                {
                    if (o instanceof Filter filter)
                    {
                        result.add(filter);
                    }
                }
                return result;
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read CompositeFilter filters", e);
        }
        return Collections.emptyList();
    }
}
