package com.ethlo.mvc.fastpath;

import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.List;

public record FastEntry(
        RequestMappingInfo requestMappingInfo,
        String pattern,
        org.springframework.web.bind.annotation.RequestMethod requestMethod,
        String pathPrefix,          // static prefix up to first variable
        List<String> variableNames, // ["var1","var2"]
        MediaType produces,
        HandlerMethod handlerMethod,
        int order,

        boolean shortCircuit) implements Comparable<FastEntry> {
    @Override
    public int compareTo(FastEntry o) {
        return Integer.compare(order, o.order);
    }
}
