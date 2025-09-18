package com.ethlo.mvc.filter;

import com.ethlo.mvc.MvcAccelerator;
import com.ethlo.mvc.config.MvcAcceleratorConfig;
import com.ethlo.mvc.fastpath.MvcAcceleratorHandlerMapping;
import com.ethlo.mvc.interceptor.InterceptorRequestMatcher;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes a minimal/optimized MVC path for high-RPS endpoints.
 * <p>
 * If {@link #selectedFilters} is empty, it behaves like the raw fast-path filter.
 * If filters are provided, they are executed in order, with the handler invoked
 * as the final element of the chain. This allows filters like
 * {@code ExceptionTranslationFilter} to wrap handler exceptions correctly.
 */
public class MvcAcceleratorPathFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(MvcAcceleratorPathFilter.class);
    private final List<HandlerAdapter> handlerAdapters;
    private final MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping;
    private final List<Map.Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> selectedInterceptors;
    private final List<Map.Entry<Filter, List<RequestMatcher>>> selectedFilters;
    private final MvcAcceleratorConfig.Mode mode;
    private final Map<Integer, CachedResult> handlerCache = new ConcurrentHashMap<>();

    public MvcAcceleratorPathFilter(MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping,
                                    List<HandlerAdapter> handlerAdapters,
                                    List<Map.Entry<HandlerInterceptor, List<InterceptorRequestMatcher>>> selectedInterceptors,
                                    List<Map.Entry<Filter, List<RequestMatcher>>> selectedFilters,
                                    MvcAcceleratorConfig.Mode mode) {
        this.mvcAcceleratorHandlerMapping = mvcAcceleratorHandlerMapping;
        this.handlerAdapters = handlerAdapters;
        this.selectedInterceptors = selectedInterceptors;
        this.selectedFilters = selectedFilters;
        this.mode = mode;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpReq) || !(response instanceof HttpServletResponse httpResp)) {
            chain.doFilter(request, response);
            return;
        }

        ServletRequestPathUtils.parseAndCache(httpReq);

        try {
            doHandle(chain, httpReq, httpResp);
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new ServletException(exc);
        }
    }

    private void doHandle(FilterChain chain, HttpServletRequest httpReq, HttpServletResponse httpResp) throws Exception {
        HandlerExecutionChain chainExec = mvcAcceleratorHandlerMapping.getHandler(httpReq);
        if (chainExec == null) {
            chain.doFilter(httpReq, httpResp);
            return;
        }

        final Object handler = chainExec.getHandler();

        if (shouldUseMvcAccelerator(handler)) {

            final int handlerId = System.identityHashCode(handler); // unique per instance

            final CachedResult cached = handlerCache.get(handlerId);
            if (cached != null) {
                new VirtualFilterChain(cached.filters, cached.interceptors, handler, handlerAdapters)
                        .doFilter(httpReq, httpResp);
                return;
            }

            final List<Filter> applicableFilters = selectedFilters.stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(matcher -> matcher.matches(httpReq)))
                    .map(Map.Entry::getKey)
                    .toList();

            final List<HandlerInterceptor> applicableInterceptors = selectedInterceptors.stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(matcher -> matcher.matches(httpReq)))
                    .map(Map.Entry::getKey)
                    .toList();

            handlerCache.put(handlerId, new CachedResult(applicableFilters, applicableInterceptors));

            // Wrap handler+interceptors inside the filter chain
            new VirtualFilterChain(applicableFilters, applicableInterceptors, handler, handlerAdapters)
                    .doFilter(httpReq, httpResp);
        } else {
            chain.doFilter(httpReq, httpResp);
        }
    }

    private boolean shouldUseMvcAccelerator(Object handler) {
        return mode == MvcAcceleratorConfig.Mode.ALL
               || (mode == MvcAcceleratorConfig.Mode.ANNOTATED
                   && handler instanceof HandlerMethod handlerMethod
                   && handlerMethod.hasMethodAnnotation(MvcAccelerator.class));
    }

    @Override
    public void destroy() {
    }

    private static class CachedResult {
        final List<Filter> filters;
        final List<HandlerInterceptor> interceptors;

        CachedResult(List<Filter> filters, List<HandlerInterceptor> interceptors) {
            this.filters = filters;
            this.interceptors = interceptors;
        }
    }
}

