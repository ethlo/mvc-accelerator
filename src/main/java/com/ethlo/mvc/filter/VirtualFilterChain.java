package com.ethlo.mvc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;

/**
 * Executes filters in order, then invokes the handler as the final element.
 */
public class VirtualFilterChain implements FilterChain {
    private static final Logger logger = LoggerFactory.getLogger(VirtualFilterChain.class);
    private final List<Filter> filters;
    private final Object handler;
    private final List<HandlerAdapter> handlerAdapters;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private int pos = 0;

    public VirtualFilterChain(List<Filter> filters,
                              Object handler,
                              List<HandlerAdapter> handlerAdapters,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        this.filters = filters;
        this.handler = handler;
        this.handlerAdapters = handlerAdapters;
        this.request = request;
        this.response = response;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
        if (pos < filters.size()) {
            final Filter next = filters.get(pos++);
            next.doFilter(req, res, this);
        } else {
            // Invoke handler inside the filter chain
            try {
                HandlerAdapter adapter = handlerAdapters.stream()
                        .filter(ha -> ha.supports(handler))
                        .findFirst()
                        .orElseThrow(() -> new ServletException("No adapter for " + handler));

                ModelAndView mv = adapter.handle(request, response, handler);
                if (mv != null && !mv.wasCleared()) {
                    throw new ServletException("View rendering not supported in fast path");
                }
            } catch (ServletException | IOException exc) {
                throw exc;
            } catch (Exception exc) {
                throw new ServletException(exc);
            }
        }
    }
}
