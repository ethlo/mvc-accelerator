package com.ethlo.spring.fastpath;

import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import java.util.List;

public record FastEntry(
        String pathPrefix,          // static prefix up to first variable
        List<String> variableNames, // ["var1","var2"]
        MediaType produces,
        HandlerMethod method
) {}
