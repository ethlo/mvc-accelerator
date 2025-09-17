package com.ethlo.mvc;

import com.ethlo.mvc.fastpath.EntryParser;
import com.ethlo.mvc.fastpath.FastEntry;
import com.ethlo.mvc.fastpath.MvcAcceleratorHandlerMapping;
import com.ethlo.mvc.filter.MvcAcceleratorPathFilter;
import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

import static com.ethlo.mvc.filter.FilterUtils.prepareFilters;

@AutoConfiguration
@ConditionalOnProperty("mvc.accelerator.enabled")
@EnableConfigurationProperties(MvcAcceleratorConfig.class)
public class MvcAcceleratorAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MvcAcceleratorAutoConfiguration.class);

    @Bean
    @ConditionalOnEnumProperty(name = "mvc.accelerator.fast-path.mode", havingValues = {MvcAcceleratorConfig.Mode.ALL, MvcAcceleratorConfig.Mode.ANNOTATED})
    public MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping(ApplicationContext applicationContext, RequestMappingHandlerMapping requestMappingHandlerMapping, MvcAcceleratorConfig mvcAcceleratorConfig) {
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
        logger.info("Adding MvcAccelerator handler mapping in mode {}", mvcAcceleratorConfig.getFastPath().getMode());
        return new MvcAcceleratorHandlerMapping(mvcAcceleratorConfig, fastEntries);
    }

    @Bean
    @ConditionalOnEnumProperty(name = "mvc.accelerator.fast-filter-chain.mode", havingValues = {MvcAcceleratorConfig.Mode.ALL, MvcAcceleratorConfig.Mode.ANNOTATED})
    public FilterRegistrationBean<Filter> mvcAcceleratorFilter(MvcAcceleratorConfig mvcAcceleratorConfig, List<Filter> allFilters, MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping, List<HandlerAdapter> handlerAdapters) {

        final List<Map.Entry<Filter, List<RequestMatcher>>> selectedOrderedFilters = prepareFilters(mvcAcceleratorConfig, allFilters);

        final MvcAcceleratorPathFilter filter = new MvcAcceleratorPathFilter(
                mvcAcceleratorHandlerMapping,
                handlerAdapters,
                selectedOrderedFilters,
                mvcAcceleratorConfig.getFastFilterChain().getMode()
        );

        final FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setName(MvcAccelerator.class.getSimpleName());

        // Run before Spring Security filter chain
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        logger.info("Adding MvcAccelerator filter handler in mode {}", mvcAcceleratorConfig.getFastFilterChain().getMode());

        return registration;
    }
}
