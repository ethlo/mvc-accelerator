package com.ethlo.mvc;

import com.ethlo.mvc.fastpath.EntryParser;
import com.ethlo.mvc.fastpath.FastEntry;
import com.ethlo.mvc.fastpath.MvcAcceleratorHandlerMapping;
import com.ethlo.mvc.filter.FilterUtils;
import com.ethlo.mvc.filter.MvcAcceleratorPathFilter;
import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoConfiguration
@EnableConfigurationProperties(MvcAcceleratorConfig.class)
public class MvcAcceleratorAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(MvcAcceleratorAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty("mvc.accelerator.fast-path.enabled")
    public MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping(ApplicationContext applicationContext, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        final Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        final List<FastEntry> fastEntries = handlerMethods
                .entrySet()
                .stream()
                .flatMap(entry ->
                {
                    final HandlerMethod rawHandlerMethod = entry.getValue();
                    final HandlerMethod handlerMethod = new HandlerMethod(applicationContext.getBean((String) rawHandlerMethod.getBean()), rawHandlerMethod.getMethod());
                    return EntryParser.parseEntry(handlerMethod, entry.getKey()).stream();
                })
                .sorted()
                .toList();

        return new MvcAcceleratorHandlerMapping(fastEntries);
    }

    @Bean
    @ConditionalOnProperty("mvc.accelerator.fast-filter-chain.enabled")
    public FilterRegistrationBean<Filter> mvcAcceleratorFilter(MvcAcceleratorConfig mvcAcceleratorConfig, List<Filter> allFilters, MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping, List<HandlerAdapter> handlerAdapters) {
        final List<String> filtersToKeep = mvcAcceleratorConfig.getFastFilterChain().getIncludedFilters();
        final List<Filter> flatFilters = FilterUtils.flattenFilters(allFilters);
        logger.info("All available filters: {}", flatFilters.stream().map(f -> f.getClass().getName()).toList());
        final List<Filter> selectedFilters = getFiltersEnabled(mvcAcceleratorConfig, flatFilters, filtersToKeep);
        logger.info("Allowed filters for fast-path:\n{}", StringUtils.collectionToDelimitedString(filtersToKeep, "\n"));
        logger.info("Actually included filters for fast-path:\n{}", StringUtils.collectionToDelimitedString(selectedFilters.stream().map(Object::getClass).map(Class::getName).toList(), "\n"));

        final MvcAcceleratorPathFilter filter = new MvcAcceleratorPathFilter(mvcAcceleratorHandlerMapping, handlerAdapters, selectedFilters);
        final FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setName(MvcAccelerator.class.getSimpleName());

        // Run before Spring Security filter chain
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private List<Filter> getFiltersEnabled(MvcAcceleratorConfig mvcAcceleratorConfig, List<Filter> flatFilters, List<String> filtersToKeep) {

        final List<String> included = mvcAcceleratorConfig.getFastFilterChain().getIncludedFilters();
        if (included.size() == 1 && included.get(0).equals("*")) {
            logger.info("Allowing all filters");
            return flatFilters;
        }

        final List<Filter> selectedFilters = flatFilters.stream()
                .filter(f -> filtersToKeep.stream()
                        .anyMatch(fqn ->
                        {
                            final Class<?> targetClass = AopUtils.getTargetClass(f);
                            return targetClass.getName().equals(fqn);
                        }))
                .toList();

        if (selectedFilters.size() != filtersToKeep.size()) {
            final Set<String> missing = filtersToKeep.stream()
                    .filter(fqn -> selectedFilters.stream()
                            .noneMatch(f -> AopUtils.getTargetClass(f).getName().equals(fqn)))
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                if (mvcAcceleratorConfig.getFastFilterChain().isFailIfFilterMissing()) {
                    throw new IllegalStateException("Fast-path filter configuration missing required filters: " + missing);
                } else {
                    logger.warn("Fast-path filter configuration missing required filters: {}", missing);
                }
            }
        }
        return selectedFilters;
    }

}
