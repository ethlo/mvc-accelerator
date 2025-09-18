package com.ethlo.mvc.config;

import com.ethlo.mvc.MvcAccelerator;
import com.ethlo.mvc.demo.PerformanceDemoController;
import com.ethlo.mvc.fastpath.EntryParser;
import com.ethlo.mvc.fastpath.FastEntry;
import com.ethlo.mvc.fastpath.MvcAcceleratorHandlerMapping;
import com.ethlo.mvc.filter.FilterUtils;
import com.ethlo.mvc.filter.MvcAcceleratorPathFilter;
import com.ethlo.mvc.interceptor.InterceptorRequestMatcher;
import com.ethlo.mvc.interceptor.InterceptorUtils;
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
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty("mvc.accelerator.enabled")
@EnableConfigurationProperties(MvcAcceleratorConfig.class)
public class MvcAcceleratorAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MvcAcceleratorAutoConfiguration.class);

    @ConditionalOnProperty("mvc.accelerator.demo.enabled")
    @Bean
    public PerformanceDemoController performanceDemoController() {
        return new PerformanceDemoController();
    }

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
    public FilterRegistrationBean<Filter> mvcAcceleratorFilter(MvcAcceleratorConfig mvcAcceleratorConfig, List<Filter> allFilters, List<HandlerInterceptor> allInterceptors, MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping, List<HandlerAdapter> handlerAdapters) {

        final List<Map.Entry<Filter, List<RequestMatcher>>> selectedOrderedFilters = FilterUtils.prepareFilters(mvcAcceleratorConfig, allFilters);
        final List<Map.Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> selectedInterceptors = InterceptorUtils.prepareInterceptors(mvcAcceleratorConfig, allInterceptors);
        final MvcAcceleratorPathFilter filter = new MvcAcceleratorPathFilter(
                mvcAcceleratorHandlerMapping,
                handlerAdapters,
                selectedInterceptors,
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
