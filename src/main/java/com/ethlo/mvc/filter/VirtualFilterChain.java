package com.ethlo.mvc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

/**
 * Executes filters in order, then invokes the handler as the final element.
 */
public class VirtualFilterChain implements FilterChain {
    private static final Logger logger = LoggerFactory.getLogger(VirtualFilterChain.class);
    private final List<Filter> filters;
    private final List<HandlerInterceptor> interceptors;
    private final Object handler;
    private final List<HandlerAdapter> handlerAdapters;
    private int pos = 0;

    public VirtualFilterChain(List<Filter> filters,
                              List<HandlerInterceptor> interceptors,
                              Object handler,
                              List<HandlerAdapter> handlerAdapters) {
        this.filters = filters;
        this.interceptors = interceptors;
        this.handler = handler;
        this.handlerAdapters = handlerAdapters;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
        if (pos < filters.size()) {
            final Filter next = filters.get(pos++);
            next.doFilter(req, res, this);
        } else {
            // Invoke handler inside the filter chain
            invokeHandlerWithInterceptors(handler, (HttpServletRequest) req, (HttpServletResponse) res);
        }
    }

    private void invokeHandlerWithInterceptors(Object handler,
                                               HttpServletRequest req,
                                               HttpServletResponse res) throws ServletException {
        int lastPreHandled = -1;
        Exception failure = null;

        try {
            for (int i = 0; i < interceptors.size(); i++) {
                if (!interceptors.get(i).preHandle(req, res, handler)) {
                    lastPreHandled = i - 1;
                    return;
                }
                lastPreHandled = i;
            }

            HandlerAdapter adapter = handlerAdapters.stream()
                    .filter(ha -> ha.supports(handler))
                    .findFirst()
                    .orElseThrow(() -> new ServletException("No adapter for handler: " + handler));

            ModelAndView mv = adapter.handle(req, res, handler);
            if (mv != null && !mv.wasCleared()) {
                throw new ServletException("View rendering not supported in " + getClass().getSimpleName());
            }

            for (int i = lastPreHandled; i >= 0; i--) {
                interceptors.get(i).postHandle(req, res, handler, mv);
            }

        } catch (RuntimeException ex) {
            failure = ex;
            throw ex;
        } catch (Exception ex) {
            failure = ex;
            throw new ServletException(ex);
        } finally {
            for (int i = lastPreHandled; i >= 0; i--) {
                try {
                    interceptors.get(i).afterCompletion(req, res, handler, failure);
                } catch (Exception e) {
                    logger.error("HandlerInterceptor.afterCompletion threw exception", e);
                }
            }
        }
    }
}
