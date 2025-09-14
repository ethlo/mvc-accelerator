package com.ethlo.spring.fastpath;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import java.util.*;

public class HighRpsHandlerMapping extends AbstractHandlerMapping {

    private final List<FastEntry> entries;
    private final List<HandlerInterceptor> interceptors;

    public HighRpsHandlerMapping(List<FastEntry> entries, List<HandlerInterceptor> interceptors) {
        this.entries = entries;
        this.interceptors = interceptors;
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) {
        String requestPath = request.getServletPath();

        for (FastEntry entry : entries) {
            if (!requestPath.startsWith(entry.pathPrefix())) continue;

            Map<String, String> vars = new HashMap<>();
            String remainder = requestPath.substring(entry.pathPrefix().length());

            if (!entry.variableNames().isEmpty()) {
                String[] parts = remainder.split("/");
                if (parts.length != entry.variableNames().size()) continue;

                for (int i = 0; i < parts.length; i++) {
                    vars.put(entry.variableNames().get(i), parts[i]);
                }
            }

            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, vars);
            request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(entry.produces()));

            return new HandlerExecutionChain(entry.method(), interceptors.toArray(new HandlerInterceptor[0]));
        }

        return null; // fall back
    }
}
