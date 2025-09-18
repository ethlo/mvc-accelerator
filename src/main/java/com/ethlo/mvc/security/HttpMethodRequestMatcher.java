package com.ethlo.mvc.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class HttpMethodRequestMatcher implements RequestMatcher {

    private final HttpMethod method;

    public HttpMethodRequestMatcher(HttpMethod method) {
        this.method = method;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return this.method.name().equals(request.getMethod());
    }

    @Override
    public String toString() {
        return "HttpMethod [" + this.method + "]";
    }

}