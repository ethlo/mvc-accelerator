package com.ethlo.mvc.filter;

import com.ethlo.mvc.fastpath.MvcAcceleratorHandlerMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ServletRequestPathUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Executes a minimal/optimized MVC path for high-RPS endpoints.
 * <p>
 * If {@link #selectedFilters} is empty, it behaves like the raw fast-path filter.
 * If filters are provided, they are executed in order, with the handler invoked
 * as the final element of the chain. This allows filters like
 * {@code ExceptionTranslationFilter} to wrap handler exceptions correctly.
 */
public class MvcAcceleratorPathFilter implements Filter {
    private final List<HandlerAdapter> handlerAdapters;
    private final MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping;
    private final List<Map.Entry<Filter, List<RequestMatcher>>> selectedFilters;

    public MvcAcceleratorPathFilter(MvcAcceleratorHandlerMapping mvcAcceleratorHandlerMapping,
                                    List<HandlerAdapter> handlerAdapters,
                                    List<Map.Entry<Filter, List<RequestMatcher>>> selectedFilters) {
        this.mvcAcceleratorHandlerMapping = mvcAcceleratorHandlerMapping;
        this.handlerAdapters = handlerAdapters;
        this.selectedFilters = selectedFilters;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpReq) ||
            !(response instanceof HttpServletResponse httpResp)) {
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
        // Resolve handler
        HandlerExecutionChain chainExec = mvcAcceleratorHandlerMapping.getHandler(httpReq);
        if (chainExec == null) {
            chain.doFilter(httpReq, httpResp);
            return;
        }

        final Object handler = chainExec.getHandler();

        if (selectedFilters.isEmpty()) {
            // No filters → straight-line handler invocation
            invokeHandler(handler, httpReq, httpResp);
        } else {
            // Dynamically find filters where at least one of their RequestMatchers matches the current request
            final List<Filter> applicableFilters = selectedFilters.stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(matcher -> matcher.matches(httpReq)))
                    .map(Map.Entry::getKey)
                    .toList();
            // Wrap handler as the final filter
            new VirtualFilterChain(applicableFilters, handler, handlerAdapters, httpReq, httpResp)
                    .doFilter(httpReq, httpResp);
        }
    }

    private void invokeHandler(Object handler, HttpServletRequest httpReq, HttpServletResponse httpResp)
            throws Exception {
        HandlerAdapter adapter = handlerAdapters.stream()
                .filter(ha -> ha.supports(handler))
                .findFirst()
                .orElseThrow(() -> new ServletException("No adapter for handler: " + handler));

        ModelAndView mv = adapter.handle(httpReq, httpResp, handler);
        if (mv != null && !mv.wasCleared()) {
            throw new ServletException("View rendering not supported in " + getClass().getSimpleName());
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

}
